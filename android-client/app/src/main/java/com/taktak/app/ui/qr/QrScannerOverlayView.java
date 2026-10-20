package com.taktak.app.ui.qr;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import androidx.annotation.Nullable;

public class QrScannerOverlayView extends View {
    private final Paint scrimPaint = new Paint();
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cornerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint laserPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF boxRect = new RectF();
    private float laserY = 0f;
    private ValueAnimator laserAnimator;

    public QrScannerOverlayView(Context context) {
        super(context);
        init();
    }

    public QrScannerOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        scrimPaint.setColor(Color.parseColor("#99000000")); // 60% black

        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        cornerPaint.setColor(Color.parseColor("#FF6B4A"));
        cornerPaint.setStyle(Paint.Style.STROKE);
        cornerPaint.setStrokeWidth(dpToPx(4));
        cornerPaint.setStrokeCap(Paint.Cap.ROUND);

        laserPaint.setColor(Color.parseColor("#FF85AA"));
        laserPaint.setStrokeWidth(dpToPx(2.5f));
        laserPaint.setStyle(Paint.Style.STROKE);
        laserPaint.setShadowLayer(dpToPx(4), 0, 0, Color.parseColor("#FF85AA"));

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float boxSize = Math.min(w * 0.72f, dpToPx(260));
        float left = (w - boxSize) / 2f;
        float top = (h - boxSize) / 2.3f;
        boxRect.set(left, top, left + boxSize, top + boxSize);

        if (laserAnimator != null) {
            laserAnimator.cancel();
        }
        laserAnimator = ValueAnimator.ofFloat(boxRect.top + dpToPx(10), boxRect.bottom - dpToPx(10));
        laserAnimator.setDuration(2200);
        laserAnimator.setRepeatMode(ValueAnimator.REVERSE);
        laserAnimator.setRepeatCount(ValueAnimator.INFINITE);
        laserAnimator.setInterpolator(new LinearInterpolator());
        laserAnimator.addUpdateListener(animation -> {
            laserY = (float) animation.getAnimatedValue();
            invalidate();
        });
        laserAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw dark scrim covering full screen
        canvas.drawRect(0, 0, getWidth(), getHeight(), scrimPaint);

        // 2. Clear center rounded box
        float cornerRadius = dpToPx(20);
        canvas.drawRoundRect(boxRect, cornerRadius, cornerRadius, clearPaint);

        // 3. Draw 4 corner brackets
        float cl = dpToPx(28); // corner line length
        Path path = new Path();

        // Top-left
        path.moveTo(boxRect.left, boxRect.top + cl);
        path.lineTo(boxRect.left, boxRect.top + cornerRadius);
        path.quadTo(boxRect.left, boxRect.top, boxRect.left + cornerRadius, boxRect.top);
        path.lineTo(boxRect.left + cl, boxRect.top);

        // Top-right
        path.moveTo(boxRect.right - cl, boxRect.top);
        path.lineTo(boxRect.right - cornerRadius, boxRect.top);
        path.quadTo(boxRect.right, boxRect.top, boxRect.right, boxRect.top + cornerRadius);
        path.lineTo(boxRect.right, boxRect.top + cl);

        // Bottom-right
        path.moveTo(boxRect.right, boxRect.bottom - cl);
        path.lineTo(boxRect.right, boxRect.bottom - cornerRadius);
        path.quadTo(boxRect.right, boxRect.bottom, boxRect.right - cornerRadius, boxRect.bottom);
        path.lineTo(boxRect.right - cl, boxRect.bottom);

        // Bottom-left
        path.moveTo(boxRect.left + cl, boxRect.bottom);
        path.lineTo(boxRect.left + cornerRadius, boxRect.bottom);
        path.quadTo(boxRect.left, boxRect.bottom, boxRect.left, boxRect.bottom - cornerRadius);
        path.lineTo(boxRect.left, boxRect.bottom - cl);

        canvas.drawPath(path, cornerPaint);

        // 4. Draw laser line
        if (laserY > 0) {
            canvas.drawLine(boxRect.left + dpToPx(8), laserY, boxRect.right - dpToPx(8), laserY, laserPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (laserAnimator != null) {
            laserAnimator.cancel();
            laserAnimator = null;
        }
    }
}
