package org.reapbenefit.gautam.intern.potholedetectorbeta;


import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
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
import java.util.List;
import java.util.Locale;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity
        implements TabLayout.OnTabSelectedListener,
        AdvancedModeFragment.OnFragmentInteractionListener, EasyModeFragment.OnFragmentInteractionListener,
        SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
        , com.google.android.gms.location.LocationListener, com.google.android.gms.location.ActivityRecognitionApi,
        EasyPermissions.PermissionCallbacks {

    private TabLayout tabLayout;
    private ViewPager viewPager;

    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected Location mCurrentLocation;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mProximity;

    protected boolean mRequestingLocationUpdates, tripStarted = false;
    int activityType, confidence;

    MediaPlayer mp;
    protected Boolean stopped, gAvailable = true;

    File logFile, file;
    OutputStream out;

    Float bumpHighThreshold, bumpLowThreshold;
    String curcar, curmodel;

    int count = 0, isbump = 0;

    int axisPressed = 2;

    private String AccXvalue;
    private String AccYvalue;
    private String AccZvalue;
    private String GyroXvalue;
    private String GyroYvalue;
    private String GyroZvalue;
    protected String LocData;

    protected String mLastUpdateTime, e1, e2, Marks;

    static Switch StartsStop;

    protected static final String TAG = "Main Activity";
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 0; // Fastest possible limited by hardware
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 0;
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("Activity Lifecycles", "Inside onCreate");

        setContentView(R.layout.activity_main);

        //Adding toolbar to the activity
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Road Quality Audit");

        StartsStop = (Switch) findViewById(R.id.stopSwitch);
        StartsStop.setChecked(false);
        Intent intentFromService = getIntent();
        mp = MediaPlayer.create(getApplication(), R.raw.beep);
        tripStarted = intentFromService.getBooleanExtra("CarMode", false); // false is default
        activityType = intentFromService.getIntExtra("ActivityType", -2);  // -2 is random.
        confidence = intentFromService.getIntExtra("Confidence", 0); // 0 is default

        if (!tripStarted) {
            // this did not come from intent service
            // Show dialog asking to open
            buildDialog();
        } else {
            StartsStop.setChecked(true);
            // start logging
            setupLogFile();
            mRequestingLocationUpdates = true;
            startLocationUpdates();
        }

        /*
        * If not from coming from service, show dialog asking driving state.
        * if coming from service, or driving confirmed, show a nice UI with
        * */

        //Initializing the tablayout
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        //Adding the tabs using addTab() method
        tabLayout.addTab(tabLayout.newTab().setIcon(R.mipmap.home));
        tabLayout.addTab(tabLayout.newTab().setIcon(R.mipmap.settings));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);


        viewPager = (ViewPager) findViewById(R.id.pager);
        Pager adapter = new Pager(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);

        //Adding onTabSelectedListener to swipe views
        tabLayout.setOnTabSelectedListener(this);

        viewPager.addOnPageChangeListener(
                new TabLayout.TabLayoutOnPageChangeListener(tabLayout)
        );


        askPermissions();

        setupSensors();

        buildGoogleApiClient();

        updateValuesFromBundle(savedInstanceState);




        // fire LocalBroadcast to updateUI
        Intent iTemp = new Intent("thisit");

        iTemp.putExtra("CarMode", true);
        iTemp.putExtra("ActivityType", activityType);
        iTemp.putExtra("Confidence", confidence);

        LocalBroadcastManager l = LocalBroadcastManager.getInstance(this);
        l.sendBroadcast(iTemp);
        Log.i("Main Activity", String.valueOf(intentFromService.getBooleanExtra("CarMode", false)));

        getFromPrefs();

        StartsStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    setupLogFile();
                    tripStarted = true;
                    mRequestingLocationUpdates = true;
                    startLocationUpdates();
                    Log.i("MainActivity", "Trip is false");
                } else {
                    tripStarted = false;
                    startService();
                    stopTrip();
                    stopLocationUpdates();
                    mRequestingLocationUpdates = false;
                }
            }
        });

     mStorageRef = FirebaseStorage.getInstance().getReference();

    }


    void buildDialog() {

        LayoutInflater inflater = getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.start_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.dialog_question);
        builder.setPositiveButton(R.string.dialog_positive,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StartsStop.setChecked(true);
                        // Start logging
                        setupLogFile();
                    }
                });
        builder.setNegativeButton(R.string.dialog_negative,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // leave as it it
                    }
                });
        builder.setView(dialoglayout);
        builder.show();
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            getLocData();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.actions_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }
        if (id == R.id.actions_credits) {
            Intent intent = new Intent(this, CreditsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {

    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        Log.d("Activity Lifecycles", "Inside onStart");

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        Log.d("Activity Lifecycles", "Inside onStop");
    }


    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_UI);
        getFromPrefs();
        Log.d("Activity Lifecycles", "Inside onResume");
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        Log.d("Activity Lifecycles", "Inside onPause");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            Log.w("Prox", String.valueOf(sensorEvent.values[0]));

            if (sensorEvent.values[0] < 5) {
                mp.start();
                Marks = " 1 , ";
                // and Marks = " , 1 ";  for audio marking
            }
        }

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {  // or TYPE_ACCELEROMETER
            AccXvalue = String.format("%.1f", sensorEvent.values[0]);
            AccYvalue = String.format("%.1f", sensorEvent.values[1]);
            AccZvalue = String.format("%.1f", sensorEvent.values[2]);

            if (sensorEvent.values[axisPressed] > bumpHighThreshold) {
                count++;
                isbump = 1;
            }
            if (sensorEvent.values[axisPressed] < bumpLowThreshold) {
                count++;
                isbump = 1;
            }
            String curVar;
            switch (axisPressed) {
                case 0:
                    curVar = "X";
                    break;
                case 1:
                    curVar = "Y";
                    break;
                case 2:
                    curVar = "Z";
                    break;
                default:
                    curVar = "Z";

            }

            // AccX, AccY, AccZ, variable(x,y,z) isBump, Threshold High, Threshold Low, GyrX, GyrY, GyrZ, latitude, longitude, timestamp, accuracy (m)
            e1 = AccXvalue + ", " + AccYvalue + ", " + AccZvalue + ", " + curVar + ", " + String.valueOf(isbump) + ", " + String.valueOf(bumpHighThreshold) + ", " + String.valueOf(bumpLowThreshold) + ", " + curcar + " " + curmodel + ", ";

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

        if (mRequestingLocationUpdates)
            writeToFile(e1, e2, LocData, Marks);


    }

    protected void writeToFile(String Acc, String Gyr, String LocationData, String MarkingData) {
        String data = Acc + Gyr + LocationData + ", " + Marks + "\n";
        Marks = null;
        try {
            out = new FileOutputStream(file, true);
            out.write(data.getBytes());
            out.close();
            Log.d(TAG, "Writing " + data);
            //Log.d(TAG, "Writing to csv file at "+ logFile.getPath() );
        } catch (IOException e) {
            Log.d(TAG, "File write failed: " + e.toString());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Connected to GoogleApiClient");

        startService();

        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
        // its value in the Bundle and check for it in onCreate(). We
        // do not request it again unless the user specifically requests location updates by pressing
        // the Start Updates button.
        //
        // Because we cache the value of the initial location in the Bundle, it means that if the
        // user launches the activity,
        // moves to a new location, and then changes the device orientation, the original location
        // is displayed as the activity is re-created.
        if (mCurrentLocation == null) {
            // retrieving .....
            try {
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            } catch (SecurityException e) {

            }
            mLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date()).replaceFirst(",", "");
            if (mCurrentLocation != null)
                getLocData();
        }

        // If the user presses the Start Updates button before GoogleApiClient connects, we set
        // mRequestingLocationUpdates to true (see startUpdatesButtonHandler()). Here, we check
        // the value of mRequestingLocationUpdates and if it is true, we start location updates.
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }

    }

    public void startService() {
        // fire only when trip started = false
        if (!tripStarted) {
            Intent intent = new Intent(this, ActivityRecognizedService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 5000, pendingIntent);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date()).replaceFirst(",", "");
        getLocData();

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i("MainActivity", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i("MainActivity", "Connection suspended");
        mGoogleApiClient.connect();
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

    }

    @AfterPermissionGranted(1)
    private void askPermissions() {

        String[] perms = {android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            // ...
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, "This app requires the following permissions to function properly",
                    1, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
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
    }

    private void setupLogFile() {
        logFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/PotholeApp/");
        logFile.mkdir();
        Log.d(TAG, logFile.getPath());
        String time = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, Locale.US).format(new Date()).replace(":", "");
        String t1 = time.replaceFirst(",", "");
        String t2 = t1.replace(" ", "") + ".csv";
        file = new File(logFile.getPath(), t2);

        // Write first header line to file
        // Accx, Acc y, Axxz, variable(x,y,z), isbump, Threshold high, Threshold low, Gyrx, gyry, gyrz, lat, long, timestamp, accuracy

        String data = "AccX, AccY, AccZ, variable(xyz), isBump, Threshold High, Threshold Low, Car, GyrX, GyrY, GyrZ, latitude, longitude, timestamp, accuracy (m), proximity marking, voice marking\n";

        try {
            out = new FileOutputStream(file, true);
            out.write(data.getBytes());
            out.close();

        } catch (IOException e) {
            Log.d(TAG, "File setup failed: " + e.toString());
        }
    }

    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .build();

        createLocationRequest();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);

        com.google.android.gms.common.api.PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i(TAG, "All location settings are satisfied.");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, 0x01);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }

    protected void startLocationUpdates() {

        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient,
                    mLocationRequest,
                    this
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

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public com.google.android.gms.common.api.PendingResult<Status> requestActivityUpdates(GoogleApiClient googleApiClient, long l, PendingIntent pendingIntent) {
        return null;
    }

    @Override
    public com.google.android.gms.common.api.PendingResult<Status> removeActivityUpdates(GoogleApiClient googleApiClient, PendingIntent pendingIntent) {
        return null;
    }

    private void getFromPrefs() {

        SharedPreferences sharedPref = getSharedPreferences("Profiles", Context.MODE_PRIVATE);
        curcar = sharedPref.getString("CurrentCar", "None");
        curmodel = sharedPref.getString("CurrentModel", "None");
        bumpHighThreshold = sharedPref.getFloat("High", 12.5f);
        bumpLowThreshold = sharedPref.getFloat("Low", 7.5f);

    }

    public void getLocData() {

        LocData = String.format("%f", mCurrentLocation.getLatitude()) + ", " +
                String.format("%f", mCurrentLocation.getLongitude()) + ", " +
                String.format("%s", mLastUpdateTime) + ", " +
                String.format("%.1f", mCurrentLocation.getAccuracy());
    }

    public void stopTrip() {

        // Makes the file available to file managers and the android system

        MediaScannerConnection.scanFile(this,
                new String[]{file.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);

                        // Upload to firebase

                        StorageReference riversRef = mStorageRef.child("logs/"+file.getName());

                        riversRef.putFile(uri)
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        // Get a URL to the uploaded content
                                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                        // this is useful information
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        // Handle unsuccessful uploads
                                        // ...
                                    }
                                });
                    }
                });

    }
}

// there is no check whether location enabled or not
// logging stops when goes out of activity but does not resume when it comes back but switch is still on   =======
// Switch goes off when back button is pressed and onCreate is started when it comes back   ======  Doesnt matter
// file is created as soon as app is opened regardless of yes or no   ======
// new file is not created when start stop is pressed in the same session, it just appends to the old file   ========


// Make file names based on start-end location
// eg koramangala to indiranagar

//  https://coolors.co/6da34d-c9cba3-202030-39304a-52528c  for colours