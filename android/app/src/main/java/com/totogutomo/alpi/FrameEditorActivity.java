package com.totogutomo.alpi;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.function.IntConsumer;
import org.json.*;

/** Native studio: a shared draft, local preview, and explicit save for the next session. */
public class FrameEditorActivity extends Activity {
  private static final int GREEN = Color.rgb(23, 107, 86),
      INK = Color.rgb(21, 44, 48),
      MUTED = Color.rgb(91, 112, 105);
  private FrameSpec draft;
  private JSONObject event;
  private String tab = "basic", previewUri = "";
  private LinearLayout root, controls, tabs;
  private TextView previewNote;
  private ImageView preview;
  private EditText mainText, activityText;
  private Bitmap shown;
  private volatile boolean alive;
  private volatile int previewVersion;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final Runnable refreshPreview = this::previewNow;

  private int dp(float n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  private GradientDrawable surface(int color, int corner) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(corner));
    return d;
  }

  private LinearLayout column() {
    LinearLayout v = new LinearLayout(this);
    v.setOrientation(LinearLayout.VERTICAL);
    return v;
  }

  private TextView text(String label, int size, int color) {
    TextView v = new TextView(this);
    v.setText(label);
    v.setTextColor(color);
    v.setTextSize(size);
    v.setPadding(0, dp(4), 0, dp(6));
    return v;
  }

  private Button button(String label, boolean primary, Runnable action) {
    Button v = new Button(this);
    v.setText(label);
    v.setAllCaps(false);
    v.setTextColor(primary ? Color.WHITE : GREEN);
    v.setTextSize(13);
    v.setBackground(surface(primary ? GREEN : Color.rgb(229, 239, 230), 10));
    v.setMinHeight(dp(46));
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
    lp.bottomMargin = dp(8);
    v.setLayoutParams(lp);
    v.setOnClickListener(b -> action.run());
    return v;
  }

  private void note(String label) {
    controls.addView(text(label, 13, MUTED));
  }

  private void heading(String label) {
    TextView t = text(label, 20, INK);
    t.setTypeface(null, Typeface.BOLD);
    controls.addView(t);
  }

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    alive = true;
    String eventId = getIntent().getStringExtra("event_id");
    event = Store.get(this).event(eventId == null ? "" : eventId);
    if (event == null) {
      finish();
      return;
    }
    draft = FrameSpec.fromEvent(event);
    tab = getIntent().getStringExtra("tab");
    if (tab == null) tab = "basic";
    if (state != null) {
      try {
        draft = new FrameSpec(new JSONObject(state.getString("draft", draft.data.toString())));
      } catch (JSONException ignored) {
      }
      tab = state.getString("tab", tab);
      previewUri = state.getString("previewUri", "");
    } else {
      if (tab.equals("basic")) draft.put("mode", 1);
      if (tab.equals("pro")) draft.put("mode", 2);
      if (!draft.flag("edited") && draft.text("text").isEmpty())
        draft.put(
            "text",
            event.optString("label").isEmpty()
                ? event.optString("title")
                : event.optString("label"));
      java.util.List<JSONObject> recent =
          Store.get(this)
              .rows(
                  "SELECT uri FROM queue WHERE event_id=? ORDER BY created DESC LIMIT 1",
                  event.optString("id"));
      if (!recent.isEmpty()) previewUri = recent.get(0).optString("uri");
    }
    root = column();
    root.setBackgroundColor(Color.rgb(245, 247, 243));
    setContentView(root);
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          if (Build.VERSION.SDK_INT >= 30) {
            Insets i =
                insets.getInsets(
                    WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout()
                        | WindowInsets.Type.ime());
            root.setPadding(i.left, i.top, i.right, i.bottom);
          } else
            root.setPadding(
                insets.getSystemWindowInsetLeft(),
                insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom());
          return insets;
        });
    LinearLayout header = column();
    header.setPadding(dp(18), dp(8), dp(18), 0);
    TextView title = text("Studio frame", 25, GREEN);
    title.setTypeface(null, Typeface.BOLD);
    header.addView(title);
    header.addView(text(event.optString("title"), 13, MUTED));
    root.addView(header);
    preview = new ImageView(this);
    preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
    preview.setContentDescription("Pratinjau frame sesuai pengaturan saat ini");
    preview.setOnClickListener(v -> enlargePreview());
    root.addView(
        preview,
        new LinearLayout.LayoutParams(
            -1, dp(getResources().getConfiguration().orientation == 2 ? 130 : 210)));
    previewNote = text("Menyiapkan pratinjau…", 11, MUTED);
    previewNote.setPadding(dp(18), 0, dp(18), dp(4));
    root.addView(previewNote);
    tabs = new LinearLayout(this);
    tabs.setPadding(dp(14), 0, dp(14), 0);
    root.addView(tabs);
    ScrollView scroll = new ScrollView(this);
    controls = column();
    controls.setPadding(dp(18), dp(8), dp(18), dp(12));
    scroll.addView(controls);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout footer = new LinearLayout(this);
    footer.setPadding(dp(14), dp(6), dp(14), dp(8));
    footer.addView(button("Batal", false, this::finish), new LinearLayout.LayoutParams(0, -2, 1));
    LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(0, -2, 1);
    saveLp.leftMargin = dp(8);
    footer.addView(button("Simpan pengaturan", true, this::save), saveLp);
    root.addView(footer);
    renderControls();
    schedulePreview();
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    pull();
    out.putString("draft", draft.data.toString());
    out.putString("tab", tab);
    out.putString("previewUri", previewUri);
    super.onSaveInstanceState(out);
  }

  @Override
  public void onDestroy() {
    alive = false;
    previewVersion++;
    handler.removeCallbacksAndMessages(null);
    if (preview != null) preview.setImageDrawable(null);
    if (shown != null) shown.recycle();
    super.onDestroy();
  }

  private void pull() {
    if (mainText != null) draft.put("text", mainText.getText().toString());
    if (activityText != null) draft.put("activity", activityText.getText().toString());
  }

  private void chooseTab(String chosen) {
    pull();
    tab = chosen;
    if (chosen.equals("basic")) draft.put("mode", 1);
    if (chosen.equals("pro")) draft.put("mode", 2);
    renderControls();
    schedulePreview();
  }

  private void renderControls() {
    controls.removeAllViews();
    tabs.removeAllViews();
    mainText = null;
    activityText = null;
    for (String name : new String[] {"compression", "basic", "pro"}) {
      String label =
          name.equals("compression") ? "Kompresi" : name.equals("basic") ? "Basic" : "Pro";
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
      lp.setMargins(dp(3), 0, dp(3), 0);
      tabs.addView(button(label, name.equals(tab), () -> chooseTab(name)), lp);
    }
    if (tab.equals("compression")) compressionControls();
    else frameControls();
    controls.addView(button("Pilih foto untuk pratinjau", false, () -> pick(121)));
    controls.addView(
        button(
            "Gunakan ilustrasi contoh",
            false,
            () -> {
              previewUri = "";
              schedulePreview();
            }));
    note(
        "Pengaturan disalin saat Mulai Sesi. Jika sesi sedang aktif, akhiri lalu mulai sesi baru."
            + " Hasil lama dapat diproses ulang dari foto asli.");
  }

  private EditText field(String label, String value, int limit) {
    controls.addView(text(label, 13, INK));
    EditText input = new EditText(this);
    input.setText(value);
    input.setTextColor(INK);
    input.setTextSize(15);
    input.setInputType(
        InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    input.setFilters(new InputFilter[] {new InputFilter.LengthFilter(limit)});
    controls.addView(input);
    input.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

          public void onTextChanged(CharSequence s, int start, int before, int count) {
            schedulePreview();
          }

          public void afterTextChanged(Editable s) {}
        });
    return input;
  }

  private void check(String label, boolean selected, java.util.function.Consumer<Boolean> action) {
    CheckBox v = new CheckBox(this);
    v.setText(label);
    v.setTextSize(14);
    v.setMinHeight(dp(48));
    v.setChecked(selected);
    v.setOnCheckedChangeListener(
        (b, on) -> {
          action.accept(on);
          schedulePreview();
        });
    controls.addView(v);
  }

  private void select(String title, String[] options, int selected, IntConsumer action) {
    controls.addView(text(title, 13, INK));
    Spinner spinner = new Spinner(this);
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options);
    spinner.setAdapter(adapter);
    spinner.setSelection(selected, false);
    spinner.setMinimumHeight(dp(48));
    controls.addView(spinner, new LinearLayout.LayoutParams(-1, -2));
    spinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          boolean first = true;

          public void onItemSelected(AdapterView<?> p, View v, int position, long id) {
            if (first) {
              first = false;
              if (position == selected) return;
            }
            action.accept(position);
            schedulePreview();
          }

          public void onNothingSelected(AdapterView<?> p) {}
        });
  }

  private void slider(String label, int value, int min, int max, IntConsumer action) {
    TextView caption = text(label + ": " + value, 13, INK);
    controls.addView(caption);
    SeekBar bar = new SeekBar(this);
    bar.setMax(max - min);
    bar.setProgress(value - min);
    bar.setMinimumHeight(dp(46));
    controls.addView(bar, new LinearLayout.LayoutParams(-1, -2));
    bar.setContentDescription(label);
    bar.setOnSeekBarChangeListener(
        new SeekBar.OnSeekBarChangeListener() {
          public void onProgressChanged(SeekBar b, int progress, boolean user) {
            if (!user) return;
            int next = progress + min;
            String text = label + ": " + next;
            caption.setText(text);
            action.accept(next);
            schedulePreview();
          }

          public void onStartTrackingTouch(SeekBar b) {}

          public void onStopTrackingTouch(SeekBar b) {}
        });
  }

  private void compressionControls() {
    heading("Kompresi hasil foto");
    note(
        "Foto asli tetap disimpan oleh kamera. Kompresi mengecilkan salinan hasil dan ukuran"
            + " unggahan; tingkat penghematan bergantung isi foto.");
    note("Aktif: " + draft.summary() + " · frame " + draft.modeName());
    select(
        "Pilihan cepat",
        FrameSpec.PRESETS,
        draft.preset(),
        index -> {
          if (index < 3) {
            pull();
            draft.preset(index);
            renderControls();
          }
        });
    int index = 0;
    for (int n = 0; n < FrameSpec.EDGES.length; n++)
      if (FrameSpec.EDGES[n] == draft.edge()) index = n;
    select(
        "Sisi panjang maksimal",
        new String[] {"1280 px", "1600 px", "2048 px", "2560 px", "3200 px"},
        index,
        n -> draft.put("edge", FrameSpec.EDGES[n]));
    slider("Kualitas JPEG", draft.quality(), 60, 95, n -> draft.put("quality", n));
    note(
        "Hemat: 1600/JPEG 72. Seimbang: 2048/JPEG 82. Detail: 2560/JPEG 90. Foto kecil tidak"
            + " diperbesar. Ukuran asli dan hasil tampil pada kartu foto setelah diproses.");
    controls.addView(
        button(
            "Gunakan frame PNG / pita lama",
            false,
            () -> {
              pull();
              draft.put("mode", 0);
              renderControls();
              schedulePreview();
            }));
  }

  private void frameControls() {
    heading(draft.mode() == 2 ? "Generator Pro" : "Generator Basic");
    mainText = field("Teks utama", draft.text("text"), 240);
    check(
        "Waktu pengambilan dari metadata",
        draft.flag("show_time"),
        on -> draft.put("show_time", on));
    check(
        "Lokasi dari metadata (koordinat GPS)",
        draft.flag("show_location"),
        on -> {
          draft.put("show_location", on);
          if (on
              && checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION)
                  != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[] {Manifest.permission.ACCESS_MEDIA_LOCATION}, 122);
        });
    note(
        "GPS membutuhkan lokasi yang sudah disimpan kamera dan izin metadata lokasi. Alpi tidak"
            + " mengambil lokasi HP saat ini. Data yang tidak tersedia dilewati dan dijelaskan pada"
            + " hasil.");
    select(
        "Warna pita",
        new String[] {"Hijau gelap", "Putih", "Hitam"},
        draft.number("theme", 0, 0, 2),
        n -> draft.put("theme", n));
    if (draft.mode() == 2) {
      activityText = field("Teks kegiatan", draft.text("activity"), 160);
      select(
          "Posisi teks dan metadata",
          FrameSpec.POSITIONS,
          draft.number("text_position", 7, 0, 8),
          n -> draft.put("text_position", n));
      slider(
          "Ukuran teks (%)",
          draft.number("text_scale", 100, 70, 160), 70, 160, n -> draft.put("text_scale", n));
      check(
          "Latar pita di belakang teks",
          draft.data.optBoolean("band", true),
          on -> draft.put("band", on));
      controls.addView(
          button(
              draft.text("logo").isEmpty() ? "+ Impor logo" : "Ganti logo",
              false,
              () -> pick(120)));
      if (!draft.text("logo").isEmpty())
        controls.addView(
            button(
                "Lepas logo",
                false,
                () -> {
                  pull();
                  draft.put("logo", "");
                  renderControls();
                  schedulePreview();
                }));
      note("Logo PNG transparan disarankan. JPG/WebP juga bisa. Rasio logo dipertahankan.");
      select(
          "Posisi logo",
          FrameSpec.POSITIONS,
          draft.number("logo_position", 1, 0, 8),
          n -> draft.put("logo_position", n));
      slider(
          "Ukuran logo (% lebar foto)",
          draft.number("logo_scale", 18, 5, 35), 5, 35, n -> draft.put("logo_scale", n));
      slider(
          "Transparansi logo (%)",
          100 - draft.number("logo_opacity", 100, 0, 100),
          0,
          100,
          n -> draft.put("logo_opacity", 100 - n));
      note(
          "Transparansi 0% berarti padat; 100% berarti tidak terlihat. Jika logo dan teks bertemu,"
              + " teks digeser agar tidak saling menutup.");
    } else
      note(
          "Basic menempatkan teks dan metadata di pita bawah. Pilih Pro untuk logo, teks kegiatan"
              + " dan pengaturan posisi.");
    note(
        "Generator aktif menggantikan komposisi PNG / pita lama pada hasil berikutnya. Pengaturan"
            + " PNG lama tetap tersimpan.");
  }

  private void pick(int request) {
    pull();
    startActivityForResult(
        new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("image/*"),
        request);
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (result != RESULT_OK || data == null || data.getData() == null) return;
    Uri source = data.getData();
    if (request == 121) {
      try {
        getContentResolver()
            .takePersistableUriPermission(source, Intent.FLAG_GRANT_READ_URI_PERMISSION);
      } catch (SecurityException ignored) {
      }
      previewUri = source.toString();
      schedulePreview();
    } else if (request == 120) {
      Toast.makeText(this, "Mengimpor logo…", Toast.LENGTH_SHORT).show();
      Pipeline.IO.execute(
          () -> {
            File temp = new File(getCacheDir(), "logo-" + UUID.randomUUID() + ".part");
            Bitmap bitmap = null;
            try {
              try (InputStream in = getContentResolver().openInputStream(source);
                  OutputStream out = new FileOutputStream(temp)) {
                if (in == null) throw new IOException("Logo tidak dapat dibuka.");
                byte[] buffer = new byte[8192];
                int n, total = 0;
                while ((n = in.read(buffer)) != -1) {
                  total += n;
                  if (total > 12 * 1024 * 1024) throw new IOException("Logo maksimal 12 MB.");
                  out.write(buffer, 0, n);
                }
              }
              bitmap = Pipeline.decode(this, Uri.fromFile(temp), 1024);
              File dir = new File(getFilesDir(), "logos");
              if (!dir.isDirectory() && !dir.mkdirs())
                throw new IOException("Penyimpanan logo tidak tersedia.");
              File target = new File(dir, UUID.randomUUID() + ".png");
              try (OutputStream out = new FileOutputStream(target)) {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out))
                  throw new IOException("Logo tidak dapat disimpan.");
              }
              handler.post(
                  () -> {
                    if (!alive) return;
                    pull();
                    draft.put("logo", target.getAbsolutePath());
                    renderControls();
                    schedulePreview();
                  });
            } catch (Exception e) {
              handler.post(
                  () -> {
                    if (alive) Toast.makeText(this, Pipeline.safe(e), Toast.LENGTH_LONG).show();
                  });
            } finally {
              temp.delete();
              if (bitmap != null) bitmap.recycle();
            }
          });
    }
  }

  @Override
  public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(request, permissions, results);
    schedulePreview();
  }

  private void schedulePreview() {
    if (!alive) return;
    handler.removeCallbacks(refreshPreview);
    handler.postDelayed(refreshPreview, 350);
  }

  private void previewNow() {
    pull();
    final int version = ++previewVersion;
    final String source = previewUri;
    final JSONObject snapshot;
    try {
      snapshot = new JSONObject(event.toString());
      snapshot.put("processing", draft.data.toString());
    } catch (JSONException e) {
      return;
    }
    previewNote.setText(R.string.preview_loading);
    Pipeline.IO.execute(
        () -> {
          if (!alive || version != previewVersion) return;
          Bitmap result = null;
          String note;
          try {
            FrameSpec spec = FrameSpec.fromEvent(snapshot);
            PhotoMetadata metadata =
                source.isEmpty()
                    ? PhotoMetadata.sample()
                    : PhotoMetadata.read(this, Uri.parse(source), spec);
            if (source.isEmpty()) {
              result = Bitmap.createBitmap(960, 720, Bitmap.Config.ARGB_8888);
              Canvas canvas = new Canvas(result);
              Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
              paint.setShader(
                  new LinearGradient(
                      0,
                      0,
                      960,
                      720,
                      new int[] {0xffa8c7c1, 0xffe8ddd0, 0xff526d65},
                      null,
                      Shader.TileMode.CLAMP));
              canvas.drawRect(0, 0, 960, 720, paint);
              paint.setShader(null);
              paint.setColor(0x55ffffff);
              canvas.drawCircle(690, 220, 125, paint);
              PhotoProcessor.decorate(this, result, snapshot, spec, metadata);
            } else result = PhotoProcessor.render(this, Uri.parse(source), snapshot, 960, metadata);
            note =
                (source.isEmpty() ? "Ilustrasi dan metadata contoh. " : "Pratinjau foto pilihan. ")
                    + spec.summary();
            if (!metadata.notes.isEmpty()) note += " · " + TextUtils.join(" ", metadata.notes);
          } catch (Exception e) {
            if (result != null) result.recycle();
            result = null;
            note = "Pratinjau belum tersedia: " + Pipeline.safe(e);
          }
          final Bitmap next = result;
          final String message = note;
          handler.post(
              () -> {
                if (!alive || version != previewVersion) {
                  if (next != null) next.recycle();
                  return;
                }
                preview.setImageBitmap(next);
                if (shown != null) shown.recycle();
                shown = next;
                previewNote.setText(message);
              });
        });
  }

  private void save() {
    pull();
    draft.put("edited", true);
    Store.get(this).eventSet(event.optString("id"), "processing", draft.data.toString());
    Toast.makeText(
            this,
            "Disimpan. Mulai sesi baru atau proses ulang hasil lama untuk menerapkan.",
            Toast.LENGTH_LONG)
        .show();
    setResult(RESULT_OK);
    finish();
  }

  private void enlargePreview() {
    if (shown == null || shown.isRecycled()) return;
    Bitmap copy = shown.copy(Bitmap.Config.ARGB_8888, false);
    ImageView image = new ImageView(this);
    image.setImageBitmap(copy);
    image.setAdjustViewBounds(true);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle("Pratinjau frame")
            .setView(image)
            .setPositiveButton("Tutup", null)
            .create();
    dialog.setOnDismissListener(
        d -> {
          image.setImageDrawable(null);
          copy.recycle();
        });
    dialog.show();
  }
}
