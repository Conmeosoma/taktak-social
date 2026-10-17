package com.taktak.app.ui.camera;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class PhotoProcessor {
    private PhotoProcessor() { }
    // Camera sau cho 3D; xử lý rotation trước crop. Không ghi EXIF hoặc xuất gallery.
    public static byte[] squareJpeg(Bitmap source, int rotation) throws IOException {
        Matrix matrix = new Matrix(); matrix.postRotate(rotation);
        Bitmap upright = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        int side = Math.min(upright.getWidth(), upright.getHeight());
        Bitmap square = Bitmap.createBitmap(upright, (upright.getWidth() - side) / 2, (upright.getHeight() - side) / 2, side, side);
        Bitmap scaled = Bitmap.createScaledBitmap(square, Math.min(side, 1280), Math.min(side, 1280), true);
        try {
            byte[] result = null;
            for (int quality = 92; quality >= 52; quality -= 8) {
                try (var output = new ByteArrayOutputStream()) {
                    if (!scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)) { throw new IOException("JPEG encoding failed."); }
                    result = output.toByteArray();
                    if (result.length <= 800 * 1024) { break; }
                }
            }
            if (result == null || result.length > 800 * 1024) { throw new IOException("Photo exceeds bounded capture size."); }
            return result;
        } finally {
            if (scaled != square && scaled != source && scaled != upright) { scaled.recycle(); }
            if (square != source && square != upright) { square.recycle(); }
            if (upright != source) { upright.recycle(); }
        }
    }
}
