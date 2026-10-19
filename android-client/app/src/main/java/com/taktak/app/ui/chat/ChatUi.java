package com.taktak.app.ui.chat;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.taktak.app.R;
import com.taktak.app.ui.theme.ThemeManager;
import com.taktak.app.ui.theme.ThemePreferences;
import java.util.Locale;

final class ChatUi {
    private ChatUi() { }
    static void theme(AppCompatActivity activity) {
        com.taktak.app.data.push.ChatNotifications.initialize(activity.getApplication());
        ThemeManager.applyBeforeCreate(activity,new ThemePreferences(activity).load(),ThemeManager.ScreenRole.MAIN);
    }
    static void insets(AppCompatActivity activity, View root, View content) {
        EdgeToEdge.enable(activity);
        int surface=com.google.android.material.color.MaterialColors.getColor(root,
                com.google.android.material.R.attr.colorSurface);
        ThemeManager.configureBars(activity,root,surface);
        int initialLeft=content.getPaddingLeft(),initialRight=content.getPaddingRight();
        ViewCompat.setOnApplyWindowInsetsListener(root,(view,insets)->{
            var safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());
            var ime=insets.getInsets(WindowInsetsCompat.Type.ime());
            content.setPadding(initialLeft+safe.left,safe.top,initialRight+safe.right,Math.max(safe.bottom,ime.bottom));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }
    static TextWatcher watch(Runnable callback) {
        return new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count) { callback.run(); }
            public void afterTextChanged(Editable value) { }
        };
    }
    static String query(CharSequence value) { return value==null?"":value.toString().strip().toLowerCase(Locale.ROOT); }
    static boolean matches(String query,String name,String username) {
        return query.isEmpty() || (name!=null && name.toLowerCase(Locale.ROOT).contains(query))
                || (username!=null && username.toLowerCase(Locale.ROOT).contains(query));
    }
}
