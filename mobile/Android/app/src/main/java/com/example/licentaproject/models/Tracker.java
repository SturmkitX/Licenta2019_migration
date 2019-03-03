package com.example.licentaproject.models;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Tracker {

    @JsonProperty(value = "_id")
    private String id;
    private String name;
    private boolean lost;
    private boolean alarmActive;
    private boolean wifiActive;
    private boolean gpsActive;
    private List<Byte> rfId;
    private String userId;

    @JsonProperty(value = "__v")
    private int version;
    private List<Object> history;

    public Tracker() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isLost() {
        return lost;
    }

    public void setLost(boolean lost) {
        this.lost = lost;
    }

    public boolean isAlarmActive() {
        return alarmActive;
    }

    public void setAlarmActive(boolean alarmActive) {
        this.alarmActive = alarmActive;
    }

    public boolean isWifiActive() {
        return wifiActive;
    }

    public void setWifiActive(boolean wifiActive) {
        this.wifiActive = wifiActive;
    }

    public boolean isGpsActive() {
        return gpsActive;
    }

    public void setGpsActive(boolean gpsActive) {
        this.gpsActive = gpsActive;
    }

    public List<Byte> getRfId() {
        return rfId;
    }

    public void setRfId(List<Byte> rfId) {
        this.rfId = rfId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<Object> getHistory() {
        return history;
    }

    public void setHistory(List<Object> history) {
        this.history = history;
    }
}
