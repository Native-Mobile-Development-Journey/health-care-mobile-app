package com.example.smdproject.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.smdproject.R;

public class DoctorDetailFragment extends Fragment {

    public DoctorDetailFragment() {
        super(R.layout.fragment_doctor_detail);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View backButton = view.findViewById(R.id.button_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }
    }
}
