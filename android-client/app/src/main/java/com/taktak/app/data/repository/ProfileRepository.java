package com.taktak.app.data.repository;

import com.taktak.app.data.remote.*;
import com.taktak.app.data.session.Session;
import com.taktak.app.data.model.AuthModels.*;
import java.io.*;
import okhttp3.*;
import retrofit2.Response;

public final class ProfileRepository {
    private final ApiClient client;
    private final Session owner;
    public ProfileRepository(ApiClient client) { this.client=client; owner=client.sessions.snapshot(); }
    public boolean publishIfCurrent(Runnable action) { return owner!=null && client.sessions.runIfCurrent(owner.generation,action); }
    private void ensureCurrent() throws ApiFailure { if(!publishIfCurrent(() -> {})) throw new ApiFailure(ApiFailure.Kind.CANCELLED); }
    public UserProfile load() throws IOException { ensureCurrent(); return checked(client.users.me().execute()); }
    public UserProfile update(String name) throws IOException {
        ensureCurrent(); name=name==null ? "" : name.strip();
        if(name.isEmpty() || name.length()>255) throw new ApiFailure(ApiFailure.Kind.VALIDATION);
        return checked(client.users.update(new UpdateProfileRequest(name)).execute());
    }
    public UserProfile upload(byte[] bytes) throws IOException {
        ensureCurrent();
        if(bytes==null || bytes.length==0 || bytes.length>5*1024*1024) throw new ApiFailure(ApiFailure.Kind.VALIDATION);
        return checked(client.users.uploadAvatar(MultipartBody.Part.createFormData("image","avatar.jpg",
                RequestBody.create(bytes,MediaType.get("image/jpeg")))).execute());
    }
    public byte[] avatar(String id) throws IOException {
        ensureCurrent();
        if(!com.taktak.app.data.model.FriendModels.uuid(id)) throw new ApiFailure(ApiFailure.Kind.VALIDATION);
        var response=client.users.avatar(id).execute(); ensureCurrent();
        if(response.code()==204 || response.code()==404) return null;
        if(!response.isSuccessful()) throw ApiFailure.from(response);
        if(response.body()==null) throw new ApiFailure(ApiFailure.Kind.SERVER);
        try(var body=response.body();var input=body.byteStream();var output=new ByteArrayOutputStream()) {
            if(body.contentLength()>1024*1024 || body.contentType()==null || !body.contentType().toString().startsWith("image/jpeg")) throw new ApiFailure(ApiFailure.Kind.SERVER);
            byte[] buffer=new byte[8192]; int read;
            while((read=input.read(buffer))!=-1) { output.write(buffer,0,read); if(output.size()>1024*1024) throw new ApiFailure(ApiFailure.Kind.SERVER); }
            ensureCurrent(); return output.toByteArray();
        }
    }
    private UserProfile checked(Response<UserProfile> response) throws IOException {
        ensureCurrent(); if(!response.isSuccessful()) throw ApiFailure.from(response);
        if(response.body()==null || !response.body().isValid()) throw new ApiFailure(ApiFailure.Kind.SERVER);
        return response.body();
    }
}
