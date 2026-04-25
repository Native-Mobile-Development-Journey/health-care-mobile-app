package com.project.healthcare.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.project.healthcare.R;
import com.project.healthcare.data.AppRepository;
import com.project.healthcare.data.models.DoctorAvailabilitySlot;
import com.project.healthcare.ui.adapter.AvailabilitySlotAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DoctorAvailabilityFragment extends Fragment implements AvailabilitySlotAdapter.OnAvailabilitySlotActionListener {

    private AvailabilitySlotAdapter adapter;
    private Spinner daySpinner;
    private EditText startTimeInput;
    private Button addSlotButton;

    public DoctorAvailabilityFragment() {
        super(R.layout.fragment_doctor_availability);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        daySpinner = view.findViewById(R.id.spinner_slot_day);
        startTimeInput = view.findViewById(R.id.input_slot_start_time);
        addSlotButton = view.findViewById(R.id.button_add_slot);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_availability_slots);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AvailabilitySlotAdapter(this);
        recyclerView.setAdapter(adapter);

        setupDaySpinner();
        addSlotButton.setOnClickListener(v -> addAvailabilitySlot());

        loadAvailability();
    }

    private void setupDaySpinner() {
        List<String> days = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, days);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daySpinner.setAdapter(spinnerAdapter);
    }

    private void loadAvailability() {
        String doctorId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (doctorId == null) {
            return;
        }

        AppRepository.getInstance().getDoctorAvailability(doctorId, new AppRepository.ListCallback<DoctorAvailabilitySlot>() {
            @Override
            public void onData(List<DoctorAvailabilitySlot> items) {
                if (isAdded()) {
                    adapter.submitList(items);
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

    private void addAvailabilitySlot() {
        String dayLabel = daySpinner.getSelectedItem() != null ? daySpinner.getSelectedItem().toString() : "Monday";
        String startTime = startTimeInput.getText() == null ? "" : startTimeInput.getText().toString().trim();
        if (startTime.isEmpty()) {
            startTimeInput.setError("Enter start time");
            return;
        }

        String endTime = calculateEndTime(startTime);
        if (endTime == null) {
            startTimeInput.setError("Enter time as HH:MM AM/PM");
            return;
        }

        String doctorId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (doctorId == null) {
            return;
        }

        DoctorAvailabilitySlot slot = new DoctorAvailabilitySlot();
        slot.dayOfWeek = dayLabel;
        slot.dayLabel = dayLabel;
        slot.startTime = startTime;
        slot.endTime = endTime;
        slot.durationMinutes = 60;
        slot.isActive = true;

        AppRepository.getInstance().saveDoctorAvailabilitySlot(doctorId, slot, (success, message) -> {
            if (!isAdded()) {
                return;
            }
            if (success) {
                Toast.makeText(requireContext(), "Availability added", Toast.LENGTH_SHORT).show();
                loadAvailability();
                startTimeInput.setText("");
            } else {
                Toast.makeText(requireContext(), message != null ? message : "Unable to save slot", Toast.LENGTH_LONG).show();
            }
        });
    }

    @Nullable
    private String calculateEndTime(String startTime) {
        try {
            String[] parts = startTime.split(" ");
            if (parts.length != 2) {
                return null;
            }
            String[] timeParts = parts[0].split(":");
            if (timeParts.length != 2) {
                return null;
            }
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            String suffix = parts[1].toUpperCase(Locale.ROOT);
            if (suffix.equals("PM") && hour != 12) {
                hour += 12;
            } else if (suffix.equals("AM") && hour == 12) {
                hour = 0;
            }
            hour += 1;
            if (hour == 24) {
                hour = 0;
            }
            int displayHour = hour % 12;
            if (displayHour == 0) {
                displayHour = 12;
            }
            String ampm = hour >= 12 ? "PM" : "AM";
            return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, ampm);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void onRemoveSlot(DoctorAvailabilitySlot slot) {
        String doctorId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (doctorId == null || slot.id == null) {
            return;
        }

        AppRepository.getInstance().deleteDoctorAvailabilitySlot(doctorId, slot, (success, message) -> {
            if (!isAdded()) {
                return;
            }
            if (success) {
                Toast.makeText(requireContext(), "Slot removed", Toast.LENGTH_SHORT).show();
                loadAvailability();
            } else {
                Toast.makeText(requireContext(), message != null ? message : "Unable to remove slot", Toast.LENGTH_LONG).show();
            }
        });
    }
}
