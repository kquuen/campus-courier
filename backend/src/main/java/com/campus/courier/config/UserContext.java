package com.campus.courier.config;

import com.campus.courier.entity.UserRole;

public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<UserRole> ROLE = new ThreadLocal<>();

    public static void set(Long userId, UserRole role) {
        USER_ID.set(userId);
        ROLE.set(role);
    }

    public static Long getUserId() { return USER_ID.get(); }
    public static UserRole getRole() { return ROLE.get(); }

    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
    }
}
