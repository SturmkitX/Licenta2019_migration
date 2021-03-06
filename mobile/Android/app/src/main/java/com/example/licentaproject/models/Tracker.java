package com.example.licentaproject.models;


import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Tracker implements Parcelable {

    @JsonProperty(value = "_id")
    private String id;
    private String name;
    private boolean lost;
    private boolean wifiActive;
    private boolean gpsActive;
    private String preferredMethod;
    private List<Byte> rfId;
    private String userId;
    private List<APPreference> aps;
    private long lastUpdated;

    private History lastPosition;

    @JsonProperty(value = "__v")
    private int version;

    @JsonIgnore
    private List<History> history;

    public Tracker() {
    }

    protected Tracker(Parcel in) {
        id = in.readString();
        name = in.readString();
        lost = in.readByte() != 0;
        wifiActive = in.readByte() != 0;
        gpsActive = in.readByte() != 0;
        preferredMethod = in.readString();
        userId = in.readString();
        version = in.readInt();
        rfId = (List<Byte>) in.readArrayList(Byte.class.getClassLoader());
        aps = (List<APPreference>) in.readArrayList(APPreference.class.getClassLoader());
        history = (List<History>) in.readArrayList(History.class.getClassLoader());
        lastUpdated = in.readLong();
        lastPosition = in.readParcelable(History.class.getClassLoader());
    }

    public static final Creator<Tracker> CREATOR = new Creator<Tracker>() {
        @Override
        public Tracker createFromParcel(Parcel in) {
            return new Tracker(in);
        }

        @Override
        public Tracker[] newArray(int size) {
            return new Tracker[size];
        }
    };

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

    public List<History> getHistory() {
        return history;
    }

    public void setHistory(List<History> history) {
        this.history = history;
    }

    public String getPreferredMethod() {
        return preferredMethod;
    }

    public void setPreferredMethod(String preferredMethod) {
        this.preferredMethod = preferredMethod;
    }

    public List<APPreference> getAps() {
        return aps;
    }

    public void setAps(List<APPreference> aps) {
        this.aps = aps;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public History getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(History lastPosition) {
        this.lastPosition = lastPosition;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeByte((byte)(lost ? 1 : 0));
        dest.writeByte((byte)(wifiActive ? 1 : 0));
        dest.writeByte((byte)(gpsActive ? 1 : 0));
        dest.writeString(preferredMethod);
        dest.writeString(userId);
        dest.writeInt(version);
        dest.writeList(rfId);
        dest.writeList(aps);
        dest.writeList(history);
        dest.writeLong(lastUpdated);
        dest.writeParcelable(lastPosition, 0);
    }
}
