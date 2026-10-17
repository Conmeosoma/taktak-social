package com.taktak.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import com.taktak.app.R;
import com.taktak.app.databinding.ActivityLoginBinding;

public class LoginActivity extends AuthFormActivity {
    public static final String SESSION_NOTICE = "session_notice";
    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configureInsets(binding.getRoot());
        observeAuth(binding.formNotice, () -> {
            binding.passwordInput.setText("");
            startActivity(new Intent(this, com.taktak.app.MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
        binding.submitButton.setOnClickListener(view -> submit());
        binding.switchButton.setOnClickListener(view -> {
            if (!isLoading()) { startActivity(new Intent(this, RegisterActivity.class)); }
        });
        binding.passwordInput.setOnEditorActionListener((view, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) { submit(); return true; }
            return false;
        });
    }

    @Override protected int initialNotice() {
        int notice = getIntent().getIntExtra(SESSION_NOTICE, 0);
        return notice == R.string.session_logged_out || notice == R.string.session_logout_local_only
                || notice == R.string.auth_invalid_credentials ? notice : 0;
    }

    private void submit() {
        if (isLoading()) { return; }
        binding.formNotice.setVisibility(View.GONE);
        boolean emailValid = showError(binding.emailLayout, AuthInputValidator.emailError(binding.emailInput.getText().toString()));
        boolean passwordValid = showError(binding.passwordLayout, AuthInputValidator.passwordError(binding.passwordInput.getText().toString()));
        if (!emailValid) { binding.emailInput.requestFocus(); return; }
        if (!passwordValid) { binding.passwordInput.requestFocus(); return; }
        if (ensureNetworkPermission()) {
            viewModel.login(AuthInputValidator.strip(binding.emailInput.getText().toString()).toLowerCase(java.util.Locale.ROOT),
                    binding.passwordInput.getText().toString());
        }
    }

    @Override
    protected void renderLoading(boolean loading) {
        binding.submitButton.setEnabled(!loading);
        binding.switchButton.setEnabled(!loading);
        binding.emailLayout.setEnabled(!loading);
        binding.passwordLayout.setEnabled(!loading);
        binding.loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.submitButton.setText(loading ? R.string.auth_processing : R.string.auth_login);
    }
}
