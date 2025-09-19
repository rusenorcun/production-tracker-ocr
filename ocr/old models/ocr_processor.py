from dataclasses import dataclass
from pathlib import Path
from typing import List, Tuple, Optional, Dict, Any
import logging
import csv
import glob
import os
import re
from tqdm import tqdm

import cv2
import numpy as np
from ultralytics import YOLO

from image_preprocessor import ImagePreprocessor, ImageVariant

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('ocr_processing.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

@dataclass
class Config:
    """Configuration class for OCR processing"""
    images_dir: str = "images/test"  # Fixed to use test directory
    output_dir: str = "outputs"
    cache_dir: str = "cache"  # Base cache directory
    processed_cache_dir: str = "cache/processed"  # Directory for processed images
    results_cache_dir: str = "cache/results"  # Directory for detailed results
    csv_name: str = "results.csv"
    digit_model_path: str = "weights/digit2.pt"
    roi_model_path: str = "weights/roi_best.pt"
    conf_digit: float = 0.1
    conf_roi: float = 0.6
    merge_iou: float = 0.35
    debug_draw_index: bool = True

@dataclass
class DetectionBox:
    """Represents a detection box with class, confidence and coordinates"""
    cls: Optional[int]
    conf: float
    box: Tuple[float, float, float, float]  # (x1, y1, x2, y2)

@dataclass
class VariantResult:
    """Results for a single image variant"""
    variant: ImageVariant
    roi_items: List[DetectionBox]
    digit_items: List[DetectionBox]
    text_results: List[Tuple[int, str]]
    avg_confidence: float

class OCRProcessor:
    """Main class for OCR processing pipeline"""
    
    def __init__(self, config: Config):
        self.config = config
        self.validate_config()
        self._init_models()
        self.preprocessor = ImagePreprocessor(
            cache_dir=config.cache_dir,
            processed_dir=config.processed_cache_dir,
            results_dir=config.results_cache_dir
        )
        Path(config.output_dir).mkdir(exist_ok=True)
        
    def validate_config(self) -> None:
        """Validate configuration settings"""
        if not Path(self.config.digit_model_path).exists():
            raise FileNotFoundError(f"Digit model not found at {self.config.digit_model_path}")
        if not Path(self.config.roi_model_path).exists():
            raise FileNotFoundError(f"ROI model not found at {self.config.roi_model_path}")
        if not Path(self.config.images_dir).exists():
            raise FileNotFoundError(f"Images directory not found at {self.config.images_dir}")

    def _init_models(self) -> None:
        """Initialize YOLO models"""
        try:
            self.digit_model = YOLO(self.config.digit_model_path)
            self.roi_model = YOLO(self.config.roi_model_path)
            logger.info("Models loaded successfully")
        except Exception as e:
            logger.error(f"Error loading models: {str(e)}")
            raise

    @staticmethod
    def iou_xyxy(a: Tuple[float, float, float, float], 
                 b: Tuple[float, float, float, float]) -> float:
        """Calculate Intersection over Union (IoU) between two boxes"""
        ax1, ay1, ax2, ay2 = a
        bx1, by1, bx2, by2 = b
        ix1, iy1 = max(ax1, bx1), max(ay1, by1)
        ix2, iy2 = min(ax2, bx2), min(ay2, by2)
        iw, ih = max(0.0, ix2 - ix1), max(0.0, iy2 - iy1)
        inter = iw * ih
        if inter <= 0: 
            return 0.0
        area_a = max(0.0, ax2 - ax1) * max(0.0, ay2 - ay1)
        area_b = max(0.0, bx2 - bx1) * max(0.0, by2 - by1)
        union = area_a + area_b - inter
        return inter / union if union > 0 else 0.0

    def nms_agnostic_keep_best(self, items: List[DetectionBox], 
                              iou_thr: float) -> List[DetectionBox]:
        """Non-Maximum Suppression keeping highest confidence detections"""
        if not items:
            return []
        items = sorted(items, key=lambda d: d.conf, reverse=True)
        keep = []
        suppressed = [False] * len(items)
        
        for i in range(len(items)):
            if suppressed[i]:
                continue
            keep.append(items[i])
            for j in range(i+1, len(items)):
                if suppressed[j]:
                    continue
                if self.iou_xyxy(items[i].box, items[j].box) >= iou_thr:
                    suppressed[j] = True
        return keep

    def order_rois_pca(self, rois: List[DetectionBox], 
                       prefer_top_first: bool = True) -> Tuple[List[DetectionBox], 
                                                             Optional[np.ndarray], 
                                                             Optional[np.ndarray]]:
        """Order ROIs using PCA-based sorting"""
        if not rois:
            return [], None, None
        if len(rois) == 1:
            box = rois[0].box
            c = np.array([[(box[0]+box[2])/2.0, (box[1]+box[3])/2.0]], dtype=np.float32)
            v = np.array([0.0, -1.0], dtype=np.float32)
            return rois, c.mean(axis=0), v

        centers = np.array(
            [[(r.box[0]+r.box[2])/2.0, (r.box[1]+r.box[3])/2.0] for r in rois],
            dtype=np.float32
        )
        mean = centers.mean(axis=0)

        try:
            X = centers - mean
            _, _, Vh = np.linalg.svd(X, full_matrices=False)
            v = Vh[0].astype(np.float32)

            if prefer_top_first and v[1] > 0:
                v = -v
            elif not prefer_top_first and v[1] < 0:
                v = -v

            t = X @ v
            order = np.argsort(-t if prefer_top_first else t)
            ordered = [rois[i] for i in order.tolist()]
            return ordered, mean, v
        except np.linalg.LinAlgError as e:
            logger.error(f"PCA calculation failed: {str(e)}")
            return rois, None, None

    def process_variant(self, variant: ImageVariant) -> VariantResult:
        """Process a single image variant and return results"""
        # Detect digits
        det_digits = self.digit_model.predict(
            variant.image, conf=self.config.conf_digit, verbose=False)[0]
        raw_digits = []
        
        if det_digits.boxes is not None:
            for box in det_digits.boxes:
                x1,y1,x2,y2 = [float(v) for v in box.xyxy[0].cpu().numpy()]
                cls = int(box.cls[0].cpu().numpy())
                conf = float(box.conf[0].cpu().numpy())
                raw_digits.append(DetectionBox(cls, conf, (x1,y1,x2,y2)))

        # Detect ROIs
        det_rois = self.roi_model.predict(
            variant.image, conf=self.config.conf_roi, verbose=False)[0]
        raw_rois = []
        
        if det_rois.boxes is not None:
            for box in det_rois.boxes:
                x1,y1,x2,y2 = [float(v) for v in box.xyxy[0].cpu().numpy()]
                conf = float(box.conf[0].cpu().numpy())
                raw_rois.append(DetectionBox(None, conf, (x1,y1,x2,y2)))

        # Apply NMS and PCA ordering
        roi_items = self.nms_agnostic_keep_best(raw_rois, self.config.merge_iou)
        digit_items = self.nms_agnostic_keep_best(raw_digits, self.config.merge_iou)
        roi_items, _, _ = self.order_rois_pca(roi_items)

        # Process each ROI
        text_results = []
        total_conf = 0.0
        
        for idx, roi in enumerate(roi_items):
            dins = self.digits_inside_roi(roi.box, digit_items)
            text = self.sort_digits_line(dins)
            text_results.append((idx, text))
            
            # Add confidences for averaging
            total_conf += roi.conf
            total_conf += sum(d.conf for d in dins)

        # Calculate average confidence
        avg_conf = total_conf / (len(roi_items) + len(digit_items)) if (len(roi_items) + len(digit_items)) > 0 else 0

        return VariantResult(variant, roi_items, digit_items, text_results, avg_conf)

    def process_image(self, img_path: str, writer: csv.writer) -> int:
        """Process a single image and return number of rows written"""
        try:
            img = cv2.imread(img_path)
            if img is None:
                logger.error(f"Failed to load image: {img_path}")
                return 0

            # Create and process variants
            variants = self.preprocessor.create_image_variants(img)
            variant_results = []
            
            logger.info(f"Processing {len(variants)} variants for {img_path}")
            for variant in variants:
                result = self.process_variant(variant)
                variant_results.append(result)

            # Select best variant based on confidence
            best_result = max(variant_results, key=lambda x: x.avg_confidence)
            logger.info(f"Best variant confidence: {best_result.avg_confidence:.3f}")

            # Write results and create output image
            img_output = best_result.variant.image.copy()
            wrote_rows = 0

            for idx, text in best_result.text_results:
                roi = best_result.roi_items[idx]
                rx1, ry1, rx2, ry2 = roi.box

                # Draw ROI box and text
                cv2.rectangle(img_output, (int(rx1),int(ry1)), (int(rx2),int(ry2)), 
                            (0,255,0), 2)
                cv2.putText(img_output, text if text else "-", 
                          (int(rx1), max(0, int(ry1)-5)),
                          cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0,0,255), 2)

                if self.config.debug_draw_index:
                    cv2.putText(img_output, f"#{idx}", (int(rx1), int(ry1)+20),
                              cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0,255,0), 2)

                writer.writerow([Path(img_path).name, idx, text])
                wrote_rows += 1

            # Save output image with best results
            out_path = Path(self.config.output_dir)/Path(img_path).name
            cv2.imwrite(str(out_path), img_output)

            # Cache the results for the best variant
            self.preprocessor.cache_results(
                img_path,
                best_result.variant.contrast,
                best_result.variant.brightness,
                {
                    'text_results': best_result.text_results,
                    'roi_confidences': [r.conf for r in best_result.roi_items],
                    'digit_confidences': [d.conf for d in best_result.digit_items]
                },
                best_result.variant.image
            )

            logger.debug(f"Processed {img_path}: {wrote_rows} ROIs found")
            return wrote_rows

        except Exception as e:
            logger.error(f"Error processing {img_path}: {str(e)}")
            return 0

    @staticmethod
    def digits_inside_roi(roi_box: Tuple[float, float, float, float], 
                         digit_items: List[DetectionBox]) -> List[DetectionBox]:
        """Find digits inside a ROI box"""
        rx1, ry1, rx2, ry2 = roi_box
        inside = []
        for it in digit_items:
            x1,y1,x2,y2 = it.box
            cx, cy = (x1+x2)/2, (y1+y2)/2
            if rx1 <= cx <= rx2 and ry1 <= cy <= ry2:
                inside.append(it)
        return inside

    @staticmethod
    def sort_digits_line(digits: List[DetectionBox]) -> str:
        """Sort and combine digits into text, handling multiple lines"""
        if not digits:
            return ""
            
        ys = [((d.box[1]+d.box[3])/2) for d in digits]
        hs = [(d.box[3]-d.box[1]) for d in digits]
        median_h = max(1.0, sum(hs)/len(hs))
        row_gap = max(10.0, median_h * 0.6)

        used = [False]*len(digits)
        rows = []
        
        for i in range(len(digits)):
            if used[i]:
                continue
            cy = ys[i]
            row_idxs = []
            
            for j in range(len(digits)):
                if used[j]:
                    continue
                if abs(ys[j]-cy) <= row_gap:
                    row_idxs.append(j)
                    used[j] = True
                    
            row = [digits[k] for k in row_idxs]
            row.sort(key=lambda d: (d.box[0] + d.box[2]) / 2.0)
            rows.append("".join(str(int(d.cls)) for d in row))
            
        return "|".join(rows)

    @staticmethod
    def natural_key(s: str) -> List[Any]:
        """Key function for natural sorting of strings"""
        return [int(t) if t.isdigit() else t.lower() for t in re.split(r'(\d+)', s)]

    def process_all_images(self) -> None:
        """Process all images in the configured directory"""
        try:
            csv_path = Path(self.config.output_dir)/self.config.csv_name
            
            # Ensure all necessary directories exist
            Path(self.config.output_dir).mkdir(exist_ok=True)
            Path(self.config.cache_dir).mkdir(exist_ok=True)
            Path(self.config.processed_cache_dir).mkdir(exist_ok=True)
            Path(self.config.results_cache_dir).mkdir(exist_ok=True)

            # Collect image paths
            patterns = ("*.jpg","*.jpeg","*.png","*.bmp","*.JPG","*.JPEG","*.PNG")
            images = []
            for patt in patterns:
                images += glob.glob(os.path.join(self.config.images_dir, patt))
            images = list(dict.fromkeys(images))
            images.sort(key=self.natural_key)
            
            logger.info(f"Processing {len(images)} images with contrast/brightness variations")

            if not images:
                logger.warning("No images found in specified directory")
                return

            with open(csv_path, "w", newline="", encoding="utf-8") as f:
                writer = csv.writer(f, delimiter=';')
                writer.writerow(["image","roi_index","text"])
                
                for p in tqdm(images, desc="Processing images"):
                    n = self.process_image(p, writer)
                    if n == 0:
                        writer.writerow([Path(p).name, -1, "-"])

            logger.info(f"Processing complete. Results saved to {csv_path}")

        except Exception as e:
            logger.error(f"Error in processing pipeline: {str(e)}")
            raise

def main():
    """Main entry point"""
    try:
        config = Config()
        processor = OCRProcessor(config)
        processor.process_all_images()
    except Exception as e:
        logger.error(f"Error in main: {str(e)}")
        raise

if __name__ == "__main__":
    main()
