package org.reapbenefit.gautam.intern.potholedetectorbeta.Core;

import android.location.Location;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;


/**
 * Created by gautam on 03/06/17.
 */

public class ApplicationClass extends MultiDexApplication {

    private static ApplicationClass singleton;
    protected static String TAG = "Application";
    private boolean tripInProgress, tripEnded = false;  // ended is to make sure that only one trip per instance
    private Location mCurrentLocation;

    private Trip trip;

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;

        Log.i(TAG, "App started");

        // Get permissions

        trip = new Trip();

        tripInProgress = false;
    }

    public static synchronized ApplicationClass getInstance() {
        return singleton;
    }

    public void setTrip(Trip incoming) {
        trip = incoming;
    }

    public Trip getTrip() {
        return trip;
    }

    public boolean isTripInProgress() {
        return tripInProgress;
    }

    public void setTripInProgress(boolean tripInProgress) {
        this.tripInProgress = tripInProgress;
    }

    public boolean isTripEnded() {
        return tripEnded;
    }

    public void setTripEnded(boolean tripEnded) {
        this.tripEnded = tripEnded;
    }

    public Location getCurrentLocation() {
        return mCurrentLocation;
    }

    public void setCurrentLocation(Location mCurrentLocation) {
        this.mCurrentLocation = mCurrentLocation;
    }
}

