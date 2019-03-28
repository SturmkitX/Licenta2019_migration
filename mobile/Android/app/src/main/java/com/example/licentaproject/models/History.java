package com.example.licentaproject.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class History implements Parcelable {

    @JsonProperty("_id")
    private String id;

    @JsonProperty("__v")
    private int version;

    @JsonProperty("lat")
    private float latitude;

    @JsonProperty("lng")
    private float longitude;

    private float range;
    private long creationDate;

    private String source;

    public History() {
    }

    protected History(Parcel in) {
        id = in.readString();
        version = in.readInt();
        latitude = in.readFloat();
        longitude = in.readFloat();
        range = in.readFloat();
        creationDate = in.readLong();
        source = in.readString();
    }

    public static final Creator<History> CREATOR = new Creator<History>() {
        @Override
        public History createFromParcel(Parcel in) {
            return new History(in);
        }

        @Override
        public History[] newArray(int size) {
            return new History[size];
        }
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getRange() {
        return range;
    }

    public void setRange(float range) {
        this.range = range;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeInt(version);
        dest.writeFloat(latitude);
        dest.writeFloat(longitude);
        dest.writeFloat(range);
        dest.writeLong(creationDate);
        dest.writeString(source);
    }
}
