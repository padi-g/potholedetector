package org.reapbenefit.gautam.intern.potholedetectorbeta;

public class UniquePotholeDataLambda {
    private float Lat;
    private float Lng;
    private int Count;
    private int Classification;
    private String PotholeID;

    public float getLat() {
        return Lat;
    }

    public void setLat(float lat) {
        Lat = lat;
    }

    public float getLng() {
        return Lng;
    }

    public void setLng(float lng) {
        Lng = lng;
    }

    public int getCount() {
        return Count;
    }

    public void setCount(int count) {
        Count = count;
    }

    public int getClassification() {
        return Classification;
    }

    public void setClassification(int classification) {
        Classification = classification;
    }

    public String getPotholeID() {
        return PotholeID;
    }

    public void setPotholeID(String potholeID) {
        PotholeID = potholeID;
    }
}
