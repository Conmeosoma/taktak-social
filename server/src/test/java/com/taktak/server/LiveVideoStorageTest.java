package com.taktak.server;

import com.taktak.server.storage.PrivateImageStorage;
import com.taktak.server.moment.MomentFailure;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;

class LiveVideoStorageTest {
    @TempDir Path root;
    private byte[] clip(String name) throws Exception {
        try (var input = getClass().getResourceAsStream("/live-video/" + name)) {
            return input.readAllBytes();
        }
    }
    private PrivateImageStorage.ValidatedVideo validate(byte[] bytes) throws Exception {
        return new PrivateImageStorage(root.toString()).validateVideo(
                new MockMultipartFile("video", "live.mp4", "video/mp4", bytes));
    }
    @Test void decodesSilentClipAndReadsActualDuration() throws Exception {
        var result = validate(clip("silent-3s.mp4"));
        assertThat(result.durationMs()).isEqualTo(3000);
        assertThat(result.hasAudio()).isFalse();
    }
    @Test void decodesAudioTrackAndReadsActualDuration() throws Exception {
        var result = validate(clip("audio-3s.mp4"));
        assertThat(result.durationMs()).isBetween(3000, 3100);
        assertThat(result.hasAudio()).isTrue();
    }
    @Test void rejectsOverlongVideo() throws Exception {
        byte[] bytes = clip("long-4s.mp4");
        assertThatThrownBy(() -> validate(bytes)).isInstanceOf(MomentFailure.class);
    }
    @Test void rejectsTruncatedRealVideo() throws Exception {
        byte[] bytes = clip("silent-3s.mp4");
        assertThatThrownBy(() -> validate(Arrays.copyOf(bytes, bytes.length / 2))).isInstanceOf(MomentFailure.class);
    }
    @Test void rejectsSignatureWithoutPlayableTracks() {
        byte[] bytes = new byte[1024];
        bytes[3] = 24; bytes[4] = 'f'; bytes[5] = 't'; bytes[6] = 'y'; bytes[7] = 'p';
        assertThatThrownBy(() -> validate(bytes)).isInstanceOf(MomentFailure.class);
    }
    @Test void rejectsCorruptFramesEvenWhenContainerHasMetadata() throws Exception {
        byte[] bytes = clip("silent-3s.mp4");
        boolean corrupted = false;
        for (int i = 4; i < bytes.length - 4; i++) {
            if (bytes[i] == 'm' && bytes[i+1] == 'd' && bytes[i+2] == 'a' && bytes[i+3] == 't') {
                int size = java.nio.ByteBuffer.wrap(bytes, i-4, 4).getInt();
                Arrays.fill(bytes, i+4, i-4+size, (byte) 0);
                corrupted = true; break;
            }
        }
        assertThat(corrupted).isTrue();
        assertThatThrownBy(() -> validate(bytes)).isInstanceOfSatisfying(MomentFailure.class,
                failure -> assertThat(failure.status.value()).isEqualTo(415));
    }
}
