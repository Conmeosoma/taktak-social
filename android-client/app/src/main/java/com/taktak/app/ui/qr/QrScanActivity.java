package com.taktak.app.ui.qr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.taktak.app.R;
import com.taktak.app.databinding.ActivityQrScanBinding;
import com.taktak.app.databinding.DialogQrFriendFoundBinding;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QrScanActivity extends AppCompatActivity {
    private ActivityQrScanBinding binding;
    private QrScanViewModel model;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService galleryExecutor = Executors.newSingleThreadExecutor();
    private ProcessCameraProvider provider;
    private Preview preview;
    private ImageAnalysis analysis;
    private Camera camera;
    private BottomSheetDialog foundDialog;
    private DialogQrFriendFoundBinding foundBinding;
    private boolean foreground, pickingImage, readingImage, flashOn, requestedPermission;
    private volatile boolean scanningPaused = true;
    private boolean alive() { return binding != null && !isFinishing() && !isDestroyed(); }
    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (!alive()) { return; }
                if (granted && foreground) { startCamera(); } else { showPermissionState(); }
            });
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::onGalleryImagePicked);

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQrScanBinding.inflate(getLayoutInflater()); setContentView(binding.getRoot());
        requestedPermission = savedInstanceState != null && savedInstanceState.getBoolean("qr_permission_requested");
        model = new ViewModelProvider(this).get(QrScanViewModel.class);
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            var bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        binding.btnQrBack.setOnClickListener(view -> finish());
        binding.btnQrFlash.setEnabled(false);
        binding.btnQrFlash.setOnClickListener(view -> {
            if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
                flashOn = !flashOn; camera.getCameraControl().enableTorch(flashOn);
                binding.btnQrFlash.setAlpha(flashOn ? 1f : .6f);
            }
        });
        binding.btnQrRetry.setOnClickListener(view -> {
            if (hasCameraPermission()) { startCamera(); }
            else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) || !requestedPermission) { requestCameraPermission(); }
            else { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))); }
        });
        binding.btnQrPickGallery.setOnClickListener(view -> { pickingImage = true; refreshPaused(); galleryLauncher.launch("image/*"); });
        binding.btnQrShowMyCode.setOnClickListener(view -> {
            var me = model.profile().getValue();
            if (!model.hasCurrentProfile()) { model.loadProfile(); Toast.makeText(this, R.string.qr_checking_identity, Toast.LENGTH_SHORT).show(); return; }
            if (!getSupportFragmentManager().isStateSaved()) {
                MyQrCodeBottomSheet.newInstance(me.username, me.displayName, me.id).showNow(getSupportFragmentManager(), "MyQrCode"); refreshPaused();
            }
        });
        getSupportFragmentManager().setFragmentResultListener(MyQrCodeBottomSheet.DISMISSED, this,
                (key, result) -> binding.getRoot().post(() -> { if (alive()) { refreshPaused(); } }));
        model.profileError().observe(this, message -> { if (message != null) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); } });
        model.state().observe(this, this::renderFound);
    }
    private boolean hasCameraPermission() { return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED; }
    private void requestCameraPermission() { requestedPermission = true; permissionLauncher.launch(Manifest.permission.CAMERA); }
    private void showPermissionState() {
        binding.qrInstruction.setText(R.string.qr_camera_permission); binding.btnQrRetry.setText(R.string.qr_camera_settings); binding.btnQrRetry.setVisibility(View.VISIBLE);
    }
    @Override protected void onSaveInstanceState(@NonNull Bundle out) { out.putBoolean("qr_permission_requested", requestedPermission); super.onSaveInstanceState(out); }
    @Override protected void onStart() {
        super.onStart(); foreground = true;
        if (hasCameraPermission()) { startCamera(); }
        else { showPermissionState(); if (!requestedPermission) { requestCameraPermission(); } }
        renderFound(model.state().getValue()); refreshPaused();
    }
    @Override protected void onStop() { foreground = false; scanningPaused = true; stopCamera(); super.onStop(); }
    private void refreshPaused() {
        scanningPaused = !foreground || pickingImage || readingImage || model.state().getValue() != null
                || isMyCodeShown();
        binding.btnQrPickGallery.setEnabled(!pickingImage && !readingImage);
    }
    private boolean isMyCodeShown() {
        var fragment = getSupportFragmentManager().findFragmentByTag("MyQrCode");
        return fragment instanceof MyQrCodeBottomSheet sheet
                && sheet.getDialog() != null && sheet.getDialog().isShowing();
    }
    private void startCamera() {
        if (!alive() || !foreground || !hasCameraPermission() || analysis != null) { return; }
        var future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            if (!alive() || !foreground || analysis != null || !hasCameraPermission()) { return; }
            try {
                provider = future.get(); preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.qrCameraPreview.getSurfaceProvider());
                analysis = new ImageAnalysis.Builder().setTargetResolution(new android.util.Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                analysis.setAnalyzer(cameraExecutor, new QrCodeAnalyzer());
                camera = provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis);
                binding.btnQrFlash.setEnabled(camera.getCameraInfo().hasFlashUnit()); binding.qrInstruction.setText(R.string.qr_scan_hint);
                binding.btnQrRetry.setVisibility(View.GONE); refreshPaused();
            } catch (Exception ignored) {
                stopCamera(); binding.qrInstruction.setText(R.string.moment_camera_error);
                binding.btnQrRetry.setText(R.string.qr_retry); binding.btnQrRetry.setVisibility(View.VISIBLE);
            }
        }, ContextCompat.getMainExecutor(this));
    }
    private void stopCamera() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) { camera.getCameraControl().enableTorch(false); }
        flashOn = false; camera = null;
        if (analysis != null) { analysis.clearAnalyzer(); }
        if (provider != null) {
            if (analysis != null) { provider.unbind(analysis); }
            if (preview != null) { provider.unbind(preview); }
        }
        analysis = null; preview = null;
        if (binding != null) { binding.btnQrFlash.setEnabled(false); binding.btnQrFlash.setAlpha(.6f); }
    }
    private final class QrCodeAnalyzer implements ImageAnalysis.Analyzer {
        private final QRCodeReader reader = new QRCodeReader();
        @Override public void analyze(@NonNull ImageProxy image) {
            try {
                if (scanningPaused) { return; }
                var plane = image.getPlanes()[0];
                var frame = QrLuminance.unpack(plane.getBuffer(), image.getWidth(), image.getHeight(), plane.getRowStride(), plane.getPixelStride(), image.getImageInfo().getRotationDegrees());
                var source = new PlanarYUVLuminanceSource(frame.data(), frame.width(), frame.height(), 0, 0, frame.width(), frame.height(), false);
                String text;
                try { text = reader.decode(new BinaryBitmap(new HybridBinarizer(source)), Map.of(DecodeHintType.TRY_HARDER, true)).getText(); }
                catch (com.google.zxing.NotFoundException ignored) {
                    reader.reset(); text = reader.decode(new BinaryBitmap(new HybridBinarizer(source.invert())), Map.of(DecodeHintType.TRY_HARDER, true)).getText();
                }
                if (text != null) {
                    scanningPaused = true; String payload = text;
                    runOnUiThread(() -> { if (alive() && foreground && !pickingImage && !readingImage && model.state().getValue() == null) { onQrCodeDetected(payload); } });
                }
            } catch (Exception ignored) { /* Khung hình không có QR không phải lỗi camera. */ }
            finally { reader.reset(); image.close(); }
        }
    }
    private void onGalleryImagePicked(@Nullable Uri uri) {
        pickingImage = false;
        if (!alive() || uri == null) { if (alive()) { refreshPaused(); } return; }
        readingImage = true; refreshPaused();
        galleryExecutor.execute(() -> {
            Bitmap bitmap = null; String decoded = null;
            try { bitmap = QrImageDecoder.read(getContentResolver(), uri); decoded = QrCodeUtils.decodeFromBitmap(bitmap); }
            catch (Exception ignored) { }
            finally { if (bitmap != null) { bitmap.recycle(); } }
            String result = decoded;
            runOnUiThread(() -> {
                if (!alive()) { return; }
                readingImage = false;
                if (foreground && result != null) { onQrCodeDetected(result); }
                else { Toast.makeText(this, R.string.qr_not_found_in_image, Toast.LENGTH_SHORT).show(); refreshPaused(); }
            });
        });
    }
    private void onQrCodeDetected(String raw) {
        if (!alive() || !foreground || model.state().getValue() != null
                || isMyCodeShown()) { return; }
        var payload = QrCodeUtils.parsePayload(raw);
        if (payload == null) {
            Toast.makeText(this, R.string.qr_invalid, Toast.LENGTH_SHORT).show();
            binding.getRoot().postDelayed(() -> { if (alive()) { refreshPaused(); } }, 1500); return;
        }
        binding.getRoot().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); scanningPaused = true; model.resolve(payload);
    }
    private void renderFound(QrScanViewModel.State state) {
        if (!alive() || !foreground || state == null) { return; }
        if (state.status() == QrScanViewModel.Status.SENT) { Toast.makeText(this, R.string.qr_request_sent_success, Toast.LENGTH_LONG).show(); finish(); return; }
        if (foundDialog == null) {
            foundDialog = new BottomSheetDialog(this); foundBinding = DialogQrFriendFoundBinding.inflate(getLayoutInflater()); foundDialog.setContentView(foundBinding.getRoot());
            foundBinding.btnFoundCancel.setOnClickListener(view -> foundDialog.dismiss());
            foundDialog.setOnDismissListener(dialog -> { foundDialog = null; foundBinding = null; if (alive()) { model.reset(); refreshPaused(); } });
            foundDialog.show();
        }
        foundBinding.foundUsername.setText("@" + state.payload().username);
        foundBinding.foundDisplayName.setText(state.target() == null ? state.payload().username : state.target().displayName);
        int message = switch (state.status()) {
            case CHECKING -> R.string.qr_checking_identity;
            case READY -> R.string.qr_found_ready;
            case SELF -> R.string.qr_is_myself;
            case FRIEND -> R.string.qr_already_friends;
            case NOT_FOUND -> R.string.qr_identity_not_found;
            case CONFLICT -> R.string.friends_conflict;
            case SENDING -> R.string.qr_sending_request;
            default -> R.string.qr_lookup_failed;
        };
        foundBinding.foundStatus.setText(message);
        boolean retry = state.status() == QrScanViewModel.Status.ERROR;
        foundBinding.btnFoundAddFriend.setText(retry ? R.string.qr_retry : R.string.qr_btn_send_request);
        foundBinding.btnFoundAddFriend.setEnabled(retry || state.status() == QrScanViewModel.Status.READY);
        foundBinding.btnFoundAddFriend.setVisibility(state.status() == QrScanViewModel.Status.SELF ? View.GONE : View.VISIBLE);
        foundBinding.btnFoundAddFriend.setOnClickListener(view -> { if (retry) { model.resolve(state.payload()); } else { model.send(); } });
    }
    @Override protected void onDestroy() {
        if (foundDialog != null) { foundDialog.setOnDismissListener(null); foundDialog.dismiss(); foundDialog = null; foundBinding = null; }
        stopCamera(); cameraExecutor.shutdownNow(); galleryExecutor.shutdownNow(); binding = null; super.onDestroy();
    }
}
