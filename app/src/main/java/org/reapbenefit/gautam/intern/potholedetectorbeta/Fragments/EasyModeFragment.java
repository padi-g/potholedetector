package org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognitionClient;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MapsActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.LoggerService;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.SpeedWithLocation;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.S3UploadService;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static android.content.Context.MODE_PRIVATE;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link EasyModeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link EasyModeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

public class EasyModeFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    static public boolean tripStatus;
    static public Uri uploadFileUri;
    private View bgframe;
    private TextView statusIndicatorText;
    private FloatingActionButton startFloatingActionButton, stopFloatingActionButton;
    private Intent loggerIntent;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 389;

    private SharedPreferences arsPreferences;
    private boolean inCar;
    private Handler handler;
    private ActivityRecognitionClient activityRecognitionClient;
    private String currentActivity;
    private boolean currentlyInCar;
    private Chronometer chronometer;
    private SharedPreferences timePreferences;
    private SharedPreferences.Editor timePreferencesEditor;
    private boolean isChronometerRunning;
    private SharedPreferences animatorPreferences;
    private SharedPreferences.Editor animatorPreferencesEditor;

    private TreeMap<Integer, SpeedWithLocation> speedWithLocationTreeMap;

    ApplicationClass app;
    private int stoppedMilliseconds;

    public EasyModeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EasyModeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EasyModeFragment newInstance(String param1, String param2) {
        EasyModeFragment fragment = new EasyModeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        app = ApplicationClass.getInstance();
        tripStatus = app.isTripInProgress();    // TODO check if this interferes with the broadcast
        loggerIntent = new Intent(getActivity(), LoggerService.class);
        timePreferences = getActivity().getSharedPreferences("timePreferences", MODE_PRIVATE);
        timePreferencesEditor = timePreferences.edit();
        animatorPreferences = getActivity().getSharedPreferences("animatorPreferences", MODE_PRIVATE);
        animatorPreferencesEditor = animatorPreferences.edit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        LocalBroadcastManager l = LocalBroadcastManager.getInstance(getActivity());
        l.registerReceiver(b, new IntentFilter("tripstatus"));

        View v = inflater.inflate(R.layout.fragment_easy_mode, container, false);

        chronometer = v.findViewById(R.id.chronometer);

        inCar = getArguments().getBoolean("inCar", false);
        // Log.i(getClass().getSimpleName(), inCar + "");
        bgframe = (CoordinatorLayout) v.findViewById(R.id.easyframe);
        bgframe.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        statusIndicatorText = (TextView) v.findViewById(R.id.easytext);
        statusIndicatorText.setText(R.string.warnings);
        startFloatingActionButton = (FloatingActionButton) v.findViewById(R.id.start_trip_button);
        stopFloatingActionButton = (FloatingActionButton) v.findViewById(R.id.stop_trip_button);

        if(!app.isTripInProgress() && !app.isTripEnded()){
            startFloatingActionButton.setVisibility(View.VISIBLE);
            stopFloatingActionButton.setVisibility(View.GONE);
        }else if(app.isTripInProgress()){       // This case handles both the first trip and trips after that during the same app launch
            startFloatingActionButton.setVisibility(View.GONE);
            stopFloatingActionButton.setVisibility(View.VISIBLE);
            statusIndicatorText.setText(getResources().getString(R.string.detecting));
            ObjectAnimator animator = ObjectAnimator.ofFloat(chronometer, "translationY", -150.0f);
            animator.setDuration(500);
            animator.start();
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    statusIndicatorText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    statusIndicatorText.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });

        }else if(!app.isTripInProgress() && app.isTripEnded()) {
            statusIndicatorText.setText("Thanks for your contribution!");
            startFloatingActionButton.setVisibility(View.VISIBLE);
            stopFloatingActionButton.setVisibility(View.GONE);
        }

        startFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!app.isTripInProgress() && checkPermissions()) {
                    app.setTripInProgress(true);
                    timePreferencesEditor.putBoolean("isChronometerRunning", true).apply();
                    chronometer.setBase(SystemClock.elapsedRealtime());
                    chronometer.start();
                    ObjectAnimator animator = ObjectAnimator.ofFloat(chronometer, "translationY", -150.0f);
                    animator.setDuration(500);
                    animator.start();
                    animator.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            statusIndicatorText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                            statusIndicatorText.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    });
                    startLogger();
                    animatorPreferencesEditor.putBoolean("animated", true).apply();
                    statusIndicatorText.setText(getResources().getString(R.string.detecting));
                    startFloatingActionButton.setVisibility(View.GONE);
                    stopFloatingActionButton.setVisibility(View.VISIBLE);
                } else if (!checkPermissions()) {
                    requestPermissions();
                    startFloatingActionButton.setVisibility(View.VISIBLE);
                    stopFloatingActionButton.setVisibility(View.GONE);
                }
                else if (!inCar && !currentlyInCar) {
                    Toast.makeText(getContext(), "Logging cannot happen outside a vehicle", Toast.LENGTH_SHORT).show();
                }
            }
        });

        stopFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.setTripInProgress(false);
                timePreferencesEditor.putBoolean("isChronometerRunning", false).apply();
                chronometer.stop();
                stopLogger();
                animatorPreferencesEditor.putBoolean("animated", false).apply();
                statusIndicatorText.setVisibility(View.INVISIBLE);
                ObjectAnimator animator = ObjectAnimator.ofFloat(chronometer, "translationY", 0.0f);
                animator.setDuration(500);
                animator.start();
                startFloatingActionButton.setVisibility(View.VISIBLE);
                stopFloatingActionButton.setVisibility(View.GONE);
                statusIndicatorText.setText("");
            }
        });

        return v;
    }

    private Trip newTrip;
    private long tripDurationInSeconds;
    /*
    *   receives the broadcast from the logger service once the trip is ended
    *   Sets the bg to one of two states depending on whether the last trip was successful
    *   in reaching the desired location accuracy
    * */
    private final BroadcastReceiver b = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals("tripstatus")) {
                newTrip = intent.getParcelableExtra("trip_object");
                tripStatus = intent.getBooleanExtra("LoggingStatus", false);
                speedWithLocationTreeMap = (TreeMap<Integer, SpeedWithLocation>) intent.getSerializableExtra(getString(R.string.speed_with_location_hashmap));
                tripDurationInSeconds = intent.getLongExtra(getString(R.string.duration_in_seconds), 0);
                if (!tripStatus) {
                    uploadFileUri = intent.getParcelableExtra("filename");
                    if (uploadFileUri == null) {
                        Toast.makeText(getActivity().getApplicationContext(), "Sorry, we could not detect your location accurately", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity().getApplicationContext(), "Thanks for your contribution!", Toast.LENGTH_SHORT).show();
                        openMap();
                    }
                    ////////// redundant
                    startFloatingActionButton.setVisibility(View.VISIBLE);
                    stopFloatingActionButton.setVisibility(View.GONE);
                    ////////// redundant
                }
            }
        }
    };

    private void openMap(){
        Intent i = new Intent(this.getActivity(), MapsActivity.class);
        // Log.d(getClass().getSimpleName(), tripDurationInSeconds + "");
        i.putExtra("duration_in_seconds", tripDurationInSeconds);
        i.putExtra(getString(R.string.is_viewing_highest_pothole_trip), false);
        i.putExtra(getString(R.string.speed_with_location_hashmap), speedWithLocationTreeMap);
        // Log.d(getClass().getSimpleName(), new Gson().toJson(speedWithLocationTreeMap));
        startActivity(i);
    }


    // TODO : Just noticed that location may not update when hotspot is on. Check whether this is true even when user is outdoors and not using wifi routers to find location.


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
    }

    public void startLogger() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getActivity().startForegroundService(loggerIntent);
        }
        else
            getActivity().startService(loggerIntent);
    }

    public void stopLogger() {
        getActivity().stopService(loggerIntent);
    }



    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            // Log.i("EasyModeFragment", "Displaying permission rationale to provide additional context.");

            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            startLocationPermissionRequest();
                        }
                    });

        } else {
            // Log.i("EasyModeFragment", "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            startLocationPermissionRequest();
        }
    }

    private void startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(getActivity().findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (timePreferences != null) {
            isChronometerRunning = timePreferences.getBoolean("isChronometerRunning", false);
            if (isChronometerRunning && chronometer != null) {
                long startTime = timePreferences.getLong("startTime", SystemClock.elapsedRealtime());
                // Log.d("Chronometer new Base", String.valueOf(startTime));
                chronometer.setBase(startTime);
                chronometer.start();
            }
        }
    }

    @Override
        public void onPause() {
        super.onPause();
        // Log.d("Chronometer", "Pausing EMF");
        //getting current time of chronometer
        String currentTime = chronometer.getText().toString();
        String currentTimeArray[] = currentTime.split(":");
        if (currentTimeArray.length == 2) {
            stoppedMilliseconds = Integer.parseInt(currentTimeArray[0]) * 60 * 1000
                    + Integer.parseInt(currentTimeArray[1]) * 1000;
        } else if (currentTimeArray.length == 3) {
            stoppedMilliseconds = Integer.parseInt(currentTimeArray[0]) * 60 * 60 * 1000
                    + Integer.parseInt(currentTimeArray[1]) * 60 * 1000
                    + Integer.parseInt(currentTimeArray[2]) * 1000;
        }
        long startTime = SystemClock.elapsedRealtime() - stoppedMilliseconds;
        timePreferencesEditor.putLong("startTime", startTime)
                .apply();
        // Log.d("Chronometer set base", SystemClock.elapsedRealtime() - chronometer.getBase() + "");
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager l = LocalBroadcastManager.getInstance(getActivity());
        l.unregisterReceiver(b);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(b);
        super.onDestroy();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onFloatingActionButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}

// TODO Annoying glitch, there's a delay between the FloatingActionButton getting replaced and the nackground getting replaced from the broadcast
