package com.example.myapplication.helpers;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesHelper {
    private static final String PREF_NAME = "ProductAppPrefs";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_PASSWORD = "userPassword";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_DARK_MODE = "isDarkMode";
    private static final String KEY_BIOMETRICS = "isBiometrics";
    private static final String KEY_NOTIFICATIONS = "isNotifications";
    private final SharedPreferences sharedPreferences;

    public PreferencesHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveLogin(String userId, String email, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_PASSWORD, password);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, "");
    }

    public String getSavedEmail() {
        return sharedPreferences.getString(KEY_USER_EMAIL, "");
    }

    public String getPassword() {
        return sharedPreferences.getString(KEY_USER_PASSWORD, "");
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USER_ID);
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        // Keep email & password if biometrics is enabled so user can biometric-login from login screen
        if (!isBiometricsEnabled()) {
            editor.remove(KEY_USER_EMAIL);
            editor.remove(KEY_USER_PASSWORD);
        }
        editor.apply();
    }

    public void setDarkMode(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_DARK_MODE, enabled);
        editor.apply();
    }

    public boolean isDarkMode() {
        return sharedPreferences.getBoolean(KEY_DARK_MODE, false);
    }

    public void setBiometricsEnabled(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_BIOMETRICS, enabled);
        // Clear cached password when biometrics is disabled for security
        if (!enabled) {
            editor.remove(KEY_USER_PASSWORD);
        }
        editor.apply();
    }

    public boolean isBiometricsEnabled() {
        return sharedPreferences.getBoolean(KEY_BIOMETRICS, false);
    }

    public void setNotificationsEnabled(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_NOTIFICATIONS, enabled);
        editor.apply();
    }

    public boolean isNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true);
    }
}
