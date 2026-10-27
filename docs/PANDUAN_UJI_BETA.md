# Panduan uji Alpi 0.1.0-beta

Alpi terdiri dari APK operator dan server yang sekaligus menyajikan album web. APK bisa mendeteksi dan memproses foto secara lokal. Unggahan dan QR tamu memerlukan server yang dapat dijangkau. Paket ini tidak otomatis menyewa hosting atau mengaktifkan domain publik.

## 1. Siapkan server pada laptop Windows

1. Pasang Python 3.12 atau lebih baru. Saat memasang, aktifkan pilihan menambahkan Python ke PATH.
2. Unduh ZIP repo atau paket server, lalu ekstrak ke satu folder.
3. Klik dua kali **MULAI_SERVER_WINDOWS.bat**. Pemasangan dependensi pertama membutuhkan internet. Pemakaian lokal selanjutnya hanya memerlukan jaringan antara HP dan laptop.
4. Biarkan jendela server terbuka. Jika Windows meminta izin firewall, izinkan untuk jaringan privat yang dipakai saat uji.
5. Catat alamat server yang ditampilkan, misalnya `http://192.168.1.10:8000`. Jika laptop memiliki VPN atau beberapa adaptor, gunakan IP IPv4 Wi-Fi dari `ipconfig`.
6. Buka file `.env` yang dibuat di folder tersebut menggunakan Notepad. Salin nilai `ALPI_ADMIN_TOKEN` setelah tanda `=`. Simpan file ini untuk pemakaian berikutnya; jangan membagikan token kepada tamu.

HP fotografer, laptop server, dan HP tamu harus dapat saling terhubung di jaringan yang sama untuk uji LAN. Jaringan tamu dengan isolasi antarperangkat dapat menghalangi akses. Album lokal tidak bisa dibuka dari internet/data seluler luar jaringan itu.

## 2. Pasang dan hubungkan APK

1. Pasang APK beta pada HP Android 10 atau lebih baru. Izinkan pemasangan dari sumber file yang Anda gunakan bila diminta.
2. Buka **Alpi → Lainnya → Server & koneksi**.
3. Masukkan alamat server dan token operator. Untuk contoh HTTP lokal, centang **Izinkan HTTP untuk uji jaringan lokal**.
4. Folder kamera awal: `DCIM/Camera/`. Jika HP menyimpan ke folder lain dalam DCIM, sesuaikan path yang tampil pada MediaStore.
5. Berikan izin **semua foto** dan izin notifikasi. Izin beberapa foto terpilih belum cukup untuk mendeteksi foto baru otomatis.

Token disimpan terenkripsi dengan Android Keystore. Foto hasil, frame dan antrean disimpan pada ruang privat aplikasi; menghapus data/uninstall Alpi akan menghapusnya. Foto asli kamera tetap berada di galeri HP.

## 3. Buat acara dan frame

1. Buka **Acara → Buat acara**. Isi nama, informasi, dan PIN tamu opsional.
2. Pilih acara tersebut. Buka **Frame & postproses**, lalu impor PNG transparan potret dan landscape. Gunakan rasio frame yang sama dengan rasio foto kamera, misalnya 3:4 dan 4:3. Frame ditempatkan pada seluruh kanvas foto; bagian PNG yang transparan memperlihatkan foto.
3. Teks pita di bawah foto dapat diisi tanpa perlu membuat PNG. Keduanya juga bisa digunakan bersamaan; pastikan teks tidak bertumpuk dengan elemen frame.
4. Pilih **Hubungkan / perbarui album online**. Setelah berhasil, tersedia tombol QR, buka album, dan bagikan tautan.
5. Mode awal adalah **tinjau dahulu**. Aktifkan **Terbit otomatis setelah diproses** bila ingin hasil langsung diunggah tanpa persetujuan per foto.

Frame, teks pita, dan mode terbit disalin pada saat Mulai Sesi. Jeda/akhiri lalu mulai sesi baru setelah mengubahnya. Bila hanya memasang satu orientasi frame, foto orientasi lain ditahan dengan pesan sampai frame sesuai tersedia. Tanpa frame PNG sama sekali, foto tetap bisa diproses; teks pita bersifat opsional.

## 4. Memotret dan mengambil foto

1. Di **Beranda**, tekan **Mulai sesi** lalu **Buka kamera bawaan**.
2. Potret satu foto landscape dan satu potret. Periksa notifikasi Alpi tetap aktif.
3. Kembali ke **Foto**. Pilih **Lihat hasil foto** untuk memeriksa frame. Dalam mode tinjau, tekan **Setujui & unggah**.
4. Di menu **Antrean**, amati status dan pesan gangguan. Tombol coba lagi tersedia bila item gagal berulang.
5. Tampilkan QR acara. Tamu memindai QR dengan kamera HP, memasukkan PIN jika dipakai, lalu membuka dan mengunduh foto.
6. Album diperbarui otomatis sekitar 12 detik sekali saat halaman terlihat. Tombol **Perbarui** tersedia untuk pemeriksaan manual.
7. Foto terunggah dapat disembunyikan dari album melalui menu Foto. Pengaturan ini juga memblokir akses langsung ke file lewat server; salinan yang sudah diunduh tamu tidak dapat ditarik kembali.

**Jeda / akhiri sesi** menghentikan penerimaan foto baru; item yang sudah masuk antrean tetap ditangani. Untuk menjeda unggahan seluruhnya, pilih jaringan yang tidak tersedia atau hentikan aplikasi; pekerjaan dilanjutkan setelah dibuka kembali. Versi ini belum memiliki tombol jeda unggah terpisah.

## 5. Skenario uji yang perlu dilaporkan

| Uji | Hasil yang diharapkan |
| --- | --- |
| Potret dengan kamera bawaan | Foto baru muncul satu kali pada Alpi, foto lama dan screenshot tidak ikut. |
| Frame dua orientasi | Orientasi benar, foto asli tetap bersih, hasil dapat dibagikan. |
| Mode tinjau | Foto belum muncul di album sebelum disetujui. |
| Putus koneksi setelah 5 foto | Antrean tersimpan dan unggahan dapat diteruskan tanpa foto ganda. |
| Jeda lalu potret | Foto selama jeda tidak masuk sesi. |
| Layar terkunci / kembali ke kamera | Periksa apakah pemantauan masih aktif dan catat jeda tampil foto. |
| Tutup paksa / restart HP | Data antrean tetap ada; buka Alpi dan mulai/lanjutkan sesi secara sadar. |
| Sembunyikan foto | Foto hilang dari album dan tautan file tidak bisa diunduh lagi. |
| Dua HP tamu | Keduanya bisa membuka QR yang sama dan mengunduh JPEG. |

Catat versi Android/HyperOS, orientasi, mode terbit, jenis jaringan, jumlah foto, dan waktu jepret sampai muncul di web. Jangan menganggap beta sudah teruji di Xiaomi sebelum skenario perangkat nyata dijalankan.

## 6. Batas beta

- Satu operator dan satu sesi aktif pada HP; server dapat menampung beberapa acara.
- Keluaran JPEG maksimal sisi panjang 2.560 px dengan kualitas 92; bukan salinan resolusi asli penuh.
- Layanan aktif menggunakan notifikasi Android dan pekerjaan cadangan terjadwal. Android 15+ membatasi waktu layanan `dataSync`; saat timeout sesi dihentikan dan alasannya ditampilkan. Respons kamera/HyperOS dan kebijakan baterai tetap perlu uji perangkat.
- Rekonsiliasi foto terlambat mencakup jendela sesi yang berakhir dalam 24 jam terakhir; antrean yang sudah tercatat tidak memiliki batas 24 jam tersebut.
- Foto dengan waktu pengambilan tidak tersedia bergantung pada ketelitian waktu simpan MediaStore. Foto burst, HDR yang lambat selesai, dan jam perangkat berubah perlu diuji.
- Web mendukung unduh satu foto dan pilihan beberapa file. Browser tertentu meminta izin unduhan berulang; gunakan unduh satu foto bila unduhan massal dibatasi.
- Mode HTTP untuk uji jaringan tepercaya. Gunakan HTTPS sebelum pemakaian publik.
- APK memakai tanda tangan debug untuk uji. Penandatanganan rilis tetap perlu disiapkan sebelum distribusi produksi; build debug dari mesin/CI berbeda mungkin perlu uninstall karena kunci berbeda.

## 7. Menjalankan server melalui terminal atau Docker

```bash
python -m venv .venv
# Windows: .venv\Scripts\activate
# Linux/macOS: source .venv/bin/activate
pip install -r server/requirements.txt
python server/run.py
```

Untuk Docker, salin `.env.example` menjadi `.env`, isi token acak minimal 24 karakter dan `ALPI_PUBLIC_BASE_URL`, lalu jalankan `docker compose up -d --build`. Data disimpan di named volume `alpi-data`; lakukan backup volume bersama database dan JPEG. Gunakan satu proses server pada rilis awal.

Untuk akses internet: tempatkan server pada host dengan disk persisten, pasang HTTPS melalui reverse proxy, isi URL publik pada `.env`, lalu gunakan alamat yang sama pada APK. Penyedia hosting/domain dan kuota belum dipilih. Server tidak menggunakan layanan foto berbayar, tetapi biaya host, listrik/jaringan dan kapasitas disk tergantung penempatan.
