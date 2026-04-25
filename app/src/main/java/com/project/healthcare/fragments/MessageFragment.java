package com.project.healthcare.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.project.healthcare.R;
import com.project.healthcare.data.AppRepository;
import com.project.healthcare.data.models.Conversation;
import com.project.healthcare.data.models.Doctor;
import com.project.healthcare.data.models.Message;
import com.project.healthcare.ui.adapter.ConversationAdapter;
import com.project.healthcare.ui.adapter.DoctorAdapter;
import com.project.healthcare.ui.adapter.MessageAdapter;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageFragment extends Fragment {

    private final AppRepository repository = AppRepository.getInstance();
    private final List<Conversation> allConversations = new ArrayList<>();
    private final List<Conversation> filteredConversations = new ArrayList<>();
    private final List<Doctor> allDoctors = new ArrayList<>();
    private final List<Doctor> filteredDoctors = new ArrayList<>();
    private final List<Message> activeMessages = new ArrayList<>();

    private ConversationAdapter conversationAdapter;
    private MessageAdapter messageAdapter;
    private ValueEventListener conversationsListener;
    private ValueEventListener messagesListener;
    private String uid;
    private Conversation activeConversation;
    private boolean isChatOpen;

    private TextInputEditText searchInput;
    private TextInputEditText chatInput;
    private TextView emptyText;
    private LinearLayout messageInputLayout;
    private RecyclerView recyclerView;

    public MessageFragment() {
        super(R.layout.fragment_message);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchInput = view.findViewById(R.id.input_message_search);
        chatInput = view.findViewById(R.id.input_chat_message);
        emptyText = view.findViewById(R.id.text_message_empty);
        messageInputLayout = view.findViewById(R.id.layout_message_input);
        recyclerView = view.findViewById(R.id.recycler_messages);

        uid = repository.getCurrentUserUid();
        messageAdapter = new MessageAdapter(uid != null ? uid : "");

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        conversationAdapter = new ConversationAdapter(this::openConversation);
        recyclerView.setAdapter(conversationAdapter);

        searchInput.setOnClickListener(v -> openDoctorSearchSheet());
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isChatOpen) {
                    applyFilters();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        ImageButton sendButton = view.findViewById(R.id.button_send_message);
        sendButton.setOnClickListener(v -> sendMessage());

        updateConversationView();
        observeConversations();
        fetchDoctorProfiles();
    }

    private void fetchDoctorProfiles() {
        repository.fetchAllDoctorProfiles(new AppRepository.ListCallback<Doctor>() {
            @Override
            public void onData(List<Doctor> items) {
                allDoctors.clear();
                allDoctors.addAll(items);
                filteredDoctors.clear();
                filteredDoctors.addAll(items);
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void observeConversations() {
        if (uid == null) {
            emptyText.setVisibility(View.VISIBLE);
            return;
        }

        conversationsListener = repository.observeConversations(uid, new AppRepository.ListCallback<Conversation>() {
            @Override
            public void onData(List<Conversation> items) {
                allConversations.clear();
                allConversations.addAll(items);
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
        String query = searchInput.getText() == null ? "" : searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);

        filteredConversations.clear();
        for (Conversation conversation : allConversations) {
            if (!query.isEmpty()
                    && !containsIgnoreCase(conversation.doctorName, query)
                    && !containsIgnoreCase(conversation.lastMessage, query)) {
                continue;
            }
            filteredConversations.add(conversation);
        }

        conversationAdapter.submitList(filteredConversations);
        emptyText.setVisibility(filteredConversations.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openDoctorSearchSheet() {
        View sheetView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_doctor_search, null);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        RecyclerView doctorList = sheetView.findViewById(R.id.recycler_doctor_search);
        TextInputEditText doctorSearchInput = sheetView.findViewById(R.id.input_doctor_search);

        doctorList.setLayoutManager(new LinearLayoutManager(requireContext()));
        DoctorAdapter doctorAdapter = new DoctorAdapter(doctor -> {
            dialog.dismiss();
            openDoctorChat(doctor);
        });
        doctorList.setAdapter(doctorAdapter);
        doctorAdapter.submitList(filteredDoctors);

        doctorSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                filteredDoctors.clear();
                for (Doctor doctor : allDoctors) {
                    if (doctor.name != null && doctor.name.toLowerCase(Locale.ROOT).contains(query)) {
                        filteredDoctors.add(doctor);
                    }
                }
                if (filteredDoctors.isEmpty()) {
                    filteredDoctors.addAll(allDoctors);
                }
                doctorAdapter.submitList(new ArrayList<>(filteredDoctors));
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        dialog.setContentView(sheetView);
        dialog.show();
    }

    private void openDoctorChat(Doctor doctor) {
        if (uid == null) {
            Toast.makeText(requireContext(), R.string.auth_error_generic, Toast.LENGTH_SHORT).show();
            return;
        }
        repository.findOrCreateConversation(uid, repository.getCurrentUserDisplayName(), doctor.id, doctor.name, "", new AppRepository.ConversationCallback() {
            @Override
            public void onConversationLoaded(@Nullable Conversation conversation) {
                if (conversation == null) {
                    return;
                }
                activeConversation = conversation;
                isChatOpen = true;
                updateConversationView();
                observeConversationMessages(conversation.id);
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
        activeConversation = conversation;
        isChatOpen = true;
        updateConversationView();
        observeConversationMessages(conversation.id);
    }

    private void observeConversationMessages(String conversationId) {
        if (messagesListener != null && activeConversation != null) {
            repository.removeConversationMessagesListener(activeConversation.id, messagesListener);
            messagesListener = null;
        }
        if (conversationId == null) {
            activeMessages.clear();
            messageAdapter.submitList(activeMessages);
            return;
        }

        messagesListener = repository.observeConversationMessages(conversationId, new AppRepository.ListCallback<Message>() {
            @Override
            public void onData(List<Message> items) {
                activeMessages.clear();
                activeMessages.addAll(items);
                messageAdapter.submitList(activeMessages);
                emptyText.setVisibility(activeMessages.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendMessage() {
        if (activeConversation == null || uid == null) {
            Toast.makeText(requireContext(), R.string.auth_error_generic, Toast.LENGTH_SHORT).show();
            return;
        }

        String text = chatInput.getText() == null ? "" : chatInput.getText().toString().trim();
        if (text.isEmpty()) {
            chatInput.setError(getString(R.string.enter_message));
            return;
        }

        Message message = new Message(
                null,
                uid,
                repository.getCurrentUserDisplayName(),
                text,
                System.currentTimeMillis(),
                false
        );
        repository.sendMessage(activeConversation.id, message, (success, error) -> {
            if (!isAdded()) {
                return;
            }
            if (success) {
                chatInput.setText("");
                return;
            }
            Toast.makeText(requireContext(), error != null ? error : getString(R.string.auth_error_generic), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateConversationView() {
        if (isChatOpen) {
            recyclerView.setAdapter(messageAdapter);
            messageInputLayout.setVisibility(View.VISIBLE);
            emptyText.setVisibility(activeMessages.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            recyclerView.setAdapter(conversationAdapter);
            messageInputLayout.setVisibility(View.GONE);
            applyFilters();
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
        if (conversationsListener != null && uid != null) {
            repository.removeConversationsListener(uid, conversationsListener);
        }
        if (messagesListener != null && activeConversation != null) {
            repository.removeConversationMessagesListener(activeConversation.id, messagesListener);
        }
    }
}
