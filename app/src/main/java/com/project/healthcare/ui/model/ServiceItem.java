package com.project.healthcare.ui.model;

public class ServiceItem {

    public final String label;
    public final int iconRes;
    public final boolean enabled;

    public ServiceItem(String label, int iconRes, boolean enabled) {
        this.label = label;
        this.iconRes = iconRes;
        this.enabled = enabled;
    }
}
