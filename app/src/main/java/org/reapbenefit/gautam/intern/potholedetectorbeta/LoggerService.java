package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentSender;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by gautam on 04/06/17.
 * This service does all the data logging
 */

public class LoggerService extends Service implements SensorEventListener, LocationListener {

    protected String TAG = "Logger Service";
    private StorageReference mStorageRef;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer, mGyroscope, mProximity;

    private String AccXvalue, AccYvalue, AccZvalue, GyroXvalue, GyroYvalue, GyroZvalue, e1, e2, Marks;
    protected String LocData, mLastUpdateTime;
    boolean gAvailable;
    MediaPlayer mp;

    Trip currentTrip;
    boolean startFlag = false;

    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    File file;
    OutputStream out;
    boolean mRequestingLocationUpdates;

    Location mCurrentLocation;

    public final long UPDATE_INTERVAL_IN_MILLISECONDS = 0; // Fastest possible limited by hardware
    public final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 0;


    public LoggerService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        setupLogFile();
        mp = MediaPlayer.create(getApplication(), R.raw.beep);
        mCurrentLocation = ApplicationClass.getGoogleApiHelper().mCurrentLocation;
        if (!ApplicationClass.getGoogleApiHelper().getGoogleApiClient().isConnected())
            ApplicationClass.getGoogleApiHelper().getGoogleApiClient().connect();
        startLocationUpdates();
        setupSensors();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        return START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //mGoogleApiClient = ApplicationClass.getGoogleApiHelper().getGoogleApiClient();
        if (!ApplicationClass.getGoogleApiHelper().getGoogleApiClient().isConnected())
            ApplicationClass.getGoogleApiHelper().getGoogleApiClient().connect();

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Road Quality Audit")
                .setContentText("Doing some work...")
                .setSubText("Please do not turn off gps setting")
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);


    }

    private void setupSensors() {

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
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

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {  // or TYPE_ACCELEROMETER
            AccXvalue = String.format("%.1f", sensorEvent.values[0]);
            AccYvalue = String.format("%.1f", sensorEvent.values[1]);
            AccZvalue = String.format("%.1f", sensorEvent.values[2]);

            // AccX, AccY, AccZ, GyrX, GyrY, GyrZ, latitude, longitude, timestamp, accuracy (m)
            e1 = AccXvalue + ", " + AccYvalue + ", " + AccZvalue + ", ";

        }

        if (!gAvailable) {
            e2 = "null" + ", " + "null" + ", " + "null" + ", ";
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

            if (gAvailable) {
                GyroXvalue = String.format("%.1f", sensorEvent.values[0]);
                GyroYvalue = String.format("%.1f", sensorEvent.values[1]);
                GyroZvalue = String.format("%.1f", sensorEvent.values[2]);

                e2 = GyroXvalue + ", " + GyroYvalue + ", " + GyroZvalue + ", ";
            }
        }
/*
        if (mCurrentLocation.getAccuracy() < 10) {
            if (!startFlag) {
                currentTrip.setStartLoc(mCurrentLocation);
                startFlag = true;
            }
  */
            writeToFile(e1, e2, LocData);
    //    }

    }

    protected void writeToFile(String Acc, String Gyr, String LocationData) {
        String data = Acc + Gyr + LocationData + ", " + Marks + "\n";
        try {
            out = new FileOutputStream(file, true);
            out.write(data.getBytes());
            out.close();
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
        String time = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, Locale.UK).format(new Date()).replace(":", "");
        String t1 = time.replaceFirst(",", "");
        String t2 = t1.replace(" ", "") + ".csv";
        String path = "logs/";
        File temp = new File(getApplicationContext().getFilesDir() + path);
        temp.mkdir();
        file = new File(temp.getPath(), t2);

        // Accx, Acc y, Axxz, Gyrx, gyry, gyrz, lat, long, timestamp, accuracy
        String data = "AccX, AccY, AccZ, variable(xyz), GyrX, GyrY, GyrZ, latitude, longitude, timestamp, accuracy (m), proximity marking\n";

        try {
            out = new FileOutputStream(file, true);
            out.write(data.getBytes());
            out.close();

        } catch (IOException e) {
            Log.d(TAG, "File setup failed: " + e.toString());
        }
    }

    public void stopTrip() {

        String time = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, Locale.UK).format(new Date()).replace(":", "");
        mp.stop();
        mSensorManager.unregisterListener(this);

        // extractTripDetails service + upload

        StorageReference riversRef = mStorageRef.child("logs/" + file.getName());

        Uri fileuri = Uri.fromFile(new File(file.getPath()));
        UploadTask uploadTask = riversRef.putFile(fileuri);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                Log.d("Uploaded", "Done");
            }
        });

        sendTripLoggingBroadcast(false);

    }

    void sendTripLoggingBroadcast(boolean status) {
        Intent iTemp = new Intent("tripstatus");
        iTemp.putExtra("LoggingStatus", status);
        LocalBroadcastManager l = LocalBroadcastManager.getInstance(this);
        l.sendBroadcast(iTemp);
    }


    @Override
    public void onDestroy() {
        stopTrip();
        stopLocationUpdates();
        ApplicationClass.getGoogleApiHelper().getGoogleApiClient().disconnect();
        stopForeground(true);
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
        } catch (SecurityException e) {

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
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date()).replaceFirst(",", "");
        LocData = getLocData();
    }

    public String getLocData() {

        String l = String.format("%f", mCurrentLocation.getLatitude()) + ", " +
                String.format("%f", mCurrentLocation.getLongitude()) + ", " +
                String.format("%s", mLastUpdateTime) + ", " +
                String.format("%.1f", mCurrentLocation.getAccuracy());
        return l;
    }

}
