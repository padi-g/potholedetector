package org.reapbenefit.gautam.intern.potholedetectorbeta;

public class UserData {
    private float TotalDistance;
    private int TotalTime;
    private int Probable;
    private int Improbable;
    private int TotalTrips;
    private String UserID;

    public float getTotalDistance() {
        return TotalDistance;
    }

    public void setTotalDistance(float totalDistance) {
        TotalDistance = totalDistance;
    }

    public int getTotalTime() {
        return TotalTime;
    }

    public void setTotalTime(int totalTime) {
        TotalTime = totalTime;
    }

    public int getProbable() {
        return Probable;
    }

    public void setProbable(int probable) {
        Probable = probable;
    }

    public int getImprobable() {
        return Improbable;
    }

    public void setImprobable(int improbable) {
        Improbable = improbable;
    }

    public int getTotalTrips() {
        return TotalTrips;
    }

    public void setTotalTrips(int totalTrips) {
        TotalTrips = totalTrips;
    }

    public String getUserID() {
        return UserID;
    }

    public void setUserID(String userID) {
        UserID = userID;
    }
}