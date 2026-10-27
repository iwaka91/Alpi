# Panduan Alpi 0.2.0-beta

Versi ini memakai Google Photos untuk album tamu. Tidak perlu laptop server, token operator, proyek Google Cloud atau login Google di Alpi. Akun Google dikelola di aplikasi Google Photos.

## 1. Pasang APK

Unduh `Alpi-v0.2.0-beta.apk` dari [rilis GitHub](https://github.com/iwaka91/Alpi/releases/tag/v0.2.0-beta), lalu pasang pada Android 10 atau lebih baru.

APK beta memakai tanda tangan debug. Jika Android menolak pembaruan karena tanda tangan berbeda dengan APK 0.1, simpan dulu hasil penting melalui menu berbagi versi lama, lalu hapus aplikasi lama dan pasang versi ini. Menghapus Alpi juga menghapus acara, frame impor dan hasil privat di Alpi. Foto asli kamera tidak dihapus. Migrasi database otomatis berlaku bila APK dapat dipasang sebagai pembaruan dengan tanda tangan yang sama.

Izinkan **semua foto** dan notifikasi. Izin beberapa foto saja tidak cukup untuk mendeteksi jepretan berikutnya secara otomatis. Pada Xiaomi/HyperOS, periksa pengaturan baterai Alpi dan notifikasi sesi; gunakan mode baterai yang mengizinkan pemantauan selama acara.

## 2. Siapkan album Google Photos

1. Buka Google Photos dengan akun pemilik acara.
2. Buat album acara, aktifkan berbagi tautan, dan salin tautannya.
3. Pada Alpi → **Acara → Buat acara**, isi nama dan tempel tautan berbagi. Contoh format: `https://photos.app.goo.gl/...` atau `https://photos.google.com/share/...?key=...`.
4. Simpan acara. Gunakan **Buka album Google Photos** untuk memeriksa tujuan.
5. Buat/bagikan QR acara. QR mengarah persis ke tautan yang disimpan dan dibuat lokal tanpa koneksi internet.

Tautan album bukan alamat unggah. Alpi tidak membuat atau mengelola album Google Photos secara otomatis. Akses tamu dan kemampuan berkolaborasi mengikuti pengaturan Google Photos. Uji QR dari HP tamu sebelum dicetak dan jangan mengganti tautan setelah QR dibagikan.

## 3. Pasang frame dan mulai sesi

1. Di acara, impor frame PNG transparan untuk potret dan/atau landscape. Gunakan rasio yang sama dengan hasil kamera.
2. Isi **Teks pita** jika ingin penanda kegiatan tanpa desain PNG.
3. Pilih mode: sakelar mati berarti hasil menunggu tinjauan; sakelar hidup berarti langsung masuk **Siap Kirim** setelah framing. Sakelar ini tidak mengirim ke Google Photos otomatis.
4. Pilih acara, lalu **Mulai sesi** di Beranda.
5. Tekan **Buka kamera bawaan** dan memotret seperti biasa.
6. Kembali ke Alpi untuk meninjau hasil. Selama sesi, notifikasi Alpi tetap terlihat.

Hanya foto baru di folder kamera setelah sesi dimulai yang dipantau. Folder default `DCIM/Camera/` dapat diganti di **Lainnya → Folder kamera** saat sesi berhenti. Foto lama, screenshot dan hasil ekspor `Pictures/Alpi` tidak dipantau. Perubahan frame/mode berlaku pada sesi baru. Hasil lama dapat diproses ulang memakai konfigurasi acara sekarang.

Hasil JPEG: sisi panjang maksimal 2.560 piksel, kualitas 92. Foto asli tidak diubah. Jika frame salah orientasi belum dipasang sementara orientasi lain memiliki frame, Alpi menahan foto untuk diperbaiki.

## 4. Tinjau dan kirim satu kelompok foto

1. Di **Foto**, perbesar hasil, tekan **Setujui → Siap Kirim** atau **Lewati foto ini**. Foto dilewati dapat dikembalikan untuk ditinjau.
2. Buka **Siap Kirim**. Pastikan acara benar; Alpi tidak mencampur foto beberapa acara dalam satu kiriman.
3. Centang foto atau pilih 30 pertama. Maksimal 30 foto per kiriman agar tidak membebani aplikasi penerima.
4. Tekan **Kirim pilihan ke Google Photos**. Alpi menyerahkan salinan hasil JPEG melalui fitur berbagi Android. Jika Google Photos tidak dapat menerima langsung, menu berbagi ditampilkan.
5. Selesaikan langkah yang ditampilkan Google Photos. Jika penerima hanya mengunggah ke koleksi utama, tambahkan foto yang sudah terunggah ke album tujuan dari dalam Google Photos. Alpi tidak dapat menentukan album pada antarmuka tersebut.
6. Kembali ke Alpi. Pada kartu **Menunggu konfirmasi kiriman**, buka album dan periksa semua hasil.
7. Setelah foto benar-benar terlihat di album, tekan **Sudah masuk album · tandai selesai**, lalu konfirmasi.

Status selesai adalah catatan operator, bukan hasil verifikasi Google Photos. Menutup menu berbagi, membuka aplikasi penerima atau kembali ke Alpi tidak otomatis menandai berhasil.

Jika dibatalkan atau belum berhasil, tekan **Belum jadi / kembalikan ke Siap Kirim**. Untuk mencoba lagi dengan salinan yang sama, tekan **Buka kiriman yang sama lagi**; periksa album lebih dahulu agar tidak mengirim duplikat. Hanya satu kelompok kiriman dapat menunggu konfirmasi pada satu waktu. Foto baru tetap dapat dideteksi dan diproses sambil menunggu.

## 5. Cadangan: simpan ke galeri

Jika Google Photos tidak menerima kiriman atau tombol terima unggahan tidak sesuai:

1. Pada Siap Kirim, pilih hasil lalu tekan **Simpan pilihan ke galeri HP**. Tombol serupa tersedia untuk kiriman yang sedang menunggu konfirmasi.
2. Buka Google Photos → folder di perangkat/**Di perangkat ini**; temukan `Alpi` atau folder berkode acara. Label menu dapat berbeda antarversi Google Photos.
3. Pilih hasil, unggah bila perlu, lalu tambahkan ke album acara dari Google Photos.
4. Periksa album. Kiriman tetap menunggu konfirmasi di Alpi; tandai selesai hanya sesudah semua foto terlihat.

Ekspor tersimpan di `Pictures/Alpi/<8 karakter kode acara>/`, nama berkas `ALPI_<id foto>.jpg`. Ekspor ulang memakai berkas milik Alpi yang sama selama salinan galeri masih ada. Jika salinan sebelumnya dihapus, Alpi membuat salinan baru. Menyimpan ke galeri sendiri tidak berarti mengunggah, dan pencadangan folder Google Photos tidak otomatis menambahkan foto ke album tertentu.

## 6. Tamu

Pasang QR pada area acara. Tamu membuka tautan album Google Photos, memilih foto dan menggunakan fitur unduh yang tersedia. Tidak perlu memasang Alpi. Kebutuhan login, menu unduh dan pengaturan akses mengikuti Google Photos/browser tamu; uji dengan HP lain sebelum acara.

## 7. Data dan batas pemantauan

- Acara, frame, antrean dan hasil berada di penyimpanan privat Alpi. Kiriman menggunakan salinan tetap agar proses ulang tidak mengubah berkas yang sedang diterima Google Photos.
- Salinan kiriman selesai/dibatalkan dapat dibersihkan otomatis setelah tujuh hari. Salinan kiriman menunggu konfirmasi dipertahankan. Hasil utama tetap tersimpan di Alpi.
- Menutup paksa, menghapus data aplikasi atau aturan baterai perangkat dapat menghentikan pemantauan. Setelah HP dimulai ulang, buka Alpi dan mulai sesi lagi.
- Android 15+ membatasi durasi layanan latar jenis dataSync; saat batas tercapai Alpi mengakhiri sesi dan menampilkan pesan untuk memulai sesi baru.
- Rekonsiliasi foto terlambat tersimpan berlangsung hingga 24 jam setelah sesi berakhir; antrean yang sudah tercatat tidak kedaluwarsa setelah 24 jam.
- Pemrosesan lokal/QR dapat berjalan tanpa internet. Unggahan Google Photos dan akses album oleh tamu memerlukan internet serta ruang akun Google yang mencukupi.
- Riwayat server 0.1 dipertahankan saat migrasi, tetapi tautan server lama tidak dipakai sebagai tautan Google Photos. Isikan tautan Google Photos pada acara.
