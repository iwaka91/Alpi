package com.totogutomo.alpi;

/** Session boundaries, independent from Android APIs so they can be tested on the JVM. */
public final class CapturePolicy {
  private CapturePolicy() {}

  public static boolean accepts(
      long baseline,
      long id,
      long start,
      long end,
      long taken,
      long added,
      String path,
      String folder) {
    if (id <= baseline || path == null || !path.equalsIgnoreCase(folder)) return false;
    if (taken > 0) return taken >= start && taken <= end;
    // DATE_ADDED has second precision: retain a new ID in an overlapping second.
    // Exact pause boundaries within that second cannot be reconstructed without DATE_TAKEN.
    return added <= end && added + 999 >= start;
  }
}
