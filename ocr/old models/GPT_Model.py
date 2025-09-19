# offline_pipeline_nms_pca_v2_cpu.py
import os, csv, glob, re, json, math, argparse
from pathlib import Path
import cv2
import numpy as np
from ultralytics import YOLO

# ================== RENKLER (BGR) ==================
COLOR_ROI      = (0, 255, 0)     # ROI kutuları: yeşil
COLOR_DIGIT    = (0, 0, 255)     # Digit kutuları: kırmızı
COLOR_INDEX    = (128, 0, 0)     # ROI index yazısı: koyu lacivert ton
COLOR_TEXT     = (0, 0, 255)     # ROI başlığı/okunan metin
COLOR_AXIS     = (0, 255, 255)   # PCA ekseni: sarı

# ================== KÜÇÜK ARAÇLAR ==================
def natural_key(s):
    return [int(t) if t.isdigit() else t.lower() for t in re.split(r'(\d+)', s)]

def iou_xyxy(a, b):
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

def diou_xyxy(a, b):
    # DIoU benzeri skor (IoU - merkez uzaklığı / dış çerçeve çapı)
    ax1, ay1, ax2, ay2 = a; bx1, by1, bx2, by2 = b
    # IoU
    ix1, iy1 = max(ax1,bx1), max(ay1,by1)
    ix2, iy2 = min(ax2,bx2), min(ay2,by2)
    inter = max(0.0, ix2-ix1) * max(0.0, iy2-iy1)
    a_area = max(0.0, ax2-ax1) * max(0.0, ay2-ay1)
    b_area = max(0.0, bx2-bx1) * max(0.0, by2-by1)
    union = a_area + b_area - inter if (a_area+b_area-inter)>0 else 1e-6
    iou = inter / union

    # En küçük kapsayan kutu ve merkez uzaklığı
    cx_a, cy_a = (ax1+ax2)/2, (ay1+ay2)/2
    cx_b, cy_b = (bx1+bx2)/2, (by1+by2)/2
    cw1, ch1 = min(ax1,bx1), min(ay1,by1)
    cw2, ch2 = max(ax2,bx2), max(ay2,by2)
    c2 = (cw2-cw1)**2 + (ch2-ch1)**2 + 1e-6
    rho2 = (cx_a-cx_b)**2 + (cy_a-cy_b)**2
    return iou - rho2/c2

def nms_class_aware_keep_best(items, iou_thr=0.35, use_diou=False):
    """
    Digitler için sınıf-bilinçli NMS. Aynı sınıflar daha sert bastırılır.
    use_diou=True -> DIoU benzeri skor ile karar.
    """
    if not items: 
        return []
    items = sorted(items, key=lambda d: d["conf"], reverse=True)
    keep = []
    suppressed = [False] * len(items)
    for i in range(len(items)):
        if suppressed[i]: 
            continue
        keep.append(items[i])
        for j in range(i+1, len(items)):
            if suppressed[j]: 
                continue
            same_cls = (items[i]["cls"] == items[j]["cls"])
            thr = iou_thr if same_cls else (iou_thr * 0.8)  # farklı sınıfa daha toleranslı
            score = diou_xyxy(items[i]["box"], items[j]["box"]) if use_diou else iou_xyxy(items[i]["box"], items[j]["box"])
            if score >= thr:
                suppressed[j] = True
    return keep

def nms_agnostic_keep_best(items, iou_thr=0.35):
    """ROI için sınıf agnostik NMS."""
    if not items: 
        return []
    items = sorted(items, key=lambda d: d["conf"], reverse=True)
    keep, suppressed = [], [False] * len(items)
    for i in range(len(items)):
        if suppressed[i]: 
            continue
        keep.append(items[i])
        for j in range(i+1, len(items)):
            if suppressed[j]: 
                continue
            if iou_xyxy(items[i]["box"], items[j]["box"]) >= iou_thr:
                suppressed[j] = True
    return keep

def order_rois_pca(rois, prefer_top_first=True):
    """
    ROI merkezlerini PCA ana eksenine projekte eder ve o eksende sıralar.
    prefer_top_first=True -> en yukarıdaki önce gelir (y küçük).
    """
    if not rois:
        return [], None, None
    if len(rois) == 1:
        box = rois[0]["box"]
        c = np.array([[(box[0]+box[2])/2.0, (box[1]+box[3])/2.0]], dtype=np.float32)
        v = np.array([0.0, -1.0], dtype=np.float32)  # yukarı
        return rois, c.mean(axis=0), v

    centers = np.array(
        [[(r["box"][0]+r["box"][2])/2.0, (r["box"][1]+r["box"][3])/2.0] for r in rois],
        dtype=np.float32
    )
    mean = centers.mean(axis=0)
    X = centers - mean
    _, _, Vh = np.linalg.svd(X, full_matrices=False)
    v = Vh[0].astype(np.float32)  # ana yön

    # Tercih edilen yön: yukarı
    if prefer_top_first:
        if v[1] > 0:  # aşağı bakıyorsa çevir
            v = -v
    else:
        if v[1] < 0:
            v = -v

    t = X @ v
    order = np.argsort(-t) if prefer_top_first else np.argsort(t)
    ordered = [rois[i] for i in order.tolist()]
    return ordered, mean, v

def draw_axis(img, origin, vec, scale=120):
    if img is None or origin is None or vec is None:
        return
    ox, oy = float(origin[0]), float(origin[1])
    ex, ey = ox + float(vec[0]) * scale, oy + float(vec[1]) * scale
    p0 = (int(round(ox)), int(round(oy)))
    p1 = (int(round(ex)), int(round(ey)))
    cv2.arrowedLine(img, p0, p1, COLOR_AXIS, 2, tipLength=0.25)
    cv2.putText(img, "PCA axis", (p0[0]+5, p0[1]-5), cv2.FONT_HERSHEY_SIMPLEX, 0.6, COLOR_AXIS, 2)

def rotate_points(points, origin, vec):
    v = vec / (np.linalg.norm(vec) + 1e-8)
    vx, vy = v[0], v[1]
    R = np.array([[vx, -vy],
                  [vy,  vx]], dtype=np.float32)
    return (points - origin) @ R.T  # (N,2)

def box_inside_ratio(inner, outer):
    ix1, iy1, ix2, iy2 = inner; ox1, oy1, ox2, oy2 = outer
    ax = max(0, min(ix2,ox2)-max(ix1,ox1))
    ay = max(0, min(iy2,oy2)-max(iy1,oy1))
    inter = ax*ay
    area = max(1.0, (ix2-ix1)*(iy2-iy1))
    return inter/area

def digits_inside_roi(roi_box, digit_items, min_inside_ratio=0.5):
    inside = []
    for it in digit_items:
        if box_inside_ratio(it["box"], roi_box) >= min_inside_ratio:
            inside.append(it)
    return inside

def digit_quality_filter(items, min_w=6, min_h=8, min_conf=0.05):
    out=[]
    for it in items:
        x1,y1,x2,y2 = it["box"]
        if (x2-x1) >= min_w and (y2-y1) >= min_h and it["conf"] >= min_conf:
            out.append(it)
    return out

def safe_digit(ch, conf, thr=0.25):
    return ch if conf >= thr else "?"

def sort_digits_line_angle_aware(digits, fallback_origin=None, fallback_axis=None,
                                 unknown_thr=0.25):
    """
    Digit merkezlerinden kendi PCA eksenini hesaplar; y-eksenine göre satır gruplar,
    x-eksenine göre soldan-sağa sıralar. 1-2 digit varsa fallback ekseni kullanır.
    """
    if not digits:
        return "", 0, 0.0

    centers = np.array(
        [[(d["box"][0]+d["box"][2])/2.0, (d["box"][1]+d["box"][3])/2.0] for d in digits],
        dtype=np.float32
    )
    if len(digits) >= 2:
        mean = centers.mean(axis=0)
        X = centers - mean
        _, _, Vh = np.linalg.svd(X, full_matrices=False)
        axis = Vh[0].astype(np.float32)
        # Tercih edilen yön: soldan sağa eksen (x-ekseni)
        if axis[0] < 0:  # sola bakıyorsa çevir
            axis = -axis
        origin = mean
    else:
        # Fallback eksen
        origin = fallback_origin if fallback_origin is not None else np.array([0,0], np.float32)
        axis   = fallback_axis   if fallback_axis   is not None else np.array([1,0], np.float32)

    rc = rotate_points(centers, origin.astype(np.float32), axis.astype(np.float32))
    ys = rc[:,1]; xs = rc[:,0]
    hs = np.array([(d["box"][3]-d["box"][1]) for d in digits], dtype=np.float32)
    row_gap = max(10.0, float(np.median(hs))*0.6)

    used = np.zeros(len(digits), bool)
    lines = []
    # üstten alta
    for i in np.argsort(ys):
        if used[i]: 
            continue
        row_idx = [i]
        used[i] = True
        # yakın y'ler aynı satır
        for j in np.argsort(np.abs(ys - ys[i])):
            if used[j]: 
                continue
            if abs(ys[j] - ys[i]) <= row_gap:
                row_idx.append(j); used[j] = True
        # satır içi soldan-sağa
        row_idx.sort(key=lambda k: xs[k])
        line = "".join(safe_digit(str(int(digits[k]["cls"])), digits[k]["conf"], thr=unknown_thr)
                       for k in row_idx)
        lines.append(line)

    text = "|".join(lines)  # Çok satır: pipe ile ayır
    # Özet istatistik
    mean_conf = float(np.mean([d["conf"] for d in digits])) if digits else 0.0
    return text, len(digits), mean_conf

def fallback_roi_if_missing(digit_items, pad=10):
    if not digit_items: 
        return []
    xs = [d["box"][0] for d in digit_items]+[d["box"][2] for d in digit_items]
    ys = [d["box"][1] for d in digit_items]+[d["box"][3] for d in digit_items]
    x1,y1,x2,y2 = max(0,min(xs)-pad), max(0,min(ys)-pad), max(xs)+pad, max(ys)+pad
    return [{"cls":None,"conf":1.0,"box":(x1,y1,x2,y2)}]

def draw_roi_box(img, box, idx=None, text=None):
    x1,y1,x2,y2 = map(int, box)
    cv2.rectangle(img, (x1,y1), (x2,y2), COLOR_ROI, 2)
    if text is not None:
        cv2.putText(img, text, (x1, max(0, y1-5)), cv2.FONT_HERSHEY_SIMPLEX, 0.7, COLOR_TEXT, 2)
    if idx is not None:
        cv2.putText(img, f"#{idx}", (x1, y1+20), cv2.FONT_HERSHEY_SIMPLEX, 0.7, COLOR_INDEX, 2)

def draw_digit_box(img, item):
    x1,y1,x2,y2 = map(int, item["box"])
    cv2.rectangle(img, (x1,y1), (x2,y2), COLOR_DIGIT, 1)
    cv2.putText(img, f'{int(item["cls"])} {item["conf"]:.2f}', (x1, max(0, y1-3)),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, COLOR_DIGIT, 1)

def dump_json(out_dir, stem, roi_items, digit_items, texts_per_roi):
    j = {
        "rois": roi_items,
        "digits": digit_items,
        "texts_per_roi": texts_per_roi
    }
    with open(str(Path(out_dir)/f"{stem}.json"), "w", encoding="utf-8") as f:
        json.dump(j, f, ensure_ascii=False, indent=2)

# ================== ANA İŞLEV ==================
def process_image(img_path, args, digit_model, roi_model, writer):
    img = cv2.imread(img_path)
    if img is None:
        return 0

    # 1) DIGIT tespiti (ham)
    det_digits = digit_model.predict(img, conf=args.conf_digit, imgsz=args.imgsz,
                                     device="cpu", verbose=False)[0]
    raw_digits = []
    if det_digits.boxes is not None:
        for box in det_digits.boxes:
            x1,y1,x2,y2 = [float(v) for v in box.xyxy[0].tolist()]
            cls  = int(box.cls[0].item())
            conf = float(box.conf[0].item())
            raw_digits.append({"cls": cls, "conf": conf, "box": (x1,y1,x2,y2)})

    # Kalite filtresi
    raw_digits = digit_quality_filter(raw_digits, args.min_digit_w, args.min_digit_h, args.min_digit_conf)

    # 1b) DIGIT NMS (sınıf-bilinçli, opsiyonel DIoU)
    digit_items = nms_class_aware_keep_best(raw_digits, iou_thr=args.merge_iou, use_diou=args.use_diou)

    # 2) ROI tespiti (ham)
    det_rois = roi_model.predict(img, conf=args.conf_roi, imgsz=args.imgsz,
                                 device="cpu", verbose=False)[0]
    raw_rois = []
    if det_rois.boxes is not None:
        for box in det_rois.boxes:
            x1,y1,x2,y2 = [float(v) for v in box.xyxy[0].tolist()]
            conf = float(box.conf[0].item())
            raw_rois.append({"cls": None, "conf": conf, "box": (x1,y1,x2,y2)})

    # 2b) ROI NMS + PCA sıralama
    roi_items = nms_agnostic_keep_best(raw_rois, args.merge_iou)
    if not roi_items and args.enable_fallback_roi:
        roi_items = fallback_roi_if_missing(digit_items, pad=args.fallback_pad)

    roi_items, roi_origin, roi_axis = order_rois_pca(roi_items, prefer_top_first=True)

    wrote_rows = 0
    texts_per_roi = []

    # Debug görseller (isteğe bağlı)
    stem = Path(img_path).stem
    out_dir = Path(args.output_dir)
    roi_dbg  = img.copy() if args.save_debug_rois or args.draw_pca_axis else None
    dig_dbg  = img.copy() if args.save_debug_digits else None
    blend_dbg= img.copy() if args.save_debug_combined else None

    if args.draw_pca_axis and roi_dbg is not None:
        draw_axis(roi_dbg, roi_origin, roi_axis, scale=180)

    # 3) Her ROI için: digitleri sırala, çiz ve CSV'ye yaz
    for idx, roi in enumerate(roi_items):
        dins = digits_inside_roi(roi["box"], digit_items, min_inside_ratio=args.inside_ratio)
        text, dcount, mean_conf = sort_digits_line_angle_aware(
            dins, fallback_origin=roi_origin, fallback_axis=roi_axis, unknown_thr=args.unknown_conf
        )
        label_text = text if text else "-"

        # Çizimler
        if roi_dbg is not None:
            draw_roi_box(roi_dbg, roi["box"], idx=idx, text=label_text)
        if blend_dbg is not None:
            draw_roi_box(blend_dbg, roi["box"], idx=idx, text=label_text)
        if dig_dbg is not None:
            for d in dins:
                draw_digit_box(dig_dbg, d)
        if blend_dbg is not None:
            for d in dins:
                draw_digit_box(blend_dbg, d)

        # CSV
        writer.writerow([Path(img_path).name, idx, label_text, dcount, f"{mean_conf:.4f}"])
        wrote_rows += 1
        texts_per_roi.append({"roi_index": idx, "text": label_text, "digit_count": dcount, "mean_conf": mean_conf})

    # Eğer hiçbir ROI yazılmadıysa "-":
    if wrote_rows == 0:
        writer.writerow([Path(img_path).name, -1, "-", 0, "0.0000"])

    # Kaydetmeler
    out_dir.mkdir(parents=True, exist_ok=True)
    if args.save_debug_rois and roi_dbg is not None:
        cv2.imwrite(str(out_dir / f"{stem}_rois.png"), roi_dbg)
    if args.save_debug_digits and dig_dbg is not None:
        cv2.imwrite(str(out_dir / f"{stem}_digits.png"), dig_dbg)
    if args.save_debug_combined and blend_dbg is not None:
        cv2.imwrite(str(out_dir / f"{stem}.png"), blend_dbg)  # birleşik görseli ana isimde tut

    if args.save_json:
        dump_json(out_dir, stem, roi_items, digit_items, texts_per_roi)

    return wrote_rows

# ================== MAIN ==================
def main():
    ap = argparse.ArgumentParser(description="CPU OCR pipeline: ROI + DIGIT (PCA/NMS/JSON/Debug)")
    ap.add_argument("--images_dir", type=str, default="images/test")
    ap.add_argument("--output_dir", type=str, default="outputs")
    ap.add_argument("--csv_name",   type=str, default="results.csv")
    ap.add_argument("--digit_model", type=str, default="weights/digit2.pt")
    ap.add_argument("--roi_model",   type=str, default="weights/roi_best.pt")
    ap.add_argument("--imgsz", type=int, default=960)
    ap.add_argument("--conf_digit", type=float, default=0.10)
    ap.add_argument("--conf_roi",   type=float, default=0.60)
    ap.add_argument("--merge_iou",  type=float, default=0.35)
    ap.add_argument("--use_diou",   action="store_true", help="Digit NMS'te DIoU skoru kullan")
    ap.add_argument("--min_digit_w", type=int, default=6)
    ap.add_argument("--min_digit_h", type=int, default=8)
    ap.add_argument("--min_digit_conf", type=float, default=0.05)
    ap.add_argument("--inside_ratio", type=float, default=0.50, help="Digit kutusunun ROI içinde kalma oranı eşiği")
    ap.add_argument("--unknown_conf", type=float, default=0.25, help="Bu confidence altında '?' yaz")
    ap.add_argument("--enable_fallback_roi", action="store_true", help="ROI bulunamazsa digitlerden pseudo-ROI üret")
    ap.add_argument("--fallback_pad", type=int, default=10)
    ap.add_argument("--draw_pca_axis", action="store_true", help="ROI PCA eksenini çiz")
    ap.add_argument("--save_debug_rois", action="store_true", help="Sadece ROI overlay görseli kaydet (…_rois.png)")
    ap.add_argument("--save_debug_digits", action="store_true", help="Sadece digit overlay görseli kaydet (…_digits.png)")
    ap.add_argument("--save_debug_combined", action="store_true", help="Hem ROI hem digit overlay (… .png)")
    ap.add_argument("--save_json", action="store_true", help="Per-image JSON dump kaydet")
    args = ap.parse_args()

    # Model yükleme (CPU)
    digit_model = YOLO(args.digit_model)
    roi_model   = YOLO(args.roi_model)

    # Görselleri sırala
    patterns = ("*.jpg","*.jpeg","*.png","*.bmp","*.JPG","*.JPEG","*.PNG","*.webp","*.tif","*.tiff")
    images = []
    for patt in patterns:
        images += glob.glob(os.path.join(args.images_dir, patt))
    images = list(dict.fromkeys(images))
    images.sort(key=natural_key)
    if not images:
        print("Görsel bulunamadı.")
        return

    out_dir = Path(args.output_dir); out_dir.mkdir(parents=True, exist_ok=True)
    csv_path = out_dir / args.csv_name
    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f, delimiter=';')  # TR Excel uyumlu
        w.writerow(["image","roi_index","text","digit_count","mean_conf"])
        for idx, p in enumerate(images, 1):
            n = process_image(p, args, digit_model, roi_model, w)
            if n == 0:
                # bir satır da yazılmış oldu process içinde; ekstra bir şey yapma
                pass
            if idx % 10 == 0 or idx == len(images):
                print(f"[{idx}/{len(images)}] işlendi: {Path(p).name}")

    print("CSV hazır ->", csv_path)

if __name__ == "__main__":
    main()
