package org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

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

    //Executed on a separate thread by Room automatically
    //Observer will be notified when data is changed
    public LiveData<List<Trip>> getLiveDataTrips() {
        return liveDataTrips;
    }

    //Must be coded to run on a separate thread.
    public void insertTrip(Trip trip) {
        new InsertAsyncTask(dao).execute(trip);
    }

    private static class InsertAsyncTask extends AsyncTask<Trip, Void, Void>{

        private LocalTripTableDao asyncTaskDao;

        public InsertAsyncTask(LocalTripTableDao dao) {
            asyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(Trip... trips) {
            asyncTaskDao.insertTrip(trips[0]);
            return null;
        }
    }
}
