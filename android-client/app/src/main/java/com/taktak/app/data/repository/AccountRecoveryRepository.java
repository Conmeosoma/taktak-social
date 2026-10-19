package com.taktak.app.data.repository;

import com.taktak.app.data.model.AuthModels.LoginRequest;
import com.taktak.app.data.model.RecoveryModels.*;
import com.taktak.app.data.remote.*;
import com.taktak.app.data.session.ResetSessionVault;
import java.io.IOException;
import retrofit2.Response;

public final class AccountRecoveryRepository {
    private final AuthApi api;
    public AccountRecoveryRepository(AuthApi api) { this.api=api; }
    public void forgot(String email) throws IOException { checked(api.forgot(new Email(email)).execute()); }
    public void requestVerification(String email, String password) throws IOException {
        checked(api.requestVerification(new LoginRequest(email,password)).execute());
    }
    public void verify(String email, String password, String code) throws IOException {
        checked(api.verify(new Verification(email,password,code)).execute());
    }
    public void reset(String temporary, String password, String confirmation) throws IOException {
        String token=ResetSessionVault.peek();
        if (token==null) throw new ApiFailure(ApiFailure.Kind.UNAUTHORIZED);
        checked(api.reset(new Reset(token,temporary,password,confirmation)).execute());
        ResetSessionVault.clear();
    }
    private void checked(Response<Notice> response) throws IOException {
        if (!response.isSuccessful()) throw ApiFailure.from(response);
        if (response.body()==null || response.body().message==null) throw new ApiFailure(ApiFailure.Kind.SERVER);
    }
}
