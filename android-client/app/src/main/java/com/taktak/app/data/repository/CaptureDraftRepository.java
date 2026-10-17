package com.taktak.app.data.repository;

import android.content.Context;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.remote.ApiFailure;
import java.io.*;
import java.nio.file.Files;
import java.util.UUID;

/** Cache nội bộ, khóa theo generation; Intent chỉ truyền ID, không nhận đường dẫn. */
public final class CaptureDraftRepository {
    public static final long MAX_AGE_MILLIS = 24 * 60 * 60 * 1000L;
    private final AuthRepository auth;
    private final File directory;
    public final long generation;

    public CaptureDraftRepository(Context context, long generation) throws IOException {
        auth = AuthGraph.repository(context);
        this.generation = generation;
        directory = new File(new File(context.getCacheDir(), "camera-drafts"), Long.toString(generation));
        requireCurrent();
    }
    public void requireCurrent() throws IOException {
        if (auth.sessionGeneration() != generation) { throw new ApiFailure(ApiFailure.Kind.CANCELLED); }
    }
    public String write(byte[] jpeg) throws IOException {
        requireCurrent();
        if (jpeg == null || jpeg.length < 3 || jpeg.length > MomentRepository.MAX_BYTES) { throw new IOException("Invalid draft size"); }
        if (!directory.isDirectory() && !directory.mkdirs()) { throw new IOException("Cache unavailable"); }
        String id = UUID.randomUUID().toString(); File file = path(id);
        try {
            try (var output = new FileOutputStream(file)) { output.write(jpeg); }
            requireCurrent(); return id;
        } catch (IOException failure) { Files.deleteIfExists(file.toPath()); throw failure; }
    }
    public File file(String id) throws IOException {
        requireCurrent(); File file = path(id);
        if (!file.isFile() || file.length() < 3 || file.length() > MomentRepository.MAX_BYTES
                || System.currentTimeMillis() - file.lastModified() > MAX_AGE_MILLIS) {
            throw new IOException("Draft expired or missing");
        }
        return file;
    }
    public byte[] read(String id) throws IOException {
        byte[] bytes;
        try (var input = new FileInputStream(file(id)); var output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > MomentRepository.MAX_BYTES) { throw new IOException("Draft too large"); }
                output.write(buffer, 0, count);
            }
            bytes = output.toByteArray();
        }
        requireCurrent(); return bytes;
    }
    // Chỉ xóa ID của repository sở hữu, kể cả sau logout; không duyệt/xóa thư mục khác.
    public void discard(String id) throws IOException { Files.deleteIfExists(path(id).toPath()); }
    private File path(String id) throws IOException {
        if (id == null || !id.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")) { throw new IOException("Invalid draft ID"); }
        return new File(directory, id + ".jpg");
    }
}
