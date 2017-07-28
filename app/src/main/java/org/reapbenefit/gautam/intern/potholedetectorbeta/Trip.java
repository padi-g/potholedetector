package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.location.Location;
import android.net.Uri;

import java.util.UUID;

/**
 * Created by gautam on 29/06/17.
 */

public class Trip {

    String trip_id;  // unique id for trip and name for the file
    String user_id;  // user who created this trip

    String firepath;  // path to firebase storage
    Uri tripfile;

    String startTime, endTime;
    //String vehicle;
    Location startLoc, endLoc;
    int no_of_lines;

    /*
        float distance; // in km
        int duration; // in minutes
        */
    String device;
    //int rating;

    public Trip(){}

    public void analyse_file(){
        // calculate the std deviation
        // find the number of bumps and the locations

        // write the file metadata into the file or not?
    }

    public void upload_file(){
        // procedure to upload this.tripfile
    }

    public String getTrip_id() {
        return trip_id;
    }

    public void setTrip_id(String trip_id) {
        this.trip_id = trip_id;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getFirepath() {
        return firepath;
    }

    public void setFirepath(String firepath) {
        this.firepath = firepath;
    }

    public Uri getTripfile() {
        return tripfile;
    }

    public void setTripfile(Uri tripfile) {
        this.tripfile = tripfile;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    /*
    public String getVehicle() {
        return vehicle;
    }

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
    }
    */

    public Location getStartLoc() {
        return startLoc;
    }

    public void setStartLoc(Location startLoc) {
        this.startLoc = startLoc;
    }

    public Location getEndLoc() {
        return endLoc;
    }

    public void setEndLoc(Location endLoc) {
        this.endLoc = endLoc;
    }

    /*
    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
    */

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    /*
    public int getRating() {
        return rating;
    }


    public void setRating(int rating) {
        this.rating = rating;
    }
    */

    public int getNo_of_lines() {
        return no_of_lines;
    }

    public void setNo_of_lines(int no_of_lines) {
        this.no_of_lines = no_of_lines;
    }
}
