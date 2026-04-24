package com.project.healthcare.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.project.healthcare.R;
import com.project.healthcare.ui.model.ServiceItem;

import java.util.ArrayList;
import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    public interface OnServiceClickListener {
        void onServiceClick(ServiceItem item);
    }

    private final List<ServiceItem> items = new ArrayList<>();
    private final OnServiceClickListener listener;
    private int selectedPosition;

    public ServiceAdapter(int selectedPosition, OnServiceClickListener listener) {
        this.selectedPosition = selectedPosition;
        this.listener = listener;
    }

    public void submitList(List<ServiceItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_service, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        ServiceItem item = items.get(position);
        holder.label.setText(item.label);
        holder.icon.setImageResource(item.iconRes);

        boolean isSelected = position == selectedPosition;
        holder.iconContainer.setBackgroundResource(isSelected ? R.drawable.bg_service_tile_selected : R.drawable.bg_service_tile);
        holder.icon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), isSelected ? R.color.base_white : R.color.neutral_400));
        holder.label.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), isSelected ? R.color.primary_500 : R.color.base_black));

        holder.root.setOnClickListener(v -> {
            int previous = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();
            if (previous != RecyclerView.NO_POSITION) {
                notifyItemChanged(previous);
            }
            notifyItemChanged(selectedPosition);
            listener.onServiceClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ServiceViewHolder extends RecyclerView.ViewHolder {

        final View root;
        final FrameLayout iconContainer;
        final ImageView icon;
        final TextView label;

        ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.item_service_root);
            iconContainer = itemView.findViewById(R.id.service_icon_container);
            icon = itemView.findViewById(R.id.service_icon);
            label = itemView.findViewById(R.id.service_label);
        }
    }
}
