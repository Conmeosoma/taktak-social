package com.taktak.app.ui.language;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.taktak.app.R;
import java.util.List;
import java.util.Locale;

public final class LanguageManager {
    public record SupportedLanguage(String code, String displayName, String nativeName) {}

    public static final List<SupportedLanguage> SUPPORTED_LANGUAGES = List.of(
            new SupportedLanguage("vi", "Tiếng Việt", "Tiếng Việt"),
            new SupportedLanguage("en", "English", "English"),
            new SupportedLanguage("ko", "Korean", "한국어"),
            new SupportedLanguage("zh", "Chinese", "中文"),
            new SupportedLanguage("es", "Spanish", "Español"),
            new SupportedLanguage("hi", "Hindi", "हिन्दी"),
            new SupportedLanguage("ja", "Japanese", "日本語")
    );

    private LanguageManager() {}

    @NonNull
    public static String getCurrentLanguageCode() {
        LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        if (!current.isEmpty()) {
            Locale locale = current.get(0);
            if (locale != null && locale.getLanguage() != null && !locale.getLanguage().isEmpty()) {
                return locale.getLanguage().toLowerCase(Locale.ROOT);
            }
        }
        return "vi";
    }

    @NonNull
    public static String getCurrentLanguageName() {
        String currentCode = getCurrentLanguageCode();
        for (SupportedLanguage lang : SUPPORTED_LANGUAGES) {
            if (lang.code().equalsIgnoreCase(currentCode)) {
                return lang.nativeName();
            }
        }
        return "Tiếng Việt";
    }

    public static void setLanguage(@NonNull String languageCode) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode));
    }

    public static void showLanguageDialog(@NonNull Context context, Runnable onLanguageSelected) {
        String currentCode = getCurrentLanguageCode();
        int selectedIndex = 0;
        CharSequence[] items = new CharSequence[SUPPORTED_LANGUAGES.size()];
        for (int i = 0; i < SUPPORTED_LANGUAGES.size(); i++) {
            SupportedLanguage lang = SUPPORTED_LANGUAGES.get(i);
            items[i] = lang.nativeName() + " (" + lang.displayName() + ")";
            if (lang.code().equalsIgnoreCase(currentCode)) {
                selectedIndex = i;
            }
        }

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.language_dialog_title)
                .setSingleChoiceItems(items, selectedIndex, (dialog, which) -> {
                    dialog.dismiss();
                    SupportedLanguage selected = SUPPORTED_LANGUAGES.get(which);
                    if (!selected.code().equalsIgnoreCase(currentCode)) {
                        setLanguage(selected.code());
                        if (onLanguageSelected != null) {
                            onLanguageSelected.run();
                        }
                    }
                })
                .setNegativeButton(R.string.theme_cancel, null)
                .show();
    }
}
