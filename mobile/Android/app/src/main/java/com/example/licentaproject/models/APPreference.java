package com.example.licentaproject.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class APPreference implements Serializable {

    @JsonProperty("_id")
    private String id;

    private String ssid;
    private String password;
    private boolean active;

    public APPreference() {
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
