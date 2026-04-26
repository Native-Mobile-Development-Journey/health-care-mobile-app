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
import com.project.healthcare.data.models.Appointment;
import com.project.healthcare.data.models.Doctor;
import com.project.healthcare.data.models.DoctorAvailabilitySlot;
import com.project.healthcare.ui.adapter.DateOptionAdapter;
import com.project.healthcare.ui.adapter.TimeOptionAdapter;
import com.project.healthcare.ui.model.DateOption;
import com.project.healthcare.ui.model.TimeOption;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DoctorDetailFragment extends Fragment {

    private static final String ARG_DOCTOR_ID = "arg_doctor_id";
    private static final String ARG_APPOINTMENT_ID = "arg_appointment_id";
    private static final String ARG_DOCTOR_OBJECT = "arg_doctor_object";
    private static final String TAG = "DoctorDetailFragment";

    private final AppRepository repository = AppRepository.getInstance();

    private DateOptionAdapter dateOptionAdapter;
    private TimeOptionAdapter timeOptionAdapter;

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
        return newInstance(doctorId, appointmentId, null);
    }

    public static DoctorDetailFragment newInstance(@Nullable String doctorId, @Nullable String appointmentId, @Nullable Doctor doctor) {
        DoctorDetailFragment fragment = new DoctorDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DOCTOR_ID, doctorId);
        args.putString(ARG_APPOINTMENT_ID, appointmentId);
        if (doctor != null) {
            args.putSerializable(ARG_DOCTOR_OBJECT, doctor);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            doctorId = getArguments().getString(ARG_DOCTOR_ID);
            appointmentId = getArguments().getString(ARG_APPOINTMENT_ID);
            selectedDoctor = (Doctor) getArguments().getSerializable(ARG_DOCTOR_OBJECT);
            Log.d(TAG, "onCreate: Retrieved doctor from bundle - " + (selectedDoctor != null ? selectedDoctor.name : "null"));
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

        if (doctorId == null || doctorId.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.doctor_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }

        // If we already have the doctor object from the bundle, use it directly
        if (selectedDoctor != null) {
            Log.d(TAG, "Using doctor from bundle: " + selectedDoctor.name);
            bindDoctor(selectedDoctor);
        } else {
            Log.d(TAG, "Doctor not in bundle, fetching from Firestore");
            loadDoctorProfileAndBind(doctorId);
        }
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

    private void loadDoctorProfileAndBind(@NonNull String targetDoctorId) {
        Log.d(TAG, "loadDoctorProfileAndBind: Starting for doctor ID " + targetDoctorId);
        repository.getDoctorProfile(targetDoctorId, new AppRepository.DoctorCallback() {
            @Override
            public void onDoctorLoaded(@Nullable Doctor doctor) {
                Log.d(TAG, "getDoctorProfile callback: doctor = " + (doctor != null ? doctor.name : "null"));
                if (doctor != null) {
                    selectedDoctor = doctor;
                    Log.d(TAG, "Found doctor in 'doctors' collection: " + doctor.name);
                    bindDoctor(doctor);
                    return;
                }
                Log.d(TAG, "Doctor not found in 'doctors' collection, trying 'users' collection");
                loadDoctorProfileFromUsers(targetDoctorId);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "getDoctorProfile error: " + message);
                loadDoctorProfileFromUsers(targetDoctorId);
            }
        });
    }

    private void loadDoctorProfileFromUsers(@NonNull String targetDoctorId) {
        Log.d(TAG, "loadDoctorProfileFromUsers: Starting for doctor ID " + targetDoctorId);
        repository.fetchDoctorProfileFromFirestoreUsers(targetDoctorId, new AppRepository.DoctorCallback() {
            @Override
            public void onDoctorLoaded(@Nullable Doctor doctor) {
                Log.d(TAG, "fetchDoctorProfileFromFirestoreUsers callback: doctor = " + (doctor != null ? doctor.name : "null"));
                if (doctor == null) {
                    Log.w(TAG, "Doctor not found in 'users' collection either");
                    if (isAdded()) {
                        Toast.makeText(requireContext(), R.string.doctor_not_ready, Toast.LENGTH_SHORT).show();
                    }
                    loadDoctorAvailability(targetDoctorId);
                    // Set placeholder text
                    if (aboutText != null) {
                        aboutText.setText(getString(R.string.about_doctor_unavailable));
                    }
                    // Set placeholder stats
                    if (doctorNameText != null) {
                        doctorNameText.setText(getString(R.string.doctor_default_name));
                    }
                    if (specialtyText != null) {
                        specialtyText.setText(getString(R.string.specialty_default));
                    }
                    return;
                }
                Log.d(TAG, "Found doctor in 'users' collection: name=" + doctor.name + ", specialty=" + doctor.specialty);
                selectedDoctor = doctor;
                bindDoctor(doctor);
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "fetchDoctorProfileFromFirestoreUsers error: " + message);
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Failed to load doctor: " + message, Toast.LENGTH_SHORT).show();
                }
                loadDoctorAvailability(targetDoctorId);
                if (aboutText != null) {
                    aboutText.setText(getString(R.string.about_doctor_unavailable));
                }
            }
        });
    }

    private void bindDoctor(Doctor doctor) {
        Log.d(TAG, "bindDoctor: Binding doctor data - name=" + doctor.name + 
              ", specialty=" + doctor.specialty + 
              ", experience=" + doctor.experienceYears +
              ", patients=" + doctor.patientCount +
              ", rating=" + doctor.rating +
              ", bio=" + (doctor.bio != null ? doctor.bio.substring(0, Math.min(50, doctor.bio.length())) : "null"));
        
        String displayName = valueOrFallback(doctor.name, getString(R.string.doctor_default_name));
        String displaySpecialty = valueOrFallback(doctor.specialty, getString(R.string.specialty_default));
        String displayExperience = String.valueOf(Math.max(0, doctor.experienceYears));
        String displayPatients = String.valueOf(Math.max(0, doctor.patientCount));
        String displayRating = String.format(Locale.getDefault(), "%.1f", Math.max(0d, doctor.rating));
        String displayAbout = valueOrFallback(doctor.bio, getString(R.string.about_doctor_unavailable));
        
        Log.d(TAG, "bindDoctor: Setting UI values - displayName=" + displayName + 
              ", displaySpecialty=" + displaySpecialty +
              ", displayExperience=" + displayExperience +
              ", displayRating=" + displayRating);
        
        if (doctorNameText != null) {
            doctorNameText.setText(displayName);
        }
        if (specialtyText != null) {
            specialtyText.setText(displaySpecialty);
        }
        if (experienceText != null) {
            experienceText.setText(displayExperience);
        }
        if (patientsText != null) {
            patientsText.setText(displayPatients);
        }
        if (ratingText != null) {
            ratingText.setText(displayRating);
        }
        if (aboutText != null) {
            aboutText.setText(displayAbout);
        }

        String availabilityDoctorId = doctor.id != null && !doctor.id.trim().isEmpty() ? doctor.id : doctorId;
        if (availabilityDoctorId != null) {
            loadDoctorAvailability(availabilityDoctorId);
        }
    }

    @NonNull
    private String valueOrFallback(@Nullable String value, @NonNull String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
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
            DateOption dateOption = dateOptionAdapter.getSelectedItem();
            TimeOption timeOption = timeOptionAdapter.getSelectedItem();
            if (dateOption == null || timeOption == null) {
                Toast.makeText(requireContext(), R.string.schedule_selection_required, Toast.LENGTH_SHORT).show();
                return;
            }

            String selectedDate = dateOption.dayLabel + ", " + dateOption.dateLabel;
            String selectedTime = timeOption.label;

            validatePatientConflict(selectedDate, selectedTime, appointmentId, hasConflict -> {
                if (!isAdded()) {
                    return;
                }
                if (hasConflict) {
                    Toast.makeText(requireContext(), R.string.schedule_conflict_error, Toast.LENGTH_LONG).show();
                    return;
                }

                if (appointmentId != null && !appointmentId.trim().isEmpty()) {
                    repository.rescheduleAppointment(appointmentId, selectedDate, selectedTime, this::handleCompletion);
                    return;
                }

                if (selectedDoctor != null) {
                    repository.createAppointment(selectedDoctor, selectedDate, selectedTime, this::handleCompletion);
                    return;
                }

                if (doctorId == null) {
                    Toast.makeText(requireContext(), R.string.doctor_not_ready, Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(requireContext(), R.string.schedule_confirming, Toast.LENGTH_SHORT).show();
                loadDoctorProfileForBooking(doctorId, new AppRepository.DoctorCallback() {
                    @Override
                    public void onDoctorLoaded(@Nullable Doctor doctor) {
                        if (!isAdded()) {
                            return;
                        }
                        if (doctor == null) {
                            Toast.makeText(requireContext(), R.string.doctor_not_ready, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        selectedDoctor = doctor;
                        bindDoctor(doctor);
                        repository.createAppointment(doctor, selectedDate, selectedTime, DoctorDetailFragment.this::handleCompletion);
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(requireContext(), message != null ? message : getString(R.string.auth_error_generic), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
    }

    private void loadDoctorProfileForBooking(@NonNull String targetDoctorId, AppRepository.DoctorCallback callback) {
        repository.getDoctorProfile(targetDoctorId, new AppRepository.DoctorCallback() {
            @Override
            public void onDoctorLoaded(@Nullable Doctor doctor) {
                if (doctor != null) {
                    callback.onDoctorLoaded(doctor);
                    return;
                }
                repository.fetchDoctorProfileFromFirestoreUsers(targetDoctorId, callback);
            }

            @Override
            public void onError(String message) {
                repository.fetchDoctorProfileFromFirestoreUsers(targetDoctorId, callback);
            }
        });
    }

    private void handleCompletion(boolean success, @Nullable String message) {
        if (!isAdded()) {
            return;
        }
        if (success) {
            Toast.makeText(requireContext(), R.string.schedule_confirm_success, Toast.LENGTH_SHORT).show();
            if (getActivity() instanceof com.project.healthcare.MainActivity && !isCurrentUserDoctor()) {
                ((com.project.healthcare.MainActivity) getActivity()).openScheduleTab();
                return;
            }
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
            return;
        }
        Toast.makeText(requireContext(), message != null ? message : getString(R.string.auth_error_generic), Toast.LENGTH_SHORT).show();
    }

    private boolean isCurrentUserDoctor() {
        String uid = repository.getCurrentUserUid();
        return uid != null && uid.equals(doctorId);
    }

    private interface ConflictCallback {
        void onCheckComplete(boolean hasConflict);
    }

    private void validatePatientConflict(String date, String time, @Nullable String currentAppointmentId, ConflictCallback callback) {
        String patientUid = repository.getCurrentUserUid();
        if (patientUid == null) {
            callback.onCheckComplete(false);
            return;
        }

        repository.getPatientAppointments(patientUid, new AppRepository.ListCallback<Appointment>() {
            @Override
            public void onData(List<Appointment> items) {
                boolean conflict = false;
                for (Appointment appointment : items) {
                    if (appointment == null || appointment.id == null) {
                        continue;
                    }
                    if (currentAppointmentId != null && currentAppointmentId.equals(appointment.id)) {
                        continue;
                    }
                    if (Appointment.STATUS_CANCELED.equalsIgnoreCase(appointment.normalizedStatus())) {
                        continue;
                    }
                    if (date.equals(appointment.date) && time.equals(appointment.time)) {
                        conflict = true;
                        break;
                    }
                }
                callback.onCheckComplete(conflict);
            }

            @Override
            public void onError(String message) {
                callback.onCheckComplete(false);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
