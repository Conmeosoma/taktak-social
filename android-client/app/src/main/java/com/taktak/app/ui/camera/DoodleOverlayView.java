package com.taktak.app.ui.camera;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DoodleOverlayView extends View {

    public enum Mode {
        DRAW,
        STICKER_DRAG,
        DISABLED
    }

    public static class Stroke {
        public final Path path;
        public final int color;
        public final float strokeWidth;

        public Stroke(Path path, int color, float strokeWidth) {
            this.path = path;
            this.color = color;
            this.strokeWidth = strokeWidth;
        }
    }

    public static class Sticker {
        public String text;
        public float x;
        public float y;
        public float textSize = 48f; // sp-equivalent in px

        public Sticker(String text, float x, float y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }
    }

    public static class MetadataTag {
        public String text;
        public float x = 24f;
        public float y = 36f;
        public boolean visible = false;

        public MetadataTag(String text) {
            this.text = text;
        }
    }

    private final List<Stroke> strokes = new ArrayList<>();
    private final List<Sticker> stickers = new ArrayList<>();
    private final MetadataTag contextTag = new MetadataTag("");

    private Mode currentMode = Mode.DISABLED;
    private int currentColor = Color.WHITE;
    private float currentStrokeWidth = 10f;

    private Path currentPath;
    private float lastX, lastY;
    private Sticker activeSticker;

    private final Paint drawPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tagBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tagTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public DoodleOverlayView(Context context) {
        super(context);
        init();
    }

    public DoodleOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint.setTextAlign(Paint.Align.CENTER);

        tagBgPaint.setColor(Color.parseColor("#99000000")); // 60% black pill
        tagBgPaint.setStyle(Paint.Style.FILL);

        tagTextPaint.setColor(Color.WHITE);
        tagTextPaint.setTextSize(spToPx(13));
        tagTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    private float spToPx(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }

    public void setMode(Mode mode) {
        this.currentMode = mode;
        invalidate();
    }

    public Mode getMode() {
        return currentMode;
    }

    public void setCurrentColor(int color) {
        this.currentColor = color;
    }

    public int getCurrentColor() {
        return currentColor;
    }

    public void setCurrentStrokeWidth(float width) {
        this.currentStrokeWidth = width;
    }

    public void addSticker(String emoji) {
        float centerX = getWidth() > 0 ? getWidth() / 2f : 300f;
        float centerY = getHeight() > 0 ? getHeight() / 2f : 300f;
        Sticker sticker = new Sticker(emoji, centerX, centerY);
        sticker.textSize = spToPx(38);
        stickers.add(sticker);
        currentMode = Mode.STICKER_DRAG;
        invalidate();
    }

    public void setContextTag(String text, boolean visible) {
        contextTag.text = text;
        contextTag.visible = visible;
        invalidate();
    }

    public boolean isContextTagVisible() {
        return contextTag.visible;
    }

    public void undo() {
        if (!strokes.isEmpty()) {
            strokes.remove(strokes.size() - 1);
            invalidate();
        } else if (!stickers.isEmpty()) {
            stickers.remove(stickers.size() - 1);
            invalidate();
        }
    }

    public void clearAll() {
        strokes.clear();
        stickers.clear();
        contextTag.visible = false;
        invalidate();
    }

    public boolean hasEdits() {
        return !strokes.isEmpty() || !stickers.isEmpty() || contextTag.visible;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw strokes
        for (Stroke stroke : strokes) {
            drawPaint.setColor(stroke.color);
            drawPaint.setStrokeWidth(stroke.strokeWidth);
            canvas.drawPath(stroke.path, drawPaint);
        }

        // 2. Draw current stroke in progress
        if (currentPath != null) {
            drawPaint.setColor(currentColor);
            drawPaint.setStrokeWidth(currentStrokeWidth);
            canvas.drawPath(currentPath, drawPaint);
        }

        // 3. Draw Stickers
        for (Sticker sticker : stickers) {
            textPaint.setTextSize(sticker.textSize);
            canvas.drawText(sticker.text, sticker.x, sticker.y, textPaint);
        }

        // 4. Draw Metadata Tag Pill (if enabled)
        if (contextTag.visible && contextTag.text != null && !contextTag.text.isEmpty()) {
            drawTagPill(canvas, contextTag.text, contextTag.x, contextTag.y);
        }
    }

    private void drawTagPill(Canvas canvas, String text, float x, float y) {
        float paddingH = spToPx(10);
        float paddingV = spToPx(6);
        float textWidth = tagTextPaint.measureText(text);
        Paint.FontMetrics fm = tagTextPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;

        RectF pillRect = new RectF(
                x,
                y,
                x + textWidth + (paddingH * 2),
                y + textHeight + (paddingV * 2)
        );
        float radius = pillRect.height() / 2f;
        canvas.drawRoundRect(pillRect, radius, radius, tagBgPaint);

        float textX = pillRect.left + paddingH;
        float textY = pillRect.top + paddingV - fm.ascent;
        canvas.drawText(text, textX, textY, tagTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (currentMode == Mode.DISABLED) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        if (currentMode == Mode.DRAW) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    currentPath = new Path();
                    currentPath.moveTo(x, y);
                    lastX = x;
                    lastY = y;
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (currentPath != null) {
                        currentPath.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
                        lastX = x;
                        lastY = y;
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (currentPath != null) {
                        currentPath.lineTo(x, y);
                        strokes.add(new Stroke(currentPath, currentColor, currentStrokeWidth));
                        currentPath = null;
                        invalidate();
                    }
                    return true;
            }
        } else if (currentMode == Mode.STICKER_DRAG) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    activeSticker = findStickerAt(x, y);
                    return activeSticker != null;
                case MotionEvent.ACTION_MOVE:
                    if (activeSticker != null) {
                        activeSticker.x = x;
                        activeSticker.y = y;
                        invalidate();
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    activeSticker = null;
                    return true;
            }
        }

        return super.onTouchEvent(event);
    }

    private Sticker findStickerAt(float x, float y) {
        float hitRadius = spToPx(40);
        for (int i = stickers.size() - 1; i >= 0; i--) {
            Sticker s = stickers.get(i);
            float dx = s.x - x;
            float dy = s.y - y;
            if (dx * dx + dy * dy <= hitRadius * hitRadius) {
                return s;
            }
        }
        return null;
    }

    /**
     * Merge the original bitmap with all doodle paths, stickers, and tags.
     * Returns a new mutated Bitmap ready for saving/uploading.
     */
    @NonNull
    public Bitmap renderCompositeBitmap(@NonNull Bitmap original) {
        Bitmap composite = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(composite);

        float scaleX = (float) composite.getWidth() / (float) Math.max(1, getWidth());
        float scaleY = (float) composite.getHeight() / (float) Math.max(1, getHeight());

        canvas.save();
        canvas.scale(scaleX, scaleY);

        // Render strokes
        for (Stroke stroke : strokes) {
            drawPaint.setColor(stroke.color);
            drawPaint.setStrokeWidth(stroke.strokeWidth);
            canvas.drawPath(stroke.path, drawPaint);
        }

        // Render stickers
        for (Sticker sticker : stickers) {
            textPaint.setTextSize(sticker.textSize);
            canvas.drawText(sticker.text, sticker.x, sticker.y, textPaint);
        }

        // Render metadata tag
        if (contextTag.visible && contextTag.text != null && !contextTag.text.isEmpty()) {
            drawTagPill(canvas, contextTag.text, contextTag.x, contextTag.y);
        }

        canvas.restore();
        return composite;
    }
}
