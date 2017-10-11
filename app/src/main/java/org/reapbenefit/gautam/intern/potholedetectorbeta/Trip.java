package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.location.Location;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by gautam on 29/06/17.
 */

public class Trip {

    String trip_id;  // unique id for trip and name for the file
    String user_id;  // user who created this trip

    //String firepath;  // path to firebase storage
    //Uri tripfile;
    long filesize;

    boolean uploaded;

    String date, startTime, endTime;
    //String vehicle;
    MyLocation startLoc, endLoc;
    int no_of_lines;

    //float distance;
    long duration;

    String device;
    //int rating;

    public Trip() {
    }

    public Trip(Trip t) {
        this.trip_id = t.trip_id;
        this.user_id = t.user_id;
        //this.firepath = t.firepath;
        //this.tripfile = t.tripfile;
        this.filesize = t.filesize;
        this.uploaded = t.uploaded;
        this.startTime = t.startTime;
        this.endTime = t.endTime;
        this.startLoc = t.startLoc;
        this.endLoc = t.endLoc;
        this.no_of_lines = t.no_of_lines;
        //this.distance = t.distance;
        this.duration = t.duration;
        this.device = t.device;
    }  // copy constructor

// LoggerService -> EasyModeFragment -> FileProcessor.java
    // when application gets killed, the old trip instance is lost.

    public void writeToJsonFile() {

        // load all trip objects to an arraylist of trips
        // search for relevant trip object
        // write new data to it

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
/*
    public Uri getTripfile() {
        return tripfile;
    }

    public void setTripfile(Uri tripfile) {
        this.tripfile = tripfile;
    }
*/
    public long getFilesize() {
        return filesize;
    }

    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    public boolean isUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
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

    public MyLocation getStartLoc() {
        return startLoc;
    }

    public void setStartLoc(MyLocation startLoc) {
        this.startLoc = startLoc;
    }

    public MyLocation getEndLoc() {
        return endLoc;
    }

    public void setEndLoc(MyLocation endLoc) {
        this.endLoc = endLoc;
    }

    public int getNo_of_lines() {
        return no_of_lines;
    }

    public void setNo_of_lines(int no_of_lines) {
        this.no_of_lines = no_of_lines;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }
}
