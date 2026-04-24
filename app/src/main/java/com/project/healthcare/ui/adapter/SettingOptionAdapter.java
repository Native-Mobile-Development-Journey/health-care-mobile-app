package com.project.healthcare.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.healthcare.R;
import com.project.healthcare.ui.model.SettingOption;

import java.util.ArrayList;
import java.util.List;

public class SettingOptionAdapter extends RecyclerView.Adapter<SettingOptionAdapter.SettingOptionViewHolder> {

    public interface OnSettingOptionClickListener {
        void onSettingOptionClick(SettingOption option);
    }

    private final List<SettingOption> items = new ArrayList<>();
    private final OnSettingOptionClickListener listener;

    public SettingOptionAdapter(OnSettingOptionClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<SettingOption> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SettingOptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_setting_option, parent, false);
        return new SettingOptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingOptionViewHolder holder, int position) {
        SettingOption item = items.get(position);
        holder.title.setText(item.title);
        holder.subtitle.setText(item.subtitle);
        holder.icon.setImageResource(item.iconRes);
        holder.root.setOnClickListener(v -> listener.onSettingOptionClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SettingOptionViewHolder extends RecyclerView.ViewHolder {

        final View root;
        final ImageView icon;
        final TextView title;
        final TextView subtitle;

        SettingOptionViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.item_setting_option_root);
            icon = itemView.findViewById(R.id.image_setting_icon);
            title = itemView.findViewById(R.id.text_setting_title);
            subtitle = itemView.findViewById(R.id.text_setting_subtitle);
        }
    }
}
