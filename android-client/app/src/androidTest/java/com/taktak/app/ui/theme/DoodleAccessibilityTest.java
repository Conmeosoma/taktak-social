package com.taktak.app.ui.theme;

import android.view.MotionEvent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.taktak.app.ui.camera.DoodleOverlayView;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DoodleAccessibilityTest {
    @Test public void completedGestureDispatchesClickButCancelAndDisabledDoNot() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            var context = InstrumentationRegistry.getInstrumentation().getTargetContext();
            var view = new DoodleOverlayView(context);
            var clicks = new AtomicInteger();
            view.setOnClickListener(v -> clicks.incrementAndGet());
            assertFalse(touch(view, MotionEvent.ACTION_DOWN));
            assertEquals(0, clicks.get());
            view.setMode(DoodleOverlayView.Mode.DRAW);
            assertTrue(touch(view, MotionEvent.ACTION_DOWN));
            assertTrue(touch(view, MotionEvent.ACTION_MOVE));
            assertEquals(0, clicks.get());
            assertTrue(touch(view, MotionEvent.ACTION_UP));
            assertEquals(1, clicks.get());
            assertTrue(touch(view, MotionEvent.ACTION_DOWN));
            assertTrue(touch(view, MotionEvent.ACTION_CANCEL));
            assertEquals(1, clicks.get());
        });
    }

    private static boolean touch(DoodleOverlayView view, int action) {
        var event = MotionEvent.obtain(0, 10, action, 20, 20, 0);
        try { return view.onTouchEvent(event); }
        finally { event.recycle(); }
    }
}
