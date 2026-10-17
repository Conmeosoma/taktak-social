package com.taktak.app.ui.camera;

import android.view.Surface;
import org.junit.Test;
import static org.junit.Assert.*;

public class CameraRotationTest {
    @Test public void quadrantsMapToCameraXSurfaceRotation() {
        assertEquals(Surface.ROTATION_0, CameraRotation.targetRotation(0));
        assertEquals(Surface.ROTATION_270, CameraRotation.targetRotation(90));
        assertEquals(Surface.ROTATION_180, CameraRotation.targetRotation(180));
        assertEquals(Surface.ROTATION_90, CameraRotation.targetRotation(270));
    }
    @Test public void boundariesSnapWithoutInvalidSurfaceValues() {
        for (int degrees = 0; degrees < 360; degrees++) {
            int expected = degrees < 45 || degrees >= 315 ? Surface.ROTATION_0
                    : degrees < 135 ? Surface.ROTATION_270 : degrees < 225 ? Surface.ROTATION_180 : Surface.ROTATION_90;
            assertEquals(expected, CameraRotation.targetRotation(degrees));
        }
    }
    @Test public void unknownOrientationIsIgnored() {
        assertEquals(-1, CameraRotation.targetRotation(-1)); assertEquals(-1, CameraRotation.targetRotation(360));
    }
}
