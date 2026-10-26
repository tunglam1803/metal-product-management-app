package com.example.myapplication.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.models.Product;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductPickerAdapter extends RecyclerView.Adapter<ProductPickerAdapter.ViewHolder> {

    public interface OnProductSelectListener {
        void onProductSelected(Product product);
    }

    private final Context context;
    private final List<Product> productList;
    private final OnProductSelectListener listener;
    private final DecimalFormat currencyFormat;

    public ProductPickerAdapter(Context context, List<Product> productList, OnProductSelectListener listener) {
        this.context = context;
        this.productList = productList != null ? new ArrayList<>(productList) : new ArrayList<>();
        this.listener = listener;

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setGroupingSeparator('.');
        this.currencyFormat = new DecimalFormat("#,###", symbols);
    }

    public void updateList(List<Product> newList) {
        this.productList.clear();
        if (newList != null) {
            this.productList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_picker_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivResultThumb;
        private final TextView tvResultName;
        private final TextView tvResultCode;
        private final TextView tvResultPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivResultThumb = itemView.findViewById(R.id.ivResultThumb);
            tvResultName = itemView.findViewById(R.id.tvResultName);
            tvResultCode = itemView.findViewById(R.id.tvResultCode);
            tvResultPrice = itemView.findViewById(R.id.tvResultPrice);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onProductSelected(productList.get(pos));
                }
            });
        }

        public void bind(Product product) {
            tvResultName.setText(product.getProduct_name() != null ? product.getProduct_name() : "Không tên");

            String code = product.getProduct_code();
            if (code != null && !code.trim().isEmpty()) {
                tvResultCode.setText("MÃ: " + code.trim());
                tvResultCode.setVisibility(View.VISIBLE);
            } else {
                tvResultCode.setText("MÃ: --");
            }

            double price = product.getSell_price();
            tvResultPrice.setText(currencyFormat.format(price) + " đ");

            String imgUrl = product.getImage_url();
            if (imgUrl != null && !imgUrl.isEmpty()) {
                Glide.with(context)
                        .load(imgUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .into(ivResultThumb);
            } else {
                ivResultThumb.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }
    }
}
