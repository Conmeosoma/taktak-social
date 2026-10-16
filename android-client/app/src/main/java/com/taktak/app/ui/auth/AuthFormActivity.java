package com.taktak.app.ui.auth;

import android.view.View;
import android.graphics.Rect;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.textfield.TextInputLayout;
import com.taktak.app.R;
import com.taktak.app.BuildConfig;
import com.taktak.app.viewmodel.AuthViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.content.pm.PackageManager;
import android.os.Build;

public abstract class AuthFormActivity extends AppCompatActivity {
    private boolean loading;
    protected AuthViewModel viewModel;
    private TextView notice;
    private final ActivityResultLauncher<String> localNetworkPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (notice != null) {
                    notice.setText(granted ? R.string.auth_permission_granted : R.string.auth_permission_denied);
                    notice.setVisibility(View.VISIBLE);
                }
            });

    protected void observeAuth(TextView notice, Runnable clearPasswords) {
        this.notice = notice;
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        viewModel.state().observe(this, state -> {
            setLoading(state.loading);
            notice.setVisibility(state.message == 0 ? View.GONE : View.VISIBLE);
            if (state.message != 0) {
                notice.setText(state.retryAfterSeconds > 0 ? getString(state.message, state.retryAfterSeconds) : getString(state.message));
            }
            if (state.success) { clearPasswords.run(); }
        });
    }

    protected boolean ensureNetworkPermission() {
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= 37
                && checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK") != PackageManager.PERMISSION_GRANTED) {
            localNetworkPermission.launch("android.permission.ACCESS_LOCAL_NETWORK");
            return false;
        }
        return true;
    }

    protected void configureInsets(View root) {
        EdgeToEdge.enable(this);
        ViewCompat.setAccessibilityHeading(root.findViewById(R.id.authBrand), true);
        ViewCompat.setAccessibilityHeading(root.findViewById(R.id.authTitle), true);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                root.post(() -> {
                    View focused = getCurrentFocus();
                    if (focused != null) {
                        focused.requestRectangleOnScreen(new Rect(0, 0, focused.getWidth(), focused.getHeight()), false);
                    }
                });
            }
            return insets;
        });
    }

    protected boolean showError(TextInputLayout field, int error) {
        field.setError(error == 0 ? null : getString(error));
        return error == 0;
    }

    public final void setLoading(boolean loading) {
        this.loading = loading;
        renderLoading(loading);
    }

    protected final boolean isLoading() { return loading; }
    protected abstract void renderLoading(boolean loading);

}
