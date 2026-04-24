package com.project.healthcare.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.project.healthcare.R;
import com.project.healthcare.ui.model.SettingOption;

import java.util.ArrayList;
import java.util.List;

public class SettingOptionAdapter extends BaseAdapter {

    public interface OnSettingActionListener {
        void onRegularOptionClick(SettingOption option);

        void onSecurityPasswordSubmit(EditText currentPasswordInput, EditText passwordInput, EditText confirmPasswordInput, Button submitButton);
    }

    private final List<SettingOption> items = new ArrayList<>();
    private final LayoutInflater inflater;
    private final int securityPosition;
    private final OnSettingActionListener listener;

    private int expandedPosition = -1;

    public SettingOptionAdapter(Context context, int securityPosition, OnSettingActionListener listener) {
        this.inflater = LayoutInflater.from(context);
        this.securityPosition = securityPosition;
        this.listener = listener;
    }

    public void submitList(List<SettingOption> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public SettingOption getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        SettingOptionViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_setting_option, parent, false);
            holder = new SettingOptionViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (SettingOptionViewHolder) convertView.getTag();
        }

        SettingOption item = getItem(position);
        holder.title.setText(item.title);
        holder.subtitle.setText(item.subtitle);
        holder.icon.setImageResource(item.iconRes);

        boolean isSecurityItem = position == securityPosition;
        boolean isExpanded = isSecurityItem && expandedPosition == position;
        holder.inlineSecurityPanel.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        holder.optionRow.setOnClickListener(v -> {
            if (isSecurityItem) {
                expandedPosition = expandedPosition == position ? -1 : position;
                notifyDataSetChanged();
                return;
            }

            if (expandedPosition != -1) {
                expandedPosition = -1;
                notifyDataSetChanged();
            }

            listener.onRegularOptionClick(item);
        });

        holder.securitySubmitButton.setOnClickListener(v -> listener.onSecurityPasswordSubmit(
            holder.securityCurrentPasswordInput,
            holder.securityPasswordInput,
            holder.securityConfirmPasswordInput,
            holder.securitySubmitButton
        ));
        return convertView;
    }

    static class SettingOptionViewHolder {

        final View optionRow;
        final ImageView icon;
        final TextView title;
        final TextView subtitle;
        final LinearLayout inlineSecurityPanel;
        final EditText securityCurrentPasswordInput;
        final EditText securityPasswordInput;
        final EditText securityConfirmPasswordInput;
        final Button securitySubmitButton;

        SettingOptionViewHolder(View itemView) {
            optionRow = itemView.findViewById(R.id.layout_setting_option_row);
            icon = itemView.findViewById(R.id.image_setting_icon);
            title = itemView.findViewById(R.id.text_setting_title);
            subtitle = itemView.findViewById(R.id.text_setting_subtitle);
            inlineSecurityPanel = itemView.findViewById(R.id.layout_setting_inline_security_panel);
            securityCurrentPasswordInput = itemView.findViewById(R.id.input_setting_inline_current_password);
            securityPasswordInput = itemView.findViewById(R.id.input_setting_inline_new_password);
            securityConfirmPasswordInput = itemView.findViewById(R.id.input_setting_inline_confirm_password);
            securitySubmitButton = itemView.findViewById(R.id.button_setting_inline_change_password);
        }
    }
}
