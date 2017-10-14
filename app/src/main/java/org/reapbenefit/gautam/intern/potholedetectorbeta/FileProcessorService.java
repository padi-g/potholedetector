package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.gson.Gson;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MainActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MapsActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    OutputStream outputStream;
    InputStream inputStream;

    private static final String TAG = "File_Processor";

    int latIndex, lngIndex, accuracyIndex;
    float distance_travelled = 0;
    float[] results = new float[]{0.0f, 0.0f, 0.0f};

    String c_gps0 = "", c_gps1 = "",temp_gps0, temp_gps1;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public FileProcessorService() {

    }

    @Override
    public void onCreate(){
        super.onCreate();

        Log.d(TAG, "onCreate");

        //Get the csv file

        trip = new Trip();
        trip.setTrip_id(fetchTripID());

        String path = "/logs/";
        //File inputfile = new File(getApplicationContext().getFilesDir() + path + trip.getTrip_id() + ".csv");
        File inputfile = new File(getExternalFilesDir(null), "mum1.csv");

        File temp = new File(getExternalFilesDir(null), "analysis/");
        temp.mkdir();
        File outputfile = new File(temp.getPath(), trip.getTrip_id()+ ".csv");
        Log.d(TAG, inputfile.toString());
        Log.d(TAG, outputfile.toString());

        try {
            inputStream = new FileInputStream(inputfile);
            outputStream = new FileOutputStream(outputfile);

            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(isr);
            String line;
            line = bufferedReader.readLine();
            String tokens[] = line.split(",");
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].contentEquals(" latitude")) {
                    latIndex = i;
                    lngIndex = latIndex + 1;
                    accuracyIndex = lngIndex + 2;
                }
            }

            Log.d(TAG,latIndex + " " + accuracyIndex + " " + lngIndex);

            //////////////////   FOR first line
            line = bufferedReader.readLine();
            String line1[] = line.split(",");

            c_gps0 = line1[latIndex];
            c_gps1 = line1[lngIndex];
            outputStream.write((c_gps0 + "," + c_gps1 + "\n").replace(" ", "").getBytes());
            //////////////////   FOR first line

            while ((line = bufferedReader.readLine()) != null) {

                String values[] = line.split(",");
                temp_gps0 = values[latIndex];
                temp_gps1 = values[lngIndex];

                if (!(temp_gps0.equals(c_gps0) && temp_gps1.equals(c_gps1))) {
                    Location.distanceBetween(Double.valueOf(c_gps0), Double.valueOf(c_gps1), Double.valueOf(temp_gps0), Double.valueOf(temp_gps1), results);
                    distance_travelled += results[0];
                    Log.d(TAG, "{lat: " + temp_gps0 + ", lng: " + temp_gps1 + "},");
                    outputStream.write((temp_gps0 + "," + temp_gps1 + "\n").replace(" ", "").getBytes());
                }
                c_gps0 = temp_gps0;
                c_gps1 = temp_gps1;

            }

            Log.d(TAG, "Distance travelled " +  String.valueOf(distance_travelled));
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            System.out.println("Exception_raised " + e.toString());
            Log.i("Exception_raised ", e.toString());
        }

        this.onDestroy();

        //TODO Change logs/ to /logs/ everywhere

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Intent notificationIntent = new Intent(this, MapsActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Road Quality Audit")
                .setContentText("Data has been processed")
                .setSubText("Press to open on map")
                .setContentIntent(pendingIntent).build();

        startForeground(1335, notification);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    private String fetchTripID(){
        String name = "null";

        String path = "tripsIDs.csv";
        File temp = new File(getExternalFilesDir(null), path);
        try {
            InputStream inputStream = new FileInputStream(temp);
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(isr);
            name = bufferedReader.readLine();
        }catch (FileNotFoundException e){

        }catch (IOException er){

        }

        return name;
    }
}

