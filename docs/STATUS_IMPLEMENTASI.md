# Status implementasi 0.1.0-beta

27 Oktober 2026 · APK pengujian, belum rilis produksi.

## Tersedia

- APK Android native (Java), Android 10+, target SDK 35.
- Acara lokal, frame PNG potret/landscape, teks pita dan mode tinjau/otomatis.
- Sesi kamera bawaan menggunakan MediaStore dan notifikasi; filter folder DCIM/Camera, waktu sesi dan baseline ID.
- JobScheduler untuk pekerjaan lanjutan dan pemantauan perubahan; antrean SQLite dan pemulihan status proses/unggah yang terputus.
- EXIF diproses oleh ImageDecoder; salinan JPEG dibuat tanpa mengubah foto asli; hasil bisa dipratinjau dan dibagikan.
- Token operator dienkripsi Android Keystore; server, folder kamera dan preferensi Wi-Fi dapat diatur.
- Server mandiri FastAPI/SQLite, upload idempoten, thumbnail, PIN album, QR stabil dan moderasi visibilitas.
- Web responsif: daftar foto, pembaruan 12 detik, tampilan besar, navigasi geser, pemilihan, unduh dan bagikan tautan.
- Peluncur Windows, opsi Docker dengan named volume, panduan LAN dan arahan pemasangan HTTPS pada host publik.
- GitHub Actions untuk build APK, lint Android, unit test dan uji API.

## Verifikasi

| Pemeriksaan | Cakupan |
| --- | --- |
| assembleDebug | APK ditandatangani debug dan dapat dihasilkan dari sumber. |
| lintDebug | Pemeriksaan statis Android; nol isu pada build yang lulus. |
| 8 unit test Android/JVM | Batas sesi, foto lama, screenshot/output sendiri, waktu simpan detik, foto terlambat tersimpan, path kamera. |
| 10 test API | Token, kestabilan URL, idempotensi, thumbnail/unduh, PIN dan perubahan PIN, moderasi, isolasi acara, berkas rusak, pembatasan percobaan PIN dan QR. |
| Browser Chromium | Viewport mobile 390×844 dan desktop 1440×1000; galeri, tanpa overflow mobile, buka/geser foto, pilih dan unduh, tanpa error JavaScript pada skenario uji. |

Uji otomatis tidak membuktikan pemantauan kamera pada perangkat nyata. Xiaomi 13T/HyperOS, burst/HDR, penghemat baterai, notifikasi dan latensi jaringan perlu uji langsung. Tidak ada perangkat Android tersambung selama pembangunan ini.

## Batas implementasi awal

- Server dan domain publik belum diaktifkan. Kode server siap dijalankan lokal atau dipasang di host pilihan pemilik.
- Penyimpanan menggunakan disk server mandiri; integrasi vendor foto/cloud tertentu belum dibuat.
- Frame diimpor sebagai komposisi PNG jadi dan ditempatkan pada kanvas foto; editor posisi logo, crop dan desain frame interaktif belum tersedia.
- Tidak ada deteksi wajah, retouch AI, video, monetisasi atau sinkronisasi banyak fotografer pada beta.
- Retensi/penghapusan album otomatis dan ekspor database belum tersedia; backup direktori data atau volume server dilakukan operator.
- Mode lihat/tinjau dan antrean tersedia; jeda unggah global terpisah belum tersedia.
- Batas jendela rekonsiliasi foto adalah 24 jam setelah sesi berakhir, bukan batas umur antrean.
- Signing produksi dan pengujian perangkat merupakan langkah berikutnya sebelum penggunaan acara penting.
