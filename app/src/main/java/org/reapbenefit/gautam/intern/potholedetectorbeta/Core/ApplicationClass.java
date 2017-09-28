package org.reapbenefit.gautam.intern.potholedetectorbeta.Core;

import android.app.Application;
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
    private static ApplicationClass mInstance;
    public Context mContext;
    protected static String TAG = "Application";
    public static boolean tripInProgress, tripEnded = false;  // ended is to make sure that only one trip per instance
    private static Trip trip;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        mInstance = this;


        Log.i(TAG, "App started");

        // Get permissions

        tripInProgress = false;
        googleApiHelper = new GoogleApiHelper(mInstance);
        mGoogleApiClient = googleApiHelper.getGoogleApiClient();
    }

    public static synchronized ApplicationClass getInstance() {
        return mInstance;
    }

    public GoogleApiHelper getGoogleApiHelperInstance() {
        return this.googleApiHelper;
    }

    public static GoogleApiHelper getGoogleApiHelper() {
        return getInstance().getGoogleApiHelperInstance();
    }

    public static void setTrip(Trip incoming) {
        trip = new Trip(incoming);
    }

    public static Trip getTrip() {
        return trip;
    }
}

/**
 * Code to check whether client is connected or not, in other places
 * <p>
 * if(App.getGoogleApiHelper().isConnected())
 * {
 * //Get google api client
 * LoggerTask(App.getGoogleApiHelper().getGoogleApiClient());
 * }
 */
