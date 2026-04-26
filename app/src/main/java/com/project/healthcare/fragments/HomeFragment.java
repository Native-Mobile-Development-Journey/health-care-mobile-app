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
import com.project.healthcare.ui.adapter.AppointmentAdapter;
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
    private RecyclerView todayScheduleRecycler;
    private AppointmentAdapter todayAppointmentAdapter;
    private TextView seeMoreButton;
    private TextInputEditText searchInput;
    private String uid;

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userNameText = view.findViewById(R.id.text_user_name);
        todayScheduleRecycler = view.findViewById(R.id.recycler_today_appointments);
        searchInput = view.findViewById(R.id.input_home_search);
        seeMoreButton = view.findViewById(R.id.button_see_more_doctors);
        doctorEmptyText = view.findViewById(R.id.text_home_doctor_empty);

        userNameText.setText(repository.getCurrentUserDisplayName());

        setupServices(view);
        setupDoctors(view);
        setupTodaySchedule();
        setupSearch();
        setupSeeMore();

        uid = repository.getCurrentUserUid();
        fetchDoctorRecommendations();
        observeAppointments();
    }

    private void setupServices(View root) {
        RecyclerView servicesRecycler = root.findViewById(R.id.recycler_home_services);
        servicesRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 4));

        serviceAdapter = new ServiceAdapter(4, item -> {
            if (item.enabled) {
                Toast.makeText(requireContext(), item.label + " selected", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), item.label + " Service coming soon...", Toast.LENGTH_SHORT).show();
            }
        });
        servicesRecycler.setAdapter(serviceAdapter);

        List<ServiceItem> items = new ArrayList<>();
        items.add(new ServiceItem(getString(R.string.service_emergency), android.R.drawable.ic_dialog_alert, false));
        items.add(new ServiceItem(getString(R.string.service_hospital), android.R.drawable.ic_menu_compass, false));
        items.add(new ServiceItem(getString(R.string.service_blood), android.R.drawable.ic_menu_edit, false));
        items.add(new ServiceItem(getString(R.string.service_prescription), android.R.drawable.ic_menu_agenda, false));
        items.add(new ServiceItem(getString(R.string.service_doctor), android.R.drawable.ic_menu_info_details, true));
        items.add(new ServiceItem(getString(R.string.service_checkup), android.R.drawable.ic_menu_recent_history, false));
        items.add(new ServiceItem(getString(R.string.service_location), android.R.drawable.ic_menu_mylocation, false));
        items.add(new ServiceItem(getString(R.string.service_radiology), android.R.drawable.ic_menu_search, false));
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

    private void setupSeeMore() {
        seeMoreButton.setOnClickListener(v -> showDoctorRecommendationSheet());
    }

    private void setupTodaySchedule() {
        todayScheduleRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        todayAppointmentAdapter = new AppointmentAdapter(new AppointmentAdapter.OnAppointmentInteractionListener() {
            @Override
            public void onAppointmentOpen(Appointment appointment) {
                openDoctorDetail(appointment.doctorId, appointment.id);
            }

            @Override
            public void onAppointmentAction(Appointment appointment) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), appointment.normalizedStatus() + " action", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAppointmentDelete(Appointment appointment) {
                // no delete action in home schedule card
            }
        }, false);
        todayScheduleRecycler.setAdapter(todayAppointmentAdapter);
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
        String currentDay = new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date());
        String currentDate = new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date());

        List<Appointment> todayAppointments = new ArrayList<>();
        for (Appointment appointment : allAppointments) {
            if (appointment.date != null && (appointment.date.contains(currentDay) || appointment.date.contains(currentDate))) {
                todayAppointments.add(appointment);
            }
        }

        todayAppointmentAdapter.submitList(todayAppointments);
    }

    private void openDoctorDetail(Doctor doctor) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openDoctorDetail(doctor == null ? null : doctor.id, null, doctor);
        }
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
