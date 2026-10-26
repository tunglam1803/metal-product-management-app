package com.example.myapplication.helpers;

import android.util.Log;

import com.example.myapplication.models.Company;
import com.example.myapplication.models.CompanyProduct;
import com.example.myapplication.models.PriceHistory;
import com.example.myapplication.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseHelper {
    private static final String TAG = "FirebaseHelper";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String getCurrentUserId() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
        return null;
    }

    public com.google.firebase.firestore.ListenerRegistration listenForProducts(EventListener<QuerySnapshot> listener) {
        String userId = getCurrentUserId();
        if (userId == null)
            return null;
        return db.collection("products")
                .whereEqualTo("user_id", userId)
                .addSnapshotListener(listener);
    }

    public void getProduct(String id,
            com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> successListener) {
        db.collection("products").document(id).get().addOnSuccessListener(successListener);
    }

    public void checkProductCodeExists(String productCode,
            com.google.android.gms.tasks.OnSuccessListener<QuerySnapshot> successListener) {
        String userId = getCurrentUserId();
        if (userId == null)
            return;
        db.collection("products")
                .whereEqualTo("user_id", userId)
                .whereEqualTo("product_code", productCode)
                .get()
                .addOnSuccessListener(successListener);
    }


    public void addProduct(Product product, OnSuccessListener successListener, OnFailureListener failureListener) {
        addProductWithId(product, id -> {
            if (successListener != null) successListener.onSuccess();
        }, failureListener);
    }

    public void addProductWithId(Product product, OnProductAddedListener successListener, OnFailureListener failureListener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (failureListener != null)
                failureListener.onFailure("Người dùng chưa xác thực!");
            return;
        }
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> map = new HashMap<>();
        map.put("user_id", userId);
        map.put("product_code", product.getProduct_code());
        map.put("product_name", product.getProduct_name());
        map.put("import_price", product.getImport_price());
        map.put("sell_price", product.getSell_price());
        map.put("image_url", product.getImage_url());
        map.put("category_id", product.getCategory_id());
        map.put("stock_quantity", product.getStock_quantity());
        map.put("note", product.getNote());
        map.put("is_bundle", product.getIs_bundle());
        map.put("created_at", now);
        map.put("updated_at", now);

        db.collection("products")
                .add(map)
                .addOnSuccessListener(docRef -> {
                    PriceHistory initialHistory = new PriceHistory(
                            0.0, product.getImport_price(),
                            0.0, product.getSell_price(),
                            now,
                            "Khởi tạo sản phẩm");
                    addPriceHistory(docRef.getId(), initialHistory);

                    if (successListener != null)
                        successListener.onProductAdded(docRef.getId());
                })
                .addOnFailureListener(e -> {
                    if (failureListener != null)
                        failureListener.onFailure(e.getMessage());
                });
    }

    public interface OnProductAddedListener {
        void onProductAdded(String productId);
    }

    public void updateProduct(String id, Product updatedProduct, PriceHistory potentialHistory,
            OnSuccessListener successListener, OnFailureListener failureListener) {
        String userId = getCurrentUserId();
        if (userId == null)
            return;
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> map = new HashMap<>();
        map.put("user_id", userId);
        map.put("product_code", updatedProduct.getProduct_code());
        map.put("product_name", updatedProduct.getProduct_name());
        map.put("import_price", updatedProduct.getImport_price());
        map.put("sell_price", updatedProduct.getSell_price());
        map.put("image_url", updatedProduct.getImage_url());
        map.put("category_id", updatedProduct.getCategory_id());
        map.put("stock_quantity", updatedProduct.getStock_quantity());
        map.put("note", updatedProduct.getNote());
        map.put("is_bundle", updatedProduct.getIs_bundle());
        map.put("updated_at", now);

        db.collection("products")
                .document(id)
                .update(map)
                .addOnSuccessListener(aVoid -> {
                    if (potentialHistory != null) {
                        potentialHistory.setChanged_at(now);
                        addPriceHistory(id, potentialHistory);
                    }
                    if (successListener != null)
                        successListener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (failureListener != null)
                        failureListener.onFailure(e.getMessage());
                });
    }

    public void addPriceHistory(String productId, PriceHistory history) {
        String userId = getCurrentUserId();
        if (userId == null)
            return;
        Map<String, Object> map = new HashMap<>();
        map.put("user_id", userId);
        map.put("old_import_price", history.getOld_import_price());
        map.put("new_import_price", history.getNew_import_price());
        map.put("old_sell_price", history.getOld_sell_price());
        map.put("new_sell_price", history.getNew_sell_price());
        map.put("changed_at",
                history.getChanged_at() == null ? System.currentTimeMillis() / 1000L : history.getChanged_at());
        map.put("note", history.getNote());

        db.collection("products")
                .document(productId)
                .collection("priceHistory")
                .add(map)
                .addOnFailureListener(e -> Log.e(TAG, "Error logging history", e));
    }

    public void getPriceHistory(String productId, EventListener<QuerySnapshot> listener) {
        String userId = getCurrentUserId();
        if (userId == null)
            return;
        db.collection("products")
                .document(productId)
                .collection("priceHistory")
                .whereEqualTo("user_id", userId)
                .addSnapshotListener(listener);
    }

    public void deleteProduct(String id, OnSuccessListener successListener, OnFailureListener failureListener) {
        db.collection("products")
                .document(id)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (successListener != null)
                        successListener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (failureListener != null)
                        failureListener.onFailure(e.getMessage());
                });
    }

    public com.google.firebase.firestore.ListenerRegistration listenForCategories(EventListener<QuerySnapshot> listener) {
        String userId = getCurrentUserId();
        if (userId == null)
            return null;
        return db.collection("categories")
                .whereEqualTo("user_id", userId)
                .addSnapshotListener(listener);
    }

    public void addCategory(String categoryName, OnSuccessListener successListener, OnFailureListener failureListener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (failureListener != null)
                failureListener.onFailure("Người dùng chưa xác thực!");
            return;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("user_id", userId);
        map.put("category_name", categoryName);
        db.collection("categories")
                .add(map)
                .addOnSuccessListener(docRef -> {
                    if (successListener != null)
                        successListener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (failureListener != null)
                        failureListener.onFailure(e.getMessage());
                });
    }

    public void deleteCategory(String categoryId, OnSuccessListener successListener,
            OnFailureListener failureListener) {
        db.collection("categories").document(categoryId).delete()
                .addOnSuccessListener(aVoid -> {
                    // Tìm tất cả sản phẩm thuộc danh mục bị xóa và cập nhật danh mục của chúng về
                    // null
                    db.collection("products")
                            .whereEqualTo("category_id", categoryId)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    db.collection("products").document(doc.getId()).update("category_id", null);
                                }
                                if (successListener != null)
                                    successListener.onSuccess();
                            });
                })
                .addOnFailureListener(e -> {
                    if (failureListener != null)
                        failureListener.onFailure(e.getMessage());
                });
    }

    public void updateCategory(String categoryId, String newName, OnSuccessListener successListener,
            OnFailureListener failureListener) {
        db.collection("categories").document(categoryId).update("category_name", newName)
                .addOnSuccessListener(aVoid -> {
                    if (successListener != null)
                        successListener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (failureListener != null)
                        failureListener.onFailure(e.getMessage());
                });
    }

    public void seedDefaultCategoriesIfNeeded(OnSuccessListener successListener) {
        String userId = getCurrentUserId();
        if (userId == null)
            return;
        db.collection("categories")
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        String[] defaults = { "Đồ máy", "Phụ kiện sửa chữa", "Khác" };
                        final int[] pending = { defaults.length };
                        for (String name : defaults) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("user_id", userId);
                            map.put("category_name", name);
                            db.collection("categories").add(map).addOnCompleteListener(task -> {
                                pending[0]--;
                                if (pending[0] == 0 && successListener != null) {
                                    successListener.onSuccess();
                                }
                            });
                        }
                    } else {
                        if (successListener != null)
                            successListener.onSuccess();
                    }
                });
    }

    // ==========================================
    // COMPANY & CUSTOM PRICING MANAGEMENT
    // ==========================================

    public ListenerRegistration listenForCompanies(EventListener<QuerySnapshot> listener) {
        String userId = getCurrentUserId();
        if (userId == null)
            return null;
        return db.collection("companies")
                .whereEqualTo("user_id", userId)
                .addSnapshotListener(listener);
    }

    public void addCompany(Company company, OnCompanyAddedListener successListener, OnFailureListener failureListener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (failureListener != null)
                failureListener.onFailure("Người dùng chưa xác thực!");
            return;
        }
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> map = new HashMap<>();
        map.put("user_id", userId);
        map.put("company_name", company.getCompany_name());
        map.put("phone", company.getPhone());
        map.put("address", company.getAddress());
        map.put("note", company.getNote());
        map.put("items_count", 0);
        map.put("created_at", now);
        map.put("updated_at", now);

        db.collection("companies")
                .add(map)
                .addOnSuccessListener(docRef -> {
                    if (successListener != null) {
                        successListener.onCompanyAdded(docRef.getId());
                    }
                })
                .addOnFailureListener(e -> {
                    if (failureListener != null)
                        failureListener.onFailure(e.getMessage());
                });
    }

    public void updateCompany(String companyId, Company company, OnSuccessListener successListener, OnFailureListener failureListener) {
        String userId = getCurrentUserId();
        if (userId == null)
            return;
        long now = System.currentTimeMillis() / 1000L;
        Map<String, Object> map = new HashMap<>();
        map.put("company_name", company.getCompany_name());
        map.put("phone", company.getPhone());
        map.put("address", company.getAddress());
        map.put("note", company.getNote());
        map.put("updated_at", now);

        db.collection("companies")
                .document(companyId)
                .update(map)
                .addOnSuccessListener(aVoid -> {
                    if (successListener != null)
                        successListener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (failureListener != null)
                        failureListener.onFailure(e.getMessage());
                });
    }

    public void deleteCompany(String companyId, OnSuccessListener successListener, OnFailureListener failureListener) {
        // Delete all products inside companies/{companyId}/products first
        db.collection("companies").document(companyId).collection("products").get()
                .addOnSuccessListener(snapshots -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshots) {
                        batch.delete(doc.getReference());
                    }
                    batch.delete(db.collection("companies").document(companyId));
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                if (successListener != null)
                                    successListener.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                if (failureListener != null)
                                    failureListener.onFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    if (failureListener != null)
                        failureListener.onFailure(e.getMessage());
                });
    }

    public ListenerRegistration listenForCompanyProducts(String companyId, EventListener<QuerySnapshot> listener) {
        return db.collection("companies")
                .document(companyId)
                .collection("products")
                .addSnapshotListener(listener);
    }

    public void addOrUpdateCompanyProduct(String companyId, CompanyProduct cp, OnSuccessListener successListener, OnFailureListener failureListener) {
        String userId = getCurrentUserId();
        if (userId == null)
            return;
        long now = System.currentTimeMillis() / 1000L;

        String docId = cp.getProduct_id() != null && !cp.getProduct_id().isEmpty() ? cp.getProduct_id() : cp.getId();
        if (docId == null || docId.isEmpty()) {
            docId = db.collection("companies").document(companyId).collection("products").document().getId();
        }

        Map<String, Object> map = new HashMap<>();
        map.put("user_id", userId);
        map.put("company_id", companyId);
        map.put("company_name", cp.getCompany_name());
        map.put("product_id", cp.getProduct_id());
        map.put("product_code", cp.getProduct_code());
        map.put("product_name", cp.getProduct_name());
        map.put("image_url", cp.getImage_url());
        map.put("custom_price", cp.getCustom_price());
        map.put("standard_price", cp.getStandard_price());
        map.put("note", cp.getNote());
        map.put("updated_at", now);

        db.collection("companies")
                .document(companyId)
                .collection("products")
                .document(docId)
                .set(map)
                .addOnSuccessListener(aVoid -> {
                    // Update items_count and updated_at on the parent company document
                    recalculateCompanyItemsCount(companyId);
                    if (successListener != null)
                        successListener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (failureListener != null)
                        failureListener.onFailure(e.getMessage());
                });
    }

    public void deleteCompanyProduct(String companyId, String productId, OnSuccessListener successListener, OnFailureListener failureListener) {
        db.collection("companies")
                .document(companyId)
                .collection("products")
                .document(productId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    recalculateCompanyItemsCount(companyId);
                    if (successListener != null)
                        successListener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (failureListener != null)
                        failureListener.onFailure(e.getMessage());
                });
    }

    private void recalculateCompanyItemsCount(String companyId) {
        db.collection("companies").document(companyId).collection("products").get()
                .addOnSuccessListener(snapshots -> {
                    int count = snapshots.size();
                    db.collection("companies").document(companyId).update(
                            "items_count", count,
                            "updated_at", System.currentTimeMillis() / 1000L
                    );
                });
    }

    public void getCompaniesForProduct(String productId, OnCompanyProductsLoadedListener listener) {
        String userId = getCurrentUserId();
        if (userId == null) {
            if (listener != null) listener.onLoaded(new ArrayList<>());
            return;
        }

        // Get all companies for this user, then fetch products matching productId
        db.collection("companies")
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(companiesSnap -> {
                    if (companiesSnap.isEmpty()) {
                        if (listener != null) listener.onLoaded(new ArrayList<>());
                        return;
                    }

                    List<CompanyProduct> result = new ArrayList<>();
                    final int totalCompanies = companiesSnap.size();
                    final int[] checked = {0};

                    for (DocumentSnapshot cDoc : companiesSnap.getDocuments()) {
                        String companyId = cDoc.getId();
                        String companyName = cDoc.getString("company_name");

                        db.collection("companies")
                                .document(companyId)
                                .collection("products")
                                .document(productId)
                                .get()
                                .addOnSuccessListener(pDoc -> {
                                    if (pDoc.exists()) {
                                        CompanyProduct cp = pDoc.toObject(CompanyProduct.class);
                                        if (cp != null) {
                                            cp.setId(pDoc.getId());
                                            cp.setCompany_name(companyName);
                                            result.add(cp);
                                        }
                                    }
                                    checked[0]++;
                                    if (checked[0] == totalCompanies && listener != null) {
                                        listener.onLoaded(result);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    checked[0]++;
                                    if (checked[0] == totalCompanies && listener != null) {
                                        listener.onLoaded(result);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onLoaded(new ArrayList<>());
                });
    }

    public interface OnSuccessListener {
        void onSuccess();
    }

    public interface OnFailureListener {
        void onFailure(String error);
    }

    public interface OnCompanyAddedListener {
        void onCompanyAdded(String companyId);
    }

    public interface OnCompanyProductsLoadedListener {
        void onLoaded(List<CompanyProduct> list);
    }
}
