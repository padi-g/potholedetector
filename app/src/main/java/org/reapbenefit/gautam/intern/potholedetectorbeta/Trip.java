package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.location.Location;
import android.net.Uri;

/**
 * Created by gautam on 29/06/17.
 */

public class Trip {

    String startTime, endTime, vehicle, username;
    Location startLoc, endLoc;
    int duration; // in minutes
    Uri tripfile;

    public Trip(String startTime, String endTime, String vehicle, String username, Location startLoc, Location endLoc, int duration, Uri tripfile) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.vehicle = vehicle;
        this.username = username;
        this.startLoc = startLoc;
        this.endLoc = endLoc;
        this.duration = duration;
        this.tripfile = tripfile;
    }

    public Trip(){}

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getVehicle() {
        return vehicle;
    }

    public String getUsername() {
        return username;
    }

    public Location getStartLoc() {
        return startLoc;
    }

    public Location getEndLoc() {
        return endLoc;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setEndLoc(Location endLoc) {
        this.endLoc = endLoc;
    }

    public void setStartLoc(Location startLoc) {
        this.startLoc = startLoc;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setTripfile(Uri tripfile) {
        this.tripfile = tripfile;
    }

    public Uri getTripfile() {

        return tripfile;
    }

}
