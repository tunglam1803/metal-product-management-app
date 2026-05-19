package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 1001;
    private PreviewView previewView;
    private ImageButton btnFlash;
    private View vLaser;
    private Camera camera;
    private boolean isFlashOn = false;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private boolean isScanned = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        previewView = findViewById(R.id.previewView);
        btnFlash = findViewById(R.id.btn_flash);
        vLaser = findViewById(R.id.v_laser);

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize Barcode Scanner
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        btnFlash.setOnClickListener(v -> toggleFlash());

        startLaserAnimation();

        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }

    private void startLaserAnimation() {
        TranslateAnimation animation = new TranslateAnimation(
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.95f);
        animation.setDuration(2500);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);
        vLaser.startAnimation(animation);
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA }, CAMERA_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Bạn cần cấp quyền truy cập Camera để quét mã vạch!", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Lỗi khởi tạo Camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

        cameraProvider.unbindAll();
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    @android.annotation.SuppressLint("UnsafeOptInUsageError")
    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (isScanned) {
            imageProxy.close();
            return;
        }

        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            barcodeScanner.process(image)
                    .addOnSuccessListener(this::handleBarcodes)
                    .addOnFailureListener(e -> imageProxy.close())
                    .addOnCompleteListener(task -> imageProxy.close());
        } else {
            imageProxy.close();
        }
    }

    private void handleBarcodes(List<Barcode> barcodes) {
        if (barcodes != null && !barcodes.isEmpty() && !isScanned) {
            isScanned = true;
            Barcode barcode = barcodes.get(0);
            String rawValue = barcode.getRawValue();

            // Gentle Beep Sound Feedback
            try {
                ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
            } catch (Exception ignored) {
            }

            runOnUiThread(() -> {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("SCANNED_BARCODE", rawValue);
                setResult(RESULT_OK, resultIntent);
                finish();
            });
        }
    }

    private void toggleFlash() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            isFlashOn = !isFlashOn;
            camera.getCameraControl().enableTorch(isFlashOn);
            btnFlash.setColorFilter(isFlashOn ? ContextCompat.getColor(this, android.R.color.holo_green_light)
                    : ContextCompat.getColor(this, android.R.color.white));
        } else {
            Toast.makeText(this, "Thiết bị không hỗ trợ đèn Flash!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
