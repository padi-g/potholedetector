package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.location.Location;
import android.net.Uri;

import java.util.UUID;

/**
 * Created by gautam on 29/06/17.
 */

public class Trip {

    String startTime, endTime, vehicle;
    String firepath;  // path to firebase storage
    String user_id;  // user who created this trip
    Location startLoc, endLoc;
    float distance; // in km
    UUID trip_id;  // unique id for trip and name for the file
    int duration; // in minutes
    Uri tripfile;

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
        return user_id;
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

    public void setUsername(String user_id) {
        this.user_id = user_id;
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


    void upload_file(){
        // procedure to upload this.tripfile
    }
}
