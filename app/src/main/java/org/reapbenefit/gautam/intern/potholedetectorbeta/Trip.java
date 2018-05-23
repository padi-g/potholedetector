package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by gautam on 29/06/17.
 */

public class Trip implements Parcelable {

    private String trip_id;  // unique id for trip and name for the file
    private String user_id;  // user who created this trip

    private long filesize;

    private boolean uploaded;

    private String startTime, endTime;
    //String vehicle;
    private MyLocation startLoc, endLoc;
    private int no_of_lines;

    private float distanceInKM;
    private long duration;

    private String device;

    private int userRating;
    private String axis;
    private float threshold;
    private int potholeCount;

    private long minutesWasted;
    private long minutesAccuracyLow;

    public Trip() {
    }

    public Trip(Trip t) {
        this.trip_id = t.trip_id;
        this.user_id = t.user_id;
        this.filesize = t.filesize;
        this.uploaded = t.uploaded;
        this.startTime = t.startTime;
        this.endTime = t.endTime;
        this.startLoc = t.startLoc;
        this.endLoc = t.endLoc;
        this.no_of_lines = t.no_of_lines;
        this.distanceInKM = t.distanceInKM;
        this.duration = t.duration;
        this.device = t.device;
        this.userRating = t.userRating;
        this.minutesWasted = t.minutesWasted;
        this.minutesAccuracyLow = t.minutesAccuracyLow;
    }  // copy constructor



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

    public int getUserRating() {
        return userRating;
    }

    public void setUserRating(int userRating) {
        this.userRating = userRating;
    }

    public float getDistanceInKM() {
        return distanceInKM;
    }

    public void setDistanceInKM(float distanceInKM) {
        this.distanceInKM = distanceInKM;
    }

    public String getAxis() {
        return axis;
    }

    public void setAxis(String axis) {
        this.axis = axis;
    }

    public float getThreshold() {
        return threshold;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public int getPotholeCount() {
        return potholeCount;
    }

    public void setPotholeCount(int potholeCount) {
        this.potholeCount = potholeCount;
    }

    public long getMinutesWasted() {
        return minutesWasted;
    }

    public void setMinutesWasted(long minutesWasted) {
        this.minutesWasted = minutesWasted;
    }

    public long getMinutesAccuracyLow() {
        return minutesAccuracyLow;
    }

    public void setMinutesAccuracyLow(long minutesAccuracyLow) {
        this.minutesAccuracyLow = minutesAccuracyLow;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.trip_id);
        dest.writeString(this.user_id);
        dest.writeLong(this.filesize);
        dest.writeByte(this.uploaded ? (byte) 1 : (byte) 0);
        dest.writeString(this.startTime);
        dest.writeString(this.endTime);
        dest.writeParcelable(this.startLoc, flags);
        dest.writeParcelable(this.endLoc, flags);
        dest.writeInt(this.no_of_lines);
        dest.writeFloat(this.distanceInKM);
        dest.writeLong(this.duration);
        dest.writeString(this.device);
        dest.writeInt(this.userRating);
        dest.writeString(this.axis);
        dest.writeFloat(this.threshold);
        dest.writeInt(this.potholeCount);
        dest.writeLong(this.minutesWasted);
        dest.writeLong(this.minutesAccuracyLow);
    }

    protected Trip(Parcel in) {
        this.trip_id = in.readString();
        this.user_id = in.readString();
        this.filesize = in.readLong();
        this.uploaded = in.readByte() != 0;
        this.startTime = in.readString();
        this.endTime = in.readString();
        this.startLoc = in.readParcelable(MyLocation.class.getClassLoader());
        this.endLoc = in.readParcelable(MyLocation.class.getClassLoader());
        this.no_of_lines = in.readInt();
        this.distanceInKM = in.readFloat();
        this.duration = in.readLong();
        this.device = in.readString();
        this.userRating = in.readInt();
        this.axis = in.readString();
        this.threshold = in.readFloat();
        this.potholeCount = in.readInt();
        this.minutesWasted = in.readLong();
        this.minutesAccuracyLow = in.readLong();
    }

    public static final Creator<Trip> CREATOR = new Creator<Trip>() {
        @Override
        public Trip createFromParcel(Parcel source) {
            return new Trip(source);
        }

        @Override
        public Trip[] newArray(int size) {
            return new Trip[size];
        }
    };
}
