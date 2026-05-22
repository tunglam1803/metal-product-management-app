package com.example.myapplication.fragment;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.helpers.FirebaseHelper;
import com.example.myapplication.models.Category;
import com.example.myapplication.models.Product;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportsFragment extends Fragment {

    private TextView tvTotalProducts;
    private TextView tvTotalCategories;
    private TextView tvTotalStockQty;
    private TextView tvTotalStockValue;

    private LinearLayout llLowStockContainer;
    private LinearLayout llCategoryBreakdownContainer;
    private LinearLayout llTopValueProductsContainer;

    private TextView tvLowStockEmptyState;
    private TextView tvCategoryBreakdownEmptyState;
    private TextView tvTopValueEmptyState;
    private TextView tvSystemStatusText;
    private com.google.android.material.card.MaterialCardView cvSystemStatus;

    private FirebaseHelper firebaseHelper;

    // Danh sách đệm đồng bộ dữ liệu thời gian thực
    private final List<Product> productsList = new ArrayList<>();
    private final List<Category> categoriesList = new ArrayList<>();
    private boolean hasProductsLoaded = false;
    private boolean hasCategoriesLoaded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        // Ánh xạ các thẻ metric chính
        tvTotalProducts = view.findViewById(R.id.tvTotalProducts);
        tvTotalCategories = view.findViewById(R.id.tvTotalCategories);
        tvTotalStockQty = view.findViewById(R.id.tvTotalStockQty);
        tvTotalStockValue = view.findViewById(R.id.tvTotalStockValue);

        // Ánh xạ các container danh sách động
        llLowStockContainer = view.findViewById(R.id.llLowStockContainer);
        llCategoryBreakdownContainer = view.findViewById(R.id.llCategoryBreakdownContainer);
        llTopValueProductsContainer = view.findViewById(R.id.llTopValueProductsContainer);

        // Ánh xạ các Empty State Views
        tvLowStockEmptyState = view.findViewById(R.id.tvLowStockEmptyState);
        tvCategoryBreakdownEmptyState = view.findViewById(R.id.tvCategoryBreakdownEmptyState);
        tvTopValueEmptyState = view.findViewById(R.id.tvTopValueEmptyState);
        tvSystemStatusText = view.findViewById(R.id.tvSystemStatusText);
        cvSystemStatus = view.findViewById(R.id.cvSystemStatus);

        if (cvSystemStatus != null) {
            cvSystemStatus.setOnClickListener(v -> showSystemNotificationCenterDialog());
        }

        firebaseHelper = new FirebaseHelper();

        loadStats();

        return view;
    }

    private void loadStats() {
        // Lắng nghe thay đổi dữ liệu Sản phẩm
        firebaseHelper.listenForProducts((value, error) -> {
            if (error == null && value != null) {
                productsList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Product p = doc.toObject(Product.class);
                    p.setId(doc.getId());
                    productsList.add(p);
                }
                hasProductsLoaded = true;
                checkAndRefreshDashboard();
            }
        });

        // Lắng nghe thay đổi dữ liệu Danh mục
        firebaseHelper.listenForCategories((value, error) -> {
            if (error == null && value != null) {
                categoriesList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Category cat = doc.toObject(Category.class);
                    cat.setId(doc.getId());
                    categoriesList.add(cat);
                }
                hasCategoriesLoaded = true;
                checkAndRefreshDashboard();
            }
        });
    }

    private synchronized void checkAndRefreshDashboard() {
        if (hasProductsLoaded && hasCategoriesLoaded) {
            updateDashboardUI();
        }
    }

    private void updateDashboardUI() {
        if (getContext() == null) return;

        // 1. Cập nhật các chỉ số tổng hợp
        int uniqueProductsCount = productsList.size();
        int uniqueCategoriesCount = categoriesList.size();

        int totalStockQuantity = 0;
        double totalStockValue = 0.0;

        for (Product p : productsList) {
            int qty = p.getStock_quantity();
            totalStockQuantity += qty;
            totalStockValue += (qty * p.getSell_price());
        }

        tvTotalProducts.setText(String.valueOf(uniqueProductsCount));
        tvTotalCategories.setText(String.valueOf(uniqueCategoriesCount));
        tvTotalStockQty.setText(String.valueOf(totalStockQuantity));
        tvTotalStockValue.setText(formatCurrency(totalStockValue));

        // 2. Render: Cảnh báo tồn kho thấp (stock_quantity <= 5)
        renderLowStockAlerts();

        // 3. Render: Cơ cấu tồn kho theo danh mục
        renderCategoryBreakdown(totalStockQuantity);

        // 4. Render: Top 3 sản phẩm có giá trị tồn kho lớn nhất
        renderTopValueProducts();

        // 5. Cập nhật trạng thái đồng bộ động
        if (tvSystemStatusText != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
            String timeStr = sdf.format(new java.util.Date());
            tvSystemStatusText.setText("Hệ thống cơ sở dữ liệu đám mây Firebase đang hoạt động ổn định. Đã đồng bộ lúc " + timeStr + ".");
        }
    }

    private void renderLowStockAlerts() {
        if (getContext() == null) return;

        // Xóa tất cả View cũ ngoại trừ Empty State
        llLowStockContainer.removeAllViews();

        List<Product> lowStockProducts = new ArrayList<>();
        for (Product p : productsList) {
            if (p.getStock_quantity() <= 5) {
                lowStockProducts.add(p);
            }
        }

        // Sắp xếp tăng dần theo lượng tồn để cảnh báo hàng khẩn cấp trước
        Collections.sort(lowStockProducts, (p1, p2) -> Integer.compare(p1.getStock_quantity(), p2.getStock_quantity()));

        if (lowStockProducts.isEmpty()) {
            if (tvLowStockEmptyState != null) {
                llLowStockContainer.addView(tvLowStockEmptyState);
            }
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < lowStockProducts.size(); i++) {
            Product p = lowStockProducts.get(i);
            View row = inflater.inflate(R.layout.item_report_row, llLowStockContainer, false);

            FrameLayout flIconBg = row.findViewById(R.id.flIconBg);
            ImageView ivRowIcon = row.findViewById(R.id.ivRowIcon);
            TextView tvRowTitle = row.findViewById(R.id.tvRowTitle);
            TextView tvRowSubtitle = row.findViewById(R.id.tvRowSubtitle);
            TextView tvRowValue = row.findViewById(R.id.tvRowValue);
            View divider = row.findViewById(R.id.viewRowDivider);

            // Cài đặt icon cảnh báo đỏ danger
            flIconBg.setBackgroundResource(R.drawable.bg_icon_circle_purple);
            flIconBg.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.cardAccentOrange)));
            ivRowIcon.setImageResource(android.R.drawable.ic_dialog_alert);
            ivRowIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.colorDanger)));

            tvRowTitle.setText(p.getProduct_name());
            tvRowSubtitle.setText("Mã SKU: " + (p.getProduct_code() != null ? p.getProduct_code() : "N/A"));
            tvRowValue.setText("Tồn: " + p.getStock_quantity());
            tvRowValue.setTextColor(ContextCompat.getColor(getContext(), R.color.colorDanger));

            // Ẩn divider cho item cuối cùng
            if (i == lowStockProducts.size() - 1) {
                divider.setVisibility(View.GONE);
            }

            llLowStockContainer.addView(row);
        }
    }

    private void renderCategoryBreakdown(int totalStock) {
        if (getContext() == null) return;

        llCategoryBreakdownContainer.removeAllViews();

        if (categoriesList.isEmpty()) {
            if (tvCategoryBreakdownEmptyState != null) {
                llCategoryBreakdownContainer.addView(tvCategoryBreakdownEmptyState);
            }
            return;
        }

        // Tính tổng tồn của mỗi danh mục
        Map<String, Integer> catStockMap = new HashMap<>();
        Map<String, Integer> catProdCountMap = new HashMap<>();
        for (Category c : categoriesList) {
            catStockMap.put(c.getId(), 0);
            catProdCountMap.put(c.getId(), 0);
        }
        // Danh mục null
        catStockMap.put("null", 0);
        catProdCountMap.put("null", 0);

        for (Product p : productsList) {
            String catId = p.getCategory_id() != null ? p.getCategory_id() : "null";
            int currentQty = catStockMap.containsKey(catId) ? catStockMap.get(catId) : 0;
            catStockMap.put(catId, currentQty + p.getStock_quantity());

            int currentCount = catProdCountMap.containsKey(catId) ? catProdCountMap.get(catId) : 0;
            catProdCountMap.put(catId, currentCount + 1);
        }

        LayoutInflater inflater = LayoutInflater.from(getContext());
        int idx = 0;
        for (Category cat : categoriesList) {
            int stockQty = catStockMap.containsKey(cat.getId()) ? catStockMap.get(cat.getId()) : 0;
            int prodCount = catProdCountMap.containsKey(cat.getId()) ? catProdCountMap.get(cat.getId()) : 0;
            double percent = totalStock > 0 ? ((double) stockQty / totalStock) * 100 : 0;

            View row = inflater.inflate(R.layout.item_report_row, llCategoryBreakdownContainer, false);

            FrameLayout flIconBg = row.findViewById(R.id.flIconBg);
            ImageView ivRowIcon = row.findViewById(R.id.ivRowIcon);
            TextView tvRowTitle = row.findViewById(R.id.tvRowTitle);
            TextView tvRowSubtitle = row.findViewById(R.id.tvRowSubtitle);
            TextView tvRowValue = row.findViewById(R.id.tvRowValue);
            ProgressBar pbRowProgress = row.findViewById(R.id.pbRowProgress);
            View divider = row.findViewById(R.id.viewRowDivider);

            // Cài đặt icon màu xanh lam thông tin
            flIconBg.setBackgroundResource(R.drawable.bg_icon_circle_blue);
            ivRowIcon.setImageResource(android.R.drawable.ic_menu_view);
            ivRowIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.colorInfo)));

            tvRowTitle.setText(cat.getCategory_name());
            tvRowSubtitle.setText(prodCount + " mặt hàng");
            
            DecimalFormat df = new DecimalFormat("#.#");
            tvRowValue.setText(stockQty + " cái (" + df.format(percent) + "%)");
            tvRowValue.setTextColor(ContextCompat.getColor(getContext(), R.color.colorInfo));

            // Hiển thị ProgressBar tỉ lệ
            pbRowProgress.setVisibility(View.VISIBLE);
            pbRowProgress.setProgress((int) Math.round(percent));
            pbRowProgress.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.colorInfo)));

            // Ẩn divider cho item cuối
            if (idx == categoriesList.size() - 1) {
                divider.setVisibility(View.GONE);
            }
            idx++;

            llCategoryBreakdownContainer.addView(row);
        }
    }

    private void renderTopValueProducts() {
        if (getContext() == null) return;

        llTopValueProductsContainer.removeAllViews();

        if (productsList.isEmpty()) {
            if (tvTopValueEmptyState != null) {
                llTopValueProductsContainer.addView(tvTopValueEmptyState);
            }
            return;
        }

        // Sắp xếp giảm dần theo: Tồn kho * Giá bán (Giá trị vốn lưu động)
        List<Product> sortedList = new ArrayList<>(productsList);
        Collections.sort(sortedList, (p1, p2) -> {
            double v1 = p1.getStock_quantity() * p1.getSell_price();
            double v2 = p2.getStock_quantity() * p2.getSell_price();
            return Double.compare(v2, v1);
        });

        int maxItems = Math.min(3, sortedList.size());
        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (int i = 0; i < maxItems; i++) {
            Product p = sortedList.get(i);
            double itemTotalVal = p.getStock_quantity() * p.getSell_price();

            View row = inflater.inflate(R.layout.item_report_row, llTopValueProductsContainer, false);

            FrameLayout flIconBg = row.findViewById(R.id.flIconBg);
            ImageView ivRowIcon = row.findViewById(R.id.ivRowIcon);
            TextView tvRowTitle = row.findViewById(R.id.tvRowTitle);
            TextView tvRowSubtitle = row.findViewById(R.id.tvRowSubtitle);
            TextView tvRowValue = row.findViewById(R.id.tvRowValue);
            View divider = row.findViewById(R.id.viewRowDivider);

            // Cài đặt icon cúp vàng / màu tím chủ đạo
            flIconBg.setBackgroundResource(R.drawable.bg_icon_circle_green);
            flIconBg.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.colorPrimaryContainer)));
            ivRowIcon.setImageResource(android.R.drawable.ic_menu_myplaces);
            ivRowIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.colorPrimary)));

            tvRowTitle.setText(p.getProduct_name());
            tvRowSubtitle.setText("Tồn kho: " + p.getStock_quantity() + " x " + formatCurrency(p.getSell_price()));
            tvRowValue.setText(formatCurrency(itemTotalVal));
            tvRowValue.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));

            // Ẩn divider cho item cuối cùng
            if (i == maxItems - 1) {
                divider.setVisibility(View.GONE);
            }

            llTopValueProductsContainer.addView(row);
        }
    }

    private String formatCurrency(double amount) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(amount) + " ₫";
    }

    private void showSystemNotificationCenterDialog() {
        if (getContext() == null) return;

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = 
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext());
        
        builder.setTitle("Trung tâm Thông báo Hệ thống 🔔");

        com.example.myapplication.helpers.PreferencesHelper prefHelper = 
            new com.example.myapplication.helpers.PreferencesHelper(requireContext());

        boolean isBiometricEnabled = prefHelper.isBiometricsEnabled();
        boolean isPushEnabled = prefHelper.isNotificationsEnabled();

        int lowStockCount = 0;
        for (Product p : productsList) {
            if (p.getStock_quantity() <= 5) {
                lowStockCount++;
            }
        }

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss dd/MM/yyyy", java.util.Locale.getDefault());
        String timeStr = sdf.format(new java.util.Date());

        StringBuilder htmlText = new StringBuilder();
        htmlText.append("<b>🟢 Kết nối đám mây Firebase:</b> Hoạt động ổn định<br>");
        htmlText.append("<font color='#666666'>Đang lắng nghe dữ liệu thời gian thực thông qua các luồng SnapshotListener bảo mật.</font><br><br>");

        htmlText.append("<b>🔒 Bảo mật sinh trắc học:</b> ")
                .append(isBiometricEnabled ? "<font color='#4CAF50'>Đã bật (Vân tay/FaceID)</font>" : "<font color='#F44336'>Chưa kích hoạt</font>")
                .append("<br><font color='#666666'>Bảo vệ thông tin kho hàng khỏi truy cập trái phép khi khởi động ứng dụng.</font><br><br>");

        htmlText.append("<b>🔔 Cảnh báo thông báo đẩy:</b> ")
                .append(isPushEnabled ? "<font color='#4CAF50'>Đang hoạt động</font>" : "<font color='#F44336'>Đã tắt</font>")
                .append("<br><font color='#666666'>Tự động thông báo trực tiếp khi sản phẩm chạm ngưỡng tồn kho thấp.</font><br><br>");

        htmlText.append("<b>⚠️ Tình trạng tồn kho hiện tại:</b> ")
                .append(lowStockCount > 0 ? "<font color='#FF9800'>Có " + lowStockCount + " cảnh báo tồn thấp</font>" : "<font color='#4CAF50'>An toàn (0 cảnh báo)</font>")
                .append("<br><font color='#666666'>Các sản phẩm có số lượng từ 5 trở xuống sẽ được đưa vào mức độ cảnh báo đỏ.</font><br><br>");

        htmlText.append("<b>📊 Lịch sử đồng bộ cuối:</b><br>")
                .append("<font color='#00BCD4'>")
                .append(timeStr)
                .append("</font>");

        TextView textView = new TextView(requireContext());
        textView.setPadding(48, 32, 48, 32);
        textView.setTextSize(14f);
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextDark));
        textView.setMovementMethod(new ScrollingMovementMethod());
        textView.setText(Html.fromHtml(htmlText.toString(), Html.FROM_HTML_MODE_LEGACY));

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.addView(textView);

        builder.setView(scrollView);
        builder.setPositiveButton("Đồng ý", (dialog, which) -> dialog.dismiss());
        
        if (isPushEnabled) {
            builder.setNeutralButton("Gửi thông báo test", (dialog, which) -> {
                dialog.dismiss();
                triggerTestNotification();
            });
        }

        builder.show();
    }

    private void triggerTestNotification() {
        if (getContext() == null) return;
        
        String channelId = "settings_notifications";
        String channelName = "Thông báo hệ thống";
        
        android.app.NotificationManager notificationManager = 
            (android.app.NotificationManager) requireContext().getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                channelId, channelName, android.app.NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Kênh thông báo quản trị hệ thống");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        
        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Kiểm tra thông báo hệ thống 🔔")
            .setContentText("Thông báo đẩy đang hoạt động hoàn hảo và sẵn sàng đưa tin!")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);
            
        if (notificationManager != null) {
            notificationManager.notify(999, builder.build());
        }
    }
}
