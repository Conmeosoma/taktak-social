package com.taktak.app.ui.theme;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.taktak.app.R;
import com.taktak.app.databinding.ActivityThemeSettingsBinding;
import com.taktak.app.databinding.ItemThemeOptionBinding;
import java.util.EnumMap;
import java.util.concurrent.Executors;

public final class ThemeSettingsActivity extends AppCompatActivity {
    private ActivityThemeSettingsBinding binding;
    private ThemeViewModel model;
    private final EnumMap<AppTheme, ItemThemeOptionBinding> options = new EnumMap<>(AppTheme.class);
    private boolean rendering;
    private ThemeSelection createdSelection;

    @Override protected void onCreate(Bundle saved) {
        var repository = new ThemeRepository(new ThemePreferences(this));
        createdSelection = repository.load();
        getDelegate().setLocalNightMode(ThemeManager.nightMode(createdSelection.colorMode()));
        setTheme(ThemeManager.chooserThemeResource(createdSelection.style()));
        super.onCreate(saved);
        EdgeToEdge.enable(this);
        binding = ActivityThemeSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        int surface = com.google.android.material.color.MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorSurface, "Theme chooser");
        // Phủ đúng palette cả vùng dưới system bars sau đổi local night mode.
        getWindow().getDecorView().setBackgroundColor(surface);
        var bars = androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        boolean lightBars = androidx.core.graphics.ColorUtils.calculateLuminance(surface) > .5;
        bars.setAppearanceLightStatusBars(lightBars);
        bars.setAppearanceLightNavigationBars(lightBars);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            var safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });
        ViewCompat.setAccessibilityHeading(binding.themeTitle, true);
        ViewCompat.setAccessibilityHeading(binding.themeModeHeading, true);
        model = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> type) {
                if (!type.equals(ThemeViewModel.class)) { throw new IllegalArgumentException("Unknown model"); }
                Handler main = new Handler(Looper.getMainLooper());
                return type.cast(new ThemeViewModel(repository, createdSelection, Executors.newSingleThreadExecutor(), main::post));
            }
        }).get(ThemeViewModel.class);
        if (saved != null && !model.state().getValue().busy() && !model.state().getValue().dirty()
                && !model.state().getValue().applied()) {
            model.select(AppTheme.fromId(saved.getString("appearance_draft_style")),
                    ColorMode.fromId(saved.getString("appearance_draft_mode")));
        }
        for (AppTheme theme : AppTheme.values()) {
            var option = ItemThemeOptionBinding.inflate(getLayoutInflater(), binding.themeOptions, false);
            option.themeOptionCard.setTag(theme.id());
            option.themeOptionTitle.setText(title(theme));
            option.themeOptionDetail.setText(detail(theme));
            option.themeOptionCard.setOnClickListener(view -> model.select(theme, model.state().getValue().mode()));
            options.put(theme, option);
            binding.themeOptions.addView(option.getRoot());
        }
        binding.themeModes.setOnCheckedChangeListener((group, checked) -> {
            if (!rendering) {
                ColorMode mode = checked == R.id.themeLight ? ColorMode.LIGHT : checked == R.id.themeDark ? ColorMode.DARK : ColorMode.SYSTEM;
                model.select(model.state().getValue().style(), mode);
            }
        });
        binding.themeApply.setOnClickListener(view -> model.apply());
        binding.themeCancel.setOnClickListener(view -> cancel());
        binding.themeBack.setOnClickListener(view -> cancel());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { cancel(); }
        });
        model.state().observe(this, this::render);
    }

    private void cancel() {
        if (!model.state().getValue().busy()) { finish(); }
    }
    private void render(ThemeViewModel.State state) {
        if (state.applied() && !state.saved().equals(createdSelection)) {
            setResult(RESULT_OK);
            recreate(); // Chỉ chooser; không recreate màn cha/draft nghiệp vụ.
            return;
        }
        rendering = true;
        for (var entry : options.entrySet()) {
            var option = entry.getValue();
            boolean selected = state.style() == entry.getKey();
            option.themeOptionCard.setChecked(selected);
            option.themeOptionCard.setEnabled(!state.busy());
            option.themeOptionCard.setContentDescription(getString(title(entry.getKey())) + ". " + getString(detail(entry.getKey())));
            ViewCompat.setStateDescription(option.themeOptionCard, selected ? getString(R.string.theme_selected) : null);
            option.themeThumbnail.setSelection(entry.getKey(), state.mode());
        }
        binding.themePreview.setSelection(state.style(), state.mode());
        binding.themeModes.check(state.mode() == ColorMode.LIGHT ? R.id.themeLight : state.mode() == ColorMode.DARK ? R.id.themeDark : R.id.themeSystem);
        binding.themeSystem.setEnabled(!state.busy()); binding.themeLight.setEnabled(!state.busy()); binding.themeDark.setEnabled(!state.busy());
        binding.themeApply.setEnabled(!state.busy() && state.dirty());
        binding.themeCancel.setEnabled(!state.busy()); binding.themeBack.setEnabled(!state.busy());
        binding.themeCancel.setText(state.dirty() ? R.string.theme_cancel : R.string.theme_done);
        binding.themeProgress.setVisibility(state.busy() ? View.VISIBLE : View.INVISIBLE);
        int notice = state.busy() ? R.string.theme_saving : state.error() ? R.string.theme_save_error : state.applied() ? R.string.theme_saved : 0;
        binding.themeNotice.setVisibility(notice == 0 ? View.GONE : View.VISIBLE);
        if (notice != 0) { binding.themeNotice.setText(notice); }
        rendering = false;
    }
    @Override protected void onSaveInstanceState(@NonNull Bundle out) {
        var state = model.state().getValue();
        out.putString("appearance_draft_style", state.style().id());
        out.putString("appearance_draft_mode", state.mode().id());
        super.onSaveInstanceState(out);
    }
    private int title(AppTheme theme) {
        return switch (theme) {
            case ORIGINAL -> R.string.theme_original; case GRADIENT -> R.string.theme_gradient;
            case GLASS -> R.string.theme_glass; case SOFT -> R.string.theme_soft;
        };
    }
    private int detail(AppTheme theme) {
        return switch (theme) {
            case ORIGINAL -> R.string.theme_original_detail; case GRADIENT -> R.string.theme_gradient_detail;
            case GLASS -> R.string.theme_glass_detail; case SOFT -> R.string.theme_soft_detail;
        };
    }
}
