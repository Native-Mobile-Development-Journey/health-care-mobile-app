package com.project.healthcare.data.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Doctor {

    public String id;
    public String name;
    public String specialty;
    public String hospital;
    public String bio;
    public int experienceYears;
    public int patientCount;
    public double rating;
    public boolean recommended;

    public Doctor() {
        // Needed for Firebase deserialization.
    }

    public Doctor(
            String id,
            String name,
            String specialty,
            String hospital,
            String bio,
            int experienceYears,
            int patientCount,
            double rating,
            boolean recommended
    ) {
        this.id = id;
        this.name = name;
        this.specialty = specialty;
        this.hospital = hospital;
        this.bio = bio;
        this.experienceYears = experienceYears;
        this.patientCount = patientCount;
        this.rating = rating;
        this.recommended = recommended;
    }
}
