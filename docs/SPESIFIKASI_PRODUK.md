# Spesifikasi produk dan menu

Status: rancangan awal · 27 Oktober 2026

## Pengguna dan tujuan

1. **Fotografer/operator** menyiapkan acara, memulai sesi, memotret dengan kamera bawaan, melihat antrean dan mengendalikan foto yang tayang.
2. **Tamu** memindai QR, melihat foto acara di browser, memilih dan mengunduh hasil tanpa login.
3. **Pengelola acara** menentukan identitas acara, frame, akses album, dan masa tayang. Pada versi awal peran ini dapat dirangkap fotografer.

## Menu aplikasi Android

Navigasi utama: **Beranda · Acara · Foto · Antrean · Lainnya**.

| Menu | Fungsi inti |
| --- | --- |
| Beranda | Acara aktif, status sesi dan izin, jumlah menunggu/diproses/terunggah/gagal; tombol Mulai/Jeda/Akhiri Sesi dan Buka Kamera bawaan. |
| Acara & Album | Buat dan ubah data acara; pilih album tujuan; atur akses album; pratinjau dan bagikan tautan/QR. |
| Frame & Postproses | Impor PNG transparan untuk potret dan landscape; pratinjau, skala, safe area, posisi elemen; pilih kualitas salinan untuk unggahan. |
| Foto | Daftar foto asli terdeteksi dan hasilnya, pratinjau, pilih/tahan sebelum tayang, proses ulang, sembunyikan/hapus dari album sesuai hak operator. |
| Antrean | Tahap Menunggu → Memproses → Menunggu jaringan → Mengunggah → Terbit; kegagalan dapat dicoba ulang, bukan hilang diam-diam. |
| QR & Bagikan | Tampilkan QR resolusi tinggi, salin tautan, simpan/bagikan QR; arahkan QR ke album acara, bukan satu foto. |
| Pengaturan | Pilih sumber/folder kamera yang didukung; Wi-Fi/data seluler; kualitas gambar; izin foto dan notifikasi; status pemantauan serta diagnostik. |
| Bantuan & Tentang | Panduan sesi, pesan gangguan, versi aplikasi, kontak dukungan/identitas pengembang yang nanti disetujui pemilik. |

QR & Bagikan serta Frame & Postproses dapat ditempatkan sebagai subhalaman Acara; Pengaturan dan Bantuan berada dalam Lainnya.

## Menu album web untuk tamu

| Halaman | Fungsi inti |
| --- | --- |
| Album acara | Sampul, identitas acara, foto terbaru, jumlah foto, informasi pembaruan dan akses PIN bila acara memakainya. |
| Lihat foto | Tampilan besar, geser, zoom, informasi sederhana dan unduh satu foto. |
| Pilih & unduh | Pilih beberapa foto dan unduh; unduhan massal diuji pada browser HP sebelum dijanjikan. |
| Bagikan | Bagikan/salin tautan album; tombol bantuan sederhana. |

Galeri harus mudah dipakai pada layar HP, hemat data melalui thumbnail, dan tidak memerlukan akun tamu. Foto penuh diunduh hanya saat dipilih.

## Aturan fungsional

- **Mulai Sesi:** simpan penanda waktu, acara, sumber foto dan konfigurasi frame. Hanya foto baru setelah sesi mulai yang masuk antrean; perubahan foto lama dan tangkapan layar tidak otomatis terbit.
- **Jeda:** tidak memasukkan foto baru; antrean yang sudah ada dapat diselesaikan. **Akhiri:** berhenti menerima foto baru dan tunjukkan sisa antrean hingga tuntas.
- **Keamanan terbit:** operator dapat memilih mode terbit otomatis atau tinjau dahulu. Default awal untuk pengujian: tinjau dahulu agar foto salah/privat tidak langsung tampil.
- **Frame:** hasil adalah file terpisah; jaga rotasi EXIF, rasio foto, tepi aman, ketajaman, dan warna. Bila frame tidak tersedia untuk orientasi tertentu, tahan foto dan beri status jelas.
- **Antrean:** item dan status tersimpan permanen di perangkat; deduplikasi dengan identitas media dan checksum seperlunya; unggahan idempoten agar percobaan ulang tidak menggandakan foto.
- **Koneksi:** ketika offline foto tetap antre; tampilkan waktu dan alasan gagal; coba ulang setelah koneksi kembali.
- **Album:** QR menunjuk alamat album stabil, sedangkan isi album dapat bertambah. Link yang beredar dianggap dapat diakses siapa pun yang memilikinya jika tanpa PIN.
- **Hapus/sembunyikan:** perubahan di sisi operator harus tercermin pada galeri; foto asli lokal tidak terhapus oleh tindakan moderasi album.

## Belum diputuskan

Penyedia backend/penyimpanan dan kuota, retensi album, domain, ukuran/kompresi keluaran, PIN atau publik sebagai default, dan apakah terbit otomatis dibuka sebagai mode produksi. Semua dicatat sebagai pilihan implementasi, bukan persetujuan tersirat.

