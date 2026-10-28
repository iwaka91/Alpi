# Rencana uji perangkat Alpi 0.3

Target awal: Xiaomi 13T dengan Android/HyperOS yang dipakai operator. Uji aktual diperlukan; pemeriksaan otomatis tidak membuktikan pengelolaan baterai OEM atau antarmuka Google Photos.

| Skenario | Hasil yang diharapkan |
| --- | --- |
| Pemasangan baru tanpa server/token | Acara dapat dibuat, frame dan QR dapat dipakai. |
| Pembaruan 0.1 dengan tanda tangan sama | Acara/frame/hasil tetap ada, URL server tidak dijadikan QR Google Photos. |
| Izin semua foto / sebagian / ditolak | Pemantauan hanya dimulai dengan akses semua foto. |
| Mulai sesi lalu foto kamera potret/landscape/HDR/burst | Foto baru ditangkap sekali, orientasi benar, frame sesuai. |
| Foto lama diedit, screenshot dan ekspor Pictures/Alpi | Tidak masuk antrean sesi. |
| Mode tinjau dan langsung Siap Kirim | REVIEW atau READY sesuai konfigurasi saat mulai sesi. Tidak ada unggah otomatis. |
| Setujui/lewati/kembalikan/proses ulang | Status dan hasil berubah sesuai tindakan, foto asli tidak berubah. |
| Pilih acara lain saat sesi aktif | Ditolak sampai sesi berakhir. |
| Kirim 1 foto dan 30 foto | Penerima memperoleh semua JPEG terpilih; tidak tercampur acara. |
| Pilih foto ke-31 | Batas dijelaskan; pilihan sebelumnya tetap ada. |
| Batal menu berbagi / kembali dari Google Photos | Alpi tetap menunggu konfirmasi; tidak menyebut terunggah. |
| Matikan proses Alpi saat ada kiriman PENDING | Saat dibuka ulang, kiriman dan salinan tetap ada dan dapat dibuka kembali. |
| Google Photos hanya menawarkan unggah ke koleksi utama | Operator dapat unggah lalu menambah ke album di Google Photos; Alpi tidak mengklaim memilih album. |
| Google Photos tidak terpasang/tidak menerima banyak berkas | Menu berbagi atau ekspor galeri menjadi cadangan. |
| Ekspor galeri dan ekspor ulang | Hasil terlihat di Pictures/Alpi; tidak menduplikasi jika salinan sebelumnya masih ada. |
| Kiriman dibatalkan | Foto kembali ke READY dan bisa dipilih lagi. |
| Konfirmasi selesai | Semua foto kelompok menjadi SENT dan dilabeli ditandai operator. |
| QR dipindai HP tamu | Membuka album yang tepat; uji akses/unduh dari Google Photos/browser tamu. |
| Tanpa internet / kuota Google penuh | Framing/QR tetap berjalan; Google Photos mengendalikan kegagalan unggah dan operator tidak menandai selesai. |
| Layar mati/penghemat baterai/reboot/force stop | Periksa latensi/status, notifikasi dan pemulihan sesi sesuai batas Android. |

## Tambahan uji 0.3 pada HP

| Skenario | Hasil yang diharapkan |
| --- | --- |
| Hemat/Seimbang/Detail/kustom | Dimensi dan kualitas sesuai, ukuran asli/hasil benar, asli tidak berubah. |
| Basic dengan teks pendek/panjang | Pita terbaca dan teks terbungkus pada potret serta landscape. |
| GPS kamera aktif/nonaktif, izin metadata diberikan/ditolak | Koordinat hanya tampil jika tersedia; tidak ada lokasi HP saat ini atau 0,0 buatan. |
| Waktu EXIF berzona/tanpa zona/tidak tersedia | Waktu berasal dari sumber foto; data yang hilang dijelaskan dan tidak diganti jam proses. |
| Pro satu logo transparan/JPG | Rasio tetap, semua posisi dapat dipilih, transparansi 0/50/100% benar. |
| Teks dan logo satu posisi | Teks digeser tanpa menutup logo, tetap dalam foto. |
| Edit teks, ganti tab, putar layar, batal/simpan | Draf bertahan; hanya Simpan yang mengubah acara. |
| Simpan saat sesi aktif | Sesi lama tetap memakai snapshot; sesi berikutnya memakai konfigurasi baru. |
| Proses ulang kelompok hasil | Membaca sumber asli; tidak menumpuk frame, kiriman aktif ditolak untuk diubah. |
| Buka album sesudah unggah hasil kompresi | Tautan dan QR tetap album yang sama; penambahan album tetap dilakukan operator. |
