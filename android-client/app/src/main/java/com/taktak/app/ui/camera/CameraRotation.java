package com.taktak.app.ui.camera;

import android.view.Surface;

public final class CameraRotation {
    private CameraRotation() { }
    public static int targetRotation(int degrees) {
        if (degrees < 0 || degrees >= 360) { return -1; }
        if (degrees < 45 || degrees >= 315) { return Surface.ROTATION_0; }
        if (degrees < 135) { return Surface.ROTATION_270; }
        if (degrees < 225) { return Surface.ROTATION_180; }
        return Surface.ROTATION_90;
    }
}
