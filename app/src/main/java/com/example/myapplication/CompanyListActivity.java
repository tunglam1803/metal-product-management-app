package com.example.myapplication;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.myapplication.adapters.CompanyAdapter;
import com.example.myapplication.helpers.FirebaseHelper;
import com.example.myapplication.models.Company;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CompanyListActivity extends AppCompatActivity {

    private RecyclerView rvCompanies;
    private CompanyAdapter adapter;
    private final List<Company> allCompanies = new ArrayList<>();
    private final List<Company> filteredCompanies = new ArrayList<>();
    private FirebaseHelper firebaseHelper;
    private ListenerRegistration companiesListener;

    private EditText etSearchCompany;
    private ImageButton btnClearSearch;
    private TextView tvCompanyCountSummary, tvTotalItemsTracked;
    private LinearLayout llEmptyState;
    private TextView tvEmptyTitle, tvEmptySubtitle;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_list);

        firebaseHelper = new FirebaseHelper();

        initViews();
        setupRecyclerView();
        setupSearch();
        loadCompanies();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnAddCompanyHeader = findViewById(R.id.btnAddCompanyHeader);
        etSearchCompany = findViewById(R.id.etSearchCompany);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        tvCompanyCountSummary = findViewById(R.id.tvCompanyCountSummary);
        tvTotalItemsTracked = findViewById(R.id.tvTotalItemsTracked);
        llEmptyState = findViewById(R.id.llEmptyState);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvCompanies = findViewById(R.id.rvCompanies);
        FloatingActionButton fabAddCompany = findViewById(R.id.fabAddCompany);
        Button btnEmptyAddCompany = findViewById(R.id.btnEmptyAddCompany);

        btnBack.setOnClickListener(v -> finish());
        btnAddCompanyHeader.setOnClickListener(v -> showAddOrEditCompanyDialog(null));
        fabAddCompany.setOnClickListener(v -> showAddOrEditCompanyDialog(null));
        btnEmptyAddCompany.setOnClickListener(v -> showAddOrEditCompanyDialog(null));

        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(this::loadCompanies);
    }

    private void setupRecyclerView() {
        rvCompanies.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CompanyAdapter(
                this,
                filteredCompanies,
                company -> {
                    Intent intent = new Intent(CompanyListActivity.this, CompanyDetailActivity.class);
                    intent.putExtra("COMPANY_ID", company.getId());
                    intent.putExtra("COMPANY_NAME", company.getCompany_name());
                    intent.putExtra("COMPANY_PHONE", company.getPhone());
                    intent.putExtra("COMPANY_ADDRESS", company.getAddress());
                    intent.putExtra("COMPANY_NOTE", company.getNote());
                    startActivity(intent);
                },
                new CompanyAdapter.OnCompanyActionListener() {
                    @Override
                    public void onEditCompany(Company company) {
                        showAddOrEditCompanyDialog(company);
                    }

                    @Override
                    public void onDeleteCompany(Company company) {
                        showDeleteCompanyConfirmation(company);
                    }
                }
        );
        rvCompanies.setAdapter(adapter);
    }

    private void setupSearch() {
        btnClearSearch.setOnClickListener(v -> etSearchCompany.setText(""));

        etSearchCompany.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                String query = s.toString();
                btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                filterCompanies(query);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadCompanies() {
        if (companiesListener != null) {
            companiesListener.remove();
        }

        companiesListener = firebaseHelper.listenForCompanies((value, error) -> {
            swipeRefreshLayout.setRefreshing(false);
            if (error != null) {
                Toast.makeText(this, "Lỗi tải danh sách công ty: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (value == null) return;

            allCompanies.clear();
            int totalItems = 0;
            for (QueryDocumentSnapshot doc : value) {
                Company c = doc.toObject(Company.class);
                c.setId(doc.getId());
                allCompanies.add(c);
                totalItems += c.getItems_count();
            }

            // Sort by latest updated or created
            allCompanies.sort((c1, c2) -> {
                Long t1 = c1.getUpdated_at() != null ? c1.getUpdated_at() : 0L;
                Long t2 = c2.getUpdated_at() != null ? c2.getUpdated_at() : 0L;
                return t2.compareTo(t1);
            });

            tvCompanyCountSummary.setText(allCompanies.size() + " công ty đối tác");
            tvTotalItemsTracked.setText(totalItems + " mặt hàng đã lưu giá");

            filterCompanies(etSearchCompany.getText().toString());
        });
    }

    private void filterCompanies(String query) {
        filteredCompanies.clear();
        String trimmed = query != null ? query.trim() : "";

        if (trimmed.isEmpty()) {
            filteredCompanies.addAll(allCompanies);
        } else {
            String normalizedQuery = removeDiacritics(trimmed.toLowerCase());
            for (Company c : allCompanies) {
                String nameNorm = removeDiacritics(c.getCompany_name().toLowerCase());
                String phone = c.getPhone().toLowerCase();
                String addressNorm = removeDiacritics(c.getAddress().toLowerCase());

                if (nameNorm.contains(normalizedQuery) || phone.contains(normalizedQuery) || addressNorm.contains(normalizedQuery)) {
                    filteredCompanies.add(c);
                }
            }
        }

        adapter.updateList(filteredCompanies);

        if (filteredCompanies.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            if (allCompanies.isEmpty()) {
                tvEmptyTitle.setText("Chưa có công ty nào");
                tvEmptySubtitle.setText("Thêm đối tác để bắt đầu lưu và theo dõi giá bán riêng theo từng mặt hàng");
            } else {
                tvEmptyTitle.setText("Không tìm thấy công ty");
                tvEmptySubtitle.setText("Không có kết quả nào khớp với từ khóa \"" + query + "\"");
            }
        } else {
            llEmptyState.setVisibility(View.GONE);
        }
    }

    private void showAddOrEditCompanyDialog(Company companyToEdit) {
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

        boolean isEditing = companyToEdit != null;
        if (isEditing) {
            tvTitle.setText("Chỉnh sửa công ty");
            etName.setText(companyToEdit.getCompany_name());
            etPhone.setText(companyToEdit.getPhone());
            etAddress.setText(companyToEdit.getAddress());
            etNote.setText(companyToEdit.getNote());
            btnSave.setText("Cập nhật");
        } else {
            tvTitle.setText("Thêm công ty mua hàng");
            btnSave.setText("Lưu công ty");
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText() != null ? etName.getText().toString().trim() : "";
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
            String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
            String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";

            if (name.isEmpty()) {
                etName.setError("Vui lòng nhập tên công ty / khách hàng");
                etName.requestFocus();
                return;
            }

            btnSave.setEnabled(false);

            if (isEditing) {
                companyToEdit.setCompany_name(name);
                companyToEdit.setPhone(phone);
                companyToEdit.setAddress(address);
                companyToEdit.setNote(note);

                firebaseHelper.updateCompany(companyToEdit.getId(), companyToEdit,
                        () -> {
                            dialog.dismiss();
                            Toast.makeText(CompanyListActivity.this, "Đã cập nhật công ty thành công!", Toast.LENGTH_SHORT).show();
                        },
                        error -> {
                            btnSave.setEnabled(true);
                            Toast.makeText(CompanyListActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                        });
            } else {
                Company newCompany = new Company(name, phone, address, note);
                firebaseHelper.addCompany(newCompany,
                        companyId -> {
                            dialog.dismiss();
                            Toast.makeText(CompanyListActivity.this, "Đã thêm công ty '" + name + "'", Toast.LENGTH_SHORT).show();
                            // Navigate directly to company detail to add items
                            Intent intent = new Intent(CompanyListActivity.this, CompanyDetailActivity.class);
                            intent.putExtra("COMPANY_ID", companyId);
                            intent.putExtra("COMPANY_NAME", name);
                            intent.putExtra("COMPANY_PHONE", phone);
                            intent.putExtra("COMPANY_ADDRESS", address);
                            intent.putExtra("COMPANY_NOTE", note);
                            startActivity(intent);
                        },
                        error -> {
                            btnSave.setEnabled(true);
                            Toast.makeText(CompanyListActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                        });
            }
        });

        dialog.show();
    }

    private void showDeleteCompanyConfirmation(Company company) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận xóa công ty")
                .setMessage("Bạn có chắc chắn muốn xóa '" + company.getCompany_name() + "' cùng toàn bộ bảng giá các mặt hàng đã lưu của công ty này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    firebaseHelper.deleteCompany(company.getId(),
                            () -> Toast.makeText(CompanyListActivity.this, "Đã xóa công ty thành công", Toast.LENGTH_SHORT).show(),
                            error -> Toast.makeText(CompanyListActivity.this, "Lỗi khi xóa: " + error, Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
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
        if (companiesListener != null) {
            companiesListener.remove();
        }
    }
}
