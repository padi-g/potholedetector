package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by gautam on 11/10/17.
 */
public class MyLocation implements Parcelable {


    private float accuracy;
    private double latitude, longitude;

    public MyLocation() {
    }

    public static MyLocation locToMyloc(Location l){

        MyLocation m = new MyLocation();

        m.setAccuracy(l.getAccuracy());
        m.setLatitude(l.getLatitude());
        m.setLongitude(l.getLongitude());

        return m;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(this.accuracy);
        dest.writeDouble(this.latitude);
        dest.writeDouble(this.longitude);
    }

    protected MyLocation(Parcel in) {
        this.accuracy = in.readFloat();
        this.latitude = in.readDouble();
        this.longitude = in.readDouble();
    }

    public static final Parcelable.Creator<MyLocation> CREATOR = new Parcelable.Creator<MyLocation>() {
        @Override
        public MyLocation createFromParcel(Parcel source) {
            return new MyLocation(source);
        }

        @Override
        public MyLocation[] newArray(int size) {
            return new MyLocation[size];
        }
    };
}
