package com.taktak.app.ui.theme;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.FrameLayout;
import com.taktak.app.R;
import com.taktak.app.ui.auth.LoginBackdropView;
import com.taktak.app.ui.main.PremiumBackdropView;

/** Đồ họa tĩnh cho host Main; giảm sắc độ khi Camera đang mở, không xử lý ảnh. */
public final class MediaBackdropView extends FrameLayout {
    private final android.view.View backdrop;
    public MediaBackdropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        var value = new TypedValue();
        boolean alternate = context.getTheme().resolveAttribute(R.attr.ttAuthAlternate, value, true) && value.data != 0;
        backdrop = alternate ? new LoginBackdropView(context, null) : new PremiumBackdropView(context, null);
        addView(backdrop,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }
    public void setCameraMode(boolean camera) {
        if (backdrop instanceof PremiumBackdropView original) original.setCameraMode(camera);
        else backdrop.setAlpha(camera ? .28f : 1f);
    }
}
