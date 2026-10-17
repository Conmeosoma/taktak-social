package com.taktak.app.data.repository;

import com.taktak.app.data.remote.*;
import com.taktak.app.data.session.Session;
import java.io.IOException;
import java.util.UUID;
import okhttp3.*;

public final class MomentRepository {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private final ApiClient client;
    private final Session owner;
    public MomentRepository(ApiClient client) { this.client = client; this.owner = client.sessions.snapshot(); }

    public String upload(byte[] jpeg) throws IOException {
        Session owner = requireSession();
        var image = MultipartBody.Part.createFormData("image", "moment.jpg", RequestBody.create(jpeg, MediaType.get("image/jpeg")));
        var response = client.moments.upload(image, RequestBody.create("[]", MediaType.get("text/plain"))).execute();
        if (!response.isSuccessful()) { throw ApiFailure.from(response); }
        ensureCurrent(owner);
        if (response.body() == null || response.body().id == null) { throw new ApiFailure(ApiFailure.Kind.SERVER); }
        try { return UUID.fromString(response.body().id).toString(); }
        catch (IllegalArgumentException invalid) { throw new ApiFailure(ApiFailure.Kind.SERVER); }
    }

    public byte[] download(String id) throws IOException {
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
