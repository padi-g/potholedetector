package org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase;

import android.arch.persistence.room.TypeConverter;

import org.reapbenefit.gautam.intern.potholedetectorbeta.MyLocation;

public class MyLocationConverter {
    @TypeConverter
    public static String myLocationToString(MyLocation myLocation) {
        double latitude = myLocation.getLatitude();
        double longitude = myLocation.getLongitude();
        double accuracy = myLocation.getAccuracy();
        return latitude + ":" + longitude + ":" + accuracy;
    }
}
