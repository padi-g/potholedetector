package org.reapbenefit.gautam.intern.potholedetectorbeta.Core;

import java.io.Serializable;

public class SpeedWithLocation implements Serializable{
    private float speed;
    private double latitude;
    private double longitude;

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
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
