package com.totogutomo.alpi;

import java.net.URI;

/** Shared Google Photos links are navigation addresses, never upload endpoints. */
public final class AlbumLinks {
  private AlbumLinks() {}

  public static String normalize(String value) {
    String link = value == null ? "" : value.trim();
    if (link.isEmpty()) return "";
    try {
      URI uri = new URI(link);
      String host = uri.getHost(), path = uri.getPath();
      boolean shortLink = "photos.app.goo.gl".equalsIgnoreCase(host);
      boolean sharedLink = "photos.google.com".equalsIgnoreCase(host);
      if (link.length() > 2500
          || !"https".equalsIgnoreCase(uri.getScheme())
          || uri.getRawUserInfo() != null
          || (uri.getPort() != -1 && uri.getPort() != 443)
          || path == null
          || (shortLink
              ? path.length() < 2
              : !(sharedLink && path.startsWith("/share/") && path.length() > 7)))
        throw new IllegalArgumentException();
      return uri.toASCIIString();
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Tempel tautan berbagi album Google Photos, misalnya https://photos.app.goo.gl/…");
    }
  }
}
