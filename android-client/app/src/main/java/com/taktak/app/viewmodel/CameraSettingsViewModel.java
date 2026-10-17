package com.taktak.app.viewmodel;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.lifecycle.ViewModel;

/** Chỉ giữ cấu hình UI qua recreate; không chứa ảnh/token hoặc lưu xuống disk. */
public final class CameraSettingsViewModel extends ViewModel {
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private int flashMode = ImageCapture.FLASH_MODE_OFF;
    public int lensFacing() { return lensFacing; }
    public int flashMode() { return flashMode; }
    public void selectLens(int facing) {
        if (facing != CameraSelector.LENS_FACING_BACK && facing != CameraSelector.LENS_FACING_FRONT) {
            throw new IllegalArgumentException("Unsupported lens facing");
        }
        lensFacing = facing;
    }
    public void cycleFlash() {
        flashMode = flashMode == ImageCapture.FLASH_MODE_OFF ? ImageCapture.FLASH_MODE_AUTO
                : flashMode == ImageCapture.FLASH_MODE_AUTO ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF;
    }
}
