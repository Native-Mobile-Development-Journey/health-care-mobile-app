package com.project.healthcare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.project.healthcare.auth.AuthActivity;
import com.project.healthcare.data.AppRepository;
import com.project.healthcare.fragments.DoctorDetailFragment;
import com.project.healthcare.fragments.HomeFragment;
import com.project.healthcare.fragments.MessageFragment;
import com.project.healthcare.fragments.ScheduleFragment;
import com.project.healthcare.fragments.SettingFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        setupInsets();
        setupBottomNavigation();

        getSupportFragmentManager().addOnBackStackChangedListener(this::syncBottomNavigationVisibility);

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else {
            syncBottomNavigationVisibility();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return;
        }

        AppRepository repository = AppRepository.getInstance();
        String displayName = currentUser.getDisplayName() != null && !currentUser.getDisplayName().trim().isEmpty()
                ? currentUser.getDisplayName().trim()
                : "Patient";
        String email = currentUser.getEmail() != null ? currentUser.getEmail() : "";
        repository.createOrUpdateUserProfile(currentUser.getUid(), displayName, email);
    }

    private void setupInsets() {
        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            bottomNavigationView.setPadding(
                    bottomNavigationView.getPaddingLeft(),
                    bottomNavigationView.getPaddingTop(),
                    bottomNavigationView.getPaddingRight(),
                    systemBars.bottom
            );
            return insets;
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                navigateToRoot(new HomeFragment(), "home");
                return true;
            }

            if (itemId == R.id.nav_schedule) {
                navigateToRoot(new ScheduleFragment(), "schedule");
                return true;
            }

            if (itemId == R.id.nav_message) {
                navigateToRoot(new MessageFragment(), "message");
                return true;
            }

            if (itemId == R.id.nav_setting) {
                navigateToRoot(new SettingFragment(), "setting");
                return true;
            }

            return false;
        });
    }

    private void navigateToRoot(Fragment fragment, String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
        bottomNavigationView.setVisibility(View.VISIBLE);
    }

    private void syncBottomNavigationVisibility() {
        boolean hasBackStack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        bottomNavigationView.setVisibility(hasBackStack ? View.GONE : View.VISIBLE);
    }

    public void openDoctorDetail() {
        openDoctorDetail(null, null);
    }

    public void openDoctorDetail(@Nullable String doctorId, @Nullable String appointmentId) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, DoctorDetailFragment.newInstance(doctorId, appointmentId), "doctor_detail")
                .addToBackStack("doctor_detail")
                .commit();
        bottomNavigationView.setVisibility(View.GONE);
    }

    public void openScheduleTab() {
        bottomNavigationView.setSelectedItemId(R.id.nav_schedule);
    }
}