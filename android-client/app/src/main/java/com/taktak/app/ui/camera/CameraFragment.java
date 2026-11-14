package com.taktak.app.ui.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Size;
import android.view.*;
import java.io.File;
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
import com.taktak.app.MainActivity;
import com.taktak.app.R;
import com.taktak.app.databinding.FragmentCameraBinding;
import com.taktak.app.viewmodel.CameraSettingsViewModel;
import com.taktak.app.viewmodel.CaptureViewModel;

public final class CameraFragment extends Fragment {
    private FragmentCameraBinding binding;
    private CaptureViewModel model;
    private CameraSettingsViewModel settings;
    private ProcessCameraProvider provider;
    private ImageCapture capture;
    private androidx.camera.video.VideoCapture<androidx.camera.video.Recorder> videoCapture;
    private androidx.camera.video.Recording activeRecording;
    private Preview preview;
    private boolean busy, active, canSwitch, hasFlash, cameraNotice, openingPreview;
    private boolean dualMode = false;
    private boolean liveMode = false;
    private boolean shutterHeld, recordingCancelled, stopRequested;
    private boolean liveAudio = false;
    private boolean pipOnLeft = true;
    private int requestId, targetRotation;
    private OrientationEventListener orientation;
    private final ActivityResultLauncher<String> audioPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    liveAudio = true;
                    if (binding != null) { binding.btnLiveAudioToggle.setText(R.string.camera_live_audio_on); }
                } else {
                    liveAudio = false;
                    if (binding != null) { binding.btnLiveAudioToggle.setText(R.string.camera_live_audio_off); }
                }
            });
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
        model = new ViewModelProvider(this).get(CaptureViewModel.class);
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
            startCamera();
        });
        binding.flashButton.setOnClickListener(clicked -> {
            if (busy || capture == null || !hasFlash) { return; }
            settings.cycleFlash(); capture.setFlashMode(settings.flashMode()); updateControls();
        });
        binding.dualModeButton.setOnClickListener(clicked -> {
            if (busy || capture == null || !canSwitch) { return; }
            dualMode = !dualMode;
            updateControls();
        });
        binding.cameraPipContainer.setOnClickListener(clicked -> {
            if (busy) { return; }
            pipOnLeft = !pipOnLeft;
            var lp = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) binding.cameraPipContainer.getLayoutParams();
            if (pipOnLeft) {
                lp.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                lp.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
            } else {
                lp.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                lp.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET;
            }
            binding.cameraPipContainer.setLayoutParams(lp);
        });
        binding.btnLiveAudioToggle.setOnClickListener(v -> {
            if (busy) return;
            if (!liveAudio) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    liveAudio = true;
                    binding.btnLiveAudioToggle.setText(R.string.camera_live_audio_on);
                } else {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
                }
            } else {
                liveAudio = false;
                binding.btnLiveAudioToggle.setText(R.string.camera_live_audio_off);
            }
        });
        binding.captureButton.setOnClickListener(clicked -> takePicture());
        binding.captureButton.setOnLongClickListener(shutter -> beginLiveHold());
        binding.captureButton.setHoldCallbacks(() -> endLiveHold(false), () -> endLiveHold(true));
        model.state().observe(getViewLifecycleOwner(), result -> {
            busy = result.busy();
            boolean error = result.message() == R.string.moment_camera_error;
            binding.momentStatus.setText(result.message());
            binding.momentProgress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
            styleStatus(busy ? R.string.camera_status_busy : error ? R.string.camera_status_error : R.string.camera_status_ready,
                    error ? R.color.m3_error_container : R.color.m3_secondary_container,
                    error ? R.color.m3_on_error_container : R.color.m3_on_secondary_container);
            updateControls();
            openPreview();
        });
    }

    @Override public void onResume() {
        super.onResume(); active = true; openingPreview = false;
        openPreview();
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
        if (binding != null) { binding.captureButton.resetGesture(); }
        endLiveHold(true);
        liveMode = false;
        if (orientation != null) { orientation.disable(); }
        unbindOwned(); updateControls();
        super.onPause();
    }

    private boolean hasPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
    private boolean pageActive() {
        return active && !openingPreview && getActivity() instanceof MainActivity activity && activity.isCameraPageActive();
    }
    private void startCamera() {
        startCameraWithCallback(null);
    }
    private void startCameraWithCallback(Runnable onReady) {
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
                var selector = new CameraSelector.Builder().requireLensFacing(settings.lensFacing()).build();

                if (liveMode) {
                    var recorder = new androidx.camera.video.Recorder.Builder()
                            .setQualitySelector(androidx.camera.video.QualitySelector.from(androidx.camera.video.Quality.SD,
                                    androidx.camera.video.FallbackStrategy.lowerQualityOrHigherThan(androidx.camera.video.Quality.SD)))
                            .build();
                    videoCapture = androidx.camera.video.VideoCapture.withOutput(recorder);
                    capture = null;
                    var camera = provider.bindToLifecycle(owner, selector, preview, videoCapture);
                    hasFlash = camera.getCameraInfo().hasFlashUnit();
                } else {
                    videoCapture = null;
                    capture = new ImageCapture.Builder().setResolutionSelector(resolution).setTargetRotation(targetRotation)
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();
                    var camera = provider.bindToLifecycle(owner, selector, preview, capture);
                    hasFlash = camera.getCameraInfo().hasFlashUnit();
                    capture.setFlashMode(hasFlash ? settings.flashMode() : ImageCapture.FLASH_MODE_OFF);
                }

                binding.cameraPermissionButton.setVisibility(View.GONE);
                binding.cameraPlaceholder.setVisibility(View.GONE);
                if (cameraNotice && model.state().getValue() != null
                        && model.state().getValue().message() == R.string.preview_capture_hint) {
                    binding.momentStatus.setText(R.string.preview_capture_hint);
                    styleStatus(R.string.camera_status_ready, R.color.m3_secondary_container, R.color.m3_on_secondary_container);
                }
                cameraNotice = false; updateControls();
                if (onReady != null) { onReady.run(); }
            } catch (Exception unavailable) {
                shutterHeld = false; liveMode = false;
                unbindOwned(); showCameraError(R.string.moment_camera_error);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void updateControls() {
        if (binding == null || settings == null) { return; }
        boolean ready = pageActive() && (capture != null || videoCapture != null) && !busy;
        binding.captureButton.setEnabled(ready || shutterHeld);
        binding.captureButton.setAlpha(liveMode && busy ? 0.65f : 1f);
        binding.btnLiveAudioToggle.setEnabled(ready && !shutterHeld);
        binding.switchCameraButton.setEnabled(ready && canSwitch);
        binding.switchCameraButton.setContentDescription(getString(!canSwitch ? R.string.camera_switch_unavailable
                : settings.lensFacing() == CameraSelector.LENS_FACING_FRONT ? R.string.camera_front_selected : R.string.camera_back_selected));
        binding.flashButton.setEnabled(ready && hasFlash && !liveMode);
        int flash = !hasFlash ? R.string.camera_flash_unavailable : settings.flashMode() == ImageCapture.FLASH_MODE_OFF
                ? R.string.camera_flash_off : settings.flashMode() == ImageCapture.FLASH_MODE_AUTO ? R.string.camera_flash_auto : R.string.camera_flash_on;
        binding.flashButton.setText(flash);
        binding.dualModeButton.setEnabled(ready && canSwitch && !liveMode);
        binding.dualModeButton.setText(dualMode ? R.string.camera_mode_dual : R.string.camera_mode_single);
        binding.cameraPipContainer.setVisibility(dualMode && ready && !liveMode ? View.VISIBLE : View.GONE);
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
        int backgroundColor = ContextCompat.getColor(requireContext(), background);
        int foregroundColor = ContextCompat.getColor(requireContext(), foreground);
        var alternate = new android.util.TypedValue();
        if (binding.getRoot().getContext().getTheme().resolveAttribute(R.attr.ttAuthAlternate,alternate,true) && alternate.data != 0) {
            boolean error = background == R.color.m3_error_container;
            backgroundColor = com.google.android.material.color.MaterialColors.getColor(binding.getRoot(),error
                    ? com.google.android.material.R.attr.colorErrorContainer : com.google.android.material.R.attr.colorSecondaryContainer);
            foregroundColor = com.google.android.material.color.MaterialColors.getColor(binding.getRoot(),error
                    ? com.google.android.material.R.attr.colorOnErrorContainer : com.google.android.material.R.attr.colorOnSecondaryContainer);
        }
        binding.statusTitle.setText(title);
        binding.statusTitle.setTextColor(foregroundColor);
        binding.momentStatus.setTextColor(foregroundColor);
        binding.statusCard.setCardBackgroundColor(backgroundColor);
    }
    private void unbindOwned() {
        if (provider != null) {
            if (preview != null) { provider.unbind(preview); }
            if (capture != null) { provider.unbind(capture); }
            if (videoCapture != null) { provider.unbind(videoCapture); }
        }
        capture = null; videoCapture = null; preview = null; canSwitch = false; hasFlash = false;
    }
    private void takePicture() {
        if (!pageActive() || busy) { return; }
        if (liveMode || shutterHeld) { return; }
        if (capture == null || !model.beginCapture()) { return; }
        var captureModel = model;
        boolean mirror = settings.lensFacing() == CameraSelector.LENS_FACING_FRONT;

        if (!dualMode) {
            capture.takePicture(ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageCapturedCallback() {
                @Override public void onCaptureSuccess(@NonNull ImageProxy image) {
                    try { captureModel.acceptCapture(image.toBitmap(), image.getImageInfo().getRotationDegrees(), mirror); }
                    catch (RuntimeException failure) { captureModel.captureFailed(); }
                    finally { image.close(); }
                }
                @Override public void onError(@NonNull ImageCaptureException error) { captureModel.captureFailed(); }
            });
            return;
        }

        // Dual Camera Sequential Fallback Capture
        styleStatus(R.string.camera_status_busy, R.color.m3_secondary_container, R.color.m3_on_secondary_container);
        binding.momentStatus.setText(R.string.camera_dual_capturing_main);

        capture.takePicture(ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageCapturedCallback() {
            @Override public void onCaptureSuccess(@NonNull ImageProxy firstImage) {
                Bitmap firstBitmap = firstImage.toBitmap();
                int firstRotation = firstImage.getImageInfo().getRotationDegrees();
                boolean firstMirror = mirror;
                firstImage.close();

                if (binding == null) {
                    firstBitmap.recycle();
                    captureModel.captureFailed();
                    return;
                }

                binding.momentStatus.setText(R.string.camera_dual_capturing_sub);
                int primaryLens = settings.lensFacing();
                int secondaryLens = (primaryLens == CameraSelector.LENS_FACING_BACK)
                        ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
                settings.selectLens(secondaryLens);

                startCameraWithCallback(() -> {
                    if (capture == null) {
                        firstBitmap.recycle();
                        settings.selectLens(primaryLens);
                        startCamera();
                        captureModel.captureFailed();
                        return;
                    }

                    boolean secondMirror = (secondaryLens == CameraSelector.LENS_FACING_FRONT);
                    capture.takePicture(ContextCompat.getMainExecutor(requireContext()), new ImageCapture.OnImageCapturedCallback() {
                        @Override public void onCaptureSuccess(@NonNull ImageProxy secondImage) {
                            Bitmap secondBitmap = secondImage.toBitmap();
                            int secondRotation = secondImage.getImageInfo().getRotationDegrees();
                            secondImage.close();

                            settings.selectLens(primaryLens);
                            startCamera();

                            binding.momentStatus.setText(R.string.camera_dual_compositing);
                            new Thread(() -> {
                                try {
                                    byte[] jpeg = DualCameraCompositor.composite(
                                            firstBitmap, firstRotation, firstMirror,
                                            secondBitmap, secondRotation, secondMirror,
                                            pipOnLeft
                                    );
                                    ContextCompat.getMainExecutor(requireContext()).execute(() -> {
                                        captureModel.acceptRawJpeg(jpeg);
                                    });
                                } catch (Exception e) {
                                    ContextCompat.getMainExecutor(requireContext()).execute(captureModel::captureFailed);
                                } finally {
                                    firstBitmap.recycle();
                                    secondBitmap.recycle();
                                }
                            }).start();
                        }

                        @Override public void onError(@NonNull ImageCaptureException error) {
                            firstBitmap.recycle();
                            settings.selectLens(primaryLens);
                            startCamera();
                            captureModel.captureFailed();
                        }
                    });
                });
            }

            @Override public void onError(@NonNull ImageCaptureException error) {
                captureModel.captureFailed();
            }
        });
    }

    private boolean beginLiveHold() {
        if (!pageActive() || busy || capture == null) { return false; }
        shutterHeld = true; liveMode = true;
        startCameraWithCallback(() -> {
            if (shutterHeld && pageActive()) { takeLiveMoment(); }
        });
        return true;
    }

    private void endLiveHold(boolean cancel) {
        shutterHeld = false;
        if (activeRecording != null) {
            recordingCancelled |= cancel;
            if (!stopRequested) {
                stopRequested = true;
                activeRecording.stop();
            }
        } else if (liveMode) {
            liveMode = false;
            startCamera();
        }
        updateControls();
    }

    private void takeLiveMoment() {
        if (!shutterHeld || videoCapture == null || !model.beginCapture()) { return; }
        busy = true; recordingCancelled = false; stopRequested = false; updateControls();
        styleStatus(R.string.camera_status_busy, R.color.m3_secondary_container, R.color.m3_on_secondary_container);
        binding.momentStatus.setText(R.string.camera_live_recording);
        File tempVideo = new File(requireContext().getCacheDir(), "live_" + java.util.UUID.randomUUID() + ".mp4");
        try {
            var outputOptions = new androidx.camera.video.FileOutputOptions.Builder(tempVideo).build();
            var pending = videoCapture.getOutput().prepareRecording(requireContext(), outputOptions);
            if (liveAudio && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                pending = pending.withAudioEnabled();
            }
            activeRecording = pending.start(ContextCompat.getMainExecutor(requireContext()), event -> {
                if (event instanceof androidx.camera.video.VideoRecordEvent.Start) {
                    var recording = activeRecording;
                    if (binding != null) {
                        binding.getRoot().postDelayed(() -> {
                            if (activeRecording == recording && recording != null) { endLiveHold(false); }
                        }, 3000);
                    }
                } else if (event instanceof androidx.camera.video.VideoRecordEvent.Finalize finalize) {
                    activeRecording = null; shutterHeld = false; liveMode = false;
                    if (!finalize.hasError() && !recordingCancelled && pageActive()) {
                        processLiveVideo(tempVideo);
                    } else {
                        tempVideo.delete(); model.captureFailed();
                        if (pageActive()) {
                            startCamera();
                            if (!recordingCancelled) { showCameraError(R.string.moment_camera_error); }
                        }
                    }
                    updateControls();
                }
            });
        } catch (RuntimeException failure) {
            tempVideo.delete(); activeRecording = null; shutterHeld = false; liveMode = false;
            model.captureFailed(); startCamera(); showCameraError(R.string.moment_camera_error);
        }
    }

    private void processLiveVideo(File tempVideo) {
        android.media.MediaMetadataRetriever mmr = new android.media.MediaMetadataRetriever();
        try {
            mmr.setDataSource(tempVideo.getAbsolutePath());
            Bitmap frame = mmr.getFrameAtTime(0, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (frame == null) { frame = mmr.getFrameAtTime(0); }
            if (frame != null) {
                byte[] thumb = PhotoProcessor.squareJpeg(frame, 0, false);
                frame.recycle();
                model.acceptLiveCapture(thumb, tempVideo);
            } else {
                tempVideo.delete();
                model.captureFailed(); startCamera();
                showCameraError(R.string.moment_camera_error);
            }
        } catch (Exception e) {
            tempVideo.delete();
            model.captureFailed(); startCamera();
            showCameraError(R.string.moment_camera_error);
        } finally {
            try { mmr.release(); } catch (Exception ignored) { }
        }
    }

    private void openPreview() {
        var state = model.state().getValue();
        if (!pageActive() || state == null || state.draftId() == null) { return; }
        var intent = new android.content.Intent(requireContext(), PreviewActivity.class)
                .putExtra(PreviewActivity.DRAFT_ID, state.draftId()).putExtra(PreviewActivity.GENERATION, model.generation());
        openingPreview = true; model.consume(); startActivity(intent);
    }
    @Override public void onDestroyView() {
        active = false; requestId++;
        if (orientation != null) { orientation.disable(); orientation = null; }
        unbindOwned();
        binding = null; super.onDestroyView();
    }
}
