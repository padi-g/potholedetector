package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;


import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
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

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.LoggerService;
import org.reapbenefit.gautam.intern.potholedetectorbeta.FileProcessorService;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments.EasyModeFragment;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments.TriplistFragment;
import org.reapbenefit.gautam.intern.potholedetectorbeta.PagerAdapter;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity
        implements TabLayout.OnTabSelectedListener,
        TriplistFragment.OnFragmentInteractionListener, EasyModeFragment.OnFragmentInteractionListener,
        EasyPermissions.PermissionCallbacks {


    private TabLayout tabLayout;
    private ViewPager viewPager;

    protected GoogleApiClient mGoogleApiClient;
    protected Location mCurrentLocation;

    private int activityType, confidence;

    private static Switch StartsStop;

    protected static final String TAG = "Main_Activity";
    private  static  final int REQUEST_CHECK_SETTINGS = 0x01;

    private Intent i;

    private FirebaseAuth mAuth;
    private Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Inside onCreate");

        setContentView(R.layout.activity_main);

        //Adding toolbar to the activity
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Road Quality Audit");

        mGoogleApiClient = ApplicationClass.getGoogleApiHelper().getGoogleApiClient();
        i = new Intent(MainActivity.this, LoggerService.class);
        settingsrequest();

        StartsStop = (Switch) findViewById(R.id.stopSwitch);
        StartsStop.setChecked(false);
        /*
        Intent intentFromService = getIntent();
        tripStarted = intentFromService.getBooleanExtra("CarMode", false); // false is default
        activityType = intentFromService.getIntExtra("ActivityType", -2);  // -2 is random.
        confidence = intentFromService.getIntExtra("Confidence", 0); // 0 is default
        */

        if (!ApplicationClass.tripInProgress && !ApplicationClass.tripEnded) {
            // this did not come from intent service
            // Show dialog asking to open
            buildDialog();
        } else if(ApplicationClass.tripInProgress && !ApplicationClass.tripEnded) {
            StartsStop.setChecked(true);
        }else if(ApplicationClass.tripInProgress && ApplicationClass.tripEnded) {
            // case is not possible
        } else /* !ApplicationClass.tripInProgress && ApplicationClass.tripEnded */ {
            StartsStop.setVisibility(View.GONE);
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

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser() == null){
            // Toast
            Toast.makeText(this, "Please login to start using the app", Toast.LENGTH_LONG).show();
            // open login activity
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
        }
        if(mAuth.getCurrentUser() != null)
            askPermissions();

        StartsStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
               if (b) {
                   if(ApplicationClass.tripInProgress)
                       // coming from dialog
                       //do nothing
                       ;
                   else {
                       ApplicationClass.tripInProgress = true;
                       startLogger();
                       sendTripLoggingBroadcast(true);
                   }

                } else {
                    ApplicationClass.tripInProgress = false;
                    //sendTripLoggingBroadcast(false);   // already being sent from the loggerservice
                    stopLogger();
                    StartsStop.setVisibility(View.GONE);

                   /*
                   Intent processor = new Intent(MainActivity.this, FileProcessorService.class);
                   startService(processor);
                   Log.d(TAG, "intent for file processor sent");
                    */
                   //MainActivity.this.finish();
                    //startActivityService();
                }
            }
        });

    }

    void sendTripLoggingBroadcast(boolean status) {
        Intent iTemp = new Intent("tripstatus");
        iTemp.putExtra("LoggingStatus", status);
        LocalBroadcastManager l = LocalBroadcastManager.getInstance(this);
        l.sendBroadcast(iTemp);
    }

    public void settingsrequest()
    {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true); //VERY IMPORTANT

        Task<LocationSettingsResponse> task =
                LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    // All location settings are satisfied. The client can initialize location
                    // requests here.
                    // continue
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be fixed by showing the
                            // user a dialog.
                            try {
                                // Cast to a resolvable exception.
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                // Show the dialog by calling startResolutionForResult(),
                                // and check the result in onActivityResult().
                                resolvable.startResolutionForResult(
                                        MainActivity.this,
                                        REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            } catch (ClassCastException e) {
                                // Ignore, should be an impossible error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have no way to fix the
                            // settings so we won't show the dialog.

                            break;
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
// Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // do nothing, continue
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(this, "The App needs your location to function properly", Toast.LENGTH_LONG).show();
                        settingsrequest();//keep asking if imp or do whatever
                        break;
                }
                break;
        }
    }



    void buildDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.dialog_question);
        builder.setPositiveButton(R.string.dialog_positive,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ApplicationClass.tripInProgress = true;
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
        startService(i);
    }

    public void stopLogger(){
        stopService(i);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {

        MainActivity.this.finish();
        System.exit(0);

    }

    @AfterPermissionGranted(1)
    private void askPermissions() {

        String[] perms = {android.Manifest.permission.ACCESS_FINE_LOCATION};
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            // ...
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, "This app will not function without the permissions asked for",
                    1, perms);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


}

// there is no check whether location enabled or not


// Make file names based on start-end location
// eg koramangala to indiranagar

//  https://coolors.co/6da34d-c9cba3-202030-39304a-52528c  for colours

// TODO : Internet checking
