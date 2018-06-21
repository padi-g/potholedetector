package org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.appsee.Appsee;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MapsActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.APIService;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.TripViewModel;
import org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase.LocalTripEntity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.MapStateManager;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;
import org.reapbenefit.gautam.intern.potholedetectorbeta.TripListAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private FloatingActionButton groupButton;
    private TripViewModel tripViewModel;
    private List<LatLng> potholeLocations = new ArrayList<>();
    private List<String> tripIds = new ArrayList<>();
    private List<LocalTripEntity> localTripEntities = new ArrayList<>();
    private List<LatLng> probableLatLngList = new ArrayList<>();
    private List<LatLng> definiteLatLngList = new ArrayList<>();
    private int locIndex;
    private LinearLayout bottomSheet;
    private TextView bottomSheetText;
    private SharedPreferences tripStatsPreferences;
    private RecyclerView highestPotholeListView;
    private Trip highestPotholeTrip;
    private TripListAdapter highestPotholeAdapter;
    private int definitePotholeCount;
    private int probablePotholeCount;
    private TextView startTimeTextView;
    private TextView countTextView;
    private TextView distanceTextView;
    private TextView sizeTextView;
    private GridLayout mostPotholesGrid;
    private FloatingActionButton floatingButton;
    private LatLng[] uniquePotholeLatLng;
    private final String TAG = getClass().getSimpleName();

    private BroadcastReceiver uniquePotholesLatLngReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("OverviewFragment", "Broadcast received");
            uniquePotholeLatLng = (LatLng[]) intent.getParcelableArrayExtra(getString(R.string.global_unique_pothole_locations));
        }
    };

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
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent fetchGlobalPotholeIntent = new Intent(getContext(), APIService.class);
        fetchGlobalPotholeIntent.putExtra("request", "GET");
        fetchGlobalPotholeIntent.putExtra("table", "UniquePotholes");
        getContext().startService(fetchGlobalPotholeIntent);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        tripStatsPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        String highestPotholeTripJson = tripStatsPreferences.getString("highestPotholeTrip", null);
        // Log.d(getClass().getSimpleName(), highestPotholeTripJson + "");
        if (highestPotholeTripJson != null) {
            highestPotholeTrip = new Gson().fromJson(highestPotholeTripJson, Trip.class);
        }
        zoomFlag = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (googleMap != null) {
            outState.putParcelable(CAMERA_POSITION, googleMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, lastLocation);
            // zoomFlag = true;
            super.onSaveInstanceState(outState);
        }
        if (mapView != null)
            mapView.onSaveInstanceState(outState);
    }

    @SuppressLint("ResourceType")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_overview, container, false);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(uniquePotholesLatLngReceiver, new IntentFilter(getString(R.string.global_unique_pothole_locations)));
        starButton = fragmentView.findViewById(R.id.personal_scores);
        groupButton = fragmentView.findViewById(R.id.group_scores);
        if (starButton.getVisibility() == View.VISIBLE)
            floatingButton = starButton;
        else
            floatingButton = groupButton;

        if (savedInstanceState != null) {
            lastLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            cameraPosition = savedInstanceState.getParcelable(CAMERA_POSITION);
            // zoomFlag = true;
        }

        starButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                groupButton.setVisibility(View.VISIBLE);
                starButton.setVisibility(View.INVISIBLE);
                floatingButton = groupButton;
                if (googleMap != null) {
                    googleMap.clear();
                    populatePersonalMap();
                }
            }
        });

        groupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                groupButton.setVisibility(View.INVISIBLE);
                starButton.setVisibility(View.VISIBLE);
                floatingButton = starButton;
                if (googleMap != null) {
                    googleMap.clear();
                    populateGlobalMap();
                }
            }
        });


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
                if (floatingButton != null) {
                    Log.d(TAG, String.valueOf((floatingButton.equals(groupButton))));
                    floatingButton.animate().scaleX(1 - slideOffset).scaleY(1 - slideOffset).setDuration(0).start();
                }
            }
        });

        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();
        mapView = fragmentView.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        if (mapView != null)
            mapView.getMapAsync((OnMapReadyCallback) this);

        definitePotholeCount = tripStatsPreferences.getInt("definitePotholes", 0);
        probablePotholeCount = tripStatsPreferences.getInt("probablePotholes", 0);

        bottomSheetText = fragmentView.findViewById(R.id.overview_sheet_text);
        String bottomSheetString = tripStatsPreferences.getInt("validTrips", 0) + " trips taken" +
                "\n" + definitePotholeCount + " definite potholes" +
                "\n" + probablePotholeCount + " probable potholes";
        bottomSheetText.setText(bottomSheetString);

        startTimeTextView = fragmentView.findViewById(R.id.start_time);
        countTextView = fragmentView.findViewById(R.id.count);
        distanceTextView = fragmentView.findViewById(R.id.distance_view);
        sizeTextView = fragmentView.findViewById(R.id.size);
        if (highestPotholeTrip != null) {
            //adding details of highestPotholeTrip to GridLayout
            String startTime = highestPotholeTrip.getStartTime();
            startTime = startTime.substring(4, startTime.indexOf("GMT") - 4);
            startTimeTextView.setText(startTime);
            countTextView.setText(highestPotholeTrip.getProbablePotholeCount() +
                    highestPotholeTrip.getDefinitePotholeCount() + " potholes");
            distanceTextView.setText(TripListAdapter.roundTwoDecimals(highestPotholeTrip.getDistanceInKM()) + "km");
            sizeTextView.setText(TripListAdapter.humanReadableByteCount(highestPotholeTrip.getFilesize(), true));

            mostPotholesGrid = fragmentView.findViewById(R.id.highest_pothole_grid);
            mostPotholesGrid.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = new Intent(getContext(), MapsActivity.class);
                    i.putExtra("trip", highestPotholeTrip);
                    i.putExtra(getString(R.string.is_viewing_highest_pothole_trip), true);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getContext().startActivity(i);
                }
            });
        }

        //getting Set<String> from SharedPreferences
        Set<String> probablePotholeLocationSet = tripStatsPreferences.getStringSet(getString(R.string.probable_pothole_location_set), new HashSet<String>());
        Set<String> definitePotholeLocationSet = tripStatsPreferences.getStringSet(getString(R.string.definite_pothole_location_set), new HashSet<String>());
        if (probablePotholeLocationSet != null) {
            List<String> probablePotholeLocationArrayList = new ArrayList<>(probablePotholeLocationSet);
            for (String potholeLocationString: probablePotholeLocationArrayList) {
                probableLatLngList.add(new Gson().fromJson(potholeLocationString, LatLng.class));
            }
        }
        // Log.d(getClass().getSimpleName(), probableLatLngList.toString());
        if (definitePotholeLocationSet != null) {
            List<String> definitePotholeLocationArrayList = new ArrayList<>(definitePotholeLocationSet);
            for (String definitePotholeLocationString: definitePotholeLocationArrayList) {
                definiteLatLngList.add(new Gson().fromJson(definitePotholeLocationString, LatLng.class));
            }
        }
        drawMarkers();
        // Log.d(getClass().getSimpleName(), definiteLatLngList.toString());
        return fragmentView;
    }

    private void drawMarkers() {
        if (starButton.getVisibility() == View.VISIBLE) {
            populateGlobalMap();
        }
        else {
            populatePersonalMap();
        }
    }

    private void populateGlobalMap() {
        if (googleMap != null && uniquePotholeLatLng != null) {
            for (int i = 0; i < uniquePotholeLatLng.length; ++i) {
                googleMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker())
                .position(uniquePotholeLatLng[i]));
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private void populatePersonalMap() {
        if (googleMap != null) {
            for (LatLng potholeLocation : definiteLatLngList) {
                googleMap.addMarker(new MarkerOptions().position(potholeLocation).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            }
        }
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
                Appsee.setLocation(currentLocation.getLatitude(), currentLocation.getLongitude(), currentLocation.getAccuracy(), currentLocation.getAccuracy());
                currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                googleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
                    @Override
                    public void onCameraMoveStarted(int i) {
                        if (i == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                            //executed when the user changes map camera position
                            zoomFlag = false;
                        }
                    }
                });
                if (zoomFlag)
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
            }
            mapView.onResume();
        }
        drawMarkers();
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
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(uniquePotholesLatLngReceiver);
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
}
