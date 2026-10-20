package com.taktak.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import com.taktak.app.R;
import com.taktak.app.databinding.ActivityLoginBinding;
import com.taktak.app.ui.theme.ThemeManager;
import com.taktak.app.ui.theme.ThemePreferences;
import com.taktak.app.ui.theme.ThemeSelection;

public class LoginActivity extends AuthFormActivity {
    public static final String SESSION_NOTICE = "session_notice";
    private ActivityLoginBinding binding;
    private ThemeSelection appliedTheme;
    private boolean themeRecreating;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        appliedTheme = new ThemePreferences(this).load();
        ThemeManager.applyBeforeCreate(this, appliedTheme, ThemeManager.ScreenRole.LOGIN);
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configureInsets(binding.getRoot());
        // Trang trí vẽ dưới system bars; chỉ form nhận safe/IME insets.
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            var safe = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()
                    | androidx.core.view.WindowInsetsCompat.Type.ime()
                    | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            binding.loginSafeContent.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            if (insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())) {
                view.post(() -> {
                    View focused = getCurrentFocus();
                    if (focused != null) { focused.requestRectangleOnScreen(
                            new android.graphics.Rect(0, 0, focused.getWidth(), focused.getHeight()), false); }
                });
            }
            return insets;
        });
        int surface = com.google.android.material.color.MaterialColors.getColor(binding.getRoot(),
                com.google.android.material.R.attr.colorSurface);
        var bars = androidx.core.view.WindowCompat.getInsetsController(getWindow(), binding.getRoot());
        boolean light = androidx.core.graphics.ColorUtils.calculateLuminance(surface) > .5;
        bars.setAppearanceLightStatusBars(light);
        bars.setAppearanceLightNavigationBars(light);
        binding.backButton.setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        binding.languageLabel.setText(com.taktak.app.ui.language.LanguageManager.getCurrentLanguageName());
        binding.languageLabel.setOnClickListener(view -> {
            if (!isLoading()) {
                com.taktak.app.ui.language.LanguageManager.showLanguageDialog(this, () -> {
                    binding.languageLabel.setText(com.taktak.app.ui.language.LanguageManager.getCurrentLanguageName());
                });
            }
        });
        binding.loginThemeButton.setOnClickListener(view -> {
            if (!isLoading()) { startActivity(new Intent(this, com.taktak.app.ui.theme.ThemeSettingsActivity.class)); }
        });
        var wordmark = new android.text.SpannableString(getString(R.string.login_wordmark));
        wordmark.setSpan(new android.text.style.ForegroundColorSpan(
                com.google.android.material.color.MaterialColors.getColor(binding.getRoot(),
                        androidx.appcompat.R.attr.colorPrimary)),
                wordmark.length()-1, wordmark.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.authBrand.setText(wordmark);
        observeAuth(binding.formNotice, () -> {
            binding.passwordInput.setText("");
            startActivity(new Intent(this, com.taktak.app.MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
        binding.submitButton.setOnClickListener(view -> submit());
        binding.forgotPasswordLabel.setOnClickListener(view -> {
            if (!isLoading()) startActivity(new Intent(this,ForgotPasswordActivity.class));
        });
        binding.verifyEmailButton.setOnClickListener(view -> {
            if (!isLoading()) startActivity(new Intent(this,EmailVerificationActivity.class));
        });
        viewModel.state().observe(this, state -> {
            if (state.resetRequired) {
                binding.passwordInput.setText("");
                startActivity(new Intent(this,ChangePasswordActivity.class)); finish();
            }
        });
        binding.switchButton.setOnClickListener(view -> {
            if (!isLoading()) { startActivity(new Intent(this, RegisterActivity.class)); }
        });
        binding.passwordInput.setOnEditorActionListener((view, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) { submit(); return true; }
            return false;
        });
        android.text.TextWatcher themeWatcher = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            public void afterTextChanged(android.text.Editable text) { binding.getRoot().post(() -> reconcileTheme()); }
        };
        binding.emailInput.addTextChangedListener(themeWatcher);
        binding.passwordInput.addTextChangedListener(themeWatcher);
    }

    @Override protected void onResume() {
        super.onResume();
        binding.getRoot().post(() -> reconcileTheme());
    }

    private void reconcileTheme() {
        if (isFinishing() || themeRecreating || !getLifecycle().getCurrentState()
                .isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) return;
        ThemeSelection saved = new ThemePreferences(this).load();
        boolean changed = saved.style() != appliedTheme.style() || saved.colorMode() != appliedTheme.colorMode();
        binding.themePendingNotice.setVisibility(changed ? View.VISIBLE : View.GONE);
        // Không recreate khi có draft/loading hoặc khi auth đang điều hướng sang màn khác.
        var auth = viewModel.state().getValue();
        if (changed && !isLoading() && binding.emailInput.length() == 0 && binding.passwordInput.length() == 0
                && auth != null && !auth.success && !auth.resetRequired) {
            themeRecreating = true;
            recreate();
        }
    }

    @Override protected int initialNotice() {
        int notice = getIntent().getIntExtra(SESSION_NOTICE, 0);
        return notice == R.string.session_logged_out || notice == R.string.session_logout_local_only
                || notice == R.string.auth_invalid_credentials || notice == R.string.recovery_changed ? notice : 0;
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
        binding.loginThemeButton.setEnabled(!loading);
        binding.submitButton.setEnabled(!loading);
        binding.switchButton.setEnabled(!loading);
        binding.forgotPasswordLabel.setEnabled(!loading);
        binding.verifyEmailButton.setEnabled(!loading);
        binding.submitButton.setAlpha(loading ? 0.55f : 1f);
        binding.switchButton.setAlpha(loading ? 0.55f : 1f);
        binding.emailLayout.setEnabled(!loading);
        binding.passwordLayout.setEnabled(!loading);
        binding.loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.submitButton.setText(loading ? R.string.auth_processing : R.string.auth_login);
        if (!loading) binding.getRoot().post(() -> reconcileTheme());
    }
}
