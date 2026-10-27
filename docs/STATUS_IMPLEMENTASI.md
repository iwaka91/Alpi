# Status implementasi Alpi 0.2.0-beta

## Tersedia

- APK operator native: kamera bawaan, pemantauan sesi MediaStore, frame PNG/pita pada salinan, tinjau/setujui/lewati, proses ulang.
- Acara dengan tautan berbagi Google Photos, buka album, berbagi tautan dan QR lokal.
- Siap Kirim per acara, maksimal 30 foto, satu/banyak JPEG lewat Android sharing, penanganan penerima tidak tersedia.
- Salinan kiriman tetap, kelompok PENDING bertahan setelah pembukaan ulang, batalkan/kembalikan, konfirmasi selesai oleh operator.
- Ekspor MediaStore Pictures/Alpi dengan penggunaan ulang URI ekspor bila tersedia.
- Migrasi database 0.1 tanpa menghapus riwayat/frame/hasil atau menganggap URL server lama sebagai album Google Photos.
- Pengaturan server/token/PIN Alpi dan izin jaringan dihapus dari APK aktif.
- CI build/lint/tes serta prerelease unduhan APK.

## Verifikasi rilis

Pemeriksaan rilis mencakup assembleDebug, lintDebug, delapan unit test kebijakan foto kamera, dua test validasi tautan dan sembilan test integrasi Android/JVM (Robolectric) untuk penyerahan kiriman, snapshot, pembatalan, persistensi, isolasi acara/transaksi, batas kelompok, hasil hilang, QR, migrasi dan alur pilihan antarmuka setelah rotasi layar. Status kelulusan tersedia pada [GitHub Actions](https://github.com/iwaka91/Alpi/actions). Backend 0.1 yang tidak dipakai APK masih memiliki sepuluh test API di CI.

Pengujian integrasi memakai lingkungan Android simulasi pada JVM. Ini belum membuktikan penerimaan Google Photos nyata, tampilan HP, izin MediaStore OEM, perilaku kamera, HyperOS atau pembatasan baterai pada Xiaomi 13T. Uji sesuai RENCANA_UJI.md diperlukan.

## Batas yang perlu dipahami

- Alpi tidak bisa unggah ke album Google Photos hanya melalui tautan. Operator menyelesaikan unggah/penambahan album di Google Photos.
- Tidak ada pemeriksaan keberhasilan cloud. SENT merupakan catatan manual operator.
- Tampilan dan kemampuan penerima Google Photos dapat berubah. Ekspor ke galeri disediakan sebagai cadangan.
- Tamu mengikuti pengaturan berbagi/login/unduh Google Photos. Alpi tidak membuat galeri web sendiri pada alur ini.
- Tidak ada AI pemilih foto, deteksi wajah, retouch AI, video, editor frame interaktif atau sinkronisasi banyak fotografer.
- Foto hasil JPEG maksimal 2.560 px pada sisi panjang, kualitas 92; asli tidak diubah.
- Beta memakai tanda tangan debug. Produksi memerlukan signing tetap milik pemilik proyek.
