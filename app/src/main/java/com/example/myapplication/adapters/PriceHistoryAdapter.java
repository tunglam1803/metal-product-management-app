package com.example.myapplication.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.PriceHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PriceHistoryAdapter extends RecyclerView.Adapter<PriceHistoryAdapter.ViewHolder> {

    private List<PriceHistory> histories = new ArrayList<>();
    public void setHistories(List<PriceHistory> histories) {
        this.histories = histories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_price_history, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PriceHistory h = histories.get(position);
        holder.tvHistoryImportPrice.setText(String.format(Locale.getDefault(), "%,.0fđ -> %,.0fđ", h.getOld_import_price(), h.getNew_import_price()));
        holder.tvHistorySellPrice.setText(String.format(Locale.getDefault(), "%,.0fđ -> %,.0fđ", h.getOld_sell_price(), h.getNew_sell_price()));
        
        if (h.getChanged_at() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
            holder.tvHistoryDate.setText(sdf.format(new Date(h.getChanged_at() * 1000L)));
        } else {
            holder.tvHistoryDate.setText("");
        }
        holder.tvHistoryNote.setText(h.getNote() != null ? h.getNote() : "");
    }

    @Override
    public int getItemCount() {
        return histories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvHistoryImportPrice, tvHistorySellPrice, tvHistoryDate, tvHistoryNote;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHistoryImportPrice = itemView.findViewById(R.id.tvHistoryImportPrice);
            tvHistorySellPrice = itemView.findViewById(R.id.tvHistorySellPrice);
            tvHistoryDate = itemView.findViewById(R.id.tvHistoryDate);
            tvHistoryNote = itemView.findViewById(R.id.tvHistoryNote);
        }
    }
}
