# API dan build

## Build Android

JDK 17, Android SDK 35 dan Build Tools 35.0.0. Tidak menggunakan kamera internal atau WebView untuk menangkap foto.

```bash
cd android
./gradlew assembleDebug lintDebug testDebugUnitTest
```

Windows: gunakan `gradlew.bat`. APK: `android/app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions membangun APK dan menjalankan lint setiap push ke main. Artifact debug untuk pengujian, belum konfigurasi signing rilis.

## Uji server

```bash
pip install -r server/requirements-dev.txt
python -m pytest server/tests -q
```

SQLite menggunakan WAL dan parameter query; media berada pada direktori privat server dan dibaca lewat route yang memeriksa otorisasi album dan visibilitas. Set `ALPI_DATA` untuk mengganti direktori persisten, `ALPI_ADMIN_TOKEN` untuk hak operator, dan `ALPI_PUBLIC_BASE_URL` untuk URL stabil yang masuk QR.

## Endpoint

Operator: header `Authorization: Bearer <token operator>`.

| Metode / route | Fungsi |
| --- | --- |
| GET /api/health | Status server dan versi. |
| PUT /api/events/{id} | Buat/perbarui acara dengan JSON title, description, pin. ID 32 karakter hex; URL album tetap untuk ID yang sama. |
| GET /api/events | Daftar acara operator dan URL. |
| GET /api/events/{id}/qr | QR PNG berisi URL album. |
| PUT /api/events/{id}/photos/{photo_id} | Badan JPEG biner, maksimum 20 MB. `X-Captured-At` berisi waktu milidetik. ID stabil digunakan sebagai kunci idempotensi. |
| PATCH /api/events/{id}/photos/{photo_id} | JSON `visible: true/false`. |
| GET /api/albums/{album_key} | Metadata dan daftar foto terbit. Album PIN memerlukan cookie dari unlock. |
| POST /api/albums/{album_key}/unlock | JSON pin; cookie HttpOnly berumur satu hari. Perubahan PIN membatalkan cookie sebelumnya. |
| GET /media/{album_key}/{photo_id} | JPEG; query `thumbnail=true` atau `download=true` opsional. Pemeriksaan akses tetap berlaku. |
| GET /a/{album_key} | Halaman album tamu. |

QR dan album_key tidak memuat token operator atau PIN. Pembacaan file media tidak tersedia melalui static directory. Untuk deployment dengan reverse proxy, konfigurasi origin HTTPS dan header/proxy yang dipercaya secara eksplisit; jangan menerima header forwarded dari internet tanpa verifikasi proxy.

## Struktur sumber

- `android/`: APK Java native, MediaStore, SQLite, JobScheduler, foreground dataSync, Android Keystore, ImageDecoder/Canvas dan HttpURLConnection.
- `server/app.py`: API FastAPI, SQLite dan penyimpanan JPEG/thumbnail lokal.
- `server/web/`: galeri HTML/CSS/JavaScript responsif.
- `server/tests/`: autentikasi, PIN, duplikasi, moderasi, isolasi album dan unduhan.
- `MULAI_SERVER_WINDOWS.bat` / `server/run.py`: peluncur uji LAN.
- `compose.yaml`: opsi container dengan disk persisten.

Token operator tidak ditanam di kode/APK. File `.env`, foto, database, build dan keystore tidak di-commit.
