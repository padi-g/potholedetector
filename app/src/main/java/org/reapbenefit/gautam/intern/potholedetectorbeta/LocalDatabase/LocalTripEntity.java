package org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import org.reapbenefit.gautam.intern.potholedetectorbeta.MyLocation;
@Entity (tableName = "LocalTripTable", primaryKeys = {"trip_id", "user_id"})
public class LocalTripEntity {
    @NonNull
    public String trip_id;
    @NonNull
    public String user_id;
    public long filesize;
    public boolean uploaded;
    public String startTime, endTime;
    public String startLoc, endLoc;
    public int no_of_lines;
    public float distanceInKM;
    public long duration;
    public String device;
    public int userRating;
    public String axis;
    public float threshold;
    public int potholeCount;
    public long minutesWasted;
    public long minutesAccuracyLow;
}
