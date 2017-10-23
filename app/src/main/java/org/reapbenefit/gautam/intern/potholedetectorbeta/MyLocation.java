package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by gautam on 11/10/17.
 */
public class MyLocation  {


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

}
