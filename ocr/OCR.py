# pipeline.py (ROI içindeki her satır için ayrı index ve CSV satırı yazar)
import os, csv, glob, re, argparse, sys, uuid, time
from pathlib import Path
import cv2
import numpy as np
from ultralytics import YOLO

# ================== DEFAULTS ==================
IMAGES_DIR = "images/test"
OUTPUT_DIR = "outputs"
CSV_NAME   = "results.csv"

DIGIT_MODEL_PATH = "weights/digit2.pt"
ROI_MODEL_PATH   = "weights/roi_best.pt"

CONF_DIGIT = 0.1
CONF_ROI   = 0.6
MERGE_IOU  = 0.35

DEBUG_DRAW_AXIS  = False
DEBUG_DRAW_INDEX = True
# ==============================================

COLOR_ROI_BOX = (0, 255, 0)   # yeşil
COLOR_TEXT    = (0, 0, 255)   # kırmızı
COLOR_INDEX   = (128, 0, 0)   # lacivert
COLOR_AXIS    = (0, 255, 255) # sarı
COLOR_TEXT_BG = (0, 0, 0)

digit_model = None
roi_model   = None

def ensure_models(digit_path: str, roi_path: str):
    global digit_model, roi_model
    if digit_model is None:
        if not Path(digit_path).exists():
            raise FileNotFoundError(f"DIGIT model dosyası yok: {digit_path}")
        digit_model = YOLO(digit_path)
    if roi_model is None:
        if not Path(roi_path).exists():
            raise FileNotFoundError(f"ROI model dosyası yok: {roi_path}")
        roi_model = YOLO(roi_path)

def natural_key(s):
    return [int(t) if t.isdigit() else t.lower() for t in re.split(r'(\d+)', s)]

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
    if not items: return []
    items = sorted(items, key=lambda d: d["conf"], reverse=True)
    keep, suppressed = [], [False]*len(items)
    for i in range(len(items)):
        if suppressed[i]: continue
        keep.append(items[i])
        for j in range(i+1, len(items)):
            if suppressed[j]: continue
            if iou_xyxy(items[i]["box"], items[j]["box"]) >= iou_thr:
                suppressed[j] = True
    return keep

def order_rois_pca(rois, prefer_top_first=True):
    if not rois:
        return [], None, None
    if len(rois) == 1:
        box = rois[0]["box"]
        c = np.array([[(box[0]+box[2])/2.0, (box[1]+box[3])/2.0]], dtype=np.float32)
        v = np.array([0.0, -1.0], dtype=np.float32)
        return rois, c.mean(axis=0), v

    centers = np.array(
        [[(r["box"][0]+r["box"][2])/2.0, (r["box"][1]+r["box"][3])/2.0] for r in rois],
        dtype=np.float32
    )
    mean = centers.mean(axis=0)
    X = centers - mean
    try:
        _, _, Vh = np.linalg.svd(X, full_matrices=False)
        v = Vh[0].astype(np.float32)
    except Exception:
        order = np.argsort(centers[:,1])
        ordered = [rois[int(i)] for i in order.tolist()]
        return ordered, mean, np.array([0.0, -1.0], dtype=np.float32)

    up = np.array([0.0, -1.0], dtype=np.float32)
    if float(np.dot(v, up)) < 0:
        v = -v

    t = X @ v
    order = np.argsort(-t) if prefer_top_first else np.argsort(t)
    ordered = [rois[int(i)] for i in order.tolist()]
    return ordered, mean, v

def draw_text(img, text, org, font=cv2.FONT_HERSHEY_SIMPLEX, font_scale=0.7,
              color=COLOR_TEXT, thickness=2, with_bg=True, bg_color=COLOR_TEXT_BG, pad=3):
    if not text: text = "-"
    x, y = int(org[0]), int(org[1])
    if with_bg:
        (tw, th), baseline = cv2.getTextSize(text, font, font_scale, thickness)
        x2, y2 = x + tw + 2*pad, y + baseline + 2*pad + int(th*0.2)
        cv2.rectangle(img, (x - pad, y - th - pad), (x2, y2), bg_color, cv2.FILLED)
    cv2.putText(img, text, (x, y), font, font_scale, (0,0,0), thickness+2, lineType=cv2.LINE_AA)
    cv2.putText(img, text, (x, y), font, font_scale, color, thickness, lineType=cv2.LINE_AA)

def kmeans1d_y(cys, k=2, iters=12):
    cys = np.asarray(cys, dtype=np.float32)
    if cys.size < k: return None
    cent = np.array([cys.min(), cys.max()], dtype=np.float32)
    for _ in range(iters):
        d0 = np.abs(cys - cent[0]); d1 = np.abs(cys - cent[1])
        assign0 = d0 <= d1
        if assign0.all() or (~assign0).all():
            return None
        cent[0] = cys[assign0].mean()
        cent[1] = cys[~assign0].mean()
    return assign0, cent

# ---- Yeni: ROI içindeki digitleri satırlara böl ve her satırı soldan sağa sırala
def group_digits_rows(digits):
    """
    Çıktı: rows -> List[List[digit_item]], üstten alta sıralı
    """
    if not digits:
        return []

    cys = [ (d["box"][1]+d["box"][3]) / 2.0 for d in digits ]
    hs  = [ (d["box"][3]-d["box"][1]) for d in digits ]
    median_h = max(1.0, float(np.median(hs)))
    use_two_rows = (len(digits) >= 6) and ((max(cys) - min(cys)) >= 0.7*median_h)
    rows = []

    if use_two_rows:
        km = kmeans1d_y(cys, k=2)
        if km is not None:
            assign0, cent = km
            sep = abs(float(cent[0] - cent[1]))
            if sep >= 0.55*median_h:
                top_is_0 = cent[0] < cent[1]
                top_idx  = [i for i,a in enumerate(assign0) if (a if top_is_0 else not a)]
                bot_idx  = [i for i,a in enumerate(assign0) if (not a if top_is_0 else a)]
                top_row = [digits[i] for i in top_idx]
                bot_row = [digits[i] for i in bot_idx]
                top_row.sort(key=lambda d: (d["box"][0] + d["box"][2]) / 2.0)
                bot_row.sort(key=lambda d: (d["box"][0] + d["box"][2]) / 2.0)
                rows = [top_row, bot_row]

    if not rows:
        # Y yakın olanları aynı satıra grupla
        order_y = sorted(range(len(digits)), key=lambda i: cys[i])
        groups, used = [], [False]*len(digits)
        row_gap = max(10.0, median_h * 0.60)
        for i in order_y:
            if used[i]: continue
            cy_ref = cys[i]; idxs = [i]; used[i] = True
            for j in order_y:
                if used[j]: continue
                if abs(cys[j] - cy_ref) <= row_gap:
                    idxs.append(j); used[j] = True
            row = [digits[k] for k in idxs]
            row.sort(key=lambda d: (d["box"][0] + d["box"][2]) / 2.0)
            groups.append(row)
        # Üstten alta sırala
        def row_avg_y(row):
            return sum((d["box"][1]+d["box"][3])/2.0 for d in row)/len(row)
        groups.sort(key=row_avg_y)
        rows = groups

    return rows

def sort_digits_line(digits):
    """Geriye metin döndürmeye devam etsin (geriye dönük kullanım için)."""
    rows = group_digits_rows(digits)
    return "|".join("".join(str(int(d["cls"])) for d in row) for row in rows)

def digits_inside_roi(roi_box, digit_items):
    rx1, ry1, rx2, ry2 = roi_box
    inside = []
    for it in digit_items:
        x1,y1,x2,y2 = it["box"]
        cx, cy = (x1+x2)/2, (y1+y2)/2
        if rx1 <= cx <= rx2 and ry1 <= cy <= ry2:
            inside.append(it)
    return inside

def predict_digits(img, conf=CONF_DIGIT, merge_iou=MERGE_IOU):
    det = digit_model.predict(img, conf=conf, verbose=False)[0]
    items = []
    if det.boxes is not None and len(det.boxes) > 0:
        for box in det.boxes:
            xyxy = box.xyxy[0].detach().cpu().numpy().tolist()
            x1,y1,x2,y2 = [float(v) for v in xyxy]
            cls  = int(box.cls[0].detach().cpu().item())
            conf = float(box.conf[0].detach().cpu().item())
            items.append({"cls": cls, "conf": conf, "box": (x1,y1,x2,y2)})
    return nms_agnostic_keep_best(items, merge_iou)

def predict_rois(img, conf=CONF_ROI, merge_iou=MERGE_IOU):
    det = roi_model.predict(img, conf=conf, verbose=False)[0]
    items = []
    if det.boxes is not None and len(det.boxes) > 0:
        for box in det.boxes:
            xyxy = box.xyxy[0].detach().cpu().numpy().tolist()
            x1,y1,x2,y2 = [float(v) for v in xyxy]
            conf = float(box.conf[0].detach().cpu().item())
            items.append({"cls": None, "conf": conf, "box": (x1,y1,x2,y2)})
    return nms_agnostic_keep_best(items, merge_iou)

def process_image_to_csv(img_path, csv_writer, output_dir=OUTPUT_DIR,
                         draw_axis=DEBUG_DRAW_AXIS, draw_index=DEBUG_DRAW_INDEX):
    img = cv2.imread(img_path)
    if img is None:
        print(f"Uyarı: Görsel okunamadı: {img_path}")
        return 0

    digit_items = predict_digits(img)
    roi_items   = predict_rois(img)
    roi_items, origin, axis_vec = order_rois_pca(roi_items, prefer_top_first=True)

    for idx, roi in enumerate(roi_items):
        rx1, ry1, rx2, ry2 = roi["box"]
        cv2.rectangle(img, (int(rx1),int(ry1)), (int(rx2),int(ry2)), COLOR_ROI_BOX, 2)

        dins = digits_inside_roi(roi["box"], digit_items)
        text = sort_digits_line(dins)  # satırları '|' ile birleştirir

        # ROI etiketi
        if draw_index:
            draw_text(img, f"#{idx}", (int(rx1), int(ry1)+22),
                      font_scale=0.7, color=COLOR_INDEX, thickness=2, with_bg=False)

        # Metni ROI üstüne yaz
        draw_text(img, text if text else "-", (int(rx1), max(15,int(ry1)-6)),
                  font_scale=0.75, color=COLOR_TEXT, thickness=2, with_bg=True)

        # CSV: image ; roi_index ; text  (ESKİ FORMAT)
        csv_writer.writerow([Path(img_path).name, idx, text if text else "-"])

    Path(output_dir).mkdir(exist_ok=True)
    out_path = Path(output_dir)/Path(img_path).name
    cv2.imwrite(str(out_path), img)
    return len(roi_items)

def run_batch(images, output_dir=OUTPUT_DIR, csv_name=CSV_NAME):
    Path(output_dir).mkdir(exist_ok=True)
    csv_path = Path(output_dir)/csv_name
    total_rows = 0
    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, delimiter=';')
        w.writerow(["image","roi_index","text"])  # eski başlık
        for p in images:
            n = process_image_to_csv(p, w, output_dir=output_dir)
            if n == 0:
                w.writerow([Path(p).name, -1, "-"])
            total_rows += max(n, 1)
    return csv_path, total_rows


def run_single(image_path, output_dir=OUTPUT_DIR, csv_name=CSV_NAME):
    Path(output_dir).mkdir(exist_ok=True)
    csv_path = Path(output_dir)/csv_name
    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, delimiter=';')
        w.writerow(["image","roi_index","text"])  # eski başlık
        n = process_image_to_csv(image_path, w, output_dir=output_dir)
        if n == 0:
            w.writerow([Path(image_path).name, -1, "-"])
    return csv_path, max(n, 1)


def csv_to_json(csv_path: Path):
    items = []
    with open(csv_path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f, delimiter=';')
        for row in reader:
            items.append({
                "image": row.get("image"),
                "roi_index": int(row.get("roi_index", "-1")),
                "line_index": int(row.get("line_index", "-1")),
                "text": row.get("text") or "-"
            })
    return items

# Opsiyonel CLI (istersen)
if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--images_dir", type=str, default=IMAGES_DIR)
    parser.add_argument("--output_dir", type=str, default=OUTPUT_DIR)
    parser.add_argument("--csv_name", type=str, default=CSV_NAME)
    parser.add_argument("--digit_weights", type=str, default=DIGIT_MODEL_PATH)
    parser.add_argument("--roi_weights", type=str, default=ROI_MODEL_PATH)
    parser.add_argument("--conf_digit", type=float, default=CONF_DIGIT)
    parser.add_argument("--conf_roi", type=float, default=CONF_ROI)
    parser.add_argument("--merge_iou", type=float, default=MERGE_IOU)
    args = parser.parse_args()

    ensure_models(args.digit_weights, args.roi_weights)
    images = sorted(glob.glob(str(Path(args.images_dir)/"*.*")), key=natural_key)
    if not images:
        print("Uyarı: Görsel bulunamadı.")
        sys.exit(0)
    csv_path, total = run_batch(images, output_dir=args.output_dir, csv_name=args.csv_name)
    print(f"CSV yazıldı: {csv_path} (toplam satır: {total})")
