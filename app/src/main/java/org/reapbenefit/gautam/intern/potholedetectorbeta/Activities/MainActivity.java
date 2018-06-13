package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;


import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceResponse;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.DefaultRequest;
import com.amazonaws.Request;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.http.HttpClient;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.http.HttpResponse;
import com.amazonaws.http.HttpResponseHandler;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.internal.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import org.reapbenefit.gautam.intern.potholedetectorbeta.BuildConfig;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.TransitionAlarm;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments.EasyModeFragment;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments.OverviewFragment;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments.TriplistFragment;
import org.reapbenefit.gautam.intern.potholedetectorbeta.PagerAdapter;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.S3UploadService;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Util;

import java.net.URI;

public class MainActivity extends AppCompatActivity
        implements TabLayout.OnTabSelectedListener,
        TriplistFragment.OnFragmentInteractionListener,
        EasyModeFragment.OnFragmentInteractionListener,
        OverviewFragment.OnFragmentInteractionListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private TabLayout tabLayout;
    private ViewPager viewPager;

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 389;
    private static final int REQUEST_CHECK_SETTINGS = 23;

    private Intent loggerIntent;
    private FusedLocationProviderClient mFusedLocationClient;

    private SharedPreferences dbPreferences;

    private FirebaseAuth mAuth;
    private Toolbar toolbar;
    ApplicationClass app;
    private GoogleApiClient apiClient;
    private final int INTERVAL_MILLISECONDS = 3000;
    private final String TAG = getClass().getSimpleName();
    private ActivityRecognitionClient activityRecognitionClient;
    private Handler handler;
    private Context context;
    private boolean inCar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        Log.d(TAG, "Inside onCreate");
        app = ApplicationClass.getInstance();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setContentView(R.layout.activity_main);

        dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        String tripUploadedJson = dbPreferences.getString("tripUploaded", null);
        Log.d(getClass().getSimpleName(), tripUploadedJson + "");
        if (tripUploadedJson != null) {
            //initiating DB update through TripViewModel
            Intent dbUpdateIntent = new Intent(getString(R.string.set_uploaded_true));
            dbUpdateIntent.putExtra("tripUploaded", new Gson().fromJson(tripUploadedJson, Trip.class));
            LocalBroadcastManager.getInstance(this).sendBroadcast(dbUpdateIntent);
            dbPreferences.edit().putString("tripUploaded", null).commit();
        }

        inCar = getIntent().getBooleanExtra("inCar", false);
        Log.i("inCar MainActivity old", inCar + "");
        //Adding toolbar to the activity
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Road Quality Audit");

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            // Toast
            Toast.makeText(this, "Please login to start using the app", Toast.LENGTH_LONG).show();
            // open login activity

            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
        }

        settingsRequest();
        checkPermissions();

        /*
        getting user data from AWS
         */
        //new GetUserDataAsyncTask().execute();

        //Initializing the tablayout
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        //Adding the tabs using addTab() method
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_action_home));
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_list));
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_map_black_24dp));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);


        viewPager = (ViewPager) findViewById(R.id.pager);
        PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount(), inCar);
        viewPager.setAdapter(adapter);

        //Adding onTabSelectedListener to swipe views
        tabLayout.setOnTabSelectedListener(this);

        viewPager.addOnPageChangeListener(
                new TabLayout.TabLayoutOnPageChangeListener(tabLayout)
        );

        SharedPreferences prefs = getSharedPreferences("uploads", MODE_PRIVATE);
        if (!prefs.contains("file_delete"))
            prefs.edit().putBoolean("file_delete", false);

        //setting TransitionAlarm
        setAlarm(5000);
    }

    private void setAlarm(long timeinMillis) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, TransitionAlarm.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmManager.setRepeating(AlarmManager.RTC, timeinMillis, timeinMillis, pendingIntent);
    }

    private void settingsRequest(){
        @SuppressLint("RestrictedApi")
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests now.
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(MainActivity.this,
                                    REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            getLastLocation();
        }
        settingsRequest();

    }

    /**
     * Provides a simple way of getting a device's location and is well suited for
     * applications that do not require a fine-grained location and that do not need location
     * updates. Gets the best and most recent location currently available, which may be null
     * in rare cases when a location is not available.
     * <p>
     * Note: this method should be called after location permission has been granted.
     */
    @SuppressWarnings("MissingPermission")
    private void getLastLocation() {
        mFusedLocationClient.getLastLocation()
                .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            app.setCurrentLocation(task.getResult());
                            if(task.getResult().getAccuracy() < 25 && app.isTripEnded()) {
                                //showSnackbar("Location detected");
                                Log.d(TAG, "Location Detected");
                            }
                        } else {
                            Log.w(TAG, "getLastLocation:exception", task.getException());

                            showSnackbar(getString(R.string.no_location_detected));
                        }
                    }
                });
    }

    /**
     * Shows a {@link Snackbar} using {@code text}.
     *
     * @param text The Snackbar text.
     */
    private void showSnackbar(final String text) {
        View container = findViewById(R.id.main_activity_container);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");

            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            startLocationPermissionRequest();
                        }
                    });

        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            startLocationPermissionRequest();
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted.
                getLastLocation();
                //buildDialog();
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
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
        Intent intent = new Intent();
        if (id == R.id.actions_about) {
            intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }
        if (id == R.id.actions_credits) {
            intent = new Intent(this, CreditsActivity.class);
            startActivity(intent);
        }
        if (id == R.id.actions_partners) {
            intent = new Intent(this, PartnersActivity.class);
            startActivity(intent);
        }
        if (id == R.id.actions_login) {
            intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        if(id == R.id.actions_settings){
            intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        if (id == R.id.actions_invite) {
            intent = new Intent();
            //intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.invite_message));
            intent.setType("text/plain");
            startActivity(intent.createChooser(intent, "Help your friends map potholes"));
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
    public void onConnected(@Nullable Bundle bundle) {
        //called when app connects to Play Services
        /*Intent intent = new Intent(this, ActivityRecognizedService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(apiClient, INTERVAL_MILLISECONDS,
                pendingIntent);*/
            }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private class GetUserDataAsyncTask extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            CognitoCachingCredentialsProvider cognitoCachingCredentialsProvider = Util.getsCredProvider(
                    context);
            String accessKey = cognitoCachingCredentialsProvider.getCredentials().getAWSAccessKeyId();
            String secretKey = cognitoCachingCredentialsProvider.getCredentials().getAWSSecretKey();
            String sessionKey = cognitoCachingCredentialsProvider.getCredentials().getSessionToken();

            Log.d("Access Key", accessKey);
            Log.d("Secret Key", secretKey);
            int i = 0;
            Log.d("Session Key", sessionKey.substring(0, sessionKey.length()/2));
            Log.d("Session Key", sessionKey.substring(sessionKey.length()/2 + 1, sessionKey.length() - 1));

            AmazonWebServiceRequest amazonWebServiceRequest = new AmazonWebServiceRequest() {};
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            String API_GATEWAY_SERVICE_NAME = "execute-api";
            Request request = new DefaultRequest(amazonWebServiceRequest,API_GATEWAY_SERVICE_NAME);
            request.setEndpoint(URI.create("https://990rl1xx1d.execute-api.ap-south-1.amazonaws.com/Staging/rdsCreate" +
                    "/potholes/"));
            request.setHttpMethod(HttpMethodName.GET);

            AWS4Signer signer = new AWS4Signer();
            signer.setServiceName(API_GATEWAY_SERVICE_NAME);
            signer.setRegionName(Region.getRegion(Regions.US_EAST_1).getName());
            signer.sign(request, cognitoCachingCredentialsProvider.getCredentials());

            BasicSessionCredentials credentials = new BasicSessionCredentials(accessKey, secretKey, sessionKey);

            AmazonWebServiceResponse response = new AmazonWebServiceResponse();

            return null;
        }
    }


    /*
    public void startActivityService() {
        // fire only when trip started = false
        if (!tripStarted) {
            Intent intent = new Intent(this, ActivityRecognizedService.class);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 5000, pendingIntent);
        }
    }
    */

}

// Make file names based on start-end location
// eg koramangala to indiranagar

//  https://coolors.co/6da34d-c9cba3-202030-39304a-52528c  for colours


