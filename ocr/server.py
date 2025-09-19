# server.py
import os, uuid, time
from pathlib import Path
from fastapi import FastAPI, UploadFile, File, HTTPException, Header, Depends, Security
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.security.api_key import APIKeyHeader
import uvicorn

from OCR import (
    ensure_models, run_single, csv_to_json,
    DIGIT_MODEL_PATH, ROI_MODEL_PATH # These are now absolute paths from OCR.py
)

# --- Path Definitions ---
# Get the directory where this script is located to make paths robust.
SCRIPT_DIR = Path(__file__).resolve().parent

API_TOKEN = os.getenv("API_TOKEN")
if not API_TOKEN:
    # Geliştirme ortamında token olmadan çalışabilmesi için uyarı veriyoruz.
    print("UYARI: API_TOKEN ortam değişkeni ayarlanmamış. Servis korumasız çalışacak.")

# The output directory should also be relative to the script's location.
OUTPUT_ROOT = Path(os.getenv("OUTPUT_ROOT", SCRIPT_DIR / "outputs_jobs"))
OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)

# Modelleri hazırla. OCR.py'dan gelen yollar artık mutlak (absolute) olduğu için
# bu kodun nereden çalıştırıldığı fark etmez.
ensure_models(
    os.getenv("DIGIT_MODEL_PATH", DIGIT_MODEL_PATH),
    os.getenv("ROI_MODEL_PATH", ROI_MODEL_PATH)
)

app = FastAPI(title="Slab OCR Service", version="1.0.0")

# CORS (gerekirse domainini ekle)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], allow_credentials=True,
    allow_methods=["*"], allow_headers=["*"],
)

API_KEY_NAME = "X-API-Token"
api_key_header = APIKeyHeader(name=API_KEY_NAME, auto_error=True)

async def get_api_key(api_key: str = Security(api_key_header)):
    if api_key == API_TOKEN:
        return api_key
    raise HTTPException(status_code=403, detail="Geçersiz veya eksik API Anahtarı")

@app.get("/health")
def health():
    return {"ok": True}


@app.post("/process")
async def process_image(
    file: UploadFile = File(...),
    # Bu bağımlılık sadece API_TOKEN ortam değişkeni ayarlıysa aktif olur.
    api_key: str = Depends(get_api_key) if API_TOKEN else None,
):
    try:
        # job klasörü
        job_id = f"{int(time.time())}_{uuid.uuid4().hex[:8]}"
        job_dir = OUTPUT_ROOT / job_id
        job_dir.mkdir(parents=True, exist_ok=True)

        # dosyayı kaydet
        in_path = job_dir / file.filename
        with in_path.open("wb") as f:
            f.write(await file.read())

        # pipeline çalıştır → job_dir/results.csv + debug görseli
        csv_path, rows = run_single(str(in_path), output_dir=str(job_dir), csv_name="results.csv")

        # CSV → JSON
        results = csv_to_json(Path(csv_path))

        # Spring için sade bir payload
        payload = {
            "job_id": job_id,
            "source_image": file.filename,
            "results_csv": str(Path(csv_path).absolute()),
            "saved_image": str((job_dir / file.filename).absolute()),
            "count": rows,
            "items": results
        }
        return JSONResponse(payload)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run("server:app", host="0.0.0.0", port=8000, reload=False)
# server.py