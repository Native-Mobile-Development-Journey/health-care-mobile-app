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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.ValueEventListener;
import com.project.healthcare.R;
import com.project.healthcare.data.AppRepository;
import com.project.healthcare.data.models.Conversation;
import com.project.healthcare.ui.adapter.ConversationAdapter;

import java.util.ArrayList;
import java.util.List;

public class MessageFragment extends Fragment {

    private final AppRepository repository = AppRepository.getInstance();
    private final List<Conversation> conversations = new ArrayList<>();

    private ConversationAdapter conversationAdapter;
    private ValueEventListener conversationsListener;
    private String uid;

    private TextView emptyText;
    private RecyclerView recyclerView;
    private FloatingActionButton newChatButton;

    public MessageFragment() {
        super(R.layout.fragment_message);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyText = view.findViewById(R.id.text_message_empty);
        recyclerView = view.findViewById(R.id.recycler_messages);
        newChatButton = view.findViewById(R.id.button_new_chat);

        uid = repository.getCurrentUserUid();

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        conversationAdapter = new ConversationAdapter(this::openConversation);
        recyclerView.setAdapter(conversationAdapter);

        newChatButton.setOnClickListener(v -> openDoctorSelection());

        observeConversations();
    }

    private void observeConversations() {
        if (uid == null) {
            emptyText.setVisibility(View.VISIBLE);
            return;
        }

        conversationsListener = repository.observeConversations(uid, new AppRepository.ListCallback<Conversation>() {
            @Override
            public void onData(List<Conversation> items) {
                conversations.clear();
                if (items != null) {
                    conversations.addAll(items);
                }
                conversationAdapter.submitList(conversations);
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

    private void openConversation(Conversation conversation) {
        if (conversation == null || conversation.id == null) {
            return;
        }
        String name = conversation.doctorName != null ? conversation.doctorName : getString(R.string.value_unavailable);
        ChatFragment chatFragment = ChatFragment.newInstance(conversation.id, name);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, chatFragment)
                .addToBackStack("patient_chat")
                .commit();
    }

    private void openDoctorSelection() {
        SelectDoctorFragment fragment = new SelectDoctorFragment();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack("select_doctor")
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (conversationsListener != null && uid != null) {
            repository.removeConversationsListener(uid, conversationsListener);
        }
    }
}
