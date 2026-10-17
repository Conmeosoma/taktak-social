package com.taktak.app;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.taktak.app.databinding.ActivityMainBinding;
import com.taktak.app.ui.auth.LoginActivity;
import com.taktak.app.viewmodel.SessionViewModel;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private SessionViewModel viewModel;
    private boolean navigating;
    private boolean showingFriends;
    private boolean authenticated;
    private androidx.activity.OnBackPressedCallback friendsBack;
    private final ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) { viewModel.check(true); }
                else { binding.sessionMessage.setText(R.string.auth_permission_denied); }
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_TakTak_Camera);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        showingFriends = savedInstanceState != null && savedInstanceState.getBoolean("showingFriends");
        friendsBack = new androidx.activity.OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { showingFriends = false; renderContent(); }
        };
        getOnBackPressedDispatcher().addCallback(this, friendsBack);
        binding.friendsButton.setOnClickListener(view -> { showingFriends = true; renderContent(); });
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
        ViewCompat.setAccessibilityHeading(binding.cameraTitle, true);
        viewModel = new ViewModelProvider(this).get(SessionViewModel.class);
        binding.retryButton.setOnClickListener(view -> {
            if (!networkAllowed()) { permission.launch("android.permission.ACCESS_LOCAL_NETWORK"); }
            else { viewModel.check(true); }
        });
        binding.logoutButton.setOnClickListener(view -> viewModel.logout());
        viewModel.state().observe(this, state -> {
            boolean checking = state.destination == SessionViewModel.Destination.CHECKING;
            boolean camera = state.destination == SessionViewModel.Destination.CAMERA;
            binding.sessionProgress.setVisibility(checking ? View.VISIBLE : View.GONE);
            binding.cameraPanel.setVisibility(camera ? View.VISIBLE : View.GONE);
            authenticated = camera;
            renderContent();
            binding.sessionMessage.setVisibility(state.message == 0 ? View.GONE : View.VISIBLE);
            if (state.message != 0) { binding.sessionMessage.setText(state.message); }
            binding.retryButton.setVisibility(state.destination == SessionViewModel.Destination.ERROR
                    || state.destination == SessionViewModel.Destination.PERMISSION ? View.VISIBLE : View.GONE);
            binding.logoutButton.setVisibility(checking || state.destination == SessionViewModel.Destination.LOGIN ? View.GONE : View.VISIBLE);
            if (state.destination == SessionViewModel.Destination.LOGIN && !navigating) {
                navigating = true;
                startActivity(new Intent(this, LoginActivity.class).putExtra(LoginActivity.SESSION_NOTICE, state.message)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            }
        });
    }

    private void renderContent() {
        boolean friends = authenticated && showingFriends;
        binding.cameraScroll.setVisibility(friends ? View.GONE : View.VISIBLE);
        binding.friendsContent.setVisibility(friends ? View.VISIBLE : View.GONE);
        friendsBack.setEnabled(friends);
        if (getSupportFragmentManager().isStateSaved()) { return; }
        var manager = getSupportFragmentManager();
        var camera = manager.findFragmentByTag("camera");
        var friend = manager.findFragmentByTag("friends");
        var transaction = manager.beginTransaction();
        if ((!authenticated || friends) && camera != null) { transaction.remove(camera); }
        if (!friends && friend != null) { transaction.remove(friend); }
        if (friends && friend == null) {
            transaction.replace(R.id.friendsContent, new com.taktak.app.ui.friends.FriendsFragment(), "friends");
        } else if (authenticated && !friends && camera == null) {
            transaction.replace(R.id.cameraContent, new com.taktak.app.ui.camera.CameraFragment(), "camera");
        }
        transaction.commitNow();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("showingFriends", showingFriends);
        super.onSaveInstanceState(outState);
    }

    @Override protected void onStart() {
        super.onStart();
        // Kiểm tra lại khi quay từ nền, không tin trạng thái UI đã lưu.
        if (!navigating) { viewModel.check(networkAllowed()); }
    }

    private boolean networkAllowed() {
        return !BuildConfig.DEBUG || Build.VERSION.SDK_INT < 37
                || checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK") == PackageManager.PERMISSION_GRANTED;
    }
}
