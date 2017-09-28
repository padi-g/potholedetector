package org.reapbenefit.gautam.intern.potholedetectorbeta.Core;

import android.app.Notification;
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
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.reapbenefit.gautam.intern.potholedetectorbeta.Core.GoogleApiHelper.mGoogleApiClient;

/**
 * Created by gautam on 04/06/17.
 * This service does all the data logging
 */

public class LoggerService extends Service implements SensorEventListener, LocationListener {

    protected String TAG = "Logger Service";
    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mGyroscope, mProximity;
    private static final int ACCURACY_REQUIRED = 30;

    private String AccXvalue, AccYvalue, AccZvalue, GyroXvalue, GyroYvalue, GyroZvalue, e1, e2, Marks;
    protected String LocData, mLastUpdateTime;
    boolean gAvailable;
    MediaPlayer mp;

    boolean startFlag = false;

    int no_of_lines = 0, meansumx =0, meansumy =0, meansumz =0;
    float meanx, meany, meanz, deviation;

    Date startTime, endTime;

    boolean locAccHit = false, locUpdating = false;
    // Sometimes location is first taken from the last known location. When location starts updating
    // the accuracy can get worse before getting better again

    UUID fileid;

    LocationRequest mLocationRequest;
    File file;
    OutputStream out;
    boolean mRequestingLocationUpdates;

    Location mCurrentLocation;

    public final long UPDATE_INTERVAL_IN_MILLISECONDS = 0; // Fastest possible limited by hardware
    public final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 0;

    Trip newtrip;
    FirebaseAuth mAuth;
    FirebaseAnalytics mFirebaseAnalytics;
    FirebaseDatabase mDatabase;
    DatabaseReference ref;

    public LoggerService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        newtrip = new Trip();
        fileid = UUID.randomUUID();
        newtrip.setTrip_id(fileid.toString());
        mAuth = FirebaseAuth.getInstance();
        newtrip.setUser_id(mAuth.getCurrentUser().toString());

        mDatabase = FirebaseDatabase.getInstance();

        ref = mDatabase.getReference(newtrip.getTrip_id().toString());

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
            mLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date()).replaceFirst(",", "");
            if (mCurrentLocation != null)
                LocData = getLocData();
        }
        startLocationUpdates();

        newtrip.setStartTime(getCurrentTime());
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

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {  // or TYPE_ACCELEROMETER
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
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

            if (gAvailable) {
                GyroXvalue = String.format("%.1f", sensorEvent.values[0]);
                GyroYvalue = String.format("%.1f", sensorEvent.values[1]);
                GyroZvalue = String.format("%.1f", sensorEvent.values[2]);

                e2 = GyroXvalue + ", " + GyroYvalue + ", " + GyroZvalue + ", ";
            }
        }
        if(mCurrentLocation!=null) {
            if (mCurrentLocation.getAccuracy() < ACCURACY_REQUIRED) {
                if (!locAccHit && locUpdating) {
                    locAccHit = true;
                    Log.i("Logger Service", "Location accuracy hit");
                    logAnalytics("location_accuracy_hit_started_logging");
                }
                if (!startFlag) {
                    newtrip.setStartLoc(mCurrentLocation);
                    startFlag = true;
                }

                no_of_lines++;
                writeToFile(e1, e2, LocData);
                // TODO : Remove the first few nulls
                // TODO : Do not log when the location is null
            } else {
                Log.i("Logger Service", "Location accuracy not hit" + mCurrentLocation.getAccuracy());
            }
        }

    }

    protected void writeToFile(String Acc, String Gyr, String LocationData) {
        String data;
        if(Gyr == null)
            data = Acc + LocationData + ", " + Marks + "\n";
        else
            data = Acc + Gyr + LocationData + ", " + Marks + "\n";

        if(LocationData == "null")

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

        Log.i(" time", "start" + String.valueOf(newtrip.getStartTime()));

        String path = "logs/";
        File temp = new File(getApplicationContext().getFilesDir() + path);
        temp.mkdir();
        file = new File(temp.getPath(), fileid.toString()+ ".csv");

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

    public void stopTrip() {

        newtrip.setNo_of_lines(no_of_lines);
        String time = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, Locale.UK).format(new Date());
        newtrip.setEndTime(time);

        Log.i(" endtime", String.valueOf(newtrip.getEndTime()));
        mp.stop();
        mSensorManager.unregisterListener(this);

        newtrip.setFilesize(file.length());
        newtrip.setUploaded(false);
        Uri fileuri = Uri.fromFile(new File(file.getPath()));
        newtrip.setTripfile(fileuri);
        newtrip.setDuration(calcTimeTravelled());
        Log.i("Time Elapsed in trip ", String.valueOf(calcTimeTravelled()) + " minutes");

        // WRITE TO DB HERE
       // ref.setValue(newtrip);    // not executing
        ApplicationClass.setTrip(newtrip);
        Log.i("Logger Service", "logged newtrip");

        // the uri of the file to be uploaded from fragment
        if(locAccHit)
            sendTripLoggingBroadcast(false, newtrip.getTripfile()/*, createEssentialsBundle(newtrip)*/);
            // WRITE TO DB HERE
        else
            sendTripLoggingBroadcast(false, null/*, null*/);


    }

    /*
    Bundle createEssentialsBundle(Trip t){

        // The bundle that is passed with the information to give the user information about
        // their trip like duration and distance travelled.

        Bundle b = new Bundle();
        b.putSerializable("startTime", startTime);
        b.putSerializable("endTime", endTime);
        b.putParcelable("startLocation", t.getStartLoc());
        b.putParcelable("endLocation", t.getEndLoc());

        return b;
    }
    */

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

        Log.i(" Means ", "x = " + String.valueOf(meanx));
        Log.i(" Means ", "y = " + String.valueOf(meany));
        Log.i(" Means ", "z = " + String.valueOf(meanz));

        try {
            out.close();
            if(!locAccHit){
                file.delete();
                // TODO Reset trip details / log failure in dynamo db
            }
        } catch (IOException e) {
            Log.d(TAG, "File closing failed: " + e.toString());
        }

        newtrip.setEndLoc(mCurrentLocation);
        newtrip.setEndTime(getCurrentTime());
        endTime = new Date();  // to pass into bundle

        stopTrip();
        stopLocationUpdates();
        ApplicationClass.getGoogleApiHelper().getGoogleApiClient().disconnect();
        ApplicationClass.tripEnded = true;
        stopForeground(true);
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

    public long calcTimeTravelled(){
        long duration  = endTime.getTime() - startTime.getTime();
        return TimeUnit.MILLISECONDS.toMinutes(duration);
    }

    public void logAnalytics(String data){
        Bundle b = new Bundle();
        b.putString("LoggerService", data);
        mFirebaseAnalytics.logEvent(data, b);
        Log.i("LoggerService", data);
    }


}
