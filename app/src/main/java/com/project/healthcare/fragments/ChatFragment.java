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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

        messageAdapter = new MessageAdapter(uid != null ? uid : "", new MessageAdapter.MessageActionListener() {
            @Override
            public void onEdit(Message message) {
                showEditMessageDialog(message);
            }

            @Override
            public void onDeleteForMe(Message message) {
                deleteMessageForMe(message);
            }

            @Override
            public void onDeleteForEveryone(Message message) {
                confirmDeleteMessageForEveryone(message);
            }
        });
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
                if (items != null) {
                    for (Message message : items) {
                        if (uid != null && message.deletedFor != null && Boolean.TRUE.equals(message.deletedFor.get(uid))) {
                            continue;
                        }
                        activeMessages.add(message);
                    }
                }
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

    private void showEditMessageDialog(Message message) {
        if (message == null || message.id == null || uid == null) {
            return;
        }
        if (message.senderUid == null || !message.senderUid.equals(uid)) {
            Toast.makeText(requireContext(), R.string.message_delete_not_allowed, Toast.LENGTH_SHORT).show();
            return;
        }

        TextInputEditText input = new TextInputEditText(requireContext());
        input.setText(message.text != null ? message.text : "");
        input.setSelection(input.getText() != null ? input.getText().length() : 0);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.edit_message_title)
                .setView(input)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            android.widget.Button button = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String updatedText = input.getText() == null ? "" : input.getText().toString().trim();
                if (updatedText.isEmpty()) {
                    input.setError(getString(R.string.enter_message));
                    return;
                }
                repository.updateMessageText(conversationId, message, updatedText, (success, error) -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (!success) {
                        Toast.makeText(requireContext(), error != null ? error : getString(R.string.auth_error_generic), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog.dismiss();
                });
            });
        });
        dialog.show();
    }

    private void deleteMessageForMe(Message message) {
        if (message == null || message.id == null || conversationId == null || uid == null) {
            return;
        }
        repository.deleteMessageForUser(conversationId, message.id, uid, (success, error) -> {
            if (!isAdded()) {
                return;
            }
            if (!success) {
                Toast.makeText(requireContext(), error != null ? error : getString(R.string.auth_error_generic), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteMessageForEveryone(Message message) {
        if (message == null || message.id == null || conversationId == null || uid == null) {
            return;
        }
        if (message.senderUid == null || !message.senderUid.equals(uid)) {
            Toast.makeText(requireContext(), R.string.message_edit_not_allowed, Toast.LENGTH_SHORT).show();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_message_everyone_title)
                .setMessage(R.string.delete_message_everyone_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        repository.deleteMessageForEveryone(conversationId, message, uid, (success, error) -> {
                            if (!isAdded()) {
                                return;
                            }
                            if (!success) {
                                Toast.makeText(requireContext(),
                                        error != null ? error : getString(R.string.auth_error_generic),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }))
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (messagesListener != null && conversationId != null) {
            repository.removeConversationMessagesListener(conversationId, messagesListener);
        }
    }
}
