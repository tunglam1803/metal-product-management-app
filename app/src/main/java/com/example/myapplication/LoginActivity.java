package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.example.myapplication.helpers.PreferencesHelper;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;
import java.util.concurrent.Executor;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private View btnBiometricLogin;
    private FirebaseAuth auth;
    private PreferencesHelper preferencesHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferencesHelper tempPref = new PreferencesHelper(this);
        if (tempPref.isDarkMode()) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnBiometricLogin = findViewById(R.id.btnBiometricLogin);

        auth = FirebaseAuth.getInstance();
        preferencesHelper = new PreferencesHelper(this);

        // Auto-navigate if already logged in with active Firebase session
        if (preferencesHelper.isLoggedIn() && auth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        btnLogin.setOnClickListener(v -> login());

        // Setup biometric login button
        setupBiometricLogin();
    }

    private void setupBiometricLogin() {
        String savedEmail = preferencesHelper.getSavedEmail();
        String savedPassword = preferencesHelper.getPassword();
        boolean biometricsEnabled = preferencesHelper.isBiometricsEnabled();

        if (biometricsEnabled && !savedEmail.isEmpty() && !savedPassword.isEmpty()) {
            // Show biometric button and pre-fill email
            btnBiometricLogin.setVisibility(View.VISIBLE);
            etEmail.setText(savedEmail);

            // Set click listener on biometric button
            btnBiometricLogin.setOnClickListener(v -> showBiometricPrompt());

            // Auto-trigger biometric prompt on launch
            showBiometricPrompt();
        } else {
            btnBiometricLogin.setVisibility(View.GONE);
        }
    }

    private void showBiometricPrompt() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

        if (biometricManager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Thiết bị không hỗ trợ sinh trắc học hoặc chưa đăng ký", Toast.LENGTH_SHORT).show();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        // User cancelled or error — just let them type manually
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED
                                && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                && errorCode != BiometricPrompt.ERROR_CANCELED) {
                            Toast.makeText(LoginActivity.this, "Lỗi sinh trắc: " + errString, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        performBiometricSignIn();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(LoginActivity.this, "Xác thực thất bại, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Đăng nhập sinh trắc học")
                .setSubtitle("Quét vân tay hoặc khuôn mặt để đăng nhập nhanh")
                .setAllowedAuthenticators(authenticators)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void performBiometricSignIn() {
        String savedEmail = preferencesHelper.getSavedEmail();
        String savedPassword = preferencesHelper.getPassword();

        if (savedEmail.isEmpty() || savedPassword.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin đăng nhập đã lưu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        btnLogin.setEnabled(false);
        btnLogin.setText("Đang xác thực...");
        btnBiometricLogin.setEnabled(false);
        etEmail.setEnabled(false);
        etPassword.setEnabled(false);

        auth.signInWithEmailAndPassword(savedEmail, savedPassword)
                .addOnSuccessListener(authResult -> {
                    String userId = Objects.requireNonNull(authResult.getUser()).getUid();
                    preferencesHelper.saveLogin(userId, savedEmail, savedPassword);
                    Toast.makeText(LoginActivity.this, "Đăng nhập sinh trắc thành công! 🔓", Toast.LENGTH_SHORT).show();

                    // Bypass biometric lock on MainActivity
                    MainApplication.isJustLoggedIn = true;

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Credentials may have changed on server — let user type manually
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Đăng Nhập");
                    btnBiometricLogin.setEnabled(true);
                    etEmail.setEnabled(true);
                    etPassword.setEnabled(true);
                    Toast.makeText(LoginActivity.this,
                            "Mật khẩu đã thay đổi. Vui lòng đăng nhập thủ công.",
                            Toast.LENGTH_LONG).show();
                });
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Nhập email");
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Nhập mật khẩu");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Đang đăng nhập...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String userId = Objects.requireNonNull(authResult.getUser()).getUid();
                    preferencesHelper.saveLogin(userId, email, password);
                    Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();

                    // Set flag to bypass biometric lock prompt immediately after typing credentials
                    MainApplication.isJustLoggedIn = true;

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Đăng Nhập");
                    Toast.makeText(LoginActivity.this, "Lỗi đăng nhập: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}

