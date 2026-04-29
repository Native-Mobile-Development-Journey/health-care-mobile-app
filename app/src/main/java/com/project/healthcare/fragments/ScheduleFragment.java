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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.healthcare.MainActivity;
import com.project.healthcare.R;
import com.project.healthcare.data.AppRepository;
import com.project.healthcare.data.models.Appointment;
import com.project.healthcare.ui.adapter.AppointmentAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScheduleFragment extends Fragment implements AppointmentAdapter.OnAppointmentInteractionListener {

    private final AppRepository repository = AppRepository.getInstance();
    private final List<Appointment> allAppointments = new ArrayList<>();
    private final List<Appointment> filteredAppointments = new ArrayList<>();

    private AppointmentAdapter appointmentAdapter;
    private ListenerRegistration appointmentsListener;
    private String uid;

    private TextInputEditText searchInput;
    private TabLayout tabLayout;
    private TextView emptyText;

    public ScheduleFragment() {
        super(R.layout.fragment_schedule);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchInput = view.findViewById(R.id.input_schedule_search);
        tabLayout = view.findViewById(R.id.tab_schedule_status);
        emptyText = view.findViewById(R.id.text_schedule_empty);

        setupRecycler(view);
        setupTabs();
        setupSearch();

        uid = repository.getCurrentUserUid();
        observeAppointments();
    }

    private void setupRecycler(View root) {
        RecyclerView recyclerView = root.findViewById(R.id.recycler_schedule_appointments);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        appointmentAdapter = new AppointmentAdapter(this, false);
        recyclerView.setAdapter(appointmentAdapter);
    }

    private void setupTabs() {
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText(R.string.status_all));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.status_upcoming));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.status_completed));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.status_cancel));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                applyFilters();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                applyFilters();
            }
        });
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void observeAppointments() {
        if (uid == null) {
            emptyText.setVisibility(View.VISIBLE);
            return;
        }
        if (appointmentsListener != null) {
            appointmentsListener.remove();
            appointmentsListener = null;
        }
        appointmentsListener = repository.observePatientAppointmentsFromFirestore(uid, new AppRepository.ListCallback<Appointment>() {
            @Override
            public void onData(List<Appointment> items) {
                if (!isAdded()) return;
                allAppointments.clear();
                allAppointments.addAll(items);
                applyFilters();
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void applyFilters() {
        if (!isAdded() || searchInput == null || tabLayout == null) {
            return;
        }
        String query = searchInput.getText() == null ? "" : searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        String selectedTab = getSelectedTabLabel();

        filteredAppointments.clear();
        for (Appointment appointment : allAppointments) {
            if (!matchesStatusFilter(selectedTab, appointment.normalizedStatus())) {
                continue;
            }
            if (!query.isEmpty()
                    && !containsIgnoreCase(appointment.doctorName, query)
                    && !containsIgnoreCase(appointment.specialty, query)
                    && !containsIgnoreCase(appointment.hospital, query)) {
                continue;
            }
            filteredAppointments.add(appointment);
        }

        appointmentAdapter.submitList(filteredAppointments);
        emptyText.setVisibility(filteredAppointments.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String getSelectedTabLabel() {
        if (!isAdded()) return "";
        TabLayout.Tab tab = tabLayout.getTabAt(tabLayout.getSelectedTabPosition());
        if (tab == null || tab.getText() == null) {
            return getString(R.string.status_all);
        }
        return tab.getText().toString();
    }

    private boolean matchesStatusFilter(String selectedTab, String status) {
        if (!isAdded()) return false;
        if (selectedTab.equalsIgnoreCase(getString(R.string.status_all))) {
            return true;
        }
        if (selectedTab.equalsIgnoreCase(getString(R.string.status_upcoming))) {
            return Appointment.STATUS_UPCOMING.equals(status);
        }
        if (selectedTab.equalsIgnoreCase(getString(R.string.status_completed))) {
            return Appointment.STATUS_COMPLETED.equals(status);
        }
        return Appointment.STATUS_CANCELED.equals(status);
    }

    private boolean containsIgnoreCase(String value, String query) {
        if (value == null) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(query);
    }

    @Override
    public void onAppointmentOpen(Appointment appointment) {
        openDoctorDetail(appointment.doctorId, appointment.id);
    }

    @Override
    public void onAppointmentAction(Appointment appointment) {
        if (!isAdded()) return;
        String status = appointment.normalizedStatus();
        if (Appointment.STATUS_UPCOMING.equals(status)) {
            openDoctorDetail(appointment.doctorId, appointment.id);
        } else {
            Toast.makeText(requireContext(), R.string.schedule_action_new_appointment, Toast.LENGTH_SHORT).show();
            openDoctorDetail(appointment.doctorId, null);
        }
    }

    @Override
    public void onAppointmentDelete(Appointment appointment) {
        if (!isAdded() || appointment == null) {
            return;
        }
        if (!Appointment.STATUS_UPCOMING.equals(appointment.normalizedStatus())) {
            Toast.makeText(requireContext(), "Appointment cannot be modified after " + appointment.normalizedStatus(), Toast.LENGTH_SHORT).show();
            return;
        }
        showStatusChangeDialog(appointment);
    }

    private void showStatusChangeDialog(Appointment appointment) {
        if (!isAdded()) return;
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.change_appointment_status_title)
                .setItems(new CharSequence[]{getString(R.string.status_cancel)}, (d, which) -> {
                    String newStatus = Appointment.STATUS_CANCELED;
                    repository.updateAppointmentStatus(appointment.id, newStatus, (success, message) -> {
                        if (!isAdded()) {
                            return;
                        }
                        if (success) {
                            requireActivity().runOnUiThread(() -> {
                                if (!isAdded()) return;
                                appointment.status = newStatus;
                                applyFilters();
                                Toast.makeText(requireContext(), getString(R.string.appointment_status_updated, newStatus), Toast.LENGTH_SHORT).show();
                            });
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), message != null ? message : getString(R.string.auth_error_generic), Toast.LENGTH_SHORT).show();
                        });
                    });
                })
                .setNegativeButton(R.string.cancel, (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            if (!isAdded()) return;
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(requireContext().getColor(R.color.primary_500));
        });
        dialog.show();
    }

    private void openDoctorDetail(@Nullable String doctorId, @Nullable String appointmentId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openDoctorDetail(doctorId, appointmentId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (appointmentsListener != null) {
            appointmentsListener.remove();
            appointmentsListener = null;
        }
    }
}
