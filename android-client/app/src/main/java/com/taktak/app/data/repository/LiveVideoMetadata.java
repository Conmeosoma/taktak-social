package com.taktak.app.data.repository;

import android.media.MediaMetadataRetriever;
import java.io.File;
import java.io.IOException;

/** Metadata đọc từ file cache, không tin giá trị cố định của UI. */
public record LiveVideoMetadata(int durationMs, boolean hasAudio) {
    public static LiveVideoMetadata read(File file) throws IOException {
        if (file.length() <= 0 || file.length() > 10 * 1024 * 1024) {
            throw new IOException("Invalid live video size.");
        }
        MediaMetadataRetriever reader = new MediaMetadataRetriever();
        try {
            reader.setDataSource(file.getAbsolutePath());
            int duration = Integer.parseInt(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            if (duration <= 0 || duration > 3300
                    || !"yes".equals(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO))) {
                throw new IOException("Invalid live video duration.");
            }
            return new LiveVideoMetadata(duration,
                    "yes".equals(reader.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)));
        } catch (RuntimeException failure) {
            throw new IOException("Invalid live video.", failure);
        } finally {
            reader.release();
        }
    }
}
