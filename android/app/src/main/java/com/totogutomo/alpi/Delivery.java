package com.totogutomo.alpi;

import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.*;
import java.util.*;
import org.json.*;

/** Local exports. Opening a receiver never means that a cloud upload has succeeded. */
public final class Delivery {
  public static final int MAX_BATCH = 30;
  public static final String PHOTOS_PACKAGE = "com.google.android.apps.photos";

  private Delivery() {}

  public static Uri uri(Context c, String group, String filename) {
    return new Uri.Builder()
        .scheme("content")
        .authority(c.getPackageName() + ".files")
        .appendPath(group)
        .appendPath(filename)
        .build();
  }

  /** Freeze copies so later reprocessing cannot change files already handed to a receiver. */
  public static JSONObject prepare(Context c, String eventId, Collection<String> ids)
      throws Exception {
    Store s = Store.get(c);
    if (s.pendingBatch() != null)
      throw new IOException("Selesaikan atau kembalikan kiriman sebelumnya lebih dahulu.");
    if (ids.isEmpty() || ids.size() > MAX_BATCH)
      throw new IOException("Pilih 1–30 foto dalam satu kiriman.");
    String batchId = UUID.randomUUID().toString().replace("-", "");
    File dir = new File(c.getFilesDir(), "deliveries");
    if (!dir.isDirectory() && !dir.mkdirs())
      throw new IOException("Penyimpanan HP tidak tersedia.");
    List<File> written = new ArrayList<>();
    try {
      for (String id : ids) {
        JSONObject q = s.photo(id);
        if (q == null
            || !eventId.equals(q.optString("event_id"))
            || !"READY".equals(q.optString("state")))
          throw new IOException(
              "Pilihan berubah. Pilih kembali foto yang siap kirim pada satu acara.");
        File original = new File(q.optString("output"));
        if (!original.isFile())
          throw new IOException("Hasil foto hilang. Proses ulang foto terlebih dahulu.");
        File snapshot = new File(dir, batchId + "_" + id + ".jpg");
        written.add(snapshot);
        copy(original, snapshot);
      }
      s.createBatch(batchId, eventId, new ArrayList<>(ids));
      return s.pendingBatch();
    } catch (Exception e) {
      for (File f : written) f.delete();
      throw e;
    }
  }

  public static Intent intent(Context c, JSONObject batch, boolean direct) throws Exception {
    JSONArray ids = new JSONArray(batch.getString("photo_ids"));
    ArrayList<Uri> uris = new ArrayList<>();
    for (int n = 0; n < ids.length(); n++) {
      String file = batch.getString("id") + "_" + ids.getString(n) + ".jpg";
      if (!new File(new File(c.getFilesDir(), "deliveries"), file).isFile())
        throw new IOException("Salinan kiriman hilang. Kembalikan ke Siap Kirim dan coba lagi.");
      uris.add(uri(c, "deliveries", file));
    }
    Intent i =
        new Intent(uris.size() == 1 ? Intent.ACTION_SEND : Intent.ACTION_SEND_MULTIPLE)
            .setType("image/jpeg")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    if (uris.size() == 1) i.putExtra(Intent.EXTRA_STREAM, uris.get(0));
    else i.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
    ClipData clip = ClipData.newUri(c.getContentResolver(), "Hasil foto Alpi", uris.get(0));
    for (int n = 1; n < uris.size(); n++) clip.addItem(new ClipData.Item(uris.get(n)));
    i.setClipData(clip);
    if (direct) i.setPackage(PHOTOS_PACKAGE);
    return i;
  }

  public static void copy(File source, File destination) throws IOException {
    try (InputStream in = new FileInputStream(source);
        FileOutputStream out = new FileOutputStream(destination)) {
      byte[] buffer = new byte[32768];
      int count;
      while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
      out.getFD().sync();
    }
  }

  /** App-owned MediaStore copies, visible in Photos' on-device folders. Never touches originals. */
  public static void gallery(Context c, Collection<String> ids) throws Exception {
    Store s = Store.get(c);
    for (String id : ids) {
      JSONObject q = s.photo(id);
      if (q == null || q.optString("output").isEmpty())
        throw new IOException("Hasil foto belum tersedia.");
      File source = new File(q.optString("output"));
      if (!source.isFile())
        throw new IOException("Hasil foto hilang. Proses ulang terlebih dahulu.");
      ContentResolver resolver = c.getContentResolver();
      ContentValues values = new ContentValues();
      values.put(MediaStore.Images.Media.DISPLAY_NAME, "ALPI_" + id + ".jpg");
      values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
      values.put(
          MediaStore.Images.Media.RELATIVE_PATH,
          "Pictures/Alpi/" + q.optString("event_id").substring(0, 8) + "/");
      values.put(MediaStore.Images.Media.IS_PENDING, 1);
      Uri saved = null;
      String previous = q.optString("gallery_uri");
      if (!previous.isEmpty()) {
        try {
          Uri old = Uri.parse(previous);
          if (resolver.update(old, values, null, null) > 0) saved = old;
        } catch (Exception ignored) {
          /* A user may have deleted the previous export. */
        }
      }
      if (saved == null)
        saved = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
      if (saved == null) throw new IOException("Galeri tidak dapat menyimpan foto.");
      try {
        try (InputStream in = new FileInputStream(source);
            OutputStream out = resolver.openOutputStream(saved, "wt")) {
          if (out == null) throw new IOException("Galeri tidak dapat membuka foto.");
          byte[] buffer = new byte[32768];
          int count;
          while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
        }
        ContentValues finished = new ContentValues();
        finished.put(MediaStore.Images.Media.IS_PENDING, 0);
        resolver.update(saved, finished, null, null);
        s.gallery(id, saved.toString());
      } catch (Exception e) {
        resolver.delete(saved, null, null);
        s.gallery(id, "");
        throw e;
      }
    }
  }

  public static File qr(Context c, String url, String eventId) throws Exception {
    String link = AlbumLinks.normalize(url);
    if (link.isEmpty()) throw new IOException("Simpan tautan album terlebih dahulu.");
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.MARGIN, 4);
    hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
    BitMatrix matrix = new MultiFormatWriter().encode(link, BarcodeFormat.QR_CODE, 720, 720, hints);
    int[] pixels = new int[720 * 720];
    for (int y = 0; y < 720; y++)
      for (int x = 0; x < 720; x++)
        pixels[y * 720 + x] = matrix.get(x, y) ? 0xff152c30 : 0xffffffff;
    Bitmap image = Bitmap.createBitmap(pixels, 720, 720, Bitmap.Config.ARGB_8888);
    File dir = new File(c.getCacheDir(), "share");
    dir.mkdirs();
    File file = new File(dir, eventId + "-" + Integer.toHexString(link.hashCode()) + ".png");
    try (OutputStream out = new FileOutputStream(file)) {
      image.compress(Bitmap.CompressFormat.PNG, 100, out);
    } finally {
      image.recycle();
    }
    return file;
  }

  public static void cleanup(Context c) {
    for (JSONObject batch :
        Store.get(c)
            .rows(
                "SELECT id FROM delivery_batches WHERE state!='PENDING' AND created<?",
                Long.toString(System.currentTimeMillis() - 7L * 86400000))) {
      File dir = new File(c.getFilesDir(), "deliveries");
      File[] files = dir.listFiles((d, name) -> name.startsWith(batch.optString("id") + "_"));
      if (files != null) for (File f : files) f.delete();
    }
  }
}
