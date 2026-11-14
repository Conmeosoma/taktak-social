package com.taktak.app.ui.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import androidx.appcompat.widget.AppCompatImageButton;

/** Chạm chụp, giữ quay; luôn dùng performClick/performLongClick cho accessibility. */
public final class ShutterButton extends AppCompatImageButton {
    private Runnable release, cancel;
    private boolean down, longPress;
    private final Runnable start = () -> {
        if (down) { longPress = true; performLongClick(); }
    };

    public ShutterButton(Context context, AttributeSet attrs) { super(context, attrs); }

    public void setHoldCallbacks(Runnable release, Runnable cancel) {
        this.release = release; this.cancel = cancel;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (release == null || cancel == null) { return super.onTouchEvent(event); }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!isEnabled()) { return false; }
                resetGesture(); down = true; longPress = false;
                setPressed(true);
                getParent().requestDisallowInterceptTouchEvent(true);
                postDelayed(start, ViewConfiguration.getLongPressTimeout());
                return true;
            case MotionEvent.ACTION_MOVE:
                int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
                if (event.getX() < -slop || event.getY() < -slop
                        || event.getX() > getWidth() + slop || event.getY() > getHeight() + slop) {
                    finish(false);
                }
                return true;
            case MotionEvent.ACTION_UP:
                boolean tap = down && !longPress;
                finish(true);
                if (tap) { performClick(); }
                return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_DOWN:
                finish(false); return true;
            default: return true;
        }
    }

    @Override public boolean performClick() { return super.performClick(); }

    private void finish(boolean commit) {
        if (!down) { return; }
        boolean wasLong = longPress;
        resetGesture();
        if (wasLong) { if (commit) { release.run(); } else { cancel.run(); } }
    }

    public void resetGesture() {
        down = false; removeCallbacks(start); setPressed(false);
        if (getParent() != null) { getParent().requestDisallowInterceptTouchEvent(false); }
    }

    @Override protected void onDetachedFromWindow() {
        resetGesture(); super.onDetachedFromWindow();
    }
}
