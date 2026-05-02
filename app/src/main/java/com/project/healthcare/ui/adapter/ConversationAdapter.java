package com.project.healthcare.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.PopupMenu;

import com.project.healthcare.R;
import com.project.healthcare.data.models.Conversation;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public interface ConversationNameProvider {
        String getName(Conversation conversation);
    }

    public interface ConversationActionListener {
        void onDelete(Conversation conversation);
    }

    private final List<Conversation> items = new ArrayList<>();
    private final OnConversationClickListener listener;
    private final ConversationNameProvider nameProvider;
    private final ConversationActionListener actionListener;

    public ConversationAdapter(OnConversationClickListener listener) {
        this(listener, conversation -> conversation.doctorName, null);
    }

    public ConversationAdapter(OnConversationClickListener listener, ConversationNameProvider nameProvider) {
        this(listener, nameProvider, null);
    }

    public ConversationAdapter(OnConversationClickListener listener,
                               ConversationNameProvider nameProvider,
                               ConversationActionListener actionListener) {
        this.listener = listener;
        this.nameProvider = nameProvider;
        this.actionListener = actionListener;
    }

    public void submitList(List<Conversation> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation item = items.get(position);
        String name = nameProvider != null ? nameProvider.getName(item) : item.doctorName;
        holder.name.setText(name != null ? name : "");
        if (TextUtils.isEmpty(item.lastMessage)) {
            holder.message.setText(R.string.conversation_message_default);
        } else {
            holder.message.setText(item.lastMessage);
        }
        holder.time.setText(item.timeLabel != null ? item.timeLabel : "");

        if (item.unreadCount > 0) {
            holder.unread.setVisibility(View.VISIBLE);
            holder.unread.setText(String.valueOf(item.unreadCount));
        } else {
            holder.unread.setVisibility(View.GONE);
        }

        holder.root.setOnClickListener(v -> listener.onConversationClick(item));
        if (actionListener != null) {
            holder.root.setOnLongClickListener(v -> {
                showConversationMenu(v, item);
                return true;
            });
        } else {
            holder.root.setOnLongClickListener(null);
        }
    }

    private void showConversationMenu(View anchor, Conversation conversation) {
        PopupMenu menu = new PopupMenu(anchor.getContext(), anchor);
        menu.getMenuInflater().inflate(R.menu.menu_conversation_actions, menu.getMenu());
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_delete_conversation) {
                if (actionListener != null) {
                    actionListener.onDelete(conversation);
                }
                return true;
            }
            return false;
        });
        menu.show();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {

        final View root;
        final TextView name;
        final TextView message;
        final TextView time;
        final TextView unread;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.item_conversation_root);
            name = itemView.findViewById(R.id.text_conversation_name);
            message = itemView.findViewById(R.id.text_conversation_message);
            time = itemView.findViewById(R.id.text_conversation_time);
            unread = itemView.findViewById(R.id.text_conversation_unread);
        }
    }
}
