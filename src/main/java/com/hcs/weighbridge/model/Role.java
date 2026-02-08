package com.hcs.weighbridge.model;

public enum Role {
    ADMIN,
    USER;

    public static Role fromString(String role) {
        if (role == null)
            return USER; // Default
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }
}
