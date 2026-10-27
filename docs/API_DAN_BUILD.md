# Build dan integrasi Alpi 0.2

## Build Android

JDK 17, Gradle wrapper 8.13, Android Gradle Plugin 8.9.2, compile/target SDK 35, min SDK 29.

```sh
cd android
./gradlew clean assembleDebug lintDebug testDebugUnitTest --no-daemon
```

Hasil: app/build/outputs/apk/debug/app-debug.apk. APK bertanda tangan debug untuk beta; tanda tangan dari mesin/CI berbeda dapat tidak kompatibel untuk pembaruan. Jangan menghapus instalasi lama sebelum mengekspor hasil penting. Rilis produksi memerlukan keystore yang disimpan pemilik dan proses signing tetap.

Dependensi runtime: ZXing Core 3.5.3 untuk QR lokal (Apache-2.0). Dependensi tes: JUnit 4.13.2 dan Robolectric 4.14.1. Tidak ada SDK/API Google Photos, OAuth client ID, API key atau token operator dalam APK 0.2.

GitHub Actions menjalankan assembleDebug, lintDebug dan testDebugUnitTest. Setelah seluruh pemeriksaan lulus pada push ke main, job release membuat prerelease sesuai versionName bila tag rilis belum ada. Biner rilis diberi nama Alpi-v<versi>.apk. Rilis lama tidak ditimpa oleh build ulang.

## Integrasi Android

| Tindakan | Kontrak |
| --- | --- |
| Kamera | INTENT_ACTION_STILL_IMAGE_CAMERA; Alpi tidak memiliki izin CAMERA. |
| Album | ACTION_VIEW tautan HTTPS Google Photos; package Google Photos diprioritaskan, browser sebagai cadangan. |
| Satu hasil | ACTION_SEND image/jpeg, EXTRA_STREAM, ClipData, FLAG_GRANT_READ_URI_PERMISSION. |
| Banyak hasil | ACTION_SEND_MULTIPLE, daftar URI pada EXTRA_STREAM dan ClipData, izin baca. Maksimal 30 foto dari satu acara. |
| Galeri | MediaStore Images, IS_PENDING selama penulisan, Pictures/Alpi/kode-acara, salinan app-owned. |
| QR | PNG 720×720, quiet zone 4 modul, koreksi kesalahan M, data persis tautan album. |

Google Photos mengendalikan layar penerimaan. Intent tidak memuat parameter album tersembunyi atau janji pemilihan album. Hasil aktivitas/penutupan chooser tidak dipakai sebagai bukti keberhasilan unggahan. Pengiriman selalu dipicu dari tombol operator.

Provider baca-saja Alpi membatasi URI ke dua segmen dan berkas langsung pada results, deliveries atau cache share. Direktori lain dan traversal ditolak. Salinan kiriman di files/deliveries tidak berubah saat hasil utama diproses ulang; dibersihkan setelah tujuh hari hanya untuk kelompok yang sudah selesai/dibatalkan.

## Penyimpanan dan privasi

SQLite alpi.db versi 2 menyimpan acara, jendela sesi, antrean, URI galeri dan delivery_batches. Frame/hasil berada di files privat. Tautan berbagi album disimpan pada acara; hak akses tamu mengikuti Google Photos. Backup Android dimatikan. Menghapus data/uninstall menghapus penyimpanan privat.

Tidak ada izin INTERNET atau ACCESS_NETWORK_STATE dalam APK 0.2. Proses lokal tidak bergantung pada jaringan. Google Photos dan browser memakai koneksinya sendiri untuk unggah/album.

## Riwayat server

Folder server/, compose.yaml, .env.example dan MULAI_SERVER_WINDOWS.bat adalah backend 0.1 yang dipertahankan. Kontrak API dan cara menjalankannya dapat dilihat pada [sumber versi 0.1](https://github.com/iwaka91/Alpi/tree/bb0d4ad93c5d4b0d860fee4f8bf9fa45d80e1b74). APK 0.2 tidak memakai backend ini. Tes API lama tetap dijalankan CI untuk menjaga sumber yang dipertahankan.
