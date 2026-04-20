package com.example.smdproject;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.smdproject.fragments.DoctorDetailFragment;
import com.example.smdproject.fragments.HomeFragment;
import com.example.smdproject.fragments.MessageFragment;
import com.example.smdproject.fragments.ScheduleFragment;
import com.example.smdproject.fragments.SettingFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new DoctorDetailFragment(), "doctor_detail")
                .addToBackStack("doctor_detail")
                .commit();
        bottomNavigationView.setVisibility(View.GONE);
    }
}