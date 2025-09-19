# production-tracker-ocr
A comprehensive platform that combines a Spring Boot-based web application for managing production data with a Python-YOLO pipeline for automatic slab number recognition and database integration. It unifies production management and computer vision in one system.

# Usage (English)

To test the OCR system, move the images you want from the "/images" folder to the "/images/test" folder and run the OCR.py file. The outputs will be created in the "/outputs" directory.

To use it via the web application, run the server.py file and then the visual data you will upload via the web application will be processed. The output of each process will be created in the "/outputs_job" directory with a different "jobID" and will be reflected in the web application.

There must be at least one ADMIN in the system. After creating the first user, do not forget to authorize the user from the database as USER > ADMIN.

# Kullanım (Türkçe)

OCR sisteminin çalışmasını test etmek için "/images" klasörü içerisinden istediğiniz resimleri "/images/test" klasörüne taşıyın ve OCR.py dosyasını çalıştırın. Çıktılar "/outputs" dizininde oluşacaktır.

Web uygulaması üzerinden kullanmak için server.py dosyasını çalıştırın ve ardından web uygulaması üzerinden yükleyeceğiniz görsel verileri işlenecektir. Her işlemin çıktısı farkı bir "jobID" ile "/outputs_job" dizininde oluşacak ve web uygulamasına yansıyacaktır.

Sistemde en az bir adet ADMIN bulunması gerekmektedir. Bu yetkilendirmeyi ilk kullanıcıyı oluşturduktan sonra veri tabanı üzerinden yetkiyi USER > ADMIN yapmayı unutmayın.

# English Description
# Production Data Tracking System (Spring Boot) + Slab OCR Pipeline (Python/YOLOv8s)

This project was developed during an internship at **Isdemir**.  
It enables the management of slab numbers on the production line through both **OCR (YOLOv8s-based)** and **manual entry**.  
The backend uses **Spring Boot (Java 24, MySQL 8.x)**, while the OCR pipeline is built with **Python + Ultralytics YOLOv8s**.

---

## Features
- **Backend (Spring Boot)**  
  - Product registration, update, and deletion  
  - Role-based security (ADMIN / OPERATOR / USER)  
  - Database structure with triggers on MySQL 8.x  
  - Web UI built with Thymeleaf + DataTables

- **OCR Pipeline (Python)**  
  - YOLOv8s for ROI and digit detection  
  - PCA-based line/column ordering, NMS for merging boxes  
  - Outputs stored under `ocr/outputs_jobs/` **(ignored by Git)**  
  - Test images located in `ocr/images/test/` and can be processed via `OCR.py`  
  - Visual outputs: ROI boxes in **green**, digits in **red**, ROI indices in **navy blue**

- **Integration**  
  - `/api/slabs/recognize` endpoint accepts images  
  - OCR results are processed as CSV and inserted into the database  
  - Manual entry is also supported via the web interface  

---


## Setup

### Requirements
- Java 24 + Maven  
- MySQL 8.x  
- Python 3.10+  
- (Optional) Git LFS – if model weights exceed 100 MB  

### Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run
```
Runs on [http://localhost:8080](http://localhost:8080) by default.

### OCR (Python)
```bash
cd ocr
python -m venv .venv
.\.venv\Scripts\activate   # Windows
pip install -r requirements.txt
python OCR.py --images "images/test" --out "outputs_jobs" --digit-weights "weights/digit.pt" --roi-weights "weights/roi.pt" --cpu
```

---

## Outputs
- Processed images: stored under `ocr/outputs_jobs/` (ignored by Git).  
- Test images: located in `ocr/images/test/`, usable with `OCR.py`.  
- CSV results are saved in the project root or under `outputs_jobs/`.  

**Example CSV row:**
```
image_name,roi_index,raw_text,conf,x1,y1,x2,y2
frame001.jpg,#0,129456,0.87,12,34,345,210
```

---

## Security
- Role-based access:  
  - **ADMIN** → full privileges  
  - **OPERATOR** → production records/operations  
  - **USER** → read-only (save buttons disabled)  
- CSRF protection with a custom `AccessDeniedHandler`  
- Toast.js notifications for errors/permission handling  

---



# Türkçe Açıklama
# Üretim Verileri Takip Sistemi (Spring Boot) + Slab OCR Pipeline (Python/YOLOv8s)

Isdemir stajı kapsamında geliştirilen bu proje, üretim hattındaki slab numaralarının hem **OCR (YOLOv8s tabanlı)** hem de **manuel giriş** yoluyla kaydedilip yönetilmesini sağlar.  
Backend tarafında **Spring Boot (Java 24, MySQL 8.x)** kullanılırken, OCR tarafında **Python + Ultralytics YOLOv8s** ile görüntü işleme yapılır.

---

## Özellikler
- **Backend (Spring Boot)**  
  - Ürün kayıt, güncelleme ve silme  
  - Rol tabanlı güvenlik (ADMIN / OPERATOR / USER)  
  - MySQL 8.x üzerinde tablolar ve trigger destekli yapılar  
  - Web arayüzü: Thymeleaf + DataTables

- **OCR Pipeline (Python)**  
  - YOLOv8s tabanlı ROI ve digit tespiti  
  - PCA tabanlı satır–kolon sıralama, NMS ile kutu birleştirme  
  - Çıktılar `ocr/outputs_jobs/` altında **(gitignore’da, repoya yüklenmez)**  
  - Test görselleri `ocr/images/test/` içinde, `OCR.py` üzerinden çalıştırılabilir  
  - Görseller üzerinde ROI kutuları **yeşil**, rakamlar **kırmızı**, ROI indeksleri **lacivert**

- **Entegrasyon**  
  - `/api/slabs/recognize` uç noktası üzerinden görsel gönderilebilir  
  - OCR sonucu CSV formatında işlenip veritabanına kaydedilir  
  - Manuel veri girişi de web arayüzü üzerinden yapılabilir  

## Kurulum

### Gereksinimler
- Java 24 + Maven
- MySQL 8.x
- Python 3.10+

### Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run
```
Sunucu varsayılan olarak [http://localhost:8080](http://localhost:8080) üzerinde çalışır.

### OCR (Python)
```bash
cd ocr
python -m venv .venv
.\.venv\Scripts\activate   # Windows
pip install -r requirements.txt
python OCR.py --images "images/test" --out "outputs_jobs" --digit-weights "weights/digit.pt" --roi-weights "weights/roi.pt" --cpu
```

---

## Çıktılar
- İşlenen görseller: `ocr/outputs_jobs/` altında tutulur (gitignore’da → GitHub’a yüklenmez).  
- Test amaçlı görseller: `ocr/images/test/` altında, `OCR.py` ile kullanılabilir.  
- CSV sonuçları proje kökünde veya `outputs_jobs/` içinde üretilir.  

**Örnek CSV satırı:**
```
image_name,roi_index,raw_text,conf,x1,y1,x2,y2
frame001.jpg,#0,129456,0.87,12,34,345,210
```

---

## Güvenlik
- Rol tabanlı erişim:  
  - **ADMIN** → tüm yetkiler  
  - **OPERATOR** → üretim kayıt/işlemleri  
  - **USER** → salt okunur (ör. kaydet butonları pasif)  
- CSRF koruması, özel `AccessDeniedHandler`  
- Toast.js bildirimleri ile hata/yetki durumları gösterilir  

---
