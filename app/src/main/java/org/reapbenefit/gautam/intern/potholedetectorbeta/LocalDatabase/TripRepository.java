package org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TripRepository {
    private LocalTripTableDao dao;
    private LiveData<List<LocalTripEntity>> liveDataTrips;
    private String trip_id;
    private int potholeCount;
    private String startTime;
    private long duration;
    private float distanceInKm;
    private long filesize;
    private String user_id;

    public TripRepository(Application app) {
        LocalTripDatabase localTripDatabase = LocalTripDatabase.getInstance(app);
        dao = localTripDatabase.localTripTableDao();
        liveDataTrips = dao.getAllTrips();
        trip_id = getTrip_id();
        potholeCount = getPotholeCount();
        startTime = getStartTime();
        duration = getDuration();
        distanceInKm = getDistanceInKm();
        filesize = getFilesize();
        user_id = getUser_id();
    }

    //Executed on a separate thread by Room automatically
    //Observer will be notified when data is changed
    public LiveData<List<LocalTripEntity>> getLiveDataTrips() {
        return liveDataTrips;
    }

    public String getTrip_id() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        final String[] result = {null};
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                result[0] = dao.getTrip_id();
            }
        });
        return result[0];
    }

    public float getDistanceInKm() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        final float[] result = {0.0f};
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                result[0] = dao.getDistanceINKM();
            }
        });
        return result[0];
    }

    public int getPotholeCount() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        final int[] result = new int[1];
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                result[0] = dao.getPotholeCount();
            }
        });
        return result[0];
    }

    public long getDuration() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        final long[] result = new long[1];
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                result[0] = dao.getDuration();
            }
        });
        return result[0];
    }

    public long getFilesize() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        final long[] result = new long[1];
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                result[0] = dao.getFileSize();
            }
        });
        return result[0];
    }

    public String getStartTime() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        final String[] result = new String[1];
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                result[0] = dao.getStartTime();
            }
        });
        return result[0];
    }

    public String getUser_id() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        final String[] result = new String[1];
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                result[0] = dao.getUser_id();
            }
        });
        return result[0];
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
