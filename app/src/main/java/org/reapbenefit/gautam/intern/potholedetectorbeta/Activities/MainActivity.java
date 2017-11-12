package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import org.reapbenefit.gautam.intern.potholedetectorbeta.BuildConfig;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.LoggerService;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments.EasyModeFragment;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments.TriplistFragment;
import org.reapbenefit.gautam.intern.potholedetectorbeta.PagerAdapter;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

public class MainActivity extends AppCompatActivity
        implements TabLayout.OnTabSelectedListener,
        TriplistFragment.OnFragmentInteractionListener,
        EasyModeFragment.OnFragmentInteractionListener{

    private TabLayout tabLayout;
    private ViewPager viewPager;

    private static Switch StartsStop;

    protected static final String TAG = "Main_Activity";
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 389;

    private Intent loggerIntent;
    private FusedLocationProviderClient mFusedLocationClient;

    private FirebaseAuth mAuth;
    private Toolbar toolbar;
    ApplicationClass app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Inside onCreate");
        app = ApplicationClass.getInstance();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        setContentView(R.layout.activity_main);

        //Adding toolbar to the activity
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Road Quality Audit");

        loggerIntent = new Intent(MainActivity.this, LoggerService.class);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser() == null){
            // Toast
            Toast.makeText(this, "Please login to start using the app", Toast.LENGTH_LONG).show();
            // open login activity
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
        }

        // TODO settings request

        StartsStop = (Switch) findViewById(R.id.stopSwitch);
        StartsStop.setChecked(false);

        if (!app.isTripInProgress() && !app.isTripEnded()) {
            // this did not come from intent service
            // Show dialog asking to open
            if(checkPermissions())
                buildDialog();
        } else if(app.isTripInProgress() && !app.isTripEnded()) {
            StartsStop.setChecked(true);
        }else if(app.isTripInProgress() && app.isTripEnded()) {
            // case is not possible
        } else /* !ApplicationClass.tripInProgress && ApplicationClass.tripEnded */ {
            StartsStop.setChecked(false);
        }

        /*
        * If not from coming from service, show dialog asking driving state.
        * if coming from service, or driving confirmed, show a nice UI with
        * */

        //Initializing the tablayout
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);

        //Adding the tabs using addTab() method
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_action_home));
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.ic_list));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);


        viewPager = (ViewPager) findViewById(R.id.pager);
        PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);

        //Adding onTabSelectedListener to swipe views
        tabLayout.setOnTabSelectedListener(this);

        viewPager.addOnPageChangeListener(
                new TabLayout.TabLayoutOnPageChangeListener(tabLayout)
        );

        StartsStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
               if (b) {
                   if(app.isTripInProgress())
                       // coming from dialog
                       //do nothing
                       ;
                   else {
                       if(checkPermissions()) {
                           app.setTripInProgress(true);
                           startLogger();
                           sendTripLoggingBroadcast(true);
                       }else {
                           StartsStop.setChecked(false);
                           requestPermissions();
                       }
                   }

                } else {
                   app.setTripInProgress(false);
                    //sendTripLoggingBroadcast(false);   // already being sent from the loggerservice
                    stopLogger();
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
                            showSnackbar(task.getResult().toString());
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
                buildDialog();
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

    void sendTripLoggingBroadcast(boolean status) {
        Intent iTemp = new Intent("tripstatus");
        iTemp.putExtra("LoggingStatus", status);
        LocalBroadcastManager l = LocalBroadcastManager.getInstance(this);
        l.sendBroadcast(iTemp);
    }




    void buildDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.dialog_question);
        builder.setPositiveButton(R.string.dialog_positive,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        app.setTripInProgress(true);
                        StartsStop.setChecked(true);
                        sendTripLoggingBroadcast(true);
                        startLogger();
                    }
                });
        builder.setIcon(R.drawable.ic_launcher);
        builder.setNegativeButton(R.string.dialog_negative,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // leave as it it
                    }
                });
        builder.show();
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
        if (id == R.id.actions_partners) {
            Intent intent = new Intent(this, PartnersActivity.class);
            startActivity(intent);
        }
        if (id == R.id.actions_login) {
            Intent intent = new Intent(this, LoginActivity.class);
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

    public void startLogger(){
        startService(loggerIntent);
    }

    public void stopLogger(){
        stopService(loggerIntent);
    }

}

// Make file names based on start-end location
// eg koramangala to indiranagar

//  https://coolors.co/6da34d-c9cba3-202030-39304a-52528c  for colours

// TODO : Internet checking
