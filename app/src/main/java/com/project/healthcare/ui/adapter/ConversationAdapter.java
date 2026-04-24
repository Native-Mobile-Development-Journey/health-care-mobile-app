package com.project.healthcare.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.healthcare.R;
import com.project.healthcare.data.models.Conversation;

import java.util.ArrayList;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    private final List<Conversation> items = new ArrayList<>();
    private final OnConversationClickListener listener;

    public ConversationAdapter(OnConversationClickListener listener) {
        this.listener = listener;
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
        holder.name.setText(item.doctorName);
        holder.message.setText(item.lastMessage);
        holder.time.setText(item.timeLabel);

        if (item.unreadCount > 0) {
            holder.unread.setVisibility(View.VISIBLE);
            holder.unread.setText(String.valueOf(item.unreadCount));
        } else {
            holder.unread.setVisibility(View.GONE);
        }

        holder.root.setOnClickListener(v -> listener.onConversationClick(item));
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
