package org.reapbenefit.gautam.intern.potholedetectorbeta.Core;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase.TripRepository;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;

import java.util.List;

public class TripViewModel extends AndroidViewModel{

    private TripRepository tripRepository;
    private LiveData<List<Trip>> allTrips;

    public TripViewModel(@NonNull Application application) {
        super(application);
        tripRepository = new TripRepository(application);
        allTrips = tripRepository.getLiveDataTrips();
    }

    //getter method to separate the implementation from the UI
    public LiveData<List<Trip>> getAllTrips() {
        return allTrips;
    }

    public void insert(Trip trip) {
        tripRepository.insertTrip(trip);
    }
}
