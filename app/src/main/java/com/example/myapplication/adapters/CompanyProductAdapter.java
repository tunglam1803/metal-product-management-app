package com.example.myapplication.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.models.CompanyProduct;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CompanyProductAdapter extends RecyclerView.Adapter<CompanyProductAdapter.ViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(CompanyProduct product);
    }

    public interface OnProductActionListener {
        void onEditPrice(CompanyProduct product);
        void onDeleteProduct(CompanyProduct product);
    }

    private final Context context;
    private final List<CompanyProduct> productList;
    private final OnProductClickListener clickListener;
    private final OnProductActionListener actionListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private final DecimalFormat currencyFormat;

    public CompanyProductAdapter(Context context, List<CompanyProduct> productList,
                                 OnProductClickListener clickListener,
                                 OnProductActionListener actionListener) {
        this.context = context;
        this.productList = new ArrayList<>();
        if (productList != null) {
            this.productList.addAll(productList);
        }
        this.clickListener = clickListener;
        this.actionListener = actionListener;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        this.currencyFormat = new DecimalFormat("#,###", symbols);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_company_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CompanyProduct product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void updateList(List<CompanyProduct> newList) {
        this.productList.clear();
        if (newList != null) {
            this.productList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    public CompanyProduct getProductAt(int position) {
        if (position >= 0 && position < productList.size()) {
            return productList.get(position);
        }
        return null;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProductImage;
        private final TextView tvProductCode, tvProductName, tvCustomPrice, tvStandardPrice, tvPriceDiffBadge, tvProductNote, tvUpdatedAt;
        private final ImageButton btnEditPrice, btnDeleteProduct;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductCode = itemView.findViewById(R.id.tvProductCode);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvCustomPrice = itemView.findViewById(R.id.tvCustomPrice);
            tvStandardPrice = itemView.findViewById(R.id.tvStandardPrice);
            tvPriceDiffBadge = itemView.findViewById(R.id.tvPriceDiffBadge);
            tvProductNote = itemView.findViewById(R.id.tvProductNote);
            tvUpdatedAt = itemView.findViewById(R.id.tvUpdatedAt);
            btnEditPrice = itemView.findViewById(R.id.btnEditPrice);
            btnDeleteProduct = itemView.findViewById(R.id.btnDeleteProduct);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onProductClick(productList.get(pos));
                }
            });
        }

        public void bind(CompanyProduct product) {
            String code = product.getProduct_code();
            tvProductCode.setText(code != null && !code.isEmpty() ? "MÃ: " + code : "CHƯA CÓ MÃ");

            tvProductName.setText(product.getProduct_name());

            // Custom price
            tvCustomPrice.setText(product.getFormattedCustomPrice());

            // Standard list price
            tvStandardPrice.setText(product.getFormattedStandardPrice());
            tvStandardPrice.setPaintFlags(tvStandardPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            // Price diff calculation
            double custom = product.getCustom_price();
            double standard = product.getStandard_price();
            double diff = custom - standard;

            if (standard > 0) {
                tvPriceDiffBadge.setVisibility(View.VISIBLE);
                if (diff < 0) {
                    tvPriceDiffBadge.setText(currencyFormat.format(diff) + " đ (Rẻ hơn)");
                    tvPriceDiffBadge.setTextColor(ContextCompat.getColor(context, R.color.colorSuccess));
                } else if (diff > 0) {
                    tvPriceDiffBadge.setText("+" + currencyFormat.format(diff) + " đ (Cao hơn)");
                    tvPriceDiffBadge.setTextColor(ContextCompat.getColor(context, R.color.colorWarning));
                } else {
                    tvPriceDiffBadge.setText("Bằng giá niêm yết");
                    tvPriceDiffBadge.setTextColor(ContextCompat.getColor(context, R.color.colorTextGrey));
                }
            } else {
                tvPriceDiffBadge.setVisibility(View.GONE);
            }

            // Note
            String note = product.getNote();
            if (note != null && !note.trim().isEmpty()) {
                tvProductNote.setVisibility(View.VISIBLE);
                tvProductNote.setText("Ghi chú: " + note);
            } else {
                tvProductNote.setVisibility(View.GONE);
            }

            // Updated at
            Long updated = product.getUpdated_at();
            if (updated != null && updated > 0) {
                tvUpdatedAt.setVisibility(View.VISIBLE);
                tvUpdatedAt.setText("Cập nhật: " + dateFormat.format(new Date(updated * 1000L)));
            } else {
                tvUpdatedAt.setVisibility(View.GONE);
            }

            // Thumbnail
            String imgUrl = product.getImage_url();
            if (imgUrl != null && !imgUrl.isEmpty()) {
                Glide.with(context)
                        .load(imgUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(ivProductImage);
            } else {
                ivProductImage.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            // Actions
            btnEditPrice.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onEditPrice(product);
                }
            });

            btnDeleteProduct.setOnClickListener(v -> {
                if (actionListener != null) {
                    actionListener.onDeleteProduct(product);
                }
            });
        }
    }
}
