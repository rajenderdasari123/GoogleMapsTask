package com.example.directionsexample.model;

import android.os.Parcel;
import android.os.Parcelable;

public class GetInfo implements Parcelable {

    private double latitude;
    private double longitude;
    private String day;
    private String time;
    private String address;
    private int id;
    private String placeId;

    public GetInfo() {

    }

    protected GetInfo(Parcel in) {
        latitude = in.readDouble();
        longitude = in.readDouble();
        day = in.readString();
        time = in.readString();
        address = in.readString();
        id = in.readInt();
        placeId = in.readString();
    }

    public static final Creator<GetInfo> CREATOR = new Creator<GetInfo>() {
        @Override
        public GetInfo createFromParcel(Parcel in) {
            return new GetInfo(in);
        }

        @Override
        public GetInfo[] newArray(int size) {
            return new GetInfo[size];
        }
    };

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }


    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(day);
        dest.writeString(time);
        dest.writeString(address);
        dest.writeInt(id);
        dest.writeString(placeId);
    }
}
