# Status implementasi Alpi 0.3.0-beta

## Tersedia

- APK operator native: kamera bawaan, sesi MediaStore, antrean persisten, tinjau/setujui/lewati, proses ulang dan kiriman berkelompok.
- Kompresi per acara: Hemat, Seimbang, Detail, kustom 1280–3200 px/JPEG 60–95, tanpa memperbesar foto kecil. Ukuran sumber/hasil, dimensi dan penghematan terukur ditampilkan pada kartu foto.
- Generator Basic: teks utama, pilihan waktu/GPS metadata, pita bawah hijau/putih/hitam.
- Generator Pro: semua Basic, satu logo, teks kegiatan, sembilan posisi teks/logo, ukuran, transparansi logo dan latar pita opsional. Rasio logo dijaga; irisan dengan teks ditangani.
- Studio dengan pratinjau, foto uji, ilustrasi bertanda contoh, draf yang bertahan saat rotasi dan penyimpanan eksplisit.
- Frame PNG/pita lama tetap tersedia sebagai mode tersendiri. Proses ulang kelompok membaca sumber asli dan memakai konfigurasi terbaru, tanpa menumpuk frame.
- Metadata EXIF/DATE_TAKEN dan GPS foto. Izin metadata lokasi diminta saat dipilih. Tidak memakai lokasi HP saat ini, geocoding, data palsu atau jam proses sebagai waktu foto.
- Acara, tautan Google Photos, buka album, QR lokal dan berbagi tautan. Kiriman maksimal 30 foto dari satu acara, snapshot JPEG tetap, konfirmasi manual dan ekspor Pictures/Alpi.
- Migrasi database 1→3 dan 2→3 mempertahankan frame, hasil, URL album serta kiriman yang belum dikonfirmasi. Kompresi lama dipertahankan sampai diedit.
- APK tidak memiliki izin internet; tidak membutuhkan server, OAuth atau Google Cloud Console.

## Verifikasi

Pemeriksaan rilis menjalankan assembleDebug, lintDebug dan testDebugUnitTest. Tes Android/JVM mencakup kebijakan sesi, tautan, kelompok kiriman, pembatalan/persistensi, QR, migrasi 0.1/0.2, kompresi JPEG sungguhan dengan grafis native, keutuhan foto asli, metadata valid/hilang, tanggal EXIF hasil, transparansi logo, serta 81 kombinasi posisi teks/logo pada masing-masing orientasi potret dan landscape. Tes antarmuka memeriksa pilihan setelah rotasi, draf studio, penyimpanan dan preset kompresi. Hasil rilis tersedia pada [GitHub Actions](https://github.com/iwaka91/Alpi/actions).

Pratinjau studio pada ukuran 390×844 dirender dan diperiksa. Pengujian berlangsung pada Android simulasi/JVM, bukan pada HP fisik. Tes backend lama tetap dijalankan CI untuk menjaga kode server 0.1 yang dipertahankan dalam repo.

Pada uji operator versi 0.2, pita kegiatan berhasil, Google Photos menerima kiriman dan tautan Alpi berhasil membuka album. Google Photos tidak menawarkan tujuan album pada layar penerima kiriman. Generator 0.3, izin metadata dan perilaku HyperOS masih membutuhkan uji langsung pada Xiaomi 13T.

## Batas

- Penambahan foto ke album Google Photos tetap manual. SENT adalah catatan konfirmasi operator; Alpi tidak memverifikasi unggahan cloud.
- Koordinat hanya tersedia jika disimpan kamera dan dapat diakses. Nama alamat tidak dicari secara online. Waktu MediaStore tanpa offset memakai zona waktu HP untuk tampilannya.
- Ukuran JPEG tergantung isi foto; hasil sederhana/kecil dapat membesar sesudah diberi frame. Asli tetap disimpan, sehingga penghematan berlaku pada salinan hasil/unggahan.
- Pro menyediakan satu logo dan posisi melalui pilihan, bukan seret bebas atau beberapa logo sekaligus.
- Tidak ada AI pemilih foto, retouch, video, pengenal wajah atau sinkronisasi banyak fotografer.
- Android/HyperOS dapat membatasi pemantauan latar. Uji kamera, GPS EXIF, baterai dan alur Google Photos sebelum acara penting.
- Signing masih debug untuk beta; pembaruan dari build dengan tanda tangan berbeda membutuhkan pemasangan ulang. Simpan hasil penting sebelum menghapus instalasi lama.
