package com.example.myapplication;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProductDetailActivity extends AppCompatActivity {

    private EditText etProductCode, etProductName, etImportPrice, etSellPrice;
    private Button btnSave, btnViewHistory;
    private ImageView imgPreview;
    private View btnPickImage;
    private TextView tvUploadStatus;
    private FirebaseHelper firebaseHelper;
    private String productId;
    private Product currentProduct;
    private String uploadedImageUrl = null;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Spinner spCategory;
    private final List<Category> categoriesList = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;
    private ImageButton btnScanCode;
    private ActivityResultLauncher<Intent> barcodeScannerLauncher;

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
            toolbar.setTitle("Sửa Sản Phẩm");
            loadProductDetails();
            btnViewHistory.setVisibility(View.VISIBLE);
        } else {
            toolbar.setTitle("Thêm Sản Phẩm");
            String prefilledCode = getIntent().getStringExtra("PRODUCT_CODE");
            if (prefilledCode != null && !prefilledCode.isEmpty()) {
                etProductCode.setText(prefilledCode);
            }
        }

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
                }
        );

        btnScanCode.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScannerActivity.class);
            barcodeScannerLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> saveProduct());
        btnViewHistory.setOnClickListener(v -> showPriceHistory());
        imgPreview.setOnClickListener(v -> showFullScreenImage());
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
        btnSave = findViewById(R.id.btnSave);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        imgPreview = findViewById(R.id.imgPreview);
        btnPickImage = findViewById(R.id.btnPickImage);
        tvUploadStatus = findViewById(R.id.tvUploadStatus);
        spCategory = findViewById(R.id.spCategory);
        btnScanCode = findViewById(R.id.btnScanCode);
    }

    private void loadProductDetails() {
        firebaseHelper.getProduct(productId, documentSnapshot -> {
            currentProduct = documentSnapshot.toObject(Product.class);
            if (currentProduct != null) {
                currentProduct.setId(documentSnapshot.getId());
                etProductCode.setText(currentProduct.getProduct_code());
                etProductName.setText(currentProduct.getProduct_name());
                etImportPrice.setText(String.valueOf(currentProduct.getImport_price().longValue()));
                etSellPrice.setText(String.valueOf(currentProduct.getSell_price().longValue()));
                uploadedImageUrl = currentProduct.getImage_url();

                selectCurrentProductCategory();

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

        btnPickImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });
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

    private void saveProduct() {
        String code = etProductCode.getText().toString().trim();
        String name = etProductName.getText().toString().trim();
        String sImport = etImportPrice.getText().toString().trim();
        String sSell = etSellPrice.getText().toString().trim();

        if (name.isEmpty()) {
            etProductName.setError("Vui lòng nhập tên");
            return;
        }

        double importPrice = sImport.isEmpty() ? 0 : Double.parseDouble(sImport);
        double sellPrice = sSell.isEmpty() ? 0 : Double.parseDouble(sSell);

        Product p = new Product();
        p.setProduct_code(code);
        p.setProduct_name(name);
        p.setImport_price(importPrice);
        p.setSell_price(sellPrice);
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

            Collections.sort(histories, (h1, h2) -> {
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
}
