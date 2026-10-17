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
import com.taktak.app.R;
import com.taktak.app.databinding.FragmentCameraBinding;
import com.taktak.app.viewmodel.MomentViewModel;

public final class CameraFragment extends Fragment {
    private FragmentCameraBinding binding;
    private MomentViewModel model;
    private ProcessCameraProvider provider;
    private ImageCapture capture;
    private Preview preview;
    private boolean busy;
    private boolean cameraNotice;
    private final ActivityResultLauncher<String> permission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (binding == null) { return; }
        if (granted) { startCamera(); }
        else {
            cameraNotice = true;
            binding.momentStatus.setText(R.string.moment_camera_denied);
            styleStatus(R.string.camera_status_error, R.color.camera_error_surface, R.color.camera_error_ink);
        }
    });

    @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle state) {
        binding = FragmentCameraBinding.inflate(inflater, parent, false); return binding.getRoot();
    }
    @Override public void onViewCreated(@NonNull View view, Bundle state) {
        model = new ViewModelProvider(this).get(MomentViewModel.class);
        binding.cameraPreview.setImplementationMode(PreviewView.ImplementationMode.COMPATIBLE);
        binding.cameraPermissionButton.setOnClickListener(clicked -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { startCamera(); }
            else { permission.launch(Manifest.permission.CAMERA); }
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
                    error ? R.color.camera_error_surface : result.downloaded != null ? R.color.camera_success_surface : R.color.camera_soft,
                    error ? R.color.camera_error_ink : result.downloaded != null ? R.color.camera_success_ink : R.color.camera_ink);
            binding.captureButton.setEnabled(capture != null && !busy);
            binding.momentRetryButton.setVisibility(result.retry ? View.VISIBLE : View.GONE);
            binding.momentRetryButton.setEnabled(!busy);
            binding.serverImage.setVisibility(result.downloaded == null ? View.GONE : View.VISIBLE);
            if (result.downloaded != null) {
                Glide.with(this).load(result.downloaded).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(binding.serverImage);
            } else { Glide.with(this).clear(binding.serverImage); binding.serverImage.setImageDrawable(null); }
        });
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) { startCamera(); }
    }

    private void startCamera() {
        if (binding == null) { return; }
        var owner = getViewLifecycleOwner();
        var future = ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            if (binding == null || owner != getViewLifecycleOwner()) { return; }
            try {
                provider = future.get();
                var resolution = new ResolutionSelector.Builder().setResolutionStrategy(new ResolutionStrategy(
                        new Size(1280, 960), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER)).build();
                preview = new Preview.Builder().setResolutionSelector(resolution).build();
                preview.setSurfaceProvider(binding.cameraPreview.getSurfaceProvider());
                capture = new ImageCapture.Builder().setResolutionSelector(resolution)
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();
                provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture);
                binding.cameraPermissionButton.setVisibility(View.GONE);
                binding.cameraPlaceholder.setVisibility(View.GONE);
                if (cameraNotice && model.state().getValue() != null
                        && model.state().getValue().message == R.string.moment_self_only) {
                    binding.momentStatus.setText(R.string.moment_self_only);
                    styleStatus(R.string.camera_status_ready, R.color.camera_soft, R.color.camera_ink);
                }
                cameraNotice = false;
                binding.captureButton.setEnabled(!busy);
            } catch (Exception unavailable) {
                cameraNotice = true;
                capture = null; binding.momentStatus.setText(R.string.moment_camera_error);
                binding.cameraPermissionButton.setVisibility(View.VISIBLE);
                binding.cameraPlaceholder.setVisibility(View.VISIBLE);
                binding.cameraPlaceholder.setText(R.string.moment_camera_error);
                styleStatus(R.string.camera_status_error, R.color.camera_error_surface, R.color.camera_error_ink);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void styleStatus(int title, int background, int foreground) {
        binding.statusTitle.setText(title);
        binding.statusTitle.setTextColor(ContextCompat.getColor(requireContext(), foreground));
        binding.momentStatus.setTextColor(ContextCompat.getColor(requireContext(), foreground));
        binding.statusCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), background));
    }

    private void takePicture() {
        if (capture == null || !model.beginCapture()) { return; }
        capture.setTargetRotation(binding.cameraPreview.getDisplay().getRotation());
        capture.takePicture(ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageCapturedCallback() {
            @Override public void onCaptureSuccess(@NonNull ImageProxy image) {
                try { model.acceptCapture(image.toBitmap(), image.getImageInfo().getRotationDegrees()); }
                catch (RuntimeException failure) { model.captureFailed(); }
                finally { image.close(); }
            }
            @Override public void onError(@NonNull ImageCaptureException error) { model.captureFailed(); }
        });
    }
    @Override public void onDestroyView() {
        if (provider != null) {
            if (preview != null) { provider.unbind(preview); }
            if (capture != null) { provider.unbind(capture); }
        }
        capture = null; preview = null;
        if (binding != null) { Glide.with(this).clear(binding.serverImage); binding.serverImage.setImageDrawable(null); }
        binding = null; super.onDestroyView();
    }
}
