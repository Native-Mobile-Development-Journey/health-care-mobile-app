package com.project.healthcare.ui.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.PopupMenu;

import com.project.healthcare.R;
import com.project.healthcare.data.models.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    public interface MessageActionListener {
        void onEdit(Message message);

        void onDelete(Message message);
    }

    private final List<Message> items = new ArrayList<>();
    private final String currentUserId;
    private final MessageActionListener actionListener;

    public MessageAdapter(String currentUserId, MessageActionListener actionListener) {
        this.currentUserId = currentUserId;
        this.actionListener = actionListener;
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
        holder.messageText.setText(message.text != null ? message.text : "");

        if (message.timestamp > 0) {
            String formatted = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(message.timestamp));
            holder.timeText.setText(formatted);
        } else {
            holder.timeText.setText("");
        }

        boolean isOutgoing = message.senderUid != null && message.senderUid.equals(currentUserId);
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) holder.bubble.getLayoutParams();
        layoutParams.gravity = isOutgoing ? Gravity.END : Gravity.START;
        holder.bubble.setLayoutParams(layoutParams);

        holder.bubble.setBackgroundResource(isOutgoing ? R.drawable.bg_message_outgoing : R.drawable.bg_message_incoming);
        int textColor = ContextCompat.getColor(holder.itemView.getContext(), isOutgoing ? R.color.base_white : R.color.base_black);
        holder.messageText.setTextColor(textColor);
        holder.timeText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), isOutgoing ? R.color.base_white : R.color.neutral_400));

        holder.root.setOnLongClickListener(v -> {
            showMessageMenu(v, message, isOutgoing);
            return true;
        });
    }

    private void showMessageMenu(View anchor, Message message, boolean isOutgoing) {
        PopupMenu menu = new PopupMenu(anchor.getContext(), anchor);
        menu.getMenuInflater().inflate(R.menu.menu_message_actions, menu.getMenu());
        menu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit_message) {
                if (!isOutgoing) {
                    Toast.makeText(anchor.getContext(), R.string.message_edit_not_allowed, Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (actionListener != null) {
                    actionListener.onEdit(message);
                }
                return true;
            }
            if (itemId == R.id.action_delete_message) {
                if (actionListener != null) {
                    actionListener.onDelete(message);
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

    static class MessageViewHolder extends RecyclerView.ViewHolder {

        final View root;
        final View bubble;
        final TextView messageText;
        final TextView timeText;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.item_message_root);
            bubble = itemView.findViewById(R.id.item_message_bubble);
            messageText = itemView.findViewById(R.id.text_message_body);
            timeText = itemView.findViewById(R.id.text_message_time);
        }
    }
}
