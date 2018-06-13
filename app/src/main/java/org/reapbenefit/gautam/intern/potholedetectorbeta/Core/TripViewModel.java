package org.reapbenefit.gautam.intern.potholedetectorbeta.Core;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase.LocalTripEntity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase.TripRepository;

import java.util.List;

public class TripViewModel extends AndroidViewModel {

    private TripRepository tripRepository;
    private LiveData<List<LocalTripEntity>> allTrips;
    private LiveData<List<LocalTripEntity>> offlineTrips;
    private String trip_id;
    private int probablePotholeCount;
    private int definitePotholeCount;
    private String startTime;
    private long duration;
    private float distanceInKm;
    private long filesize;
    private String user_id;
    private LiveData<LocalTripEntity> highestPotholeTrip;

    public TripViewModel(@NonNull Application application) {
        super(application);
        tripRepository = new TripRepository(application);
        allTrips = tripRepository.getLiveDataTrips();
        trip_id = tripRepository.getTrip_id();
        probablePotholeCount = tripRepository.getProbablePotholeCount();
        definitePotholeCount = tripRepository.getDefinitePotholeCount();
        startTime = tripRepository.getStartTime();
        duration = tripRepository.getDuration();
        distanceInKm = tripRepository.getDistanceInKm();
        filesize = tripRepository.getFilesize();
        user_id = tripRepository.getUser_id();
        offlineTrips = tripRepository.getAllOfflineTrips();
        highestPotholeTrip = tripRepository.getHighestPotholeTrip();
    }

    public LiveData<List<LocalTripEntity>> getAllTrips() {
        return allTrips;
    }

    public void insert(LocalTripEntity trip) {
        tripRepository.insertTrip(trip);
    }

    public String getTrip_id() {
        return trip_id;
    }

    public float getDistanceInKm() {
        return distanceInKm;
    }

    public int getProbablePotholeCount() {
        return probablePotholeCount;
    }

    public int getDefinitePotholeCount() {
        return definitePotholeCount;
    }

    public long getDuration() {
        return duration;
    }

    public long getFilesize() {
        return filesize;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getUser_id() {
        return user_id;
    }

    public void deleteAll() {
        tripRepository.deleteAll();
    }

    public LiveData<List<LocalTripEntity>> getOfflineTrips() {
        return offlineTrips;
    }

    public LiveData<LocalTripEntity> getHighestPotholeTrip(FragmentActivity activity, Observer<List<LocalTripEntity>> observer) {
        return highestPotholeTrip;
    }
}
