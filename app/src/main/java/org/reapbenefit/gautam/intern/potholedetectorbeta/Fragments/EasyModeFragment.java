package org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MainActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MapsActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.LoggerService;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.S3UploadSevice;
import org.w3c.dom.Text;

import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.*;

import java.util.List;

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
    private Button startButton, stopButton;
    private Intent loggerIntent;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 389;

    ApplicationClass app;

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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        LocalBroadcastManager l = LocalBroadcastManager.getInstance(getActivity());
        l.registerReceiver(b, new IntentFilter("tripstatus"));

        View v = inflater.inflate(R.layout.fragment_easy_mode, container, false);

        bgframe = (RelativeLayout) v.findViewById(R.id.easyframe);
        statusIndicatorText = (TextView) v.findViewById(R.id.easytext);
        statusIndicatorText.setText(R.string.warnings);
        startButton = (Button) v.findViewById(R.id.start_trip_button);
        stopButton = (Button) v.findViewById(R.id.stop_trip_button);

        if(!app.isTripInProgress() && !app.isTripEnded()){
            startButton.setVisibility(View.VISIBLE);
            stopButton.setVisibility(View.GONE);
            bgframe.setBackgroundResource(R.drawable.notlogging_bg);
        }else if(app.isTripInProgress()){       // This case handles both the first trip and trips after that during the same app launch
            startButton.setVisibility(View.GONE);
            stopButton.setVisibility(View.VISIBLE);
            bgframe.setBackgroundResource(R.drawable.logging_bg);
            statusIndicatorText.setText(getResources().getString(R.string.detecting));
        }else if(!app.isTripInProgress() && app.isTripEnded()) {
            statusIndicatorText.setText("Thanks for your contribution!");
        }

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!app.isTripInProgress() && checkPermissions()) {
                    app.setTripInProgress(true);
                    startLogger();
                    bgframe.setBackgroundResource(R.drawable.logging_bg);
                    statusIndicatorText.setText(getResources().getString(R.string.detecting));
                    startButton.setVisibility(View.GONE);
                    stopButton.setVisibility(View.VISIBLE);
                } else if(!checkPermissions()) {
                    requestPermissions();
                    startButton.setVisibility(View.VISIBLE);
                    stopButton.setVisibility(View.GONE);
                    bgframe.setBackgroundResource(R.drawable.notlogging_bg);
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.setTripInProgress(false);
                stopLogger();
                startButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.GONE);
                statusIndicatorText.setText("");
                bgframe.setBackgroundResource(R.drawable.notlogging_bg);
            }
        });

        return v;
    }

    /*
    *   receives the broadcast from the logger service once the trip is ended
    *   Sets the bg to one of two states depending on whether the last trip was successful
    *   in reaching the desired location accuracy
    * */
    private final BroadcastReceiver b = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            tripStatus = intent.getBooleanExtra("LoggingStatus", false);
            if(!tripStatus){
                uploadFileUri = intent.getParcelableExtra("filename");
                if(uploadFileUri == null){
                    statusIndicatorText.setText("Sorry, we could not detect your location accurately");
                }else {
                    Log.d("Upload", "file received is" + String.valueOf(uploadFileUri));
                    statusIndicatorText.setText("Thanks for your contribution!");
                    if(internetAvailable() && autoUploadOn()) {
                        startUploadService();
                    }else if(!internetAvailable()){
                        Toast.makeText(getActivity().getApplicationContext(), "Internet not available. You can upload manually later", Toast.LENGTH_LONG).show();
                    }else if(!autoUploadOn())
                        Toast.makeText(getActivity().getApplicationContext(), "Auto Upload is turned off. You can upload manually later", Toast.LENGTH_LONG).show();
                        Toast.makeText(getActivity().getApplicationContext(), "Auto Upload is turned off. You can upload manually later", Toast.LENGTH_LONG).show();
                    openMap();
                }
                ////////// redundant
                bgframe.setBackgroundResource(R.drawable.notlogging_bg);
                startButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.GONE);
                ////////// redundant
            }
        }
    };

    private boolean internetAvailable(){
        ConnectivityManager connMgr = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean isWifiConn = networkInfo.isConnected();
        networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean isMobileConn = networkInfo.isConnected();
        if(isMobileConn || isWifiConn)
            return true;
        else
            return false;
    }

    private boolean autoUploadOn(){
        SharedPreferences prefs = getActivity().getSharedPreferences("uploads", MODE_PRIVATE);
        return prefs.getBoolean("auto_upload", true);
    }

    public void startUploadService(){
        Intent intent = new Intent(getContext(), S3UploadSevice.class);
        //Intent intent = new Intent(getContext(), UploadTasksService.class);
        intent.setAction("upload_now");
        intent.putExtra("upload_uri", uploadFileUri);
        this.getContext().startService(intent);

    }

    private void openMap(){
        Intent i = new Intent(this.getActivity(), MapsActivity.class);
        startActivity(i);
    }


    // TODO : Just noticed that location may not update when hotspot is on. Check whether this is true even when user is outdoors and not using wifi routers to find location.


    public void startLogger(){
        getActivity().startService(loggerIntent);
    }

    public void stopLogger(){
        getActivity().stopService(loggerIntent);
    }



    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i("EasyModeFragment", "Displaying permission rationale to provide additional context.");

            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            startLocationPermissionRequest();
                        }
                    });

        } else {
            Log.i("EasyModeFragment", "Requesting permission");
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

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(getActivity(),
                android.Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager l = LocalBroadcastManager.getInstance(getActivity());
        l.unregisterReceiver(b);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
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

// TODO Annoying glitch, there's a delay between the button getting replaced and the nackground getting replaced from the broadcast
