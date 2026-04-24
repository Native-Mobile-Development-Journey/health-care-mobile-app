package com.project.healthcare.ui.model;

public class DateOption {

    public final String dayLabel;
    public final String dateLabel;
    public final boolean disabled;

    public DateOption(String dayLabel, String dateLabel, boolean disabled) {
        this.dayLabel = dayLabel;
        this.dateLabel = dateLabel;
        this.disabled = disabled;
    }
}
