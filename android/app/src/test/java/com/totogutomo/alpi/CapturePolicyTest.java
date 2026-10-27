package com.totogutomo.alpi;

import static org.junit.Assert.*;

import org.junit.Test;

public class CapturePolicyTest {
  boolean accept(long id, long taken, long added, String path) {
    return CapturePolicy.accepts(100, id, 10500, 20500, taken, added, path, "DCIM/Camera/");
  }

  @Test
  public void newPhotoInsideSession() {
    assertTrue(accept(101, 12000, 12000, "DCIM/Camera/"));
  }

  @Test
  public void preExistingPhotoIsNeverImported() {
    assertFalse(accept(99, 12000, 12000, "DCIM/Camera/"));
  }

  @Test
  public void oldPhotoEditedDuringSessionIsExcluded() {
    assertFalse(accept(101, 9000, 12000, "DCIM/Camera/"));
  }

  @Test
  public void pausedPhotosAreExcluded() {
    assertFalse(accept(102, 21000, 21000, "DCIM/Camera/"));
  }

  @Test
  public void screenshotsAndOutputsAreExcluded() {
    assertFalse(accept(101, 12000, 12000, "Pictures/Screenshots/"));
    assertFalse(accept(101, 12000, 12000, "DCIM/Alpi/"));
  }

  @Test
  public void absentTakenTimeUsesSecondPrecisionWithoutLosingFirstShot() {
    assertTrue(accept(101, 0, 10000, "DCIM/Camera/"));
    assertFalse(accept(101, 0, 9000, "DCIM/Camera/"));
  }

  @Test
  public void delayedSaveUsesOriginalCaptureTime() {
    assertTrue(accept(101, 18000, 23000, "DCIM/Camera/"));
  }

  @Test
  public void providerPathIsCaseInsensitiveButNotRecursive() {
    assertTrue(accept(101, 12000, 12000, "dcim/camera/"));
    assertFalse(accept(101, 12000, 12000, "DCIM/Camera/Subfolder/"));
  }
}
