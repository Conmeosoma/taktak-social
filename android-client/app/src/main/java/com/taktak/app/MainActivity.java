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
    private boolean authenticated;
    private boolean imeVisible;
    private int selectedTab;
    private androidx.activity.OnBackPressedCallback tabBack;
    private static final int[] TAB_IDS = {R.id.nav_camera, R.id.nav_history, R.id.nav_friends, R.id.nav_profile};
    private final ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) { viewModel.check(true); }
                else { binding.sessionMessage.setText(R.string.auth_permission_denied); }
            });
    private final ActivityResultLauncher<String> profileAvatar = getActivityResultRegistry().register("profile.avatar",this,
            new ActivityResultContracts.GetContent(),uri -> new ViewModelProvider(this)
                    .get(com.taktak.app.viewmodel.ProfileViewModel.class).uploadSelection(uri));
    public void chooseProfileAvatar() {
        if(new ViewModelProvider(this).get(com.taktak.app.viewmodel.ProfileViewModel.class).beginAvatarSelection()) {
            profileAvatar.launch("image/*");
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_TakTak_Camera);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        selectedTab = savedInstanceState == null ? 0 : Math.max(0, Math.min(3, savedInstanceState.getInt("selectedTab")));
        // Fragment khôi phục không được mở camera trước khi phiên được xác minh lại.
        clearPages();
        tabBack = new androidx.activity.OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { binding.mainPager.setCurrentItem(0, false); }
        };
        getOnBackPressedDispatcher().addCallback(this, tabBack);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (!authenticated) { return false; }
            for (int i = 0; i < TAB_IDS.length; i++) {
                if (TAB_IDS[i] == item.getItemId()) { binding.mainPager.setCurrentItem(i, false); return true; }
            }
            return false;
        });
        binding.mainPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) {
                selectedTab = position;
                if (binding.bottomNavigation.getSelectedItemId() != TAB_IDS[position]) {
                    binding.bottomNavigation.setSelectedItemId(TAB_IDS[position]);
                }
                renderNavigation();
            }
        });
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            renderNavigation();
            return insets;
        });
        ViewCompat.setAccessibilityHeading(binding.sessionTitle, true);
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
            binding.sessionCard.setVisibility(camera ? View.GONE : View.VISIBLE);
            binding.sessionTitle.setText(checking || state.destination == SessionViewModel.Destination.LOGIN ? R.string.m3_session_waiting
                    : state.destination == SessionViewModel.Destination.PERMISSION
                    ? R.string.m3_session_permission : R.string.m3_session_error);
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
        binding.getRoot().setBackgroundColor(androidx.core.content.ContextCompat.getColor(this,
                R.color.m3_surface));
        binding.sessionScroll.setVisibility(authenticated ? View.GONE : View.VISIBLE);
        renderNavigation();
        if (!authenticated) { clearPages(); }
        else if (binding.mainPager.getAdapter() == null) {
            int restoreTab = selectedTab;
            binding.mainPager.setSaveEnabled(false);
            binding.mainPager.setAdapter(new com.taktak.app.ui.main.MainPagerAdapter(this));
            binding.mainPager.setCurrentItem(restoreTab, false);
        }
    }

    private void renderNavigation() {
        binding.bottomNavigation.setVisibility(authenticated && !imeVisible ? View.VISIBLE : View.GONE);
        binding.mainToolbar.setVisibility(authenticated && (selectedTab == 2 || selectedTab == 3) ? View.GONE : View.VISIBLE);
        if (tabBack != null) { tabBack.setEnabled(authenticated && selectedTab != 0); }
    }

    private void clearPages() {
        binding.mainPager.setAdapter(null);
        var manager = getSupportFragmentManager();
        if (manager.isStateSaved() || manager.getFragments().isEmpty()) { return; }
        var transaction = manager.beginTransaction();
        for (var fragment : manager.getFragments()) { transaction.remove(fragment); }
        transaction.commitNow();
    }

    public boolean isCameraPageActive() {
        return authenticated && selectedTab == 0 && binding.cameraPanel.isShown();
    }

    @Override protected void onResumeFragments() {
        super.onResumeFragments();
        if (binding != null) { renderContent(); }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("selectedTab", selectedTab);
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
