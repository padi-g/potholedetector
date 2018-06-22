package org.reapbenefit.gautam.intern.potholedetectorbeta.Core;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MainActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.BuildConfig;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by gautam on 04/06/17.
 * This service does all the data logging
 */

public class LoggerService extends Service implements SensorEventListener {

    protected String TAG = "Logger_Service";
    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mGyroscope, mProximity;
    //1525 metre accuracy sensitive if in debug state, 25 in release
    private static final int ACCURACY_REQUIRED = BuildConfig.DEBUG?1525:25;

    private String Marks;
    float accVals[] = null, gyrVals[] = null;
    protected String LocData, mLastUpdateTime;
    boolean gAvailable = true;
    private MediaPlayer mp;

    private boolean startFlag = false;

    private int no_of_lines = 0;
    private float meanx, meany, meanz, threshold, meansumx =0, meansumy =0, meansumz =0;
    String axisOfInterest;

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
    private FusedLocationProviderClient mFusedLocationClient;
    private long inaccurateTiming = SystemClock.elapsedRealtimeNanos();
    private Intent trafficIntent;
    private Handler trafficHandler;
    private BroadcastReceiver trafficReceiver;
    private long minutesWasted = -1;
    private SharedPreferences dbPreferences;
    private Date newTime;
    private Date startTrafficTime;
    private String currentActivity;

    private Trip newtrip;
    private FirebaseAuth mAuth;
    private FirebaseAnalytics mFirebaseAnalytics;

    private ArrayList<MyLocation> gpsPolls;
    private float distance_travelled = 0;
    private float[] results = new float[]{0.0f, 0.0f, 0.0f};
    private double[] prevLoc = new double[2];

    ApplicationClass app;
    private LocationCallback mLocationCallback;

    private Date accuracyLostTime;
    private long minutesAccuracyLow;
    private Date startAccuracyTime;
    private TripViewModel tripViewModel;
    private Set<String> newTripSet;
    private Set<String> toBeUploadedTripSet;
    private float speed;
    private TreeMap<Integer, SpeedWithLocation> speedWithLocationMap = new TreeMap<>();

    public LoggerService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        app = ApplicationClass.getInstance();

        mCurrentLocation = app.getCurrentLocation();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        newtrip = new Trip();
        fileid = UUID.randomUUID();
        newtrip.setTrip_id(fileid.toString());
        mAuth = FirebaseAuth.getInstance();
        gpsPolls = new ArrayList<>();

        newtrip.setDevice(Build.MANUFACTURER + " " + Build.MODEL + " " + Build.PRODUCT);
        newtrip.setUser_id(mAuth.getCurrentUser().getUid());

        //getting previously logged trips in HashSet<>()
        SharedPreferences dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        newTripSet = dbPreferences.getStringSet("newTripJson", null);
        if (newTripSet == null)
            newTripSet = new HashSet<>();
        toBeUploadedTripSet = dbPreferences.getStringSet("toBeUploadedTripSet", null);
        if (toBeUploadedTripSet == null)
            toBeUploadedTripSet = new HashSet<>();

        if (mCurrentLocation != null) {
                mLastUpdateTime = getCurrentTime();
                LocData = getLocData();
            }
        createLocationCallback();
        createLocationRequest();
        startLocationUpdates();

        ///////////// TO MAKE sure that devices without gyroscope dont have null locations
        if(mCurrentLocation != null) {
            newtrip.setStartLoc(MyLocation.locToMyloc(mCurrentLocation));
            newtrip.setEndLoc(MyLocation.locToMyloc(mCurrentLocation));
        }
        //////////////

        newtrip.setStartTime(getCurrentDateTime());
        startTime = new Date();  // to pass into bundle

        startTrafficTime = startTime;
        startAccuracyTime = startTime;
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

        dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());

    }

    @SuppressLint("RestrictedApi")
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Location location = locationResult.getLastLocation();

                locUpdating = true;
                if (location.getAccuracy() < ACCURACY_REQUIRED && mCurrentLocation != null) {
                    if(age_minutes(mCurrentLocation) < 1) {
                        prevLoc[0] = mCurrentLocation.getLatitude();
                        prevLoc[1] = mCurrentLocation.getLongitude();
                        Location.distanceBetween(prevLoc[0], prevLoc[1], location.getLatitude(), location.getLongitude(), results);
                        distance_travelled += results[0];
                        gpsPolls.add(MyLocation.locToMyloc(location));
                    }
                }
                mCurrentLocation = location;
                speed = location.getSpeed();
                SpeedWithLocation speedWithLocation = new SpeedWithLocation();
                speedWithLocation.setLatitude(location.getLatitude());
                speedWithLocation.setLongitude(location.getLongitude());
                speedWithLocation.setSpeed(speed);
                speedWithLocationMap.put(no_of_lines, speedWithLocation);
                mLastUpdateTime = getCurrentTime();
                LocData = getLocData();
            }
        };
    }

    private long age_ms(Location last) {
        return (SystemClock.elapsedRealtimeNanos() - last
                .getElapsedRealtimeNanos()) / 1000000;
    }

    public int age_minutes(Location last) {
        return (int)age_ms(last)/(60*1000);
    }

    private void startLocationUpdates() {

        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
            mRequestingLocationUpdates = true;
        }catch (SecurityException e){

        }
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

        // Log.i(TAG, "Sensors setup");

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_FASTEST);

        // Log.i(TAG, "Sensors listeners setup");

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        //timing traffic latency
        //calcTrafficTime();

        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            // Log.w("Prox", String.valueOf(sensorEvent.values[0]));

            if (sensorEvent.values[0] < 5) {
                mp.start();
                Marks = "1, ";
            }
        }

        if (sensorEvent.sensor.getType() == mAccelerometer.getType()) {
            accVals = sensorEvent.values;
        }

        if (!gAvailable) {
            gyrVals = null;
        } else if (sensorEvent.sensor.getType() == mGyroscope.getType()) {
            gyrVals = sensorEvent.values;
        }

        if(mCurrentLocation!=null && accVals!=null && (gyrVals!=null || !gAvailable)) {
            if (mCurrentLocation.getAccuracy() < ACCURACY_REQUIRED) {
                if (!locAccHit && locUpdating) {
                    locAccHit = true;
                    // Log.i(TAG, "Location accuracy hit");
                    logAnalytics("location_accuracy_hit_started_logging");
                    prevLoc[0] = mCurrentLocation.getLatitude();
                    prevLoc[1] = mCurrentLocation.getLongitude();
                }
                if (!startFlag) {
                    newtrip.setStartLoc(MyLocation.locToMyloc(mCurrentLocation));
                    startFlag = true;
                }

                meansumx += Math.abs(accVals[0]);
                meansumy += Math.abs(accVals[1]);
                meansumz += Math.abs(accVals[2]);
                no_of_lines++;
                writeToFile(accVals, gyrVals, LocData);
            } else {
                // Log.i(TAG, "Location accuracy not hit " + mCurrentLocation.getAccuracy());
                calcAccuracyLowTime();
            }
        }

    }

    private void calcAccuracyLowTime() {
        accuracyLostTime = Calendar.getInstance().getTime();
        minutesAccuracyLow += accuracyLostTime.getTime() - startAccuracyTime.getTime();
        startAccuracyTime = accuracyLostTime;
    }

    /*private void calcTrafficTime() {
        //tracking time wasted in traffic
        transitionPrefs = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());transitionPrefs = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        currentActivity = transitionPrefs.getString("currentActivity", null);
        Log.d(TAG, currentActivity.toString());
        if (currentActivity != null) {
            if (currentActivity.toString().contains("STILL")) {
                newTime = Calendar.getInstance().getTime();
                minutesWasted += newTime.getTime() - startTrafficTime.getTime();
                startTrafficTime = newTime;
                Log.d(TAG, minutesWasted + "");
            }
        }

    }*/

    protected void writeToFile(float[] acc, float[] gyr, String LocationData) {
        String data;

        if(gAvailable)

            data = floatArraytoString(acc) + floatArraytoString(gyr) + LocationData + ", " + Marks + "," + speed + "\n";
        else
            data = floatArraytoString(acc) + LocationData + ", " + Marks + "," + speed + "\n";

        try {
            out.write(data.getBytes());
            // Log.i(TAG, "Writing " + data);
        } catch (IOException e) {
            // Log.d(TAG, "File write failed: " + e.toString());
        }
        Marks = null;

    }

    public String floatArraytoString(float[] fa){
        StringBuilder sb = new StringBuilder();
        for(float f : fa) {
            f = Math.abs(f);
            String temp = String.valueOf(f).trim();
            if(temp.length()>3)
                sb.append(temp.substring(0, 4) + ",");
            else
                sb.append(temp.substring(0, 3) + ",");
        }
        return sb.toString();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void setupLogFile() {

        // Log.i(TAG, "start" + String.valueOf(newtrip.getStartTime()));

        String path = "/logs/";
        File temp = new File(getApplicationContext().getFilesDir() + "/logs/");
        temp.mkdir();
        file = new File(temp.getPath(), fileid.toString()+ ".csv");

        // Log.i(TAG, file.toString());

        String data;

        if (gAvailable){
            // Accx, Acc y, Axxz, Gyrx, gyry, gyrz, lat, long, timestamp, accuracy, speed
            data = "AccX, AccY, AccZ, GyrX, GyrY, GyrZ, latitude, longitude, timestamp, accuracy, proximity, speed\n";
        }else{
            // Accx, Acc y, Axxz, lat, long, timestamp, accuracy
            data = "AccX, AccY, AccZ, latitude, longitude, timestamp, accuracy, proximity, speed\n";
        }

        try {
            out = new FileOutputStream(file, true);
            out.write(data.getBytes());

        } catch (IOException e) {
            // Log.d(TAG, "File setup failed: " + e.toString());
        }
    }


    void sendTripLoggingBroadcast(boolean status, Uri uploadFileId /*, Bundle essentials*/) {
        Intent iTemp = new Intent("tripstatus");
        iTemp.putExtra("LoggingStatus", status);
        iTemp.putExtra("filename", uploadFileId);
        iTemp.putExtra(getString(R.string.duration_in_seconds), TimeUnit.MILLISECONDS.toSeconds(endTime.getTime() - startTime.getTime()));
        iTemp.putExtra("trip_object", newtrip);
        iTemp.putExtra(getString(R.string.speed_with_location_hashmap), speedWithLocationMap);
        Log.d(TAG, speedWithLocationMap.toString() + "");
        LocalBroadcastManager l = LocalBroadcastManager.getInstance(this);
        l.sendBroadcast(iTemp);
    }


    @Override
    public void onDestroy() {
        calcMeans();

        // Log.i(TAG+"Means", "x = " + String.valueOf(meanx));
        // Log.i(TAG+"Means", "y = " + String.valueOf(meany));
        // Log.i(TAG+"Means", "z = " + String.valueOf(meanz));

        try {
            out.close();
            if(!locAccHit){
                file.delete();
            }
        } catch (IOException e) {
            // Log.d(TAG, "File closing failed: " + e.toString());
        }

        if(mCurrentLocation!=null) {
            newtrip.setEndLoc(MyLocation.locToMyloc(mCurrentLocation));
        }

        stopTrip();
        stopLocationUpdates();
        app.setTripEnded(true);
        stopForeground(true);
    }

    public void stopTrip() {

        newtrip.setEndTime(getCurrentDateTime());
        endTime = new Date();
        try {
            newtrip.setNo_of_lines((int) (no_of_lines / calcTimeTravelledSecs()));
        }catch (ArithmeticException ae){
            newtrip.setNo_of_lines(0);
        }
        // Log.i(TAG+" endtime", String.valueOf(newtrip.getEndTime()));
        mp.stop();
        mSensorManager.unregisterListener(this);

        newtrip.setFilesize(file.length());
        newtrip.setUploaded(false);
        newtrip.setDistanceInKM(distance_travelled/1000);
        // Log.d(TAG, String.valueOf(distance_travelled));
        Uri fileuri = Uri.fromFile(new File(file.getPath()));

        newtrip.setDuration(calcTimeTravelledMins());
        // Log.i(TAG+" Duration", String.valueOf(calcTimeTravelledMins()) + " minutes");

        app.setTrip(newtrip);
        // Log.i(TAG, "logged newtrip");

        minutesWasted = dbPreferences.getLong("minutesWasted", minutesWasted);
        if (minutesWasted != -1) {
            Log.d("minutesWasted", minutesWasted + " milliseconds");
            minutesWasted = TimeUnit.MILLISECONDS.toMinutes(minutesWasted);
            newtrip.setMinutesWasted(minutesWasted);
        }

        dbPreferences.edit().remove("minutesWasted").commit();
        dbPreferences.edit().remove(getString(R.string.traffic_time_start)).commit();
        minutesAccuracyLow = Math.round((minutesWasted/1000.0)/60.0);

        logAnalytics("stopped_logging_sensor_data");
        if(locAccHit) {
            logGPSpollstoFile(gpsPolls);
            dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
            newTripSet.add(new Gson().toJson(newtrip));
            if (internetAvailable())
                toBeUploadedTripSet.add(new Gson().toJson(newtrip));
            // Log.d("newTripSet", newTripSet.toString());
            // Log.d("toBeUploadedTripSet", newTripSet.toString());
            dbPreferences.edit().putStringSet("newTripJson", newTripSet).commit();
            dbPreferences.edit().putStringSet("toBeUploadedTripSet", toBeUploadedTripSet).commit();
            sendTripLoggingBroadcast(false, fileuri);
            Intent apiUpdateIntent = new Intent(this, APIService.class);
            apiUpdateIntent.putExtra("newTrip", newtrip);
            apiUpdateIntent.putExtra("request", "POST");
            apiUpdateIntent.putExtra("table", getString(R.string.trip_data_table));
            startService(apiUpdateIntent);
        }else {
            logAnalytics("unsuccessful_in_starting_logging");
            sendTripLoggingBroadcast(false, null/*, null*/);
        }

    }

    private boolean internetAvailable() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean isWifiConn = networkInfo.isConnected();
        networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean isMobileConn = networkInfo.isConnected();
        if(isMobileConn || isWifiConn)
            return true;
        else
            return false;
    }

    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            // Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
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

    private void calcMeans(){
        meanx = meansumx / no_of_lines;
        meany = meansumy / no_of_lines;
        meanz = meansumz / no_of_lines;

        if(meanz >= meany && meanz >= meanx) {
            threshold = meanz;
            axisOfInterest = "AccZ";
        }
        else if(meanx >= meany && meanx >= meanz) {
            threshold = meanx;
            axisOfInterest = "AccX";
        }
        else {
            threshold = meany;
            axisOfInterest = "AccY";
        }

        newtrip.setThreshold(threshold);
        newtrip.setAxis(axisOfInterest);
    }

    public void logAnalytics(String data){
        Bundle b = new Bundle();
        b.putString("LoggerService", data);
        mFirebaseAnalytics.logEvent(data, b);
        // Log.i("LoggerService", data);
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
            // Log.d(TAG, "File_setup_failed: " + e.toString());
        } catch (IOException e) {
            // Log.d(TAG, "File_setup_failed: " + e.toString());
        }

    }

}

// TODO : Android O Support : fileURI