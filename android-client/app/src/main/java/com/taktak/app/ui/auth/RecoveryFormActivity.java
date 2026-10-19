package com.taktak.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.textfield.TextInputLayout;
import com.taktak.app.R;
import com.taktak.app.data.session.ResetSessionVault;
import com.taktak.app.databinding.ActivityRecoveryFormBinding;
import com.taktak.app.viewmodel.RecoveryViewModel;

public abstract class RecoveryFormActivity extends AuthFormActivity {
    protected static final int FORGOT=0,VERIFY=1,CHANGE=2;
    protected abstract int mode();
    private ActivityRecoveryFormBinding binding;
    private RecoveryViewModel recovery;
    private boolean lastConfirm;
    @Override protected void onCreate(Bundle saved) {
        com.taktak.app.ui.theme.ThemeManager.applyBeforeCreate(this,
                new com.taktak.app.ui.theme.ThemePreferences(this).load(),
                com.taktak.app.ui.theme.ThemeManager.ScreenRole.AUTH);
        super.onCreate(saved);
        binding=ActivityRecoveryFormBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        configureInsets(binding.getRoot());
        configureAuthBars(binding.getRoot());
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(),(view,insets) -> {
            var safe=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()
                    | androidx.core.view.WindowInsetsCompat.Type.ime() | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            binding.safeContent.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            if (insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())) view.post(() -> {
                View focused=getCurrentFocus();
                if (focused!=null) focused.requestRectangleOnScreen(new android.graphics.Rect(0,0,focused.getWidth(),focused.getHeight()),false);
            });
            return insets;
        });
        binding.authTitle.setText(mode()==FORGOT ? R.string.recovery_forgot_title : mode()==VERIFY ? R.string.recovery_verify_title : R.string.recovery_change_title);
        binding.detail.setText(mode()==FORGOT ? R.string.recovery_forgot_detail : mode()==VERIFY ? R.string.recovery_verify_detail : R.string.recovery_change_detail);
        binding.emailLayout.setVisibility(mode()==CHANGE ? View.GONE : View.VISIBLE);
        binding.currentLayout.setVisibility(mode()==FORGOT ? View.GONE : View.VISIBLE);
        binding.currentLayout.setHint(mode()==CHANGE ? R.string.recovery_temporary : R.string.recovery_current);
        binding.codeLayout.setVisibility(mode()==VERIFY ? View.VISIBLE : View.GONE);
        binding.newLayout.setVisibility(mode()==CHANGE ? View.VISIBLE : View.GONE);
        binding.confirmLayout.setVisibility(mode()==CHANGE ? View.VISIBLE : View.GONE);
        binding.confirmCode.setVisibility(mode()==VERIFY ? View.VISIBLE : View.GONE);
        binding.submit.setText(mode()==FORGOT ? R.string.recovery_send : mode()==VERIFY ? R.string.recovery_request_code : R.string.recovery_save);
        recovery=new ViewModelProvider(this).get(RecoveryViewModel.class);
        recovery.state().observe(this,state -> {
            setLoading(state.loading);
            int message=state.message;
            if (message==0 && !state.loading && mode()==CHANGE && ResetSessionVault.peek()==null) {
                message=R.string.recovery_expired;
            }
            binding.notice.setVisibility(message==0 ? View.GONE : View.VISIBLE);
            if (message!=0) binding.notice.setText(message);
            binding.retry.setVisibility(state.retry ? View.VISIBLE : View.GONE);
            if (state.fieldError!=0 && mode()==CHANGE) showError(binding.newLayout,state.fieldError);
            if (state.message==R.string.recovery_invalid && mode()!=FORGOT) {
                showError(binding.currentLayout,R.string.recovery_invalid);
                if (mode()==VERIFY) showError(binding.codeLayout,R.string.recovery_invalid);
            }
            if (state.changed) { clearSensitive(); goLogin(true); }
        });
        binding.submit.setOnClickListener(v -> submit(false));
        binding.confirmCode.setOnClickListener(v -> submit(true));
        binding.retry.setOnClickListener(v -> submit(lastConfirm));
        binding.back.setOnClickListener(v -> { if (!isLoading()) { if (mode()==CHANGE) ResetSessionVault.clear(); goLogin(false); } });
        if (mode()==CHANGE && ResetSessionVault.peek()==null) {
            binding.submit.setEnabled(false); binding.notice.setText(R.string.recovery_expired); binding.notice.setVisibility(View.VISIBLE);
        }
    }
    private void goLogin(boolean changed) {
        Intent intent=new Intent(this,LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (changed) intent.putExtra(LoginActivity.SESSION_NOTICE,R.string.recovery_changed);
        startActivity(intent); finish();
    }
    private void clearSensitive() {
        binding.currentInput.setText(""); binding.codeInput.setText(""); binding.newInput.setText(""); binding.confirmInput.setText("");
    }
    private void submit(boolean confirm) {
        if (isLoading()) return; lastConfirm=confirm;
        String email=AuthInputValidator.strip(binding.emailInput.getText().toString()).toLowerCase(java.util.Locale.ROOT);
        String current=binding.currentInput.getText().toString(), password=binding.newInput.getText().toString();
        String confirmation=binding.confirmInput.getText().toString(), code=AuthInputValidator.strip(binding.codeInput.getText().toString());
        boolean valid=true;
        if (mode()!=CHANGE) valid &= showError(binding.emailLayout,AuthInputValidator.emailError(email));
        if (mode()!=FORGOT) valid &= showError(binding.currentLayout,AuthInputValidator.passwordError(current));
        if (mode()==VERIFY && confirm) valid &= showError(binding.codeLayout,code.matches("[A-Za-z0-9_-]{43}") ? 0 : R.string.recovery_invalid_code);
        if (mode()==CHANGE) {
            valid &= showError(binding.newLayout,password.equals(current) ? R.string.recovery_different : AuthInputValidator.passwordError(password));
            valid &= showError(binding.confirmLayout,AuthInputValidator.confirmationError(password,confirmation));
            if (ResetSessionVault.peek()==null) { binding.notice.setText(R.string.recovery_expired); binding.notice.setVisibility(View.VISIBLE); return; }
        }
        if (!valid || !ensureNetworkPermission()) return;
        if (mode()==FORGOT) recovery.forgot(email);
        else if (mode()==VERIFY && !confirm) recovery.requestVerification(email,current);
        else if (mode()==VERIFY) recovery.verify(email,current,code);
        else recovery.reset(current,password,confirmation);
    }
    @Override protected void renderLoading(boolean loading) {
        binding.submit.setEnabled(!loading && (mode()!=CHANGE || ResetSessionVault.peek()!=null));
        binding.submit.setAlpha(loading ? .55f : 1f); binding.confirmCode.setEnabled(!loading);
        binding.retry.setEnabled(!loading); binding.back.setEnabled(!loading);
        for (TextInputLayout field : new TextInputLayout[]{binding.emailLayout,binding.currentLayout,binding.codeLayout,binding.newLayout,binding.confirmLayout}) field.setEnabled(!loading);
        binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}
