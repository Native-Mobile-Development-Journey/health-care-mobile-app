package com.project.healthcare.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.healthcare.MainActivity;
import com.project.healthcare.R;
import com.project.healthcare.data.AppRepository;
import com.project.healthcare.data.models.Appointment;
import com.project.healthcare.data.models.Doctor;
import com.project.healthcare.ui.adapter.DoctorAdapter;
import com.project.healthcare.ui.adapter.ServiceAdapter;
import com.project.healthcare.ui.model.ServiceItem;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private final AppRepository repository = AppRepository.getInstance();
    private final List<Doctor> allDoctors = new ArrayList<>();
    private final List<Doctor> filteredDoctors = new ArrayList<>();
    private final List<Appointment> allAppointments = new ArrayList<>();

    private DoctorAdapter doctorAdapter;
    private ServiceAdapter serviceAdapter;
    private ValueEventListener doctorsListener;
    private ValueEventListener appointmentsListener;

    private TextView userNameText;
    private TextView scheduleDoctorText;
    private TextView scheduleSpecialtyText;
    private TextView scheduleHospitalText;
    private TextView scheduleDateText;
    private TextView scheduleTimeText;
    private TextInputEditText searchInput;
    private View scheduleCard;
    private Appointment selectedScheduleAppointment;
    private String uid;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userNameText = view.findViewById(R.id.text_user_name);
        scheduleDoctorText = view.findViewById(R.id.text_schedule_doctor);
        scheduleSpecialtyText = view.findViewById(R.id.text_schedule_specialty);
        scheduleHospitalText = view.findViewById(R.id.text_schedule_hospital);
        scheduleDateText = view.findViewById(R.id.text_schedule_date);
        scheduleTimeText = view.findViewById(R.id.text_schedule_time);
        searchInput = view.findViewById(R.id.input_home_search);
        scheduleCard = view.findViewById(R.id.card_schedule_today);

        userNameText.setText(repository.getCurrentUserDisplayName());

        setupServices(view);
        setupDoctors(view);
        setupSearch();
        setupScheduleClick();

        uid = repository.getCurrentUserUid();
        observeDoctors();
        observeAppointments();
    }

    private void setupServices(View root) {
        RecyclerView servicesRecycler = root.findViewById(R.id.recycler_home_services);
        servicesRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 4));

        serviceAdapter = new ServiceAdapter(4, item ->
                Toast.makeText(requireContext(), item.label + " selected", Toast.LENGTH_SHORT).show());
        servicesRecycler.setAdapter(serviceAdapter);

        List<ServiceItem> items = new ArrayList<>();
        items.add(new ServiceItem(getString(R.string.service_emergency), android.R.drawable.ic_dialog_alert));
        items.add(new ServiceItem(getString(R.string.service_hospital), android.R.drawable.ic_menu_compass));
        items.add(new ServiceItem(getString(R.string.service_blood), android.R.drawable.ic_menu_edit));
        items.add(new ServiceItem(getString(R.string.service_prescription), android.R.drawable.ic_menu_agenda));
        items.add(new ServiceItem(getString(R.string.service_doctor), android.R.drawable.ic_menu_info_details));
        items.add(new ServiceItem(getString(R.string.service_checkup), android.R.drawable.ic_menu_recent_history));
        items.add(new ServiceItem(getString(R.string.service_location), android.R.drawable.ic_menu_mylocation));
        items.add(new ServiceItem(getString(R.string.service_radiology), android.R.drawable.ic_menu_search));
        serviceAdapter.submitList(items);
    }

    private void setupDoctors(View root) {
        RecyclerView doctorsRecycler = root.findViewById(R.id.recycler_home_doctors);
        doctorsRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        doctorAdapter = new DoctorAdapter(this::openDoctorDetail);
        doctorsRecycler.setAdapter(doctorAdapter);
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyDoctorFilter(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupScheduleClick() {
        scheduleCard.setOnClickListener(v -> {
            if (selectedScheduleAppointment != null) {
                openDoctorDetail(selectedScheduleAppointment.doctorId, selectedScheduleAppointment.id);
            } else {
                openDoctorDetail((Doctor) null);
            }
        });
    }

    private void observeDoctors() {
        doctorsListener = repository.observeDoctors(new AppRepository.ListCallback<Doctor>() {
            @Override
            public void onData(List<Doctor> items) {
                allDoctors.clear();
                allDoctors.addAll(items);
                String query = searchInput.getText() == null ? "" : searchInput.getText().toString();
                applyDoctorFilter(query);
                bindScheduleCard();
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void observeAppointments() {
        if (uid == null) {
            return;
        }
        appointmentsListener = repository.observeAppointments(uid, new AppRepository.ListCallback<Appointment>() {
            @Override
            public void onData(List<Appointment> items) {
                allAppointments.clear();
                allAppointments.addAll(items);
                bindScheduleCard();
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void applyDoctorFilter(String query) {
        filteredDoctors.clear();

        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean hasQuery = !normalized.isEmpty();

        for (Doctor doctor : allDoctors) {
            boolean matchesQuery = !hasQuery
                    || containsIgnoreCase(doctor.name, normalized)
                    || containsIgnoreCase(doctor.specialty, normalized)
                    || containsIgnoreCase(doctor.hospital, normalized);
            if (!matchesQuery) {
                continue;
            }

            if (!hasQuery && !doctor.recommended) {
                continue;
            }
            filteredDoctors.add(doctor);
        }

        if (!hasQuery && filteredDoctors.isEmpty()) {
            filteredDoctors.addAll(allDoctors);
        }

        doctorAdapter.submitList(filteredDoctors);
    }

    private void bindScheduleCard() {
        Appointment upcoming = null;
        if (!allAppointments.isEmpty()) {
            for (Appointment appointment : allAppointments) {
                if (Appointment.STATUS_UPCOMING.equals(appointment.normalizedStatus())) {
                    upcoming = appointment;
                    break;
                }
            }
            if (upcoming == null) {
                upcoming = allAppointments.get(0);
            }
        }

        selectedScheduleAppointment = upcoming;
        if (upcoming != null) {
            scheduleDoctorText.setText(upcoming.doctorName);
            scheduleSpecialtyText.setText(upcoming.specialty);
            scheduleHospitalText.setText(upcoming.hospital);
            scheduleDateText.setText(upcoming.date);
            scheduleTimeText.setText(upcoming.time);
            return;
        }

        if (!allDoctors.isEmpty()) {
            Doctor doctor = allDoctors.get(0);
            scheduleDoctorText.setText(doctor.name);
            scheduleSpecialtyText.setText(doctor.specialty);
            scheduleHospitalText.setText(doctor.hospital);
        }
        scheduleDateText.setText(getString(R.string.no_date_selected));
        scheduleTimeText.setText(getString(R.string.no_time_selected));
    }

    private void openDoctorDetail(Doctor doctor) {
        openDoctorDetail(doctor == null ? null : doctor.id, null);
    }

    private void openDoctorDetail(@Nullable String doctorId, @Nullable String appointmentId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openDoctorDetail(doctorId, appointmentId);
        }
    }

    private boolean containsIgnoreCase(String value, String query) {
        if (value == null) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(query);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (doctorsListener != null) {
            repository.removeDoctorsListener(doctorsListener);
        }
        if (appointmentsListener != null && uid != null) {
            repository.removeAppointmentsListener(uid, appointmentsListener);
        }
    }
}
