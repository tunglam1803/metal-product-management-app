package com.example.myapplication.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.myapplication.BuildConfig;
import com.example.myapplication.R;
import com.example.myapplication.models.Category;
import com.example.myapplication.models.Product;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private final Context context;
    private List<Product> productList = new ArrayList<>();
    private final OnProductClickListener listener;
    private boolean isGridView = false;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public ProductAdapter(Context context, OnProductClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setProducts(List<Product> products) {
        this.productList = products;
        notifyDataSetChanged();
    }

    public void setGridView(boolean isGridView) {
        this.isGridView = isGridView;
        notifyDataSetChanged();
    }

    public Product getProductAt(int position) {
        if (position >= 0 && position < productList.size()) {
            return productList.get(position);
        }
        return null;
    }

    private final Map<String, String> categoriesMap = new HashMap<>();

    public void setCategories(List<Category> categories) {
        categoriesMap.clear();
        for (Category cat : categories) {
            categoriesMap.put(cat.getId(), cat.getCategory_name());
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isGridView ? R.layout.item_product_grid : R.layout.item_product;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Khôi phục lại trạng thái trượt của thẻ để tránh lỗi giao diện khi tái sử dụng view
        holder.itemView.setTranslationX(0f);
        holder.itemView.setTranslationY(0f);

        Product product = productList.get(position);

        holder.tvProductName.setText(product.getProduct_name());
        holder.tvSellPriceNew.setText(formatCurrency(product.getSell_price()));

        // Bind resolved category name (e.g. "Đồ máy", "Phụ kiện sửa chữa")
        String categoryName = "Khác";
        if (product.getCategory_id() != null && categoriesMap.containsKey(product.getCategory_id())) {
            categoryName = categoriesMap.get(product.getCategory_id());
        }
        holder.tvProductUnit.setText(categoryName);

        // Bind stock quantity
        int stock = product.getStock_quantity();
        if (stock > 0) {
            holder.tvStockQuantity.setText("Còn: " + stock + " cái");
            holder.tvStockQuantity.setTextColor(ContextCompat.getColor(context, R.color.colorTextGrey));
        } else {
            holder.tvStockQuantity.setText("Hết hàng");
            holder.tvStockQuantity.setTextColor(ContextCompat.getColor(context, R.color.colorDanger));
        }

        // Load image using Glide
        if (product.getImage_url() != null && !product.getImage_url().isEmpty()) {
            holder.imgProduct.setImageTintList(null);
            
            String finalUrl = product.getImage_url();
            if (finalUrl.contains("/object/public/")) {
                finalUrl = finalUrl.replace("/object/public/", "/object/");
            }

            GlideUrl glideUrl = new GlideUrl(finalUrl,
                new LazyHeaders.Builder()
                    .addHeader("Authorization", "Bearer " + BuildConfig.SUPABASE_ANON_KEY)
                    .build());
            
            Glide.with(context)
                    .load(glideUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.imgProduct);
        } else {
            holder.imgProduct.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, android.R.color.darker_gray)));
            holder.imgProduct.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Handle click on complete item row
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });

        // Handle click on Share Button
        holder.btnShare.setOnClickListener(v -> {
            Toast.makeText(context, "Đang chuẩn bị chia sẻ sản phẩm " + product.getProduct_name() + "...", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    private String formatCurrency(Double amount) {
        if (amount == null) return "0 đ";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return df.format(amount) + " đ";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView tvProductName, tvProductUnit, tvSellPriceNew, tvStockQuantity;
        View btnShare;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductUnit = itemView.findViewById(R.id.tvProductUnit);
            tvSellPriceNew = itemView.findViewById(R.id.tvSellPriceNew);
            tvStockQuantity = itemView.findViewById(R.id.tvStockQuantity);
            btnShare = itemView.findViewById(R.id.btnShare);
        }
    }
}
