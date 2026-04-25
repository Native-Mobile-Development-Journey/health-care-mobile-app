package com.project.healthcare.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.project.healthcare.R;
import com.project.healthcare.data.models.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<Message> items = new ArrayList<>();
    private final String currentUserId;

    public MessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void submitList(List<Message> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = items.get(position);
        holder.messageText.setText(message.text);
        holder.timeText.setText(message.timestamp > 0 ? String.valueOf(message.timestamp) : "");

        boolean isOutgoing = message.senderUid != null && message.senderUid.equals(currentUserId);
        holder.root.setBackgroundResource(isOutgoing ? R.drawable.bg_message_outgoing : R.drawable.bg_message_incoming);
        int textColor = ContextCompat.getColor(holder.itemView.getContext(), isOutgoing ? R.color.base_white : R.color.base_black);
        holder.messageText.setTextColor(textColor);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {

        final View root;
        final TextView messageText;
        final TextView timeText;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.item_message_root);
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
        }
    }
}
