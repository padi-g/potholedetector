package org.reapbenefit.gautam.intern.potholedetectorbeta.Core;

import android.location.Location;

public class SpeedWithLocation {
    private float speed;
    private Location location;

    public float getSpeed() {
        return speed;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }
}
