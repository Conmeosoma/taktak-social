package com.taktak.app.ui.qr;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import java.io.IOException;

public final class QrImageDecoder {
    private QrImageDecoder() { }
    public static Bitmap read(ContentResolver resolver, Uri uri) throws IOException {
        if (Build.VERSION.SDK_INT >= 28) return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri), (decoder, info, source) -> {
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            int width = info.getSize().getWidth(), height = info.getSize().getHeight();
            double scale = Math.min(1.0, 2048.0 / Math.max(width, height));
            decoder.setTargetSize(Math.max(1, (int) (width * scale)), Math.max(1, (int) (height * scale)));
        });
        var options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
        try (var input = resolver.openInputStream(uri)) { BitmapFactory.decodeStream(input, null, options); }
        if (options.outWidth < 1 || options.outHeight < 1) throw new IOException("Invalid image");
        options.inSampleSize = 1;
        while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 2048) options.inSampleSize *= 2;
        options.inJustDecodeBounds = false; options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap;
        try (var input = resolver.openInputStream(uri)) { bitmap = BitmapFactory.decodeStream(input, null, options); }
        if (bitmap == null) throw new IOException("Invalid image");
        return bitmap;
    }
}
