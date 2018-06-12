package org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.TripViewModel;
import org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase.LocalTripEntity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.MapStateManager;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OverviewFragment extends Fragment implements
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback,
        com.google.android.gms.location.LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationSource.OnLocationChangedListener {

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private OnFragmentInteractionListener mListener;
    private MapView mapView;
    private LocationCallback locationCallback;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationRequest locationRequest;
    private Location currentLocation;
    private LatLng currentLatLng;
    private GoogleMap googleMap;
    private GoogleApiClient googleApiClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location lastLocation;
    private CameraPosition cameraPosition;
    private boolean zoomFlag;
    private static final String KEY_LOCATION = "keyLocation";
    private static final String CAMERA_POSITION = "cameraPosition";
    private FloatingActionButton starButton;
    private TripViewModel tripViewModel;
    private List<LatLng> potholeLocations = new ArrayList<>();
    private List<String> tripIds = new ArrayList<>();
    private List<LocalTripEntity> localTripEntities = new ArrayList<>();
    private HashMap<Integer, String> pointsOfInterest = new HashMap<>();
    private int locIndex;
    private LinearLayout bottomSheet;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tripViewModel = ViewModelProviders.of(this).get(TripViewModel.class);
        tripViewModel.getAllTrips().observe(getActivity(), new android.arch.lifecycle.Observer<List<LocalTripEntity>>() {
            @Override
            public void onChanged(@Nullable List<LocalTripEntity> localTripEntities) {
                if (localTripEntities != null)
                    OverviewFragment.this.localTripEntities = localTripEntities;
            }
        });
        new ProcessFileTask().execute(localTripEntities);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (googleMap != null) {
            outState.putParcelable(CAMERA_POSITION, googleMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastLocation);
            zoomFlag = true;
            super.onSaveInstanceState(outState);
        }
        if (mapView != null)
            mapView.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_overview, container, false);
        starButton = fragmentView.findViewById(R.id.personal_scores);
        if (savedInstanceState != null) {
            lastLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(CAMERA_POSITION);
            zoomFlag = true;
        }

        bottomSheet = fragmentView.findViewById(R.id.bottom_sheet);
        final BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        bottomSheet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {

            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                starButton.animate().scaleX(1 - slideOffset).scaleY(1 - slideOffset).setDuration(0).start();
            }
        });

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
        mapView = fragmentView.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        if (mapView != null)
            mapView.getMapAsync((OnMapReadyCallback) this);
        getMarkers();
        return fragmentView;
    }


    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void getMarkers() {
    }

    private void drawMarkers() {
        Iterator it = pointsOfInterest.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            potholeLocations.add(extractLatLng((String) pair.getValue()));
            it.remove(); // avoids a ConcurrentModificationException
        }
        if (potholeLocations != null) {
            for (LatLng potholeLocation: potholeLocations) {
                googleMap.addMarker(new MarkerOptions().position(potholeLocation));
            }
        }
    }

    private LatLng extractLatLng(String line) {
        String vals[] = line.split(",");
        LatLng l = new LatLng(Double.valueOf(vals[locIndex]), Double.valueOf(vals[locIndex+1]));
        return l;
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
                currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 18.0f));
            }
        };
    }

    @SuppressLint("RestrictedApi")
    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setFastestInterval(UPDATE_INTERVAL_IN_MILLISECONDS / 5);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        drawMarkers();
        googleApiClient = new GoogleApiClient.Builder(getActivity()).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            if (googleMap != null && !(ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                getMarkers();
            }
            mapView.onResume();
        }
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null)
            mapView.onPause();
        MapStateManager mapStateManager = new MapStateManager(getContext());
        if (googleMap != null)
            mapStateManager.saveMapState(googleMap);
        stopLocationUpdates();
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
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null)
            mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null)
            mapView.onLowMemory();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    private class ProcessFileTask extends AsyncTask<List<LocalTripEntity>, Void, Integer> {

        int lineNumber = 0, prevLineNumber = 0;
        FileInputStream is;
        int axisIndex, locIndex;
        private HashMap<Integer, String> pointsOfInterest = new HashMap<>();

        @Override
        protected Integer doInBackground(List<LocalTripEntity>... params) {
            for (int i = 0; i < params[0].size(); ++i) {
                String tripID = params[0].get(i).trip_id;
                File file = new File(getActivity().getApplicationContext().getFilesDir(), "logs/" + tripID + ".csv");
                try {
                    is = new FileInputStream(file);
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader bufferedReader = new BufferedReader(isr);
                    String line;
                    try {
                        //extracting the index of the value to be compared in a given line of data
                        line = bufferedReader.readLine();
                        String tokens[] = line.split(",");
                        for (int j = 0; j < tokens.length; j++) {
                            if (tokens[j].contains(params[0].get(i).axis)) {
                                axisIndex = j;
                            }
                            if (tokens[i].contains("latitude")) {
                                locIndex = j;
                            }
                        }

                        // populating our set of the points we are interested in
                        while ((line = bufferedReader.readLine()) != null) {
                            String values[] = line.split(",");
                            lineNumber++;
                            if (Float.valueOf(values[axisIndex]) > params[0].get(i).threshold && lineNumber > prevLineNumber + (params[0].get(i).no_of_lines * 3)) {
                                // this ignores the first period of data
                                pointsOfInterest.put(lineNumber, line);
                                prevLineNumber = lineNumber;
                            }
                        }

                    } catch (Exception e) {

                    }
                    return pointsOfInterest.size();
                } catch (FileNotFoundException e) {
                    return null;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            OverviewFragment.this.pointsOfInterest = pointsOfInterest;
            OverviewFragment.this.locIndex = locIndex;
            drawMarkers();
        }
    }
}
