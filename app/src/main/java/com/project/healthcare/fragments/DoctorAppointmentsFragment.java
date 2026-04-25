package com.project.healthcare.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.healthcare.R;
import com.project.healthcare.data.AppRepository;
import com.project.healthcare.data.models.Appointment;
import com.project.healthcare.ui.adapter.AppointmentAdapter;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class DoctorAppointmentsFragment extends Fragment implements AppointmentAdapter.OnAppointmentInteractionListener {

    private AppointmentAdapter appointmentAdapter;

    public DoctorAppointmentsFragment() {
        super(R.layout.fragment_doctor_appointments);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_doctor_appointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        appointmentAdapter = new AppointmentAdapter(this, true);
        recyclerView.setAdapter(appointmentAdapter);

        loadAppointments();
    }

    private void loadAppointments() {
        String doctorId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (doctorId == null) {
            return;
        }

        AppRepository.getInstance().getDoctorAppointments(doctorId, new AppRepository.ListCallback<Appointment>() {
            @Override
            public void onData(List<Appointment> items) {
                if (isAdded()) {
                    appointmentAdapter.submitList(items);
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

    @Override
    public void onAppointmentOpen(Appointment appointment) {
        // Future: open appointment detail screen.
        if (isAdded()) {
            Toast.makeText(requireContext(), "Open appointment " + appointment.id, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAppointmentAction(Appointment appointment) {
        // Future: support reschedule / mark complete.
        if (isAdded()) {
            Toast.makeText(requireContext(), "Action on " + appointment.status, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAppointmentDelete(Appointment appointment) {
        if (!isAdded() || appointment == null) {
            return;
        }
        showDeleteConfirmation(appointment);
    }

    private void showDeleteConfirmation(Appointment appointment) {
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_appointment_title)
                .setMessage(R.string.delete_appointment_message)
                .setNegativeButton(R.string.cancel, (d, which) -> d.dismiss())
                .setPositiveButton(R.string.delete, (d, which) -> AppRepository.getInstance().deleteAppointment(appointment.id, (success, message) -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (success) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), R.string.delete_appointment_success, Toast.LENGTH_SHORT).show();
                            loadAppointments();
                        });
                        return;
                    }
                    requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), message != null ? message : getString(R.string.auth_error_generic), Toast.LENGTH_SHORT).show());
                }))
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(requireContext().getColor(R.color.primary_500));
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(requireContext().getColor(R.color.red_500));
        });
        dialog.show();
    }
}
