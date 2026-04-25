package com.project.healthcare;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.project.healthcare.auth.AuthActivity;
import com.project.healthcare.fragments.DoctorAppointmentsFragment;
import com.project.healthcare.fragments.DoctorAvailabilityFragment;
import com.project.healthcare.fragments.DoctorMessagesFragment;
import com.project.healthcare.fragments.DoctorOverviewFragment;
import com.project.healthcare.fragments.DoctorSettingsFragment;

public class DoctorDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_doctor_dashboard);
        setupInsets();
        setupNavigation();
        setCurrentFragment(new DoctorOverviewFragment());
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_doctor);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_doctor_overview) {
                setCurrentFragment(new DoctorOverviewFragment());
                return true;
            }
            if (itemId == R.id.menu_doctor_appointments) {
                setCurrentFragment(new DoctorAppointmentsFragment());
                return true;
            }
            if (itemId == R.id.menu_doctor_availability) {
                setCurrentFragment(new DoctorAvailabilityFragment());
                return true;
            }
            if (itemId == R.id.menu_doctor_messages) {
                setCurrentFragment(new DoctorMessagesFragment());
                return true;
            }
            if (itemId == R.id.menu_doctor_settings) {
                setCurrentFragment(new DoctorSettingsFragment());
                return true;
            }
            return false;
        });
    }

    private void setCurrentFragment(@NonNull Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.doctor_dashboard_container, fragment);
        transaction.commit();
    }

    private void setupInsets() {
        View root = findViewById(R.id.doctor_dashboard_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
