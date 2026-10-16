package com.taktak.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import com.taktak.app.R;
import com.taktak.app.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AuthFormActivity {
    private ActivityRegisterBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configureInsets(binding.getRoot());
        observeAuth(binding.formNotice, () -> {
            binding.passwordInput.setText("");
            binding.confirmPasswordInput.setText("");
        });
        binding.submitButton.setOnClickListener(view -> submit());
        binding.switchButton.setOnClickListener(view -> {
            if (!isLoading()) {
                startActivity(new Intent(this, LoginActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                finish();
            }
        });
        binding.confirmPasswordInput.setOnEditorActionListener((view, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) { submit(); return true; }
            return false;
        });
    }

    private void submit() {
        if (isLoading()) { return; }
        binding.formNotice.setVisibility(View.GONE);
        boolean nameValid = showError(binding.displayNameLayout, AuthInputValidator.displayNameError(binding.displayNameInput.getText().toString()));
        boolean usernameValid = showError(binding.usernameLayout, AuthInputValidator.usernameError(binding.usernameInput.getText().toString()));
        boolean emailValid = showError(binding.emailLayout, AuthInputValidator.emailError(binding.emailInput.getText().toString()));
        String password = binding.passwordInput.getText().toString();
        boolean passwordValid = showError(binding.passwordLayout, AuthInputValidator.passwordError(password));
        boolean confirmationValid = showError(binding.confirmPasswordLayout,
                AuthInputValidator.confirmationError(password, binding.confirmPasswordInput.getText().toString()));
        if (!nameValid) { binding.displayNameInput.requestFocus(); return; }
        if (!usernameValid) { binding.usernameInput.requestFocus(); return; }
        if (!emailValid) { binding.emailInput.requestFocus(); return; }
        if (!passwordValid) { binding.passwordInput.requestFocus(); return; }
        if (!confirmationValid) { binding.confirmPasswordInput.requestFocus(); return; }
        if (ensureNetworkPermission()) {
            viewModel.register(AuthInputValidator.strip(binding.emailInput.getText().toString()).toLowerCase(java.util.Locale.ROOT),
                    AuthInputValidator.strip(binding.usernameInput.getText().toString()).toLowerCase(java.util.Locale.ROOT),
                    AuthInputValidator.strip(binding.displayNameInput.getText().toString()), password);
        }
    }

    @Override
    protected void renderLoading(boolean loading) {
        binding.submitButton.setEnabled(!loading);
        binding.switchButton.setEnabled(!loading);
        binding.displayNameLayout.setEnabled(!loading);
        binding.usernameLayout.setEnabled(!loading);
        binding.emailLayout.setEnabled(!loading);
        binding.passwordLayout.setEnabled(!loading);
        binding.confirmPasswordLayout.setEnabled(!loading);
        binding.loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.submitButton.setText(loading ? R.string.auth_processing : R.string.auth_register);
    }
}
