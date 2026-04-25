package com.project.healthcare.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private final AppRepository repository = AppRepository.getInstance();
    private static final int MAX_RECOMMENDED_DOCTORS = 3;

    private final List<Doctor> allDoctors = new ArrayList<>();
    private final List<Doctor> recommendedDoctors = new ArrayList<>();
    private final List<Doctor> filteredDoctors = new ArrayList<>();
    private final List<Appointment> allAppointments = new ArrayList<>();

    private RecyclerView doctorsRecycler;
    private DoctorAdapter doctorAdapter;
    private ServiceAdapter serviceAdapter;
    private ValueEventListener doctorsListener;
    private ValueEventListener appointmentsListener;

    private TextView userNameText;
    private TextView doctorEmptyText;
    private TextView scheduleDoctorText;
    private TextView scheduleSpecialtyText;
    private TextView scheduleHospitalText;
    private TextView scheduleDateText;
    private TextView scheduleTimeText;
    private TextView seeMoreButton;
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
        seeMoreButton = view.findViewById(R.id.button_see_more_doctors);
        scheduleCard = view.findViewById(R.id.card_schedule_today);
        doctorEmptyText = view.findViewById(R.id.text_home_doctor_empty);

        userNameText.setText(repository.getCurrentUserDisplayName());

        setupServices(view);
        setupDoctors(view);
        setupSearch();
        setupScheduleClick();
        setupSeeMore();

        uid = repository.getCurrentUserUid();
        fetchDoctorRecommendations();
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
        doctorsRecycler = root.findViewById(R.id.recycler_home_doctors);
        doctorsRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        doctorAdapter = new DoctorAdapter(this::openDoctorDetail);
        doctorsRecycler.setAdapter(doctorAdapter);
        updateDoctorEmptyState();
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
            } else if (!allDoctors.isEmpty()) {
                openDoctorDetail(allDoctors.get(0).id, null);
            }
        });
    }

    private void setupSeeMore() {
        seeMoreButton.setOnClickListener(v -> showDoctorRecommendationSheet());
    }

    private void fetchDoctorRecommendations() {
        repository.fetchDoctorProfilesFromFirestoreUsers(new AppRepository.ListCallback<Doctor>() {
            @Override
            public void onData(List<Doctor> items) {
                Log.d(TAG, "fetchDoctorProfilesFromFirestoreUsers loaded count=" + items.size());
                if (items.isEmpty()) {
                    fetchDoctorsFromFirestoreFallback();
                    return;
                }
                populateDoctorRecommendations(items);
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
                fetchDoctorsFromFirestoreFallback();
            }
        });
    }

    private void fetchDoctorsFromFirestoreFallback() {
        repository.fetchAllDoctorProfiles(new AppRepository.ListCallback<Doctor>() {
            @Override
            public void onData(List<Doctor> items) {
                Log.d(TAG, "fetchAllDoctorProfiles loaded count=" + items.size());
                if (items.isEmpty()) {
                    fetchDoctorUsersFromFirestoreFallback();
                    return;
                }
                populateDoctorRecommendations(items);
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
                fetchDoctorUsersFromFirestoreFallback();
            }
        });
    }

    private void fetchDoctorUsersFromFirestoreFallback() {
        repository.fetchDoctorProfilesFromFirestoreUsers(new AppRepository.ListCallback<Doctor>() {
            @Override
            public void onData(List<Doctor> items) {
                Log.d(TAG, "fetchDoctorProfilesFromFirestoreUsers loaded count=" + items.size());
                if (items.isEmpty()) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "No doctor profiles found in Firestore users", Toast.LENGTH_SHORT).show();
                    }
                    allDoctors.clear();
                    populateDoctorRecommendations(allDoctors);
                    return;
                }
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Loaded " + items.size() + " doctors from Firestore users", Toast.LENGTH_SHORT).show();
                }
                populateDoctorRecommendations(items);
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
                allDoctors.clear();
                populateDoctorRecommendations(allDoctors);
            }
        });
    }

    private void populateDoctorRecommendations(List<Doctor> items) {
        allDoctors.clear();
        allDoctors.addAll(items);

        recommendedDoctors.clear();
        recommendedDoctors.addAll(allDoctors);

        applyDoctorFilter("");
        bindScheduleCard();
    }

    private void showDoctorRecommendationSheet() {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_doctor_list, null);
        RecyclerView recyclerView = sheetView.findViewById(R.id.recycler_doctor_list);
        TextView emptyText = sheetView.findViewById(R.id.text_doctor_list_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        DoctorAdapter sheetAdapter = new DoctorAdapter(doctor -> {
            dialog.dismiss();
            openDoctorDetail(doctor.id, null);
        });
        recyclerView.setAdapter(sheetAdapter);
        sheetAdapter.submitList(allDoctors);

        emptyText.setVisibility(allDoctors.isEmpty() ? View.VISIBLE : View.GONE);

        dialog.setContentView(sheetView);
        dialog.show();
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

        if (!hasQuery) {
            filteredDoctors.addAll(recommendedDoctors.isEmpty() ? allDoctors : recommendedDoctors);
        } else {
            for (Doctor doctor : allDoctors) {
                boolean matchesQuery = containsIgnoreCase(doctor.name, normalized)
                        || containsIgnoreCase(doctor.specialty, normalized)
                        || containsIgnoreCase(doctor.hospital, normalized);
                if (matchesQuery) {
                    filteredDoctors.add(doctor);
                }
            }
        }

        doctorAdapter.submitList(filteredDoctors);
        updateDoctorEmptyState();
    }

    private void updateDoctorEmptyState() {
        boolean isEmpty = filteredDoctors.isEmpty();
        doctorsRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        doctorEmptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void bindScheduleCard() {
        Appointment todayAppointment = null;
        Appointment upcoming = null;
        String currentDay = new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date());
        String currentDate = new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date());

        for (Appointment appointment : allAppointments) {
            if (appointment.date != null && (appointment.date.contains(currentDay) || appointment.date.contains(currentDate))) {
                todayAppointment = appointment;
                break;
            }
            if (upcoming == null && Appointment.STATUS_UPCOMING.equals(appointment.normalizedStatus())) {
                upcoming = appointment;
            }
        }

        Appointment displayAppointment = todayAppointment != null ? todayAppointment : upcoming;
        if (displayAppointment == null && !allAppointments.isEmpty()) {
            displayAppointment = allAppointments.get(0);
        }

        selectedScheduleAppointment = displayAppointment;
        if (displayAppointment != null) {
            scheduleDoctorText.setText(displayAppointment.doctorName);
            scheduleSpecialtyText.setText(displayAppointment.specialty);
            scheduleHospitalText.setText(displayAppointment.hospital);
            scheduleDateText.setText(displayAppointment.date);
            scheduleTimeText.setText(displayAppointment.time);
            return;
        }

        scheduleDoctorText.setText(getString(R.string.schedule_no_appointment_today));
        scheduleSpecialtyText.setText(getString(R.string.schedule_book_doctor));
        scheduleHospitalText.setText(getString(R.string.hospital_default));
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
