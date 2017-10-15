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

    public GoogleApiClient mGoogleApiClient;
    private GoogleApiHelper googleApiHelper;
    private static ApplicationClass singleton;
    public Context mContext;
    protected static String TAG = "Application";
    public static boolean tripInProgress, tripEnded = false;  // ended is to make sure that only one trip per instance

    private Trip trip;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        singleton = this;

        Log.i(TAG, "App started");

        // Get permissions

        trip = new Trip();

        tripInProgress = false;
        googleApiHelper = new GoogleApiHelper(singleton);
        mGoogleApiClient = googleApiHelper.getGoogleApiClient();
    }

    public static synchronized ApplicationClass getInstance() {
        return singleton;
    }

    public GoogleApiHelper getGoogleApiHelperInstance() {
        return this.googleApiHelper;
    }

    public static GoogleApiHelper getGoogleApiHelper() {
        return getInstance().getGoogleApiHelperInstance();
    }

    public void setTrip(Trip incoming) {
        trip = incoming;
    }

    public Trip getTrip() {
        return trip;
    }
}

