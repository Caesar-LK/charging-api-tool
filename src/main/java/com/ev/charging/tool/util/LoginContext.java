package com.ev.charging.tool.util;

/**
 * 登录态上下文（ThreadLocal 实现，每个请求线程独立）。
 */
public final class LoginContext {

    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();
    private static final ThreadLocal<String> PHONE = new ThreadLocal<>();

    private LoginContext() {
    }

    public static void setToken(String token) {
        TOKEN.set(token);
    }

    public static String getToken() {
        return TOKEN.get();
    }

    public static void setPhone(String phone) {
        PHONE.set(phone);
    }

    public static String getPhone() {
        return PHONE.get();
    }

    public static boolean isLoggedIn() {
        String t = TOKEN.get();
        return t != null && !t.isEmpty();
    }

    public static void clear() {
        TOKEN.remove();
        PHONE.remove();
    }
}
