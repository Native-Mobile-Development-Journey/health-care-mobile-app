package com.project.healthcare.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.healthcare.R;
import com.project.healthcare.data.AppRepository;
import com.project.healthcare.data.models.Conversation;
import com.project.healthcare.data.models.Doctor;
import com.project.healthcare.ui.adapter.DoctorAdapter;

import java.util.ArrayList;
import java.util.List;

public class SelectDoctorFragment extends Fragment {

    private final AppRepository repository = AppRepository.getInstance();
    private final List<Doctor> doctors = new ArrayList<>();

    private DoctorAdapter adapter;
    private TextView emptyText;
    private String uid;
    private boolean isOpeningChat;

    public SelectDoctorFragment() {
        super(R.layout.fragment_select_doctor);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyText = view.findViewById(R.id.text_select_doctor_empty);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_select_doctor);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        uid = repository.getCurrentUserUid();

        adapter = new DoctorAdapter(this::openDoctorChat);
        recyclerView.setAdapter(adapter);

        fetchDoctorProfiles();
    }

    private void fetchDoctorProfiles() {
        repository.fetchDoctorProfilesFromFirestoreUsers(new AppRepository.ListCallback<Doctor>() {
            @Override
            public void onData(List<Doctor> items) {
                if (items == null || items.isEmpty()) {
                    fetchDoctorProfilesFromDoctorsCollection();
                    return;
                }
                updateDoctors(items);
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
                fetchDoctorProfilesFromDoctorsCollection();
            }
        });
    }

    private void fetchDoctorProfilesFromDoctorsCollection() {
        repository.fetchAllDoctorProfiles(new AppRepository.ListCallback<Doctor>() {
            @Override
            public void onData(List<Doctor> items) {
                updateDoctors(items);
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateDoctors(@Nullable List<Doctor> items) {
        doctors.clear();
        if (items != null) {
            doctors.addAll(items);
        }
        adapter.submitList(doctors);
        emptyText.setVisibility(doctors.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openDoctorChat(Doctor doctor) {
        if (isOpeningChat) {
            return;
        }
        if (uid == null) {
            Toast.makeText(requireContext(), R.string.auth_error_generic, Toast.LENGTH_SHORT).show();
            return;
        }
        if (doctor == null || doctor.id == null || doctor.id.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.auth_error_generic, Toast.LENGTH_SHORT).show();
            return;
        }

        String doctorName = doctor.name != null && !doctor.name.trim().isEmpty()
                ? doctor.name.trim()
                : getString(R.string.value_unavailable);

        isOpeningChat = true;
        Toast.makeText(requireContext(), getString(R.string.messages_opening_thread, doctorName), Toast.LENGTH_SHORT).show();
        repository.findOrCreateConversation(uid, repository.getCurrentUserDisplayName(), doctor.id, doctorName, "", new AppRepository.ConversationCallback() {
            @Override
            public void onConversationLoaded(@Nullable Conversation conversation) {
                isOpeningChat = false;
                if (!isAdded()) {
                    return;
                }
                if (conversation == null || conversation.id == null) {
                    Toast.makeText(requireContext(), R.string.auth_error_generic, Toast.LENGTH_SHORT).show();
                    return;
                }
                openChat(conversation.id, doctorName);
            }

            @Override
            public void onError(String message) {
                isOpeningChat = false;
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openChat(String conversationId, String doctorName) {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        fragmentManager.popBackStackImmediate("select_doctor", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        ChatFragment chatFragment = ChatFragment.newInstance(conversationId, doctorName);
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, chatFragment)
                .addToBackStack("patient_chat")
                .commit();
    }
}
