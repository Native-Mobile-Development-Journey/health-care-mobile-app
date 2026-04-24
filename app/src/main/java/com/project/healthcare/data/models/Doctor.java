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
    public int ratingCount;
    public String qualification;
    public String languages;
    public String consultationFee;
    public String address;
    public String photoUrl;
    public boolean isAvailable;
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
            int ratingCount,
            String qualification,
            String languages,
            String consultationFee,
            String address,
            String photoUrl,
            boolean isAvailable,
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
        this.ratingCount = ratingCount;
        this.qualification = qualification;
        this.languages = languages;
        this.consultationFee = consultationFee;
        this.address = address;
        this.photoUrl = photoUrl;
        this.isAvailable = isAvailable;
        this.recommended = recommended;
    }
}
