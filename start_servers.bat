@echo off
setlocal

rem ====================================================================
rem   Spring Boot + Python OCR Server - Basit Başlatıcı
rem ====================================================================
rem Bu betik, sunucuları başlatmak için aşağıdaki yapıyı varsayar:
rem 1. 'backend' klasöründe 'mvnw.cmd' dosyası mevcuttur.
rem 2. 'ocr' klasöründe '.venv' adında bir sanal ortam mevcuttur.
rem 3. Gerekli Python paketleri (requirements.txt) zaten kuruludur.
rem ====================================================================

set "PROJECT_ROOT=%~dp0"

echo.
echo [1/2] Spring Boot sunucusu baslatiliyor...
start "Spring Boot Backend" cmd /k "cd /d "%PROJECT_ROOT%backend" && echo Spring Boot sunucusu baslatiliyor... && mvnw.cmd spring-boot:run"

echo.
echo [2/2] Python OCR sunucusu baslatiliyor...
start "Python OCR Server" cmd /k "cd /d "%PROJECT_ROOT%ocr" && echo Sanal ortam aktive ediliyor ve Python sunucusu baslatiliyor... && .\.venv\Scripts\activate && python server.py"

echo.
echo Sunucular yeni pencerelerde baslatildi.
echo Hata olup olmadigini lutfen acilan pencerelerden kontrol edin.
pause
endlocal
