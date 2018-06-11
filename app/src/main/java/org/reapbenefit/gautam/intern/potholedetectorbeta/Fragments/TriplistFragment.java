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
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import org.apache.commons.math3.distribution.LogisticDistribution;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.TripViewModel;
import org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase.LocalTripEntity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
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
    private ArrayList<Trip> highestPotholeTrips = new ArrayList<>();
    private String TAG = getClass().getSimpleName();

    private BroadcastReceiver newTripReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (tripViewModel != null) {
                Trip newTrip = intent.getParcelableExtra("trip_object");
                tripViewModel.insert(Trip.tripToLocalTripEntity(newTrip));
                Log.d(TAG, "Trip inserted " + newTrip.getTrip_id());
                offlineTrips.add(newTrip);
                createOfflineTripsListView();
            }
        }
    };

    private BroadcastReceiver uploadBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "broadcast received");
            if (tripViewModel != null) {
                Trip uploadedTrip = intent.getParcelableExtra("tripUploaded");
                tripViewModel.setUploaded(Trip.tripToLocalTripEntity(uploadedTrip));
                offlineTrips.remove(uploadedTrip);
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

        tripViewModel.getHighestPotholeTrips().observe(getActivity(), new Observer<List<LocalTripEntity>>() {
            @Override
            public void onChanged(@Nullable List<LocalTripEntity> localTripEntities) {
                ArrayList<Trip> highestPotholeTrips = new ArrayList<>();
                for (LocalTripEntity highestPotholeTrip: localTripEntities) {
                    Trip highPotholeTrip = Trip.localTripEntityToTrip(highestPotholeTrip);
                    highestPotholeTrips.add(highPotholeTrip);
                }
                TriplistFragment.this.highestPotholeTrips = highestPotholeTrips;
                //createHighestPotholeListView();
            }
        });
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
    }

    private long getTime(String date) throws ParseException{
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        Date d = sdf.parse(date);
        Log.d("Times", String.valueOf(d.getTime())+d.toString());
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
        Log.d(TAG, offlineTrips.toString());
        if(!offlineTrips.isEmpty() && getActivity()!=null) {
            Log.d(TAG, "inside OfflineTLV");
            Collections.sort(offlineTrips, comparator);
            recyclerAdapter = new TripListAdapter(getActivity(), offlineTrips, uploadStatus, 0, tripViewModel);
            recyclerView.setAdapter(recyclerAdapter);
            recyclerAdapter.notifyDataSetChanged();
        }
        else if (offlineTrips.isEmpty() && getActivity() != null) {
            Log.d(TAG, "inside OfflineTLV empty");
            Collections.sort(offlineTrips, comparator);
            recyclerAdapter = new TripListAdapter(getActivity(), offlineTrips, uploadStatus, 0, tripViewModel);
            recyclerView.setAdapter(recyclerAdapter);
            recyclerAdapter.notifyDataSetChanged();
        }
    }

    private void createHighestPotholeListView() {
        if(!highestPotholeTrips.isEmpty() && getActivity()!=null) {
            Collections.sort(highestPotholeTrips, comparator);
            recyclerAdapter = new TripListAdapter(getActivity(), offlineTrips, uploadStatus, 0, tripViewModel);
            recyclerView.setAdapter(recyclerAdapter);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(uploadBroadcastReceiver, new IntentFilter("SET_UPLOADED_TRUE"));
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        /*SharedPreferences dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        Set<String> newTripSet = dbPreferences.getStringSet("newTripJson", null);
        Set<String> uploadedTrips = dbPreferences.getStringSet("uploadedTrips", null);
        if (getActivity() != null)
            getActivity().registerReceiver(uploadBroadcastReceiver, new IntentFilter("SET_UPLOADED_TRUE"));
        if (isVisibleToUser) {
            if (newTripSet != null)
                new UpdateDataAsyncTask().execute();

            if (uploadedTrips != null)
                new UpdateTicksAsyncTask().execute();

            //reading SharedPreferences to check if user has logged out
            SharedPreferences logoutPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
            boolean loggedOut = logoutPreferences.getBoolean("loggedOut", false);
            Log.i(TAG, loggedOut + "");
            if (loggedOut) {
                if (tripViewModel == null) {
                    tripViewModel = ViewModelProviders.of(this).get(TripViewModel.class);
                }
                tripViewModel.deleteAll();
            }
        }
        else {
            if (getActivity() != null) {
                getActivity().registerReceiver(uploadBroadcastReceiver, new IntentFilter("SET_UPLOADED_TRUE"));
            }
        }*/
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
        createOfflineTripsListView();
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
                Log.d("Times", e.getMessage());
                return 0;
            }
        }
    }

    private class UpdateDataAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            //reading for new trip registered by LoggerService
            Set<String> newTripSet = dbPreferences.getStringSet("newTripJson", null);
            List<String> newTripJson = new ArrayList<>();
            if (newTripSet != null)
                newTripJson = new ArrayList<>(newTripSet);
            if (newTripJson != null && tripViewModel != null) {
                for (int i = 0; i < newTripJson.size(); ++i) {
                    Trip newTrip = new Gson().fromJson(newTripJson.get(i), Trip.class);
                    tripViewModel.insert(Trip.tripToLocalTripEntity(newTrip));
                }

                    try {
                        tripViewModel.getAllTrips().observe(getActivity(), new Observer<List<LocalTripEntity>>() {
                            @Override
                            public void onChanged(@Nullable List<LocalTripEntity> localTripEntities) {
                                ArrayList<Trip> latestTrips = new ArrayList<>();
                                for (int i = 0; i < localTripEntities.size(); ++i) {
                                    Trip trip = Trip.localTripEntityToTrip(localTripEntities.get(i));
                                    Log.i(TAG, i + " " + new Gson().toJson(trip.toString()));
                                    latestTrips.add(trip);
                                }
                                offlineTrips = latestTrips;
                            }
                        });
                    } catch (IllegalStateException e) {
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            recyclerAdapter = new TripListAdapter(getActivity(), offlineTrips, uploadStatus, null, tripViewModel);
            Collections.sort(offlineTrips, new CustomTripComparator());
            recyclerView.setAdapter(recyclerAdapter);
        }
    }

    private class UpdateTicksAsyncTask extends AsyncTask<Void, Void, Void> {

        private LocalTripEntity tripEntityUploaded;
        private boolean uploadStatus;
        private boolean autoUpload;
        private ArrayList<String> trip_ids = new ArrayList<>();
        @Override
        protected Void doInBackground(Void... voids) {
            //reading SharedPreferences to see if a recent upload has happened
            Set<String> uploadedTrips = dbPreferences.getStringSet("uploadedTrips", null);
            if (uploadedTrips == null)
                return null;
            Log.d("TLF UTS", uploadedTrips.toString());
            if (tripViewModel != null) {
                List<LocalTripEntity> localTripEntityList = tripViewModel.getAllTrips().getValue();
                for (int i = localTripEntityList.size() - 1; i >= 0; --i) {
                    if (uploadedTrips.contains(localTripEntityList.get(i).trip_id)) {
                        //ID matched with trip in database, must update ticks of trip with this ID
                        LocalTripEntity matchedTripEntity = localTripEntityList.get(i);
                        matchedTripEntity.uploaded = true;
                        tripViewModel.insert(matchedTripEntity);
                        offlineTrips.add(Trip.localTripEntityToTrip(matchedTripEntity));
                    }
                }
            }
            dbPreferences.edit().putStringSet("uploadedTrips", null).commit();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            recyclerAdapter = new TripListAdapter(getActivity(), offlineTrips, uploadStatus, trip_ids, tripViewModel);
            Collections.sort(offlineTrips, new CustomTripComparator());
            recyclerView.setAdapter(recyclerAdapter);
            recyclerAdapter.notifyDataSetChanged();
        }
    }
}
