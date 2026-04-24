package com.project.healthcare.auth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AuthActivityTest {

    @Test
    public void isDoctorRole_returnsTrueForDoctorExactMatch() {
        assertTrue(AuthActivity.isDoctorRole("doctor"));
    }

    @Test
    public void isDoctorRole_returnsTrueForDoctorWithDifferentCasing() {
        assertTrue(AuthActivity.isDoctorRole("DoCtOr"));
    }

    @Test
    public void isDoctorRole_returnsFalseForEmptyRole() {
        assertFalse(AuthActivity.isDoctorRole(""));
    }

    @Test
    public void isDoctorRole_returnsFalseForPatientRole() {
        assertFalse(AuthActivity.isDoctorRole("patient"));
    }

    @Test
    public void isDoctorRole_returnsFalseForNullRole() {
        assertFalse(AuthActivity.isDoctorRole(null));
    }
}
