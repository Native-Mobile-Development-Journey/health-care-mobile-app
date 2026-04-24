package com.project.healthcare.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class UserProfileDataUtilTest {

    private static final String TEST_UID = "test-user-id";
    private static final String TEST_NAME = "Dr. Alice";
    private static final String TEST_EMAIL = "doctor-test@example.com";
    private static final String TEST_ROLE = "doctor";

    @Test
    public void buildProfile_includesDoctorRoleAndEmail() {
        Map<String, Object> profile = UserProfileDataUtil.buildProfile(TEST_UID, TEST_NAME, TEST_EMAIL, TEST_ROLE);

        assertEquals(TEST_UID, profile.get("uid"));
        assertEquals(TEST_NAME, profile.get("name"));
        assertEquals(TEST_EMAIL, profile.get("email"));
        assertEquals(TEST_ROLE, profile.get("role"));
    }

    @Test
    public void buildProfile_allowsNullRole() {
        Map<String, Object> profile = UserProfileDataUtil.buildProfile(TEST_UID, TEST_NAME, TEST_EMAIL, null);

        assertEquals(TEST_UID, profile.get("uid"));
        assertEquals(TEST_NAME, profile.get("name"));
        assertEquals(TEST_EMAIL, profile.get("email"));
        assertNull(profile.get("role"));
    }

    @Test
    public void parseRoleFromMap_returnsLowercasedDoctorRole() {
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("uid", TEST_UID);
        profileData.put("name", TEST_NAME);
        profileData.put("email", TEST_EMAIL);
        profileData.put("role", "Doctor   ");

        assertEquals("doctor", UserProfileDataUtil.parseRoleFromMap(profileData));
    }

    @Test
    public void parseRoleFromMap_returnsNullWhenRoleMissing() {
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("uid", TEST_UID);
        profileData.put("name", TEST_NAME);
        profileData.put("email", TEST_EMAIL);

        assertNull(UserProfileDataUtil.parseRoleFromMap(profileData));
    }

    @Test
    public void parseRoleFromMap_returnsNullWhenMapMissing() {
        assertNull(UserProfileDataUtil.parseRoleFromMap(null));
    }
}
