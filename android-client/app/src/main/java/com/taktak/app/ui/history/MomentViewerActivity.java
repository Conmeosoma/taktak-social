package com.taktak.app.ui.history;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.taktak.app.R;
import com.taktak.app.data.AuthGraph;
import com.taktak.app.data.push.PushEventBus;
import com.taktak.app.data.remote.ApiFailure;
import com.taktak.app.databinding.ActivityMomentViewerBinding;
import com.taktak.app.widget.WidgetUpdater;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MomentViewerActivity extends AppCompatActivity {

    public static final String EXTRA_MOMENT_ID = "extra_moment_id";
    public static final String EXTRA_SENDER_NAME = "extra_sender_name";
    public static final String EXTRA_SENDER_ID = "extra_sender_id";
    public static final String EXTRA_SENDER_USERNAME = "extra_sender_username";
    public static final String EXTRA_CAPTION = "extra_caption";
    public static final String EXTRA_CREATED_AT = "extra_created_at";
    public static final String EXTRA_IS_SENDER = "extra_is_sender";

    private ActivityMomentViewerBinding binding;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private String momentId;
    private boolean isSender;
    private String caption;
    private boolean foreground, revoked;
    private int imageRequest;
    private com.taktak.app.data.repository.MomentRepository repository;
    private com.bumptech.glide.RequestManager images;
    private com.taktak.app.ui.profile.AvatarLoader avatars;
    private final PushEventBus.Listener pushListener = new PushEventBus.Listener() {
        public void onMomentCreated(String id) { }
        public void onMomentDeleted(String id) {
            if (id.equals(momentId)) { main.post(() -> { if (foreground && binding != null) { revoke(); } }); }
        }
        public void onSessionCleared() {
            main.post(() -> { if (foreground && binding != null) { revoke(); } });
        }
    };

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    saveImageToGallery();
                } else {
                    new MaterialAlertDialogBuilder(this)
                            .setMessage(R.string.gallery_permission_denied)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        com.taktak.app.ui.theme.ThemeManager.applyBeforeCreate(this,
                new com.taktak.app.ui.theme.ThemePreferences(this).load(),
                com.taktak.app.ui.theme.ThemeManager.ScreenRole.VIEWER);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMomentViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        var viewerBackground = new android.util.TypedValue();
        getTheme().resolveAttribute(R.attr.ttMediaBackground,viewerBackground,true);
        com.taktak.app.ui.theme.ThemeManager.configureBars(this,binding.getRoot(),viewerBackground.data);
        images = Glide.with(this);
        avatars=new com.taktak.app.ui.profile.AvatarLoader(images);
        try { repository = AuthGraph.repository(getApplication()).moments(); }
        catch (IOException failure) { revoked = true; }

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            var safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });

        momentId = getIntent().getStringExtra(EXTRA_MOMENT_ID);
        String senderName = getIntent().getStringExtra(EXTRA_SENDER_NAME);
        String senderUsername = getIntent().getStringExtra(EXTRA_SENDER_USERNAME);
        caption = getIntent().getStringExtra(EXTRA_CAPTION);
        String createdAt = getIntent().getStringExtra(EXTRA_CREATED_AT);
        isSender = getIntent().getBooleanExtra(EXTRA_IS_SENDER, false);

        binding.viewerSenderName.setText(senderName != null ? senderName : "");
        binding.viewerSenderUsername.setText(senderUsername != null
                ? getString(R.string.friends_username, senderUsername) : "");
        binding.viewerTimestamp.setText(HistoryTimeFormatter.format(this, createdAt));

        avatars.load(getIntent().getStringExtra(EXTRA_SENDER_ID),binding.viewerSenderAvatar);

        if (caption != null && !caption.trim().isEmpty()) {
            binding.viewerCaptionCard.setVisibility(View.VISIBLE);
            binding.viewerCaption.setText(caption);
        } else {
            binding.viewerCaptionCard.setVisibility(View.GONE);
        }

        binding.viewerCloseButton.setOnClickListener(v -> finish());
        binding.viewerRetryButton.setOnClickListener(v -> loadImage());

        binding.viewerSaveButton.setOnClickListener(v -> checkPermissionAndSave());

        binding.viewerDeleteButton.setVisibility(isSender ? View.VISIBLE : View.GONE);
        binding.viewerDeleteButton.setOnClickListener(v -> confirmDelete());

        showLoading();
    }

    private void checkPermissionAndSave() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return;
            }
        }
        saveImageToGallery();
    }

    private void saveImageToGallery() {
        if (momentId == null || repository == null || revoked || !foreground) { return; }
        binding.viewerSaveButton.setEnabled(false);
        Toast.makeText(this, R.string.moment_saving, Toast.LENGTH_SHORT).show();

        worker.execute(() -> {
            try {
                byte[] bytes = repository.download(momentId);
                if (!repository.publishIfCurrent(() -> {
                    try { GallerySaver.saveJpeg(getApplicationContext(), bytes, "taktak_moment"); }
                    catch (IOException failure) { throw new java.io.UncheckedIOException(failure); }
                })) { throw new ApiFailure(ApiFailure.Kind.CANCELLED); }

                main.post(() -> {
                    if (foreground && !isFinishing() && !isDestroyed()) {
                        repository.publishIfCurrent(() -> {
                            binding.viewerSaveButton.setEnabled(!revoked);
                            Toast.makeText(MomentViewerActivity.this, R.string.moment_saved_to_gallery, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } catch (Exception failure) {
                main.post(() -> {
                    if (foreground && !isFinishing() && !isDestroyed()) {
                        if (accessDenied(failure)) { revoke(); }
                        binding.viewerSaveButton.setEnabled(!revoked);
                        Toast.makeText(MomentViewerActivity.this, R.string.moment_save_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.moment_delete_confirm_title)
                .setMessage(R.string.moment_delete_confirm_message)
                .setPositiveButton(R.string.moment_delete_action, (dialog, which) -> executeDelete())
                .setNegativeButton(R.string.moment_delete_cancel, null)
                .show();
    }

    private void executeDelete() {
        if (momentId == null || repository == null || revoked || !foreground) { return; }
        binding.viewerDeleteButton.setEnabled(false);
        showLoading();
        worker.execute(() -> {
            try {
                repository.delete(momentId);
                HistoryImageCache.remove(momentId);
                PushEventBus.emitMomentDeleted(momentId);
                WidgetUpdater.enqueueOneTimeWork(getApplicationContext());
                main.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        finish();
                    }
                });
            } catch (Exception failure) {
                int errorResId;
                if (failure instanceof ApiFailure apiFailure && apiFailure.kind == ApiFailure.Kind.NETWORK) {
                    errorResId = R.string.moment_delete_network_error;
                } else if (failure instanceof IOException) {
                    errorResId = R.string.moment_delete_network_error;
                } else {
                    errorResId = R.string.moment_delete_generic_error;
                }
                main.post(() -> {
                    if (foreground && !isFinishing() && !isDestroyed()) {
                        if (accessDenied(failure)) { revoke(); } else { loadImage(); }
                        new MaterialAlertDialogBuilder(MomentViewerActivity.this)
                                .setMessage(errorResId)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                });
            }
        });
    }

    private void loadImage() {
        if (momentId == null || repository == null || revoked) {
            showError();
            return;
        }

        int ticket = ++imageRequest;
        showLoading();
        worker.execute(() -> {
            try {
                byte[] bytes = repository.download(momentId);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap == null) {
                    throw new IOException("Failed to decode downloaded JPEG");
                }
                main.post(() -> {
                    if (foreground && !revoked && ticket == imageRequest && !isFinishing() && !isDestroyed()) {
                        if (!repository.publishIfCurrent(() -> {
                            HistoryImageCache.put(momentId, bitmap); showBitmap(bitmap);
                        })) { revoke(); }
                    }
                });
            } catch (Exception failure) {
                main.post(() -> {
                    if (foreground && ticket == imageRequest && !isFinishing() && !isDestroyed()) {
                        if (accessDenied(failure)) { revoke(); } else { showError(); }
                    }
                });
            }
        });
    }

    private void showLoading() {
        privateVisible(false);
        binding.viewerProgress.setVisibility(View.VISIBLE);
        binding.viewerErrorView.setVisibility(View.GONE);
        binding.viewerImage.setImageDrawable(null);
    }

    private void showBitmap(@NonNull Bitmap bitmap) {
        privateVisible(true);
        binding.viewerProgress.setVisibility(View.GONE);
        binding.viewerErrorView.setVisibility(View.GONE);
        binding.viewerImage.setImageBitmap(bitmap);
    }

    private void showError() {
        privateVisible(false);
        binding.viewerProgress.setVisibility(View.GONE);
        binding.viewerErrorView.setVisibility(View.VISIBLE);
        binding.viewerImage.setImageDrawable(null);
    }

    private void privateVisible(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.INVISIBLE;
        binding.viewerSenderName.setVisibility(visibility);
        binding.viewerSenderUsername.setVisibility(visibility);
        binding.viewerSenderAvatar.setVisibility(visibility);
        binding.viewerTimestamp.setVisibility(visibility);
        binding.viewerCaptionCard.setVisibility(visible && caption != null && !caption.isBlank() ? View.VISIBLE : View.GONE);
        binding.viewerSaveButton.setEnabled(visible && !revoked);
        binding.viewerDeleteButton.setEnabled(visible && !revoked);
    }
    private boolean accessDenied(Exception failure) {
        return failure instanceof ApiFailure api && (api.status == 401 || api.status == 403 || api.status == 404
                || api.kind == ApiFailure.Kind.CANCELLED || api.kind == ApiFailure.Kind.UNAUTHORIZED);
    }
    private void revoke() {
        revoked = true; imageRequest++;
        HistoryImageCache.remove(momentId); showError();
    }
    @Override protected void onStart() {
        super.onStart(); foreground = true; PushEventBus.register(pushListener); loadImage();
    }
    @Override protected void onStop() {
        foreground = false; imageRequest++; PushEventBus.unregister(pushListener);
        if (binding != null) { showLoading(); }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if(avatars!=null) avatars.close();
        main.removeCallbacksAndMessages(null);
        worker.shutdownNow();
        if (binding != null) {
            binding.viewerImage.setImageDrawable(null);
            images.clear(binding.viewerSenderAvatar);
            binding = null;
        }
        super.onDestroy();
    }
}
