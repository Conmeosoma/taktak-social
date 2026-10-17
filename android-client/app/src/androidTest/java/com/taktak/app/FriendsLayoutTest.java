package com.taktak.app;

import android.content.res.Configuration;
import android.view.*;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FriendsLayoutTest {
    @Test public void narrowLargeTextFriendsLayoutFitsInBothThemes() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            var target = InstrumentationRegistry.getInstrumentation().getTargetContext();
            for (int night : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
                var config = new Configuration(target.getResources().getConfiguration());
                config.fontScale = 1.5f;
                config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
                var themed = new androidx.appcompat.view.ContextThemeWrapper(target.createConfigurationContext(config), R.style.Theme_TakTak_Camera);
                float density = themed.getResources().getDisplayMetrics().density;
                for (int layout : new int[]{R.layout.fragment_friends, R.layout.item_friend, R.layout.item_friends_section}) {
                    View root = LayoutInflater.from(themed).inflate(layout, null, false);
                    TextView name = root.findViewById(R.id.friendName);
                    if (name != null) { name.setText("Tên hiển thị dài 📷 và có dấu tiếng Việt"); }
                    TextView username = root.findViewById(R.id.friendUsername);
                    if (username != null) { username.setText("@username_abcdefghijklmnopqrstuvwxyz_abcdefghijklmnopqrstuvwxyz"); }
                    TextView primary = root.findViewById(R.id.friendPrimary);
                    if (primary != null) { primary.setText(R.string.friends_accept); }
                    int width = Math.round(280 * density);
                    root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(Math.round(560 * density),
                                    layout == R.layout.fragment_friends ? View.MeasureSpec.EXACTLY : View.MeasureSpec.AT_MOST));
                    root.layout(0, 0, width, root.getMeasuredHeight());
                    bounds(root, width);
                    View recycler = root.findViewById(R.id.friendsRecycler);
                    if (recycler != null) { assertTrue("List retains usable height", recycler.getHeight() >= Math.round(100 * density)); }
                }
            }
        });
    }
    private void bounds(View view, int parentWidth) {
        if (view.getVisibility() == View.GONE) { return; }
        assertTrue("No horizontal overflow: " + view.getClass().getSimpleName(), view.getLeft() >= 0 && view.getRight() <= parentWidth);
        if (view instanceof TextView text && text.getLayout() != null) {
            assertTrue("Text height fits: " + view.getClass().getSimpleName(), text.getLayout().getHeight()
                    <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom());
        }
        if (view instanceof ViewGroup group) {
            for (int index = 0; index < group.getChildCount(); index++) { bounds(group.getChildAt(index), group.getWidth()); }
        }
    }
}
