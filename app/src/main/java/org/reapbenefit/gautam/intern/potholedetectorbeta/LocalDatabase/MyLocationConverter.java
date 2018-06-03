package org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase;

import android.arch.persistence.room.TypeConverter;

import org.reapbenefit.gautam.intern.potholedetectorbeta.MyLocation;

public class MyLocationConverter {
    @TypeConverter
    public static String myLocationToString(MyLocation myLocation) {
        double latitude = myLocation.getLatitude();
        double longitude = myLocation.getLongitude();
        float accuracy = myLocation.getAccuracy();
        return latitude + ":" + longitude + ":" + accuracy;
    }
    @TypeConverter
    public static MyLocation StringToMyLocation(String string) {
        int startIndex = string.indexOf(':');
        double latitude = Double.parseDouble(string.substring(0, startIndex));
        int endIndex = string.lastIndexOf(':');
        double longitude = Double.parseDouble(string.substring(startIndex + 1, endIndex));
        float accuracy = Float.parseFloat(string.substring(endIndex + 1));
        MyLocation myLocation = new MyLocation(latitude, longitude, accuracy);
        return myLocation;
    }
}
