# server.py
import os, uuid, time
from pathlib import Path
from fastapi import FastAPI, UploadFile, File, HTTPException, Header
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import uvicorn

from OCR import (
    ensure_models, run_single, csv_to_json,
    DIGIT_MODEL_PATH, ROI_MODEL_PATH
)

API_TOKEN = os.getenv("API_TOKEN")  # optional: basit bearer kontrol
OUTPUT_ROOT = Path(os.getenv("OUTPUT_ROOT", "outputs_jobs"))
OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)

# modelleri hazırla (ilk istekten önce de yükleyebiliriz)
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

@app.get("/health")
def health():
    return {"ok": True}


@app.post("/process")
async def process_image(
    file: UploadFile = File(...),
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