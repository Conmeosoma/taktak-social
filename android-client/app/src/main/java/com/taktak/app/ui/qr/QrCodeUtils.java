package com.taktak.app.ui.qr;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class QrCodeUtils {
    private QrCodeUtils() { }

    public static final class FriendPayload {
        @NonNull public final String username;
        @Nullable public final String userId;

        public FriendPayload(@NonNull String username, @Nullable String userId) {
            this.username = username;
            this.userId = userId;
        }
    }

    public static String buildFriendPayload(@NonNull String username, @Nullable String userId) {
        if (!username.matches("[a-z0-9_]{1,255}") || userId != null && !com.taktak.app.data.model.FriendModels.uuid(userId)) {
            throw new IllegalArgumentException("Invalid public friend identity");
        }
        String base = "taktak://friend?u=" + Uri.encode(username);
        if (userId != null && !userId.trim().isEmpty()) {
            base += "&id=" + Uri.encode(userId);
        }
        return base;
    }

    @Nullable
    public static FriendPayload parsePayload(@Nullable String raw) {
        if (raw == null || raw.length() > 2048 || raw.trim().isEmpty()) return null;
        String text = raw.trim();
        try {
            String simple = text.startsWith("@") ? text.substring(1) : text;
            if (simple.matches("[a-zA-Z0-9_]{1,255}")) return new FriendPayload(simple.toLowerCase(java.util.Locale.ROOT), null);
            java.net.URI uri = new java.net.URI(text);
            if (uri.getUserInfo() != null || uri.getPort() != -1 || uri.getFragment() != null) return null;
            boolean app = "taktak".equalsIgnoreCase(uri.getScheme());
            boolean web = "https".equalsIgnoreCase(uri.getScheme()) && "taktak.app".equalsIgnoreCase(uri.getHost());
            String path = uri.getPath();
            boolean friend = app && "friend".equalsIgnoreCase(uri.getHost()) && "".equals(path)
                    || web && "/friend".equals(path);
            if (friend) {
                java.util.Map<String,String> params = new java.util.HashMap<>();
                if (uri.getRawQuery() == null) return null;
                for (String part : uri.getRawQuery().split("&", -1)) {
                    String[] pair = part.split("=", 2);
                    if (pair.length != 2) return null;
                    String key = java.net.URLDecoder.decode(pair[0], "UTF-8");
                    String value = java.net.URLDecoder.decode(pair[1], "UTF-8");
                    if (!(key.equals("u") || key.equals("id")) || params.put(key, value) != null) return null;
                }
                String username = params.get("u"), id = params.get("id");
                if (username == null || !username.matches("[a-zA-Z0-9_]{1,255}")
                        || id != null && !com.taktak.app.data.model.FriendModels.uuid(id)) return null;
                return new FriendPayload(username.toLowerCase(java.util.Locale.ROOT), id);
            }
            if (uri.getQuery() != null) return null;
            String username = null;
            if (app && "user".equalsIgnoreCase(uri.getHost()) && path != null && path.startsWith("/")) username = path.substring(1);
            if (web && path != null && path.startsWith("/u/")) username = path.substring(3);
            if (username != null && username.matches("[a-zA-Z0-9_]{1,255}")) return new FriendPayload(username.toLowerCase(java.util.Locale.ROOT), null);
        } catch (Exception ignored) { }
        return null;
    }

    @Nullable
    public static Bitmap generateQrBitmap(@NonNull String content, int size, int fgColor, int bgColor) {
        if (size < 128 || size > 2048 || content.isEmpty() || content.length() > 2048) return null;
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 4);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = bitMatrix.get(x, y) ? fgColor : bgColor;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static String decodeFromBitmap(@NonNull Bitmap bitmap) {
        Bitmap readable = null;
        try {
            if (bitmap.isRecycled() || (long) bitmap.getWidth() * bitmap.getHeight() > 4096L * 4096) return null;
            if (bitmap.getConfig() == Bitmap.Config.HARDWARE) {
                readable = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                if (readable == null) return null;
                bitmap = readable;
            }
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if ((long) width * height > 4096L * 4096) return null;
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(DecodeHintType.TRY_HARDER, true);

            MultiFormatReader reader = new MultiFormatReader();
            Result result;
            try { result = reader.decode(binaryBitmap, hints); }
            catch (com.google.zxing.NotFoundException missing) {
                reader.reset(); result = reader.decode(new BinaryBitmap(new HybridBinarizer(source.invert())), hints);
            }
            return result != null ? result.getText() : null;
        } catch (Exception e) {
            return null;
        } finally {
            if (readable != null) readable.recycle();
        }
    }
}
