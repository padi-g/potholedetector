package org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import org.reapbenefit.gautam.intern.potholedetectorbeta.BuildConfig;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.TripViewModel;
import org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase.LocalTripEntity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.S3UploadService;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;
import org.reapbenefit.gautam.intern.potholedetectorbeta.TripListAdapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TriplistFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    private ArrayList<Trip> offlineTrips = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    //TODO: CHECK IF TIMESCORE AND DISTANCESCORE NEED TO BE REIMPLEMENTED
    private long timeScore = 0;  // calculated based on time logged
    private long distanceScore = 0;  // calculated based on distance logged
    private CustomTripComparator comparator;
    ApplicationClass app;
    private boolean uploadStatus;
    private int positionChanged;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter recyclerAdapter;
    private RecyclerView.LayoutManager recyclerLayoutManager;
    private TriplistFragment triplistFragment = this;
    private SharedPreferences dbPreferences;
    private TripViewModel tripViewModel;
    private Trip highestPotholeTrip;
    private String tripUploadedId = null;
    private ImageButton uploadAllButton;
    private String TAG = getClass().getSimpleName();
    private int maxPotholeCount;

    private BroadcastReceiver newTripReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (tripViewModel != null) {
                Trip newTrip = intent.getParcelableExtra("trip_object");
                if (newTrip.getDistanceInKM() < 0.5 && !BuildConfig.DEBUG)
                    return;
                tripViewModel.insert(Trip.tripToLocalTripEntity(newTrip));
                // Log.d(TAG, "Trip inserted " + newTrip.getTrip_id());
                offlineTrips.add(newTrip);
                maxPotholeCount = dbPreferences.getInt("maxPotholeCount", 0);
                if (newTrip.getProbablePotholeCount() + newTrip.getDefinitePotholeCount() >= maxPotholeCount) {
                    // Log.d(TAG, "inside if probablePotholeCount");
                    highestPotholeTrip = newTrip;
                    dbPreferences.edit().putString("highestPotholeTrip", new Gson().toJson(highestPotholeTrip)).apply();
                    maxPotholeCount = newTrip.getProbablePotholeCount() + newTrip.getDefinitePotholeCount();
                    dbPreferences.edit().putInt("maxPotholeCount", maxPotholeCount).apply();
                }
                createOfflineTripsListView();
            }
        }
    };

    private BroadcastReceiver uploadBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Log.d("uploadBroadcast", "broadcast received");
            if (tripViewModel != null) {
                Trip uploadedTrip = intent.getParcelableExtra("tripUploaded");
                uploadedTrip.setUploaded(true);
                tripViewModel.insert(Trip.tripToLocalTripEntity(uploadedTrip));
                // Log.d("tripUploaded", new Gson().toJson(uploadedTrip));
                offlineTrips.remove(intent.getParcelableExtra("tripUploaded"));
                createOfflineTripsListView();
            }
        }
    };

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tripViewModel = ViewModelProviders.of(this).get(TripViewModel.class);

        tripViewModel.getOfflineTrips().observe(getActivity(), new Observer<List<LocalTripEntity>>() {
            @Override
            public void onChanged(@Nullable List<LocalTripEntity> localTripEntities) {
                Set<Trip> tripSet = new HashSet<>();
                for (LocalTripEntity offlineTripEntity: localTripEntities) {
                    Trip offlineTrip = Trip.localTripEntityToTrip(offlineTripEntity);
                    tripSet.add(offlineTrip);
                }
                offlineTrips = new ArrayList<>(tripSet);
                createOfflineTripsListView();
            }
        });
        dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        tripUploadedId = dbPreferences.getString("tripUploadedId", null);
        if (tripUploadedId != null) {
            uploadStatus = true;
            createOfflineTripsListView();
        }
        // Log.d(TAG, "tripUploaded " + tripUploadedId);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = ApplicationClass.getInstance();
        comparator = new CustomTripComparator();
        sharedPreferences = getActivity().getSharedPreferences("uploads", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(uploadBroadcastReceiver, new IntentFilter(getString(R.string.set_uploaded_true)));
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(newTripReceiver, new IntentFilter(getString(R.string.new_trip_insert)));
        dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        maxPotholeCount = dbPreferences.getInt("maxPotholeCount", 0);
    }

    private long getTime(String date) throws ParseException{
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        Date d = sdf.parse(date);
        // Log.d("Times", String.valueOf(d.getTime())+d.toString());
        return d.getTime();
    }

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

    private void createOfflineTripsListView(){
        // Log.d(TAG, offlineTrips.toString());
        if(!offlineTrips.isEmpty() && getActivity()!=null) {
            // Log.d(TAG, "inside OfflineTLV");
            Collections.sort(offlineTrips, comparator);
            // Log.d("OfflineTLV", new Gson().toJson(offlineTrips));
            recyclerAdapter = new TripListAdapter(getActivity(), offlineTrips, uploadStatus, tripUploadedId, tripViewModel, getActivity().getBaseContext());
            recyclerView.setAdapter(recyclerAdapter);
            recyclerAdapter.notifyDataSetChanged();
        }
        else if (offlineTrips.isEmpty() && getActivity() != null) {
            // Log.d(TAG, "inside OfflineTLV empty");
            Collections.sort(offlineTrips, comparator);
            recyclerAdapter = new TripListAdapter(getActivity(), offlineTrips, uploadStatus, tripUploadedId, tripViewModel, getActivity().getBaseContext());
            recyclerView.setAdapter(recyclerAdapter);
            recyclerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            String tripUploadedJson = dbPreferences.getString("tripUploaded", null);
            // Log.d(getClass().getSimpleName(), tripUploadedJson + "");
            if (tripUploadedJson != null) {
                //initiating DB update through TripViewModel
                // Log.d("uploadBroadcast", "broadcast received");
                Trip uploadedTrip = new Gson().fromJson(tripUploadedJson, Trip.class);
                if (tripViewModel != null) {
                    try {
                        // Log.d("uploadBroadcast", "inside try");
                        Trip tempTrip = uploadedTrip;
                        tempTrip.setUploaded(true);
                        tripViewModel.insert(Trip.tripToLocalTripEntity(tempTrip));
                        // Log.d("tripUploaded", new Gson().toJson(uploadedTrip));
                        offlineTrips.remove(uploadedTrip);
                    } catch (Exception e) {}
                }
                dbPreferences.edit().putString("tripUploaded", null).apply();
            }
            createOfflineTripsListView();
            //reading SharedPreferences to check if user has logged out
            SharedPreferences logoutPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
            boolean loggedOut = logoutPreferences.getBoolean("loggedOut", false);
            // Log.i(TAG, loggedOut + "");
            if (loggedOut) {
                if (tripViewModel == null) {
                    tripViewModel = ViewModelProviders.of(this).get(TripViewModel.class);
                }
                tripViewModel.deleteAll();
            }
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_triplist, container, false);

        recyclerView = v.findViewById(R.id.trips_list);
        recyclerView.setHasFixedSize(true);
        recyclerLayoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(recyclerLayoutManager);
        dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        createOfflineTripsListView();
        uploadAllButton = (ImageButton) v.findViewById(R.id.upload_all_button);
        uploadAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent uploadAllIntent = new Intent(getContext(), S3UploadService.class);
                uploadAllIntent.putExtra("trip_arrayList", offlineTrips);
                dbPreferences.edit().putBoolean("batchUpload", true).commit();
                getContext().startService(uploadAllIntent);
                createOfflineTripsListView();
            }
        });
        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(uploadBroadcastReceiver);
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(newTripReceiver);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        }
        else
            throw new RuntimeException(context.toString() + "must implement OnFragmentInteractionListener");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public class CustomTripComparator implements Comparator<Trip> {
        @Override
        public int compare(Trip o2, Trip o1) {
            try{
                return Long.valueOf(getTime(o1.getEndTime())).compareTo(Long.valueOf(getTime(o2.getEndTime())));
            }catch (ParseException e){
                // Log.d("Times", e.getMessage());
                return 0;
            }
        }
    }

    public TripViewModel getTripViewModel() {
        return tripViewModel;
    }
}
