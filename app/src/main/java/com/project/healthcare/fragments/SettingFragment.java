package com.project.healthcare.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.project.healthcare.R;
import com.project.healthcare.auth.AuthActivity;
import com.project.healthcare.ui.adapter.SettingOptionAdapter;
import com.project.healthcare.ui.model.SettingOption;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;


public class SettingFragment extends Fragment {

    private TextView userNameText;
    private TextView userEmailText;

    public SettingFragment() {
        super(R.layout.fragment_setting);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userNameText = view.findViewById(R.id.text_setting_user_name);
        userEmailText = view.findViewById(R.id.text_setting_user_email);

        bindProfile();
        setupSettingOptions(view);

        View signOutButton = view.findViewById(R.id.button_sign_out);
        if (signOutButton != null) {
            signOutButton.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(requireContext(), AuthActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
        }
    }

    private void bindProfile() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            userNameText.setText(R.string.user_name);
            userEmailText.setText(R.string.settings_guest_email);
            return;
        }

        if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            userNameText.setText(user.getDisplayName().trim());
        } else {
            userNameText.setText(R.string.settings_patient);
        }

        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            userEmailText.setText(user.getEmail().trim());
        } else {
            userEmailText.setText(R.string.settings_guest_email);
        }
    }

    private void setupSettingOptions(View root) {
        RecyclerView recyclerView = root.findViewById(R.id.recycler_setting_options);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        SettingOptionAdapter adapter = new SettingOptionAdapter(option ->
                Toast.makeText(requireContext(), option.title + " clicked", Toast.LENGTH_SHORT).show());
        recyclerView.setAdapter(adapter);

        List<SettingOption> options = new ArrayList<>();
        options.add(new SettingOption(getString(R.string.settings_notifications), getString(R.string.settings_notifications_subtitle), android.R.drawable.ic_dialog_info));
        options.add(new SettingOption(getString(R.string.settings_privacy), getString(R.string.settings_privacy_subtitle), android.R.drawable.ic_menu_manage));
        options.add(new SettingOption(getString(R.string.settings_security), getString(R.string.settings_security_subtitle), android.R.drawable.ic_lock_lock));
        options.add(new SettingOption(getString(R.string.settings_help), getString(R.string.settings_help_subtitle), android.R.drawable.ic_menu_help));
        adapter.submitList(options);
    }
}
