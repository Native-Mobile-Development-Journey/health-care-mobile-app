package com.project.healthcare.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.healthcare.R;
import com.project.healthcare.data.AppRepository;
import com.project.healthcare.data.models.Doctor;
import com.project.healthcare.data.models.DoctorAvailabilitySlot;
import com.project.healthcare.ui.adapter.DateOptionAdapter;
import com.project.healthcare.ui.adapter.TimeOptionAdapter;
import com.project.healthcare.ui.model.DateOption;
import com.project.healthcare.ui.model.TimeOption;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DoctorDetailFragment extends Fragment {

    private static final String ARG_DOCTOR_ID = "arg_doctor_id";
    private static final String ARG_APPOINTMENT_ID = "arg_appointment_id";

    private final AppRepository repository = AppRepository.getInstance();

    private DateOptionAdapter dateOptionAdapter;
    private TimeOptionAdapter timeOptionAdapter;
    private ValueEventListener doctorsListener;

    private static final String TAG = "DoctorDetailFragment";
    private final List<DoctorAvailabilitySlot> availabilitySlots = new ArrayList<>();
    private String doctorId;
    private String appointmentId;
    private Doctor selectedDoctor;

    private TextView doctorNameText;
    private TextView specialtyText;
    private TextView experienceText;
    private TextView patientsText;
    private TextView ratingText;
    private TextView aboutText;
    private TextView scheduleEmptyText;

    public DoctorDetailFragment() {
        super(R.layout.fragment_doctor_detail);
    }

    public static DoctorDetailFragment newInstance(@Nullable String doctorId, @Nullable String appointmentId) {
        DoctorDetailFragment fragment = new DoctorDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOCTOR_ID, doctorId);
        args.putString(ARG_APPOINTMENT_ID, appointmentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            doctorId = getArguments().getString(ARG_DOCTOR_ID);
            appointmentId = getArguments().getString(ARG_APPOINTMENT_ID);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        doctorNameText = view.findViewById(R.id.text_detail_name);
        specialtyText = view.findViewById(R.id.text_detail_specialty);
        experienceText = view.findViewById(R.id.text_stat_experience_value);
        patientsText = view.findViewById(R.id.text_stat_patients_value);
        ratingText = view.findViewById(R.id.text_stat_rating_value);
        aboutText = view.findViewById(R.id.text_about_doctor);
        scheduleEmptyText = view.findViewById(R.id.text_schedule_empty);

        setupDateRecycler(view);
        setupTimeRecycler(view);
        setupConfirmButton(view);

        View backButton = view.findViewById(R.id.button_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        }

        if (doctorId != null) {
            loadDoctorAvailability(doctorId);
        }
        observeDoctors();
    }

    private void setupDateRecycler(View root) {
        RecyclerView recyclerView = root.findViewById(R.id.recycler_detail_dates);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        dateOptionAdapter = new DateOptionAdapter(option -> updateTimeOptionsForSelectedDay());
        recyclerView.setAdapter(dateOptionAdapter);
        dateOptionAdapter.submitList(new ArrayList<>());
    }

    private void setupTimeRecycler(View root) {
        RecyclerView recyclerView = root.findViewById(R.id.recycler_detail_times);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        timeOptionAdapter = new TimeOptionAdapter(option -> {
            // Selection state is managed by the adapter.
        });
        recyclerView.setAdapter(timeOptionAdapter);
        timeOptionAdapter.submitList(new ArrayList<>());
    }

    private List<DateOption> buildDateOptions() {
        List<DateOption> options = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            String day = dayFormat.format(calendar.getTime());
            String date = dateFormat.format(calendar.getTime());
            boolean disabled = i == 6;
            options.add(new DateOption(day, date, disabled));
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return options;
    }

    private void observeDoctors() {
        doctorsListener = repository.observeDoctors(new AppRepository.ListCallback<Doctor>() {
            @Override
            public void onData(List<Doctor> items) {
                Log.d(TAG, "observeDoctors loaded count=" + items.size() + " doctorId=" + doctorId);
                Doctor doctor = repository.findDoctorById(items, doctorId);
                if (doctor != null) {
                    selectedDoctor = doctor;
                    bindDoctor(doctor);
                    return;
                }
                // Always try to load availability even when the doctor profile is not available
                loadDoctorAvailability(doctorId);
                fetchDoctorFromFirestoreUsers();
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
                loadDoctorAvailability(doctorId);
                fetchDoctorFromFirestoreUsers();
            }
        });
    }

    private void fetchDoctorFromFirestoreUsers() {
        if (doctorId == null) {
            return;
        }
        repository.fetchDoctorProfileFromFirestoreUsers(doctorId, new AppRepository.DoctorCallback() {
            @Override
            public void onDoctorLoaded(@Nullable Doctor doctor) {
                if (doctor == null) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Doctor profile not found", Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                selectedDoctor = doctor;
                bindDoctor(doctor);
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
        doctorNameText.setText(doctor.name);
        specialtyText.setText(doctor.specialty);
        experienceText.setText(String.valueOf(doctor.experienceYears));
        patientsText.setText(String.valueOf(doctor.patientCount));
        ratingText.setText(String.format(Locale.getDefault(), "%.1f", doctor.rating));
        aboutText.setText(doctor.bio);
        loadDoctorAvailability(doctor.id);
    }

    private void loadDoctorAvailability(String doctorId) {
        repository.getDoctorAvailability(doctorId, new AppRepository.ListCallback<DoctorAvailabilitySlot>() {
            @Override
            public void onData(List<DoctorAvailabilitySlot> items) {
                availabilitySlots.clear();
                availabilitySlots.addAll(items);
                updateAvailableDays();
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateAvailableDays() {
        List<String> uniqueDays = new ArrayList<>();
        for (DoctorAvailabilitySlot slot : availabilitySlots) {
            if (slot.dayLabel == null) {
                continue;
            }
            if (!uniqueDays.contains(slot.dayLabel)) {
                uniqueDays.add(slot.dayLabel);
            }
        }

        List<DateOption> dayOptions = new ArrayList<>();
        for (String day : uniqueDays) {
            int count = 0;
            for (DoctorAvailabilitySlot slot : availabilitySlots) {
                if (day.equals(slot.dayLabel)) {
                    count++;
                }
            }
            String countLabel = String.format(Locale.getDefault(), "%d slots", count);
            dayOptions.add(new DateOption(day, countLabel, false));
        }

        if (dayOptions.isEmpty()) {
            scheduleEmptyText.setVisibility(View.VISIBLE);
            dateOptionAdapter.submitList(new ArrayList<>());
            timeOptionAdapter.submitList(new ArrayList<>());
            return;
        }

        scheduleEmptyText.setVisibility(View.GONE);
        dateOptionAdapter.submitList(dayOptions);
        updateTimeOptionsForSelectedDay();
    }

    private void updateTimeOptionsForSelectedDay() {
        DateOption selectedDay = dateOptionAdapter.getSelectedItem();
        if (selectedDay == null) {
            timeOptionAdapter.submitList(new ArrayList<>());
            return;
        }

        List<TimeOption> timeOptions = new ArrayList<>();
        for (DoctorAvailabilitySlot slot : availabilitySlots) {
            if (selectedDay.dayLabel != null && selectedDay.dayLabel.equals(slot.dayLabel)) {
                timeOptions.add(new TimeOption(slot.startTime));
            }
        }
        timeOptionAdapter.submitList(timeOptions);
    }

    private void setupConfirmButton(View root) {
        View button = root.findViewById(R.id.button_confirm_schedule);
        button.setOnClickListener(v -> {
            if (selectedDoctor == null) {
                Toast.makeText(requireContext(), R.string.doctor_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }

            DateOption dateOption = dateOptionAdapter.getSelectedItem();
            TimeOption timeOption = timeOptionAdapter.getSelectedItem();
            if (dateOption == null || timeOption == null) {
                Toast.makeText(requireContext(), R.string.schedule_selection_required, Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedDate = dateOption.dayLabel + ", " + dateOption.dateLabel;
            String selectedTime = timeOption.label;

            if (appointmentId != null && !appointmentId.trim().isEmpty()) {
                repository.rescheduleAppointment(appointmentId, selectedDate, selectedTime, this::handleCompletion);
            } else {
                repository.createAppointment(selectedDoctor, selectedDate, selectedTime, this::handleCompletion);
            }
        });
    }

    private void handleCompletion(boolean success, @Nullable String message) {
        if (!isAdded()) {
            return;
        }
        if (success) {
            Toast.makeText(requireContext(), R.string.schedule_confirm_success, Toast.LENGTH_SHORT).show();
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
            return;
        }
        Toast.makeText(requireContext(), message != null ? message : getString(R.string.auth_error_generic), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (doctorsListener != null) {
            repository.removeDoctorsListener(doctorsListener);
        }
    }
}
