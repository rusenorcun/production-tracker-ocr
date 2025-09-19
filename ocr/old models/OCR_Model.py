# offline_pipeline_nms.py
import os, csv, glob, math
from pathlib import Path
import cv2
from ultralytics import YOLO

# ================== AYARLAR ==================
IMAGES_DIR = "images/test"
OUTPUT_DIR = "outputs"
CSV_NAME   = "results.csv"

DIGIT_MODEL_PATH = "weights/digit_best.pt"
ROI_MODEL_PATH   = "weights/roi_best.pt"

CONF_DIGIT = 0.5
CONF_ROI   = 0.2

# "Birleşme değeri": IoU > MERGE_IOU ise aynı kare say ve en yüksek güveni bırak
MERGE_IOU  = 0.35
# =============================================

digit_model = YOLO(DIGIT_MODEL_PATH)
roi_model   = YOLO(ROI_MODEL_PATH)

def iou_xyxy(a, b):
    ax1, ay1, ax2, ay2 = a
    bx1, by1, bx2, by2 = b
    ix1, iy1 = max(ax1, bx1), max(ay1, by1)
    ix2, iy2 = min(ax2, bx2), min(ay2, by2)
    iw, ih = max(0.0, ix2 - ix1), max(0.0, iy2 - iy1)
    inter = iw * ih
    if inter <= 0: return 0.0
    area_a = max(0.0, ax2 - ax1) * max(0.0, ay2 - ay1)
    area_b = max(0.0, bx2 - bx1) * max(0.0, by2 - by1)
    union = area_a + area_b - inter
    return inter / union if union > 0 else 0.0

def nms_agnostic_keep_best(items, iou_thr):
    """
    Class-agnostic NMS: items -> list of dicts:
      {"cls": int or None, "conf": float, "box": (x1,y1,x2,y2)}
    IoU > iou_thr çakışanlardan sadece EN YÜKSEK güveni bırakır.
    """
    if not items: return []
    items = sorted(items, key=lambda d: d["conf"], reverse=True)
    keep = []
    suppressed = [False] * len(items)
    for i in range(len(items)):
        if suppressed[i]: continue
        keep.append(items[i])
        for j in range(i+1, len(items)):
            if suppressed[j]: continue
            if iou_xyxy(items[i]["box"], items[j]["box"]) >= iou_thr:
                suppressed[j] = True
    return keep

def digits_inside_roi(roi_box, digit_items):
    rx1, ry1, rx2, ry2 = roi_box
    inside = []
    for it in digit_items:
        x1,y1,x2,y2 = it["box"]
        cx, cy = (x1+x2)/2, (y1+y2)/2
        if rx1 <= cx <= rx2 and ry1 <= cy <= ry2:
            inside.append(it)
    return inside

def sort_digits_line(digits):
    """Basit 2-satır/çok-satır gruplayıp soldan-sağa sırala ve birleştir."""
    if not digits:
        return ""
    # satır merkezleri
    ys = [((d["box"][1]+d["box"][3])/2) for d in digits]
    hs = [(d["box"][3]-d["box"][1]) for d in digits]
    median_h = max(1.0, sum(hs)/len(hs))
    row_gap = max(10.0, median_h * 0.6)

    # kullanılmamışları gez
    used = [False]*len(digits)
    rows = []
    for i in range(len(digits)):
        if used[i]: continue
        cy = ys[i]
        row_idxs = []
        for j in range(len(digits)):
            if used[j]: continue
            if abs(ys[j]-cy) <= row_gap:
                row_idxs.append(j)
                used[j] = True
        # satır içi x'e göre sırala
        row = [digits[k] for k in row_idxs]
        row.sort(key=lambda d: (d["box"][0] + d["box"][2]) / 2.0)
        rows.append("".join(str(int(d["cls"])) for d in row))
    return ";".join(rows)

def process_image(img_path, writer):
    img = cv2.imread(img_path)

    # 1) DIGIT tespiti (ham)
    det_digits = digit_model.predict(img, conf=CONF_DIGIT, verbose=False)[0]
    raw_digits = []
    if det_digits.boxes is not None:
        for box in det_digits.boxes:
            x1,y1,x2,y2 = [float(v) for v in box.xyxy[0].cpu().numpy()]
            cls  = int(box.cls[0].cpu().numpy())
            conf = float(box.conf[0].cpu().numpy())
            raw_digits.append({"cls": cls, "conf": conf, "box": (x1,y1,x2,y2)})

    # 1b) DIGIT ikinci aşama NMS (aynı karede tek en iyi)
    digit_items = nms_agnostic_keep_best(raw_digits, MERGE_IOU)

    # 2) ROI tespiti (ham)
    det_rois = roi_model.predict(img, conf=CONF_ROI, verbose=False)[0]
    raw_rois = []
    if det_rois.boxes is not None:
        for box in det_rois.boxes:
            x1,y1,x2,y2 = [float(v) for v in box.xyxy[0].cpu().numpy()]
            conf = float(box.conf[0].cpu().numpy())
            raw_rois.append({"cls": None, "conf": conf, "box": (x1,y1,x2,y2)})

    # 2b) ROI ikinci aşama NMS (çakışan bloklardan tek en iyi)
    roi_items = nms_agnostic_keep_best(raw_rois, MERGE_IOU)

    # 3) Her ROI için rakamları sırala, çiz ve CSV'ye yaz
    for idx, roi in enumerate(roi_items):
        rx1, ry1, rx2, ry2 = roi["box"]
        dins = digits_inside_roi(roi["box"], digit_items)
        text = sort_digits_line(dins)

        # ROI kutusu ve okunan metin
        cv2.rectangle(img, (int(rx1),int(ry1)), (int(rx2),int(ry2)), (0,255,0), 2)
        cv2.putText(img, text if text else "-", (int(rx1), max(0, int(ry1)-5)),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0,255,0), 2)

        writer.writerow([Path(img_path).name, idx, text])

    # Kaydet
    Path(OUTPUT_DIR).mkdir(exist_ok=True)
    out_path = Path(OUTPUT_DIR)/Path(img_path).name
    cv2.imwrite(str(out_path), img)

def main():
    Path(OUTPUT_DIR).mkdir(exist_ok=True)
    csv_path = Path(OUTPUT_DIR)/CSV_NAME

    patterns = ("*.jpg","*.jpeg","*.png","*.bmp","*.JPG","*.JPEG","*.PNG")
    images = sorted({p for patt in patterns for p in glob.glob(os.path.join(IMAGES_DIR, patt))})
    if not images:
        print("Görsel bulunamadı.")
        return

    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["image","roi_index","text"])
        for p in images:
            process_image(p, w)

    print("CSV hazır ->", csv_path)

if __name__ == "__main__":
    main()
