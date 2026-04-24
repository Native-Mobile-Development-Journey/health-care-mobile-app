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

import com.project.healthcare.R;
import com.project.healthcare.data.AppRepository;
import com.project.healthcare.data.models.Conversation;
import com.project.healthcare.ui.adapter.ConversationAdapter;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MessageFragment extends Fragment {

    private final AppRepository repository = AppRepository.getInstance();
    private final List<Conversation> allConversations = new ArrayList<>();
    private final List<Conversation> filteredConversations = new ArrayList<>();

    private ConversationAdapter adapter;
    private ValueEventListener conversationsListener;
    private String uid;

    private TextInputEditText searchInput;
    private TextView emptyText;

    public MessageFragment() {
        super(R.layout.fragment_message);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchInput = view.findViewById(R.id.input_message_search);
        emptyText = view.findViewById(R.id.text_message_empty);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ConversationAdapter(conversation ->
                Toast.makeText(requireContext(), getString(R.string.messages_opening_thread, conversation.doctorName), Toast.LENGTH_SHORT).show());
        recyclerView.setAdapter(adapter);

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

        uid = repository.getCurrentUserUid();
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

        adapter.submitList(filteredConversations);
        emptyText.setVisibility(filteredConversations.isEmpty() ? View.VISIBLE : View.GONE);
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
    }
}
