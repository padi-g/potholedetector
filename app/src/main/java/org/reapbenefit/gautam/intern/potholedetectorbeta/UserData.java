package org.reapbenefit.gautam.intern.potholedetectorbeta;

public class UserData {
    private float TotalDistance;
    private int TotalTime;
    private int Probable;
    private int Improbable;
    private int TotalTrips;
    private String UserID;

    public String getUserID() {
        return UserID;
    }

    public int getTotalTrips() {
        return TotalTrips;
    }

    public int getTotalTime() {
        return TotalTime;
    }

    public int getProbable() {
        return Probable;
    }

    public int getImprobable() {
        return Improbable;
    }

    public float getTotalDistance() {
        return TotalDistance;
    }

    public void setUserID(String userID) {
        UserID = userID;
    }
}