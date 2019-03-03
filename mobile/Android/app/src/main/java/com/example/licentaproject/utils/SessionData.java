package com.example.licentaproject.utils;

import com.example.licentaproject.models.User;

public class SessionData {

    private static final String serverUrl = "192.168.0.106:3000";
    private static String token = null;
    private static User user = null;

    private SessionData() {

    }

    public static String getToken() {
        return token;
    }

    public static void setToken(String token) {
        SessionData.token = token;
    }

    public static String getServerUrl() {
        return serverUrl;
    }

    public static User getUser() {
        return user;
    }

    public static void setUser(User user) {
        SessionData.user = user;
    }
}
