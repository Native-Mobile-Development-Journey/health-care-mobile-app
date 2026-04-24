package com.project.healthcare.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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

public class SignupFragment extends Fragment {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
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

        if (!validateInput(name, email, password, confirmPassword)) {
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
                        updateProfileAndContinue(name, email);
                    } else {
                        setLoading(false);
                        String errorMessage = getString(R.string.auth_error_generic);
                        if (task.getException() != null && !TextUtils.isEmpty(task.getException().getLocalizedMessage())) {
                            errorMessage = task.getException().getLocalizedMessage();
                        }
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateProfileAndContinue(String name, String email) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            completeSignup(name, email);
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
                    completeSignup(name, email);
                });
    }

    private void completeSignup(String name, String email) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            AppRepository.getInstance().createOrUpdateUserProfile(firebaseUser.getUid(), name, email);
        }

        setLoading(false);
        Toast.makeText(requireContext(), R.string.auth_signup_success, Toast.LENGTH_SHORT).show();
        if (getActivity() instanceof AuthActivity) {
            ((AuthActivity) getActivity()).onAuthSuccess();
        }
    }

    private boolean validateInput(String name, String email, String password, String confirmPassword) {
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
        signupProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}