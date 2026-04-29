package com.project.healthcare.fragments;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.healthcare.R;
import com.project.healthcare.data.AppRepository;
import com.project.healthcare.data.models.Conversation;
import com.project.healthcare.fragments.ChatFragment;
import com.project.healthcare.ui.adapter.ConversationAdapter;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DoctorMessagesFragment extends Fragment {

    private final AppRepository repository = AppRepository.getInstance();
    private final List<Conversation> conversations = new ArrayList<>();

    private ConversationAdapter adapter;
    private ValueEventListener conversationsListener;
    private TextView emptyText;

    public DoctorMessagesFragment() {
        super(R.layout.fragment_doctor_messages);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyText = view.findViewById(R.id.text_doctor_messages_empty);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_doctor_conversations);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ConversationAdapter(conversation -> {
            if (conversation.id == null) {
                return;
            }
            String participantName = conversation.patientName != null ? conversation.patientName : getString(R.string.value_unavailable);
            ChatFragment chatFragment = ChatFragment.newInstance(conversation.id, participantName);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.doctor_dashboard_container, chatFragment)
                    .addToBackStack("doctor_chat")
                    .commit();
        }, conversation -> conversation.patientName);
        recyclerView.setAdapter(adapter);

        String doctorUid = repository.getCurrentUserUid();
        if (doctorUid == null) {
            emptyText.setVisibility(View.VISIBLE);
            return;
        }

        conversationsListener = repository.observeDoctorConversations(doctorUid, new AppRepository.ListCallback<Conversation>() {
            @Override
            public void onData(List<Conversation> items) {
                conversations.clear();
                conversations.addAll(items);
                adapter.submitList(conversations);
                emptyText.setVisibility(conversations.isEmpty() ? View.VISIBLE : View.GONE);
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
    public void onDestroyView() {
        super.onDestroyView();
        String doctorUid = repository.getCurrentUserUid();
        if (conversationsListener != null && doctorUid != null) {
            repository.removeDoctorConversationsListener(doctorUid, conversationsListener);
        }
    }
}
