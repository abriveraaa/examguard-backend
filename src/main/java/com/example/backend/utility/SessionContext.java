package com.example.backend.utility;

public class SessionContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> SCHOOL_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    public static void set(
            String userId,
            String schoolId,
            String role
    ) {
        USER_ID.set(userId);
        SCHOOL_ID.set(schoolId);
        ROLE.set(role);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static String getSchoolId() {
        return SCHOOL_ID.get();
    }

    public static String getRole() {
        return ROLE.get();
    }

    public static void clear() {
        USER_ID.remove();
        SCHOOL_ID.remove();
        ROLE.remove();
    }
}