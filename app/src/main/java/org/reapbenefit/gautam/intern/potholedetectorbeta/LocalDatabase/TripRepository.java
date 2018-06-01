package org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase;

import android.app.Application;
import android.arch.lifecycle.LiveData;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;

import java.util.List;

public class TripRepository {
    private LocalTripTableDao dao;
    private LiveData<List<Trip>> liveDataTrips;

    public TripRepository(Application app) {
        LocalTripDatabase localTripDatabase = LocalTripDatabase.getInstance(app);
        dao = localTripDatabase.localTripTableDao();
        liveDataTrips = dao.getAllTrips();
    }

    public LiveData<List<Trip>> getLiveDataTrips() {
        return liveDataTrips;
    }

    
}
