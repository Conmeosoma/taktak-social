package com.taktak.app.qa;

/** Host RemoteViews QA độc lập với điều hướng đăng nhập; chỉ có trong debug APK. */
public final class WidgetQaHostActivity extends android.app.Activity {
    @Override protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        var root=new android.widget.FrameLayout(this);
        root.setBackgroundColor(android.graphics.Color.rgb(24,24,30));
        setContentView(root);
    }
}
