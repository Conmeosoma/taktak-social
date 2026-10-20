package com.taktak.app.data.security;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import java.util.concurrent.Executor;

public final class BiometricHelper {

    public interface AuthCallback {
        void onSuccess();
        void onError(int errorCode, @NonNull CharSequence errString);
        void onFailed();
    }

    private BiometricHelper() {}

    public static boolean canAuthenticate(@NonNull Context context) {
        BiometricManager manager = BiometricManager.from(context);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static void authenticate(
            @NonNull FragmentActivity activity,
            @NonNull String title,
            @NonNull String subtitle,
            @NonNull AuthCallback callback
    ) {
        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                BiometricPreferences.get(activity).recordUnlock();
                callback.onSuccess();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                callback.onError(errorCode, errString);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                callback.onFailed();
            }
        });

        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(authenticators)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}
