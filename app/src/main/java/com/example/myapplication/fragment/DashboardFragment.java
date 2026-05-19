package com.example.myapplication.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageButton;
import android.graphics.Color;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.MainActivity;
import com.example.myapplication.ProductDetailActivity;
import com.example.myapplication.R;
import com.example.myapplication.adapters.ProductAdapter;
import com.example.myapplication.helpers.FirebaseHelper;
import com.example.myapplication.models.Category;
import com.example.myapplication.models.Product;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DashboardFragment extends Fragment {

    private TextView tvUserEmail, tvProfileInitial;
    private TextView tvStatsProductsCount, tvStatsCategoriesCount;
    private RecyclerView rvRecentProducts;
    private ProductAdapter adapter;
    private FirebaseHelper firebaseHelper;
    private final List<Product> allProducts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        firebaseHelper = new FirebaseHelper();

        // 1. Bind Views
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvProfileInitial = view.findViewById(R.id.tvProfileInitial);
        tvStatsProductsCount = view.findViewById(R.id.tvStatsProductsCount);
        tvStatsCategoriesCount = view.findViewById(R.id.tvStatsCategoriesCount);
        rvRecentProducts = view.findViewById(R.id.rvRecentProducts);

        View btnActionProducts = view.findViewById(R.id.btnActionProducts);
        View btnActionAddProduct = view.findViewById(R.id.btnActionAddProduct);
        View btnActionAddCategory = view.findViewById(R.id.btnActionAddCategory);

        // 2. Setup Profile Info
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String email = auth.getCurrentUser().getEmail();
            tvUserEmail.setText(email);
            if (email != null && email.length() > 0) {
                tvProfileInitial.setText(email.substring(0, 1).toUpperCase());
            }
        }

        // 3. Setup RecyclerView
        rvRecentProducts.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProductAdapter(getContext(), product -> {
            Intent intent = new Intent(getActivity(), ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.getId());
            startActivity(intent);
        });
        rvRecentProducts.setAdapter(adapter);

        // 4. Click Listeners for Action Shortcuts
        btnActionProducts.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).selectTab(R.id.nav_home);
            }
        });

        btnActionAddProduct.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ProductDetailActivity.class));
        });

        btnActionAddCategory.setOnClickListener(v -> showCategoryManagerDialog());

        // 5. Load Real-time Stats and List
        loadDashboardData();

        return view;
    }

    private void loadDashboardData() {
        firebaseHelper.listenForProducts((value, error) -> {
            if (error != null || value == null)
                return;

            allProducts.clear();
            for (QueryDocumentSnapshot doc : value) {
                Product p = doc.toObject(Product.class);
                p.setId(doc.getId());
                allProducts.add(p);
            }

            tvStatsProductsCount.setText(String.valueOf(allProducts.size()));

            allProducts.sort((p1, p2) -> {
                Long t1 = p1.getUpdated_at() != null ? p1.getUpdated_at() : 0L;
                Long t2 = p2.getUpdated_at() != null ? p2.getUpdated_at() : 0L;
                return t2.compareTo(t1);
            });

            List<Product> recent = new ArrayList<>();
            for (int i = 0; i < Math.min(3, allProducts.size()); i++) {
                recent.add(allProducts.get(i));
            }
            adapter.setProducts(recent);
        });

        firebaseHelper.listenForCategories((value, error) -> {
            if (error != null || value == null)
                return;

            tvStatsCategoriesCount.setText(String.valueOf(value.size()));

            List<Category> categoriesList = new ArrayList<>();
            for (QueryDocumentSnapshot doc : value) {
                Category cat = doc.toObject(Category.class);
                cat.setId(doc.getId());
                categoriesList.add(cat);
            }
            adapter.setCategories(categoriesList);
        });
    }

    private int dpToPx(int dp) {
        if (getContext() == null)
            return dp;
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void showCategoryManagerDialog() {
        Context context = getContext();
        if (context == null)
            return;

        // Inflate the main dialog layout
        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_category_manager, null);

        EditText etNewCat = dialogView.findViewById(R.id.et_new_category);
        Button btnAdd = dialogView.findViewById(R.id.btn_add_category);
        LinearLayout listLayout = dialogView.findViewById(R.id.ll_categories_list);

        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        int textColorPrimary = typedValue.data;
        boolean isDarkMode = (textColorPrimary == Color.WHITE || (textColorPrimary & 0xFFFFFF) == 0xFFFFFF);
        int dividerColor = ContextCompat.getColor(context, R.color.divider);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Quản Lý Danh Mục")
                .setView(dialogView)
                .setNegativeButton("Đóng", null)
                .create();

        // Lắng nghe sự kiện click Thêm danh mục
        btnAdd.setOnClickListener(v -> {
            String catName = etNewCat.getText().toString().trim();
            if (catName.isEmpty()) {
                Toast.makeText(context, "Tên danh mục không được để trống!", Toast.LENGTH_SHORT).show();
                return;
            }
            firebaseHelper.addCategory(catName, () -> {
                etNewCat.setText("");
                Toast.makeText(context, "Thêm danh mục '" + catName + "' thành công!", Toast.LENGTH_SHORT).show();
            }, error -> {
                Toast.makeText(context, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            });
        });

        // Thiết lập bộ lắng nghe cập nhật danh sách Real-time
        firebaseHelper.listenForCategories((value, error) -> {
            if (error != null || value == null || !dialog.isShowing())
                return;

            listLayout.removeAllViews();

            List<Category> list = new ArrayList<>();
            for (QueryDocumentSnapshot doc : value) {
                Category cat = doc.toObject(Category.class);
                cat.setId(doc.getId());
                list.add(cat);
            }

            if (list.isEmpty()) {
                TextView tvEmpty = new TextView(context);
                tvEmpty.setText("Chưa có danh mục nào.");
                tvEmpty.setGravity(Gravity.CENTER);
                tvEmpty.setPadding(0, dpToPx(40), 0, dpToPx(40));

                context.getTheme().resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
                tvEmpty.setTextColor(typedValue.data);
                listLayout.addView(tvEmpty);
                return;
            }

            for (Category cat : list) {
                // Inflate item layout
                View rowView = LayoutInflater.from(context)
                        .inflate(R.layout.item_category_manage, listLayout, false);

                TextView tvName = rowView.findViewById(R.id.tv_category_name);
                ImageButton btnEdit = rowView.findViewById(R.id.btn_edit_category);
                ImageButton btnDelete = rowView.findViewById(R.id.btn_delete_category);
                View divider = rowView.findViewById(R.id.divider);

                tvName.setText(cat.getCategory_name());
                divider.setBackgroundColor(dividerColor);

                // Sự kiện click nút Sửa
                btnEdit.setOnClickListener(v2 -> {
                    EditText etEdit = new EditText(context);
                    etEdit.setText(cat.getCategory_name());
                    etEdit.setTextColor(textColorPrimary);
                    etEdit.setHintTextColor(ContextCompat.getColor(context, R.color.text_hint));
                    etEdit.setSelection(etEdit.getText().length());
                    etEdit.setBackgroundResource(R.drawable.edit_text_background);
                    etEdit.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));

                    FrameLayout editContainer = new FrameLayout(context);
                    FrameLayout.LayoutParams editParams = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    editParams.leftMargin = dpToPx(24);
                    editParams.rightMargin = dpToPx(24);
                    editParams.topMargin = dpToPx(16);
                    editParams.bottomMargin = dpToPx(16);
                    etEdit.setLayoutParams(editParams);
                    editContainer.addView(etEdit);

                    new MaterialAlertDialogBuilder(context)
                            .setTitle("Sửa Tên Danh Mục")
                            .setView(editContainer)
                            .setPositiveButton("Cập nhật", (d, w) -> {
                                String newName = etEdit.getText().toString().trim();
                                if (!newName.isEmpty()) {
                                    firebaseHelper.updateCategory(cat.getId(), newName, () -> {
                                        Toast.makeText(context, "Cập nhật danh mục thành công!", Toast.LENGTH_SHORT)
                                                .show();
                                    }, err -> {
                                        Toast.makeText(context, "Lỗi: " + err, Toast.LENGTH_SHORT).show();
                                    });
                                }
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                });

                // Sự kiện click nút Xóa
                btnDelete.setOnClickListener(v2 -> {
                    new MaterialAlertDialogBuilder(context)
                            .setTitle("Xóa Danh Mục")
                            .setMessage("Bạn có chắc chắn muốn xóa danh mục '" + cat.getCategory_name()
                                    + "'?\n\nTất cả sản phẩm thuộc danh mục này sẽ tự động chuyển sang nhóm 'Không xác định' (null).")
                            .setPositiveButton("Xóa", (d, w) -> {
                                firebaseHelper.deleteCategory(cat.getId(), () -> {
                                    Toast.makeText(context, "Đã xóa danh mục thành công!", Toast.LENGTH_SHORT).show();
                                }, err -> {
                                    Toast.makeText(context, "Lỗi xóa: " + err, Toast.LENGTH_SHORT).show();
                                });
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                });

                listLayout.addView(rowView);
            }
        });

        dialog.show();
    }
}
