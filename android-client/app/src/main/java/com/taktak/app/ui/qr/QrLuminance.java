package com.taktak.app.ui.qr;

import java.nio.ByteBuffer;

/** Đọc plane Y có padding/pixel stride mà không đổi vị trí buffer CameraX. */
public final class QrLuminance {
    private QrLuminance() { }
    public record Frame(byte[] data, int width, int height) { }
    public static Frame unpack(ByteBuffer buffer, int width, int height, int rowStride, int pixelStride, int rotation) {
        if (width < 1 || height < 1 || (long) width * height > 4096L * 4096 || pixelStride < 1
                || rowStride < (long) (width - 1) * pixelStride + 1
                || rotation != 0 && rotation != 90 && rotation != 180 && rotation != 270) throw new IllegalArgumentException("Invalid Y plane");
        ByteBuffer input = buffer.duplicate(); int start = input.position();
        if ((long) start + (long) (height - 1) * rowStride + (long) (width - 1) * pixelStride >= input.limit())
            throw new IllegalArgumentException("Truncated Y plane");
        int outWidth = rotation == 90 || rotation == 270 ? height : width;
        int outHeight = rotation == 90 || rotation == 270 ? width : height;
        byte[] pixels = new byte[width * height];
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int index = switch (rotation) {
                case 90 -> x * height + height - 1 - y;
                case 180 -> (height - 1 - y) * width + width - 1 - x;
                case 270 -> (width - 1 - x) * height + y;
                default -> y * width + x;
            };
            pixels[index] = input.get(start + y * rowStride + x * pixelStride);
        }
        return new Frame(pixels, outWidth, outHeight);
    }
}
