# Alpi 0.3.0-beta

Kompresi foto dan generator frame lokal kini tersedia pada setiap acara.

- **Kompresi**: preset Hemat (1600 px/JPEG 72), Seimbang (2048/82), Detail (2560/90), atau kustom 1280–3200 px dan kualitas 60–95. Ukuran asli/hasil, dimensi dan penghematan tampil pada kartu foto.
- **Basic**: teks utama, pilihan menampilkan waktu pengambilan dan koordinat GPS dari metadata, pita bawah hijau/putih/hitam.
- **Pro**: semua fitur Basic, teks kegiatan, satu logo PNG/JPG/WebP, sembilan posisi teks dan logo, ukuran teks/logo serta transparansi logo 0–100%.
- Studio dengan pratinjau lokal, pilihan foto uji atau ilustrasi contoh. Rasio logo dijaga, tata letak menyesuaikan orientasi dan mencegah logo menutup teks.
- Pilihan hasil siap kirim dapat diproses ulang bersama dari foto asli menggunakan kompresi/frame terbaru.
- Metadata lokasi memakai izin ACCESS_MEDIA_LOCATION untuk membaca GPS yang sudah disimpan kamera. Tidak ada pengambilan lokasi HP saat ini atau pencarian alamat online. Data yang hilang dilewati dan dijelaskan pada hasil.
- Tanggal pengambilan EXIF dipertahankan pada hasil untuk membantu urutan foto. Koordinat tidak disalin ke EXIF hasil; hanya dicetak pada gambar jika dipilih operator.
- Migrasi database versi 3 mempertahankan acara, frame lama, antrean, hasil dan kiriman sebelumnya. Frame PNG/pita lama tetap tersedia.

Foto asli tidak diubah atau dihapus. Penghematan berlaku pada salinan hasil; ukuran tergantung isi foto dan pengaturan. Pengaturan baru berlaku pada sesi berikutnya atau melalui Proses ulang.

Alur Google Photos tetap semi-manual: unggah hasil ke Photos, buka album melalui tautan Alpi, tambahkan hasil dari dalam album, lalu tandai kiriman selesai di Alpi. Tidak ada Google Cloud Console, server atau API Google Photos yang diperlukan.

APK bertanda tangan debug untuk beta. Jika Android menolak pembaruan karena tanda tangan berbeda, simpan hasil penting sebelum memasang ulang. Perilaku generator dan izin metadata pada Xiaomi 13T/HyperOS tetap perlu diuji langsung.
