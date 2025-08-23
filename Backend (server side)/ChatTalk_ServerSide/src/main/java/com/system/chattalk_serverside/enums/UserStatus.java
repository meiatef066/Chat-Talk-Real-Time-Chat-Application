package com.system.chattalk_serverside.enums;

public enum UserStatus {

    ACTIVE("ACTIVE", "User is active and can use the application"),

    INACTIVE("INACTIVE", "User account is inactive"),

    SUSPENDED("SUSPENDED", "User account is suspended"),

    DELETED("DELETED", "User account is deleted");

    private final String statusName;
    private final String description;

    UserStatus(String statusName, String description) {
        this.statusName = statusName;
        this.description = description;
    }

    public String getStatusName() {
        return statusName;
    }

    public String getDescription() {
        return description;
    }

    public static UserStatus fromStatusName(String statusName) {
        for (UserStatus status : values()) {
            if (status.statusName.equalsIgnoreCase(statusName)) {
                return status;
            }
        }
        return null;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean canLogin() {
        return this == ACTIVE || this == INACTIVE;
    }

    @Override
    public String toString() {
        return statusName;
    }
}
