package com.totogutomo.alpi;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.*;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;
import java.text.*;
import java.util.*;

/** Capture metadata only: never substitutes the current clock or current phone location. */
public final class PhotoMetadata {
  public String time = "", location = "", originalDate = "", offset = "";
  public long sourceBytes;
  public final List<String> notes = new ArrayList<>();

  public static PhotoMetadata sample() {
    PhotoMetadata m = new PhotoMetadata();
    m.time = "28 Okt 2026 10:30:00 (contoh)";
    m.location = "-2.55000, 114.18000 (contoh GPS)";
    return m;
  }

  public static PhotoMetadata read(Context c, Uri uri, FrameSpec spec) {
    PhotoMetadata m = new PhotoMetadata();
    boolean wantsGps = spec.mode() > 0 && spec.flag("show_location");
    boolean media = "content".equals(uri.getScheme()) && "media".equals(uri.getAuthority());
    boolean locationPermission =
        c.checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION)
            == PackageManager.PERMISSION_GRANTED;
    Uri source = media && wantsGps && locationPermission ? MediaStore.setRequireOriginal(uri) : uri;
    try (InputStream in = c.getContentResolver().openInputStream(source)) {
      if (in != null) {
        ExifInterface exif = new ExifInterface(in);
        m.originalDate = cleanDate(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL));
        if (m.originalDate.isEmpty())
          m.originalDate = cleanDate(exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED));
        String zone = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL);
        m.offset = zone != null && zone.matches("[+-](?:0[0-9]|1[0-4]):[0-5][0-9]") ? zone : "";
        if (!m.originalDate.isEmpty()) {
          m.time = dateLabel(m.originalDate) + (m.offset.isEmpty() ? "" : " UTC" + m.offset);
        }
        if (wantsGps) {
          double[] gps = exif.getLatLong();
          if (gps != null
              && Double.isFinite(gps[0])
              && Double.isFinite(gps[1])
              && Math.abs(gps[0]) <= 90
              && Math.abs(gps[1]) <= 180)
            m.location = String.format(Locale.US, "%.5f, %.5f", gps[0], gps[1]);
        }
      }
    } catch (Exception ignored) {
      /* Unsupported/redacted metadata must not prevent framing. */
    }
    if ("file".equals(uri.getScheme())) m.sourceBytes = new File(uri.getPath()).length();
    if ("content".equals(uri.getScheme())) {
      try (Cursor cur = c.getContentResolver().query(uri, null, null, null, null)) {
        if (cur != null && cur.moveToFirst()) {
          int size = cur.getColumnIndex(OpenableColumns.SIZE);
          if (size >= 0) m.sourceBytes = cur.getLong(size);
          int taken = cur.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
          if (m.time.isEmpty() && taken >= 0 && cur.getLong(taken) > 0)
            m.time =
                new SimpleDateFormat("dd MMM yyyy HH:mm:ss", new Locale("id", "ID"))
                    .format(new Date(cur.getLong(taken)));
        }
      } catch (Exception ignored) {
      }
    }
    if (spec.mode() > 0 && spec.flag("show_time") && m.time.isEmpty())
      m.notes.add("Waktu pengambilan tidak tersedia pada metadata foto.");
    if (wantsGps && m.location.isEmpty())
      m.notes.add(
          media && !locationPermission
              ? "Izin metadata lokasi belum diberikan; GPS tidak ditampilkan."
              : "Foto tidak menyimpan koordinat GPS yang dapat dibaca.");
    return m;
  }

  static String cleanDate(String value) {
    if (value == null || !value.matches("[0-9]{4}:[0-9]{2}:[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}"))
      return "";
    try {
      SimpleDateFormat f = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
      f.setLenient(false);
      f.setTimeZone(TimeZone.getTimeZone("UTC"));
      f.parse(value);
      return value;
    } catch (ParseException e) {
      return "";
    }
  }

  static String dateLabel(String date) throws ParseException {
    SimpleDateFormat in = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
    in.setTimeZone(TimeZone.getTimeZone("UTC"));
    SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", new Locale("id", "ID"));
    out.setTimeZone(TimeZone.getTimeZone("UTC"));
    return out.format(in.parse(date));
  }
}
