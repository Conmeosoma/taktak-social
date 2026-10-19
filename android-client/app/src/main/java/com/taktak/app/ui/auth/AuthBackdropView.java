package com.taktak.app.ui.auth;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.FrameLayout;
import com.taktak.app.R;
import com.taktak.app.ui.main.PremiumBackdropView;

/** Giữ renderer Original; ba kiểu mới dùng cùng token với Login, không nhân bản shader. */
public final class AuthBackdropView extends FrameLayout {
    public AuthBackdropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        var value = new TypedValue();
        boolean alternate = context.getTheme().resolveAttribute(R.attr.ttAuthAlternate, value, true) && value.data != 0;
        addView(alternate ? new LoginBackdropView(context, null) : new PremiumBackdropView(context, null),
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }
}
