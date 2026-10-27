# Rencana pembangunan dan uji

Status: rancangan awal · 27 Oktober 2026

## Tahapan

| Tahap | Hasil yang perlu terbukti | Kriteria selesai |
| --- | --- | --- |
| 0. Prototipe Android lokal | Sesi memantau kamera bawaan dan mengenali foto baru tanpa upload. | Foto baru masuk daftar sekali; foto lama/screenshot/output Alpi tidak ikut; jeda dan akhir sesi bekerja. |
| 1. Postproses | Frame PNG potret/landscape diterapkan pada salinan. | Orientasi, rasio, tulisan dan foto asli benar; proses gagal terlihat dan bisa dicoba ulang. |
| 2. Antrean & upload | Item bertahan setelah aplikasi/koneksi terputus; server menolak duplikat. | Tidak ada foto hilang atau terbit ganda pada pengujian mati aplikasi, jaringan putus, dan retry. |
| 3. Album & QR | Web mobile menampilkan hanya foto terbit, thumbnail dan unduhan; URL album stabil. | Dua HP tamu dapat membuka QR yang sama, melihat foto tambahan, dan mengunduh hasil. |
| 4. Uji acara | Alur penuh, moderasi, indikator, kuota dan kemampuan pemulihan. | Laporan latensi nyata, kegagalan, pemakaian data/baterai, kapasitas, dan langkah operator tersedia. |

## Skenario uji wajib pada Xiaomi 13T

- Kamera bawaan tetap dapat dipakai normal saat sesi Alpi aktif.
- Foto potret, landscape, burst/HDR dan perubahan nama/penulisan bertahap tidak menyebabkan duplikasi.
- Jeda, akhiri, buka ulang aplikasi, kunci layar, optimasi baterai, serta memori rendah.
- Offline selama 10 foto, lalu online; seluruh antrean tertangani tepat sekali.
- Frame untuk kedua orientasi dan kasus frame hilang/rusak.
- Mode tinjau dahulu: foto tertahan tidak terlihat tamu; foto tersembunyi hilang dari album.
- QR dari cetakan dan layar; unduh satu foto pada Android/iPhone; album tetap dapat dibuka saat foto baru ditambahkan.
- Ukur waktu dari jepret hingga terlihat pada web pada Wi-Fi dan data seluler, tanpa mengklaim target sebelum ada hasil.

## Ukuran dan kuota: contoh perencanaan, bukan angka baku

Jika satu hasil unggahan 2 MB, maka 500 foto memerlukan sekitar 1 GB penyimpanan hasil, di luar thumbnail. Bila 100 tamu masing-masing mengunduh 10 foto, lalu lintas unduhnya sekitar 2 GB; melihat thumbnail dan retry menambah penggunaan. Foto asli beresolusi penuh dapat jauh lebih besar. Pilihan backend gratis harus diuji terhadap jumlah foto, kunjungan, dan batas layanan yang berlaku saat implementasi.

## Batas rilis awal

Rilis awal menargetkan **satu operator, satu acara aktif, frame statis PNG, galeri web, QR stabil, moderasi sederhana dan antrean tahan gangguan**. Pengenalan wajah, edit AI, pembayaran foto, banyak fotografer serentak, dan video berada di luar cakupan awal.

## Keputusan berikutnya sebelum backend

- Pilih penyedia storage/backend dan kebijakan biaya/kuota.
- Tentukan album publik via tautan atau PIN sebagai default, masa tayang dan prosedur hapus.
- Tetapkan ukuran unggahan dan apakah tamu mengunduh salinan ber-frame saja.
- Setujui alur terbit otomatis atau tinjau dahulu untuk pemakaian nyata.

