package com.totogutomo.alpi;

import android.content.Context;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

public final class Api {
  private final Context context;

  public Api(Context c) {
    context = c.getApplicationContext();
  }

  public HttpURLConnection connection(String path, String method) throws Exception {
    String base = Prefs.get(context, "server", "");
    if (base.isEmpty()) throw new IOException("Atur alamat server di Pengaturan");
    URL url = new URL(base + path);
    if (!url.getProtocol().equals("https") && !url.getProtocol().equals("http"))
      throw new IOException("Gunakan HTTP atau HTTPS");
    HttpURLConnection c = (HttpURLConnection) url.openConnection();
    c.setConnectTimeout(12000);
    c.setReadTimeout(45000);
    c.setInstanceFollowRedirects(false);
    c.setRequestMethod(method);
    c.setRequestProperty("Authorization", "Bearer " + Prefs.token(context));
    return c;
  }

  static byte[] read(InputStream input, int limit) throws Exception {
    try (input;
        ByteArrayOutputStream b = new ByteArrayOutputStream()) {
      byte[] buf = new byte[8192];
      int n;
      while ((n = input.read(buf)) != -1) {
        b.write(buf, 0, n);
        if (b.size() > limit) throw new IOException("Respons terlalu besar");
      }
      return b.toByteArray();
    }
  }

  private byte[] result(HttpURLConnection c) throws Exception {
    int status = c.getResponseCode();
    if (status < 200 || status >= 300) {
      InputStream in = c.getErrorStream();
      String detail = in == null ? "" : new String(read(in, 4096), StandardCharsets.UTF_8);
      throw new IOException("Server " + status + ": " + detail);
    }
    return read(c.getInputStream(), 2 * 1024 * 1024);
  }

  public JSONObject json(String path, String method, JSONObject body) throws Exception {
    HttpURLConnection c = connection(path, method);
    try {
      if (body != null) {
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json");
        try (OutputStream out = c.getOutputStream()) {
          out.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
      }
      return new JSONObject(new String(result(c), StandardCharsets.UTF_8));
    } finally {
      c.disconnect();
    }
  }

  public JSONObject sync(JSONObject event) throws Exception {
    JSONObject body = new JSONObject();
    body.put("title", event.optString("title"));
    body.put("description", event.optString("description"));
    body.put("pin", event.optString("pin"));
    JSONObject remote = json("/api/events/" + event.optString("id"), "PUT", body);
    Store.get(context).eventSet(event.optString("id"), "album_url", remote.getString("album_url"));
    return remote;
  }

  public void upload(JSONObject item, File file) throws Exception {
    HttpURLConnection c =
        connection(
            "/api/events/" + item.getString("event_id") + "/photos/" + item.getString("id"), "PUT");
    try {
      c.setDoOutput(true);
      c.setFixedLengthStreamingMode(file.length());
      c.setRequestProperty("Content-Type", "image/jpeg");
      c.setRequestProperty("X-Captured-At", item.optString("captured"));
      try (InputStream in = new FileInputStream(file);
          OutputStream out = c.getOutputStream()) {
        byte[] buffer = new byte[65536];
        int n;
        while ((n = in.read(buffer)) != -1) {
          if (Thread.currentThread().isInterrupted()) throw new IOException("Unggahan dijeda");
          out.write(buffer, 0, n);
        }
      }
      result(c);
    } finally {
      c.disconnect();
    }
  }

  public void visible(JSONObject item, boolean visible) throws Exception {
    JSONObject body = new JSONObject().put("visible", visible);
    json(
        "/api/events/" + item.optString("event_id") + "/photos/" + item.optString("id"),
        "PATCH",
        body);
  }

  public byte[] qr(String id) throws Exception {
    HttpURLConnection c = connection("/api/events/" + id + "/qr", "GET");
    try {
      return result(c);
    } finally {
      c.disconnect();
    }
  }
}
