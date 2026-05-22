package com.example.myapplication.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.MotionEvent;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.app.Activity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import com.example.myapplication.ProductDetailActivity;
import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.ScannerActivity;
import com.example.myapplication.adapters.ProductAdapter;
import com.example.myapplication.helpers.FirebaseHelper;
import com.example.myapplication.models.Category;
import com.example.myapplication.models.Product;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class HomeFragment extends Fragment {

    private ProductAdapter adapter;
    private EditText etSearch;
    private FirebaseHelper firebaseHelper;
    private final List<Product> allProducts = new ArrayList<>();
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private LinearLayout llPillsContainer;
    private final List<Category> categoriesList = new ArrayList<>();
    private String selectedCategoryId = null;
    private int swipedPosition = -1;
    private RecyclerView rvProducts;
    private ActivityResultLauncher<Intent> barcodeScannerLauncher;
    private int currentSortMode = 0; // 0=Mới nhất, 1=Cũ nhất, 2=Giá tăng, 3=Giá giảm, 4=Tên A-Z, 5=Tên Z-A
    private boolean isGridView = false;
    private int currentTab = 0; // 0=Sản phẩm, 1=Tồn kho, 2=Bán kèm
    private ImageButton btnGridToggle;
    private View tabProducts, tabInventory, tabBundle;
    private SwipeRefreshLayout swipeRefreshLayout;
    private com.google.firebase.firestore.ListenerRegistration productsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        firebaseHelper = new FirebaseHelper();

        // 1. Ánh xạ các View chính
        rvProducts = view.findViewById(R.id.rvProducts);
        etSearch = view.findViewById(R.id.etSearch);
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        ImageButton btnScan = view.findViewById(R.id.btnScan);
        ImageButton btnSort = view.findViewById(R.id.btnSort);
        btnGridToggle = view.findViewById(R.id.btnGridToggle);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        // Configure swipe refresh layout
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadProducts();
            loadCategoriesAndPopulatePills();
        });

        // Register Barcode Scanner Launcher
        barcodeScannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String barcode = result.getData().getStringExtra("SCANNED_BARCODE");
                        if (barcode != null && !barcode.isEmpty()) {
                            etSearch.setText(barcode);
                            Toast.makeText(getContext(), "Đã quét: " + barcode, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // 2. Ánh xạ các View phụ (Tabs & Pills)
        tabProducts = view.findViewById(R.id.tabProducts);
        tabInventory = view.findViewById(R.id.tabInventory);
        tabBundle = view.findViewById(R.id.tabBundle);
        View tabCategory = view.findViewById(R.id.tabCategory);

        llPillsContainer = view.findViewById(R.id.llPillsContainer);
        View btnPillsGrid = view.findViewById(R.id.btnPillsGrid);

        // 3. Thiết lập RecyclerView & Adapter
        rvProducts.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ProductAdapter(getContext(), product -> {
            Intent intent = new Intent(getActivity(), ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.getId());
            startActivity(intent);
        });
        rvProducts.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                    @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 9.0f;
            }

            @Override
            public float getSwipeEscapeVelocity(float defaultValue) {
                return defaultValue * 10;
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int position = viewHolder.getAdapterPosition();
                super.clearView(recyclerView, viewHolder);
                if (swipedPosition == position && position != -1) {
                    int swipeLimit = dpToPx(160);
                    viewHolder.itemView.setTranslationX(-swipeLimit);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                    int actionState, boolean isCurrentlyActive) {

                View itemView = viewHolder.itemView;
                int itemHeight = itemView.getBottom() - itemView.getTop();
                int position = viewHolder.getAdapterPosition();

                // Tính toán giới hạn vuốt bằng DP để thích ứng hoàn hảo với mọi độ phân giải
                // màn hình
                int swipeLimit = dpToPx(160); // 160dp tổng cho 2 nút (mỗi nút 80dp)
                int halfLimit = swipeLimit / 2; // 80dp mỗi nút

                float translationX = dX;
                if (!isCurrentlyActive) {
                    if (swipedPosition == position) {
                        float currentTx = itemView.getTranslationX();
                        translationX = currentTx + (-swipeLimit - currentTx) * 0.25f;
                    }
                } else {
                    if (dX < -halfLimit) {
                        swipedPosition = position;
                    } else if (dX > -20f) {
                        if (swipedPosition == position) {
                            swipedPosition = -1;
                        }
                    }
                }

                float clampedDx = Math.max(translationX, -swipeLimit);

                Paint paint = new Paint();

                // 1. Vẽ nút "SỬA" màu Xanh Lam (Bên trái)
                paint.setColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary));
                c.drawRoundRect(
                        (float) itemView.getRight() - swipeLimit, (float) itemView.getTop() + 10,
                        (float) itemView.getRight() - halfLimit, (float) itemView.getBottom() - 10,
                        16, 16, paint);

                Drawable editIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_edit);
                if (editIcon != null) {
                    editIcon.setTint(Color.WHITE);
                    int intrinsicWidth = editIcon.getIntrinsicWidth();
                    int intrinsicHeight = editIcon.getIntrinsicHeight();

                    int editIconLeft = itemView.getRight() - (swipeLimit + halfLimit) / 2 - intrinsicWidth / 2;
                    int editIconRight = editIconLeft + intrinsicWidth;
                    int editIconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2 - 12;
                    int editIconBottom = editIconTop + intrinsicHeight;

                    editIcon.setBounds(editIconLeft, editIconTop, editIconRight, editIconBottom);
                    editIcon.draw(c);
                }

                paint.setColor(Color.WHITE);
                paint.setTextSize(26);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setFakeBoldText(true);
                c.drawText("Sửa", (float) itemView.getRight() - (float) (swipeLimit + halfLimit) / 2,
                        (float) itemView.getBottom() - 24, paint);

                // 2. Vẽ nút "XÓA" màu Đỏ Rực (Bên phải)
                paint.setColor(ContextCompat.getColor(requireContext(), R.color.colorDanger));
                c.drawRoundRect(
                        (float) itemView.getRight() - halfLimit, (float) itemView.getTop() + 10,
                        (float) itemView.getRight(), (float) itemView.getBottom() - 10,
                        16, 16, paint);

                Drawable deleteIcon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete);
                if (deleteIcon != null) {
                    deleteIcon.setTint(Color.WHITE);
                    int intrinsicWidth = deleteIcon.getIntrinsicWidth();
                    int intrinsicHeight = deleteIcon.getIntrinsicHeight();
                    int deleteIconLeft = itemView.getRight() - halfLimit / 2 - intrinsicWidth / 2;
                    int deleteIconRight = deleteIconLeft + intrinsicWidth;
                    int deleteIconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2 - 12;
                    int deleteIconBottom = deleteIconTop + intrinsicHeight;

                    deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
                    deleteIcon.draw(c);
                }

                paint.setColor(Color.WHITE);
                paint.setTextSize(26);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setFakeBoldText(true);
                c.drawText("Xóa", (float) itemView.getRight() - (float) halfLimit / 2,
                        (float) itemView.getBottom() - 24, paint);

                if (!isCurrentlyActive && swipedPosition == position) {
                    itemView.setTranslationX(clampedDx);
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, clampedDx, dY, actionState, isCurrentlyActive);
                }
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvProducts);

        rvProducts.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            private float startX, startY;

            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (swipedPosition == -1)
                    return false;

                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = e.getX();
                    startY = e.getY();

                    RecyclerView.ViewHolder swipedHolder = rv.findViewHolderForAdapterPosition(swipedPosition);
                    if (swipedHolder != null) {
                        View itemView = swipedHolder.itemView;
                        int swipeLimit = dpToPx(160);

                        // Kiểm tra nếu chạm nằm trong vùng 2 nút của thẻ đang mở -> đánh chặn chạm để
                        // xử lý click
                        if (startY >= itemView.getTop() && startY <= itemView.getBottom() &&
                                startX >= itemView.getRight() - swipeLimit && startX <= itemView.getRight()) {
                            return true;
                        }
                    }
                    // Nếu chạm ra ngoài vùng 2 nút -> tự động khép menu trượt lại mượt mà và KHÔNG
                    // đánh chặn (để click/scroll bình thường)
                    resetSwipe();
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if (swipedPosition == -1)
                    return;

                RecyclerView.ViewHolder swipedHolder = rv.findViewHolderForAdapterPosition(swipedPosition);
                if (swipedHolder == null) {
                    resetSwipe();
                    return;
                }

                View itemView = swipedHolder.itemView;
                int swipeLimit = dpToPx(160);
                int halfLimit = swipeLimit / 2;

                if (e.getAction() == MotionEvent.ACTION_UP) {
                    float endX = e.getX();
                    float endY = e.getY();

                    if (Math.abs(endX - startX) < 15 && Math.abs(endY - startY) < 15) {
                        if (endY >= itemView.getTop() && endY <= itemView.getBottom()) {
                            if (endX >= itemView.getRight() - swipeLimit && endX < itemView.getRight() - halfLimit) {
                                Product product = adapter.getProductAt(swipedPosition);
                                if (product != null) {
                                    Intent intent = new Intent(getActivity(), ProductDetailActivity.class);
                                    intent.putExtra("PRODUCT_ID", product.getId());
                                    startActivity(intent);
                                }
                                resetSwipe();
                                return;
                            } else if (endX >= itemView.getRight() - halfLimit && endX <= itemView.getRight()) {
                                Product product = adapter.getProductAt(swipedPosition);
                                if (product != null) {
                                    showDeleteConfirmation(product, swipedPosition);
                                }
                                resetSwipe();
                                return;
                            }
                        }
                    }
                    resetSwipe();
                }
            }
        });

        // 4. Thiết lập sự kiện cho các Nút Toolbar Tìm Kiếm
        btnBack.setOnClickListener(v -> {
            if (etSearch.getText().length() > 0) {
                etSearch.setText("");
            } else {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).selectTab(R.id.nav_dashboard);
                }
            }
            hideKeyboard();
        });

        btnScan.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), ScannerActivity.class);
            barcodeScannerLauncher.launch(intent);
        });

        btnSort.setOnClickListener(v -> showSortDialog());
        btnGridToggle.setOnClickListener(v -> toggleGridView());

        // 5. Thiết lập sự kiện cho các Tab ngang
        tabProducts.setOnClickListener(v -> {
            currentTab = 0;
            updateTabStyles();
            filterProducts(etSearch.getText().toString());
        });
        tabInventory.setOnClickListener(v -> {
            currentTab = 1;
            updateTabStyles();
            filterProducts(etSearch.getText().toString());
        });
        tabBundle.setOnClickListener(v -> {
            currentTab = 2;
            updateTabStyles();
            filterProducts(etSearch.getText().toString());
        });
        tabCategory.setOnClickListener(v -> showCategoryManagerDialog());

        // 6. Thiết lập sự kiện cho các Pills nhãn lọc
        btnPillsGrid.setOnClickListener(v -> showCategoriesBottomSheet());

        // 7. Thiết lập sự kiện nút FAB
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ProductDetailActivity.class));
        });

        // 8. Tải dữ liệu & Debounce Tìm kiếm
        setupSearchDebounce();
        loadProducts();
        loadCategoriesAndPopulatePills();

        return view;
    }

    private void loadProducts() {
        firebaseHelper.listenForProducts((value, error) -> {
            swipeRefreshLayout.setRefreshing(false);
            if (error != null) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi Firestore: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
                return;
            }
            if (value == null)
                return;

            allProducts.clear();
            for (QueryDocumentSnapshot doc : value) {
                Product p = doc.toObject(Product.class);
                p.setId(doc.getId());
                allProducts.add(p);
            }

            allProducts.sort((p1, p2) -> {
                Long t1 = p1.getUpdated_at() != null ? p1.getUpdated_at() : 0L;
                Long t2 = p2.getUpdated_at() != null ? p2.getUpdated_at() : 0L;
                return t2.compareTo(t1);
            });

            filterProducts(etSearch.getText().toString());
        });
    }

    private void setupSearchDebounce() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    debounceHandler.removeCallbacks(searchRunnable);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchRunnable = () -> filterProducts(s.toString());
                debounceHandler.postDelayed(searchRunnable, 300);
            }
        });
    }

    private String removeAccents(String src) {
        if (src == null)
            return "";
        String normalized = Normalizer.normalize(src, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String out = pattern.matcher(normalized).replaceAll("");
        return out.replaceAll("đ", "d").replaceAll("Đ", "d");
    }

    private void filterProducts(String query) {
        String lowerQuery = removeAccents(query.toLowerCase().trim());
        List<Product> filtered = new ArrayList<>();

        for (Product p : allProducts) {
            if (selectedCategoryId != null && !selectedCategoryId.equals(p.getCategory_id())) {
                continue;
            }

            if (currentTab == 1 && p.getStock_quantity() <= 0) {
                continue;
            }
            if (currentTab == 2 && !p.getIs_bundle()) {
                continue;
            }

            String name = p.getProduct_name() != null ? removeAccents(p.getProduct_name().toLowerCase()) : "";
            String code = p.getProduct_code() != null ? removeAccents(p.getProduct_code().toLowerCase()) : "";

            if (lowerQuery.isEmpty() || name.contains(lowerQuery) || code.contains(lowerQuery)) {
                filtered.add(p);
            }
        }

        filtered.sort((p1, p2) -> {
            switch (currentSortMode) {
                case 1: // Cũ nhất
                    Long t1 = p1.getUpdated_at() != null ? p1.getUpdated_at() : 0L;
                    Long t2 = p2.getUpdated_at() != null ? p2.getUpdated_at() : 0L;
                    return t1.compareTo(t2);
                case 2: // Giá tăng dần
                    return Double.compare(p1.getSell_price(), p2.getSell_price());
                case 3: // Giá giảm dần
                    return Double.compare(p2.getSell_price(), p1.getSell_price());
                case 4: // Tên A-Z
                    String n1 = p1.getProduct_name() != null ? p1.getProduct_name().toLowerCase() : "";
                    String n2 = p2.getProduct_name() != null ? p2.getProduct_name().toLowerCase() : "";
                    return n1.compareTo(n2);
                case 5: // Tên Z-A
                    String n3 = p1.getProduct_name() != null ? p1.getProduct_name().toLowerCase() : "";
                    String n4 = p2.getProduct_name() != null ? p2.getProduct_name().toLowerCase() : "";
                    return n4.compareTo(n3);
                case 0: // Mới nhất (default)
                default:
                    Long t3 = p1.getUpdated_at() != null ? p1.getUpdated_at() : 0L;
                    Long t4 = p2.getUpdated_at() != null ? p2.getUpdated_at() : 0L;
                    return t4.compareTo(t3);
            }
        });

        adapter.setProducts(filtered);
    }

    private void showSortDialog() {
        String[] sortOptions = { "Mới cập nhật", "Cũ nhất", "Giá: Thấp đến Cao", "Giá: Cao đến Thấp", "Tên: A-Z",
                "Tên: Z-A" };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sắp xếp sản phẩm")
                .setSingleChoiceItems(sortOptions, currentSortMode, (dialog, which) -> {
                    currentSortMode = which;
                    filterProducts(etSearch.getText().toString());
                    dialog.dismiss();
                })
                .show();
    }

    private void toggleGridView() {
        isGridView = !isGridView;
        if (isGridView) {
            btnGridToggle.setImageResource(android.R.drawable.ic_menu_sort_by_size); // Set list icon to switch back
            rvProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));
        } else {
            btnGridToggle.setImageResource(android.R.drawable.ic_dialog_dialer); // Set grid icon to switch to grid
            rvProducts.setLayoutManager(new LinearLayoutManager(getContext()));
        }
        adapter.setGridView(isGridView);
    }

    private void updateTabStyles() {
        if (getContext() == null)
            return;

        int activeColor = ContextCompat.getColor(getContext(), R.color.colorTabActive);
        int inactiveColor = ContextCompat.getColor(getContext(), R.color.colorTextGrey);
        int transparent = Color.TRANSPARENT;

        // Sản phẩm
        ((TextView) ((ViewGroup) tabProducts).getChildAt(0))
                .setTextColor(currentTab == 0 ? activeColor : inactiveColor);
        ((TextView) ((ViewGroup) tabProducts).getChildAt(0)).setTypeface(null,
                currentTab == 0 ? Typeface.BOLD : Typeface.NORMAL);
        ((ViewGroup) tabProducts).getChildAt(1).setBackgroundColor(currentTab == 0 ? activeColor : transparent);

        // Tồn kho
        ((TextView) ((ViewGroup) tabInventory).getChildAt(0))
                .setTextColor(currentTab == 1 ? activeColor : inactiveColor);
        ((TextView) ((ViewGroup) tabInventory).getChildAt(0)).setTypeface(null,
                currentTab == 1 ? Typeface.BOLD : Typeface.NORMAL);
        ((ViewGroup) tabInventory).getChildAt(1).setBackgroundColor(currentTab == 1 ? activeColor : transparent);

        // Bán kèm
        ((TextView) ((ViewGroup) tabBundle).getChildAt(0)).setTextColor(currentTab == 2 ? activeColor : inactiveColor);
        ((TextView) ((ViewGroup) tabBundle).getChildAt(0)).setTypeface(null,
                currentTab == 2 ? Typeface.BOLD : Typeface.NORMAL);
        ((ViewGroup) tabBundle).getChildAt(1).setBackgroundColor(currentTab == 2 ? activeColor : transparent);
    }

    private void hideKeyboard() {
        if (getActivity() != null && getView() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
            }
        }
    }

    private void loadCategoriesAndPopulatePills() {
        firebaseHelper.seedDefaultCategoriesIfNeeded(() -> {
            firebaseHelper.listenForCategories((value, error) -> {
                if (error != null || value == null)
                    return;

                categoriesList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Category cat = doc.toObject(Category.class);
                    cat.setId(doc.getId());
                    categoriesList.add(cat);
                }

                adapter.setCategories(categoriesList);
                populatePills();
            });
        });
    }

    private void populatePills() {
        if (getContext() == null || llPillsContainer == null)
            return;
        llPillsContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        View pillAllView = inflater.inflate(R.layout.item_category_pill, llPillsContainer, false);
        TextView tvAll = pillAllView.findViewById(R.id.tvPill);
        tvAll.setText("Tất cả");
        if (selectedCategoryId == null) {
            tvAll.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPillActiveBorder));
            tvAll.setBackgroundResource(R.drawable.bg_pill_active);
        } else {
            tvAll.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTextGrey));
            tvAll.setBackgroundResource(R.drawable.bg_pill_inactive);
        }
        tvAll.setOnClickListener(v -> {
            selectedCategoryId = null;
            populatePills();
            filterProducts(etSearch.getText().toString());
        });
        llPillsContainer.addView(pillAllView);

        // 2. Các nhãn lọc danh mục lấy từ Firebase
        for (Category cat : categoriesList) {
            View pillView = inflater.inflate(R.layout.item_category_pill, llPillsContainer, false);
            TextView tvPill = pillView.findViewById(R.id.tvPill);
            tvPill.setText(cat.getCategory_name());

            boolean isActive = cat.getId().equals(selectedCategoryId);
            if (isActive) {
                tvPill.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPillActiveBorder));
                tvPill.setBackgroundResource(R.drawable.bg_pill_active);
            } else {
                tvPill.setTextColor(ContextCompat.getColor(getContext(), R.color.colorTextGrey));
                tvPill.setBackgroundResource(R.drawable.bg_pill_inactive);
            }

            tvPill.setOnClickListener(v -> {
                selectedCategoryId = cat.getId();
                populatePills();
                filterProducts(etSearch.getText().toString());
            });
            llPillsContainer.addView(pillView);
        }
    }

    private void showCategoriesBottomSheet() {
        if (getContext() == null)
            return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_categories, null);
        bottomSheetDialog.setContentView(sheetView);

        ChipGroup chipGroup = sheetView.findViewById(R.id.chipGroupCategories);
        Button btnApply = sheetView.findViewById(R.id.btnApplyCategoryFilter);

        final String[] tempSelectedId = { selectedCategoryId };

        Chip chipAll = new Chip(getContext());
        chipAll.setText("Tất cả");
        chipAll.setCheckable(true);
        chipAll.setChecked(tempSelectedId[0] == null);
        chipAll.setOnClickListener(v -> {
            tempSelectedId[0] = null;
            chipGroup.clearCheck();
            chipAll.setChecked(true);
        });
        chipGroup.addView(chipAll);

        for (Category cat : categoriesList) {
            Chip chip = new Chip(getContext());
            chip.setText(cat.getCategory_name());
            chip.setCheckable(true);
            boolean isChecked = cat.getId().equals(tempSelectedId[0]);
            chip.setChecked(isChecked);
            if (isChecked) {
                chipAll.setChecked(false);
            }

            chip.setOnClickListener(v -> {
                tempSelectedId[0] = cat.getId();
                chipGroup.clearCheck();
                chip.setChecked(true);
            });
            chipGroup.addView(chip);
        }

        btnApply.setOnClickListener(v -> {
            selectedCategoryId = tempSelectedId[0];
            populatePills();
            filterProducts(etSearch.getText().toString());
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void showCategoryManagerDialog() {
        Context context = getContext();
        if (context == null)
            return;

        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_category_manager, null);

        EditText etNewCat = dialogView.findViewById(R.id.et_new_category);
        Button btnAdd = dialogView.findViewById(R.id.btn_add_category);
        LinearLayout listLayout = dialogView.findViewById(R.id.ll_categories_list);

        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        int textColorPrimary = typedValue.data;
        int dividerColor = ContextCompat.getColor(context, R.color.divider);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle("Quản Lý Danh Mục")
                .setView(dialogView)
                .setNegativeButton("Đóng", null)
                .create();

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
                View rowView = LayoutInflater.from(context)
                        .inflate(R.layout.item_category_manage, listLayout, false);

                TextView tvName = rowView.findViewById(R.id.tv_category_name);
                ImageButton btnEdit = rowView.findViewById(R.id.btn_edit_category);
                ImageButton btnDelete = rowView.findViewById(R.id.btn_delete_category);
                View divider = rowView.findViewById(R.id.divider);

                tvName.setText(cat.getCategory_name());
                divider.setBackgroundColor(dividerColor);

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

    private void showDeleteConfirmation(Product product, int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xác Nhận Xóa")
                .setMessage("Bạn có chắc chắn muốn xóa sản phẩm \"" + product.getProduct_name()
                        + "\" không? Thao tác này không thể hoàn tác.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    Log.d("SWIPE_DELETE", "User clicked Positive Button: Xóa");
                    firebaseHelper.deleteProduct(product.getId(), () -> {
                        Toast.makeText(getContext(), "Đã xóa sản phẩm thành công!", Toast.LENGTH_SHORT).show();
                    }, error -> {
                        Toast.makeText(getContext(), "Lỗi xóa sản phẩm: " + error, Toast.LENGTH_SHORT).show();
                        resetSwipe();
                    });
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    Log.d("SWIPE_DELETE", "User clicked Negative Button: Hủy");
                    Toast.makeText(getContext(), "Đã hủy thao tác xóa", Toast.LENGTH_SHORT).show();
                    resetSwipe();
                })
                .setCancelable(false)
                .show();
    }

    private void resetSwipe() {
        if (swipedPosition != -1) {
            RecyclerView.ViewHolder holder = rvProducts.findViewHolderForAdapterPosition(swipedPosition);
            if (holder != null) {
                holder.itemView.animate()
                        .translationX(0f)
                        .translationY(0f)
                        .setDuration(250)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
            swipedPosition = -1;
        }
    }

    private int dpToPx(int dp) {
        if (getContext() == null)
            return dp * 3;
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
