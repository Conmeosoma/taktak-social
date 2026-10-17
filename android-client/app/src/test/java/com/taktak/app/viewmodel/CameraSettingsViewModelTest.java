package com.taktak.app.viewmodel;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import org.junit.Test;
import static org.junit.Assert.*;

public class CameraSettingsViewModelTest {
    @Test public void startsRearAndFlashOffThenCyclesThreeModes() {
        var settings = new CameraSettingsViewModel();
        assertEquals(CameraSelector.LENS_FACING_BACK, settings.lensFacing());
        assertEquals(ImageCapture.FLASH_MODE_OFF, settings.flashMode());
        settings.cycleFlash(); assertEquals(ImageCapture.FLASH_MODE_AUTO, settings.flashMode());
        settings.cycleFlash(); assertEquals(ImageCapture.FLASH_MODE_ON, settings.flashMode());
        settings.cycleFlash(); assertEquals(ImageCapture.FLASH_MODE_OFF, settings.flashMode());
    }
    @Test public void selectingLensKeepsFlashPreferenceAndRejectsOtherValues() {
        var settings = new CameraSettingsViewModel(); settings.cycleFlash();
        settings.selectLens(CameraSelector.LENS_FACING_FRONT);
        assertEquals(CameraSelector.LENS_FACING_FRONT, settings.lensFacing());
        assertEquals(ImageCapture.FLASH_MODE_AUTO, settings.flashMode());
        assertThrows(IllegalArgumentException.class, () -> settings.selectLens(99));
        assertEquals(CameraSelector.LENS_FACING_FRONT, settings.lensFacing());
    }
}
