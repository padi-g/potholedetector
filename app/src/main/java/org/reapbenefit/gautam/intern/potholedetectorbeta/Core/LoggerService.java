package org.reapbenefit.gautam.intern.potholedetectorbeta.Core;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MainActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MapsActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.MyLocation;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.reapbenefit.gautam.intern.potholedetectorbeta.Core.GoogleApiHelper.mGoogleApiClient;

/**
 * Created by gautam on 04/06/17.
 * This service does all the data logging
 */

public class LoggerService extends Service implements SensorEventListener, LocationListener {

    protected String TAG = "Logger_Service";
    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mGyroscope, mProximity;
    private static final int ACCURACY_REQUIRED = 25;

    private String AccXvalue, AccYvalue, AccZvalue, GyroXvalue, GyroYvalue, GyroZvalue, e1, e2, Marks;
    protected String LocData, mLastUpdateTime;
    boolean gAvailable = true;
    private MediaPlayer mp;

    private boolean startFlag = false;

    private int no_of_lines = 0, meansumx =0, meansumy =0, meansumz =0;
    private float meanx, meany, meanz, deviation;

    private Date startTime, endTime;

    boolean locAccHit = false, locUpdating = false;
    // Sometimes location is first taken from the last known location. When location starts updating
    // the accuracy can get worse before getting better again

    private UUID fileid;

    private LocationRequest mLocationRequest;
    private File file;
    private OutputStream out;
    private boolean mRequestingLocationUpdates;

    private Location mCurrentLocation;

    public final long UPDATE_INTERVAL_IN_MILLISECONDS = 0; // Fastest possible limited by hardware
    public final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 0;

    private Trip newtrip;
    private FirebaseAuth mAuth;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseDatabase mDatabase;
    private DatabaseReference ref;

    private ArrayList<MyLocation> gpsPolls;
    private float distance_travelled = 0;
    private float[] results = new float[]{0.0f, 0.0f, 0.0f};
    private double[] prevLoc = new double[2];

    public LoggerService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        newtrip = new Trip();
        fileid = UUID.randomUUID();
        newtrip.setTrip_id(fileid.toString());
        mAuth = FirebaseAuth.getInstance();

        mDatabase = FirebaseDatabase.getInstance();

        ref = mDatabase.getReference();
        gpsPolls = new ArrayList<>();

        newtrip.setDevice(Build.MANUFACTURER + " " + Build.MODEL + " " + Build.PRODUCT);
        newtrip.setUser_id(mAuth.getCurrentUser().getUid());

        mCurrentLocation = ApplicationClass.getGoogleApiHelper().mCurrentLocation;

        if (mCurrentLocation == null) {
            // retrieving .....
            try {
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                Log.d(TAG, "got initial location data");
            } catch (SecurityException e) {
                Log.d(TAG, "Permission not granted for gps");
            }
            //mLastUpdateTime = new Date().toString();
            if (mCurrentLocation != null) {
                mLastUpdateTime = getCurrentTime();
                LocData = getLocData();
            }
        }
        else{
            mLastUpdateTime = getCurrentTime();
            LocData = getLocData();
        }
        startLocationUpdates();

        ///////////// TO MAKE sure that devices without gyroscope dont have null locations
        if(mCurrentLocation != null) {
            newtrip.setStartLoc(MyLocation.locToMyloc(mCurrentLocation));
            newtrip.setEndLoc(MyLocation.locToMyloc(mCurrentLocation));
        }
        //////////////

        newtrip.setStartTime(getCurrentDateTime());
        startTime = new Date();  // to pass into bundle
        setupSensors();
        setupLogFile();
        mp = MediaPlayer.create(getApplication(), R.raw.beep);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        logAnalytics("trip_started");

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();


        if (!ApplicationClass.getGoogleApiHelper().getGoogleApiClient().isConnected())
            ApplicationClass.getGoogleApiHelper().getGoogleApiClient().connect();

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Road Quality Audit")
                .setContentText("Collecting sensor data")
                .setSubText("Please do not turn off the GPS")
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);

    }

    private void setupSensors() {

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        }
        else {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) {
            mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        } else {
            // Gyro not available
            gAvailable = false;
        }

        Log.i(TAG, "Sensors setup");

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_FASTEST);

        Log.i(TAG, "Sensors listeners setup");

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            Log.w("Prox", String.valueOf(sensorEvent.values[0]));

            if (sensorEvent.values[0] < 5) {
                mp.start();
                Marks = "1, ";
                // and Marks = " , 1 ";  for audio marking
            }
        }

        if (sensorEvent.sensor.getType() == mAccelerometer.getType()) {  // or TYPE_ACCELEROMETER
            AccXvalue = String.format("%.1f", sensorEvent.values[0]);
            AccYvalue = String.format("%.1f", sensorEvent.values[1]);
            AccZvalue = String.format("%.1f", sensorEvent.values[2]);

            // AccX, AccY, AccZ, GyrX, GyrY, GyrZ, latitude, longitude, timestamp, accuracy (m)
            e1 = AccXvalue + ", " + AccYvalue + ", " + AccZvalue + ", ";

            meansumx += sensorEvent.values[0];
            meansumy += sensorEvent.values[1];
            meansumz += sensorEvent.values[2];
        }

        if (!gAvailable) {
            e2 = null;
        } else if (sensorEvent.sensor.getType() == mGyroscope.getType()) {

            GyroXvalue = String.format("%.1f", sensorEvent.values[0]);
            GyroYvalue = String.format("%.1f", sensorEvent.values[1]);
            GyroZvalue = String.format("%.1f", sensorEvent.values[2]);

            e2 = GyroXvalue + ", " + GyroYvalue + ", " + GyroZvalue + ", ";

        }
        if(mCurrentLocation!=null && e1!=null && (e2!=null || !gAvailable)) {
            if (mCurrentLocation.getAccuracy() < ACCURACY_REQUIRED) {
                if (!locAccHit && locUpdating) {
                    locAccHit = true;
                    Log.i(TAG, "Location accuracy hit");
                    logAnalytics("location_accuracy_hit_started_logging");
                    prevLoc[0] = mCurrentLocation.getLatitude();
                    prevLoc[1] = mCurrentLocation.getLongitude();
                }
                if (!startFlag) {
                    newtrip.setStartLoc(MyLocation.locToMyloc(mCurrentLocation));
                    startFlag = true;
                }

                no_of_lines++;
                writeToFile(e1, e2, LocData);
            } else {
                Log.i(TAG, "Location accuracy not hit " + mCurrentLocation.getAccuracy());
            }
        }

    }

    protected void writeToFile(String Acc, String Gyr, String LocationData) {
        String data;

        if(gAvailable)
            data = Acc + Gyr + LocationData + ", " + Marks + "\n";
        else
            data = Acc + LocationData + ", " + Marks + "\n";

        try {
            out.write(data.getBytes());
            Log.d(TAG, "Writing " + data);
            //Log.d(TAG, "Writing to csv file at "+ logFile.getPath() );
        } catch (IOException e) {
            Log.d(TAG, "File write failed: " + e.toString());
        }
        Marks = null;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void setupLogFile() {

        Log.i(TAG, "start" + String.valueOf(newtrip.getStartTime()));

        String path = "/logs/";
        File temp = new File(getApplicationContext().getFilesDir() + path);
        temp.mkdir();
        file = new File(temp.getPath(), fileid.toString()+ ".csv");

        Log.i(TAG, file.toString());

        String data;

        if (gAvailable){
            // Accx, Acc y, Axxz, Gyrx, gyry, gyrz, lat, long, timestamp, accuracy
            data = "AccX, AccY, AccZ, GyrX, GyrY, GyrZ, latitude, longitude, timestamp, accuracy, proximity\n";
        }else{
            // Accx, Acc y, Axxz, lat, long, timestamp, accuracy
            data = "AccX, AccY, AccZ, latitude, longitude, timestamp, accuracy, proximity\n";
        }

        try {
            out = new FileOutputStream(file, true);
            out.write(data.getBytes());

        } catch (IOException e) {
            Log.d(TAG, "File setup failed: " + e.toString());
        }
    }


    void sendTripLoggingBroadcast(boolean status, Uri uploadFileId /*, Bundle essentials*/) {
        Intent iTemp = new Intent("tripstatus");
        iTemp.putExtra("LoggingStatus", status);
        iTemp.putExtra("filename", uploadFileId);
        //iTemp.putExtra("essentials", essentials);
        LocalBroadcastManager l = LocalBroadcastManager.getInstance(this);
        l.sendBroadcast(iTemp);
    }


    @Override
    public void onDestroy() {
        meanx = (float)meansumx / no_of_lines;
        meany = (float)meansumy / no_of_lines;
        meanz = (float)meansumz / no_of_lines;

        Log.i(TAG+"Means", "x = " + String.valueOf(meanx));
        Log.i(TAG+"Means", "y = " + String.valueOf(meany));
        Log.i(TAG+"Means", "z = " + String.valueOf(meanz));

        try {
            out.close();
            if(!locAccHit){
                file.delete();
            }
        } catch (IOException e) {
            Log.d(TAG, "File closing failed: " + e.toString());
        }

        if(mCurrentLocation!=null) {
            newtrip.setEndLoc(MyLocation.locToMyloc(mCurrentLocation));
        }

        stopTrip();
        stopLocationUpdates();
        ApplicationClass.getGoogleApiHelper().getGoogleApiClient().disconnect();
        ApplicationClass.tripEnded = true;
        stopForeground(true);
    }

    public void stopTrip() {

        newtrip.setEndTime(getCurrentDateTime());
        endTime = new Date();
        newtrip.setNo_of_lines((int)(no_of_lines/calcTimeTravelledSecs()));
        newtrip.setNo_of_lines((int)(no_of_lines/calcTimeTravelledSecs()));

        Log.i(TAG+" endtime", String.valueOf(newtrip.getEndTime()));
        mp.stop();
        mSensorManager.unregisterListener(this);

        newtrip.setFilesize(file.length());
        newtrip.setUploaded(false);
        newtrip.setDistanceInKM(distance_travelled/1000);
        Uri fileuri = Uri.fromFile(new File(file.getPath()));

        newtrip.setDuration(calcTimeTravelledMins());
        Log.i(TAG+" Duration", String.valueOf(calcTimeTravelledMins()) + " minutes");

        ApplicationClass.getInstance().setTrip(newtrip);
        Log.i(TAG, "logged newtrip");

        // the uri of the file to be uploaded from fragment
        logAnalytics("stopped_logging_sensor_data");
        if(locAccHit) {
            logGPSpollstoFile(gpsPolls);
            // sendNotificationForMap();
            logTripIDtoFile(newtrip.getTrip_id());
            ref.child(newtrip.getUser_id()).child(newtrip.getTrip_id()).setValue(newtrip);
            sendTripLoggingBroadcast(false, fileuri/*, createEssentialsBundle(newtrip)*/);
        }else {
            logAnalytics("unsuccessful_in_starting_logging");
            sendTripLoggingBroadcast(false, null/*, null*/);
        }

    }

    public String getCurrentTime(){
        Date d = new Date();
        try {
            d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(new Date().toString());
        }catch (ParseException e){

        }
        String time = new SimpleDateFormat("HH:mm:ss").format(d);
        return time;
    }

    public String getCurrentDateTime(){
        Date d = new Date();
        try {
            d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(new Date().toString());
        }catch (ParseException e){

        }
        return d.toString();
    }

    public void startLocationUpdates() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    ApplicationClass.getGoogleApiHelper().getGoogleApiClient(),
                    mLocationRequest,
                    this, Looper.getMainLooper()
            ).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    mRequestingLocationUpdates = true;
                }
            });
        } catch (SecurityException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        LocationServices.FusedLocationApi.removeLocationUpdates(ApplicationClass.getGoogleApiHelper().getGoogleApiClient(), this);
    }

    @Override
    public void onLocationChanged(Location location) {
        locUpdating = true;
        if (location.getAccuracy() < ACCURACY_REQUIRED && mCurrentLocation != null) {
            prevLoc[0] = mCurrentLocation.getLatitude();
            prevLoc[1] = mCurrentLocation.getLongitude();
            Location.distanceBetween(prevLoc[0], prevLoc[1], location.getLatitude(), location.getLongitude(), results);
            distance_travelled += results[0];
            gpsPolls.add(MyLocation.locToMyloc(location));
        }
        mCurrentLocation = location;
        mLastUpdateTime = getCurrentTime();
        LocData = getLocData();
    }

    public String getLocData() {

        String l = String.format("%f", mCurrentLocation.getLatitude()) + ", " +
                String.format("%f", mCurrentLocation.getLongitude()) + ", " +
                String.format("%s", mLastUpdateTime) + ", " +
                String.format("%.1f", mCurrentLocation.getAccuracy());
        return l;
    }

    public long calcTimeTravelledMins(){
        long duration  = endTime.getTime() - startTime.getTime();
        return TimeUnit.MILLISECONDS.toMinutes(duration);
    }

    public long calcTimeTravelledSecs(){
        long duration  = endTime.getTime() - startTime.getTime();
        return TimeUnit.MILLISECONDS.toSeconds(duration);
    }

    public void logAnalytics(String data){
        Bundle b = new Bundle();
        b.putString("LoggerService", data);
        mFirebaseAnalytics.logEvent(data, b);
        Log.i("LoggerService", data);
    }

    private void logTripIDtoFile(String name){

        String path = "tripsIDs.csv";
        File temp = new File(getApplicationContext().getFilesDir(), path);

        Log.i("filename", temp.toString());

        String data = name;

        try {
            FileOutputStream out = new FileOutputStream(temp, false);   // TODO : change to append the trip ids
            out.write(data.getBytes());

        } catch (IOException e) {
            Log.d(TAG, "File setup failed: " + e.toString());
        }
        try {
            out.close();
        }catch (IOException e){

        }

    }

    private void logGPSpollstoFile(ArrayList<MyLocation> polls) {

        String path = "/analysis/";
        File temp = new File(getApplicationContext().getFilesDir() + path);
        temp.mkdir();
        File outfile = new File(temp.getPath(), fileid.toString() + ".csv");

        try {
            out = new FileOutputStream(outfile, true);

            for (MyLocation loc : polls) {
                String data = String.valueOf(loc.getLatitude()).trim() + "," + String.valueOf(loc.getLongitude()).trim()
                        + "," + String.valueOf(loc.getAccuracy()).trim() + "\n";
                out.write(data.getBytes());
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File_setup_failed: " + e.toString());
        } catch (IOException e) {
            Log.d(TAG, "File_setup_failed: " + e.toString());
        }

    }

    private void sendNotificationForMap() {
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

}

// TODO : Log no. of location updates, locations changed ...

// TODO : Android O Support : fileURI