package com.totogutomo.alpi;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import java.io.*;

/** Read-only provider, restricted to generated results and exported QR images. */
public class ShareProvider extends ContentProvider {
  @Override
  public boolean onCreate() {
    return true;
  }

  File file(Uri uri) throws FileNotFoundException {
    try {
      String group = uri.getPathSegments().get(0), name = uri.getLastPathSegment();
      if (!group.equals("results") && !group.equals("share")) throw new IOException();
      File root =
          group.equals("results")
              ? new File(getContext().getFilesDir(), "results")
              : new File(getContext().getCacheDir(), "share");
      File f = new File(root, name).getCanonicalFile();
      if (!f.getParentFile().equals(root.getCanonicalFile()) || !f.isFile())
        throw new IOException();
      return f;
    } catch (Exception e) {
      throw new FileNotFoundException();
    }
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
    if (!mode.equals("r")) throw new FileNotFoundException();
    return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY);
  }

  @Override
  public String getType(Uri uri) {
    return uri.toString().endsWith(".png") ? "image/png" : "image/jpeg";
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) {
    try {
      File f = file(uri);
      MatrixCursor c =
          new MatrixCursor(new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
      c.addRow(new Object[] {f.getName(), f.length()});
      return c;
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  public Uri insert(Uri u, ContentValues v) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int update(Uri u, ContentValues v, String s, String[] a) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int delete(Uri u, String s, String[] a) {
    throw new UnsupportedOperationException();
  }
}
