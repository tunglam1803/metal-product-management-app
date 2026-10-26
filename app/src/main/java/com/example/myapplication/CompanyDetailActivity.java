package com.example.myapplication;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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

import com.example.myapplication.adapters.CompanyProductAdapter;
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
    private Spinner activeProductSpinner;

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
                        if (barcode != null && !barcode.isEmpty() && activeProductSpinner != null) {
                            int matchedIndex = -1;
                            for (int i = 0; i < inventoryProducts.size(); i++) {
                                Product p = inventoryProducts.get(i);
                                if (barcode.equalsIgnoreCase(p.getProduct_code())) {
                                    matchedIndex = i;
                                    break;
                                }
                            }
                            if (matchedIndex != -1) {
                                activeProductSpinner.setSelection(matchedIndex);
                                Toast.makeText(this, "Đã khớp sản phẩm: " + barcode, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Không tìm thấy sản phẩm có mã " + barcode + " trong kho", Toast.LENGTH_LONG).show();
                            }
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
        LinearLayout llSelectSection = dialog.findViewById(R.id.llSelectProductSection);
        Spinner spProductPicker = dialog.findViewById(R.id.spProductPicker);
        activeProductSpinner = spProductPicker;
        ImageButton btnScan = dialog.findViewById(R.id.btnScanBarcodePicker);

        TextView tvPickedName = dialog.findViewById(R.id.tvPickedProductName);
        TextView tvPickedCode = dialog.findViewById(R.id.tvPickedProductCode);
        TextView tvPickedStandardPrice = dialog.findViewById(R.id.tvPickedStandardPrice);

        TextInputEditText etCustomPrice = dialog.findViewById(R.id.etCustomPriceInput);
        TextInputEditText etNote = dialog.findViewById(R.id.etProductNoteInput);

        Button btnCancel = dialog.findViewById(R.id.btnCancelPicker);
        Button btnSave = dialog.findViewById(R.id.btnSaveCustomPrice);

        final Product[] selectedProductHolder = new Product[1];
        boolean isEditing = productToEdit != null;

        if (isEditing) {
            tvTitle.setText("Sửa giá bán cho công ty");
            llSelectSection.setVisibility(View.GONE); // Can't change the underlying product when just editing price

            tvPickedName.setText(productToEdit.getProduct_name());
            tvPickedCode.setText("Mã: " + productToEdit.getProduct_code());
            tvPickedStandardPrice.setText("Giá niêm yết: " + productToEdit.getFormattedStandardPrice());

            long customPriceLong = Math.round(productToEdit.getCustom_price());
            etCustomPrice.setText(String.valueOf(customPriceLong));
            etNote.setText(productToEdit.getNote());
            btnSave.setText("Cập nhật giá");
        } else {
            tvTitle.setText("Thêm mặt hàng & Giá riêng");
            llSelectSection.setVisibility(View.VISIBLE);
            btnSave.setText("Lưu giá riêng");

            // Build product spinner items
            List<String> productLabels = new ArrayList<>();
            if (inventoryProducts.isEmpty()) {
                productLabels.add("Chưa có sản phẩm nào trong kho");
                selectedProductHolder[0] = null;
            } else {
                for (Product p : inventoryProducts) {
                    String code = p.getProduct_code() != null ? "[" + p.getProduct_code() + "] " : "";
                    productLabels.add(code + p.getProduct_name());
                }

                // Pre-populate with first product immediately
                selectedProductHolder[0] = inventoryProducts.get(0);
                Product chosen = selectedProductHolder[0];
                tvPickedName.setText(chosen.getProduct_name());
                tvPickedCode.setText("Mã: " + (chosen.getProduct_code() != null ? chosen.getProduct_code() : "--"));
                tvPickedStandardPrice.setText("Giá niêm yết: " + currencyFormat.format(chosen.getSell_price()) + " đ");
                etCustomPrice.setText(String.valueOf(Math.round(chosen.getSell_price())));
            }

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, productLabels);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spProductPicker.setAdapter(spinnerAdapter);

            spProductPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position >= 0 && position < inventoryProducts.size()) {
                        Product chosen = inventoryProducts.get(position);
                        selectedProductHolder[0] = chosen;

                        tvPickedName.setText(chosen.getProduct_name());
                        tvPickedCode.setText("Mã: " + (chosen.getProduct_code() != null ? chosen.getProduct_code() : "--"));
                        tvPickedStandardPrice.setText("Giá niêm yết: " + currencyFormat.format(chosen.getSell_price()) + " đ");
                        etCustomPrice.setText(String.valueOf(Math.round(chosen.getSell_price())));
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            btnScan.setOnClickListener(v -> {
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
                customPrice = Double.parseDouble(priceStr);
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
            } else {
                Product chosenProduct = selectedProductHolder[0];
                if (chosenProduct == null) {
                    Toast.makeText(CompanyDetailActivity.this, "Vui lòng chọn sản phẩm", Toast.LENGTH_SHORT).show();
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
            activeProductSpinner = null;
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
