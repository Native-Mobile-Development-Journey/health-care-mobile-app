package com.project.healthcare.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.project.healthcare.R;
import com.project.healthcare.data.AppRepository;
import com.project.healthcare.data.models.Appointment;
import com.project.healthcare.data.models.Doctor;

import java.util.List;
import java.util.Locale;

public class DoctorOverviewFragment extends Fragment {

    private TextView nameText;
    private TextView experienceText;
    private TextView ratingText;
    private TextView patientsText;
    private TextView upcomingText;
    private TextView completedText;

    public DoctorOverviewFragment() {
        super(R.layout.fragment_doctor_overview);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        nameText = view.findViewById(R.id.text_overview_name);
        experienceText = view.findViewById(R.id.text_overview_experience);
        ratingText = view.findViewById(R.id.text_overview_rating);
        patientsText = view.findViewById(R.id.text_overview_patients);
        upcomingText = view.findViewById(R.id.text_overview_upcoming);
        completedText = view.findViewById(R.id.text_overview_completed);

        bindDefaultDoctorName();
        loadDoctorOverview();
    }

    private void bindDefaultDoctorName() {
        String defaultName = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getDisplayName()
                : null;
        if (defaultName == null || defaultName.trim().isEmpty()) {
            defaultName = getString(R.string.doctor_default_name);
        }
        nameText.setText(defaultName);
    }

    private void loadDoctorOverview() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid == null) {
            return;
        }

        AppRepository.getInstance().getDoctorProfile(uid, new AppRepository.DoctorCallback() {
            @Override
            public void onDoctorLoaded(@Nullable Doctor doctor) {
                if (doctor == null) {
                    return;
                }
                bindDoctor(doctor);
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });

        AppRepository.getInstance().getDoctorAppointments(uid, new AppRepository.ListCallback<Appointment>() {
            @Override
            public void onData(List<Appointment> items) {
                int upcoming = 0;
                int completed = 0;
                for (Appointment appointment : items) {
                    String status = appointment.normalizedStatus();
                    if (Appointment.STATUS_UPCOMING.equals(status)) {
                        upcoming++;
                    } else if (Appointment.STATUS_COMPLETED.equals(status)) {
                        completed++;
                    }
                }
                if (isAdded()) {
                    upcomingText.setText(String.format(Locale.getDefault(), "%d", upcoming));
                    completedText.setText(String.format(Locale.getDefault(), "%d", completed));
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void bindDoctor(Doctor doctor) {
        String name = doctor.name;
        if (name == null || name.trim().isEmpty()) {
            name = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getDisplayName()
                    : null;
        }
        if (name == null || name.trim().isEmpty()) {
            name = getString(R.string.doctor_default_name);
        }

        nameText.setText(name);
        experienceText.setText(String.valueOf(doctor.experienceYears));
        ratingText.setText(String.format(Locale.getDefault(), "%.1f", doctor.rating));
        patientsText.setText(String.valueOf(doctor.patientCount));
    }
}
