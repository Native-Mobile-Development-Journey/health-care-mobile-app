package com.project.healthcare.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.project.healthcare.R;
import com.project.healthcare.ui.model.TimeOption;

import java.util.ArrayList;
import java.util.List;

public class TimeOptionAdapter extends RecyclerView.Adapter<TimeOptionAdapter.TimeOptionViewHolder> {

    public interface OnTimeSelectedListener {
        void onTimeSelected(TimeOption option);
    }

    private final List<TimeOption> items = new ArrayList<>();
    private final OnTimeSelectedListener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public TimeOptionAdapter(OnTimeSelectedListener listener) {
        this.listener = listener;
    }

    public void submitList(List<TimeOption> newItems) {
        items.clear();
        items.addAll(newItems);
        selectedPosition = items.isEmpty() ? RecyclerView.NO_POSITION : 0;
        notifyDataSetChanged();
    }

    public TimeOption getSelectedItem() {
        if (selectedPosition == RecyclerView.NO_POSITION || selectedPosition >= items.size()) {
            return null;
        }
        return items.get(selectedPosition);
    }

    @NonNull
    @Override
    public TimeOptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_option, parent, false);
        return new TimeOptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeOptionViewHolder holder, int position) {
        TimeOption item = items.get(position);
        holder.text.setText(item.label);

        boolean isSelected = position == selectedPosition;
        holder.text.setBackgroundResource(isSelected ? R.drawable.bg_time_chip_selected : R.drawable.bg_time_chip_default);
        holder.text.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), isSelected ? R.color.primary_400 : R.color.neutral_400));

        holder.text.setOnClickListener(v -> {
            int previous = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();
            if (previous != RecyclerView.NO_POSITION) {
                notifyItemChanged(previous);
            }
            notifyItemChanged(selectedPosition);
            listener.onTimeSelected(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TimeOptionViewHolder extends RecyclerView.ViewHolder {

        final TextView text;

        TimeOptionViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.item_time_text);
        }
    }
}
