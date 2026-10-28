package com.totogutomo.alpi;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;
import org.json.*;

/** The same renderer is used for previews, session processing and reprocessing originals. */
public final class PhotoProcessor {
  private PhotoProcessor() {}

  public static Bitmap render(
      Context c, Uri source, JSONObject event, int max, PhotoMetadata metadata) throws Exception {
    FrameSpec spec = FrameSpec.fromEvent(event);
    Bitmap raw = Pipeline.decode(c, source, Math.min(spec.edge(), max));
    Bitmap output = raw.copy(Bitmap.Config.ARGB_8888, true);
    raw.recycle();
    try {
      decorate(c, output, event, spec, metadata);
      return output;
    } catch (Exception e) {
      output.recycle();
      throw e;
    }
  }

  public static void decorate(
      Context c, Bitmap bitmap, JSONObject event, FrameSpec spec, PhotoMetadata metadata)
      throws Exception {
    if (spec.mode() > 0) {
      Bitmap logo = null;
      try {
        if (spec.mode() == 2 && !spec.text("logo").isEmpty()) {
          File file = new File(spec.text("logo"));
          if (!file.isFile())
            throw new IOException("Logo tidak tersedia. Impor ulang logo dan proses ulang foto.");
          logo = Pipeline.decode(c, Uri.fromFile(file), 1024);
        }
        FramePainter.paint(bitmap, spec, metadata, logo);
      } finally {
        if (logo != null) logo.recycle();
      }
      return;
    }
    String framePath =
        event.optString(bitmap.getHeight() > bitmap.getWidth() ? "portrait" : "landscape");
    boolean anyFrame =
        !event.optString("portrait").isEmpty() || !event.optString("landscape").isEmpty();
    if (anyFrame && framePath.isEmpty())
      throw new IOException("Frame untuk orientasi ini belum dipasang");
    Canvas canvas = new Canvas(bitmap);
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    if (!framePath.isEmpty()) {
      File file = new File(framePath);
      if (!file.isFile())
        throw new IOException("Frame tidak tersedia. Pilih Proses ulang setelah mengganti frame.");
      Bitmap frame = Pipeline.decode(c, Uri.fromFile(file), 2560);
      canvas.drawBitmap(frame, null, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), paint);
      frame.recycle();
    }
    String label = event.optString("label").trim();
    if (!label.isEmpty()) {
      float band = bitmap.getHeight() * .095f;
      paint.setColor(Color.argb(215, 13, 56, 44));
      canvas.drawRect(0, bitmap.getHeight() - band, bitmap.getWidth(), bitmap.getHeight(), paint);
      paint.setColor(Color.WHITE);
      paint.setTextSize(bitmap.getWidth() * .026f);
      paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
      paint.setTextAlign(Paint.Align.CENTER);
      while (paint.measureText(label) > bitmap.getWidth() * .88f)
        paint.setTextSize(paint.getTextSize() - 1);
      canvas.drawText(label, bitmap.getWidth() / 2f, bitmap.getHeight() - band * .38f, paint);
    }
  }

  public static final class Result {
    public String path;
    public JSONObject stats;
  }

  public static Result process(Context c, JSONObject item) throws Exception {
    JSONObject event = new JSONObject(item.getString("config"));
    FrameSpec spec = FrameSpec.fromEvent(event);
    Uri source = Uri.parse(item.getString("uri"));
    PhotoMetadata metadata = PhotoMetadata.read(c, source, spec);
    Bitmap bitmap = render(c, source, event, spec.edge(), metadata);
    File dir = new File(c.getFilesDir(), "results");
    if (!dir.isDirectory() && !dir.mkdirs()) {
      bitmap.recycle();
      throw new IOException("Penyimpanan hasil tidak tersedia.");
    }
    File output = new File(dir, item.getString("id") + ".jpg");
    File tmp = new File(dir, item.getString("id") + ".part");
    try {
      try (FileOutputStream stream = new FileOutputStream(tmp)) {
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, spec.quality(), stream))
          throw new IOException("Gagal menyimpan hasil foto.");
        stream.getFD().sync();
      }
      // Preserve capture date for album ordering. GPS is only drawn when requested, never copied
      // covertly.
      if (!metadata.originalDate.isEmpty()) {
        ExifInterface exif = new ExifInterface(tmp.getAbsolutePath());
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, metadata.originalDate);
        if (!metadata.offset.isEmpty())
          exif.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL, metadata.offset);
        exif.setAttribute(
            ExifInterface.TAG_ORIENTATION, Integer.toString(ExifInterface.ORIENTATION_NORMAL));
        exif.saveAttributes();
      }
      if (!tmp.renameTo(output)) throw new IOException("Gagal menyelesaikan berkas hasil.");
      Result result = new Result();
      result.path = output.getAbsolutePath();
      result.stats =
          new JSONObject()
              .put("source", metadata.sourceBytes)
              .put("bytes", output.length())
              .put("width", bitmap.getWidth())
              .put("height", bitmap.getHeight())
              .put("quality", spec.quality())
              .put("notes", new JSONArray(metadata.notes));
      return result;
    } finally {
      tmp.delete();
      bitmap.recycle();
    }
  }
}
