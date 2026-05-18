package com.example.myapplication.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.LoginActivity;
import com.example.myapplication.R;
import com.example.myapplication.helpers.PreferencesHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MoreFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        TextView tvGreeting = view.findViewById(R.id.tvGreeting);
        TextView tvEmail = view.findViewById(R.id.tvEmail);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            String name = user.getEmail().split("@")[0];
            tvGreeting.setText("Chào " + name + " 👋");
            tvEmail.setText(user.getEmail());
        }

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            new PreferencesHelper(requireContext()).logout();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null)
                getActivity().finish();
        });

        return view;
    }
}
