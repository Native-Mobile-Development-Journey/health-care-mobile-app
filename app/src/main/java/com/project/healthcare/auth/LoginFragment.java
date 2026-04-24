package com.project.healthcare.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    private EditText emailInput;
    private EditText passwordInput;
    private Button loginButton;
    private TextView switchToSignupText;
    private ProgressBar loginProgress;

    public LoginFragment() {
        super(R.layout.fragment_login);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emailInput = view.findViewById(R.id.input_login_email);
        passwordInput = view.findViewById(R.id.input_login_password);
        loginButton = view.findViewById(R.id.button_login);
        switchToSignupText = view.findViewById(R.id.text_switch_to_signup);
        loginProgress = view.findViewById(R.id.progress_login);

        loginButton.setOnClickListener(v -> attemptLogin());
        switchToSignupText.setOnClickListener(v -> navigateToSignup());
    }

    private void navigateToSignup() {
        if (getActivity() instanceof AuthActivity) {
            ((AuthActivity) getActivity()).openSignup();
        }
    }

    private void attemptLogin() {
        String email = readTrimmedText(emailInput);
        String password = readText(passwordInput);

        if (!validateInput(email, password)) {
            return;
        }

        setLoading(true);
        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) {
                        return;
                    }

                    setLoading(false);

                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(), R.string.auth_login_success, Toast.LENGTH_SHORT).show();
                        if (getActivity() instanceof AuthActivity) {
                            ((AuthActivity) getActivity()).onAuthSuccess();
                        }
                    } else {
                        String errorMessage = resolveAuthError(task.getException());
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String resolveAuthError(@Nullable Exception exception) {
        if (exception == null) {
            return getString(R.string.auth_error_generic);
        }

        Log.e(TAG, "Login failed", exception);

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

    private boolean validateInput(String email, String password) {
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
        loginButton.setEnabled(!loading);
        switchToSignupText.setEnabled(!loading);
        emailInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
        loginProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }
}