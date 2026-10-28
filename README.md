# Alpi — Album Photo Live

**Versi aktif: 0.3.0-beta · Android 10+ · tanpa server atau Google Cloud Console.**

Kamera bawaan HP tetap dipakai. Selama sesi aktif, Alpi mendeteksi foto baru dari folder kamera, memberi frame PNG/pita kegiatan pada salinan, lalu menyiapkan hasil untuk dikirim berkelompok ke Google Photos. Operator menyelesaikan unggah dan menambahkan foto ke album di Google Photos.

- Kompresi per acara: Hemat, Seimbang, Detail atau kustom; ukuran asli/hasil tampil pada kartu foto.
- Generator Basic: teks, waktu dan GPS dari metadata. Pro menambah logo, teks kegiatan, sembilan posisi dan transparansi logo.
- Studio dengan pratinjau; proses ulang hasil lama dari foto asli.
- Tinjau, setujui atau lewati hasil. Mode langsung Siap Kirim tersedia per acara.
- Pilih hingga 30 foto dari satu acara untuk dibagikan sekaligus.
- Tombol Google Photos dengan menu berbagi Android sebagai cadangan.
- Ekspor hasil ke galeri `Pictures/Alpi/<kode acara>/` bila kiriman langsung tidak didukung oleh Google Photos di HP.
- Tautan berbagi album per acara, tombol buka album, QR PNG lokal dan berbagi tautan.
- Status kiriman menunggu konfirmasi; hanya operator yang dapat menandai selesai setelah memeriksa album.
- Proses frame, antrean dan QR berjalan lokal. APK tidak memiliki izin internet.

**Tautan album hanya untuk membuka album dan QR, bukan alamat unggah.** Membuka Google Photos tidak membuktikan bahwa foto sudah terunggah atau masuk album. Tampilan penerimaan foto bergantung pada versi Google Photos. Cadangan folder juga tidak otomatis memasukkan hasil ke album tertentu.

## Unduh dan mulai

- [APK 0.3.0-beta](https://github.com/iwaka91/Alpi/releases/download/v0.3.0-beta/Alpi-v0.3.0-beta.apk)
- [Panduan pemasangan dan alur Google Photos](docs/PANDUAN_UJI_BETA.md)
- [Perubahan 0.3.0](docs/RELEASE_NOTES_v0.3.0-beta.md)
- [Panduan Studio frame dan kompresi](docs/STUDIO_FRAME_DAN_KOMPRESI.md)
- [Status implementasi dan batas pengujian](docs/STATUS_IMPLEMENTASI.md)
- [Build, penyimpanan dan integrasi Android](docs/API_DAN_BUILD.md)
- [Spesifikasi produk](docs/SPESIFIKASI_PRODUK.md), [arsitektur](docs/ARSITEKTUR_ALUR.md), [rencana uji perangkat](docs/RENCANA_UJI.md)

## Pengembangan

```sh
cd android
./gradlew assembleDebug lintDebug testDebugUnitTest
```

JDK 17 dan Android SDK 35 diperlukan. QR memakai ZXing Core 3.5.3 (Apache-2.0). Metadata dibaca dengan AndroidX ExifInterface 1.4.2. Robolectric dipakai untuk pengujian Android/JVM. Workflow GitHub menjalankan build, lint dan tes sebelum membuat prerelease APK beta.

Folder `server/`, `compose.yaml` dan peluncur Windows merupakan implementasi **versi 0.1** yang dipertahankan sebagai riwayat teknis. APK 0.3 tidak memakainya dan tidak menyediakan pengaturan server, token operator atau PIN album sendiri.

Ini APK beta bertanda tangan debug. Uji kamera, pengelolaan baterai HyperOS dan alur penerimaan Google Photos pada perangkat nyata tetap diperlukan sebelum dipakai pada acara penting.
