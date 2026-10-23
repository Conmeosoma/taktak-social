package com.taktak.app.ui.splash;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.taktak.app.MainActivity;
import com.taktak.app.R;
import com.taktak.app.databinding.DialogIntroSettingsBinding;

public final class IntroSettings {
    private IntroSettings() { }
    public static void show(MainActivity activity) {
        var preferences = new IntroPreferences(activity);
        var binding = DialogIntroSettingsBinding.inflate(activity.getLayoutInflater());
        binding.introEnabled.setChecked(preferences.enabled());
        binding.introSoundEnabled.setChecked(preferences.sound());
        binding.introEnabled.setOnCheckedChangeListener((v, checked) -> preferences.enabled(checked));
        binding.introSoundEnabled.setOnCheckedChangeListener((v, checked) -> preferences.sound(checked));
        var dialog = new MaterialAlertDialogBuilder(activity).setTitle(R.string.intro_settings)
                .setView(binding.getRoot()).setPositiveButton(android.R.string.ok, null).create();
        binding.introPreview.setOnClickListener(v -> { dialog.dismiss(); activity.showIntroPreview(); });
        dialog.show();
    }
}
