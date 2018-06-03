package org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;

import java.util.List;

public class TripRepository {
    private LocalTripTableDao dao;
    private LiveData<List<LocalTripEntity>> liveDataTrips;

    public TripRepository(Application app) {
        LocalTripDatabase localTripDatabase = LocalTripDatabase.getInstance(app);
        dao = localTripDatabase.localTripTableDao();
        liveDataTrips = dao.getAllTrips();
    }

    //Executed on a separate thread by Room automatically
    //Observer will be notified when data is changed
    public LiveData<List<LocalTripEntity>> getLiveDataTrips() {
        return liveDataTrips;
    }

    //Must be coded to run on a separate thread.
    public void insertTrip(LocalTripEntity trip) {
        new InsertAsyncTask(dao).execute(trip);
    }

    private static class InsertAsyncTask extends AsyncTask<LocalTripEntity, Void, Void>{

        private LocalTripTableDao asyncTaskDao;

        public InsertAsyncTask(LocalTripTableDao dao) {
            asyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(LocalTripEntity... trips) {
            asyncTaskDao.insertTrip(trips[0]);
            return null;
        }
    }
}
