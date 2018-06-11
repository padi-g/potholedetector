package org.reapbenefit.gautam.intern.potholedetectorbeta.Core;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase.LocalTripEntity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase.TripRepository;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;

import java.util.List;

public class TripViewModel extends AndroidViewModel {

    private TripRepository tripRepository;
    private LiveData<List<LocalTripEntity>> allTrips;
    private LiveData<List<LocalTripEntity>> offlineTrips;
    private String trip_id;
    private int potholeCount;
    private String startTime;
    private long duration;
    private float distanceInKm;
    private long filesize;
    private String user_id;
    private LiveData<List<LocalTripEntity>> highestPotholeTrips;

    public TripViewModel(@NonNull Application application) {
        super(application);
        tripRepository = new TripRepository(application);
        allTrips = tripRepository.getLiveDataTrips();
        trip_id = tripRepository.getTrip_id();
        potholeCount = tripRepository.getPotholeCount();
        startTime = tripRepository.getStartTime();
        duration = tripRepository.getDuration();
        distanceInKm = tripRepository.getDistanceInKm();
        filesize = tripRepository.getFilesize();
        user_id = tripRepository.getUser_id();
        offlineTrips = tripRepository.getAllOfflineTrips();
        highestPotholeTrips = tripRepository.getHighestPotholeTrips();
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

    public int getPotholeCount() {
        return potholeCount;
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

    public LiveData<List<LocalTripEntity>> getHighestPotholeTrips() {
        return highestPotholeTrips;
    }
}
