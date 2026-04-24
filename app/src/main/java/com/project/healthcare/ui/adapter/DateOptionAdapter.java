package com.project.healthcare.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.project.healthcare.R;
import com.project.healthcare.ui.model.DateOption;

import java.util.ArrayList;
import java.util.List;

public class DateOptionAdapter extends RecyclerView.Adapter<DateOptionAdapter.DateOptionViewHolder> {

    public interface OnDateSelectedListener {
        void onDateSelected(DateOption option);
    }

    private final List<DateOption> items = new ArrayList<>();
    private final OnDateSelectedListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public DateOptionAdapter(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    public void submitList(List<DateOption> newItems) {
        items.clear();
        items.addAll(newItems);
        selectedPosition = findFirstSelectablePosition();
        notifyDataSetChanged();
    }

    public DateOption getSelectedItem() {
        if (selectedPosition == RecyclerView.NO_POSITION || selectedPosition >= items.size()) {
            return null;
        }
        return items.get(selectedPosition);
    }

    @NonNull
    @Override
    public DateOptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_date_option, parent, false);
        return new DateOptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateOptionViewHolder holder, int position) {
        DateOption item = items.get(position);

        holder.dayLabel.setText(item.dayLabel);
        holder.dateLabel.setText(item.dateLabel);

        boolean isSelected = position == selectedPosition;
        if (item.disabled) {
            holder.root.setBackgroundResource(R.drawable.bg_date_chip_disabled);
            holder.dayLabel.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.neutral_300));
            holder.dateLabel.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.neutral_300));
            holder.root.setEnabled(false);
            holder.root.setOnClickListener(null);
            return;
        }

        holder.root.setEnabled(true);
        holder.root.setBackgroundResource(isSelected ? R.drawable.bg_date_chip_selected : R.drawable.bg_date_chip_default);
        int textColor = ContextCompat.getColor(holder.itemView.getContext(), isSelected ? R.color.base_white : R.color.neutral_400);
        holder.dayLabel.setTextColor(textColor);
        holder.dateLabel.setTextColor(textColor);

        holder.root.setOnClickListener(v -> {
            int previous = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();
            if (previous != RecyclerView.NO_POSITION) {
                notifyItemChanged(previous);
            }
            notifyItemChanged(selectedPosition);
            listener.onDateSelected(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private int findFirstSelectablePosition() {
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).disabled) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    static class DateOptionViewHolder extends RecyclerView.ViewHolder {

        final LinearLayout root;
        final TextView dayLabel;
        final TextView dateLabel;

        DateOptionViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.item_date_root);
            dayLabel = itemView.findViewById(R.id.text_day_label);
            dateLabel = itemView.findViewById(R.id.text_date_label);
        }
    }
}
