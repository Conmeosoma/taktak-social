package com.taktak.app.ui.camera;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class PhotoProcessor {
    private PhotoProcessor() { }
    // Xử lý rotation trước crop; mirror trên ảnh đã thẳng, không ghi EXIF/gallery.
    public static byte[] squareJpeg(Bitmap source, int rotation) throws IOException {
        return squareJpeg(source, rotation, false);
    }
    public static byte[] squareJpeg(Bitmap source, int rotation, boolean mirror) throws IOException {
        Matrix matrix = new Matrix(); matrix.postRotate(rotation);
        if (mirror) { matrix.postScale(-1, 1); }
        Bitmap upright = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        int side = Math.min(upright.getWidth(), upright.getHeight());
        Bitmap square = Bitmap.createBitmap(upright, (upright.getWidth() - side) / 2, (upright.getHeight() - side) / 2, side, side);
        try {
            // Không upscale hoặc thêm byte cho ảnh đơn giản chỉ để đạt 500 KB.
            for (int edge : new int[]{1280, 1024, 768}) {
                Bitmap scaled = Bitmap.createScaledBitmap(square, Math.min(side, edge), Math.min(side, edge), true);
                try {
                    for (int quality = 92; quality >= 52; quality -= 8) {
                        try (var output = new ByteArrayOutputStream()) {
                            if (!scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)) { throw new IOException("JPEG encoding failed."); }
                            byte[] result = output.toByteArray();
                            if (result.length <= 800 * 1024) { return result; }
                        }
                    }
                } finally {
                    if (scaled != square && scaled != source && scaled != upright) { scaled.recycle(); }
                }
            }
            throw new IOException("Photo exceeds bounded capture size.");
        } finally {
            if (square != source && square != upright) { square.recycle(); }
            if (upright != source) { upright.recycle(); }
        }
    }
}
