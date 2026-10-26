package com.example.myapplication.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.models.Company;

import java.util.ArrayList;
import java.util.List;

public class CompanyAdapter extends RecyclerView.Adapter<CompanyAdapter.ViewHolder> {

    public interface OnCompanyClickListener {
        void onCompanyClick(Company company);
    }

    public interface OnCompanyActionListener {
        void onEditCompany(Company company);
        void onDeleteCompany(Company company);
    }

    private final Context context;
    private final List<Company> companyList;
    private final OnCompanyClickListener clickListener;
    private final OnCompanyActionListener actionListener;

    public CompanyAdapter(Context context, List<Company> companyList,
                          OnCompanyClickListener clickListener,
                          OnCompanyActionListener actionListener) {
        this.context = context;
        this.companyList = new ArrayList<>();
        if (companyList != null) {
            this.companyList.addAll(companyList);
        }
        this.clickListener = clickListener;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_company, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Company company = companyList.get(position);
        holder.bind(company);
    }

    @Override
    public int getItemCount() {
        return companyList.size();
    }

    public void updateList(List<Company> newList) {
        this.companyList.clear();
        if (newList != null) {
            this.companyList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCompanyInitial, tvCompanyName, tvCompanyPhone, tvCompanyAddress, tvCompanyNote, tvItemsCount;
        private final ImageButton btnQuickCall, btnCompanyMore;
        private final View llPhoneRow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCompanyInitial = itemView.findViewById(R.id.tvCompanyInitial);
            tvCompanyName = itemView.findViewById(R.id.tvCompanyName);
            tvCompanyPhone = itemView.findViewById(R.id.tvCompanyPhone);
            tvCompanyAddress = itemView.findViewById(R.id.tvCompanyAddress);
            tvCompanyNote = itemView.findViewById(R.id.tvCompanyNote);
            tvItemsCount = itemView.findViewById(R.id.tvItemsCount);
            btnQuickCall = itemView.findViewById(R.id.btnQuickCall);
            btnCompanyMore = itemView.findViewById(R.id.btnCompanyMore);
            llPhoneRow = itemView.findViewById(R.id.llPhoneRow);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onCompanyClick(companyList.get(pos));
                }
            });
        }

        public void bind(Company company) {
            String name = company.getCompany_name();
            tvCompanyName.setText(name);

            if (name != null && !name.trim().isEmpty()) {
                tvCompanyInitial.setText(name.trim().substring(0, 1).toUpperCase());
            } else {
                tvCompanyInitial.setText("C");
            }

            String phone = company.getPhone();
            if (phone != null && !phone.trim().isEmpty()) {
                llPhoneRow.setVisibility(View.VISIBLE);
                tvCompanyPhone.setText(phone);
                btnQuickCall.setVisibility(View.VISIBLE);
                btnQuickCall.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + phone.trim()));
                        context.startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(context, "Không thể mở ứng dụng gọi điện", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                llPhoneRow.setVisibility(View.GONE);
                btnQuickCall.setVisibility(View.GONE);
            }

            String address = company.getAddress();
            if (address != null && !address.trim().isEmpty()) {
                tvCompanyAddress.setVisibility(View.VISIBLE);
                tvCompanyAddress.setText(address);
            } else {
                tvCompanyAddress.setVisibility(View.GONE);
            }

            int count = company.getItems_count();
            tvItemsCount.setText(count + " mặt hàng hay mua");

            String note = company.getNote();
            if (note != null && !note.trim().isEmpty()) {
                tvCompanyNote.setText(note);
            } else {
                tvCompanyNote.setText("Xem bảng giá riêng");
            }

            btnCompanyMore.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, btnCompanyMore);
                popup.getMenu().add(0, 1, 0, "Sửa thông tin");
                popup.getMenu().add(0, 2, 1, "Xóa công ty");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        if (actionListener != null) {
                            actionListener.onEditCompany(company);
                        }
                        return true;
                    } else if (item.getItemId() == 2) {
                        if (actionListener != null) {
                            actionListener.onDeleteCompany(company);
                        }
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }
}
