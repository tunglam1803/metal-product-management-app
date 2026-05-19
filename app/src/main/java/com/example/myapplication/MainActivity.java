package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.myapplication.fragment.DashboardFragment;
import com.example.myapplication.fragment.HomeFragment;
import com.example.myapplication.fragment.MoreFragment;
import com.example.myapplication.fragment.ReportsFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> barcodeScannerLauncher;

    // View declarations for custom tabs
    private View tabDashboard, tabHome, tabReports, tabMore;
    private ImageView ivTabDashboard, ivTabHome, ivTabReports, ivTabMore;
    private TextView tvTabDashboard, tvTabHome, tvTabReports, tvTabMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize Views
        initCustomTabs();

        barcodeScannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String barcode = result.getData().getStringExtra("SCANNED_BARCODE");
                        if (barcode != null && !barcode.isEmpty()) {
                            lookupProductAndNavigate(barcode);
                        }
                    }
                });

        View btnScanGlobal = findViewById(R.id.btnScanGlobal);
        btnScanGlobal.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScannerActivity.class);
            barcodeScannerLauncher.launch(intent);
        });

        // Load default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DashboardFragment())
                    .commit();
            updateTabStyles(R.id.tab_dashboard);
        }
    }

    private void initCustomTabs() {
        tabDashboard = findViewById(R.id.tab_dashboard);
        tabHome = findViewById(R.id.tab_home);
        tabReports = findViewById(R.id.tab_reports);
        tabMore = findViewById(R.id.tab_more);

        ivTabDashboard = findViewById(R.id.iv_tab_dashboard);
        ivTabHome = findViewById(R.id.iv_tab_home);
        ivTabReports = findViewById(R.id.iv_tab_reports);
        ivTabMore = findViewById(R.id.iv_tab_more);

        tvTabDashboard = findViewById(R.id.tv_tab_dashboard);
        tvTabHome = findViewById(R.id.tv_tab_home);
        tvTabReports = findViewById(R.id.tv_tab_reports);
        tvTabMore = findViewById(R.id.tv_tab_more);

        tabDashboard.setOnClickListener(v -> {
            loadFragment(new DashboardFragment());
            updateTabStyles(R.id.tab_dashboard);
        });

        tabHome.setOnClickListener(v -> {
            loadFragment(new HomeFragment());
            updateTabStyles(R.id.tab_home);
        });

        tabReports.setOnClickListener(v -> {
            loadFragment(new ReportsFragment());
            updateTabStyles(R.id.tab_reports);
        });

        tabMore.setOnClickListener(v -> {
            loadFragment(new MoreFragment());
            updateTabStyles(R.id.tab_more);
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void updateTabStyles(int activeTabId) {
        // Reset all to inactive state
        int inactiveColor = ContextCompat.getColor(this, R.color.bottomNavInactive);
        int activeColor = ContextCompat.getColor(this, R.color.bottomNavActive);

        ivTabDashboard.setColorFilter(inactiveColor);
        ivTabHome.setColorFilter(inactiveColor);
        ivTabReports.setColorFilter(inactiveColor);
        ivTabMore.setColorFilter(inactiveColor);

        tvTabDashboard.setTextColor(inactiveColor);
        tvTabHome.setTextColor(inactiveColor);
        tvTabReports.setTextColor(inactiveColor);
        tvTabMore.setTextColor(inactiveColor);

        tvTabDashboard.setTypeface(null, Typeface.NORMAL);
        tvTabHome.setTypeface(null, Typeface.NORMAL);
        tvTabReports.setTypeface(null, Typeface.NORMAL);
        tvTabMore.setTypeface(null, Typeface.NORMAL);

        // Apply active state
        if (activeTabId == R.id.tab_dashboard) {
            ivTabDashboard.setColorFilter(activeColor);
            tvTabDashboard.setTextColor(activeColor);
            tvTabDashboard.setTypeface(null, Typeface.BOLD);
        } else if (activeTabId == R.id.tab_home) {
            ivTabHome.setColorFilter(activeColor);
            tvTabHome.setTextColor(activeColor);
            tvTabHome.setTypeface(null, Typeface.BOLD);
        } else if (activeTabId == R.id.tab_reports) {
            ivTabReports.setColorFilter(activeColor);
            tvTabReports.setTextColor(activeColor);
            tvTabReports.setTypeface(null, Typeface.BOLD);
        } else if (activeTabId == R.id.tab_more) {
            ivTabMore.setColorFilter(activeColor);
            tvTabMore.setTextColor(activeColor);
            tvTabMore.setTypeface(null, Typeface.BOLD);
        }
    }

    private void lookupProductAndNavigate(String scannedCode) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("products")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("product_code", scannedCode)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Product found! Navigate to edit screen.
                        String productId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        Intent intent = new Intent(MainActivity.this, ProductDetailActivity.class);
                        intent.putExtra("PRODUCT_ID", productId);
                        startActivity(intent);
                    } else {
                        // Product not found! Navigate to create new product with pre-filled code.
                        Intent intent = new Intent(MainActivity.this, ProductDetailActivity.class);
                        intent.putExtra("PRODUCT_CODE", scannedCode);
                        startActivity(intent);
                        Toast.makeText(MainActivity.this, "Sản phẩm chưa tồn tại. Nhập mới với mã: " + scannedCode,
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Lỗi tìm kiếm sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                });
    }

    public void selectTab(int tabId) {
        if (tabId == R.id.nav_dashboard) {
            loadFragment(new DashboardFragment());
            updateTabStyles(R.id.tab_dashboard);
        } else if (tabId == R.id.nav_home) {
            loadFragment(new HomeFragment());
            updateTabStyles(R.id.tab_home);
        }
    }
}