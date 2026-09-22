# Makro by Gunz

Aplikasi Android (Kotlin, native) — bukan simulasi. Fitur benar-benar berjalan lewat AccessibilityService + WindowManager overlay milik sistem Android.

## Alur aplikasi
1. **Splash screen** — logo ⚡ + nama "Makro by Gunz" tampil ±1.5 detik lalu masuk ke menu utama.
2. **Menu utama** — 2 langkah wajib sebelum bisa mulai:
   - **Aktifkan Layanan Aksesibilitas** — tombol membuka `Settings > Aksesibilitas`, lalu kamu nyalakan "Makro by Gunz" di daftar layanan. (Ini yang berfungsi seperti "opsi pengembang" untuk mengizinkan aplikasi mengontrol ketukan layar — Android tidak mengizinkan auto-tap tanpa izin Accessibility Service ini.)
   - **Pilih Aplikasi** — menampilkan semua aplikasi yang terpasang di HP (`PackageManager.queryIntentActivities`), pilih salah satu, lalu konfirmasi lewat dialog.
3. Setelah dua syarat terpenuhi, tombol **Mulai Mode Mengambang** aktif. Ini akan meminta izin **"Tampil di atas aplikasi lain"** (overlay) jika belum diberikan, lalu menjalankan `FloatingService`.

## Fitur utama (mode mengambang)
- Muncul ikon bulat mengambang di layar (bisa terlihat di atas aplikasi apa pun).
- **Tekan sekali (tap singkat)** → toggle ON/OFF ketuk otomatis. Saat aktif, ikon mengirim ketukan nyata (`GestureDescription` via AccessibilityService `dispatchGesture`) berulang setiap 150ms ke posisi ikon — bukan simulasi visual, ini gesture yang benar-benar dikirim ke sistem.
- **Tahan lama (long-press ±500ms)** → masuk **mode edit posisi**: ikon bisa digeser bebas ke seluruh layar, dan tombol centang (✓) hijau muncul di bawahnya. Tekan tombol centang untuk **konfirmasi** posisi baru — posisi tersimpan (`SharedPreferences`) dan dipakai lagi saat service dijalankan ulang.

## Alur aplikasi (v2 — update terbaru)
1. **Splash screen** — logo custom + nama "Makro by Gunz" tampil ±1.5 detik lalu masuk ke menu utama.
2. **Menu utama**, urut dari atas:
   - **Status Aktivasi** (paling atas) — tombol untuk mengaktifkan Accessibility Service.
   - **Aplikasi Target** — baris horizontal berisi ikon aplikasi yang sudah dipilih. Bisa **digeser urutannya langsung** (drag & drop dengan animasi bawaan RecyclerView). Tap salah satu ikon → muncul pilihan **"Ganti aplikasi"** atau **"Hapus dari daftar"**. Di ujung kanan ada tombol **➕** untuk menambah aplikasi baru (bisa lebih dari satu aplikasi target).
   - **Atur Kecepatan Ketuk Otomatis** — membuka halaman baru dengan slider dari **Lambat** sampai **Sangat Cepat** (20ms–1000ms per ketukan), ada tombol **Konfirmasi** dan **Kembali**.
   - **Tombol Mulai** (paling bawah) — **nonaktif (abu-abu)** selama Accessibility Service belum aktif atau belum ada aplikasi dipilih. Begitu ditekan: otomatis **membuka aplikasi target pertama** dan langsung menyalakan **mode mengambang** di atasnya.

## Fitur mode mengambang (v2)
- **Double tap (ketuk 2x cepat)** pada ikon mengambang → toggle ON/OFF ketuk-otomatis (dipilih double-tap, bukan sekali tap, supaya tidak sengaja aktif saat menggeser ikon).
- **Geser (drag) ikon kapan saja** → ikon mengikuti jari secara realtime dan halus, tidak terikat harus masuk "mode edit" dulu — jadi tidak ada bug nyangkut saat digeser.
- **Tahan lama (long-press)** → masuk **mode edit**: muncul 2 tombol kecil di sekitar ikon:
  - ✓ hijau = **konfirmasi** posisi (tersimpan otomatis, tombol langsung hilang).
  - ✕ merah = **matikan** seluruhnya — service berhenti dan semua ikon/tombol overlay hilang otomatis.
- Notifikasi mode mengambang juga punya tombol "Matikan" langsung dari notifikasi.
- Kecepatan ketuk otomatis mengikuti nilai yang diset di halaman "Atur Kecepatan".

## Cara build APK LANGSUNG DARI HP (tanpa PC)

Build native Android (Gradle + Kotlin) tidak bisa jalan langsung di HP tanpa Android Studio/SDK. Cara paling praktis dari HP adalah **build di cloud pakai GitHub Actions** — workflow-nya sudah saya siapkan di `.github/workflows/build-apk.yml`, jadi HP kamu cuma tugasnya upload kode, sisanya di-build otomatis oleh server GitHub.

### Langkah-langkah (semua di HP)
1. **Ekstrak** ZIP `MakroByGunz.zip` ini di HP (pakai app Files/ZIP Extractor bawaan).
2. **Install Termux** (disarankan dari F-Droid, bukan Play Store karena versi Play Store sudah usang).
3. Buka Termux, jalankan:
   ```
   pkg update && pkg install git -y
   termux-setup-storage
   cd storage/downloads/MakroByGunz    # sesuaikan lokasi hasil ekstrak
   git init
   git add .
   git commit -m "init makro by gunz"
   ```
4. Buat **repository baru** di GitHub (lewat browser HP, buka github.com → New repository, kasih nama misal `makro-by-gunz`, jangan centang "add README").
5. Buat **Personal Access Token** (untuk login lewat Termux): GitHub → Settings → Developer settings → Personal access tokens → Generate new token (centang scope `repo`). Simpan tokennya.
6. Kembali ke Termux:
   ```
   git remote add origin https://github.com/USERNAME/makro-by-gunz.git
   git branch -M main
   git push -u origin main
   ```
   Saat diminta username/password: username = username GitHub kamu, password = **token** dari langkah 5 (bukan password akun).
7. Buka repo kamu di browser → tab **Actions** → akan ada proses "Build APK" berjalan otomatis (±3-5 menit).
8. Setelah selesai (centang hijau), buka run tersebut → scroll ke bawah ke bagian **Artifacts** → download `MakroByGunz-debug-apk` (berupa .zip berisi APK).
9. Ekstrak zip itu, dapat file `app-debug.apk` → install di HP (aktifkan dulu izin "Install aplikasi tidak dikenal" untuk browser/file manager kamu).

### Alternatif (kurang disarankan)
Ada app seperti **AIDE** yang bisa coding & build APK langsung di HP tanpa PC/internet berat, tapi dukungan Kotlin + Gradle modern + AccessibilityService di AIDE terbatas dan sering gagal untuk project seperti ini. GitHub Actions di atas jauh lebih reliable.

## Izin yang diminta ke pengguna (native Android, sesuai permintaan)
- **Accessibility Service** — wajib, untuk mengirim ketukan otomatis.
- **SYSTEM_ALERT_WINDOW (tampil di atas aplikasi lain)** — wajib, untuk bubble mengambang.
- **QUERY_ALL_PACKAGES** — untuk menampilkan semua aplikasi terpasang di daftar pilihan.

## Logo custom
Karena belum ada file logo darimu, saya buatkan logo custom sendiri (vector, bukan foto/dummy): lingkaran oranye dengan simbol petir putih di tengah — dipakai sebagai icon aplikasi (adaptive icon, muncul di launcher HP) maupun di splash screen. File-nya di:
- `res/drawable/ic_launcher_foreground.xml` + `ic_launcher_background.xml` + `mipmap-anydpi-v26/ic_launcher.xml` → icon aplikasi
- `res/drawable/logo_splash.xml` → logo di splash screen

Kalau nanti kamu punya logo sendiri (file PNG/JPG), kirim ke saya dan saya gantikan file-file di atas dengan logo kamu.

## Catatan teknis
- Karena ketukan dikirim tepat di titik ikon, di beberapa kondisi Android bisa saja ketukan mengenai jendela overlay itu sendiri, bukan aplikasi di bawahnya (batasan umum pada semua auto-clicker berbasis AccessibilityService). Jika ingin titik ketuk yang terpisah dari posisi ikon (misalnya ikon di pojok, titik ketuk di tengah layar), beri tahu saya dan saya tambahkan mode "atur titik ketuk terpisah".
- Nama paket: `com.gunz.makro`. Ganti `applicationId` di `app/build.gradle` bila perlu.
