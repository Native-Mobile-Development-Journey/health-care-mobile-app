package com.project.healthcare.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.project.healthcare.R;
import com.project.healthcare.auth.AuthActivity;
import com.project.healthcare.ui.adapter.SettingOptionAdapter;
import com.project.healthcare.ui.model.SettingOption;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;


public class SettingFragment extends Fragment {

    private static final int SECURITY_OPTION_INDEX = 2;

    private TextView userNameText;
    private TextView userEmailText;
    private final List<SettingOption> options = new ArrayList<>();

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
        ListView listView = root.findViewById(R.id.list_setting_options);
        SettingOptionAdapter adapter = new SettingOptionAdapter(
                requireContext(),
                SECURITY_OPTION_INDEX,
                new SettingOptionAdapter.OnSettingActionListener() {
                    @Override
                    public void onRegularOptionClick(SettingOption option) {
                        Toast.makeText(requireContext(), option.title + " clicked", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSecurityPasswordSubmit(EditText passwordInput, Button submitButton) {
                        attemptPasswordChange(passwordInput, submitButton);
                    }
                }
        );
        listView.setAdapter(adapter);

        options.clear();
        options.add(new SettingOption(getString(R.string.settings_notifications), getString(R.string.settings_notifications_subtitle), android.R.drawable.ic_dialog_info));
        options.add(new SettingOption(getString(R.string.settings_privacy), getString(R.string.settings_privacy_subtitle), android.R.drawable.ic_menu_manage));
        options.add(new SettingOption(getString(R.string.settings_security), getString(R.string.settings_security_subtitle), android.R.drawable.ic_lock_lock));
        options.add(new SettingOption(getString(R.string.settings_help), getString(R.string.settings_help_subtitle), android.R.drawable.ic_menu_help));

        adapter.submitList(options);
    }

    private void attemptPasswordChange(EditText passwordInput, Button submitButton) {
        if (passwordInput == null || submitButton == null) {
            return;
        }

        String newPassword = passwordInput.getText() == null
                ? ""
                : passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(newPassword)) {
            passwordInput.setError(getString(R.string.auth_error_password_required));
            passwordInput.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            passwordInput.setError(getString(R.string.auth_error_password_min));
            passwordInput.requestFocus();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), R.string.settings_password_update_failed, Toast.LENGTH_LONG).show();
            return;
        }

        setPasswordChangeLoading(true, passwordInput, submitButton);
        user.updatePassword(newPassword).addOnCompleteListener(task -> {
            if (!isAdded()) {
                return;
            }

            setPasswordChangeLoading(false, passwordInput, submitButton);
            if (task.isSuccessful()) {
                passwordInput.setText("");
                Toast.makeText(requireContext(), R.string.settings_password_update_success, Toast.LENGTH_SHORT).show();
                return;
            }

            if (task.getException() instanceof FirebaseAuthRecentLoginRequiredException) {
                Toast.makeText(requireContext(), R.string.settings_password_reauth_required, Toast.LENGTH_LONG).show();
                return;
            }

            String message = task.getException() != null && task.getException().getLocalizedMessage() != null
                    ? task.getException().getLocalizedMessage()
                    : getString(R.string.settings_password_update_failed);
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        });
    }

    private void setPasswordChangeLoading(boolean loading, EditText passwordInput, Button submitButton) {
        submitButton.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
    }
}
