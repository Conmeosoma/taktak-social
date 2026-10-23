package com.taktak.app.ui.splash;

import android.app.NotificationManager;
import android.content.*;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.*;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.taktak.app.R;
import com.taktak.app.databinding.ViewBrandedIntroBinding;

/** Một player/Surface cho cả hình và tiếng của MP4 local. Chỉ dùng trên main thread. */
public final class BrandedIntro implements TextureView.SurfaceTextureListener {
    private final AppCompatActivity activity;
    private final ViewBrandedIntroBinding binding;
    private final IntroPreferences preferences;
    private final AudioManager audio;
    private final boolean otherAudioAtEntry;
    private Runnable completed;
    private MediaPlayer player;
    private Surface surface;
    private AudioFocusRequest focus;
    private boolean done, prepared, started, registered, soundRequested, soundAllowed;
    private final Runnable watchdog = this::finish;
    private final BroadcastReceiver quietMode = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            var notifications = activity.getSystemService(NotificationManager.class);
            if (audio == null || audio.getRingerMode() != AudioManager.RINGER_MODE_NORMAL
                    || notifications == null || notifications.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_ALL) mute();
        }
    };

    public BrandedIntro(AppCompatActivity activity, ViewGroup parent, Runnable completed) {
        this.activity = activity;
        this.completed = completed;
        preferences = new IntroPreferences(activity);
        soundRequested = preferences.sound();
        audio = activity.getSystemService(AudioManager.class);
        otherAudioAtEntry = audio != null && audio.isMusicActive();
        binding = ViewBrandedIntroBinding.inflate(activity.getLayoutInflater(), parent, false);
        parent.addView(binding.getRoot());
        binding.introVideo.setOpaque(false);
        binding.introSkip.setOnClickListener(v -> finish());
        binding.introSound.setOnClickListener(v -> {
            soundRequested = !soundRequested;
            preferences.sound(soundRequested);
            if (prepared) configureSound();
            label();
        });
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            var safe = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()
                    | androidx.core.view.WindowInsetsCompat.Type.displayCutout());
            v.setPadding(safe.left, safe.top, safe.right, safe.bottom); return insets;
        });
        label();
        binding.introVideo.setSurfaceTextureListener(this);
        // Watchdog chỉ cho prepare/surface lỗi, không phải trì hoãn navigation.
        binding.getRoot().postDelayed(watchdog, 5000);
    }

    private void label() { binding.introSound.setText(soundRequested ? R.string.intro_sound_on : R.string.intro_sound_off); }
    @Override public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
        if (done || player != null) return;
        try {
            surface = new Surface(texture);
            MediaPlayer next = new MediaPlayer(); player = next;
            next.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            next.setVolume(0, 0);
            next.setSurface(surface);
            try (var descriptor = activity.getResources().openRawResourceFd(R.raw.taktak_intro)) {
                next.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            }
            next.setLooping(false);
            next.setOnErrorListener((p, what, extra) -> { finish(); return true; });
            next.setOnCompletionListener(p -> finish());
            next.setOnVideoSizeChangedListener((p, w, h) -> fit(w, h));
            next.setOnPreparedListener(p -> {
                if (done || player != p) return;
                prepared = true;
                try {
                    fit(p.getVideoWidth(), p.getVideoHeight());
                    if (activity.hasWindowFocus()) startPlayback();
                } catch (RuntimeException failure) { finish(); }
            });
            next.prepareAsync();
        } catch (Exception failure) { finish(); }
    }

    private void startPlayback() {
        if (done || !prepared || started || player == null) return;
        try {
            configureSound();
            player.start(); started = true;
            binding.getRoot().removeCallbacks(watchdog);
        } catch (RuntimeException failure) { finish(); }
    }

    public void windowFocusChanged(boolean focused) {
        if (focused) startPlayback();
        else if (started) mute();
    }

    private void fit(int videoWidth, int videoHeight) {
        int width = binding.introVideo.getWidth(), height = binding.introVideo.getHeight();
        float scale = IntroPolicy.fitScale(width, height, videoWidth, videoHeight);
        if (scale == 0) return;
        Matrix transform = new Matrix();
        transform.setScale(videoWidth * scale / width, videoHeight * scale / height, width / 2f, height / 2f);
        binding.introVideo.setTransform(transform);
    }

    private void configureSound() {
        mute();
        if (done || !prepared || audio == null) return;
        var notifications = activity.getSystemService(NotificationManager.class);
        boolean allowed = IntroPolicy.audible(soundRequested, activity.hasWindowFocus(),
                audio.getRingerMode() == AudioManager.RINGER_MODE_NORMAL,
                notifications != null && notifications.getCurrentInterruptionFilter() == NotificationManager.INTERRUPTION_FILTER_ALL,
                otherAudioAtEntry || (!isPlaying() && audio.isMusicActive()), audio.isStreamMute(AudioManager.STREAM_MUSIC)
                        || audio.getStreamVolume(AudioManager.STREAM_MUSIC) == 0);
        if (!allowed) return;
        try {
            focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener(change -> { if (change != AudioManager.AUDIOFOCUS_GAIN) mute(); },
                            new Handler(Looper.getMainLooper())).build();
            if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { abandonFocus(); return; }
            if (!registered) {
                IntentFilter filter = new IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION);
                filter.addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED);
                ContextCompat.registerReceiver(activity, quietMode, filter, ContextCompat.RECEIVER_EXPORTED);
                registered = true;
            }
            // Âm lượng cục bộ; không thay volume hệ thống.
            player.setVolume(.55f, .55f); soundAllowed = true;
        } catch (RuntimeException failure) { mute(); }
    }

    private void abandonFocus() {
        if (focus != null && audio != null) { audio.abandonAudioFocusRequest(focus); focus = null; }
    }
    private void mute() {
        soundAllowed = false;
        if (player != null) { try { player.setVolume(0, 0); } catch (IllegalStateException ignored) { } }
        abandonFocus();
    }
    public boolean isPlaying() { return !done && prepared && player != null && player.isPlaying(); }
    public boolean isSoundAllowed() { return soundAllowed; }
    public void finish() {
        if (done) return;
        done = true;
        binding.getRoot().removeCallbacks(watchdog);
        mute();
        if (registered) { activity.unregisterReceiver(quietMode); registered = false; }
        if (player != null) {
            player.setOnPreparedListener(null); player.setOnCompletionListener(null);
            player.setOnErrorListener(null); player.setOnVideoSizeChangedListener(null);
            player.release(); player = null;
        }
        binding.introVideo.setSurfaceTextureListener(null);
        if (surface != null) { surface.release(); surface = null; }
        if (binding.getRoot().getParent() instanceof ViewGroup parent) parent.removeView(binding.getRoot());
        Runnable callback = completed; completed = null;
        if (callback != null) callback.run();
    }
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
        if (prepared && player != null) fit(player.getVideoWidth(), player.getVideoHeight());
    }
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) { finish(); return true; }
    @Override public void onSurfaceTextureUpdated(SurfaceTexture texture) { }
}
