package com.taktak.app.data.repository;

import com.taktak.app.data.remote.*;
import com.taktak.app.data.session.Session;
import java.io.IOException;
import java.util.UUID;
import okhttp3.*;

public final class MomentRepository {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    public boolean publishIfCurrent(Runnable action) {
        return owner != null && client.sessions.runIfCurrent(owner.generation, action);
    }
    private final ApiClient client;
    private final Session owner;
    public MomentRepository(ApiClient client) { this.client = client; this.owner = client.sessions.snapshot(); }

    public interface ProgressListener {
        void onProgress(int percent, long bytesWritten, long totalBytes);
    }

    public String upload(byte[] jpeg) throws IOException {
        return upload(jpeg, "");
    }
    public String upload(byte[] jpeg, String caption) throws IOException {
        return upload(jpeg, caption, java.util.List.of());
    }
    public String upload(byte[] jpeg, String caption, java.util.List<String> recipients) throws IOException {
        return upload(jpeg, caption, recipients, null, null);
    }
    public String upload(byte[] jpeg, String caption, java.util.List<String> recipients, String idempotencyKey,
            ProgressListener progressListener) throws IOException {
        if (caption == null || caption.codePointCount(0, caption.length()) > 50 || caption.indexOf('\0') >= 0) {
            throw new ApiFailure(ApiFailure.Kind.VALIDATION);
        }
        if (recipients == null || recipients.size() > 100) { throw new ApiFailure(ApiFailure.Kind.VALIDATION); }
        var unique = new java.util.HashSet<String>();
        for (String id : recipients) {
            if (!com.taktak.app.data.model.FriendModels.uuid(id) || !unique.add(UUID.fromString(id).toString())) {
                throw new ApiFailure(ApiFailure.Kind.VALIDATION);
            }
        }
        if (idempotencyKey != null && !com.taktak.app.data.model.FriendModels.uuid(idempotencyKey)) {
            throw new ApiFailure(ApiFailure.Kind.VALIDATION);
        }
        Session owner = requireSession();
        RequestBody rawImage = RequestBody.create(jpeg, MediaType.get("image/jpeg"));
        RequestBody imageBody = progressListener == null ? rawImage : new CountingRequestBody(rawImage, (bytesWritten, contentLength) -> {
            int percent = contentLength > 0 ? (int) Math.min(100, Math.max(0, (100 * bytesWritten) / contentLength)) : 0;
            progressListener.onProgress(percent, bytesWritten, contentLength);
        });
        var image = MultipartBody.Part.createFormData("image", "moment.jpg", imageBody);
        var text = MediaType.get("text/plain; charset=utf-8");
        var parts = recipients.isEmpty() ? java.util.List.of(RequestBody.create("[]", text))
                : recipients.stream().map(id -> RequestBody.create(id, text)).collect(java.util.stream.Collectors.toList());
        var response = client.moments.uploadToFriends(idempotencyKey, image, parts, RequestBody.create(caption, text)).execute();
        if (!response.isSuccessful()) { throw ApiFailure.from(response); }
        ensureCurrent(owner);
        if (response.body() == null || response.body().id == null) { throw new ApiFailure(ApiFailure.Kind.SERVER); }
        try { return UUID.fromString(response.body().id).toString(); }
        catch (IllegalArgumentException invalid) { throw new ApiFailure(ApiFailure.Kind.SERVER); }
    }

    public byte[] download(String id) throws IOException {
        try { return downloadOnce(id); }
        catch (ApiFailure failure) { throw failure; }
        catch (IOException transport) {
            // GET an toàn để thử lại một lần; không retry POST hoặc lỗi HTTP/quyền/phiên.
            return downloadOnce(id);
        }
    }
    private byte[] downloadOnce(String id) throws IOException {
        Session owner = requireSession();
        var response = client.moments.image(id).execute();
        if (!response.isSuccessful()) { throw ApiFailure.from(response); }
        if (response.body() == null) { throw new ApiFailure(ApiFailure.Kind.SERVER); }
        byte[] bytes;
        try (var body = response.body(); var input = body.byteStream()) {
            if (body.contentLength() > MAX_BYTES || body.contentType() == null || !body.contentType().toString().startsWith("image/jpeg")) {
                throw new ApiFailure(ApiFailure.Kind.SERVER);
            }
            try (var output = new java.io.ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int read;
                while (output.size() <= MAX_BYTES && (read = input.read(buffer, 0,
                        Math.min(buffer.length, MAX_BYTES + 1 - output.size()))) != -1) { output.write(buffer, 0, read); }
                bytes = output.toByteArray();
            }
        }
        ensureCurrent(owner);
        if (bytes.length > MAX_BYTES || bytes.length < 3) { throw new ApiFailure(ApiFailure.Kind.SERVER); }
        var options = new android.graphics.BitmapFactory.Options(); options.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        if (options.outWidth < 1 || options.outHeight < 1 || options.outWidth > 4096 || options.outHeight > 4096) {
            throw new ApiFailure(ApiFailure.Kind.SERVER);
        }
        return bytes;
    }

    public com.taktak.app.data.model.MomentModels.MomentsPage received(int page) throws IOException {
        return list("received", page);
    }

    public com.taktak.app.data.model.MomentModels.MomentsPage sent(int page) throws IOException {
        return list("sent", page);
    }

    public com.taktak.app.data.model.MomentModels.MomentsPage list(String type, int page) throws IOException {
        if (page < 0 || (!"received".equals(type) && !"sent".equals(type))) {
            throw new ApiFailure(ApiFailure.Kind.VALIDATION);
        }
        Session owner = requireSession();
        var response = client.moments.listMoments(type, page).execute();
        if (!response.isSuccessful()) { throw ApiFailure.from(response); }
        ensureCurrent(owner);
        var body = response.body();
        if (body == null || body.items == null) { throw new ApiFailure(ApiFailure.Kind.SERVER); }
        for (var item : body.items) {
            if (item == null || !item.valid()) { throw new ApiFailure(ApiFailure.Kind.SERVER); }
        }
        return body;
    }

    public void delete(String id) throws IOException {
        if (!com.taktak.app.data.model.FriendModels.uuid(id)) { throw new ApiFailure(ApiFailure.Kind.VALIDATION); }
        Session owner = requireSession();
        var response = client.moments.deleteMoment(id).execute();
        if (!response.isSuccessful()) { throw ApiFailure.from(response); }
        ensureCurrent(owner);
    }

    public com.taktak.app.data.model.MomentModels.MomentItem latest() throws IOException {
        Session owner = requireSession();
        var response = client.moments.latest().execute();
        if (response.code() == 204) { ensureCurrent(owner); return null; }
        if (!response.isSuccessful()) { throw ApiFailure.from(response); }
        ensureCurrent(owner);
        var body = response.body();
        if (body == null || !body.valid()) { throw new ApiFailure(ApiFailure.Kind.SERVER); }
        return body;
    }

    private Session requireSession() throws IOException {
        Session session = client.sessions.snapshot();
        if (session == null) { throw new ApiFailure(ApiFailure.Kind.UNAUTHORIZED); }
        if (owner == null || session.generation != owner.generation) { throw new ApiFailure(ApiFailure.Kind.CANCELLED); }
        return session;
    }
    private void ensureCurrent(Session owner) throws IOException {
        Session current = client.sessions.snapshot();
        if (current == null || current.generation != owner.generation) { throw new ApiFailure(ApiFailure.Kind.CANCELLED); }
    }
}
