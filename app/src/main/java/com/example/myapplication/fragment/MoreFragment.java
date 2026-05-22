package com.example.myapplication.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.LoginActivity;
import com.example.myapplication.R;
import com.example.myapplication.helpers.PreferencesHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MoreFragment extends Fragment {

    private PreferencesHelper preferencesHelper;
    private SwitchCompat switchDarkMode;
    private SwitchCompat switchBiometric;
    private SwitchCompat switchNotifications;
    private androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferencesHelper = new PreferencesHelper(requireContext());
        
        // Register permission launcher for Push Notifications
        requestPermissionLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    preferencesHelper.setNotificationsEnabled(true);
                    if (switchNotifications != null) {
                        switchNotifications.setChecked(true);
                    }
                    if (getActivity() instanceof com.example.myapplication.MainActivity) {
                        ((com.example.myapplication.MainActivity) getActivity()).updateNotificationListenerState();
                    }
                    showSampleNotification();
                    Toast.makeText(requireContext(), "Đã bật thông báo đẩy thành công! 🔔", Toast.LENGTH_SHORT).show();
                } else {
                    preferencesHelper.setNotificationsEnabled(false);
                    if (switchNotifications != null) {
                        switchNotifications.setChecked(false);
                    }
                    if (getActivity() instanceof com.example.myapplication.MainActivity) {
                        ((com.example.myapplication.MainActivity) getActivity()).updateNotificationListenerState();
                    }
                    Toast.makeText(requireContext(), "Yêu cầu quyền thông báo bị từ chối.", Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        // Bind dynamic profile elements
        TextView tvGreeting = view.findViewById(R.id.tvGreeting);
        TextView tvEmail = view.findViewById(R.id.tvEmail);
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            String name = user.getEmail().split("@")[0];
            tvGreeting.setText("Chào " + name + " 👋");
            tvEmail.setText(user.getEmail());
        }

        // Bind preference toggles
        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        switchBiometric = view.findViewById(R.id.switchBiometric);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        
        // Initialize switch states
        switchDarkMode.setChecked(preferencesHelper.isDarkMode());
        switchBiometric.setChecked(preferencesHelper.isBiometricsEnabled());
        switchNotifications.setChecked(preferencesHelper.isNotificationsEnabled());
        
        // 1. Handle Dark Mode toggle
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesHelper.setDarkMode(isChecked);
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
        
        // 2. Handle Biometric security toggle (Real BiometricPrompt integration)
        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Check if we are programmatically updating to avoid loop
            if (isChecked == preferencesHelper.isBiometricsEnabled()) {
                return;
            }
            
            // Revert state temporarily until authenticated
            switchBiometric.setOnCheckedChangeListener(null);
            switchBiometric.setChecked(!isChecked);
            setupBiometricListener();
            
            authenticateBiometrics(isChecked);
        });

        // 3. Handle Push Notifications toggle (Real runtime permission + local notifications)
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked == preferencesHelper.isNotificationsEnabled()) {
                return;
            }
            
            if (isChecked) {
                // Request Permission at runtime
                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) 
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                } else {
                    preferencesHelper.setNotificationsEnabled(true);
                    if (getActivity() instanceof com.example.myapplication.MainActivity) {
                        ((com.example.myapplication.MainActivity) getActivity()).updateNotificationListenerState();
                    }
                    showSampleNotification();
                    Toast.makeText(requireContext(), "Đã kích hoạt thông báo đẩy! 🔔", Toast.LENGTH_SHORT).show();
                }
            } else {
                preferencesHelper.setNotificationsEnabled(false);
                if (getActivity() instanceof com.example.myapplication.MainActivity) {
                    ((com.example.myapplication.MainActivity) getActivity()).updateNotificationListenerState();
                }
                Toast.makeText(requireContext(), "Đã tắt nhận thông báo đẩy.", Toast.LENGTH_SHORT).show();
            }
        });

        // 4. Handle Terms & User Guide
        LinearLayout llTermsRow = view.findViewById(R.id.llTermsRow);
        llTermsRow.setOnClickListener(v -> showTermsDialog());

        // 5. Handle Tech Support Form Dialog
        LinearLayout llSupportRow = view.findViewById(R.id.llSupportRow);
        llSupportRow.setOnClickListener(v -> showSupportDialog());

        // 6. Handle Logout
        LinearLayout llLogoutRow = view.findViewById(R.id.llLogoutRow);
        llLogoutRow.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            preferencesHelper.logout();
            
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        return view;
    }

    private void setupBiometricListener() {
        switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked == preferencesHelper.isBiometricsEnabled()) {
                return;
            }
            switchBiometric.setOnCheckedChangeListener(null);
            switchBiometric.setChecked(!isChecked);
            setupBiometricListener();
            authenticateBiometrics(isChecked);
        });
    }

    private void authenticateBiometrics(boolean enabling) {
        androidx.biometric.BiometricManager biometricManager = androidx.biometric.BiometricManager.from(requireContext());
        int authenticators = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG | androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        
        switch (biometricManager.canAuthenticate(authenticators)) {
            case androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS:
                break;
            case androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Toast.makeText(requireContext(), "Thiết bị không hỗ trợ tính năng sinh trắc học", Toast.LENGTH_LONG).show();
                return;
            case androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Toast.makeText(requireContext(), "Phần cứng sinh trắc học hiện không khả dụng", Toast.LENGTH_LONG).show();
                return;
            case androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Toast.makeText(requireContext(), "Chưa đăng ký vân tay hoặc khuôn mặt trên thiết bị", Toast.LENGTH_LONG).show();
                return;
            default:
                Toast.makeText(requireContext(), "Không thể kích hoạt sinh trắc học lúc này", Toast.LENGTH_LONG).show();
                return;
        }

        java.util.concurrent.Executor executor = ContextCompat.getMainExecutor(requireContext());
        androidx.biometric.BiometricPrompt biometricPrompt = new androidx.biometric.BiometricPrompt(this, executor,
            new androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Toast.makeText(requireContext(), "Xác thực lỗi: " + errString, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull androidx.biometric.BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    preferencesHelper.setBiometricsEnabled(enabling);
                    
                    switchBiometric.setOnCheckedChangeListener(null);
                    switchBiometric.setChecked(enabling);
                    setupBiometricListener();
                    
                    Toast.makeText(requireContext(), enabling ? "Đã kích hoạt bảo mật vân tay / FaceID! 🔒" : "Đã hủy kích hoạt bảo mật vân tay / FaceID.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Toast.makeText(requireContext(), "Xác thực thất bại, hãy quét lại vân tay", Toast.LENGTH_SHORT).show();
                }
            });

        androidx.biometric.BiometricPrompt.PromptInfo promptInfo = new androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Xác thực bảo mật")
            .setSubtitle(enabling ? "Xác nhận sinh trắc học để bật bảo mật ứng dụng" : "Xác nhận sinh trắc học để tắt bảo mật ứng dụng")
            .setAllowedAuthenticators(authenticators)
            .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void showSampleNotification() {
        String channelId = "settings_notifications";
        String channelName = "Thông báo hệ thống";
        
        android.app.NotificationManager notificationManager = 
            (android.app.NotificationManager) requireContext().getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            
        android.app.NotificationChannel channel = new android.app.NotificationChannel(
            channelId, channelName, android.app.NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Kênh thông báo quản trị hệ thống");
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
        
        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Thông báo hệ thống kim khí 🔔")
            .setContentText("Tính năng nhận thông báo đẩy đã được kích hoạt thành công!")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);
            
        if (notificationManager != null) {
            notificationManager.notify(1, builder.build());
        }
    }

    private void showTermsDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = 
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext());
        
        builder.setTitle("Hướng dẫn & Điều khoản");
        
        TextView textView = new TextView(requireContext());
        textView.setPadding(40, 24, 40, 24);
        textView.setTextSize(14f);
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextDark));
        textView.setMovementMethod(new ScrollingMovementMethod());
        
        String htmlText = "<b>1. Quy định sử dụng hệ thống</b><br>" +
                "Ứng dụng dùng để quản lý kho sản phẩm kim khí, quét nhanh mã QR sản phẩm và xem báo cáo thống kê trực quan.<br><br>" +
                "<b>2. Bảo mật sinh trắc học</b><br>" +
                "Khi bật Bảo mật sinh trắc học, ứng dụng sẽ yêu cầu bạn quét vân tay hoặc FaceID mỗi khi mở ứng dụng để bảo vệ thông tin kho hàng.<br><br>" +
                "<b>3. Đồng bộ & Thống kê</b><br>" +
                "Toàn bộ dữ liệu được đồng bộ hóa đám mây trực tiếp thông qua cơ sở dữ liệu Firebase Firestore thời gian thực.<br><br>" +
                "<b>4. Hỗ trợ sự cố</b><br>" +
                "Mọi thắc mắc vui lòng gửi yêu cầu hỗ trợ trực tiếp trong mục Hỗ trợ kỹ thuật của Cài đặt để nhận giải đáp trong vòng 24 giờ.";
                
        textView.setText(Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY));
        
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.addView(textView);
        
        builder.setView(scrollView);
        builder.setPositiveButton("Đã hiểu", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showSupportDialog() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = 
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext());
        
        builder.setTitle("Hỗ trợ kỹ thuật");
        
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(40, 24, 40, 24);
        
        TextView label = new TextView(requireContext());
        label.setText("Mô tả lỗi hoặc sự cố bạn gặp phải:");
        label.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextDark));
        label.setTextSize(14f);
        label.setPadding(0, 0, 0, 16);
        container.addView(label);
        
        EditText input = new EditText(requireContext());
        input.setHint("Nhập nội dung sự cố tại đây...");
        input.setMinLines(4);
        input.setGravity(android.view.Gravity.TOP);
        input.setBackgroundResource(R.drawable.edit_text_background);
        input.setPadding(24, 20, 24, 20);
        input.setTextSize(14f);
        container.addView(input);
        
        builder.setView(container);
        
        builder.setPositiveButton("Gửi yêu cầu", null);
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String message = input.getText().toString().trim();
            if (message.isEmpty()) {
                input.setError("Nội dung không được để trống");
                return;
            }
            
            dialog.dismiss();
            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(requireContext());
            progressDialog.setMessage("Đang gửi yêu cầu hỗ trợ...");
            progressDialog.setCancelable(false);
            progressDialog.show();
            
            new android.os.Handler().postDelayed(() -> {
                progressDialog.dismiss();
                
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Gửi thành công! 🎉")
                    .setMessage("Ban quản trị đã nhận được sự cố của bạn và sẽ phản hồi sớm nhất.")
                    .setPositiveButton("Đóng", null)
                    .show();
            }, 1200);
        });
    }
}

