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
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by gautam on 04/06/17.
 * This service does all the data logging
 */

public class LoggerService extends Service implements SensorEventListener {

    private static final int STANDARDISED_MILLISECONDS = 334;
    protected String TAG = "Logger_Service";
    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mGyroscope, mProximity;
    //1525 metre accuracy sensitive if in debug state, 25 in release
    private static final int ACCURACY_REQUIRED = BuildConfig.DEBUG?1525:25;
    private static final String INPUT_NODE = "input_1";
    private static final String OUTPUT_NODE = "output_node0";

    private String Marks;
    float accVals[] = null, gyrVals[] = null;
    protected String LocData, mLastUpdateTime;
    boolean gAvailable = true;

    private final float IDLE_TIME_UPPER_THRESHOLD = 1.38f;

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

    private TensorFlowInferenceInterface tensorFlowInferenceInterface;
    private float[] loggedData = new float[4];
    private double[] loggedLatLng = new double[3];
    private float[] modelOutput = new float[4];
    private String[] outputNodes = new String[1];
    private Timer timer = new Timer();

    public LoggerService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        app = ApplicationClass.getInstance();

        tensorFlowInferenceInterface = app.getTensorFlowInferenceInterface();
        outputNodes[0] = OUTPUT_NODE;

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
        setupSquaredErrorFile();

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

        minutesWasted = dbPreferences.getLong(getString(R.string.minutes_wasted), 0);
        minutesAccuracyLow = dbPreferences.getLong(getString(R.string.minutes_accuracy_low), 0);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // picking up values generated by model and calculating MSE
                tensorFlowInferenceInterface.feed(INPUT_NODE, loggedData, 1l, loggedData.length);
                tensorFlowInferenceInterface.run(outputNodes);
                tensorFlowInferenceInterface.fetch(OUTPUT_NODE, modelOutput);
                float[] readingSquaredError = new float[modelOutput.length];
                for (int i = 0; i < modelOutput.length; ++i) {
                    readingSquaredError[i] = (loggedData[i] - modelOutput[i]) * (loggedData[i] - modelOutput[i]);
                }
                saveSquaredError(readingSquaredError, loggedLatLng);
            }
        }, STANDARDISED_MILLISECONDS, STANDARDISED_MILLISECONDS);
    }

    @SuppressLint("RestrictedApi")
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
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
                checkIfIdle(speed);
                startTrafficTime = Calendar.getInstance().getTime();
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

    private void checkIfIdle(float s) {
        if (s <= IDLE_TIME_UPPER_THRESHOLD) {
            minutesWasted += Calendar.getInstance().getTimeInMillis() - startTrafficTime.getTime();
        }
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

        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            // Log.w("Prox", String.valueOf(sensorEvent.values[0]));

            if (sensorEvent.values[0] < 5) {
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
                prepSensorReadings(accVals);
            } else {
                // Log.i(TAG, "Location accuracy not hit " + mCurrentLocation.getAccuracy());
                calcAccuracyLowTime();
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void calcAccuracyLowTime() {
        accuracyLostTime = Calendar.getInstance().getTime();
        minutesAccuracyLow += accuracyLostTime.getTime() - startAccuracyTime.getTime();
        startAccuracyTime = accuracyLostTime;
    }

    private void setupSquaredErrorFile() {
        String path = "/modelErrors/";
        File temp = new File(getApplicationContext().getFilesDir() + "/modelErrors/");
        temp.mkdir();
        file = new File(temp.getPath(), fileid.toString() + ".csv");

        String data = "AccX,AccY,AccZ,Speed,Latitude,Longitude,AbsoluteSpeed\n";

        try {
            out = new FileOutputStream(file, true);
            out.write(data.getBytes());

        } catch (IOException e) {
            // Log.d(TAG, "File setup failed: " + e.toString());
        }
    }

    private void saveSquaredError(float[] squaredError, double[] latlng) {
        String data = floatArraytoString(squaredError) + String.valueOf(latlng[0]) + ","
                + String.valueOf(latlng[1]) + "," + String.valueOf(latlng[2]) + "\n";
        try {
            Log.d(TAG, "Writing " + data);
            out.write(data.getBytes());
        } catch(IOException ioException) {
            Log.e(TAG, ioException.getStackTrace().toString());
        }
    }

    protected void prepSensorReadings(float[] acc) {
        loggedData[0] = acc[0];
        loggedData[1] = acc[1];
        loggedData[2] = acc[2];
        loggedData[3] = speed;
        loggedLatLng[0] = mCurrentLocation.getLatitude();
        loggedLatLng[1] = mCurrentLocation.getLongitude();
        loggedLatLng[2] = mCurrentLocation.getSpeed();
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


    void sendTripLoggingBroadcast(boolean status, Uri uploadFileId /*, Bundle essentials*/) {
        Intent iTemp = new Intent("tripstatus");
        iTemp.putExtra("LoggingStatus", status);
        iTemp.putExtra("filename", uploadFileId);
        iTemp.putExtra(getString(R.string.duration_in_seconds), TimeUnit.MILLISECONDS.toSeconds(endTime.getTime() - startTime.getTime()));
        iTemp.putExtra("trip_object", newtrip);
        iTemp.putExtra(getString(R.string.speed_with_location_hashmap), speedWithLocationMap);
        // Log.d(TAG, speedWithLocationMap.toString() + "");
        LocalBroadcastManager l = LocalBroadcastManager.getInstance(this);
        l.sendBroadcast(iTemp);
        timer.purge();
    }


    @Override
    public void onDestroy() {
        calcMeans();
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
        timer.cancel();
        newtrip.setEndTime(getCurrentDateTime());
        endTime = new Date();
        try {
            newtrip.setNo_of_lines((int) (no_of_lines / calcTimeTravelledSecs()));
        }catch (ArithmeticException ae){
            newtrip.setNo_of_lines(0);
        }
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

        if (minutesWasted != -1) {
            // Log.d("minutesWasted", minutesWasted + " milliseconds");
            minutesWasted = TimeUnit.MILLISECONDS.toMinutes(minutesWasted);
            newtrip.setMinutesWasted(minutesWasted);
        }

        dbPreferences.edit().remove(getString(R.string.minutes_wasted)).commit();
        dbPreferences.edit().remove(getString(R.string.traffic_time_start)).commit();
        minutesAccuracyLow = TimeUnit.MILLISECONDS.toMinutes(minutesAccuracyLow);
        newtrip.setMinutesAccuracyLow(minutesAccuracyLow);

        logAnalytics("stopped_logging_sensor_data");
        if(locAccHit) {
            dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
            newTripSet.add(new Gson().toJson(newtrip));
            if (internetAvailable()) {
                toBeUploadedTripSet.add(new Gson().toJson(newtrip));
            }
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
}

// TODO : Android O Support : fileURI