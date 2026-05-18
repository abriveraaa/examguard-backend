package com.example.backend.utility;

public class SessionContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    public static void set(String userId, String role) {
        USER_ID.set(userId);
        ROLE.set(role);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static String getRole() {
        return ROLE.get();
    }

    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
    }
}