# Alpi 0.2.0-beta

Alur tanpa server: kamera bawaan → deteksi lokal → tinjau/frame → pilih kelompok hasil → serahkan ke Google Photos → operator menambahkan ke album → QR tamu.

- Pengaturan tautan berbagi Google Photos per acara.
- Siap Kirim dengan pilihan hingga 30 foto dari satu acara dan pratinjau hasil.
- Kiriman satu/beberapa JPEG melalui URI Android berizin baca sementara.
- Salinan kiriman tetap dan status menunggu konfirmasi yang bertahan setelah aplikasi dibuka ulang.
- Tandai selesai secara manual sesudah memeriksa album; batalkan untuk kembali ke Siap Kirim.
- Ekspor hasil ke Pictures/Alpi sebagai cadangan jika penerima berbagi tidak sesuai.
- QR dibuat lokal; pengaturan server, token dan PIN album Alpi dihapus dari APK.
- APK tidak menggunakan API Google Photos, OAuth, atau izin internet.
- Migrasi data versi 0.1 mempertahankan acara, frame, hasil dan riwayat server lama.

Google Photos mengendalikan layar penerimaan/unggah dan penambahan album. Alpi tidak dapat mengarahkan kiriman langsung ke album tertentu melalui tautan atau memverifikasi keberhasilan unggahan.

APK ini ditandatangani debug untuk pengujian. Pembaruan dari beta sebelumnya dapat membutuhkan pemasangan ulang jika tanda tangan berbeda; simpan hasil penting sebelum menghapus aplikasi. Uji pada Xiaomi 13T/HyperOS dan Google Photos nyata tetap diperlukan.
