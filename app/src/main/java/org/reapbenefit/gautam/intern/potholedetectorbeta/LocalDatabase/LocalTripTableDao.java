package org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface LocalTripTableDao {
    @Query("SELECT * FROM localTripTable")
    LiveData<List<LocalTripEntity>> getAllTrips();

    @Query("SELECT trip_id FROM localtriptable")
    String getTrip_id();

    @Query("SELECT potholeCount FROM localtriptable")
    int getPotholeCount();

    @Query("SELECT startTime FROM localtriptable")
    String getStartTime();

    @Query("SELECT duration FROM localtriptable")
    long getDuration();

    @Query("SELECT distanceInKM FROM localtriptable")
    float getDistanceINKM();

    @Query("SELECT filesize FROM localtriptable")
    long getFileSize();

    @Query("SELECT user_id FROM localtriptable")
    String getUser_id();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTrip(LocalTripEntity trip);

    @Query("DELETE FROM localtriptable")
    void deleteAll();

    @Update
    void setUploaded(LocalTripEntity localTripEntity);

    @Query("SELECT * FROM localtriptable WHERE uploaded='FALSE'")
    LiveData<List<LocalTripEntity>> getAllOfflineTrips();
}
