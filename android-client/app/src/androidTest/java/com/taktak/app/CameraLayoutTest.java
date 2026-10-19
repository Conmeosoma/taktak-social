package com.taktak.app;

import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Kiểm tra layout 320dp/chữ lớn trong context riêng, không đổi cài đặt điện thoại. */
@RunWith(AndroidJUnit4.class)
public class CameraLayoutTest {
    @Test public void narrowLargeTextLayoutDoesNotClipControls() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            var target = InstrumentationRegistry.getInstrumentation().getTargetContext();
            for (int night : new int[] { Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES }) {
                var config = new Configuration(target.getResources().getConfiguration());
                config.fontScale = 1.5f;
                config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
                var themed = new androidx.appcompat.view.ContextThemeWrapper(target.createConfigurationContext(config),
                        R.style.Theme_TakTak_Media_Main);
                View root = LayoutInflater.from(themed).inflate(R.layout.fragment_camera, null, false);
                int width = Math.round(280 * themed.getResources().getDisplayMetrics().density);
                root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                root.layout(0, 0, width, root.getMeasuredHeight());
                checkBounds(root, width);
                View shutter = root.findViewById(R.id.captureButton);
                assertEquals(shutter.getWidth(), shutter.getHeight());
                var preview = root.findViewById(R.id.cameraPreview);
                assertTrue(Math.abs(preview.getWidth() - preview.getHeight()) <= 1);
            }
        });
    }

    private void checkBounds(View view, int parentWidth) {
        assertTrue("No horizontal overflow: " + view.getClass().getSimpleName(), view.getRight() <= parentWidth);
        if (view instanceof TextView text && text.getLayout() != null) {
            for (int line = 0; line < text.getLayout().getLineCount(); line++) {
                assertEquals("No ellipsized text", 0, text.getLayout().getEllipsisCount(line));
            }
            assertTrue("Text height fits", text.getLayout().getHeight()
                    <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom());
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) { checkBounds(group.getChildAt(i), group.getWidth()); }
        }
    }
}
