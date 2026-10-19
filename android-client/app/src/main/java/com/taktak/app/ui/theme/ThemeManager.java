package com.taktak.app.ui.theme;

import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.taktak.app.R;
import java.util.Objects;

/** Chỉ phân giải/apply lúc tạo màn. Không tự recreate hoặc giữ tham chiếu Activity. */
public final class ThemeManager {
    public enum ScreenRole { LOGIN, AUTH, MAIN, PREVIEW, VIEWER }

    public record Resolution(ThemeSelection requested, AppTheme effectiveStyle,
                             @StyleRes int themeResource, boolean requestedStyleAvailable) { }

    private ThemeManager() { }

    /** Các resource này chỉ dành cho chooser, không tuyên bố phủ các màn nghiệp vụ. */
    @StyleRes public static int chooserThemeResource(AppTheme style) {
        return switch (Objects.requireNonNull(style)) {
            case ORIGINAL -> R.style.Theme_TakTak_Chooser;
            case GRADIENT -> R.style.Theme_TakTak_Chooser_Gradient;
            case GLASS -> R.style.Theme_TakTak_Chooser_Glass;
            case SOFT -> R.style.Theme_TakTak_Chooser_Soft;
        };
    }

    public static Resolution resolve(ThemeSelection selection, ScreenRole role) {
        Objects.requireNonNull(role, "role");
        ThemeSelection requested = selection == null ? ThemeSelection.defaults() : selection;
        if (role == ScreenRole.AUTH) {
            int auth = switch (requested.style()) {
                case ORIGINAL -> R.style.Theme_TakTak_Auth;
                case GRADIENT -> R.style.Theme_TakTak_Auth_Gradient;
                case GLASS -> R.style.Theme_TakTak_Auth_Glass;
                case SOFT -> R.style.Theme_TakTak_Auth_Soft;
            };
            return new Resolution(requested, requested.style(), auth, true);
        }
        if (role == ScreenRole.LOGIN) {
            int login = switch (requested.style()) {
                case ORIGINAL -> R.style.Theme_TakTak_LoginReference;
                case GRADIENT -> R.style.Theme_TakTak_Login_Gradient;
                case GLASS -> R.style.Theme_TakTak_Login_Glass;
                case SOFT -> R.style.Theme_TakTak_Login_Soft;
            };
            return new Resolution(requested, requested.style(), login, true);
        }
        int resource = switch (role) {
            case LOGIN -> R.style.Theme_TakTak_LoginReference;
            case AUTH -> R.style.Theme_TakTak_Auth;
            case MAIN -> switch (requested.style()) {
                case ORIGINAL -> R.style.Theme_TakTak_Media_Main;
                case GRADIENT -> R.style.Theme_TakTak_Media_Main_Gradient;
                case GLASS -> R.style.Theme_TakTak_Media_Main_Glass;
                case SOFT -> R.style.Theme_TakTak_Media_Main_Soft;
            };
            case PREVIEW -> switch (requested.style()) {
                case ORIGINAL -> R.style.Theme_TakTak_Media_Preview;
                case GRADIENT -> R.style.Theme_TakTak_Media_Preview_Gradient;
                case GLASS -> R.style.Theme_TakTak_Media_Preview_Glass;
                case SOFT -> R.style.Theme_TakTak_Media_Preview_Soft;
            };
            case VIEWER -> switch (requested.style()) {
                case ORIGINAL -> R.style.Theme_TakTak_Media_Viewer;
                case GRADIENT -> R.style.Theme_TakTak_Media_Viewer_Gradient;
                case GLASS -> R.style.Theme_TakTak_Media_Viewer_Glass;
                case SOFT -> R.style.Theme_TakTak_Media_Viewer_Soft;
            };
        };
        // MAIN chỉ là host/navigation/Camera; nội dung History/Friends/Profile giữ context Original.
        return new Resolution(requested, requested.style(), resource, true);
    }

    public static int nightMode(ColorMode mode) {
        return switch (Objects.requireNonNull(mode)) {
            case SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            case LIGHT -> AppCompatDelegate.MODE_NIGHT_NO;
            case DARK -> AppCompatDelegate.MODE_NIGHT_YES;
        };
    }

    /** Gọi trước super.onCreate/inflate, không tự recreate Activity hoặc đổi mode toàn app. */
    public static Resolution applyBeforeCreate(AppCompatActivity activity, ThemeSelection selection,
                                               ScreenRole role) {
        Resolution resolution = resolve(selection, role);
        activity.getDelegate().setLocalNightMode(nightMode(resolution.requested().colorMode()));
        activity.setTheme(resolution.themeResource());
        return resolution;
    }

    public static void configureBars(AppCompatActivity activity, android.view.View root, int surface) {
        var bars = androidx.core.view.WindowCompat.getInsetsController(activity.getWindow(), root);
        boolean light = androidx.core.graphics.ColorUtils.calculateLuminance(surface) > .5;
        bars.setAppearanceLightStatusBars(light);
        bars.setAppearanceLightNavigationBars(light);
    }
}
