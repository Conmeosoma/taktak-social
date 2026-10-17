package com.taktak.app.ui.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.view.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.*;
import androidx.camera.core.resolutionselector.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.taktak.app.MainActivity;
import com.taktak.app.R;
import com.taktak.app.databinding.FragmentCameraBinding;
import com.taktak.app.viewmodel.CameraSettingsViewModel;
import com.taktak.app.viewmodel.MomentViewModel;

public final class CameraFragment extends Fragment {
    private FragmentCameraBinding binding;
    private MomentViewModel model;
    private CameraSettingsViewModel settings;
    private ProcessCameraProvider provider;
    private ImageCapture capture;
    private Preview preview;
    private boolean busy, active, canSwitch, hasFlash, cameraNotice;
    private int requestId, targetRotation;
    private OrientationEventListener orientation;
    private final ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (binding == null) { return; }
                if (granted) { startCamera(); }
                else { showCameraError(R.string.moment_camera_denied); }
            });

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle state) {
        binding = FragmentCameraBinding.inflate(inflater, parent, false); return binding.getRoot();
    }
    @Override public void onViewCreated(@NonNull View view, Bundle state) {
        model = new ViewModelProvider(this).get(MomentViewModel.class);
        settings = new ViewModelProvider(requireActivity()).get(CameraSettingsViewModel.class);
        binding.cameraPreview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        orientation = new OrientationEventListener(requireContext()) {
            @Override public void onOrientationChanged(int degrees) {
                int rotation = CameraRotation.targetRotation(degrees);
                if (active && rotation != -1) {
                    targetRotation = rotation;
                    if (capture != null) { capture.setTargetRotation(rotation); }
                }
            }
        };
        binding.cameraPermissionButton.setOnClickListener(clicked -> {
            if (hasPermission()) { startCamera(); } else { permission.launch(Manifest.permission.CAMERA); }
        });
        binding.switchCameraButton.setOnClickListener(clicked -> {
            if (busy || capture == null || !canSwitch) { return; }
            settings.selectLens(settings.lensFacing() == CameraSelector.LENS_FACING_BACK
                    ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK);
            binding.serverImage.setVisibility(View.GONE);
            startCamera();
        });
        binding.flashButton.setOnClickListener(clicked -> {
            if (busy || capture == null || !hasFlash) { return; }
            settings.cycleFlash(); capture.setFlashMode(settings.flashMode()); updateControls();
        });
        binding.captureButton.setOnClickListener(clicked -> takePicture());
        binding.momentRetryButton.setOnClickListener(clicked -> model.retry());
        model.state().observe(getViewLifecycleOwner(), result -> {
            busy = result.busy;
            boolean error = result.retry || result.message == R.string.moment_camera_error
                    || result.message == R.string.auth_invalid_credentials;
            binding.momentStatus.setText(result.message);
            binding.momentProgress.setVisibility(result.busy ? View.VISIBLE : View.INVISIBLE);
            styleStatus(result.busy ? R.string.camera_status_busy : error ? R.string.camera_status_error
                    : result.downloaded != null ? R.string.camera_status_success : R.string.camera_status_ready,
                    error ? R.color.m3_error_container : result.downloaded != null ? R.color.m3_success_container : R.color.m3_secondary_container,
                    error ? R.color.m3_on_error_container : result.downloaded != null ? R.color.m3_on_success_container : R.color.m3_on_secondary_container);
            updateControls();
            binding.momentRetryButton.setVisibility(result.retry ? View.VISIBLE : View.GONE);
            binding.momentRetryButton.setEnabled(!busy);
            binding.serverImage.setVisibility(result.downloaded == null ? View.GONE : View.VISIBLE);
            if (result.downloaded != null) {
                Glide.with(this).load(result.downloaded).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(binding.serverImage);
            } else { Glide.with(this).clear(binding.serverImage); binding.serverImage.setImageDrawable(null); }
        });
    }

    @Override public void onResume() {
        super.onResume(); active = true;
        if (binding.cameraPreview.getDisplay() != null) { targetRotation = binding.cameraPreview.getDisplay().getRotation(); }
        if (orientation.canDetectOrientation()) { orientation.enable(); }
        if (hasPermission()) { startCamera(); }
        else {
            binding.cameraPermissionButton.setVisibility(View.VISIBLE);
            binding.cameraPermissionButton.setText(R.string.moment_camera_permission);
            binding.cameraPlaceholder.setVisibility(View.VISIBLE);
            updateControls();
        }
    }

    @Override public void onPause() {
        active = false; requestId++;
        if (orientation != null) { orientation.disable(); }
        unbindOwned(); updateControls();
        super.onPause();
    }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
    private boolean pageActive() {
        return active && getActivity() instanceof MainActivity activity && activity.isCameraPageActive();
    }
    private void startCamera() {
        if (binding == null || !pageActive() || !hasPermission()) { return; }
        int ticket = ++requestId;
        var currentBinding = binding;
        var owner = getViewLifecycleOwner();
        unbindOwned(); updateControls();
        binding.cameraPlaceholder.setText(R.string.camera_starting);
        binding.cameraPlaceholder.setVisibility(View.VISIBLE);
        var future = ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            if (binding != currentBinding || ticket != requestId || !pageActive()) { return; }
            try {
                provider = future.get();
                boolean back = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);
                boolean front = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
                if (!back && !front) { throw new IllegalStateException("Camera unavailable"); }
                if (settings.lensFacing() == CameraSelector.LENS_FACING_BACK && !back) { settings.selectLens(CameraSelector.LENS_FACING_FRONT); }
                if (settings.lensFacing() == CameraSelector.LENS_FACING_FRONT && !front) { settings.selectLens(CameraSelector.LENS_FACING_BACK); }
                canSwitch = back && front;
                var resolution = new ResolutionSelector.Builder().setResolutionStrategy(new ResolutionStrategy(
                        new Size(1280, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)).build();
                preview = new Preview.Builder().setResolutionSelector(resolution).setTargetRotation(targetRotation).build();
                preview.setSurfaceProvider(binding.cameraPreview.getSurfaceProvider());
                capture = new ImageCapture.Builder().setResolutionSelector(resolution).setTargetRotation(targetRotation)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();
                var selector = new CameraSelector.Builder().requireLensFacing(settings.lensFacing()).build();
                var camera = provider.bindToLifecycle(owner, selector, preview, capture);
                hasFlash = camera.getCameraInfo().hasFlashUnit();
                capture.setFlashMode(hasFlash ? settings.flashMode() : ImageCapture.FLASH_MODE_OFF);
                binding.cameraPermissionButton.setVisibility(View.GONE);
                binding.cameraPlaceholder.setVisibility(View.GONE);
                if (cameraNotice && model.state().getValue() != null
                        && model.state().getValue().message == R.string.moment_self_only) {
                    binding.momentStatus.setText(R.string.moment_self_only);
                    styleStatus(R.string.camera_status_ready, R.color.m3_secondary_container, R.color.m3_on_secondary_container);
                }
                cameraNotice = false; updateControls();
            } catch (Exception unavailable) {
                unbindOwned(); showCameraError(R.string.moment_camera_error);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void updateControls() {
        if (binding == null || settings == null) { return; }
        boolean ready = pageActive() && capture != null && !busy;
        binding.captureButton.setEnabled(ready);
        binding.switchCameraButton.setEnabled(ready && canSwitch);
        binding.switchCameraButton.setContentDescription(getString(!canSwitch ? R.string.camera_switch_unavailable
                : settings.lensFacing() == CameraSelector.LENS_FACING_FRONT ? R.string.camera_front_selected : R.string.camera_back_selected));
        binding.flashButton.setEnabled(ready && hasFlash);
        int flash = !hasFlash ? R.string.camera_flash_unavailable : settings.flashMode() == ImageCapture.FLASH_MODE_OFF
                ? R.string.camera_flash_off : settings.flashMode() == ImageCapture.FLASH_MODE_AUTO ? R.string.camera_flash_auto : R.string.camera_flash_on;
        binding.flashButton.setText(flash);
    }
    private void showCameraError(int message) {
        cameraNotice = true;
        if (binding == null) { return; }
        binding.momentStatus.setText(message);
        binding.cameraPermissionButton.setVisibility(View.VISIBLE);
        binding.cameraPermissionButton.setText(hasPermission() ? R.string.camera_open_retry : R.string.moment_camera_permission);
        binding.cameraPlaceholder.setVisibility(View.VISIBLE);
        binding.cameraPlaceholder.setText(hasPermission() ? R.string.moment_camera_error : R.string.camera_permission_hint);
        styleStatus(R.string.camera_status_error, R.color.m3_error_container, R.color.m3_on_error_container);
        updateControls();
    }
    private void styleStatus(int title, int background, int foreground) {
        binding.statusTitle.setText(title);
        binding.statusTitle.setTextColor(ContextCompat.getColor(requireContext(), foreground));
        binding.momentStatus.setTextColor(ContextCompat.getColor(requireContext(), foreground));
        binding.statusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), background));
    }
    private void unbindOwned() {
        if (provider != null) {
            if (preview != null) { provider.unbind(preview); }
            if (capture != null) { provider.unbind(capture); }
        }
        capture = null; preview = null; canSwitch = false; hasFlash = false;
    }
    private void takePicture() {
        if (!pageActive() || capture == null || !model.beginCapture()) { return; }
        var captureModel = model;
        capture.takePicture(ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageCapturedCallback() {
            @Override public void onCaptureSuccess(@NonNull ImageProxy image) {
                try { captureModel.acceptCapture(image.toBitmap(), image.getImageInfo().getRotationDegrees()); }
                catch (RuntimeException failure) { captureModel.captureFailed(); }
                finally { image.close(); }
            }
            @Override public void onError(@NonNull ImageCaptureException error) { captureModel.captureFailed(); }
        });
    }
    @Override public void onDestroyView() {
        active = false; requestId++;
        if (orientation != null) { orientation.disable(); orientation = null; }
        unbindOwned();
        if (binding != null) { Glide.with(this).clear(binding.serverImage); binding.serverImage.setImageDrawable(null); }
        binding = null; super.onDestroyView();
    }
}
