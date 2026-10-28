package com.totogutomo.alpi;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import org.json.*;

/** Native, single-activity UI. The stock camera owns capture and its processing. */
public class MainActivity extends Activity {
  private static final int GREEN = Color.rgb(23, 107, 86),
      INK = Color.rgb(21, 44, 48),
      MUTED = Color.rgb(91, 112, 105),
      BG = Color.rgb(245, 247, 243);
  private LinearLayout root, body, nav;
  private ScrollView scroll;
  private Store store;
  private String page = "Beranda", detail = "", importField = "", importEvent = "";
  private final Handler handler = new Handler(Looper.getMainLooper());
  private boolean alive;
  private int photoLimit = 30;
  private final LinkedHashSet<String> selectedPhotos = new LinkedHashSet<>();
  private final BroadcastReceiver updates =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
          if (page.equals("Beranda") || page.equals("Foto") || page.equals("Siap Kirim")) refresh();
        }
      };

  private int dp(float n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  private GradientDrawable bg(int color, int radius) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    return d;
  }

  private void margin(View v, int bottom) {
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.bottomMargin = dp(bottom);
    v.setLayoutParams(p);
  }

  private LinearLayout column() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  private TextView text(String s, int size, int color, boolean bold) {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setLineSpacing(dp(3), 1);
    if (bold) t.setTypeface(null, Typeface.BOLD);
    margin(t, 8);
    return t;
  }

  private void title(String s) {
    body.addView(text(s, 27, INK, true));
  }

  private void hint(String s) {
    body.addView(text(s, 14, MUTED, false));
  }

  private void space() {
    View v = new View(this);
    body.addView(v, new LinearLayout.LayoutParams(-1, dp(12)));
  }

  private Button button(String label, boolean primary, Runnable action) {
    Button b = new Button(this);
    b.setText(label);
    b.setAllCaps(false);
    b.setTextSize(14);
    b.setTextColor(primary ? Color.WHITE : GREEN);
    b.setBackground(bg(primary ? GREEN : Color.rgb(230, 239, 231), 12));
    b.setMinHeight(dp(48));
    b.setPadding(dp(16), dp(8), dp(16), dp(8));
    margin(b, 10);
    b.setOnClickListener(v -> action.run());
    return b;
  }

  private void action(String label, boolean primary, Runnable run) {
    body.addView(button(label, primary, run));
  }

  private LinearLayout card() {
    LinearLayout l = column();
    l.setPadding(dp(18), dp(18), dp(18), dp(12));
    l.setBackground(bg(Color.WHITE, 18));
    margin(l, 14);
    body.addView(l);
    return l;
  }

  private void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_LONG).show();
  }

  private String selected() {
    String id = Prefs.get(this, "selected", "");
    if (store.event(id) == null && !store.events().isEmpty()) {
      id = store.events().get(0).optString("id");
      Prefs.set(this, "selected", id);
    }
    return id;
  }

  private JSONObject current() {
    return store.event(selected());
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    store = Store.get(this);
    alive = true;
    if (state != null) {
      page = state.getString("page", "Beranda");
      detail = state.getString("detail", "");
      importField = state.getString("importField", "");
      importEvent = state.getString("importEvent", "");
      ArrayList<String> selection = state.getStringArrayList("selection");
      if (selection != null) selectedPhotos.addAll(selection);
    }
    if (page.equals("Antrean")) page = "Siap Kirim";
    root = column();
    root.setBackgroundColor(BG);
    setContentView(root);
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          if (Build.VERSION.SDK_INT >= 30) {
            android.graphics.Insets bars =
                insets.getInsets(
                    WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout()
                        | WindowInsets.Type.ime());
            root.setPadding(bars.left, bars.top, bars.right, bars.bottom);
          } else
            root.setPadding(
                insets.getSystemWindowInsetLeft(),
                insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom());
          return insets;
        });
    LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER_VERTICAL);
    header.setPadding(dp(22), dp(10), dp(22), dp(6));
    TextView brand = text("alpi", 33, GREEN, true);
    header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
    TextView mark = text("ALBUM PHOTO LIVE\n0.3.0 BETA", 10, MUTED, true);
    mark.setGravity(Gravity.END);
    header.addView(mark);
    root.addView(header);
    scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    body = column();
    body.setPadding(dp(22), dp(12), dp(22), dp(22));
    scroll.addView(body);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    nav = new LinearLayout(this);
    nav.setPadding(dp(6), dp(6), dp(6), dp(8));
    root.addView(nav);
    if (Build.VERSION.SDK_INT >= 33)
      registerReceiver(
          updates, new IntentFilter("com.totogutomo.alpi.UPDATE"), Context.RECEIVER_NOT_EXPORTED);
    else registerLegacyUpdates();
    SyncJob.watch(this);
    Pipeline.kick(this);
    render();
  }

  // API 29-32 predate receiver export flags. A signature permission protects this fallback.
  @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
  private void registerLegacyUpdates() {
    registerReceiver(
        updates,
        new IntentFilter("com.totogutomo.alpi.UPDATE"),
        "com.totogutomo.alpi.INTERNAL",
        null);
  }

  @Override
  protected void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putString("page", page);
    b.putString("detail", detail);
    b.putString("importField", importField);
    b.putString("importEvent", importEvent);
    b.putStringArrayList("selection", new ArrayList<>(selectedPhotos));
  }

  @Override
  public void onResume() {
    super.onResume();
    if (body != null) {
      refresh();
      Pipeline.kick(this);
    }
  }

  @Override
  public void onDestroy() {
    alive = false;
    handler.removeCallbacksAndMessages(null);
    unregisterReceiver(updates);
    super.onDestroy();
  }

  @Override
  public void onBackPressed() {
    if (!detail.isEmpty()) {
      detail = "";
      render();
    } else if (!page.equals("Beranda")) {
      go("Beranda");
    } else super.onBackPressed();
  }

  private void go(String name) {
    page = name;
    detail = "";
    render();
    scroll.scrollTo(0, 0);
  }

  private void refresh() {
    int y = scroll.getScrollY();
    render();
    scroll.post(() -> scroll.scrollTo(0, y));
  }

  private void render() {
    body.removeAllViews();
    nav.removeAllViews();
    for (String name : new String[] {"Beranda", "Acara", "Foto", "Siap Kirim", "Lainnya"}) {
      TextView t = text(name, 11, page.equals(name) ? GREEN : MUTED, page.equals(name));
      t.setGravity(Gravity.CENTER);
      t.setMinHeight(dp(50));
      if (page.equals(name)) t.setBackground(bg(Color.rgb(224, 237, 225), 12));
      t.setOnClickListener(v -> go(name));
      nav.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
    }
    switch (page) {
      case "Beranda":
        home();
        break;
      case "Acara":
        if (detail.isEmpty()) events();
        else eventDetail();
        break;
      case "Foto":
        photos(false);
        break;
      case "Siap Kirim":
        photos(true);
        break;
      default:
        more();
    }
  }

  private void home() {
    title("Kamera Anda.\nMomen untuk semua.");
    hint("Potret dengan kamera bawaan. Alpi menyiapkan frame dan kiriman ke Google Photos.");
    space();
    JSONObject e = current(), session = store.active();
    LinearLayout c = card();
    c.addView(
        text(
            session == null
                ? "SESI BELUM AKTIF"
                : (LiveService.running ? "● SESI AKTIF" : "● SESI TERJADWAL"),
            11,
            GREEN,
            true));
    c.addView(text(e == null ? "Buat acara pertama" : e.optString("title"), 23, INK, true));
    c.addView(
        text(
            session == null
                ? "Pilih acara, pasang frame, lalu mulai sesi."
                : "Foto kamera yang baru akan diproses di HP."
                    + (LiveService.running
                        ? ""
                        : " Tekan Lanjutkan pemantauan untuk respons lebih cepat."),
            14,
            MUTED,
            false));
    c.addView(
        button(
            session == null ? "Mulai sesi" : "Buka kamera bawaan",
            true,
            session == null ? this::start : this::camera));
    if (session != null) {
      if (!LiveService.running)
        c.addView(button("Lanjutkan pemantauan", false, this::resumeService));
      c.addView(button("Jeda / akhiri sesi", false, this::stop));
    } else c.addView(button("Acara & frame", false, () -> go("Acara")));
    int ready = 0, review = 0, finished = 0;
    for (JSONObject q : store.queue()) {
      if (e == null || !e.optString("id").equals(q.optString("event_id"))) continue;
      if (q.optString("state").equals("READY")) ready++;
      if (q.optString("state").equals("REVIEW")) review++;
      if (q.optString("state").equals("SENT")) finished++;
    }
    LinearLayout stats = card();
    stats.addView(text(ready + " siap kirim  ·  " + review + " perlu ditinjau", 17, INK, true));
    stats.addView(text(finished + " ditandai selesai pada acara terpilih", 13, MUTED, false));
    stats.addView(button("Pilih & kirim hasil", true, () -> go("Siap Kirim")));
    pendingCard();
    if (e != null && !e.optString("google_album_url").isEmpty()) {
      action("Buka album Google Photos ↗", false, () -> open(e.optString("google_album_url")));
      action("QR album untuk tamu", false, () -> qr(e.optString("id")));
    }
    if (!Pipeline.allowed(this)) action("Berikan izin semua foto", false, this::permissions);
    String error = Prefs.get(this, "service_error", "");
    if (!error.isEmpty()) hint(error);
    String scan = Prefs.get(this, "scan_error", "");
    if (!scan.isEmpty()) hint("Pemantauan: " + scan);
    hint("Proses frame dan QR berjalan di HP. Unggah diselesaikan di Google Photos.");
  }

  private void events() {
    title("Acara & album");
    hint("Satu acara, satu QR yang tetap.");
    action("+ Buat acara", true, () -> editEvent(null));
    for (JSONObject e : store.events()) {
      LinearLayout c = card();
      boolean chosen = e.optString("id").equals(selected());
      c.addView(text((chosen ? "● " : "") + e.optString("title"), 20, INK, true));
      c.addView(
          text(
              e.optString("google_album_url").isEmpty()
                  ? "Album belum dihubungkan"
                  : e.optString("google_album_url"),
              12,
              MUTED,
              false));
      c.addView(
          button(
              "Kelola acara",
              false,
              () -> {
                detail = e.optString("id");
                render();
              }));
    }
  }

  private void eventDetail() {
    JSONObject e = store.event(detail);
    if (e == null) {
      detail = "";
      events();
      return;
    }
    title(e.optString("title"));
    hint(e.optString("description"));
    action(
        "← Semua acara",
        false,
        () -> {
          detail = "";
          render();
        });
    action(
        e.optString("id").equals(selected()) ? "✓ Acara terpilih" : "Pilih untuk sesi berikutnya",
        true,
        () -> {
          if (store.active() != null) {
            toast("Akhiri sesi aktif sebelum mengganti acara.");
            return;
          }
          Prefs.set(this, "selected", e.optString("id"));
          render();
        });
    action("Ubah acara & tautan album", false, () -> editEvent(e));
    FrameSpec spec = FrameSpec.fromEvent(e);
    LinearLayout studio = card();
    studio.addView(text("Kompresi & generator frame", 20, INK, true));
    studio.addView(text(spec.modeName() + " · " + spec.summary(), 14, MUTED, false));
    studio.addView(button("Kompresi hasil foto", false, () -> studio(e, "compression")));
    studio.addView(button("Generator Basic · teks, waktu & GPS", false, () -> studio(e, "basic")));
    studio.addView(button("Generator Pro · logo & tata letak", true, () -> studio(e, "pro")));
    studio.addView(
        text(
            "Pratinjau tersedia di studio. Mulai sesi baru setelah menyimpan pengaturan. Hasil lama"
                + " dapat diproses ulang dari foto asli.",
            12,
            MUTED,
            false));
    LinearLayout frames = card();
    frames.addView(text("Frame PNG & pita lama", 20, INK, true));
    frames.addView(
        text(
            "Impor PNG transparan dengan rasio yang sama dengan foto. Untuk memakainya, pilih mode"
                + " PNG / pita lama di tab Kompresi pada studio.",
            13,
            MUTED,
            false));
    for (String orientation : new String[] {"portrait", "landscape"}) {
      String name = orientation.equals("portrait") ? "potret" : "landscape";
      String file = e.optString(orientation);
      frames.addView(
          button(
              (file.isEmpty() ? "+ Tambah" : "✓ Ganti") + " frame " + name,
              false,
              () -> pickFrame(e.optString("id"), orientation)));
      if (!file.isEmpty()) {
        frames.addView(
            button(
                "Pratinjau frame " + name,
                false,
                () -> preview(Uri.fromFile(new File(file)), false)));
        frames.addView(
            button(
                "Lepas frame " + name,
                false,
                () -> {
                  store.eventSet(e.optString("id"), orientation, "");
                  render();
                }));
      }
    }
    frames.addView(
        button(
            "Teks pita: " + (e.optString("label").isEmpty() ? "tanpa teks" : e.optString("label")),
            false,
            () -> textBand(e)));
    Switch auto = new Switch(this);
    auto.setText(R.string.auto_ready);
    auto.setTextSize(13);
    auto.setPadding(0, dp(10), 0, dp(14));
    auto.setChecked(e.optInt("automatic") == 1);
    auto.setOnCheckedChangeListener(
        (b, on) -> store.eventSet(e.optString("id"), "automatic", on ? "1" : "0"));
    frames.addView(auto);
    frames.addView(
        text(
            "Saat mati, hasil menunggu tinjauan di menu Foto. Pengaturan frame dan mode disalin"
                + " saat Mulai Sesi; mulai sesi baru setelah perubahan.",
            12,
            MUTED,
            false));
    hint(
        "Simpan tautan berbagi album Google Photos milik Anda. Tautan dipakai untuk membuka album"
            + " dan QR tamu.");
    action(
        "Pilih hasil untuk dikirim",
        true,
        () -> {
          if (!e.optString("id").equals(selected())) {
            if (store.active() != null) {
              toast("Akhiri sesi aktif sebelum mengganti acara.");
              return;
            }
            Prefs.set(this, "selected", e.optString("id"));
            selectedPhotos.clear();
          }
          go("Siap Kirim");
        });
    String url = e.optString("google_album_url");
    if (!url.isEmpty()) {
      action("Tampilkan & bagikan QR", false, () -> qr(e.optString("id")));
      action("Buka album Google Photos ↗", false, () -> open(url));
      action("Bagikan tautan album", false, () -> shareText(url));
    }
  }

  private EditText input(LinearLayout parent, String label, String value, int type) {
    parent.addView(text(label, 12, MUTED, true));
    EditText field = new EditText(this);
    field.setText(value);
    field.setTextColor(INK);
    field.setTextSize(15);
    field.setInputType(type);
    field.setPadding(dp(10), dp(8), dp(10), dp(8));
    margin(field, 14);
    parent.addView(field);
    return field;
  }

  private void editEvent(JSONObject e) {
    LinearLayout form = column();
    form.setPadding(dp(20), dp(12), dp(20), dp(8));
    EditText name =
        input(
            form,
            "Nama acara",
            e == null ? "" : e.optString("title"),
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    EditText description =
        input(
            form,
            "Informasi acara",
            e == null ? "" : e.optString("description"),
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    EditText album =
        input(
            form,
            "Tautan berbagi album Google Photos (boleh diisi nanti)",
            e == null ? "" : e.optString("google_album_url"),
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
    form.addView(
        text(
            "Buat album di Google Photos, aktifkan berbagi tautan, lalu tempel tautannya di sini."
                + " Akses tamu mengikuti pengaturan Google Photos.",
            12,
            MUTED,
            false));
    ScrollView sc = new ScrollView(this);
    sc.addView(form);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(e == null ? "Acara baru" : "Ubah acara")
            .setView(sc)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan", null)
            .create();
    dialog.setOnShowListener(
        v ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    b -> {
                      String t = name.getText().toString().trim();
                      if (t.isEmpty() || t.length() > 120) {
                        name.setError("Isi nama acara, maksimal 120 karakter");
                        return;
                      }
                      if (description.length() > 1000) {
                        toast("Informasi maksimal 1.000 karakter.");
                        return;
                      }
                      String id =
                          e == null
                              ? UUID.randomUUID().toString().replace("-", "")
                              : e.optString("id");
                      try {
                        store.eventSave(
                            id, t, description.getText().toString(), album.getText().toString());
                      } catch (IllegalArgumentException ex) {
                        album.setError(ex.getMessage());
                        return;
                      }
                      if (selected().isEmpty()) Prefs.set(this, "selected", id);
                      page = "Acara";
                      detail = id;
                      dialog.dismiss();
                      render();
                    }));
    dialog.show();
  }

  private void textBand(JSONObject e) {
    LinearLayout f = column();
    f.setPadding(dp(20), dp(10), dp(20), dp(10));
    EditText field =
        input(
            f,
            "Teks di bagian bawah hasil (opsional)",
            e.optString("label"),
            InputType.TYPE_CLASS_TEXT);
    new AlertDialog.Builder(this)
        .setTitle("Pita kegiatan")
        .setView(f)
        .setNegativeButton("Batal", null)
        .setPositiveButton(
            "Simpan",
            (d, w) -> {
              store.eventSet(
                  e.optString("id"),
                  "label",
                  field.getText().toString().substring(0, Math.min(120, field.length())));
              render();
            })
        .show();
  }

  private void pickFrame(String event, String field) {
    importEvent = event;
    importField = field;
    Intent i =
        new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .setType("image/png")
            .addCategory(Intent.CATEGORY_OPENABLE);
    startActivityForResult(i, 40);
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request == 40 && result == RESULT_OK && data != null && data.getData() != null) {
      Uri uri = data.getData();
      String event = importEvent, field = importField;
      background(
          "Menyimpan frame…",
          () -> {
            File dir = new File(getFilesDir(), "frames");
            dir.mkdirs();
            File target = new File(dir, UUID.randomUUID() + ".png");
            try {
              try (InputStream in = getContentResolver().openInputStream(uri);
                  FileOutputStream out = new FileOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int n, total = 0;
                while ((n = in.read(buffer)) != -1) {
                  total += n;
                  if (total > 12 * 1024 * 1024) throw new IOException("Frame maksimal 12 MB");
                  out.write(buffer, 0, n);
                }
              }
              BitmapFactory.Options options = new BitmapFactory.Options();
              options.inJustDecodeBounds = true;
              BitmapFactory.decodeFile(target.getAbsolutePath(), options);
              if (!"image/png".equals(options.outMimeType)
                  || options.outWidth < 1
                  || options.outWidth > 8000
                  || options.outHeight > 8000)
                throw new IOException("Gunakan PNG valid, maksimal 8.000 piksel per sisi");
              store.eventSet(event, field, target.getAbsolutePath());
            } catch (Exception ex) {
              target.delete();
              throw ex;
            }
          },
          this::render);
    }
  }

  private void eventFilter() {
    JSONObject e = current();
    action(
        e == null ? "Pilih / buat acara" : "Acara: " + e.optString("title") + " ▾",
        false,
        () -> {
          List<JSONObject> events = store.events();
          if (events.isEmpty()) {
            go("Acara");
            return;
          }
          String[] names = new String[events.size()];
          for (int n = 0; n < events.size(); n++) names[n] = events.get(n).optString("title");
          new AlertDialog.Builder(this)
              .setTitle("Pilih acara")
              .setItems(
                  names,
                  (d, which) -> {
                    String id = events.get(which).optString("id");
                    if (store.active() != null && !id.equals(selected())) {
                      toast("Akhiri sesi aktif sebelum mengganti acara.");
                      return;
                    }
                    Prefs.set(this, "selected", id);
                    selectedPhotos.clear();
                    photoLimit = 30;
                    refresh();
                  })
              .setNegativeButton("Batal", null)
              .show();
        });
  }

  private List<JSONObject> readyPhotos() {
    return store.rows(
        "SELECT * FROM queue WHERE event_id=? AND state='READY' ORDER BY created", selected());
  }

  private void photos(boolean queueOnly) {
    title(queueOnly ? "Siap Kirim" : "Foto acara");
    hint(
        queueOnly
            ? "Kirim hingga 30 hasil foto sekaligus. Selesaikan unggah dan penambahan album di"
                + " Google Photos."
            : "Tinjau hasil, setujui, atau lewati foto sebelum dibagikan.");
    eventFilter();
    if (queueOnly) pendingCard();
    List<JSONObject> ready = readyPhotos();
    HashSet<String> valid = new HashSet<>();
    for (JSONObject q : ready) valid.add(q.optString("id"));
    selectedPhotos.retainAll(valid);
    if (queueOnly && !ready.isEmpty()) {
      TextView count =
          text(
              getString(R.string.selection_count, selectedPhotos.size(), ready.size()),
              16,
              INK,
              true);
      body.addView(count);
      action(
          "Pilih 30 pertama / semua jika kurang",
          false,
          () -> {
            selectedPhotos.clear();
            for (JSONObject q : ready) {
              if (selectedPhotos.size() == Delivery.MAX_BATCH) break;
              selectedPhotos.add(q.optString("id"));
            }
            refresh();
          });
      action(
          "Batal pilih semua",
          false,
          () -> {
            selectedPhotos.clear();
            refresh();
          });
      action("Kirim pilihan ke Google Photos", true, this::sendSelected);
      action(
          "Terapkan kompresi / frame terbaru ke pilihan",
          false,
          () -> {
            if (selectedPhotos.isEmpty()) {
              toast("Pilih foto dahulu.");
              return;
            }
            ArrayList<String> ids = new ArrayList<>(selectedPhotos);
            new AlertDialog.Builder(this)
                .setTitle("Proses ulang " + ids.size() + " foto?")
                .setMessage(
                    "Hasil dibuat kembali dari foto asli memakai kompresi dan frame acara saat ini."
                        + " Pilihan akan menunggu proses lagi.")
                .setPositiveButton(
                    "Proses ulang",
                    (d, w) -> {
                      try {
                        store.reprocess(selected(), ids);
                        selectedPhotos.clear();
                        Pipeline.kick(this);
                        refresh();
                      } catch (Exception ex) {
                        toast(Pipeline.safe(ex));
                      }
                    })
                .setNegativeButton("Batal", null)
                .show();
          });
      action(
          "Simpan pilihan ke galeri HP",
          false,
          () -> exportGallery(new ArrayList<>(selectedPhotos)));
      JSONObject e = current();
      if (e != null && !e.optString("google_album_url").isEmpty())
        action("Buka album tujuan ↗", false, () -> open(e.optString("google_album_url")));
      hint(
          "Simpan ke galeri menjadi cadangan jika Google Photos tidak menerima kiriman langsung."
              + " Folder hasil: Pictures/Alpi.");
      renderPhotos(true, count);
    } else renderPhotos(queueOnly, null);
    if (!queueOnly) action("Buka Siap Kirim", true, () -> go("Siap Kirim"));
  }

  private void renderPhotos(boolean readyOnly, TextView count) {
    int shown = 0;
    for (JSONObject q : store.queue()) {
      if (!selected().equals(q.optString("event_id"))) continue;
      String state = q.optString("state"), id = q.optString("id"), output = q.optString("output");
      if (readyOnly && !state.equals("READY")) continue;
      if (++shown > photoLimit) {
        action(
            "Muat 30 foto berikutnya",
            false,
            () -> {
              photoLimit += 30;
              refresh();
            });
        break;
      }
      LinearLayout c = card();
      c.addView(
          text(label(state), 15, state.equals("ERROR") ? Color.rgb(165, 65, 45) : GREEN, true));
      c.addView(
          text(
              android.text.format.DateFormat.format(
                      "dd MMM · HH:mm:ss", Pipeline.number(q, "captured"))
                  + " · "
                  + id.substring(0, 8),
              12,
              MUTED,
              false));
      if (readyOnly) {
        CheckBox choice = new CheckBox(this);
        choice.setText(R.string.choose_photo);
        choice.setChecked(selectedPhotos.contains(id));
        choice.setMinHeight(dp(48));
        choice.setOnCheckedChangeListener(
            (b, checked) -> {
              if (checked && selectedPhotos.size() >= Delivery.MAX_BATCH) {
                choice.setChecked(false);
                toast("Maksimal 30 foto per kiriman. Kirim pilihan saat ini dahulu.");
                return;
              }
              if (checked) selectedPhotos.add(id);
              else selectedPhotos.remove(id);
              if (count != null)
                count.setText(
                    getString(
                        R.string.selection_count, selectedPhotos.size(), readyPhotos().size()));
            });
        c.addView(choice);
      }
      if (!output.isEmpty() && new File(output).isFile()) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(output, bounds);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 360)
          options.inSampleSize *= 2;
        Bitmap thumb = BitmapFactory.decodeFile(output, options);
        ImageView image = new ImageView(this);
        image.setImageBitmap(thumb);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setContentDescription("Pratinjau hasil frame. Ketuk untuk memperbesar.");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(170));
        lp.bottomMargin = dp(12);
        c.addView(image, lp);
        image.setOnClickListener(
            v -> previewImage(Uri.fromFile(new File(output)), false, "Hasil foto"));
        c.addView(
            button(
                "Lihat hasil foto",
                false,
                () -> previewImage(Uri.fromFile(new File(output)), false, "Hasil foto")));
      }
      if (!q.optString("stats").isEmpty()) {
        try {
          JSONObject stats = new JSONObject(q.optString("stats"));
          long original = stats.optLong("source"), result = stats.optLong("bytes");
          String size =
              "Hasil "
                  + FrameSpec.size(result)
                  + " · "
                  + stats.optInt("width")
                  + "×"
                  + stats.optInt("height")
                  + " px";
          if (original > 0) {
            size = FrameSpec.size(original) + " → " + size;
            if (result < original)
              size += " · hemat " + Math.round(100d * (original - result) / original) + "%";
          }
          c.addView(text(size, 12, GREEN, false));
          JSONArray notes = stats.optJSONArray("notes");
          if (notes != null)
            for (int n = 0; n < notes.length(); n++)
              c.addView(text(notes.optString(n), 12, MUTED, false));
        } catch (JSONException ignored) {
        }
      }
      if (!q.optString("error").isEmpty()) c.addView(text(q.optString("error"), 12, MUTED, false));
      c.addView(
          button(
              "Lihat foto asli",
              false,
              () -> previewImage(Uri.parse(q.optString("uri")), false, "Foto asli")));
      if (state.equals("REVIEW"))
        c.addView(
            button(
                "Setujui → Siap Kirim",
                true,
                () -> {
                  store.state(id, "READY", null, "", 0);
                  refresh();
                }));
      if (state.equals("REVIEW") || state.equals("READY"))
        c.addView(
            button(
                "Lewati foto ini",
                false,
                () -> {
                  store.state(id, "REJECTED", null, "", 0);
                  selectedPhotos.remove(id);
                  refresh();
                }));
      if (state.equals("REJECTED"))
        c.addView(
            button(
                "Kembalikan untuk ditinjau",
                false,
                () -> {
                  store.state(id, "REVIEW", null, "", 0);
                  refresh();
                }));
      if (state.equals("REVIEW")
          || state.equals("READY")
          || state.equals("ERROR")
          || state.equals("REJECTED"))
        c.addView(button("Proses ulang · kompresi & frame sekarang", false, () -> reprocess(q)));
      if (state.equals("SENT") || state.equals("DONE"))
        c.addView(
            button(
                "Siapkan lagi untuk dikirim",
                false,
                () ->
                    new AlertDialog.Builder(this)
                        .setTitle("Siapkan foto ini lagi?")
                        .setMessage(
                            "Pengiriman ulang dapat membuat salinan lain di Google Photos. Periksa"
                                + " album terlebih dahulu.")
                        .setPositiveButton(
                            "Siapkan",
                            (d, w) -> {
                              store.state(id, "READY", null, "", 0);
                              refresh();
                            })
                        .setNegativeButton("Batal", null)
                        .show()));
    }
    if (shown == 0)
      hint(
          readyOnly
              ? "Belum ada foto siap kirim. Setujui hasil di menu Foto, atau aktifkan langsung Siap"
                  + " Kirim sebelum memulai sesi."
              : "Belum ada foto untuk acara ini. Mulai sesi, lalu potret dengan kamera bawaan.");
  }

  private void studio(JSONObject event, String tab) {
    startActivity(
        new Intent(this, FrameEditorActivity.class)
            .putExtra("event_id", event.optString("id"))
            .putExtra("tab", tab));
  }

  private void reprocess(JSONObject q) {
    try {
      store.reprocess(q.optString("event_id"), Collections.singleton(q.optString("id")));
      selectedPhotos.remove(q.optString("id"));
      Pipeline.kick(this);
      refresh();
    } catch (Exception e) {
      toast(Pipeline.safe(e));
    }
  }

  private void sendSelected() {
    if (selectedPhotos.isEmpty()) {
      toast("Pilih foto yang akan dikirim.");
      return;
    }
    JSONObject e = current();
    if (e == null || e.optString("google_album_url").isEmpty()) {
      toast("Simpan tautan berbagi album pada acara terlebih dahulu.");
      if (e != null) editEvent(e);
      return;
    }
    ArrayList<String> ids = new ArrayList<>(selectedPhotos);
    JSONObject[] batch = new JSONObject[1];
    background(
        "Menyiapkan salinan kiriman…",
        () -> {
          batch[0] = Delivery.prepare(this, e.optString("id"), ids);
        },
        () -> {
          selectedPhotos.clear();
          refresh();
          launchBatch(batch[0]);
        });
  }

  private void launchBatch(JSONObject batch) {
    try {
      Intent direct = Delivery.intent(this, batch, true);
      if (direct.resolveActivity(getPackageManager()) != null) startActivity(direct);
      else {
        toast("Pilih Google Photos jika tersedia. Jika tidak, simpan hasil ke galeri HP.");
        startActivity(
            Intent.createChooser(Delivery.intent(this, batch, false), "Kirim hasil foto Alpi"));
      }
    } catch (Exception e) {
      toast("Kiriman belum dibuka. " + Pipeline.safe(e));
    }
  }

  private void pendingCard() {
    JSONObject batch = store.pendingBatch();
    if (batch == null) return;
    JSONObject event = store.event(batch.optString("event_id"));
    int count = new JSONArraySafe(batch.optString("photo_ids")).size();
    LinearLayout c = card();
    c.addView(text("Menunggu konfirmasi kiriman", 20, INK, true));
    c.addView(
        text(
            count + " foto · " + (event == null ? "Acara" : event.optString("title")),
            14,
            GREEN,
            true));
    c.addView(
        text(
            "Selesaikan unggah di Google Photos, lalu tambahkan foto ke album tujuan. Setelah foto"
                + " terlihat di album, tandai selesai di Alpi. Membuka Google Photos saja belum"
                + " berarti berhasil.",
            13,
            MUTED,
            false));
    if (!batch.optString("album_url").isEmpty())
      c.addView(
          button("Buka album → Tambahkan foto ↗", false, () -> open(batch.optString("album_url"))));
    c.addView(
        button(
            "Sudah masuk album · tandai selesai",
            true,
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("Foto sudah terlihat di album?")
                    .setMessage(
                        "Tandai hanya setelah memeriksa seluruh foto kiriman. Alpi mencatat"
                            + " konfirmasi Anda dan tidak dapat memeriksa unggahan Google Photos.")
                    .setPositiveButton("Ya, tandai selesai", (d, w) -> finishBatch(batch, true))
                    .setNegativeButton("Belum", null)
                    .show()));
    c.addView(button("Buka kiriman yang sama lagi", false, () -> launchBatch(batch)));
    c.addView(
        button(
            "Simpan kiriman ke galeri HP",
            false,
            () -> exportGallery(new JSONArraySafe(batch.optString("photo_ids")).values)));
    c.addView(
        button("Belum jadi / kembalikan ke Siap Kirim", false, () -> finishBatch(batch, false)));
  }

  private static class JSONArraySafe {
    final ArrayList<String> values = new ArrayList<>();

    JSONArraySafe(String text) {
      try {
        JSONArray a = new JSONArray(text);
        for (int n = 0; n < a.length(); n++) values.add(a.getString(n));
      } catch (JSONException ignored) {
      }
    }

    int size() {
      return values.size();
    }
  }

  private void finishBatch(JSONObject batch, boolean confirmed) {
    try {
      store.finishBatch(batch.optString("id"), confirmed);
      selectedPhotos.clear();
      refresh();
      toast(confirmed ? "Kiriman ditandai selesai oleh Anda." : "Foto kembali ke Siap Kirim.");
    } catch (Exception e) {
      toast(Pipeline.safe(e));
    }
  }

  private void exportGallery(Collection<String> ids) {
    if (ids.isEmpty()) {
      toast("Pilih foto dahulu.");
      return;
    }
    ArrayList<String> copy = new ArrayList<>(ids);
    background(
        "Menyimpan salinan hasil ke galeri…",
        () -> {
          JSONObject pending = store.pendingBatch();
          if (pending == null) {
            JSONObject first = store.photo(copy.get(0));
            if (first == null) throw new IOException("Foto tidak ditemukan.");
            Delivery.prepare(this, first.optString("event_id"), copy);
          } else {
            Set<String> staged =
                new HashSet<>(new JSONArraySafe(pending.optString("photo_ids")).values);
            if (!staged.equals(new HashSet<>(copy)))
              throw new IOException(
                  "Selesaikan atau kembalikan kiriman sebelumnya terlebih dahulu.");
          }
          Delivery.gallery(this, copy);
        },
        () -> {
          selectedPhotos.clear();
          toast(
              "Hasil tersedia di Pictures/Alpi. Buka Google Photos → folder di perangkat, lalu"
                  + " tambahkan ke album.");
          refresh();
        });
  }

  static String label(String s) {
    switch (s) {
      case "NEW":
        return "Menunggu proses";
      case "PROCESSING":
        return "Memasang frame";
      case "REVIEW":
        return "Perlu tinjauan";
      case "READY":
        return "Siap dikirim";
      case "SHARED":
        return "Menunggu konfirmasi kiriman";
      case "SENT":
        return "Ditandai selesai oleh operator";
      case "DONE":
        return "Riwayat unggah server versi lama";
      case "REJECTED":
        return "Dilewati";
      case "ERROR":
        return "Perlu perhatian";
      default:
        return s;
    }
  }

  private void more() {
    title("Pengaturan & bantuan");
    action("Folder kamera", true, this::settings);
    action("Izin foto & notifikasi", false, this::permissions);
    action(
        "Pengaturan aplikasi Android",
        false,
        () ->
            startActivity(
                new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()))));
    LinearLayout c = card();
    c.addView(text("Cara memakai Alpi", 21, INK, true));
    c.addView(
        text(
            "1. Buat acara dan tempel tautan berbagi album Google Photos.\n"
                + "2. Atur kompresi dan generator Basic/Pro, atau pakai PNG/pita lama.\n"
                + "3. Mulai sesi, lalu buka kamera bawaan.\n"
                + "4. Tinjau hasil di Foto, lalu pilih foto di Siap Kirim.\n"
                + "5. Kirim ke Google Photos; selesaikan unggah dan tambahkan ke album di sana.\n"
                + "6. Cek album, lalu tandai kiriman selesai. QR tamu tetap memakai tautan album.",
            14,
            MUTED,
            false));
    c.addView(
        text(
            "Jika Google Photos tidak menerima kiriman, gunakan Simpan ke galeri HP lalu pilih"
                + " hasil dari folder Pictures/Alpi di Google Photos. Cadangan folder tidak"
                + " otomatis memasukkan foto ke album tertentu.",
            13,
            MUTED,
            false));
    c.addView(
        text(
            "Saat sesi dijeda, foto baru tidak dipantau. Hasil dan pilihan kiriman tetap di HP."
                + " Mematikan paksa Alpi menghentikan pekerjaan sampai dibuka lagi. Periksa"
                + " notifikasi saat memakai penghemat baterai.",
            13,
            MUTED,
            false));
    hint(
        "Alpi · Album Photo Live\n"
            + "Versi 0.3.0-beta · Tanpa server dan Google Cloud Console.\n"
            + "Kompresi JPEG dan frame diatur per acara melalui Studio frame. Foto asli tidak"
            + " diubah.");
  }

  private void settings() {
    LinearLayout f = column();
    f.setPadding(dp(20), dp(12), dp(20), dp(12));
    EditText folder =
        input(
            f,
            "Folder kamera di MediaStore",
            Prefs.get(this, "folder", "DCIM/Camera/"),
            InputType.TYPE_CLASS_TEXT);
    f.addView(
        text(
            "Biasanya DCIM/Camera/. Folder hasil Pictures/Alpi tidak ikut dipantau. Ubah saat sesi"
                + " berhenti.",
            13,
            MUTED,
            false));
    AlertDialog d =
        new AlertDialog.Builder(this)
            .setTitle("Folder kamera")
            .setView(f)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan", null)
            .create();
    d.setOnShowListener(
        v ->
            d.getButton(-1)
                .setOnClickListener(
                    b -> {
                      if (store.active() != null) {
                        toast("Akhiri sesi sebelum mengganti folder kamera.");
                        return;
                      }
                      String path = folder.getText().toString().trim();
                      if (!path.startsWith("DCIM/") || !path.endsWith("/") || path.contains("..")) {
                        folder.setError("Folder harus diawali DCIM/ dan diakhiri /.");
                        return;
                      }
                      Prefs.set(this, "folder", path);
                      d.dismiss();
                      Pipeline.kick(this);
                      render();
                    }));
    d.show();
  }

  private void permissions() {
    ArrayList<String> needs = new ArrayList<>();
    if (!Pipeline.allowed(this)) {
      needs.add(Pipeline.permission());
      if (Build.VERSION.SDK_INT >= 34)
        needs.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
    }
    if (Build.VERSION.SDK_INT >= 33
        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) needs.add(Manifest.permission.POST_NOTIFICATIONS);
    if (needs.isEmpty()) {
      toast("Izin foto dan notifikasi tersedia.");
      return;
    }
    new AlertDialog.Builder(this)
        .setTitle("Akses foto kamera")
        .setMessage(
            "Alpi perlu akses semua foto untuk mendeteksi jepretan baru secara otomatis saat sesi"
                + " aktif. Aplikasi hanya memproses foto pada folder kamera yang dipilih sejak sesi"
                + " dimulai.")
        .setPositiveButton(
            "Lanjutkan", (d, w) -> requestPermissions(needs.toArray(new String[0]), 42))
        .setNegativeButton("Batal", null)
        .show();
  }

  @Override
  public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(request, permissions, results);
    refresh();
    if (!Pipeline.allowed(this))
      toast("Pilih izin semua foto pada Pengaturan aplikasi untuk pemantauan otomatis.");
  }

  private void start() {
    if (current() == null) {
      go("Acara");
      return;
    }
    if (!Pipeline.allowed(this)) {
      permissions();
      return;
    }
    JSONObject e = current();
    background(
        "Menyiapkan sesi…",
        () -> {
          store.begin(store.event(e.optString("id")), Pipeline.baseline(this));
        },
        () -> {
          resumeService();
          refresh();
        });
  }

  private void resumeService() {
    try {
      startForegroundService(new Intent(this, LiveService.class));
      handler.postDelayed(this::refresh, 700);
    } catch (Exception e) {
      toast(Pipeline.safe(e));
    }
  }

  private void stop() {
    store.end();
    stopService(new Intent(this, LiveService.class));
    Pipeline.kick(this);
    SyncJob.watch(this);
    refresh();
  }

  private void camera() {
    try {
      startActivity(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA));
    } catch (ActivityNotFoundException e) {
      toast("Buka kamera bawaan dari layar utama HP.");
    }
  }

  private void open(String url) {
    try {
      String link = AlbumLinks.normalize(url);
      if (link.isEmpty()) {
        toast("Tautan album belum diisi.");
        return;
      }
      Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
      i.setPackage(Delivery.PHOTOS_PACKAGE);
      try {
        startActivity(i);
      } catch (ActivityNotFoundException unavailable) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
      }
    } catch (Exception e) {
      toast("Album tidak dapat dibuka. Periksa tautan dan aplikasi Google Photos / browser.");
    }
  }

  private void shareText(String value) {
    Intent i =
        new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, value);
    startActivity(Intent.createChooser(i, "Bagikan album Alpi"));
  }

  private void shareFile(File file, String group) {
    Uri u = Uri.parse("content://" + getPackageName() + ".files/" + group + "/" + file.getName());
    Intent i =
        new Intent(Intent.ACTION_SEND)
            .setType(file.getName().endsWith(".png") ? "image/png" : "image/jpeg")
            .putExtra(Intent.EXTRA_STREAM, u)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    i.setClipData(ClipData.newUri(getContentResolver(), "Alpi", u));
    startActivity(Intent.createChooser(i, "Simpan / bagikan"));
  }

  private void qr(String id) {
    JSONObject e = store.event(id);
    if (e == null || e.optString("google_album_url").isEmpty()) {
      toast("Simpan tautan berbagi album dahulu.");
      return;
    }
    File[] file = new File[1];
    background(
        "Membuat QR album di HP…",
        () -> {
          file[0] = Delivery.qr(this, e.optString("google_album_url"), id);
        },
        () -> {
          Bitmap image = BitmapFactory.decodeFile(file[0].getAbsolutePath());
          ImageView v = new ImageView(this);
          v.setImageBitmap(image);
          v.setAdjustViewBounds(true);
          v.setPadding(dp(24), dp(24), dp(24), dp(24));
          AlertDialog dialog =
              new AlertDialog.Builder(this)
                  .setTitle("QR album · " + e.optString("title"))
                  .setView(v)
                  .setPositiveButton("Bagikan QR", (d, w) -> shareFile(file[0], "share"))
                  .setNegativeButton("Tutup", null)
                  .create();
          dialog.setOnDismissListener(d -> image.recycle());
          dialog.show();
        });
  }

  private void preview(Uri uri, boolean result) {
    previewImage(uri, result, result ? "Hasil foto" : "Frame PNG");
  }

  private void previewImage(Uri uri, boolean result, String heading) {
    final Bitmap[] decoded = new Bitmap[1];
    background(
        "Memuat pratinjau…",
        () -> {
          decoded[0] = Pipeline.decode(this, uri, 1400);
        },
        () -> {
          Bitmap image = decoded[0];
          ImageView view = new ImageView(this);
          view.setImageBitmap(image);
          view.setAdjustViewBounds(true);
          ScrollView sc = new ScrollView(this);
          sc.addView(view);
          AlertDialog.Builder b =
              new AlertDialog.Builder(this)
                  .setTitle(heading)
                  .setView(sc)
                  .setNegativeButton("Tutup", null);
          if (result)
            b.setPositiveButton(
                "Simpan / bagikan", (d, w) -> shareFile(new File(uri.getPath()), "results"));
          AlertDialog d = b.create();
          d.setOnDismissListener(v -> image.recycle());
          d.show();
        });
  }

  interface Work {
    void run() throws Exception;
  }

  private void background(String status, Work work, Runnable success) {
    toast(status);
    Pipeline.IO.execute(
        () -> {
          try {
            work.run();
            handler.post(
                () -> {
                  if (alive) {
                    success.run();
                  }
                });
          } catch (Exception e) {
            handler.post(
                () -> {
                  if (alive) toast(Pipeline.safe(e));
                });
          }
        });
  }
}
