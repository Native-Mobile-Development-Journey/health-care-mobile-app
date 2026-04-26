package com.project.healthcare.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
    private Spinner timeSpinner;
    private Button addSlotButton;

    public DoctorAvailabilityFragment() {
        super(R.layout.fragment_doctor_availability);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        daySpinner = view.findViewById(R.id.spinner_slot_day);
        timeSpinner = view.findViewById(R.id.spinner_slot_time);
        addSlotButton = view.findViewById(R.id.button_add_slot);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_availability_slots);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AvailabilitySlotAdapter(this);
        recyclerView.setAdapter(adapter);

        setupDaySpinner();
        setupTimeSpinner();
        addSlotButton.setOnClickListener(v -> addAvailabilitySlot());

        loadAvailability();
    }

    private void setupDaySpinner() {
        List<String> days = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, days);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daySpinner.setAdapter(spinnerAdapter);
    }

    private void setupTimeSpinner() {
        List<String> slots = createHourlySlotLabels();
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, slots);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSpinner.setAdapter(spinnerAdapter);
    }

    private List<String> createHourlySlotLabels() {
        List<String> slots = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            String start = formatHour(hour);
            String end = formatHour((hour + 1) % 24);
            slots.add(start + " - " + end);
        }
        return slots;
    }

    private String formatHour(int hour24) {
        int hour12 = hour24 % 12 == 0 ? 12 : hour24 % 12;
        String period = hour24 < 12 ? "AM" : "PM";
        return String.format(Locale.getDefault(), "%02d:00 %s", hour12, period);
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
        String selectedSlot = timeSpinner.getSelectedItem() != null ? timeSpinner.getSelectedItem().toString() : "12:00 AM - 01:00 AM";

        String[] parts = selectedSlot.split(" - ");
        if (parts.length != 2) {
            Toast.makeText(requireContext(), "Select a valid time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        String startTime = parts[0].trim();
        String endTime = parts[1].trim();
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
                timeSpinner.setSelection(0);
            } else {
                Toast.makeText(requireContext(), message != null ? message : "Unable to save slot", Toast.LENGTH_LONG).show();
            }
        });
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
