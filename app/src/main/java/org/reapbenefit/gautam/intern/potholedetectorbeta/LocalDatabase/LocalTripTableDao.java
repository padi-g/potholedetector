package org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;

import java.util.List;

@Dao
public interface LocalTripTableDao {
    @Query("SELECT * FROM localTripTable")
    LiveData<List<Trip>> getAllTrips();

    @Insert
    void insertTrip(Trip trip);
}
