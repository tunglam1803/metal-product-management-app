package com.example.myapplication;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.myapplication.adapters.CompanyProductAdapter;
import com.example.myapplication.adapters.ProductPickerAdapter;
import com.example.myapplication.helpers.FirebaseHelper;
import com.example.myapplication.models.Company;
import com.example.myapplication.models.CompanyProduct;
import com.example.myapplication.models.Product;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class CompanyDetailActivity extends AppCompatActivity {

    private interface OnBarcodeScannedListener {
        void onScanned(String barcode);
    }

    private String companyId;
    private String companyName;
    private String companyPhone;
    private String companyAddress;
    private String companyNote;

    private TextView tvCompanyNameHeader;
    private TextView tvCompanyPhoneMeta, tvCompanyAddressMeta, tvCompanyNoteMeta, tvItemsCountMeta;
    private TextView tvFilterCount;
    private EditText etSearchProduct;
    private ImageButton btnClearSearch;
    private LinearLayout llEmptyState;
    private TextView tvEmptyTitle, tvEmptySubtitle;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvCompanyProducts;

    private CompanyProductAdapter adapter;
    private final List<CompanyProduct> allCompanyProducts = new ArrayList<>();
    private final List<CompanyProduct> filteredCompanyProducts = new ArrayList<>();
    private final List<Product> inventoryProducts = new ArrayList<>();

    private FirebaseHelper firebaseHelper;
    private ListenerRegistration companyProductsListener;
    private ListenerRegistration inventoryListener;

    private ActivityResultLauncher<Intent> barcodeScannerLauncher;
    private ActivityResultLauncher<Intent> dialogBarcodeScannerLauncher;
    private Dialog activePickerDialog;
    private OnBarcodeScannedListener activeDialogBarcodeListener;

    private final DecimalFormat currencyFormat;

    public CompanyDetailActivity() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        this.currencyFormat = new DecimalFormat("#,###", symbols);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_detail);

        firebaseHelper = new FirebaseHelper();

        companyId = getIntent().getStringExtra("COMPANY_ID");
        companyName = getIntent().getStringExtra("COMPANY_NAME");
        companyPhone = getIntent().getStringExtra("COMPANY_PHONE");
        companyAddress = getIntent().getStringExtra("COMPANY_ADDRESS");
        companyNote = getIntent().getStringExtra("COMPANY_NOTE");

        if (companyId == null || companyId.isEmpty()) {
            Toast.makeText(this, "Thiếu thông tin công ty", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initScannerLaunchers();
        initViews();
        setupRecyclerView();
        setupSearch();
        loadInventoryProducts();
        loadCompanyProducts();
    }

    private void initScannerLaunchers() {
        // Scanner for main search bar
        barcodeScannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String barcode = result.getData().getStringExtra("SCANNED_BARCODE");
                        if (barcode != null && !barcode.isEmpty()) {
                            etSearchProduct.setText(barcode);
                            Toast.makeText(this, "Đã quét: " + barcode, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Scanner for Add/Edit product dialog
        dialogBarcodeScannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String barcode = result.getData().getStringExtra("SCANNED_BARCODE");
                        if (barcode != null && !barcode.trim().isEmpty() && activeDialogBarcodeListener != null) {
                            activeDialogBarcodeListener.onScanned(barcode.trim());
                        }
                    }
                });
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnCallCompany = findViewById(R.id.btnCallCompany);
        ImageButton btnEditCompany = findViewById(R.id.btnEditCompany);
        tvCompanyNameHeader = findViewById(R.id.tvCompanyNameHeader);

        tvCompanyPhoneMeta = findViewById(R.id.tvCompanyPhoneMeta);
        tvCompanyAddressMeta = findViewById(R.id.tvCompanyAddressMeta);
        tvCompanyNoteMeta = findViewById(R.id.tvCompanyNoteMeta);
        tvItemsCountMeta = findViewById(R.id.tvItemsCountMeta);
        tvFilterCount = findViewById(R.id.tvFilterCount);

        etSearchProduct = findViewById(R.id.etSearchProduct);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        ImageButton btnScanSearch = findViewById(R.id.btnScanSearch);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvCompanyProducts = findViewById(R.id.rvCompanyProducts);
        llEmptyState = findViewById(R.id.llEmptyState);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        Button btnEmptyAddProduct = findViewById(R.id.btnEmptyAddProduct);
        ExtendedFloatingActionButton fabAddProduct = findViewById(R.id.fabAddProduct);

        updateCompanyMetaViews();

        btnBack.setOnClickListener(v -> finish());

        btnCallCompany.setOnClickListener(v -> {
            if (companyPhone != null && !companyPhone.trim().isEmpty()) {
                try {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + companyPhone.trim()));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Không thể mở ứng dụng gọi điện", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Công ty này chưa có số điện thoại", Toast.LENGTH_SHORT).show();
            }
        });

        btnEditCompany.setOnClickListener(v -> showEditCompanyDialog());
        fabAddProduct.setOnClickListener(v -> showAddProductDialog(null));
        btnEmptyAddProduct.setOnClickListener(v -> showAddProductDialog(null));

        btnScanSearch.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScannerActivity.class);
            barcodeScannerLauncher.launch(intent);
        });

        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(this::loadCompanyProducts);
    }

    private void updateCompanyMetaViews() {
        tvCompanyNameHeader.setText(companyName != null ? companyName : "Chi tiết công ty");

        if (companyPhone != null && !companyPhone.trim().isEmpty()) {
            tvCompanyPhoneMeta.setText("SĐT: " + companyPhone);
            tvCompanyPhoneMeta.setVisibility(View.VISIBLE);
        } else {
            tvCompanyPhoneMeta.setText("SĐT: Chưa có");
        }

        if (companyAddress != null && !companyAddress.trim().isEmpty()) {
            tvCompanyAddressMeta.setText("Địa chỉ: " + companyAddress);
            tvCompanyAddressMeta.setVisibility(View.VISIBLE);
        } else {
            tvCompanyAddressMeta.setVisibility(View.GONE);
        }

        if (companyNote != null && !companyNote.trim().isEmpty()) {
            tvCompanyNoteMeta.setText("Ghi chú: " + companyNote);
            tvCompanyNoteMeta.setVisibility(View.VISIBLE);
        } else {
            tvCompanyNoteMeta.setVisibility(View.GONE);
        }
    }

    private void setupRecyclerView() {
        rvCompanyProducts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CompanyProductAdapter(
                this,
                filteredCompanyProducts,
                product -> {
                    // Navigate to product detail if productId is available
                    if (product.getProduct_id() != null && !product.getProduct_id().isEmpty()) {
                        Intent intent = new Intent(CompanyDetailActivity.this, ProductDetailActivity.class);
                        intent.putExtra("PRODUCT_ID", product.getProduct_id());
                        startActivity(intent);
                    }
                },
                new CompanyProductAdapter.OnProductActionListener() {
                    @Override
                    public void onEditPrice(CompanyProduct product) {
                        showAddProductDialog(product);
                    }

                    @Override
                    public void onDeleteProduct(CompanyProduct product) {
                        showDeleteProductConfirmation(product);
                    }
                }
        );
        rvCompanyProducts.setAdapter(adapter);
    }

    private void setupSearch() {
        btnClearSearch.setOnClickListener(v -> etSearchProduct.setText(""));

        etSearchProduct.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                String query = s.toString();
                btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                filterProducts(query);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadInventoryProducts() {
        inventoryListener = firebaseHelper.listenForProducts((value, error) -> {
            if (error != null || value == null) return;
            inventoryProducts.clear();
            for (QueryDocumentSnapshot doc : value) {
                Product p = doc.toObject(Product.class);
                p.setId(doc.getId());
                inventoryProducts.add(p);
            }
        });
    }

    private void loadCompanyProducts() {
        if (companyProductsListener != null) {
            companyProductsListener.remove();
        }

        companyProductsListener = firebaseHelper.listenForCompanyProducts(companyId, (value, error) -> {
            swipeRefreshLayout.setRefreshing(false);
            if (error != null) {
                Toast.makeText(this, "Lỗi tải mặt hàng của công ty: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (value == null) return;

            allCompanyProducts.clear();
            for (QueryDocumentSnapshot doc : value) {
                CompanyProduct cp = doc.toObject(CompanyProduct.class);
                cp.setId(doc.getId());
                allCompanyProducts.add(cp);
            }

            // Sort by latest updated
            allCompanyProducts.sort((p1, p2) -> {
                Long t1 = p1.getUpdated_at() != null ? p1.getUpdated_at() : 0L;
                Long t2 = p2.getUpdated_at() != null ? p2.getUpdated_at() : 0L;
                return t2.compareTo(t1);
            });

            tvItemsCountMeta.setText(allCompanyProducts.size() + " mặt hàng hay mua");
            filterProducts(etSearchProduct.getText().toString());
        });
    }

    private void filterProducts(String query) {
        filteredCompanyProducts.clear();
        String trimmed = query != null ? query.trim() : "";

        if (trimmed.isEmpty()) {
            filteredCompanyProducts.addAll(allCompanyProducts);
        } else {
            String normalizedQuery = removeDiacritics(trimmed.toLowerCase());
            for (CompanyProduct cp : allCompanyProducts) {
                String codeNorm = removeDiacritics(cp.getProduct_code().toLowerCase());
                String nameNorm = removeDiacritics(cp.getProduct_name().toLowerCase());
                String noteNorm = removeDiacritics(cp.getNote().toLowerCase());

                // Search by code or product name as requested: "tìm kiếm theo mã, Tên hàng"
                if (codeNorm.contains(normalizedQuery) || nameNorm.contains(normalizedQuery) || noteNorm.contains(normalizedQuery)) {
                    filteredCompanyProducts.add(cp);
                }
            }
        }

        adapter.updateList(filteredCompanyProducts);
        tvFilterCount.setText(filteredCompanyProducts.size() + " kết quả");

        if (filteredCompanyProducts.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            if (allCompanyProducts.isEmpty()) {
                tvEmptyTitle.setText("Chưa có mặt hàng nào");
                tvEmptySubtitle.setText("Lưu các mặt hàng công ty này hay mua cùng giá bán riêng để các lần sau khớp giá chính xác");
            } else {
                tvEmptyTitle.setText("Không tìm thấy mặt hàng");
                tvEmptySubtitle.setText("Không có mặt hàng nào khớp với mã hoặc tên: \"" + query + "\"");
            }
        } else {
            llEmptyState.setVisibility(View.GONE);
        }
    }

    private void showAddProductDialog(CompanyProduct productToEdit) {
        Dialog dialog = new Dialog(this);
        activePickerDialog = dialog;
        dialog.setContentView(R.layout.dialog_company_product_picker);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvPickerTitle);
        TextView tvSubtitle = dialog.findViewById(R.id.tvPickerSubtitle);

        LinearLayout llSearchAndPickSection = dialog.findViewById(R.id.llSearchAndPickSection);
        TextView tvPickerSearchLabel = dialog.findViewById(R.id.tvPickerSearchLabel);
        LinearLayout llSearchBarContainer = dialog.findViewById(R.id.llSearchBarContainer);
        EditText etPickerSearch = dialog.findViewById(R.id.etPickerSearch);
        ImageButton btnClearPickerSearch = dialog.findViewById(R.id.btnClearPickerSearch);
        ImageButton btnScanBarcodePicker = dialog.findViewById(R.id.btnScanBarcodePicker);
        RecyclerView rvPickerSearchResults = dialog.findViewById(R.id.rvPickerSearchResults);

        View cardNotFoundAction = dialog.findViewById(R.id.cardNotFoundAction);
        TextView tvNotFoundActionTitle = dialog.findViewById(R.id.tvNotFoundActionTitle);
        TextView tvNotFoundActionSubtitle = dialog.findViewById(R.id.tvNotFoundActionSubtitle);

        View cardSelectedProduct = dialog.findViewById(R.id.cardSelectedProduct);
        ImageView ivPickedProductImage = dialog.findViewById(R.id.ivPickedProductImage);
        TextView tvPickedProductName = dialog.findViewById(R.id.tvPickedProductName);
        TextView tvPickedProductCode = dialog.findViewById(R.id.tvPickedProductCode);
        TextView tvPickedStandardPrice = dialog.findViewById(R.id.tvPickedStandardPrice);
        TextView btnChangeSelectedProduct = dialog.findViewById(R.id.btnChangeSelectedProduct);

        LinearLayout llCreateNewProductSection = dialog.findViewById(R.id.llCreateNewProductSection);
        TextView btnCancelCreateNewMode = dialog.findViewById(R.id.btnCancelCreateNewMode);
        TextInputEditText etNewProductName = dialog.findViewById(R.id.etNewProductName);
        TextInputEditText etNewProductCode = dialog.findViewById(R.id.etNewProductCode);
        ImageButton btnScanNewProductCode = dialog.findViewById(R.id.btnScanNewProductCode);
        TextInputEditText etNewProductStandardPrice = dialog.findViewById(R.id.etNewProductStandardPrice);

        TextInputEditText etCustomPrice = dialog.findViewById(R.id.etCustomPriceInput);
        TextInputEditText etNote = dialog.findViewById(R.id.etProductNoteInput);

        setupCurrencyFormatting(etCustomPrice);
        setupCurrencyFormatting(etNewProductStandardPrice);

        Button btnCancel = dialog.findViewById(R.id.btnCancelPicker);
        Button btnSave = dialog.findViewById(R.id.btnSaveCustomPrice);

        final Product[] selectedProductHolder = new Product[1];
        final boolean[] isCreateNewMode = new boolean[]{false};
        boolean isEditing = productToEdit != null;

        if (isEditing) {
            tvTitle.setText("Sửa giá bán cho công ty");
            if (tvSubtitle != null) {
                tvSubtitle.setText("Điều chỉnh giá bán riêng hoặc ghi chú cho mặt hàng này");
            }
            llSearchAndPickSection.setVisibility(View.VISIBLE);
            tvPickerSearchLabel.setVisibility(View.GONE);
            llSearchBarContainer.setVisibility(View.GONE);
            rvPickerSearchResults.setVisibility(View.GONE);
            cardNotFoundAction.setVisibility(View.GONE);
            llCreateNewProductSection.setVisibility(View.GONE);

            cardSelectedProduct.setVisibility(View.VISIBLE);
            btnChangeSelectedProduct.setVisibility(View.GONE);

            tvPickedProductName.setText(productToEdit.getProduct_name());
            tvPickedProductCode.setText("Mã: " + (productToEdit.getProduct_code() != null ? productToEdit.getProduct_code() : "--"));
            tvPickedStandardPrice.setText("• Giá niêm yết: " + productToEdit.getFormattedStandardPrice());

            if (productToEdit.getImage_url() != null && !productToEdit.getImage_url().isEmpty()) {
                Glide.with(this)
                        .load(productToEdit.getImage_url())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(ivPickedProductImage);
            } else {
                ivPickedProductImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            long customPriceLong = Math.round(productToEdit.getCustom_price());
            setCurrencyValue(etCustomPrice, customPriceLong);
            etNote.setText(productToEdit.getNote());
            btnSave.setText("Cập nhật giá");
        } else {
            tvTitle.setText("Thêm mặt hàng & Giá riêng");
            if (tvSubtitle != null) {
                tvSubtitle.setText("Lưu giá bán lần đầu để các lần sau khớp giá chính xác");
            }
            llSearchAndPickSection.setVisibility(View.VISIBLE);
            tvPickerSearchLabel.setVisibility(View.VISIBLE);
            llSearchBarContainer.setVisibility(View.VISIBLE);
            cardSelectedProduct.setVisibility(View.GONE);
            btnChangeSelectedProduct.setVisibility(View.VISIBLE);
            llCreateNewProductSection.setVisibility(View.GONE);
            btnSave.setText("Lưu giá riêng");

            List<Product> searchResults = new ArrayList<>();
            ProductPickerAdapter pickerAdapter = new ProductPickerAdapter(this, searchResults, chosen -> {
                selectedProductHolder[0] = chosen;
                isCreateNewMode[0] = false;

                tvPickedProductName.setText(chosen.getProduct_name());
                tvPickedProductCode.setText("Mã: " + (chosen.getProduct_code() != null ? chosen.getProduct_code() : "--"));
                tvPickedStandardPrice.setText("• Giá niêm yết: " + currencyFormat.format(chosen.getSell_price()) + " đ");

                if (chosen.getImage_url() != null && !chosen.getImage_url().isEmpty()) {
                    Glide.with(this)
                            .load(chosen.getImage_url())
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery)
                            .into(ivPickedProductImage);
                } else {
                    ivPickedProductImage.setImageResource(android.R.drawable.ic_menu_gallery);
                }

                cardSelectedProduct.setVisibility(View.VISIBLE);
                llSearchBarContainer.setVisibility(View.GONE);
                tvPickerSearchLabel.setVisibility(View.GONE);
                rvPickerSearchResults.setVisibility(View.GONE);
                cardNotFoundAction.setVisibility(View.GONE);

                setCurrencyValue(etCustomPrice, Math.round(chosen.getSell_price()));
            });

            rvPickerSearchResults.setLayoutManager(new LinearLayoutManager(this));
            rvPickerSearchResults.setAdapter(pickerAdapter);
            rvPickerSearchResults.setOnTouchListener((v, event) -> {
                v.getParent().requestDisallowInterceptTouchEvent(true);
                if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                }
                return false;
            });

            Runnable filterRunnable = () -> {
                String query = etPickerSearch.getText() != null ? etPickerSearch.getText().toString().trim() : "";
                btnClearPickerSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);

                searchResults.clear();
                if (query.isEmpty()) {
                    int limit = Math.min(inventoryProducts.size(), 50);
                    for (int i = 0; i < limit; i++) {
                        searchResults.add(inventoryProducts.get(i));
                    }
                    rvPickerSearchResults.setVisibility(searchResults.isEmpty() ? View.GONE : View.VISIBLE);
                    cardNotFoundAction.setVisibility(View.VISIBLE);
                    tvNotFoundActionTitle.setText("+ Thêm mới sản phẩm vào kho");
                    tvNotFoundActionSubtitle.setText("Chưa có trong kho? Bấm để tạo mới và lưu giá luôn");
                } else {
                    String normQuery = removeDiacritics(query.toLowerCase());
                    for (Product p : inventoryProducts) {
                        String code = p.getProduct_code() != null ? removeDiacritics(p.getProduct_code().toLowerCase()) : "";
                        String name = p.getProduct_name() != null ? removeDiacritics(p.getProduct_name().toLowerCase()) : "";
                        if (code.contains(normQuery) || name.contains(normQuery)) {
                            searchResults.add(p);
                            if (searchResults.size() >= 50) break;
                        }
                    }
                    if (searchResults.isEmpty()) {
                        rvPickerSearchResults.setVisibility(View.GONE);
                        cardNotFoundAction.setVisibility(View.VISIBLE);
                        tvNotFoundActionTitle.setText("+ Tạo mới \"" + query + "\" vào kho");
                        tvNotFoundActionSubtitle.setText("Không tìm thấy trong kho. Bấm để thêm mới và lưu giá ngay!");
                    } else {
                        rvPickerSearchResults.setVisibility(View.VISIBLE);
                        cardNotFoundAction.setVisibility(View.VISIBLE);
                        tvNotFoundActionTitle.setText("+ Thêm mới \"" + query + "\" vào kho");
                        tvNotFoundActionSubtitle.setText("Không phải các sản phẩm trên? Bấm để tạo mới sản phẩm này");
                    }
                }

                // If > 3 items, constrain dropdown height to 180dp; if 1-3 items, wrap content naturally
                int maxDropdownHeight = Math.round(180 * getResources().getDisplayMetrics().density);
                ViewGroup.LayoutParams params = rvPickerSearchResults.getLayoutParams();
                if (params != null) {
                    if (searchResults.size() > 3) {
                        if (params.height != maxDropdownHeight) {
                            params.height = maxDropdownHeight;
                            rvPickerSearchResults.setLayoutParams(params);
                        }
                    } else {
                        if (params.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                            rvPickerSearchResults.setLayoutParams(params);
                        }
                    }
                }

                pickerAdapter.updateList(searchResults);
            };

            filterRunnable.run();

            btnClearPickerSearch.setOnClickListener(v -> etPickerSearch.setText(""));

            etPickerSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int count, int after) {
                    filterRunnable.run();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            btnChangeSelectedProduct.setOnClickListener(v -> {
                selectedProductHolder[0] = null;
                cardSelectedProduct.setVisibility(View.GONE);
                llSearchBarContainer.setVisibility(View.VISIBLE);
                tvPickerSearchLabel.setVisibility(View.VISIBLE);
                filterRunnable.run();
            });

            cardNotFoundAction.setOnClickListener(v -> {
                isCreateNewMode[0] = true;
                selectedProductHolder[0] = null;
                llSearchAndPickSection.setVisibility(View.GONE);
                llCreateNewProductSection.setVisibility(View.VISIBLE);

                String currentQuery = etPickerSearch.getText() != null ? etPickerSearch.getText().toString().trim() : "";
                if (!currentQuery.isEmpty()) {
                    etNewProductName.setText(currentQuery);
                }
                etNewProductName.requestFocus();
            });

            btnCancelCreateNewMode.setOnClickListener(v -> {
                isCreateNewMode[0] = false;
                llCreateNewProductSection.setVisibility(View.GONE);
                llSearchAndPickSection.setVisibility(View.VISIBLE);
                if (selectedProductHolder[0] != null) {
                    cardSelectedProduct.setVisibility(View.VISIBLE);
                    llSearchBarContainer.setVisibility(View.GONE);
                    tvPickerSearchLabel.setVisibility(View.GONE);
                    rvPickerSearchResults.setVisibility(View.GONE);
                    cardNotFoundAction.setVisibility(View.GONE);
                } else {
                    cardSelectedProduct.setVisibility(View.GONE);
                    llSearchBarContainer.setVisibility(View.VISIBLE);
                    tvPickerSearchLabel.setVisibility(View.VISIBLE);
                    filterRunnable.run();
                }
            });

            etNewProductStandardPrice.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int count, int after) {
                    if (isCreateNewMode[0]) {
                        String stdStr = s != null ? s.toString().trim() : "";
                        String customStr = etCustomPrice.getText() != null ? etCustomPrice.getText().toString().trim() : "";
                        if (customStr.isEmpty() && !stdStr.isEmpty()) {
                            etCustomPrice.setText(stdStr);
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            btnScanBarcodePicker.setOnClickListener(v -> {
                activeDialogBarcodeListener = barcode -> {
                    Product matched = null;
                    for (Product p : inventoryProducts) {
                        if (barcode.equalsIgnoreCase(p.getProduct_code())) {
                            matched = p;
                            break;
                        }
                    }
                    if (matched != null) {
                        selectedProductHolder[0] = matched;
                        isCreateNewMode[0] = false;

                        tvPickedProductName.setText(matched.getProduct_name());
                        tvPickedProductCode.setText("Mã: " + (matched.getProduct_code() != null ? matched.getProduct_code() : "--"));
                        tvPickedStandardPrice.setText("• Giá niêm yết: " + currencyFormat.format(matched.getSell_price()) + " đ");

                        if (matched.getImage_url() != null && !matched.getImage_url().isEmpty()) {
                            Glide.with(CompanyDetailActivity.this)
                                    .load(matched.getImage_url())
                                    .placeholder(android.R.drawable.ic_menu_gallery)
                                    .error(android.R.drawable.ic_menu_gallery)
                                    .into(ivPickedProductImage);
                        } else {
                            ivPickedProductImage.setImageResource(android.R.drawable.ic_menu_gallery);
                        }

                        cardSelectedProduct.setVisibility(View.VISIBLE);
                        llSearchBarContainer.setVisibility(View.GONE);
                        tvPickerSearchLabel.setVisibility(View.GONE);
                        rvPickerSearchResults.setVisibility(View.GONE);
                        cardNotFoundAction.setVisibility(View.GONE);

                        setCurrencyValue(etCustomPrice, Math.round(matched.getSell_price()));
                        Toast.makeText(CompanyDetailActivity.this, "Đã khớp sản phẩm: " + matched.getProduct_name(), Toast.LENGTH_SHORT).show();
                    } else {
                        isCreateNewMode[0] = true;
                        selectedProductHolder[0] = null;
                        llSearchAndPickSection.setVisibility(View.GONE);
                        llCreateNewProductSection.setVisibility(View.VISIBLE);
                        etNewProductCode.setText(barcode);
                        etNewProductName.requestFocus();
                        Toast.makeText(CompanyDetailActivity.this, "Mã " + barcode + " chưa có trong kho. Điền tên để tạo mới!", Toast.LENGTH_LONG).show();
                    }
                };
                Intent intent = new Intent(this, ScannerActivity.class);
                dialogBarcodeScannerLauncher.launch(intent);
            });

            btnScanNewProductCode.setOnClickListener(v -> {
                activeDialogBarcodeListener = barcode -> {
                    etNewProductCode.setText(barcode);
                    Toast.makeText(CompanyDetailActivity.this, "Đã quét mã: " + barcode, Toast.LENGTH_SHORT).show();
                };
                Intent intent = new Intent(this, ScannerActivity.class);
                dialogBarcodeScannerLauncher.launch(intent);
            });
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String priceStr = etCustomPrice.getText() != null ? etCustomPrice.getText().toString().trim() : "";
            String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";

            if (priceStr.isEmpty()) {
                etCustomPrice.setError("Vui lòng nhập giá riêng cho công ty");
                etCustomPrice.requestFocus();
                return;
            }

            double customPrice;
            try {
                String cleanPriceStr = priceStr.replaceAll("[^0-9]", "");
                customPrice = Double.parseDouble(cleanPriceStr);
            } catch (Exception e) {
                etCustomPrice.setError("Giá không hợp lệ");
                return;
            }

            btnSave.setEnabled(false);

            if (isEditing) {
                productToEdit.setCustom_price(customPrice);
                productToEdit.setNote(note);

                firebaseHelper.addOrUpdateCompanyProduct(companyId, productToEdit,
                        () -> {
                            dialog.dismiss();
                            Toast.makeText(CompanyDetailActivity.this, "Đã cập nhật giá riêng!", Toast.LENGTH_SHORT).show();
                        },
                        error -> {
                            btnSave.setEnabled(true);
                            Toast.makeText(CompanyDetailActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                        });
            } else if (isCreateNewMode[0]) {
                String newName = etNewProductName.getText() != null ? etNewProductName.getText().toString().trim() : "";
                String newCode = etNewProductCode.getText() != null ? etNewProductCode.getText().toString().trim() : "";
                String newStdPriceStr = etNewProductStandardPrice.getText() != null ? etNewProductStandardPrice.getText().toString().trim() : "";

                if (newName.isEmpty()) {
                    etNewProductName.setError("Vui lòng nhập tên sản phẩm mới");
                    etNewProductName.requestFocus();
                    btnSave.setEnabled(true);
                    return;
                }

                double standardPrice = customPrice;
                if (!newStdPriceStr.isEmpty()) {
                    try {
                        String cleanStdStr = newStdPriceStr.replaceAll("[^0-9]", "");
                        standardPrice = Double.parseDouble(cleanStdStr);
                    } catch (Exception ignored) {}
                }

                Product newProd = new Product();
                newProd.setProduct_name(newName);
                newProd.setProduct_code(newCode.isEmpty() ? null : newCode);
                newProd.setSell_price(standardPrice);
                newProd.setImport_price(0.0);
                newProd.setStock_quantity(0);
                newProd.setIs_bundle(false);

                double finalStandardPrice = standardPrice;
                firebaseHelper.addProductWithId(newProd, newDocId -> {
                    CompanyProduct cp = new CompanyProduct(
                            companyId,
                            newDocId,
                            newCode,
                            newName,
                            customPrice,
                            finalStandardPrice,
                            note
                    );
                    cp.setCompany_name(companyName);

                    firebaseHelper.addOrUpdateCompanyProduct(companyId, cp,
                            () -> {
                                dialog.dismiss();
                                Toast.makeText(CompanyDetailActivity.this, "Đã tạo sản phẩm mới vào kho và lưu giá riêng!", Toast.LENGTH_SHORT).show();
                            },
                            err -> {
                                btnSave.setEnabled(true);
                                Toast.makeText(CompanyDetailActivity.this, "Lỗi lưu giá công ty: " + err, Toast.LENGTH_SHORT).show();
                            });
                }, err -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(CompanyDetailActivity.this, "Lỗi tạo sản phẩm mới: " + err, Toast.LENGTH_SHORT).show();
                });
            } else {
                Product chosenProduct = selectedProductHolder[0];
                if (chosenProduct == null) {
                    Toast.makeText(CompanyDetailActivity.this, "Vui lòng chọn sản phẩm hoặc bấm 'Thêm mới'", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    return;
                }

                CompanyProduct newCP = new CompanyProduct(
                        companyId,
                        chosenProduct.getId(),
                        chosenProduct.getProduct_code(),
                        chosenProduct.getProduct_name(),
                        customPrice,
                        chosenProduct.getSell_price(),
                        note
                );
                newCP.setImage_url(chosenProduct.getImage_url());
                newCP.setCompany_name(companyName);

                firebaseHelper.addOrUpdateCompanyProduct(companyId, newCP,
                        () -> {
                            dialog.dismiss();
                            Toast.makeText(CompanyDetailActivity.this, "Đã lưu giá riêng cho mặt hàng!", Toast.LENGTH_SHORT).show();
                        },
                        error -> {
                            btnSave.setEnabled(true);
                            Toast.makeText(CompanyDetailActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                        });
            }
        });

        dialog.setOnDismissListener(d -> {
            activePickerDialog = null;
            activeDialogBarcodeListener = null;
        });

        dialog.show();
    }

    private void showDeleteProductConfirmation(CompanyProduct product) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa mặt hàng khỏi công ty")
                .setMessage("Bạn có chắc chắn muốn xóa mặt hàng '" + product.getProduct_name() + "' khỏi danh sách thường mua của " + companyName + " không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    firebaseHelper.deleteCompanyProduct(companyId, product.getId(),
                            () -> Toast.makeText(CompanyDetailActivity.this, "Đã xóa mặt hàng thành công", Toast.LENGTH_SHORT).show(),
                            error -> Toast.makeText(CompanyDetailActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditCompanyDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_company);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        TextInputEditText etName = dialog.findViewById(R.id.etCompanyName);
        TextInputEditText etPhone = dialog.findViewById(R.id.etCompanyPhone);
        TextInputEditText etAddress = dialog.findViewById(R.id.etCompanyAddress);
        TextInputEditText etNote = dialog.findViewById(R.id.etCompanyNote);
        Button btnCancel = dialog.findViewById(R.id.btnCancelCompany);
        Button btnSave = dialog.findViewById(R.id.btnSaveCompany);

        tvTitle.setText("Chỉnh sửa công ty");
        etName.setText(companyName);
        etPhone.setText(companyPhone);
        etAddress.setText(companyAddress);
        etNote.setText(companyNote);
        btnSave.setText("Cập nhật");

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
            String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
            String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";

            if (name.isEmpty()) {
                etName.setError("Vui lòng nhập tên công ty");
                return;
            }

            Company updated = new Company(name, phone, address, note);
            updated.setId(companyId);

            firebaseHelper.updateCompany(companyId, updated,
                    () -> {
                        dialog.dismiss();
                        companyName = name;
                        companyPhone = phone;
                        companyAddress = address;
                        companyNote = note;
                        updateCompanyMetaViews();
                        Toast.makeText(CompanyDetailActivity.this, "Đã cập nhật thông tin công ty!", Toast.LENGTH_SHORT).show();
                    },
                    error -> Toast.makeText(CompanyDetailActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }

    private String formatCurrencyText(long amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(amount);
    }

    private void setCurrencyValue(EditText editText, long value) {
        if (editText == null) return;
        editText.setText(value > 0 ? formatCurrencyText(value) : (value == 0 ? "0" : ""));
        if (editText.getText() != null) {
            editText.setSelection(editText.getText().length());
        }
    }

    private void setupCurrencyFormatting(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (str.equals(current)) return;
                editText.removeTextChangedListener(this);

                String cleanString = str.replaceAll("[^0-9]", "");
                if (cleanString.isEmpty()) {
                    current = "";
                    editText.setText("");
                } else {
                    try {
                        long parsed = Long.parseLong(cleanString);
                        String formatted = formatCurrencyText(parsed);
                        current = formatted;
                        editText.setText(formatted);
                        editText.setSelection(formatted.length());
                    } catch (NumberFormatException e) {
                        editText.setText(current);
                        editText.setSelection(current.length());
                    }
                }

                editText.addTextChangedListener(this);
            }
        });
    }

    private String removeDiacritics(String str) {
        if (str == null) return "";
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (companyProductsListener != null) {
            companyProductsListener.remove();
        }
        if (inventoryListener != null) {
            inventoryListener.remove();
        }
    }
}
