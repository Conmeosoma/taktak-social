package com.taktak.app.ui.history;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class GallerySaver {

    private GallerySaver() {}

    /**
     * Lưu ảnh (byte array JPEG) vào Bộ sưu tập thiết bị (MediaStore Pictures/Taktak).
     * @return Uri của ảnh đã lưu trong MediaStore
     */
    @NonNull
    public static Uri saveJpeg(@NonNull Context context, @NonNull byte[] jpegBytes, @NonNull String filenamePrefix) throws IOException {
        if (jpegBytes == null || jpegBytes.length == 0) {
            throw new IOException("JPEG bytes cannot be empty");
        }
        String filename = filenamePrefix + "_" + System.currentTimeMillis() + ".jpg";
        ContentResolver resolver = context.getContentResolver();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "Taktak");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);

            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                throw new IOException("Failed to create MediaStore entry");
            }

            try {
                try (OutputStream out = resolver.openOutputStream(uri)) {
                    if (out == null) {
                        throw new IOException("Failed to open output stream for MediaStore Uri");
                    }
                    out.write(jpegBytes);
                    out.flush();
                }

                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                resolver.update(uri, values, null, null);
                return uri;
            } catch (Exception failure) {
                resolver.delete(uri, null, null);
                if (failure instanceof IOException) {
                    throw (IOException) failure;
                }
                throw new IOException("Failed to save image to gallery", failure);
            }
        } else {
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File taktakDir = new File(picturesDir, "Taktak");
            if (!taktakDir.exists() && !taktakDir.mkdirs()) {
                throw new IOException("Failed to create Taktak pictures directory");
            }
            File imageFile = new File(taktakDir, filename);
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                fos.write(jpegBytes);
                fos.flush();
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.DATA, imageFile.getAbsolutePath());
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                return Uri.fromFile(imageFile);
            }
            return uri;
        }
    }

    /**
     * Lưu Bitmap vào Bộ sưu tập thiết bị.
     */
    @NonNull
    public static Uri saveBitmap(@NonNull Context context, @NonNull Bitmap bitmap, @NonNull String filenamePrefix) throws IOException {
        if (bitmap == null || bitmap.isRecycled()) {
            throw new IOException("Bitmap is invalid or recycled");
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
            throw new IOException("Failed to compress bitmap to JPEG");
        }
        return saveJpeg(context, stream.toByteArray(), filenamePrefix);
    }
}
