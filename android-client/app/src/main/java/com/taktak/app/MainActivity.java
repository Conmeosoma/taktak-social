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
    private static final com.taktak.app.ui.splash.IntroPolicy INTRO_POLICY = new com.taktak.app.ui.splash.IntroPolicy();
    private com.taktak.app.ui.splash.BrandedIntro intro;
    private boolean introActive, foreground;
    private androidx.activity.OnBackPressedCallback introBack;
    private SessionViewModel viewModel;
    private boolean navigating;
    private boolean authenticated;
    private boolean imeVisible;
    private int selectedTab;
    private com.taktak.app.ui.theme.ThemeSelection createdTheme;
    private androidx.activity.OnBackPressedCallback tabBack;
    private static final int[] TAB_IDS = {R.id.nav_camera, R.id.nav_history, R.id.nav_messages, R.id.nav_friends, R.id.nav_profile};
    private com.taktak.app.viewmodel.ChatViewModel chatModel;
    private String notificationConversation;
    private final ActivityResultLauncher<String> notifications = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> { });
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
        var splash = androidx.core.splashscreen.SplashScreen.installSplashScreen(this);
        createdTheme = new com.taktak.app.ui.theme.ThemePreferences(this).load();
        com.taktak.app.ui.theme.ThemeManager.applyBeforeCreate(this,createdTheme,
                com.taktak.app.ui.theme.ThemeManager.ScreenRole.MAIN);
        super.onCreate(savedInstanceState);
        splash.setOnExitAnimationListener(androidx.core.splashscreen.SplashScreenViewProvider::remove);
        com.taktak.app.data.push.ChatNotifications.initialize(getApplication());
        notificationConversation = getIntent().getStringExtra("chat.conversation");
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        boolean launcher = Intent.ACTION_MAIN.equals(getIntent().getAction())
                && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER);
        boolean special = getIntent().getExtras() != null && !getIntent().getExtras().isEmpty();
        if (INTRO_POLICY.consume(savedInstanceState != null, launcher, special,
                new com.taktak.app.ui.splash.IntroPreferences(this).enabled(),
                android.animation.ValueAnimator.areAnimatorsEnabled())) showIntroPreview();
        com.taktak.app.ui.theme.ThemeManager.configureBars(this,binding.getRoot(),
                com.google.android.material.color.MaterialColors.getColor(binding.getRoot(),
                        com.google.android.material.R.attr.colorSurface));
        selectedTab = savedInstanceState == null ? 0 : Math.max(0, Math.min(4, savedInstanceState.getInt("selectedTab")));
        chatModel = new ViewModelProvider(this).get(com.taktak.app.viewmodel.ChatViewModel.class);
        chatModel.state().observe(this, chat -> {
            long unread = chat.totalUnreadCount;
            var badge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_messages);
            badge.setNumber((int) Math.min(99, unread)); badge.setVisible(authenticated && unread > 0);
        });
        // Fragment khôi phục không được mở camera trước khi phiên được xác minh lại.
        clearPages();
        tabBack = new androidx.activity.OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() { binding.mainPager.setCurrentItem(0, false); }
        };
        getOnBackPressedDispatcher().addCallback(this, tabBack);
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (!authenticated) { return false; }
            for (int i = 0; i < TAB_IDS.length; i++) {
                if (TAB_IDS[i] == item.getItemId()) {
                    binding.mainPager.setCurrentItem(i, false);
                    return true;
                }
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
            binding.mainSafeContent.setPadding(safe.left, safe.top, safe.right, safe.bottom);
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
        binding.biometricUnlockButton.setOnClickListener(v -> showBiometricPrompt());
        binding.biometricLockOverlay.setOnClickListener(v -> showBiometricPrompt());
        viewModel.state().observe(this, this::renderSession);
    }

    private void renderSession(SessionViewModel.State state) {
            if (state == null || introActive || navigating || !foreground) return;
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
            if (camera) {
                chatModel.startSync();
                if (com.taktak.app.data.model.FriendModels.uuid(notificationConversation)) {
                    String id = notificationConversation; notificationConversation = null;
                    getIntent().removeExtra("chat.conversation");
                    startActivity(new Intent(this, com.taktak.app.ui.chat.ChatActivity.class).putExtra("conversationId", id));
                }
            } else { chatModel.stopSync(); }
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
    }

    public void showIntroPreview() {
        if (introActive || isFinishing() || isDestroyed()) return;
        introActive = true;
        binding.mainSafeContent.setVisibility(View.INVISIBLE);
        binding.premiumBackdrop.setVisibility(View.GONE);
        introBack = new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { if (intro != null) intro.finish(); }
        };
        getOnBackPressedDispatcher().addCallback(this, introBack);
        intro = new com.taktak.app.ui.splash.BrandedIntro(this, binding.getRoot(), () -> {
            introActive = false; intro = null;
            if (introBack != null) { introBack.remove(); introBack = null; }
            binding.mainSafeContent.setVisibility(View.VISIBLE);
            binding.premiumBackdrop.setVisibility(View.VISIBLE);
            com.taktak.app.ui.theme.ThemeManager.configureBars(this, binding.getRoot(),
                    com.google.android.material.color.MaterialColors.getColor(binding.getRoot(),
                            com.google.android.material.R.attr.colorSurface));
            if (foreground && viewModel != null) renderSession(viewModel.state().getValue());
        });
        var bars = androidx.core.view.WindowCompat.getInsetsController(getWindow(), binding.getRoot());
        bars.setAppearanceLightStatusBars(false); bars.setAppearanceLightNavigationBars(false);
    }

    private void renderContent() {
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
        binding.premiumBackdrop.setCameraMode(selectedTab == 0);
        binding.bottomNavigation.setVisibility(authenticated && !imeVisible ? View.VISIBLE : View.GONE);
        binding.mainToolbar.setVisibility(authenticated && selectedTab >= 2 ? View.GONE : View.VISIBLE);
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
    public void openFriendsTab() { if (authenticated) binding.mainPager.setCurrentItem(3, false); }
    public void requestChatNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != PackageManager.PERMISSION_GRANTED) notifications.launch("android.permission.POST_NOTIFICATIONS");
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        if (intro != null && (!Intent.ACTION_MAIN.equals(intent.getAction())
                || (intent.getExtras() != null && !intent.getExtras().isEmpty()))) intro.finish();
        notificationConversation = intent.getStringExtra("chat.conversation");
        if (authenticated && com.taktak.app.data.model.FriendModels.uuid(notificationConversation)) {
            String id = notificationConversation; notificationConversation = null; intent.removeExtra("chat.conversation");
            startActivity(new Intent(this, com.taktak.app.ui.chat.ChatActivity.class).putExtra("conversationId", id));
        }
    }

    @Override protected void onPause() {
        foreground = false;
        if (intro != null) intro.finish();
        super.onPause();
    }

    @Override protected void onStop() { chatModel.stopSync(); super.onStop(); }

    @Override protected void onDestroy() { if (intro != null) intro.finish(); super.onDestroy(); }

    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW && intro != null) intro.finish();
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (intro != null) intro.windowFocusChanged(focused);
    }

    @Override protected void onResumeFragments() {
        super.onResumeFragments();
        if (binding != null) {
            var saved = new com.taktak.app.ui.theme.ThemePreferences(this).load();
            boolean pending = saved.style() != createdTheme.style() || saved.colorMode() != createdTheme.colorMode();
            binding.themeMediaNotice.setVisibility(pending ? View.VISIBLE : View.GONE);
            renderContent();
        }
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

    @Override protected void onResume() {
        super.onResume();
        foreground = true;
        if (!introActive && viewModel != null) renderSession(viewModel.state().getValue());
        if (introActive) {
            var bars = androidx.core.view.WindowCompat.getInsetsController(getWindow(), binding.getRoot());
            bars.setAppearanceLightStatusBars(false); bars.setAppearanceLightNavigationBars(false);
        }
        checkBiometricLock();
    }

    private boolean isPromptingBiometric = false;

    private void checkBiometricLock() {
        var bioPrefs = com.taktak.app.data.security.BiometricPreferences.get(this);
        if (bioPrefs.isAppLocked()) {
            binding.biometricLockOverlay.setVisibility(View.VISIBLE);
            if (!isPromptingBiometric) {
                showBiometricPrompt();
            }
        } else {
            binding.biometricLockOverlay.setVisibility(View.GONE);
        }
    }

    private void showBiometricPrompt() {
        var bioPrefs = com.taktak.app.data.security.BiometricPreferences.get(this);
        if (!bioPrefs.isBiometricEnabled()) {
            binding.biometricLockOverlay.setVisibility(View.GONE);
            return;
        }
        isPromptingBiometric = true;
        com.taktak.app.data.security.BiometricHelper.authenticate(
                this,
                getString(R.string.biometric_prompt_title),
                getString(R.string.biometric_prompt_subtitle),
                new com.taktak.app.data.security.BiometricHelper.AuthCallback() {
                    @Override public void onSuccess() {
                        isPromptingBiometric = false;
                        binding.biometricLockOverlay.setVisibility(View.GONE);
                    }
                    @Override public void onError(int errorCode, @androidx.annotation.NonNull CharSequence errString) {
                        isPromptingBiometric = false;
                    }
                    @Override public void onFailed() {
                        isPromptingBiometric = false;
                    }
                }
        );
    }

    private boolean networkAllowed() {
        return !BuildConfig.DEBUG || Build.VERSION.SDK_INT < 37
                || checkSelfPermission("android.permission.ACCESS_LOCAL_NETWORK") == PackageManager.PERMISSION_GRANTED;
    }
}
