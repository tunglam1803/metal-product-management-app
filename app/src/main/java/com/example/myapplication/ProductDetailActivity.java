package com.example.myapplication;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.myapplication.adapters.PriceHistoryAdapter;
import com.example.myapplication.helpers.FirebaseHelper;
import com.example.myapplication.models.Category;
import com.example.myapplication.models.PriceHistory;
import com.example.myapplication.models.Product;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProductDetailActivity extends AppCompatActivity {

    private EditText etProductCode, etProductName, etImportPrice, etSellPrice, etStockQuantity, etNote;
    private SwitchMaterial swIsBundle;
    private Button btnSave, btnEdit, btnViewHistory;
    private TextView tvLastImportInfo;
    private ImageView imgPreview;
    private View btnPickImage;
    private TextView tvUploadStatus;
    private FirebaseHelper firebaseHelper;
    private String productId;
    private Product currentProduct;
    private String uploadedImageUrl = null;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private Uri cameraPhotoUri;
    private Spinner spCategory;
    private final List<Category> categoriesList = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    private ImageButton btnScanCode;
    private ActivityResultLauncher<Intent> barcodeScannerLauncher;
    private boolean isEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        firebaseHelper = new FirebaseHelper();
        productId = getIntent().getStringExtra("PRODUCT_ID");

        initViews();
        setupImagePicker();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        loadCategories();

        if (productId != null) {
            toolbar.setTitle("Chi Tiết Sản Phẩm");
            isEditMode = false;
            loadProductDetails();
            btnViewHistory.setVisibility(View.VISIBLE);
        } else {
            toolbar.setTitle("Thêm Sản Phẩm");
            isEditMode = true;
            String prefilledCode = getIntent().getStringExtra("PRODUCT_CODE");
            if (prefilledCode != null && !prefilledCode.isEmpty()) {
                etProductCode.setText(prefilledCode);
            }
        }
        
        setEditMode(isEditMode);

        barcodeScannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String barcode = result.getData().getStringExtra("SCANNED_BARCODE");
                        if (barcode != null && !barcode.isEmpty()) {
                            etProductCode.setText(barcode);
                            Toast.makeText(this, "Đã quét và áp dụng: " + barcode, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        btnScanCode.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScannerActivity.class);
            barcodeScannerLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> handleSaveClick());
        btnEdit.setOnClickListener(v -> {
            isEditMode = true;
            setEditMode(true);
            toolbar.setTitle("Sửa Sản Phẩm");
        });
        btnViewHistory.setOnClickListener(v -> showPriceHistory());
        imgPreview.setOnClickListener(v -> showFullScreenImage());

        // Setup currency formatting for price fields
        setupCurrencyFormatting(etImportPrice);
        setupCurrencyFormatting(etSellPrice);
    }

    private void showFullScreenImage() {
        if (imgPreview.getDrawable() == null)
            return;

        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_screen_image);

        ImageView imgFullScreen = dialog.findViewById(R.id.imgFullScreen);
        View btnClose = dialog.findViewById(R.id.btnClose);

        imgFullScreen.setImageDrawable(imgPreview.getDrawable());

        btnClose.setOnClickListener(v -> dialog.dismiss());
        imgFullScreen.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void initViews() {
        etProductCode = findViewById(R.id.etProductCode);
        etProductName = findViewById(R.id.etProductName);
        etImportPrice = findViewById(R.id.etImportPrice);
        etSellPrice = findViewById(R.id.etSellPrice);
        etStockQuantity = findViewById(R.id.etStockQuantity);
        etNote = findViewById(R.id.etNote);
        swIsBundle = findViewById(R.id.swIsBundle);
        btnSave = findViewById(R.id.btnSave);
        btnEdit = findViewById(R.id.btnEdit);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        tvLastImportInfo = findViewById(R.id.tvLastImportInfo);
        imgPreview = findViewById(R.id.imgPreview);
        btnPickImage = findViewById(R.id.btnPickImage);
        tvUploadStatus = findViewById(R.id.tvUploadStatus);
        spCategory = findViewById(R.id.spCategory);
        btnScanCode = findViewById(R.id.btnScanCode);
    }
    
    private void setEditMode(boolean edit) {
        etProductCode.setEnabled(edit);
        etProductCode.setFocusableInTouchMode(edit);
        etProductName.setEnabled(edit);
        etProductName.setFocusableInTouchMode(edit);
        etImportPrice.setEnabled(edit);
        etImportPrice.setFocusableInTouchMode(edit);
        etSellPrice.setEnabled(edit);
        etSellPrice.setFocusableInTouchMode(edit);
        etStockQuantity.setEnabled(edit);
        etStockQuantity.setFocusableInTouchMode(edit);
        etNote.setEnabled(edit);
        etNote.setFocusableInTouchMode(edit);
        
        // Keep switch visually clear but disable interaction in view mode
        swIsBundle.setClickable(edit);
        swIsBundle.setOnTouchListener(edit ? null : (v, event) -> true);
        
        spCategory.setEnabled(edit);
        
        btnPickImage.setVisibility(edit ? View.VISIBLE : View.GONE);
        btnScanCode.setVisibility(edit ? View.VISIBLE : View.GONE);
        
        if (productId != null) {
            btnEdit.setVisibility(edit ? View.GONE : View.VISIBLE);
            btnSave.setVisibility(edit ? View.VISIBLE : View.GONE);
        } else {
            btnEdit.setVisibility(View.GONE);
            btnSave.setVisibility(View.VISIBLE);
        }
    }

    private void loadProductDetails() {
        firebaseHelper.getProduct(productId, documentSnapshot -> {
            currentProduct = documentSnapshot.toObject(Product.class);
            if (currentProduct != null) {
                currentProduct.setId(documentSnapshot.getId());
                etProductCode.setText(currentProduct.getProduct_code());
                etProductName.setText(currentProduct.getProduct_name());
                setCurrencyValue(etImportPrice, currentProduct.getImport_price().longValue());
                setCurrencyValue(etSellPrice, currentProduct.getSell_price().longValue());
                etStockQuantity.setText(String.valueOf(currentProduct.getStock_quantity()));
                if (currentProduct.getNote() != null) {
                    etNote.setText(currentProduct.getNote());
                }
                swIsBundle.setChecked(currentProduct.getIs_bundle());
                uploadedImageUrl = currentProduct.getImage_url();

                selectCurrentProductCategory();
                loadLastImportInfo();

                if (uploadedImageUrl != null && !uploadedImageUrl.isEmpty()) {
                    imgPreview.setImageTintList(null);

                    if (uploadedImageUrl.contains("/object/public/")) {
                        uploadedImageUrl = uploadedImageUrl.replace("/object/public/", "/object/");
                    }

                    GlideUrl glideUrl = new GlideUrl(
                            uploadedImageUrl,
                            new LazyHeaders.Builder()
                                    .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                                    .build());

                    Glide.with(this).load(glideUrl)
                            .centerCrop()
                            .listener(
                                    new RequestListener<Drawable>() {
                                        @Override
                                        public boolean onLoadFailed(
                                                @Nullable GlideException e,
                                                Object model,
                                                @NonNull Target<Drawable> target,
                                                boolean isFirstResource) {
                                            runOnUiThread(() -> {
                                                Toast.makeText(ProductDetailActivity.this,
                                                        "Lỗi tải ảnh: " + (e != null ? e.getMessage() : "Không rõ"),
                                                        Toast.LENGTH_LONG).show();
                                            });
                                            return false;
                                        }

                                        @Override
                                        public boolean onResourceReady(@NonNull Drawable resource,
                                                @NonNull Object model,
                                                Target<Drawable> target,
                                                @NonNull DataSource dataSource,
                                                boolean isFirstResource) {
                                            runOnUiThread(() -> imgPreview.setPadding(0, 0, 0, 0));
                                            return false;
                                        }
                                    })
                            .into(imgPreview);

                    tvUploadStatus.setText("Hình ảnh sản phẩm");
                }
            }
        });
    }

    private void loadLastImportInfo() {
        if (productId == null) return;
        firebaseHelper.getPriceHistory(productId, (value, error) -> {
            if (error != null || value == null || value.isEmpty()) {
                tvLastImportInfo.setVisibility(View.GONE);
                return;
            }
            
            List<PriceHistory> histories = new ArrayList<>();
            for (QueryDocumentSnapshot doc : value) {
                PriceHistory h = doc.toObject(PriceHistory.class);
                histories.add(h);
            }
            
            histories.sort((h1, h2) -> {
                Long t1 = h1.getChanged_at() != null ? h1.getChanged_at() : 0L;
                Long t2 = h2.getChanged_at() != null ? h2.getChanged_at() : 0L;
                return t2.compareTo(t1);
            });
            
            if (!histories.isEmpty()) {
                PriceHistory latest = histories.get(0);
                long dateInMillis = (latest.getChanged_at() != null ? latest.getChanged_at() : 0) * 1000;
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String dateString = sdf.format(new Date(dateInMillis));
                
                String info = "Giá nhập mới nhất: " + formatCurrencyText(latest.getNew_import_price().longValue()) + " đ — Ngày: " + dateString;
                tvLastImportInfo.setText(info);
                tvLastImportInfo.setVisibility(View.VISIBLE);
            } else {
                tvLastImportInfo.setVisibility(View.GONE);
            }
        });
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        imgPreview.setImageTintList(null);
                        imgPreview.setImageURI(imageUri);
                        imgPreview.setPadding(0, 0, 0, 0);
                        uploadImageToSupabase(imageUri);
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success && cameraPhotoUri != null) {
                        imgPreview.setImageTintList(null);
                        imgPreview.setImageURI(cameraPhotoUri);
                        imgPreview.setPadding(0, 0, 0, 0);
                        uploadImageToSupabase(cameraPhotoUri);
                    }
                });

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(this, "Bạn cần cấp quyền Camera để chụp ảnh!", Toast.LENGTH_SHORT).show();
                    }
                });

        btnPickImage.setOnClickListener(v -> showImageSourceChooser());
    }

    private void openCamera() {
        try {
            File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "product_" + System.currentTimeMillis() + ".jpg");
            cameraPhotoUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(cameraPhotoUri);
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showImageSourceChooser() {
        String[] options = {"Chụp ảnh", "Chọn từ thư viện"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Chọn ảnh sản phẩm")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Camera
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            openCamera();
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                        }
                    } else {
                        // Gallery
                        Intent intent = new Intent(Intent.ACTION_PICK);
                        intent.setType("image/*");
                        imagePickerLauncher.launch(intent);
                    }
                })
                .show();
    }

    private void uploadImageToSupabase(Uri uri) {
        tvUploadStatus.setText("Đang tải ảnh lên...");
        btnSave.setEnabled(false);

        String BUCKET = "uploads";
        String fileName = "product_" + System.currentTimeMillis() + ".jpg";
        String uploadUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + fileName;

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[4096];
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] imageBytes = buffer.toByteArray();

            RequestBody body = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));
            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                    .put(body)
                    .build();

            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        tvUploadStatus.setText("Lỗi tải ảnh!");
                        btnSave.setEnabled(true);
                        Toast.makeText(ProductDetailActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        if (response.isSuccessful()) {
                            uploadedImageUrl = BuildConfig.SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/"
                                    + fileName;
                            tvUploadStatus.setText("Tải ảnh hoàn tất!");
                        } else {
                            tvUploadStatus.setText("Lỗi máy chủ!");
                        }
                    });
                }
            });
        } catch (Exception e) {
            tvUploadStatus.setText("Lỗi chuẩn bị!");
            btnSave.setEnabled(true);
        }
    }

    private void handleSaveClick() {
        String sImport = etImportPrice.getText().toString().trim();
        double newImportPrice = sImport.isEmpty() ? 0 : Double.parseDouble(sImport.replace(".", ""));

        String sSell = etSellPrice.getText().toString().trim();
        double newSellPrice = sSell.isEmpty() ? 0 : Double.parseDouble(sSell.replace(".", ""));

        if (productId != null && currentProduct != null) {
            double oldImportPrice = currentProduct.getImport_price() != null ? currentProduct.getImport_price() : 0;
            double oldSellPrice = currentProduct.getSell_price() != null ? currentProduct.getSell_price() : 0;

            boolean importChanged = newImportPrice != oldImportPrice;
            boolean sellChanged = newSellPrice != oldSellPrice;

            if (importChanged || sellChanged) {
                StringBuilder messageBuilder = new StringBuilder();
                if (importChanged) {
                    String comparison = newImportPrice > oldImportPrice ? "CAO HƠN" : "THẤP HƠN";
                    messageBuilder.append(String.format(Locale.getDefault(), 
                        "• Giá nhập mới (%s đ) %s giá cũ (%s đ).\n",
                        formatCurrencyText((long)newImportPrice),
                        comparison,
                        formatCurrencyText((long)oldImportPrice)
                    ));
                }

                if (sellChanged) {
                    String comparison = newSellPrice > oldSellPrice ? "CAO HƠN" : "THẤP HƠN";
                    messageBuilder.append(String.format(Locale.getDefault(), 
                        "• Giá bán mới (%s đ) %s giá cũ (%s đ).\n",
                        formatCurrencyText((long)newSellPrice),
                        comparison,
                        formatCurrencyText((long)oldSellPrice)
                    ));
                }

                messageBuilder.append("\nBạn có đồng ý lưu thay đổi này không?");

                new MaterialAlertDialogBuilder(this)
                    .setTitle("Cảnh báo thay đổi giá")
                    .setMessage(messageBuilder.toString())
                    .setPositiveButton("Đồng ý", (dialog, which) -> {
                        saveProduct(newImportPrice, newSellPrice);
                    })
                    .setNegativeButton("Bỏ qua", (dialog, which) -> {
                        // Restore old price
                        if (importChanged) setCurrencyValue(etImportPrice, (long)oldImportPrice);
                        if (sellChanged) setCurrencyValue(etSellPrice, (long)oldSellPrice);
                    })
                    .show();
                return;
            }
        }
        
        saveProduct(newImportPrice, newSellPrice);
    }

    private void saveProduct(double importPrice, double sellPrice) {
        String code = etProductCode.getText().toString().trim();
        String name = etProductName.getText().toString().trim();
        String sStock = etStockQuantity.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (name.isEmpty()) {
            etProductName.setError("Vui lòng nhập tên");
            return;
        }
        int stockQuantity = sStock.isEmpty() ? 0 : Integer.parseInt(sStock);
        boolean isBundle = swIsBundle.isChecked();

        Product p = new Product();
        p.setProduct_code(code);
        p.setProduct_name(name);
        p.setImport_price(importPrice);
        p.setSell_price(sellPrice);
        p.setStock_quantity(stockQuantity);
        p.setNote(note);
        p.setIs_bundle(isBundle);
        p.setImage_url(uploadedImageUrl);

        if (spCategory.getSelectedItemPosition() != AdapterView.INVALID_POSITION
                && spCategory.getSelectedItemPosition() < categoriesList.size()) {
            p.setCategory_id(categoriesList.get(spCategory.getSelectedItemPosition()).getId());
        }

        btnSave.setEnabled(false);
        if (productId == null) {
            firebaseHelper.addProduct(p, () -> {
                Toast.makeText(this, "Thêm thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }, error -> {
                btnSave.setEnabled(true);
                Toast.makeText(this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            });
        } else {
            PriceHistory change = null;
            if (currentProduct != null && (currentProduct.getImport_price() != importPrice
                    || currentProduct.getSell_price() != sellPrice)) {
                change = new PriceHistory(
                        currentProduct.getImport_price(), importPrice,
                        currentProduct.getSell_price(), sellPrice,
                        System.currentTimeMillis() / 1000L,
                        "Cập nhật thông tin");
            }
            firebaseHelper.updateProduct(productId, p, change, () -> {
                Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                finish();
            }, error -> {
                btnSave.setEnabled(true);
                Toast.makeText(this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showPriceHistory() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_price_history, null);
        dialog.setContentView(bottomSheetView);

        RecyclerView rvPriceHistory = bottomSheetView.findViewById(R.id.rvPriceHistory);
        TextView tvEmptyHistory = bottomSheetView.findViewById(R.id.tvEmptyHistory);

        rvPriceHistory.setLayoutManager(new LinearLayoutManager(this));
        PriceHistoryAdapter adapter = new PriceHistoryAdapter();
        rvPriceHistory.setAdapter(adapter);

        firebaseHelper.getPriceHistory(productId, (value, error) -> {
            if (error != null) {
                Toast.makeText(this, "Lỗi lịch sử: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (value == null)
                return;

            List<PriceHistory> histories = new ArrayList<>();
            for (QueryDocumentSnapshot doc : value) {
                PriceHistory h = doc.toObject(PriceHistory.class);
                if (h != null) {
                    h.setId(doc.getId());
                    histories.add(h);
                }
            }

            histories.sort((h1, h2) -> {
                Long t1 = h1.getChanged_at() != null ? h1.getChanged_at() : 0L;
                Long t2 = h2.getChanged_at() != null ? h2.getChanged_at() : 0L;
                return t2.compareTo(t1);
            });

            adapter.setHistories(histories);
            tvEmptyHistory.setVisibility(histories.isEmpty() ? View.VISIBLE : View.GONE);
        });

        dialog.show();
    }

    private void loadCategories() {
        firebaseHelper.seedDefaultCategoriesIfNeeded(() -> {
            firebaseHelper.listenForCategories((value, error) -> {
                if (error != null || value == null)
                    return;

                categoriesList.clear();
                List<String> names = new ArrayList<>();

                for (QueryDocumentSnapshot doc : value) {
                    Category cat = doc.toObject(Category.class);
                    cat.setId(doc.getId());
                    categoriesList.add(cat);
                    names.add(cat.getCategory_name());
                }

                categoryAdapter = new ArrayAdapter<>(ProductDetailActivity.this,
                        android.R.layout.simple_spinner_item, names);
                categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spCategory.setAdapter(categoryAdapter);

                selectCurrentProductCategory();
            });
        });
    }

    private void selectCurrentProductCategory() {
        if (currentProduct != null && currentProduct.getCategory_id() != null && categoryAdapter != null) {
            for (int i = 0; i < categoriesList.size(); i++) {
                if (categoriesList.get(i).getId().equals(currentProduct.getCategory_id())) {
                    spCategory.setSelection(i);
                    break;
                }
            }
        }
    }

    // --- Currency formatting helpers ---

    private String formatCurrencyText(long amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(amount);
    }

    private void setCurrencyValue(EditText editText, long value) {
        editText.setText(value > 0 ? formatCurrencyText(value) : "");
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
                if (s.toString().equals(current)) return;
                editText.removeTextChangedListener(this);

                String cleanString = s.toString().replace(".", "");
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
                        // Restore previous value if parsing fails
                    }
                }

                editText.addTextChangedListener(this);
            }
        });
    }
}
