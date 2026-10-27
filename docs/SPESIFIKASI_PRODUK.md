# Spesifikasi produk Alpi 0.2

Nama: **Album Photo Live (Alpi)**. Platform operator: APK Android 10+. Album tamu: Google Photos milik operator, dibagikan melalui tautan/QR.

## Pengalaman operator

| Menu | Isi |
| --- | --- |
| Beranda | Acara terpilih, mulai/jeda sesi, buka kamera bawaan, ringkasan hasil, kiriman menunggu konfirmasi, album dan QR. |
| Acara | Nama/informasi, tautan berbagi Google Photos, pemilihan acara, frame potret/landscape, pita kegiatan, mode tinjauan/langsung Siap Kirim. |
| Foto | Pratinjau hasil/asli, setujui, lewati/kembalikan, proses ulang, riwayat hasil dan status kiriman. |
| Siap Kirim | Filter acara, pilihan maksimal 30 foto, kirim ke Google Photos, ekspor galeri, konfirmasi kiriman atau kembalikan pilihan. |
| Lainnya | Folder kamera, izin, pengaturan Android dan bantuan penggunaan. |

Alpi menyiapkan hasil pada HP dan tidak menjalankan server atau meminta proyek Google Cloud. Tautan album dipakai untuk navigasi dan QR; unggah diteruskan lewat fitur berbagi atau dilakukan dari folder hasil di galeri.

## Batas fungsi

Google Photos memiliki kendali penuh atas penerimaan berkas dan penambahan album. Tidak ada unggah otomatis melalui tautan album, pemilihan album otomatis, atau deteksi keberhasilan unggahan. Operator menyelesaikan tindakan tersebut di Google Photos dan menandai kiriman selesai setelah memeriksa album.

Seleksi berarti tinjauan operator dengan setujui/lewati, bukan AI pemilih foto terbaik. Foto asli tidak berubah; hasil frame JPEG dibatasi 2.560 px pada sisi panjang. Selama sesi aktif, Alpi hanya mengambil foto kamera baru; bukan semua isi galeri.

Tamu menggunakan Google Photos/browser melalui QR dan tidak memerlukan APK Alpi. Hak akses, unduh, penyimpanan dan masa berlaku tautan mengikuti akun serta layanan Google Photos.

Implementasi server versi 0.1 masih berada di repo sebagai riwayat teknis dan tidak dipakai APK aktif.
