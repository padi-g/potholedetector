package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MapsActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import static java.lang.Math.abs;


/**
 * Created by gautam on 29/06/17.
 *
 * This Service is run as soon as the trip is stopped.
 * This service will geocode the locations and modify the file to contain all the necessary information before uploading to firebase
 * Also trims the file to contain only important
 */

public class FileProcessorService extends Service {

    private OutputStream outputStream;
    private InputStream inputStream;

    private SummaryStatistics stats = new SummaryStatistics();

    private static final String TAG = "File_Processor";

    private int latIndex, lngIndex, accuracyIndex;
    private float distance_travelled = 0;
    private float[] results = new float[]{0.0f, 0.0f, 0.0f};

    private String c_gps0 = "", c_gps1 = "",temp_gps0, temp_gps1;
    private double mean, std;

    private FirebaseAuth mAuth;
    private DatabaseReference db;

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

        // Log.d(TAG, "onCreate");

        //Get the csv file

        db = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        String path = "/logs/";
        File inputfile = new File(getApplicationContext().getFilesDir() + path + fetchTripID() + ".csv");
        //File inputfile = new File(getExternalFilesDir(null), "datafile.csv");

        File temp = new File(getApplicationContext().getFilesDir(), "analysis/");
        temp.mkdir();
        File outputfile = new File(temp.getPath() + fetchTripID() + ".csv");
        //File outputfile = new File(temp.getPath(), "test.csv");

        // Log.d(TAG, inputfile.toString());
        // Log.d(TAG, outputfile.toString());

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

            // Log.d(TAG,latIndex + " " + accuracyIndex + " " + lngIndex);

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
                    // Log.d(TAG, "{lat: " + temp_gps0 + ", lng: " + temp_gps1 + "},");
                    outputStream.write((temp_gps0 + "," + temp_gps1 + "\n").replace(" ", "").getBytes());
                }
                c_gps0 = temp_gps0;
                c_gps1 = temp_gps1;

                stats.addValue(abs(Double.parseDouble(values[2].trim())));

            }

            // Log.d(TAG, "Distance travelled " +  String.valueOf(distance_travelled));
            mean = stats.getMean();
            std = stats.getStandardDeviation();
            // Log.d(TAG, "Mean " + mean);
            // Log.d(TAG, "std " + std);
            setDistance_travelled(distance_travelled);
            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            System.out.println("Exception_raised " + e.toString());
            // Log.i("Exception_raised ", e.toString());
        }

        this.onDestroy();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Road Quality Audit")
                        .setContentText("Data has been processed")
                        .setSubText("Click to open map");
        Intent resultIntent = new Intent(this, MapsActivity.class);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);
        mBuilder.setAutoCancel(true);

        int mNotificationId = 001;
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

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

    private void setDistance_travelled(float a){
        a = a/1000;
        db = db.child(mAuth.getCurrentUser().getUid()).child(fetchTripID()).child("distanceInKM");
        db.setValue(a);
        ApplicationClass.getInstance().getTrip().setDistanceInKM(a);
    }

}