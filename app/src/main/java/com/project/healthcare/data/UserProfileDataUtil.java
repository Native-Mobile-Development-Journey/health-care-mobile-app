package com.project.healthcare.data;

import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserProfileDataUtil {

    @Nullable
    public static String parseRoleFromSnapshot(DataSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }

        Object roleValue = snapshot.child("role").getValue();
        if (roleValue instanceof String) {
            String role = ((String) roleValue).trim().toLowerCase(Locale.ROOT);
            return role.isEmpty() ? null : role;
        }

        return null;
    }

    @Nullable
    public static String parseRoleFromMap(@Nullable Map<String, Object> profileData) {
        if (profileData == null) {
            return null;
        }

        Object roleValue = profileData.get("role");
        if (roleValue instanceof String) {
            String role = ((String) roleValue).trim().toLowerCase(Locale.ROOT);
            return role.isEmpty() ? null : role;
        }

        return null;
    }

    public static Map<String, Object> buildProfile(String uid, String name, String email, @Nullable String role) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("uid", uid);
        profile.put("name", name);
        profile.put("email", email);
        if (role != null) {
            profile.put("role", role);
        }
        return profile;
    }
}
