package org.reapbenefit.gautam.intern.potholedetectorbeta;

import java.io.Serializable;

public class UserPothole implements Serializable {
    private String UserID;
    private float PotLat;
    private float PotLong;
    private int classification;

    public float getPotLong() {
        return PotLong;
    }

    public void setPotLong(float potLong) {
        PotLong = potLong;
    }

    public float getPotLat() {
        return PotLat;
    }

    public void setPotLat(float potLat) {
        PotLat = potLat;
    }

    public String getUserID() {
        return UserID;
    }

    public void setUserID(String userID) {
        UserID = userID;
    }

    public int getClassification() {
        return classification;
    }

    public void setClassification(int classification) {
        this.classification = classification;
    }
}
