package com.example.licentaproject.utils;

import android.location.Location;

import com.example.licentaproject.models.User;
import com.google.android.gms.maps.model.Circle;

import java.util.ArrayList;
import java.util.List;

public class SessionData {

    private static final String serverUrl = "192.168.1.102:3000";
    private static final String pingUrl = "192.168.1.102";
    private static String token = null;
    private static User user = null;
    private static Circle activeTracker = null;    // used for map discovery
    private static List<Object> foundPool = new ArrayList<>();
    private static Location lastLocation = null;

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

    public static Circle getActiveTracker() {
        return activeTracker;
    }

    public static void setActiveTracker(Circle activeTracker) {
        SessionData.activeTracker = activeTracker;
    }

    public static List<Object> getFoundPool() {
        return foundPool;
    }

    public static String getPingUrl() {
        return pingUrl;
    }

    public static Location getLastLocation() {
        return lastLocation;
    }

    public static void setLastLocation(Location lastLocation) {
        SessionData.lastLocation = lastLocation;
    }
}
