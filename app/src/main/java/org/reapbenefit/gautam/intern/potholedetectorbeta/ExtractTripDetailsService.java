package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by gautam on 29/06/17.
 *
 * This Service is run as soon as the trip is stopped.
 * This service will geocode the locations and modify the file to contain all the necessary information before uploading to firebase
 * Also trims the file to contain only important
 */

public class ExtractTripDetailsService extends Service {

    Trip trip;
    File file;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public ExtractTripDetailsService(Uri fileuri, Trip trip) {
        this.trip = trip;
        // setup file to operate on it
    }

    @Override
    public void onCreate() {

        Thread thread = new Thread() {
            @Override
            public void run() {
                Geocoder geocoder= new Geocoder(getBaseContext(), Locale.ENGLISH);

                try {

                    //Place your latitude and longitude
                    List<Address> addresses = geocoder.getFromLocation(trip.getStartLoc().getLatitude(),trip.getStartLoc().getLongitude(), 1);

                    if(addresses != null) {

                        Address fetchedAddress = addresses.get(0);
                        StringBuilder strAddress = new StringBuilder();

                        for(int i=0; i<fetchedAddress.getMaxAddressLineIndex(); i++) {
                            strAddress.append(fetchedAddress.getAddressLine(i)).append("\n");
                        }
                        Log.i("ExtractTripDetail", strAddress.toString());
                    }

                    else
                        Log.i("ExtractTripDetail", "Could not location");
                }
                catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    Log.i("ExtractTripDetail", "Could not location exception");
                }
            }

        };
        thread.start();

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
}
