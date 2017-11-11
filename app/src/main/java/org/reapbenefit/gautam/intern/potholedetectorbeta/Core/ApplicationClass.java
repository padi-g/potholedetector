package org.reapbenefit.gautam.intern.potholedetectorbeta.Core;

import android.content.Context;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;


/**
 * Created by gautam on 03/06/17.
 */

public class ApplicationClass extends MultiDexApplication {

    private GoogleApiHelper googleApiHelper;
    private static ApplicationClass singleton;
    protected static String TAG = "Application";
    private boolean tripInProgress, tripEnded = false;  // ended is to make sure that only one trip per instance

    private Trip trip;

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;

        Log.i(TAG, "App started");

        // Get permissions

        trip = new Trip();

        tripInProgress = false;
        googleApiHelper = new GoogleApiHelper(singleton);
    }

    public static synchronized ApplicationClass getInstance() {
        return singleton;
    }

    public GoogleApiHelper getGoogleApiHelper() {
        return this.googleApiHelper;
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
}

