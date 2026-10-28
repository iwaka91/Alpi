package com.totogutomo.alpi;

import android.graphics.*;
import android.text.*;
import java.util.*;

/** Pixel-based layout shared by preview and final JPEGs. Logo aspect ratio is preserved. */
public final class FramePainter {
  private FramePainter() {}

  public static RectF anchor(int position, float w, float h, int width, int height, float margin) {
    int column = position % 3, row = position / 3;
    float x = column == 0 ? margin : column == 1 ? (width - w) / 2 : width - margin - w;
    float y = row == 0 ? margin : row == 1 ? (height - h) / 2 : height - margin - h;
    return new RectF(x, y, x + w, y + h);
  }

  public static String lines(FrameSpec s, PhotoMetadata metadata) {
    List<String> lines = new ArrayList<>();
    if (!s.text("text").trim().isEmpty()) lines.add(s.text("text").trim());
    if (s.mode() == 2 && !s.text("activity").trim().isEmpty()) lines.add(s.text("activity").trim());
    if (s.flag("show_time") && !metadata.time.isEmpty()) lines.add(metadata.time);
    if (s.flag("show_location") && !metadata.location.isEmpty())
      lines.add("GPS " + metadata.location);
    return TextUtils.join("\n", lines);
  }

  public static final class Layout {
    public RectF text = new RectF(), logo = new RectF();
    public StaticLayout lettering;
    public float padding;
  }

  public static Layout layout(
      int width, int height, FrameSpec s, PhotoMetadata metadata, Bitmap logo) {
    Layout result = new Layout();
    float margin = Math.min(width, height) * .035f;
    result.padding = Math.min(width, height) * .017f;
    String content = lines(s, metadata);
    if (s.mode() == 2 && logo != null && s.number("logo_opacity", 100, 0, 100) > 0) {
      float scale =
          Math.min(
              width * s.number("logo_scale", 18, 5, 35) / 100f / logo.getWidth(),
              height * .24f / logo.getHeight());
      result.logo =
          anchor(
              s.number("logo_position", 1, 0, 8),
              logo.getWidth() * scale,
              logo.getHeight() * scale,
              width,
              height,
              margin);
    }
    if (content.isEmpty()) return result;
    boolean basic = s.mode() == 1;
    TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    paint.setColor(s.number("theme", 0, 0, 2) == 1 ? Color.rgb(21, 44, 48) : Color.WHITE);
    float size = width * .027f * s.number("text_scale", 100, 70, 160) / 100f;
    int maxWidth = Math.max(1, Math.round(width * .86f));
    int position = basic ? 7 : s.number("text_position", 7, 0, 8);
    for (int n = 0; n < 30; n++) {
      paint.setTextSize(size);
      result.lettering =
          StaticLayout.Builder.obtain(content, 0, content.length(), paint, maxWidth)
              .setAlignment(
                  position % 3 == 0
                      ? android.text.Layout.Alignment.ALIGN_NORMAL
                      : position % 3 == 2
                          ? android.text.Layout.Alignment.ALIGN_OPPOSITE
                          : android.text.Layout.Alignment.ALIGN_CENTER)
              .setIncludePad(false)
              .setLineSpacing(size * .12f, 1f)
              .build();
      if (result.lettering.getHeight() <= height * .30f || size < width * .012f) break;
      size *= .92f;
    }
    float textHeight = result.lettering.getHeight() + result.padding * 2;
    result.text =
        basic
            ? new RectF(0, height - textHeight, width, height)
            : anchor(position, maxWidth + result.padding * 2, textHeight, width, height, margin);
    if (!result.logo.isEmpty() && RectF.intersects(result.text, result.logo)) {
      float gap = margin * .5f;
      float below = result.logo.bottom + gap;
      if (below + textHeight <= height - margin) result.text.offsetTo(result.text.left, below);
      else
        result.text.offsetTo(
            result.text.left, Math.max(margin, result.logo.top - gap - textHeight));
    }
    return result;
  }

  public static Layout paint(Bitmap bitmap, FrameSpec s, PhotoMetadata metadata, Bitmap logo) {
    Layout layout = layout(bitmap.getWidth(), bitmap.getHeight(), s, metadata, logo);
    Canvas canvas = new Canvas(bitmap);
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    if (!layout.logo.isEmpty()) {
      p.setAlpha(Math.round(s.number("logo_opacity", 100, 0, 100) * 2.55f));
      canvas.drawBitmap(logo, null, layout.logo, p);
      p.setAlpha(255);
    }
    if (layout.lettering != null) {
      int theme = s.number("theme", 0, 0, 2);
      if (s.mode() == 1 || s.data.optBoolean("band", true)) {
        p.setColor(
            theme == 1
                ? Color.argb(224, 255, 255, 255)
                : theme == 2 ? Color.argb(205, 12, 16, 20) : Color.argb(215, 13, 56, 44));
        canvas.drawRoundRect(
            layout.text, s.mode() == 1 ? 0 : layout.padding, s.mode() == 1 ? 0 : layout.padding, p);
      } else layout.lettering.getPaint().setShadowLayer(3, 0, 1, Color.BLACK);
      float x =
          s.mode() == 1
              ? (bitmap.getWidth() - layout.lettering.getWidth()) / 2f
              : layout.text.left + layout.padding;
      canvas.save();
      canvas.translate(x, layout.text.top + layout.padding);
      layout.lettering.draw(canvas);
      canvas.restore();
    }
    return layout;
  }
}
