# Album Photo Live (Alpi)

Alpi adalah sistem foto acara: fotografer tetap memakai **kamera bawaan HP**, aplikasi Android mendeteksi hasil jepretan saat sesi aktif, memasang frame kegiatan pada salinan foto, lalu mengunggahnya ke album online. Tamu memindai QR acara untuk membuka album web dan mengunduh foto tanpa memasang aplikasi.

**Status:** dokumen rancangan awal, 27 Oktober 2026. Belum ada APK, layanan penyimpanan, atau album web yang berjalan.

## Keputusan yang sudah disepakati

- Nama **Album Photo Live (Alpi)** ditentukan oleh pemilik proyek. Penamaan produk berikutnya merupakan kewenangan pemilik.
- Fotografer menggunakan kamera bawaan HP; Alpi berjalan sebagai pendamping Android.
- Hasil jepretan baru diproses pada sesi acara aktif; foto asli tidak diubah.
- Frame kegiatan dipasang otomatis pada salinan hasil foto sebelum upload.
- Foto yang berhasil diunggah tampil di album web melalui tautan/QR acara yang tetap.
- Aplikasi Android untuk fotografer, web untuk tamu, penyimpanan daring untuk foto.
- Target awal pengujian: Xiaomi 13T (Android 16). Dukungan perangkat lain diverifikasi kemudian.

## Dokumen

- [Spesifikasi produk dan menu](docs/SPESIFIKASI_PRODUK.md)
- [Arsitektur dan alur foto](docs/ARSITEKTUR_ALUR.md)
- [Rencana pembangunan dan uji](docs/RENCANA_UJI.md)

## Prinsip pengerjaan

Mulai dari pembuktian deteksi foto kamera bawaan dan antrean yang tahan gangguan, kemudian pemrosesan frame, backend album, serta pengalaman QR tamu. Pilihan penyedia penyimpanan, domain, dan kebijakan akses album belum dikunci. Jangan menganggap target latensi sebagai jaminan sebelum pengujian perangkat dan jaringan.
