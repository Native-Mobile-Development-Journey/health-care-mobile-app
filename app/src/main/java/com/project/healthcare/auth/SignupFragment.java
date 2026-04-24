package com.project.healthcare.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.project.healthcare.R;
import com.project.healthcare.data.AppRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Locale;

public class SignupFragment extends Fragment {

    private static final String TAG = "SignupFragment";
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final String ROLE_PATIENT = "patient";
    private static final String ROLE_DOCTOR = "doctor";

    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private RadioGroup roleGroup;
    private RadioButton rolePatientRadio;
    private RadioButton roleDoctorRadio;
    private Button signupButton;
    private TextView switchToLoginText;
    private ProgressBar signupProgress;

    public SignupFragment() {
        super(R.layout.fragment_signup);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nameInput = view.findViewById(R.id.input_signup_name);
        emailInput = view.findViewById(R.id.input_signup_email);
        passwordInput = view.findViewById(R.id.input_signup_password);
        confirmPasswordInput = view.findViewById(R.id.input_signup_confirm_password);
        roleGroup = view.findViewById(R.id.group_signup_role);
        rolePatientRadio = view.findViewById(R.id.radio_role_patient);
        roleDoctorRadio = view.findViewById(R.id.radio_role_doctor);
        signupButton = view.findViewById(R.id.button_signup);
        switchToLoginText = view.findViewById(R.id.text_switch_to_login);
        signupProgress = view.findViewById(R.id.progress_signup);

        signupButton.setOnClickListener(v -> attemptSignup());
        switchToLoginText.setOnClickListener(v -> navigateToLogin());
    }

    private void navigateToLogin() {
        if (getActivity() instanceof AuthActivity) {
            ((AuthActivity) getActivity()).openLogin();
        }
    }

    private void attemptSignup() {
        String name = readTrimmedText(nameInput);
        String email = readTrimmedText(emailInput);
        String password = readText(passwordInput);
        String confirmPassword = readText(confirmPasswordInput);
        String role = readSelectedRole();

        if (!validateInput(name, email, password, confirmPassword, role)) {
            return;
        }

        setLoading(true);
        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) {
                        return;
                    }

                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = task.getResult() != null
                                ? task.getResult().getUser()
                                : FirebaseAuth.getInstance().getCurrentUser();
                        updateProfileAndContinue(firebaseUser, name, email, role);
                    } else {
                        setLoading(false);
                        String errorMessage = resolveAuthError(task.getException());
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String resolveAuthError(@Nullable Exception exception) {
        if (exception == null) {
            return getString(R.string.auth_error_generic);
        }

        Log.e(TAG, "Signup failed", exception);

        String localizedMessage = exception.getLocalizedMessage();
        String normalized = localizedMessage == null ? "" : localizedMessage.toUpperCase(Locale.US);

        if (normalized.contains("CONFIGURATION_NOT_FOUND")) {
            return getString(R.string.auth_error_firebase_configuration);
        }

        if (!TextUtils.isEmpty(localizedMessage)) {
            return localizedMessage;
        }

        return getString(R.string.auth_error_generic);
    }

    private void updateProfileAndContinue(@Nullable FirebaseUser firebaseUser, String name, String email, String role) {
        if (firebaseUser == null) {
            setLoading(false);
            Toast.makeText(requireContext(), getString(R.string.auth_error_generic), Toast.LENGTH_LONG).show();
            return;
        }

        UserProfileChangeRequest profileChangeRequest =
                new UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build();

        firebaseUser.updateProfile(profileChangeRequest)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) {
                        return;
                    }
                    completeSignup(firebaseUser, name, email, role);
                });
    }

    private void completeSignup(FirebaseUser firebaseUser, String name, String email, String role) {
        AppRepository.getInstance().createOrUpdateUserProfile(firebaseUser.getUid(), name, email, role, (success, message) -> {
            if (!isAdded()) {
                return;
            }
            setLoading(false);
            if (success) {
                Toast.makeText(requireContext(), R.string.auth_signup_success, Toast.LENGTH_SHORT).show();
                if (getActivity() instanceof AuthActivity) {
                    ((AuthActivity) getActivity()).onAuthSuccess();
                }
            } else {
                Toast.makeText(requireContext(), message != null ? message : getString(R.string.auth_error_generic), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateInput(String name, String email, String password, String confirmPassword, String role) {
        if (TextUtils.isEmpty(name)) {
            nameInput.setError(getString(R.string.auth_error_name_required));
            nameInput.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            emailInput.setError(getString(R.string.auth_error_email_required));
            emailInput.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError(getString(R.string.auth_error_email_invalid));
            emailInput.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError(getString(R.string.auth_error_password_required));
            passwordInput.requestFocus();
            return false;
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            passwordInput.setError(getString(R.string.auth_error_password_min));
            passwordInput.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInput.setError(getString(R.string.auth_error_confirm_password_required));
            confirmPasswordInput.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError(getString(R.string.auth_error_confirm_password_mismatch));
            confirmPasswordInput.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(role)) {
            Toast.makeText(requireContext(), R.string.auth_error_role_required, Toast.LENGTH_SHORT).show();
            roleGroup.requestFocus();
            return false;
        }

        return true;
    }

    private String readText(EditText editText) {
        if (editText.getText() == null) {
            return "";
        }
        return editText.getText().toString();
    }

    private String readTrimmedText(EditText editText) {
        return readText(editText).trim();
    }

    private void setLoading(boolean loading) {
        signupButton.setEnabled(!loading);
        switchToLoginText.setEnabled(!loading);
        nameInput.setEnabled(!loading);
        emailInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
        confirmPasswordInput.setEnabled(!loading);
        roleGroup.setEnabled(!loading);
        rolePatientRadio.setEnabled(!loading);
        roleDoctorRadio.setEnabled(!loading);
        signupProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private String readSelectedRole() {
        int checkedId = roleGroup.getCheckedRadioButtonId();
        if (checkedId == R.id.radio_role_doctor) {
            return ROLE_DOCTOR;
        }
        if (checkedId == R.id.radio_role_patient) {
            return ROLE_PATIENT;
        }
        return "";
    }
}