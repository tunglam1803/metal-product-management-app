package com.example.myapplication.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.helpers.FirebaseHelper;

public class ReportsFragment extends Fragment {

    private TextView tvTotalProducts;
    private TextView tvTotalCategories;
    private FirebaseHelper firebaseHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        tvTotalProducts = view.findViewById(R.id.tvTotalProducts);
        tvTotalCategories = view.findViewById(R.id.tvTotalCategories);
        firebaseHelper = new FirebaseHelper();

        loadStats();

        return view;
    }

    private void loadStats() {
        firebaseHelper.listenForProducts((value, error) -> {
            if (error == null && value != null) {
                tvTotalProducts.setText(String.valueOf(value.size()));
            }
        });

        firebaseHelper.listenForCategories((value, error) -> {
            if (error == null && value != null) {
                tvTotalCategories.setText(String.valueOf(value.size()));
            }
        });
    }
}
