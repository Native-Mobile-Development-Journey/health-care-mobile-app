package com.project.healthcare.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.project.healthcare.R;
import com.project.healthcare.data.AppRepository;
import com.project.healthcare.data.models.Message;
import com.project.healthcare.ui.adapter.MessageAdapter;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private static final String ARG_CONVERSATION_ID = "conversation_id";
    private static final String ARG_CONVERSATION_NAME = "conversation_name";

    private final AppRepository repository = AppRepository.getInstance();
    private final List<Message> activeMessages = new ArrayList<>();

    private MessageAdapter messageAdapter;
    private ValueEventListener messagesListener;
    private String uid;
    private String conversationId;
    private String conversationName;

    private TextInputEditText chatInput;
    private TextView emptyText;

    public ChatFragment() {
        super(R.layout.fragment_chat);
    }

    public static ChatFragment newInstance(String conversationId, String conversationName) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONVERSATION_ID, conversationId);
        args.putString(ARG_CONVERSATION_NAME, conversationName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            conversationId = getArguments().getString(ARG_CONVERSATION_ID);
            conversationName = getArguments().getString(ARG_CONVERSATION_NAME);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        uid = repository.getCurrentUserUid();
        chatInput = view.findViewById(R.id.input_chat_message);
        emptyText = view.findViewById(R.id.text_chat_empty);
        TextView titleText = view.findViewById(R.id.text_chat_title);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_chat_messages);
        ImageButton sendButton = view.findViewById(R.id.button_chat_send);

        titleText.setText(TextUtils.isEmpty(conversationName) ? getString(R.string.nav_message) : conversationName);

        messageAdapter = new MessageAdapter(uid != null ? uid : "");
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(messageAdapter);

        sendButton.setOnClickListener(v -> sendMessage());

        if (conversationId == null) {
            emptyText.setText(R.string.messages_empty);
            emptyText.setVisibility(View.VISIBLE);
            return;
        }

        observeConversationMessages(conversationId);
    }

    private void observeConversationMessages(String conversationId) {
        if (messagesListener != null) {
            repository.removeConversationMessagesListener(conversationId, messagesListener);
            messagesListener = null;
        }
        messagesListener = repository.observeConversationMessages(conversationId, new AppRepository.ListCallback<Message>() {
            @Override
            public void onData(List<Message> items) {
                activeMessages.clear();
                activeMessages.addAll(items);
                messageAdapter.submitList(activeMessages);
                emptyText.setVisibility(activeMessages.isEmpty() ? View.VISIBLE : View.GONE);
                if (uid != null) {
                    repository.markMessagesRead(conversationId, uid, activeMessages);
                    repository.markConversationRead(conversationId, uid, null);
                }
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
        if (conversationId == null || uid == null) {
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
        repository.sendMessage(conversationId, message, (success, error) -> {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (messagesListener != null && conversationId != null) {
            repository.removeConversationMessagesListener(conversationId, messagesListener);
        }
    }
}
