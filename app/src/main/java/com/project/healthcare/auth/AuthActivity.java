package com.project.healthcare.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.project.healthcare.MainActivity;
import com.project.healthcare.DoctorDashboardActivity;
import com.project.healthcare.R;
import com.project.healthcare.data.AppRepository;
import com.google.firebase.auth.FirebaseAuth;

public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);

        setupInsets();

        if (savedInstanceState == null) {
            showLoginScreen();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            routeUserByRole();
        }
    }

    public void openSignup() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.auth_fragment_container, new SignupFragment(), "signup")
                .addToBackStack("signup")
                .commit();
    }

    public void openLogin() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            showLoginScreen();
        }
    }

    public void onAuthSuccess() {
        routeUserByRole();
    }

    public static boolean isDoctorRole(@Nullable String role) {
        return role != null && "doctor".equalsIgnoreCase(role.trim());
    }

    private void routeUserByRole() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            openMainAndFinish();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        AppRepository.getInstance().fetchUserRole(uid, new AppRepository.RoleCallback() {
            @Override
            public void onRoleLoaded(@Nullable String role) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (isDoctorRole(role)) {
                    openDoctorDashboardAndFinish();
                } else {
                    openMainAndFinish();
                }
            }

            @Override
            public void onError(String message) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                openMainAndFinish();
            }
        });
    }

    private void showLoginScreen() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.auth_fragment_container, new LoginFragment(), "login")
                .commit();
    }

    private void openMainAndFinish() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void openDoctorDashboardAndFinish() {
        Intent intent = new Intent(this, DoctorDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupInsets() {
        View root = findViewById(R.id.auth_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}