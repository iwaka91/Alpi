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
import java.net.URL;
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
  private int photoLimit = 80;
  private final BroadcastReceiver updates =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
          if (page.equals("Beranda") || page.equals("Foto") || page.equals("Antrean")) refresh();
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
    }
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
    TextView mark = text("ALBUM PHOTO LIVE\n0.1.0 BETA", 10, MUTED, true);
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
    for (String name : new String[] {"Beranda", "Acara", "Foto", "Antrean", "Lainnya"}) {
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
      case "Antrean":
        photos(true);
        break;
      default:
        more();
    }
  }

  private void home() {
    title("Siap mengabadikan\nmomen hari ini?");
    hint("Potret dengan kamera bawaan. Alpi mengurus frame dan album Anda.");
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
                : "Foto baru dari folder kamera akan masuk antrean. "
                    + (LiveService.running
                        ? ""
                        : "Buka Alpi dan tekan Lanjutkan pemantauan untuk respons lebih cepat."),
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
    int all = 0, done = 0, review = 0, waiting = 0;
    for (JSONObject q : store.queue()) {
      all++;
      String state = q.optString("state");
      if (state.equals("DONE")) done++;
      else if (state.equals("REVIEW")) review++;
      else waiting++;
    }
    LinearLayout stats = card();
    stats.addView(text(done + " terunggah  ·  " + review + " perlu ditinjau", 17, INK, true));
    stats.addView(
        text(waiting + " dalam antrean  /  " + all + " foto terdeteksi", 13, MUTED, false));
    stats.addView(button("Lihat antrean", false, () -> go("Antrean")));
    if (!Pipeline.allowed(this)) action("Berikan izin semua foto", false, this::permissions);
    String error = Prefs.get(this, "service_error", "");
    if (!error.isEmpty()) hint(error);
    String scan = Prefs.get(this, "scan_error", "");
    if (!scan.isEmpty()) hint("Pemantauan: " + scan);
    if (Prefs.get(this, "server", "").isEmpty())
      action("Hubungkan server album", false, this::settings);
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
              e.optString("album_url").isEmpty()
                  ? "Album belum dihubungkan"
                  : e.optString("album_url"),
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
    action("Ubah nama, informasi & PIN", false, () -> editEvent(e));
    LinearLayout frames = card();
    frames.addView(text("Frame & postproses", 20, INK, true));
    frames.addView(
        text(
            "Impor PNG transparan dengan rasio yang sama dengan foto. Foto asli tetap tersimpan.",
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
    auto.setText(R.string.publish_automatically);
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
    action(
        "Hubungkan / perbarui album online",
        true,
        () ->
            background(
                "Menghubungkan album…",
                () -> {
                  new Api(this).sync(store.event(e.optString("id")));
                },
                this::render));
    String url = e.optString("album_url");
    if (!url.isEmpty()) {
      action("Tampilkan & bagikan QR", false, () -> qr(e.optString("id")));
      action("Buka album tamu ↗", false, () -> open(url));
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
    EditText pin =
        input(
            form,
            "PIN tamu (kosong = akses melalui tautan)",
            e == null ? "" : e.optString("pin"),
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
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
                      if (description.length() > 1000 || pin.length() > 32) {
                        toast("Informasi maksimal 1.000 karakter; PIN maksimal 32.");
                        return;
                      }
                      String id =
                          e == null
                              ? UUID.randomUUID().toString().replace("-", "")
                              : e.optString("id");
                      store.eventSave(
                          id, t, description.getText().toString(), pin.getText().toString());
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

  private void photos(boolean queueOnly) {
    title(queueOnly ? "Antrean unggah" : "Foto acara");
    hint(
        queueOnly
            ? "Antrean disimpan di HP, termasuk saat koneksi terputus."
            : "Tinjau hasil frame sebelum mengirimkannya ke tamu.");
    action(
        "Periksa & lanjutkan antrean",
        true,
        () -> {
          Pipeline.kick(this);
          SyncJob.retry(this);
          toast("Antrean diperiksa.");
        });
    List<JSONObject> items = store.queue();
    int shown = 0;
    for (JSONObject q : items) {
      if (queueOnly && q.optString("state").equals("DONE")) continue;
      if (++shown > photoLimit) {
        action(
            "Muat 80 foto berikutnya",
            false,
            () -> {
              photoLimit += 80;
              refresh();
            });
        break;
      }
      String state = q.optString("state");
      LinearLayout c = card();
      c.addView(text(q.optString("title"), 16, INK, true));
      c.addView(
          text(
              label(state) + (q.optInt("visible") == 0 ? " · disembunyikan" : ""),
              13,
              state.equals("ERROR") ? Color.rgb(165, 65, 45) : GREEN,
              true));
      c.addView(
          text(
              android.text.format.DateFormat.format(
                      "dd MMM · HH:mm:ss", Pipeline.number(q, "captured"))
                  + " · "
                  + q.optString("id").substring(0, 8),
              12,
              MUTED,
              false));
      if (!q.optString("error").isEmpty()) c.addView(text(q.optString("error"), 12, MUTED, false));
      c.addView(
          button(
              "Lihat foto asli",
              false,
              () -> previewImage(Uri.parse(q.optString("uri")), false, "Foto asli")));
      String output = q.optString("output");
      if (!output.isEmpty())
        c.addView(
            button("Lihat hasil foto", false, () -> preview(Uri.fromFile(new File(output)), true)));
      if (state.equals("REVIEW"))
        c.addView(
            button(
                "Setujui & unggah",
                true,
                () -> {
                  store.state(q.optString("id"), "READY", null, "", 0);
                  Pipeline.kick(this);
                  SyncJob.retry(this);
                  refresh();
                }));
      if (state.equals("ERROR"))
        c.addView(
            button(
                "Coba lagi",
                true,
                () -> {
                  store
                      .getWritableDatabase()
                      .execSQL(
                          "UPDATE queue SET attempts=0 WHERE id=?",
                          new Object[] {q.optString("id")});
                  store.state(q.optString("id"), output.isEmpty() ? "NEW" : "READY", null, "", 0);
                  Pipeline.kick(this);
                  SyncJob.retry(this);
                  refresh();
                }));
      if (state.equals("REVIEW") || state.equals("ERROR"))
        c.addView(
            button(
                "Proses ulang dengan frame sekarang",
                false,
                () -> {
                  JSONObject e = store.event(q.optString("event_id"));
                  android.content.ContentValues v = new android.content.ContentValues();
                  v.put("config", e.toString());
                  v.put("attempts", 0);
                  store
                      .getWritableDatabase()
                      .update("queue", v, "id=?", new String[] {q.optString("id")});
                  store.state(q.optString("id"), "NEW", "", "", 0);
                  Pipeline.kick(this);
                  refresh();
                }));
      if (state.equals("DONE")) {
        boolean visible = q.optInt("visible") == 1;
        c.addView(
            button(
                visible ? "Sembunyikan dari album" : "Tampilkan kembali",
                false,
                () ->
                    background(
                        "Memperbarui album…",
                        () -> {
                          new Api(this).visible(q, !visible);
                          store.visibility(q.optString("id"), !visible);
                        },
                        this::refresh)));
      }
    }
    if (shown == 0) hint("Belum ada foto di sini. Mulai sesi, lalu potret dengan kamera bawaan.");
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
        return "Menunggu unggah";
      case "UPLOADING":
        return "Mengunggah";
      case "DONE":
        return "Terunggah";
      case "ERROR":
        return "Perlu perhatian";
      default:
        return s;
    }
  }

  private void more() {
    title("Pengaturan & bantuan");
    action("Server & koneksi", true, this::settings);
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
            "1. Hubungkan alamat server dan token operator.\n"
                + "2. Buat acara, pilih frame dan hubungkan album.\n"
                + "3. Mulai sesi, lalu buka kamera bawaan.\n"
                + "4. Tinjau hasil di Foto, atau aktifkan terbit otomatis sebelum mulai sesi.\n"
                + "5. Bagikan QR acara kepada tamu.",
            14,
            MUTED,
            false));
    c.addView(
        text(
            "Saat sesi dijeda/diakhiri, foto baru tidak dipantau; antrean lama tetap diselesaikan."
                + " Mematikan paksa Alpi menghentikan pekerjaan sampai dibuka lagi. Android dapat"
                + " membatasi sesi latar belakang; periksa notifikasi dan status di Beranda.",
            13,
            MUTED,
            false));
    hint(
        "Alpi · Album Photo Live\n"
            + "Versi 0.1.0-beta · Uji awal pada perangkat diperlukan.\n"
            + "Foto hasil: JPEG, sisi panjang maksimal 2.560 px, kualitas 92. Foto asli tidak"
            + " diubah.");
  }

  private void settings() {
    LinearLayout f = column();
    f.setPadding(dp(20), dp(12), dp(20), dp(12));
    EditText server =
        input(
            f,
            "Alamat server (contoh https://album.domain.id)",
            Prefs.get(this, "server", ""),
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
    String secret = "";
    try {
      secret = Prefs.token(this);
    } catch (Exception ignored) {
    }
    EditText token =
        input(
            f,
            "Token operator",
            secret,
            InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    EditText folder =
        input(
            f,
            "Folder kamera di MediaStore",
            Prefs.get(this, "folder", "DCIM/Camera/"),
            InputType.TYPE_CLASS_TEXT);
    CheckBox wifi = new CheckBox(this);
    wifi.setText(R.string.wifi_only);
    wifi.setChecked(Prefs.p(this).getBoolean("wifi", false));
    f.addView(wifi);
    CheckBox http = new CheckBox(this);
    http.setText(R.string.allow_http);
    http.setChecked(Prefs.p(this).getBoolean("http", false));
    f.addView(http);
    f.addView(
        text(
            "Gunakan HTTPS untuk server publik. Perubahan server berlaku untuk semua acara pada HP"
                + " ini. PIN album akan diperbarui saat Hubungkan album.",
            12,
            MUTED,
            false));
    ScrollView sc = new ScrollView(this);
    sc.addView(f);
    AlertDialog d =
        new AlertDialog.Builder(this)
            .setTitle("Server & koneksi")
            .setView(sc)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan", null)
            .create();
    d.setOnShowListener(
        v ->
            d.getButton(-1)
                .setOnClickListener(
                    b -> {
                      try {
                        String url = server.getText().toString().trim().replaceAll("/+$", "");
                        if (!url.isEmpty()) {
                          URL u = new URL(url);
                          if ((!u.getProtocol().equals("https")
                                  && !(http.isChecked() && u.getProtocol().equals("http")))
                              || u.getHost().isEmpty()
                              || u.getUserInfo() != null
                              || u.getQuery() != null
                              || u.getRef() != null
                              || !u.getPath().isEmpty())
                            throw new IOException(
                                "Gunakan alamat HTTP(S) tanpa path; HTTP perlu diizinkan.");
                          if (token.length() < 24)
                            throw new IOException("Token operator minimal 24 karakter.");
                        }
                        String path = folder.getText().toString().trim();
                        if (!path.startsWith("DCIM/") || !path.endsWith("/") || path.contains(".."))
                          throw new IOException("Folder harus diawali DCIM/ dan diakhiri /.");
                        Prefs.set(this, "server", url);
                        Prefs.token(this, token.getText().toString().trim());
                        Prefs.set(this, "folder", path);
                        Prefs.p(this)
                            .edit()
                            .putBoolean("wifi", wifi.isChecked())
                            .putBoolean("http", http.isChecked())
                            .apply();
                        d.dismiss();
                        Pipeline.kick(this);
                        render();
                      } catch (Exception e) {
                        toast(Pipeline.safe(e));
                      }
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
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    } catch (Exception e) {
      toast("Browser tidak tersedia.");
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
    background(
        "Membuat QR album…",
        () -> {
          new Api(this).sync(store.event(id));
          byte[] bytes = new Api(this).qr(id);
          File dir = new File(getCacheDir(), "share");
          dir.mkdirs();
          try (FileOutputStream out = new FileOutputStream(new File(dir, id + ".png"))) {
            out.write(bytes);
          }
        },
        () -> {
          File f = new File(new File(getCacheDir(), "share"), id + ".png");
          Bitmap image = BitmapFactory.decodeFile(f.getAbsolutePath());
          ImageView v = new ImageView(this);
          v.setImageBitmap(image);
          v.setAdjustViewBounds(true);
          v.setPadding(dp(24), dp(24), dp(24), dp(24));
          new AlertDialog.Builder(this)
              .setTitle("QR album · " + store.event(id).optString("title"))
              .setView(v)
              .setPositiveButton("Bagikan QR", (d, w) -> shareFile(f, "share"))
              .setNegativeButton("Tutup", (d, w) -> image.recycle())
              .show();
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
