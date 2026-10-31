package com.campus.events.model;

public enum AttendanceStatus {
    PRESENT("Present", "badge-success"),
    ABSENT("Not Checked In", "badge-secondary");

    private final String displayName;
    private final String badgeClass;

    AttendanceStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}

