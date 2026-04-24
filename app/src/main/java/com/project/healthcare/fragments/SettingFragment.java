package com.project.healthcare.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.project.healthcare.R;
import com.project.healthcare.auth.AuthActivity;
import com.project.healthcare.ui.model.SettingOption;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
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
        LinearLayout optionsContainer = root.findViewById(R.id.container_setting_options);
        optionsContainer.removeAllViews();

        options.clear();
        options.add(new SettingOption(getString(R.string.settings_notifications), getString(R.string.settings_notifications_subtitle), android.R.drawable.ic_dialog_info));
        options.add(new SettingOption(getString(R.string.settings_privacy), getString(R.string.settings_privacy_subtitle), android.R.drawable.ic_menu_manage));
        options.add(new SettingOption(getString(R.string.settings_security), getString(R.string.settings_security_subtitle), android.R.drawable.ic_lock_lock));
        options.add(new SettingOption(getString(R.string.settings_help), getString(R.string.settings_help_subtitle), android.R.drawable.ic_menu_help));

        final LinearLayout[] currentExpandedPanel = new LinearLayout[1];

        for (int i = 0; i < options.size(); i++) {
            SettingOption option = options.get(i);
            View itemView = getLayoutInflater().inflate(R.layout.item_setting_option, optionsContainer, false);
            TextView title = itemView.findViewById(R.id.text_setting_title);
            TextView subtitle = itemView.findViewById(R.id.text_setting_subtitle);
            ImageView icon = itemView.findViewById(R.id.image_setting_icon);
            ImageView actionIcon = itemView.findViewById(R.id.image_setting_action);
            LinearLayout optionRow = itemView.findViewById(R.id.layout_setting_option_row);
            LinearLayout inlineSecurityPanel = itemView.findViewById(R.id.layout_setting_inline_security_panel);
            EditText currentPasswordInput = itemView.findViewById(R.id.input_setting_inline_current_password);
            EditText passwordInput = itemView.findViewById(R.id.input_setting_inline_new_password);
            EditText confirmPasswordInput = itemView.findViewById(R.id.input_setting_inline_confirm_password);
            Button submitButton = itemView.findViewById(R.id.button_setting_inline_change_password);

            title.setText(option.title);
            subtitle.setText(option.subtitle);
            icon.setImageResource(option.iconRes);
            actionIcon.setImageResource(android.R.drawable.arrow_down_float);

            if (i == SECURITY_OPTION_INDEX) {
                optionRow.setOnClickListener(v -> {
                    if (currentExpandedPanel[0] != null && currentExpandedPanel[0] != inlineSecurityPanel) {
                        currentExpandedPanel[0].setVisibility(View.GONE);
                    }
                    boolean willExpand = inlineSecurityPanel.getVisibility() != View.VISIBLE;
                    inlineSecurityPanel.setVisibility(willExpand ? View.VISIBLE : View.GONE);
                    actionIcon.setImageResource(willExpand ? android.R.drawable.ic_media_next : android.R.drawable.arrow_down_float);
                    currentExpandedPanel[0] = willExpand ? inlineSecurityPanel : null;
                });

                submitButton.setOnClickListener(v -> attemptPasswordChange(
                        currentPasswordInput,
                        passwordInput,
                        confirmPasswordInput,
                        submitButton
                ));
            } else {
                inlineSecurityPanel.setVisibility(View.GONE);
                actionIcon.setImageResource(android.R.drawable.arrow_down_float);
                optionRow.setOnClickListener(v -> {
                    if (currentExpandedPanel[0] != null) {
                        currentExpandedPanel[0].setVisibility(View.GONE);
                        currentExpandedPanel[0] = null;
                    }
                    Toast.makeText(requireContext(), option.title + " clicked", Toast.LENGTH_SHORT).show();
                });
            }

            optionsContainer.addView(itemView);
        }
    }

    private void attemptPasswordChange(EditText currentPasswordInput, EditText passwordInput, EditText confirmPasswordInput, Button submitButton) {
        if (currentPasswordInput == null || passwordInput == null || confirmPasswordInput == null || submitButton == null) {
            return;
        }

        String currentPassword = currentPasswordInput.getText() == null
                ? ""
                : currentPasswordInput.getText().toString().trim();
        String newPassword = passwordInput.getText() == null
                ? ""
                : passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText() == null
                ? ""
                : confirmPasswordInput.getText().toString().trim();

        if (TextUtils.isEmpty(currentPassword)) {
            currentPasswordInput.setError(getString(R.string.settings_current_password_required));
            currentPasswordInput.requestFocus();
            return;
        }

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

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInput.setError(getString(R.string.auth_error_confirm_password_required));
            confirmPasswordInput.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(requireContext(), R.string.settings_password_mismatch, Toast.LENGTH_SHORT).show();
            confirmPasswordInput.requestFocus();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.settings_password_update_failed, Toast.LENGTH_LONG).show();
            return;
        }

        setPasswordChangeLoading(true, currentPasswordInput, passwordInput, confirmPasswordInput, submitButton);
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail().trim(), currentPassword);
        user.reauthenticate(credential).addOnCompleteListener(reauthTask -> {
            if (!isAdded()) {
                return;
            }

            if (!reauthTask.isSuccessful()) {
                setPasswordChangeLoading(false, currentPasswordInput, passwordInput, confirmPasswordInput, submitButton);
                if (reauthTask.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(requireContext(), R.string.settings_wrong_current_password, Toast.LENGTH_LONG).show();
                } else {
                    String message = reauthTask.getException() != null && reauthTask.getException().getLocalizedMessage() != null
                            ? reauthTask.getException().getLocalizedMessage()
                            : getString(R.string.settings_password_reauth_required);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
                return;
            }

            user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                if (!isAdded()) {
                    return;
                }

                setPasswordChangeLoading(false, currentPasswordInput, passwordInput, confirmPasswordInput, submitButton);
                if (updateTask.isSuccessful()) {
                    currentPasswordInput.setText("");
                    passwordInput.setText("");
                    confirmPasswordInput.setText("");
                    Toast.makeText(requireContext(), R.string.settings_password_update_success, Toast.LENGTH_SHORT).show();
                    return;
                }

                String message = updateTask.getException() != null && updateTask.getException().getLocalizedMessage() != null
                        ? updateTask.getException().getLocalizedMessage()
                        : getString(R.string.settings_password_update_failed);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            });
        });
    }

    private void setPasswordChangeLoading(boolean loading, EditText currentPasswordInput, EditText passwordInput, EditText confirmPasswordInput, Button submitButton) {
        submitButton.setEnabled(!loading);
        currentPasswordInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
        confirmPasswordInput.setEnabled(!loading);
    }
}
