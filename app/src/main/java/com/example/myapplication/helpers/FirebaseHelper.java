package com.example.myapplication.helpers;

import android.util.Log;

import com.example.myapplication.models.PriceHistory;
import com.example.myapplication.models.Product;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
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

    public void listenForProducts(EventListener<QuerySnapshot> listener) {
        String userId = getCurrentUserId();
        if (userId == null)
            return;
        db.collection("products")
                .whereEqualTo("user_id", userId)
                .addSnapshotListener(listener);
    }

    public void getProduct(String id,
            com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot> successListener) {
        db.collection("products").document(id).get().addOnSuccessListener(successListener);
    }

    public void addProduct(Product product, OnSuccessListener successListener, OnFailureListener failureListener) {
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
                        successListener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (failureListener != null)
                        failureListener.onFailure(e.getMessage());
                });
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

    public void listenForCategories(EventListener<QuerySnapshot> listener) {
        String userId = getCurrentUserId();
        if (userId == null)
            return;
        db.collection("categories")
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

    public interface OnSuccessListener {
        void onSuccess();
    }

    public interface OnFailureListener {
        void onFailure(String error);
    }
}
