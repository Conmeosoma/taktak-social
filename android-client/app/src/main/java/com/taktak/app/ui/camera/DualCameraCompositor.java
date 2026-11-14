package com.taktak.app.ui.camera;

import android.graphics.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class DualCameraCompositor {
    private DualCameraCompositor() { }

    public static byte[] composite(Bitmap mainSource, int mainRotation, boolean mainMirror,
            Bitmap pipSource, int pipRotation, boolean pipMirror, boolean pipOnLeft) throws IOException {
        Bitmap uprightMain = orientAndSquare(mainSource, mainRotation, mainMirror);
        Bitmap uprightPip = orientAndSquare(pipSource, pipRotation, pipMirror);

        int canvasSize = 1080;
        Bitmap composite = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(composite);

        try {
            // Draw main background photo (1080x1080)
            Rect destMain = new Rect(0, 0, canvasSize, canvasSize);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(uprightMain, null, destMain, paint);

            // Draw PiP inset
            int pipSize = 300;
            int margin = 48;
            int left = pipOnLeft ? margin : (canvasSize - pipSize - margin);
            int top = margin;
            RectF pipRect = new RectF(left, top, left + pipSize, top + pipSize);
            float cornerRadius = 36f;

            // Soft drop shadow behind PiP
            Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            shadowPaint.setColor(Color.argb(80, 0, 0, 0));
            shadowPaint.setMaskFilter(new BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL));
            canvas.drawRoundRect(new RectF(pipRect.left - 4, pipRect.top - 2, pipRect.right + 4, pipRect.bottom + 8),
                    cornerRadius, cornerRadius, shadowPaint);

            // Clip PiP rounded corners and draw pip photo
            Path clipPath = new Path();
            clipPath.addRoundRect(pipRect, cornerRadius, cornerRadius, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(clipPath);
            canvas.drawBitmap(uprightPip, null, pipRect, paint);
            canvas.restore();

            // Border stroke
            Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setColor(Color.WHITE);
            borderPaint.setStrokeWidth(6f);
            canvas.drawRoundRect(pipRect, cornerRadius, cornerRadius, borderPaint);

            // Compress to target JPEG under 800 KB
            return PhotoProcessor.squareJpeg(composite, 0, false);
        } finally {
            if (composite != null && !composite.isRecycled()) composite.recycle();
            if (uprightMain != null && uprightMain != mainSource && !uprightMain.isRecycled()) uprightMain.recycle();
            if (uprightPip != null && uprightPip != pipSource && !uprightPip.isRecycled()) uprightPip.recycle();
        }
    }

    private static Bitmap orientAndSquare(Bitmap source, int rotation, boolean mirror) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        if (mirror) {
            matrix.postScale(-1, 1);
        }
        Bitmap upright = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        int side = Math.min(upright.getWidth(), upright.getHeight());
        int cropX = (upright.getWidth() - side) / 2;
        int cropY = (upright.getHeight() - side) / 2;
        Bitmap square = Bitmap.createBitmap(upright, cropX, cropY, side, side);
        if (square != upright && upright != source) {
            upright.recycle();
        }
        return square;
    }
}

