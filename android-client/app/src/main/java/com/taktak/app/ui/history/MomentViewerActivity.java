package com.taktak.app.ui.history;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MomentViewerActivity extends AppCompatActivity {

    public static final String EXTRA_MOMENT_ID = "extra_moment_id";
    public static final String EXTRA_SENDER_NAME = "extra_sender_name";
    public static final String EXTRA_SENDER_USERNAME = "extra_sender_username";
    public static final String EXTRA_CAPTION = "extra_caption";
    public static final String EXTRA_CREATED_AT = "extra_created_at";
    public static final String EXTRA_IS_SENDER = "extra_is_sender";

    private ActivityMomentViewerBinding binding;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private String momentId;
    private boolean isSender;

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
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMomentViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (view, insets) -> {
            var safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });

        momentId = getIntent().getStringExtra(EXTRA_MOMENT_ID);
        String senderName = getIntent().getStringExtra(EXTRA_SENDER_NAME);
        String senderUsername = getIntent().getStringExtra(EXTRA_SENDER_USERNAME);
        String caption = getIntent().getStringExtra(EXTRA_CAPTION);
        String createdAt = getIntent().getStringExtra(EXTRA_CREATED_AT);
        isSender = getIntent().getBooleanExtra(EXTRA_IS_SENDER, false);

        binding.viewerSenderName.setText(senderName != null ? senderName : "");
        binding.viewerSenderUsername.setText(senderUsername != null
                ? getString(R.string.friends_username, senderUsername) : "");
        binding.viewerTimestamp.setText(HistoryTimeFormatter.format(this, createdAt));

        Glide.with(this)
                .load(R.drawable.friend_avatar)
                .circleCrop()
                .into(binding.viewerSenderAvatar);

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

        loadImage();
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
        if (momentId == null) { return; }
        binding.viewerSaveButton.setEnabled(false);
        Toast.makeText(this, R.string.moment_saving, Toast.LENGTH_SHORT).show();

        worker.execute(() -> {
            try {
                byte[] bytes = null;
                try {
                    var repository = AuthGraph.repository(getApplication()).moments();
                    bytes = repository.download(momentId);
                } catch (Exception networkError) {
                    // Fallback to cached bitmap if offline
                    Bitmap cached = HistoryImageCache.get(momentId);
                    if (cached != null) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        if (cached.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                            bytes = stream.toByteArray();
                        }
                    }
                }

                if (bytes == null || bytes.length == 0) {
                    throw new IOException("Unable to obtain image bytes to save");
                }

                GallerySaver.saveJpeg(getApplicationContext(), bytes, "taktak_moment");

                main.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        binding.viewerSaveButton.setEnabled(true);
                        Toast.makeText(MomentViewerActivity.this, R.string.moment_saved_to_gallery, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception failure) {
                android.util.Log.e("TaktakQa", "Failed to save moment to gallery: " + momentId, failure);
                main.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        binding.viewerSaveButton.setEnabled(true);
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
        if (momentId == null) { return; }
        binding.viewerDeleteButton.setEnabled(false);
        showLoading();
        worker.execute(() -> {
            try {
                var repository = AuthGraph.repository(getApplication()).moments();
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
                android.util.Log.e("TaktakQa", "Failed to delete moment: " + momentId, failure);
                int errorResId;
                if (failure instanceof ApiFailure apiFailure && apiFailure.kind == ApiFailure.Kind.NETWORK) {
                    errorResId = R.string.moment_delete_network_error;
                } else if (failure instanceof IOException) {
                    errorResId = R.string.moment_delete_network_error;
                } else {
                    errorResId = R.string.moment_delete_generic_error;
                }
                main.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        binding.viewerDeleteButton.setEnabled(true);
                        Bitmap cached = HistoryImageCache.get(momentId);
                        if (cached != null) {
                            showBitmap(cached);
                        } else {
                            showError();
                        }
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
        if (momentId == null) {
            showError();
            return;
        }

        Bitmap cached = HistoryImageCache.get(momentId);
        if (cached != null) {
            showBitmap(cached);
            return;
        }

        showLoading();
        worker.execute(() -> {
            try {
                var repository = AuthGraph.repository(getApplication()).moments();
                byte[] bytes = repository.download(momentId);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap == null) {
                    throw new IOException("Failed to decode downloaded JPEG");
                }
                HistoryImageCache.put(momentId, bitmap);
                main.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        showBitmap(bitmap);
                    }
                });
            } catch (Exception failure) {
                android.util.Log.e("TaktakQa", "Failed to download image in viewer: " + momentId, failure);
                main.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        showError();
                    }
                });
            }
        });
    }

    private void showLoading() {
        binding.viewerProgress.setVisibility(View.VISIBLE);
        binding.viewerErrorView.setVisibility(View.GONE);
        binding.viewerImage.setImageDrawable(null);
    }

    private void showBitmap(@NonNull Bitmap bitmap) {
        binding.viewerProgress.setVisibility(View.GONE);
        binding.viewerErrorView.setVisibility(View.GONE);
        binding.viewerImage.setImageBitmap(bitmap);
    }

    private void showError() {
        binding.viewerProgress.setVisibility(View.GONE);
        binding.viewerErrorView.setVisibility(View.VISIBLE);
        binding.viewerImage.setImageDrawable(null);
    }

    @Override
    protected void onDestroy() {
        main.removeCallbacksAndMessages(null);
        worker.shutdownNow();
        if (binding != null) {
            binding.viewerImage.setImageDrawable(null);
            Glide.with(this).clear(binding.viewerSenderAvatar);
            binding = null;
        }
        super.onDestroy();
    }
}
