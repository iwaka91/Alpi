# Kompresi dan generator frame Alpi 0.3

Buka **Acara → Kelola acara → Kompresi & generator frame**. Tombol Kompresi, Basic dan Pro membuka studio yang sama dengan satu draf pengaturan per acara. Perubahan belum diterapkan sebelum menekan **Simpan pengaturan**.

## Kompresi

| Pilihan | Sisi panjang maksimal | Kualitas JPEG |
| --- | --- | --- |
| Hemat | 1600 px | 72 |
| Seimbang | 2048 px | 82 |
| Detail | 2560 px | 90 |
| Kustom | 1280, 1600, 2048, 2560, 3200 px | 60–95 |

Kompresi menjaga rasio foto dan tidak memperbesar foto yang sudah lebih kecil. Acara baru memakai Seimbang. Acara/antrean lama mempertahankan pengaturan 0.2 (2560/JPEG 92) sampai pengaturannya diedit. Pada kartu hasil, Alpi menampilkan ukuran sumber, hasil, dimensi dan persentase penghematan jika hasil lebih kecil. Foto kecil atau sederhana dapat menghasilkan ukuran lebih besar sesudah ditambah frame; Alpi tidak menjanjikan ukuran file tertentu.

Kompresi hanya memengaruhi salinan hasil. Foto asli kamera tetap berada di galeri; salinan kiriman serta ekspor galeri juga memakai ruang tersendiri.

## Basic

1. Isi **Teks utama** (maksimal 240 karakter).
2. Centang **Waktu pengambilan dari metadata** dan/atau **Lokasi dari metadata (koordinat GPS)** bila ingin dicetak pada foto.
3. Pilih warna pita hijau gelap, putih atau hitam.
4. Lihat pratinjau, lalu simpan.

Basic menempatkan teks dan metadata dalam pita bawah. Alpi menyesuaikan tinggi pita dan pembungkusan teks agar beberapa baris tetap terbaca.

## Pro

Semua fitur Basic ditambah:

- **Teks kegiatan**, maksimal 160 karakter.
- **Posisi teks dan metadata**: atas/tengah/bawah × kiri/tengah/kanan.
- Ukuran teks 70–160% dan pilihan latar pita.
- Satu logo dari PNG, JPG atau WebP, maksimal berkas impor 12 MB. PNG transparan disarankan. Salinan logo disimpan privat dengan sisi panjang maksimal 1024 px; rasio gambar dijaga.
- Posisi logo pada sembilan titik, ukuran 5–35% lebar foto dengan batas tinggi agar tidak mendominasi foto.
- **Transparansi logo 0–100%**: 0% padat, 100% tidak terlihat.

Jika logo dan teks beririsan, teks digeser ke bawah atau atas logo dengan jarak aman. Posisi berlaku relatif terhadap ukuran foto, sehingga pengaturan yang sama dapat dipakai untuk potret dan landscape. Ini pengaturan posisi melalui pilihan; bukan editor seret bebas dan tidak mendukung beberapa logo sekaligus.

## Metadata yang benar-benar berasal dari foto

- Waktu dibaca dari EXIF DateTimeOriginal, lalu DateTimeDigitized bila diperlukan. Untuk sumber MediaStore, DATE_TAKEN dapat dipakai jika EXIF tidak ada. Waktu unggah, jam saat ini dan waktu berkas ditambahkan tidak dijadikan waktu pengambilan.
- Jika offset zona waktu ada di EXIF, offset tersebut ditampilkan. Waktu EXIF tanpa offset dipertahankan sebagai waktu yang tertulis pada foto; fallback timestamp MediaStore ditampilkan dalam zona waktu HP.
- Lokasi berupa koordinat GPS yang sudah disimpan pada foto, bukan nama jalan atau koordinat HP saat ini. Aktifkan penyimpanan lokasi di kamera bawaan jika ingin foto berikutnya memiliki GPS.
- Android dapat menyembunyikan GPS. Saat pilihan lokasi diaktifkan, izinkan akses metadata lokasi jika diminta. Izin ini berbeda dari izin mengukur lokasi perangkat secara langsung; Alpi tidak meminta GPS aktif melalui layanan lokasi.
- Jika metadata tidak tersedia atau izinnya belum diberikan, foto tetap diproses, baris yang bersangkutan tidak dicetak, dan kartu hasil memberikan keterangan. Alpi tidak mengisi lokasi palsu atau waktu saat pemrosesan.
- Hasil mempertahankan tanggal pengambilan EXIF bila tersedia; GPS tidak disalin ke EXIF hasil. Jika dicentang, koordinat menjadi teks yang terlihat pada gambar dan dapat dilihat tamu.

## Pratinjau dan penerapan

Pratinjau default memakai foto terbaru pada acara bila dapat dibaca. Tombol **Pilih foto untuk pratinjau** memungkinkan foto uji lain. **Gunakan ilustrasi contoh** menampilkan kanvas dan metadata bertanda contoh; data contoh tidak pernah dipakai pada hasil produksi. Pratinjau berukuran kecil untuk melihat tata letak; ukuran hasil mengikuti kompresi acara.

Setelah menyimpan, **akhiri sesi yang aktif lalu Mulai sesi baru**. Konfigurasi acara disalin saat sesi dimulai dan disimpan bersama setiap foto.

Untuk hasil lama: menu Foto menyediakan **Proses ulang · kompresi & frame sekarang**. Di Siap Kirim, pilih satu kelompok lalu **Terapkan kompresi / frame terbaru ke pilihan**. Proses dimulai lagi dari foto asli agar frame tidak ditumpuk di atas hasil frame lama. Foto yang sedang diserahkan ke Google Photos atau sudah ditandai selesai tidak diubah; kembalikan ke Siap Kirim terlebih dahulu bila ingin memproses ulang.

## Memakai frame PNG lama

Di tab Kompresi, tekan **Gunakan frame PNG / pita lama** lalu simpan. Frame PNG dan pita yang sudah diimpor pada acara tetap tersimpan. Generator Basic/Pro menggantikan komposisi PNG/pita lama selama mode tersebut aktif, sehingga keduanya tidak dicetak bertumpuk.

## Google Photos dan QR

Pengiriman tetap memakai hasil JPEG yang sudah dikompresi dan diberi frame. Upload ke Photos, buka album dari tautan Alpi, lalu gunakan Tambahkan foto dari dalam album. QR tamu tetap mengarah ke album yang sama. Generator dan kompresi tidak mengubah batas integrasi album.
