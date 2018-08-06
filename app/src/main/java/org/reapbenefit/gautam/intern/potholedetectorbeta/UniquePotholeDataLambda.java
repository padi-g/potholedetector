package org.reapbenefit.gautam.intern.potholedetectorbeta;

public class UniquePotholeDataLambda {
    private float Lat;
    private float Lng;
    private int Hits;
    private int Classification;
    private String PotholeID;

    private String GeoHash;

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

    public int getHits() {
        return Hits;
    }

    public void setHits(int hits) {
        Hits = hits;
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


    public String getGeoHash() {
        return GeoHash;
    }

    public void setGeoHash(String geoHash) {
        GeoHash = geoHash;
    }
}
