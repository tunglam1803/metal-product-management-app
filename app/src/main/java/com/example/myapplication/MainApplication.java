package com.example.myapplication;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import com.example.myapplication.helpers.PreferencesHelper;

public class MainApplication extends Application {
    private int startedActivities = 0;
    private boolean wasInBackground = true; // Start with true to trigger lock on cold launch
    private boolean isAuthenticating = false;
    
    // Flag to bypass biometric prompt when user has just logged in using credentials
    public static boolean isJustLoggedIn = false;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize and apply Dark Mode configuration process-wide right at startup
        PreferencesHelper prefHelper = new PreferencesHelper(this);
        if (prefHelper.isDarkMode()) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                // Ensure dark mode persists on activity creation
                if (prefHelper.isDarkMode()) {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                }
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                startedActivities++;
                if (wasInBackground) {
                    wasInBackground = false;
                    
                    // Trigger biometrics lock if enabled, user is logged in, and they didn't just log in
                    if (prefHelper.isBiometricsEnabled() && !(activity instanceof LoginActivity)) {
                        if (prefHelper.isLoggedIn() && !isJustLoggedIn) {
                            promptBiometricLock(activity);
                        }
                    }
                }
                
                // Clear the bypass flag once the MainActivity starts up
                if (activity instanceof MainActivity) {
                    isJustLoggedIn = false;
                }
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {}

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                startedActivities--;
                if (startedActivities == 0) {
                    wasInBackground = true;
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {}
        });
    }

    private void promptBiometricLock(Activity activity) {
        if (!(activity instanceof FragmentActivity)) {
            return;
        }

        final FragmentActivity fragmentActivity = (FragmentActivity) activity;
        final View contentRoot = fragmentActivity.findViewById(android.R.id.content);
        
        // Protect user privacy by hiding UI before successful authentication
        if (contentRoot != null) {
            contentRoot.setVisibility(View.INVISIBLE);
        }

        androidx.biometric.BiometricManager biometricManager = androidx.biometric.BiometricManager.from(fragmentActivity);
        int authenticators = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG | androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;

        if (biometricManager.canAuthenticate(authenticators) != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            if (contentRoot != null) {
                contentRoot.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (isAuthenticating) {
            return;
        }
        isAuthenticating = true;

        java.util.concurrent.Executor executor = ContextCompat.getMainExecutor(fragmentActivity);
        androidx.biometric.BiometricPrompt biometricPrompt = new androidx.biometric.BiometricPrompt(fragmentActivity, executor,
            new androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    isAuthenticating = false;
                    Toast.makeText(fragmentActivity, "Bảo mật: " + errString, Toast.LENGTH_LONG).show();
                    // Terminate all activities in the task if user cancels or authentication fails
                    fragmentActivity.finishAffinity();
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull androidx.biometric.BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    isAuthenticating = false;
                    if (contentRoot != null) {
                        contentRoot.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(fragmentActivity, "Xác thực bảo mật thành công!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                }
            });

        androidx.biometric.BiometricPrompt.PromptInfo promptInfo = new androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Xác thực ứng dụng")
            .setSubtitle("Vui lòng quét vân tay hoặc khuôn mặt để tiếp tục truy cập")
            .setAllowedAuthenticators(authenticators)
            .build();

        biometricPrompt.authenticate(promptInfo);
    }
}
