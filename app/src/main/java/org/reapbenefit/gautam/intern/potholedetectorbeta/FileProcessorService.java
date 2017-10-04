package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Created by gautam on 29/06/17.
 *
 * This Service is run as soon as the trip is stopped.
 * This service will geocode the locations and modify the file to contain all the necessary information before uploading to firebase
 * Also trims the file to contain only important
 */

public class FileProcessorService extends Service {

    Trip trip;
    String filename = "trips.json";
    File file;
    OutputStream out;

    int latIndex, lngIndex, accuracyIndex;
    float distance_travelled = 0;
    float[] results;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public FileProcessorService() {
        trip = new Trip(ApplicationClass.getTrip());
        // setup file to operate on it
    }

    @Override
    public void onCreate(){
        Log.d("File Processor ", "onCreate");



//Get the text file
        File file = new File(getApplicationContext().getFilesDir(), "logs/"+trip.getTrip_id()+".csv");

        double c_gps[] = {0.0, 0.0};
        double temp_gps[] = {0.0, 0.0};
        try {
            Log.i("File Processor", trip.getTrip_id());
            BufferedReader in = new BufferedReader(new FileReader(file));
            String line = "";

            line = in.readLine();
            String tokens[] = line.split(",");
            for(int i=0; i<tokens.length; i++){
                if(tokens[i].contentEquals(" latitude")) {
                    latIndex = i;
                    lngIndex = latIndex + 1;
                    accuracyIndex = lngIndex + 2;
                }
            }

            //line.contains("null, null")

            while (!(line = in.readLine()).contentEquals(null)) {
                if(line.contains("null, null"))
                    continue;
                String values[] = line.split(",");
                for(int i=0; i<values.length; i++){
                    temp_gps[0] = Float.valueOf(values[latIndex]);
                    temp_gps[1] = Float.valueOf(values[lngIndex]);
                    if(Arrays.equals(temp_gps, c_gps)){
                        Location.distanceBetween(c_gps[0], c_gps[1], temp_gps[0], temp_gps[1], results);
                        distance_travelled += results[0];
                    }
                    c_gps = temp_gps;
                    System.out.println("{lat: "+ temp_gps[0]+ ", lng: "+ temp_gps[1]+ "},");

                }

            }
        }catch (Exception f){}


        Gson gson = new Gson();
        String json = gson.toJson(trip);
        Log.d("File Processor ", json);
/*
        File file = new File(getApplicationContext().getFilesDir(), filename);
        try {
            out = new FileOutputStream(file, true);
            out.write(json.getBytes());

        } catch (IOException e) {
            Log.d("File Processor ", "File setup failed: " + e.toString());
        }
//        Trip t = new Trip(gson.fromJson(json, Trip.class));
//        Log.d("File Processor ", t.getUser_id());
*/
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


// TODO 1 : Convert trip from appclass to json and store in a file
// TODO 2 : Build json schema
// TODO 2 : Extract device info and
