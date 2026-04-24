package com.project.healthcare.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.healthcare.R;
import com.project.healthcare.data.models.DoctorAvailabilitySlot;

import java.util.ArrayList;
import java.util.List;

public class AvailabilitySlotAdapter extends RecyclerView.Adapter<AvailabilitySlotAdapter.SlotViewHolder> {

    public interface OnAvailabilitySlotActionListener {
        void onRemoveSlot(DoctorAvailabilitySlot slot);
    }

    private final List<DoctorAvailabilitySlot> items = new ArrayList<>();
    private final OnAvailabilitySlotActionListener listener;

    public AvailabilitySlotAdapter(OnAvailabilitySlotActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<DoctorAvailabilitySlot> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_availability_slot, parent, false);
        return new SlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        DoctorAvailabilitySlot slot = items.get(position);
        holder.day.setText(slot.dayLabel != null ? slot.dayLabel : slot.dayOfWeek);
        holder.time.setText(slot.startTime + " - " + slot.endTime);
        holder.status.setText(slot.isActive ? "Active" : "Inactive");
        holder.removeButton.setOnClickListener(v -> listener.onRemoveSlot(slot));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SlotViewHolder extends RecyclerView.ViewHolder {

        final TextView day;
        final TextView time;
        final TextView status;
        final ImageButton removeButton;

        SlotViewHolder(@NonNull View itemView) {
            super(itemView);
            day = itemView.findViewById(R.id.text_slot_day);
            time = itemView.findViewById(R.id.text_slot_time);
            status = itemView.findViewById(R.id.text_slot_status);
            removeButton = itemView.findViewById(R.id.button_remove_slot);
        }
    }
}
