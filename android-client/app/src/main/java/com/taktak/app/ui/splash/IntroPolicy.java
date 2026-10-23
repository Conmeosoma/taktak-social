package com.taktak.app.ui.splash;

import java.util.concurrent.atomic.AtomicBoolean;

/** Mỗi process chỉ tiêu thụ cơ hội mở intro một lần, kể cả entry đặc biệt. */
public final class IntroPolicy {
    private final AtomicBoolean consumed = new AtomicBoolean();
    public boolean consume(boolean restored, boolean launcher, boolean special, boolean enabled, boolean motion) {
        return !consumed.getAndSet(true) && !restored && launcher && !special && enabled && motion;
    }
    public static boolean audible(boolean enabled, boolean foreground, boolean normalRinger,
                                  boolean dndOff, boolean otherMusic, boolean streamMuted) {
        return enabled && foreground && normalRinger && dndOff && !otherMusic && !streamMuted;
    }
    public static float fitScale(int viewWidth, int viewHeight, int videoWidth, int videoHeight) {
        if (viewWidth <= 0 || viewHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) return 0;
        return Math.min(viewWidth / (float) videoWidth, viewHeight / (float) videoHeight);
    }
}
