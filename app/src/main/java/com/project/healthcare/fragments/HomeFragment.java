package com.project.healthcare.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.project.healthcare.MainActivity;
import com.project.healthcare.R;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int[] clickableDoctorViews = {
                R.id.card_schedule_today,
                R.id.card_doctor_1,
                R.id.card_doctor_2,
                R.id.card_doctor_3,
                R.id.card_doctor_4
        };

        for (int viewId : clickableDoctorViews) {
            View clickableView = view.findViewById(viewId);
            if (clickableView != null) {
                clickableView.setOnClickListener(v -> openDoctorDetail());
            }
        }
    }

    private void openDoctorDetail() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openDoctorDetail();
        }
    }
}
