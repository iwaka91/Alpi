# Arsitektur dan alur foto

Status: rancangan teknis awal · 27 Oktober 2026

## Komponen

1. **Kamera bawaan Android** menyimpan foto asli ke koleksi media.
2. **APK Alpi** memantau perubahan foto ketika sesi aktif, membaca item yang diizinkan, membuat salinan ber-frame, dan mengelola antrean lokal.
3. **Backend album** memverifikasi hak unggah operator, menyimpan metadata acara/foto, dan membuat akses unduh untuk tamu.
4. **Penyimpanan objek** menampung thumbnail dan foto hasil. Foto asli di HP tetap sumber utama/arsip lokal.
5. **Web album** mengambil daftar foto terbit dan melayani pratinjau/unduhan. QR memuat URL album acara yang stabil.

## Urutan satu foto

1. Operator memilih acara dan frame lalu menekan Mulai Sesi; aplikasi mencatat penanda sesi.
2. Kamera bawaan memotret dan menyimpan foto. Aplikasi mendeteksi perubahan melalui koleksi media, menunggu penulisan selesai, lalu memeriksa waktu, jenis, folder/sumber, dan identitas item.
3. Foto baru masuk basis data antrean lokal. Jika izin hanya memberi akses foto terpilih, aplikasi menjelaskan bahwa deteksi otomatis memerlukan akses yang sesuai.
4. Pemroses menormalkan orientasi, membuat salinan ber-frame serta thumbnail, dan menyimpan hasil sementara secara atomik.
5. Berdasarkan mode acara, hasil ditahan untuk tinjau atau masuk antrean unggah. Backend menerbitkan foto hanya setelah upload dan pencatatan berhasil.
6. Web memuat foto terbaru; tamu melihat thumbnail dan mengunduh versi hasil saat dipilih.
7. Jika gagal, item tetap pada antrean dengan alasan dan percobaan berikutnya; tidak dianggap berhasil sebelum server mengonfirmasi.

## Android: batas dan pendekatan

- Gunakan MediaStore untuk menemukan foto kamera yang baru. Pemicu perubahan koleksi dapat memulai pekerjaan yang dijadwalkan; ketika sesi membutuhkan respons lebih cepat, pertimbangkan pemantauan aktif dengan notifikasi yang jelas.
- Pekerjaan di latar belakang tunduk pada batas Android dan pengaturan baterai pabrikan. Karena itu latensi tidak dijanjikan instan; uji langsung pada Xiaomi 13T dengan kamera bawaan, layar terkunci, dan koneksi berubah.
- Pengamatan koleksi media dapat mengirim lebih dari satu sinyal atau sinyal sebelum berkas selesai. Gunakan jeda singkat, pemeriksaan kesiapan, rekonsiliasi berkala selama sesi, dan kunci deduplikasi.
- Minta izin foto hanya sesuai kebutuhan inti; pada Android baru pengguna bisa memberi akses parsial, yang tidak cukup untuk menangkap semua foto baru secara otomatis.
- Simpan rahasia server di backend, bukan di APK. Operator terautentikasi; tamu hanya mendapat akses baca sesuai pengaturan acara.
- Simpan foto asal dan hasil dalam lokasi berbeda agar output frame tidak tertangkap kembali sebagai masukan.
- Jangan menaruh kredensial penyimpanan dengan hak tulis umum pada halaman web atau QR.

## Model data konseptual

| Entitas | Kolom penting |
| --- | --- |
| event | id, judul, waktu, slug/URL, status, aturan akses, frame aktif |
| frame | id, event_id, orientasi, versi, file PNG, parameter komposisi |
| photo | id, event_id, capture_time, source_key, process_version, status, storage_key, thumb_key, published_at |
| upload_queue (lokal) | source_key, event_id, status, attempts, next_retry, error, processed_path |
| operator | id, identitas login, hak akses ke acara |

Kunci unik pada pasangan acara dan identitas foto mencegah upload ulang membuat item ganda. URL album dibuat dari ID/slug yang stabil, tidak berubah setiap penambahan foto.

## Pilihan penyimpanan

Penyedia belum dipilih. Penilaian nanti harus memakai simulasi: jumlah foto × ukuran hasil + jumlah tamu × jumlah unduhan. Layanan gratis umumnya membatasi kapasitas simpan dan bandwidth; album yang ramai dapat melampaui batas. Pertimbangkan masa tayang dan ekspor arsip sejak awal.

## Privasi

Wajah tamu dapat terlihat dalam album. Tetapkan tanda pemberitahuan di lokasi acara, kebijakan akses (tautan publik atau PIN), cara meminta foto disembunyikan, dan tanggal penghapusan album sebelum produksi.

