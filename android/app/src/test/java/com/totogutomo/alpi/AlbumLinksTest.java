package com.totogutomo.alpi;

import static org.junit.Assert.*;

import org.junit.Test;

public class AlbumLinksTest {
  @Test
  public void acceptsSharedAlbumLinksAndPreservesKey() {
    assertEquals(
        "https://photos.app.goo.gl/wcFbg5Dcf13mkyKx9",
        AlbumLinks.normalize(" https://photos.app.goo.gl/wcFbg5Dcf13mkyKx9 "));
    assertEquals(
        "https://photos.google.com/share/abc?key=example",
        AlbumLinks.normalize("https://photos.google.com/share/abc?key=example"));
    assertEquals("", AlbumLinks.normalize(""));
  }

  @Test
  public void rejectsPrivateAlbumAndOtherDestinations() {
    for (String value :
        new String[] {
          "http://photos.app.goo.gl/abc",
          "https://photos.google.com/album/private",
          "https://photos.app.goo.gl.evil.example/abc",
          "https://photos.app.goo.gl@evil.example/abc",
          "https://user@photos.app.goo.gl/abc",
          "https://photos.app.goo.gl:8000/abc",
          "https://photos.app.goo.gl/",
          "file:///tmp/album",
          "javascript:alert(1)",
          "https://photos.google.com/share/"
        }) {
      assertThrows(value, IllegalArgumentException.class, () -> AlbumLinks.normalize(value));
    }
  }
}
