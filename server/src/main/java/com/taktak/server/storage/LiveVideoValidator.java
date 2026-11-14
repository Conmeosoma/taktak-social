package com.taktak.server.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taktak.server.moment.MomentFailure;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;

/** Giải mã nội bộ, không cho media mở URL hoặc ghi log nội dung người dùng. */
final class LiveVideoValidator {
    private static final Semaphore SLOTS = new Semaphore(2);
    private static final ObjectMapper JSON = new ObjectMapper();
    record Metadata(int durationMs, boolean hasAudio) { }

    Metadata inspect(byte[] bytes) throws IOException {
        if (bytes.length < 16 || bytes[4] != 'f' || bytes[5] != 't' || bytes[6] != 'y' || bytes[7] != 'p') {
            throw invalid();
        }
        if (!SLOTS.tryAcquire()) { throw unavailable(); }
        Path dir = null;
        try {
            dir = Files.createTempDirectory("taktak-live-");
            if (Files.getFileStore(dir).supportsFileAttributeView("posix")) {
                Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwx------"));
            }
            Path input = dir.resolve("input.mp4"), output = dir.resolve("probe.json");
            Files.write(input, bytes);
            run(List.of(binary("TAKTAK_FFPROBE_PATH", "ffprobe"), "-v", "error",
                    "-protocol_whitelist", "file,pipe", "-f", "mov", "-i", input.toString(),
                    "-show_entries", "format=duration:stream=codec_type,codec_name,width,height,duration",
                    "-of", "json"), output);
            if (Files.size(output) > 65536) { throw invalid(); }
            JsonNode probe = JSON.readTree(Files.readAllBytes(output));
            JsonNode streams = probe.path("streams");
            int videoCount = 0, audioCount = 0;
            for (JsonNode stream : streams) {
                String type = stream.path("codec_type").asText();
                if ("video".equals(type)) {
                    videoCount++;
                    int width = stream.path("width").asInt(), height = stream.path("height").asInt();
                    if (!"h264".equals(stream.path("codec_name").asText()) || width < 1 || height < 1
                            || width > 1920 || height > 1920 || (long) width * height > 2073600) { throw invalid(); }
                } else if ("audio".equals(type)) {
                    audioCount++;
                    if (!"aac".equals(stream.path("codec_name").asText())) { throw invalid(); }
                } else { throw invalid(); }
                if (stream.has("duration")) { duration(stream.path("duration").asDouble(Double.NaN)); }
            }
            if (videoCount != 1 || audioCount > 1) { throw invalid(); }
            int duration = duration(probe.path("format").path("duration").asDouble(Double.NaN));
            run(List.of(binary("TAKTAK_FFMPEG_PATH", "ffmpeg"), "-nostdin", "-v", "error", "-xerror",
                    "-max_alloc", "67108864", "-threads", "1", "-protocol_whitelist", "file,pipe",
                    "-f", "mov", "-i", input.toString(), "-map", "0:v:0", "-map", "0:a?",
                    "-threads", "1", "-progress", "pipe:1", "-f", "null", "-"), output);
            if (Files.size(output) > 65536) { throw invalid(); }
            long frames = 0, decodedUs = 0;
            for (String line : Files.readAllLines(output)) {
                if (line.startsWith("frame=")) { frames = Long.parseLong(line.substring(6).strip()); }
                if (line.startsWith("out_time_us=")) { decodedUs = Long.parseLong(line.substring(12).strip()); }
            }
            if (frames < 1 || decodedUs <= 0 || decodedUs > 3300000) { throw invalid(); }
            return new Metadata(duration, audioCount == 1);
        } catch (NumberFormatException malformed) {
            throw invalid();
        } finally {
            try {
                if (dir != null) {
                    Files.deleteIfExists(dir.resolve("input.mp4"));
                    Files.deleteIfExists(dir.resolve("probe.json"));
                    Files.deleteIfExists(dir);
                }
            } finally { SLOTS.release(); }
        }
    }

    private static int duration(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0 || seconds > 3.3) { throw invalid(); }
        return (int) Math.round(seconds * 1000);
    }

    private static String binary(String name, String fallback) {
        String configured = System.getenv(name);
        return configured == null || configured.isBlank() ? fallback : configured;
    }

    private static void run(List<String> args, Path output) throws IOException {
        Process process;
        try {
            process = new ProcessBuilder(args).redirectOutput(output.toFile())
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        } catch (IOException missingTool) { throw unavailable(); }
        try {
            if (!process.waitFor(15, TimeUnit.SECONDS)) { throw unavailable(); }
            if (process.exitValue() != 0) { throw invalid(); }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                try { process.waitFor(2, TimeUnit.SECONDS); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }
        }
    }

    private static MomentFailure invalid() { return new MomentFailure(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVALID_VIDEO"); }
    private static MomentFailure unavailable() { return new MomentFailure(HttpStatus.SERVICE_UNAVAILABLE, "VIDEO_VALIDATION_UNAVAILABLE"); }
}
