package org.reapbenefit.gautam.intern.potholedetectorbeta;

public class TripDataLambda {
    private String TripID;
    private String UserID;
    private String StartTime;
    private String EndTime;
    private long Filesize;
    private int Uploaded;
    private float StartLat;
    private float StartLong;
    private float StartAcc;
    private float EndLat;
    private float EndLong;
    private float EndAcc;
    private int NoOfLines;
    private float DistanceInKM;
    private long Duration;
    private String Device;
    private int UserRating;
    private String Axis;
    private float Threshold;
    private long MinutesWasted;
    private long MinutesAccuracyLow;
    private int DefinitePotholeCount;
    private int ProbablePotholeCount;
    private boolean UpdateUploadedFlag;

    public String getTripID() {
        return TripID;
    }

    public void setTripID(String tripID) {
        TripID = tripID;
    }

    public String getUserID() {
        return UserID;
    }

    public void setUserID(String userID) {
        UserID = userID;
    }

    public String getStartTime() {
        return StartTime;
    }

    public void setStartTime(String startTime) {
        StartTime = startTime;
    }

    public String getEndTime() {
        return EndTime;
    }

    public void setEndTime(String endTime) {
        EndTime = endTime;
    }

    public long getFilesize() {
        return Filesize;
    }

    public void setFilesize(long filesize) {
        Filesize = filesize;
    }

    public int getUploaded() {
        return Uploaded;
    }

    public void setUploaded(int uploaded) {
        Uploaded = uploaded;
    }

    public float getStartLat() {
        return StartLat;
    }

    public void setStartLat(float startLat) {
        StartLat = startLat;
    }

    public float getStartLong() {
        return StartLong;
    }

    public void setStartLong(float startLong) {
        StartLong = startLong;
    }

    public float getStartAcc() {
        return StartAcc;
    }

    public void setStartAcc(float startAcc) {
        StartAcc = startAcc;
    }

    public float getEndLat() {
        return EndLat;
    }

    public void setEndLat(float endLat) {
        EndLat = endLat;
    }

    public float getEndLong() {
        return EndLong;
    }

    public void setEndLong(float endLong) {
        EndLong = endLong;
    }

    public float getEndAcc() {
        return EndAcc;
    }

    public void setEndAcc(float endAcc) {
        EndAcc = endAcc;
    }

    public int getNoOfLines() {
        return NoOfLines;
    }

    public void setNoOfLines(int noOfLines) {
        NoOfLines = noOfLines;
    }

    public float getDistanceInKM() {
        return DistanceInKM;
    }

    public void setDistanceInKM(float distanceInKM) {
        DistanceInKM = distanceInKM;
    }

    public long getDuration() {
        return Duration;
    }

    public void setDuration(long duration) {
        Duration = duration;
    }

    public String getDevice() {
        return Device;
    }

    public void setDevice(String device) {
        Device = device;
    }

    public int getUserRating() {
        return UserRating;
    }

    public void setUserRating(int userRating) {
        UserRating = userRating;
    }

    public String getAxis() {
        return Axis;
    }

    public void setAxis(String axis) {
        Axis = axis;
    }

    public float getThreshold() {
        return Threshold;
    }

    public void setThreshold(float threshold) {
        Threshold = threshold;
    }

    public long getMinutesWasted() {
        return MinutesWasted;
    }

    public void setMinutesWasted(long minutesWasted) {
        MinutesWasted = minutesWasted;
    }

    public long getMinutesAccuracyLow() {
        return MinutesAccuracyLow;
    }

    public void setMinutesAccuracyLow(long minutesAccuracyLow) {
        MinutesAccuracyLow = minutesAccuracyLow;
    }

    public int getDefinitePotholeCount() {
        return DefinitePotholeCount;
    }

    public void setDefinitePotholeCount(int definitePotholeCount) {
        DefinitePotholeCount = definitePotholeCount;
    }

    public int getProbablePotholeCount() {
        return ProbablePotholeCount;
    }

    public void setProbablePotholeCount(int probablePotholeCount) {
        ProbablePotholeCount = probablePotholeCount;
    }

    public boolean getUpdateUploadedFlag() {
        return UpdateUploadedFlag;
    }

    public void setUpdateUploadedFlag(boolean updateUploadedFlag) {
        UpdateUploadedFlag = updateUploadedFlag;
    }
}
