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
    private boolean isConversationReady;

    private TextInputEditText searchInput;
    private TextInputEditText chatInput;
    private TextInputLayout searchLayout;
    private TextView chatHeader;
    private TextView emptyText;
    private LinearLayout messageInputLayout;
    private ImageButton sendButton;
    private RecyclerView recyclerView;

    public MessageFragment() {
        super(R.layout.fragment_message);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchInput = view.findViewById(R.id.input_message_search);
        searchLayout = view.findViewById(R.id.layout_message_search);
        chatHeader = view.findViewById(R.id.text_chat_header);
        chatInput = view.findViewById(R.id.input_chat_message);
        emptyText = view.findViewById(R.id.text_message_empty);
        messageInputLayout = view.findViewById(R.id.layout_message_input);
        sendButton = view.findViewById(R.id.button_send_message);
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

        sendButton.setOnClickListener(v -> sendMessage());

        updateConversationView();
        observeConversations();
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
                fetchDoctorProfilesFromDoctorsCollection();
            }
        });
    }

    private void fetchDoctorProfilesFromDoctorsCollection() {
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
            if (doctor == null || doctor.id == null || doctor.id.trim().isEmpty()) {
                Toast.makeText(requireContext(), R.string.auth_error_generic, Toast.LENGTH_SHORT).show();
                return;
            }
            openDoctorChat(doctor, dialog);
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
        openDoctorChat(doctor, null);
    }

    private void openDoctorChat(Doctor doctor, @Nullable BottomSheetDialog dialog) {
        if (uid == null) {
            Toast.makeText(requireContext(), R.string.auth_error_generic, Toast.LENGTH_SHORT).show();
            if (dialog != null) {
                dialog.dismiss();
            }
            return;
        }
        if (doctor == null || doctor.id == null || doctor.id.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.auth_error_generic, Toast.LENGTH_SHORT).show();
            if (dialog != null) {
                dialog.dismiss();
            }
            return;
        }
        String doctorName = doctor.name != null && !doctor.name.trim().isEmpty() ? doctor.name.trim() : getString(R.string.value_unavailable);
        String temporaryConversationId = "pending_" + System.currentTimeMillis();
        activeConversation = new Conversation(
                temporaryConversationId,
                uid,
                repository.getCurrentUserDisplayName(),
                doctor.id,
                doctorName,
                "",
                getString(R.string.messages_opening_thread, doctorName),
                0,
                null
        );
        isChatOpen = true;
        isConversationReady = false;
        updateConversationView();
        observeConversationMessages(activeConversation.id);

        repository.findOrCreateConversation(uid, repository.getCurrentUserDisplayName(), doctor.id, doctorName, "", new AppRepository.ConversationCallback() {
            @Override
            public void onConversationLoaded(@Nullable Conversation conversation) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                if (!isAdded()) {
                    return;
                }
                if (conversation == null) {
                    Toast.makeText(requireContext(), R.string.auth_error_generic, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (messagesListener != null && activeConversation != null) {
                    repository.removeConversationMessagesListener(activeConversation.id, messagesListener);
                    messagesListener = null;
                }
                activeConversation = conversation;
                isConversationReady = true;
                updateConversationView();
                observeConversationMessages(conversation.id);
                Toast.makeText(requireContext(), getString(R.string.messages_opening_thread, doctorName), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                if (isAdded()) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
                isChatOpen = false;
                isConversationReady = false;
                activeConversation = null;
                updateConversationView();
            }
        });
    }

    private void openConversation(Conversation conversation) {
        if (messagesListener != null && activeConversation != null) {
            repository.removeConversationMessagesListener(activeConversation.id, messagesListener);
            messagesListener = null;
        }
        activeConversation = conversation;
        isChatOpen = true;
        isConversationReady = true;
        updateConversationView();
        observeConversationMessages(conversation.id);
    }

    private void observeConversationMessages(String conversationId) {
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
            sendButton.setEnabled(isConversationReady);
            chatInput.setEnabled(isConversationReady);
            if (activeConversation != null) {
                String threadTitle = getString(R.string.messages_opening_thread, activeConversation.doctorName != null ? activeConversation.doctorName : getString(R.string.nav_message));
                chatHeader.setText(threadTitle);
            }
            chatHeader.setVisibility(View.VISIBLE);
            searchLayout.setVisibility(View.GONE);
            emptyText.setVisibility(activeMessages.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            recyclerView.setAdapter(conversationAdapter);
            messageInputLayout.setVisibility(View.GONE);
            chatHeader.setVisibility(View.GONE);
            searchLayout.setVisibility(View.VISIBLE);
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
