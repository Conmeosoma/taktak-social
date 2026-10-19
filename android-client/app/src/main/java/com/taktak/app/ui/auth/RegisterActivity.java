package com.taktak.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import com.taktak.app.R;
import com.taktak.app.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AuthFormActivity {
    private ActivityRegisterBinding binding;
    private final Handler usernameHandler = new Handler(Looper.getMainLooper());
    private Runnable checkUsernameRunnable;
    private String lastCheckedUsername = "";
    private Boolean lastCheckedAvailable = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.taktak.app.ui.theme.ThemeManager.applyBeforeCreate(this,
                new com.taktak.app.ui.theme.ThemePreferences(this).load(),
                com.taktak.app.ui.theme.ThemeManager.ScreenRole.AUTH);
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        configureInsets(binding.getRoot());
        configureAuthBars(binding.getRoot());
        var brand = new android.text.SpannableString(getString(R.string.login_wordmark));
        brand.setSpan(new android.text.style.ForegroundColorSpan(com.google.android.material.color.MaterialColors.getColor(
                binding.getRoot(), androidx.appcompat.R.attr.colorPrimary)), brand.length()-1,
                brand.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        binding.authBrand.setText(brand);
        binding.registerBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            var safe = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()
                    | androidx.core.view.WindowInsetsCompat.Type.ime() | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            binding.registerSafeContent.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            if (insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())) {
                view.post(() -> {
                    View focused = getCurrentFocus();
                    if (focused != null) focused.requestRectangleOnScreen(new android.graphics.Rect(0,0,focused.getWidth(),focused.getHeight()),false);
                });
            }
            return insets;
        });
        androidx.core.view.ViewCompat.setAccessibilityHeading(binding.registerIdentity, true);
        androidx.core.view.ViewCompat.setAccessibilityHeading(binding.registerSecurity, true);
        observeAuth(binding.formNotice, () -> {
            binding.passwordInput.setText("");
            binding.confirmPasswordInput.setText("");
        });
        viewModel.state().observe(this, state -> {
            if (state.message == R.string.auth_email_conflict) {
                binding.emailLayout.setError(getString(R.string.auth_email_conflict));
                binding.emailInput.requestFocus();
            } else if (state.message == R.string.auth_username_conflict) {
                binding.usernameLayout.setError(getString(R.string.auth_username_conflict));
                binding.usernameInput.requestFocus();
            }
        });

        setupValidationListeners();

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

    private void setupValidationListeners() {
        // 1. Display Name
        binding.displayNameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                showError(binding.displayNameLayout, AuthInputValidator.displayNameError(binding.displayNameInput.getText().toString()));
            }
        });
        binding.displayNameInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                if (binding.displayNameLayout.getError() != null) {
                    showError(binding.displayNameLayout, AuthInputValidator.displayNameError(s.toString()));
                }
            }
        });

        // 2. Username
        binding.usernameInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                int err = AuthInputValidator.usernameError(binding.usernameInput.getText().toString());
                showError(binding.usernameLayout, err);
                if (err == 0) {
                    checkUsernameAvailabilityDebounced(true);
                }
            }
        });
        binding.usernameInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                checkUsernameAvailabilityDebounced(false);
            }
        });

        // 3. Email
        binding.emailInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                showError(binding.emailLayout, AuthInputValidator.emailError(binding.emailInput.getText().toString()));
            }
        });
        binding.emailInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                if (binding.emailLayout.getError() != null) {
                    showError(binding.emailLayout, AuthInputValidator.emailError(s.toString()));
                }
            }
        });

        // 4. Password
        binding.passwordInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                showError(binding.passwordLayout, AuthInputValidator.passwordError(binding.passwordInput.getText().toString()));
                if (binding.confirmPasswordInput.getText().length() > 0) {
                    showError(binding.confirmPasswordLayout, AuthInputValidator.confirmationError(
                            binding.passwordInput.getText().toString(), binding.confirmPasswordInput.getText().toString()));
                }
            }
        });
        binding.passwordInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                updatePasswordStrength(s.toString());
                if (binding.passwordLayout.getError() != null) {
                    showError(binding.passwordLayout, AuthInputValidator.passwordError(s.toString()));
                }
                if (binding.confirmPasswordLayout.getError() != null || binding.confirmPasswordInput.getText().length() > 0) {
                    showError(binding.confirmPasswordLayout, AuthInputValidator.confirmationError(
                            s.toString(), binding.confirmPasswordInput.getText().toString()));
                }
            }
        });

        // 5. Confirm Password
        binding.confirmPasswordInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                showError(binding.confirmPasswordLayout, AuthInputValidator.confirmationError(
                        binding.passwordInput.getText().toString(), binding.confirmPasswordInput.getText().toString()));
            }
        });
        binding.confirmPasswordInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                if (binding.confirmPasswordLayout.getError() != null) {
                    showError(binding.confirmPasswordLayout, AuthInputValidator.confirmationError(
                            binding.passwordInput.getText().toString(), s.toString()));
                }
            }
        });
    }

    private void checkUsernameAvailabilityDebounced(boolean immediate) {
        usernameHandler.removeCallbacks(checkUsernameRunnable);
        String text = binding.usernameInput.getText().toString();
        String username = AuthInputValidator.strip(text).toLowerCase(java.util.Locale.ROOT);
        if (username.isEmpty()) {
            lastCheckedUsername = "";
            lastCheckedAvailable = null;
            binding.usernameLayout.setHelperText(getString(R.string.auth_username_help));
            if (binding.usernameLayout.getError() != null) {
                showError(binding.usernameLayout, 0);
            }
            return;
        }
        int formatErr = AuthInputValidator.usernameError(text);
        if (formatErr != 0) {
            lastCheckedUsername = username;
            lastCheckedAvailable = false;
            if (immediate || text.length() >= 3) {
                showError(binding.usernameLayout, formatErr);
            }
            return;
        }
        if (username.equals(lastCheckedUsername) && lastCheckedAvailable != null) {
            if (lastCheckedAvailable) {
                binding.usernameLayout.setError(null);
                binding.usernameLayout.setHelperText(getString(R.string.auth_username_available));
            } else {
                binding.usernameLayout.setError(getString(R.string.auth_username_conflict));
            }
            return;
        }
        showError(binding.usernameLayout, 0);
        binding.usernameLayout.setHelperText(getString(R.string.auth_username_checking));
        checkUsernameRunnable = () -> {
            viewModel.checkUsername(username, available -> {
                if (isDestroyed() || isFinishing()) { return; }
                String current = AuthInputValidator.strip(binding.usernameInput.getText().toString()).toLowerCase(java.util.Locale.ROOT);
                if (!username.equals(current)) { return; }
                lastCheckedUsername = username;
                lastCheckedAvailable = available;
                if (available == null) {
                    binding.usernameLayout.setHelperText(getString(R.string.auth_username_help));
                } else if (available) {
                    binding.usernameLayout.setError(null);
                    binding.usernameLayout.setHelperText(getString(R.string.auth_username_available));
                } else {
                    binding.usernameLayout.setError(getString(R.string.auth_username_conflict));
                }
            });
        };
        if (immediate) {
            checkUsernameRunnable.run();
        } else {
            usernameHandler.postDelayed(checkUsernameRunnable, 400);
        }
    }

    private void updatePasswordStrength(String password) {
        AuthInputValidator.PasswordStrength strength = AuthInputValidator.calculatePasswordStrength(password);
        if (strength == AuthInputValidator.PasswordStrength.NONE) {
            binding.passwordStrengthContainer.setVisibility(View.GONE);
            return;
        }
        binding.passwordStrengthContainer.setVisibility(View.VISIBLE);
        binding.passwordStrengthProgress.setProgressCompat(strength.percent, true);
        int color;
        switch (strength) {
            case WEAK:
                color = 0xFFEF4444; // Red
                break;
            case MEDIUM:
                color = 0xFFF59E0B; // Amber
                break;
            case STRONG:
            default:
                color = 0xFF10B981; // Emerald green
                break;
        }
        binding.passwordStrengthProgress.setIndicatorColor(color);
        binding.passwordStrengthText.setText(getString(R.string.password_strength_label, getString(strength.labelRes)));
        binding.passwordStrengthText.setTextColor(color);
    }

    private void submit() {
        if (isLoading()) { return; }
        binding.formNotice.setVisibility(View.GONE);
        boolean nameValid = showError(binding.displayNameLayout, AuthInputValidator.displayNameError(binding.displayNameInput.getText().toString()));
        boolean usernameValid = showError(binding.usernameLayout, AuthInputValidator.usernameError(binding.usernameInput.getText().toString()));
        if (usernameValid && Boolean.FALSE.equals(lastCheckedAvailable)) {
            binding.usernameLayout.setError(getString(R.string.auth_username_conflict));
            usernameValid = false;
        }
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
        binding.submitButton.setAlpha(loading ? 0.55f : 1f);
        binding.displayNameLayout.setEnabled(!loading);
        binding.usernameLayout.setEnabled(!loading);
        binding.emailLayout.setEnabled(!loading);
        binding.passwordLayout.setEnabled(!loading);
        binding.confirmPasswordLayout.setEnabled(!loading);
        binding.loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.submitButton.setText(loading ? R.string.auth_processing : R.string.auth_register);
    }

    @Override
    protected void onDestroy() {
        usernameHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private static abstract class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int count, int after) { }
    }
}
