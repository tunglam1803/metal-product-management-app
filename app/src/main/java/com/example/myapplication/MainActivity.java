package com.example.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.myapplication.fragment.DashboardFragment;
import com.example.myapplication.fragment.HomeFragment;
import com.example.myapplication.fragment.MoreFragment;
import com.example.myapplication.fragment.ReportsFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.myapplication.helpers.PreferencesHelper;
import com.example.myapplication.helpers.FirebaseHelper;
import com.example.myapplication.models.Product;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> barcodeScannerLauncher;
    private View tabDashboard, tabHome, tabReports, tabMore;
    private ImageView ivTabDashboard, ivTabHome, ivTabReports, ivTabMore;
    private TextView tvTabDashboard, tvTabHome, tvTabReports, tvTabMore;

    private static final java.util.Map<String, Integer> notifiedProductStocks = new java.util.HashMap<>();
    private com.google.firebase.firestore.ListenerRegistration productsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferencesHelper prefHelper = new PreferencesHelper(this);
        if (prefHelper.isDarkMode()) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Apply system bar insets so bottom nav doesn't overlap with system navigation and top doesn't hide behind status bar
        View fragmentContainer = findViewById(R.id.fragment_container);
        View customBottomBar = findViewById(R.id.customBottomBar);
        View btnScanGlobalView = findViewById(R.id.btnScanGlobal);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            // Adjust fragment container top padding for the status bar height
            fragmentContainer.setPadding(
                fragmentContainer.getPaddingLeft(),
                insets.top,
                fragmentContainer.getPaddingRight(),
                fragmentContainer.getPaddingBottom()
            );

            // Add bottom padding to the bottom bar
            customBottomBar.setPadding(
                customBottomBar.getPaddingLeft(),
                customBottomBar.getPaddingTop(),
                customBottomBar.getPaddingRight(),
                insets.bottom
            );
            ViewGroup.LayoutParams barParams = customBottomBar.getLayoutParams();
            barParams.height = (int) (76 * getResources().getDisplayMetrics().density) + insets.bottom;
            customBottomBar.setLayoutParams(barParams);
            // Adjust the scan button margin to account for the nav bar
            ViewGroup.MarginLayoutParams scanParams = (ViewGroup.MarginLayoutParams) btnScanGlobalView.getLayoutParams();
            scanParams.bottomMargin = (int) (32 * getResources().getDisplayMetrics().density) + insets.bottom;
            btnScanGlobalView.setLayoutParams(scanParams);
            return windowInsets;
        });

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

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
        updateNotificationListenerState();
    }

    public void updateNotificationListenerState() {
        PreferencesHelper prefHelper = new PreferencesHelper(this);
        if (prefHelper.isNotificationsEnabled()) {
            if (productsListener == null) {
                startNotificationListener();
            }
        } else {
            if (productsListener != null) {
                productsListener.remove();
                productsListener = null;
            }
            notifiedProductStocks.clear();
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
                        String productId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        Intent intent = new Intent(MainActivity.this, ProductDetailActivity.class);
                        intent.putExtra("PRODUCT_ID", productId);
                        startActivity(intent);
                    } else {
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

    private void startNotificationListener() {
        if (productsListener != null) {
            productsListener.remove();
        }

        FirebaseHelper firebaseHelper = new FirebaseHelper();
        productsListener = firebaseHelper.listenForProducts((value, error) -> {
            if (error != null || value == null) {
                return;
            }

            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                Product p = doc.toObject(Product.class);
                p.setId(doc.getId());

                int stock = p.getStock_quantity();
                if (stock <= 5) {
                    if (!notifiedProductStocks.containsKey(p.getId()) || notifiedProductStocks.get(p.getId()) != stock) {
                        sendLowStockPushNotification(p);
                        notifiedProductStocks.put(p.getId(), stock);
                    }
                } else {
                    notifiedProductStocks.remove(p.getId());
                }
            }
        });
    }

    private void sendLowStockPushNotification(Product p) {
        String channelId = "settings_notifications";
        String channelName = "Thông báo hệ thống";

        android.app.NotificationManager notificationManager = 
            (android.app.NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                channelId, channelName, android.app.NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Kênh thông báo quản trị hệ thống");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Cảnh báo tồn kho thấp! ⚠️")
            .setContentText("Sản phẩm '" + p.getProduct_name() + "' chỉ còn " + p.getStock_quantity() + " sản phẩm trong kho.")
            .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle()
                .bigText("Sản phẩm '" + p.getProduct_name() + "' (SKU: " + (p.getProduct_code() != null ? p.getProduct_code() : "N/A") + ") hiện đang ở mức báo động với chỉ " + p.getStock_quantity() + " đơn vị tồn kho. Vui lòng nhập thêm hàng sớm!"))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify(p.getId().hashCode(), builder.build());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (productsListener != null) {
            productsListener.remove();
        }
    }
}