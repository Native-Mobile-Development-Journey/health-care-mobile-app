package com.project.healthcare.fragments;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.project.healthcare.MainActivity;
import com.project.healthcare.R;

public class ScheduleFragment extends Fragment {

    public ScheduleFragment() {
        super(R.layout.fragment_schedule);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int[] clickableAppointmentViews = {
                R.id.card_appointment_1,
                R.id.card_appointment_2,
                R.id.card_appointment_3,
                R.id.card_appointment_4,
                R.id.button_reschedule_1,
                R.id.button_reschedule_3,
                R.id.button_reschedule_4,
                R.id.button_new_appointment
        };

        for (int viewId : clickableAppointmentViews) {
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
