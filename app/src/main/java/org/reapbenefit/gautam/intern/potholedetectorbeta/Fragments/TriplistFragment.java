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
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.TripViewModel;
import org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase.AsyncTaskInterfaces;
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
import java.util.List;
import java.util.Locale;

public class TriplistFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private ListView l;
    private ImageButton refreshButton;

    private ArrayList<Trip> trips = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

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

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (recyclerView != null) {
                uploadStatus = true;
                positionChanged = intent.getIntExtra("positionChanged", -1);
                if (tripViewModel == null)
                    tripViewModel = ViewModelProviders.of(triplistFragment).get(TripViewModel.class);
                recyclerAdapter = new TripListAdapter(getActivity(), trips, uploadStatus, positionChanged, tripViewModel);
                Collections.sort(trips, new CustomTripComparator());
                recyclerView.setAdapter(recyclerAdapter);
                recyclerAdapter.notifyDataSetChanged();
            }
        }
    };

    private TripViewModel tripViewModel;
    private String TAG = getClass().getSimpleName();
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        tripViewModel = ViewModelProviders.of(this).get(TripViewModel.class);
        tripViewModel.getAllTrips().observe(getActivity(), new Observer<List<LocalTripEntity>>() {
            @Override
            public void onChanged(@Nullable List<LocalTripEntity> localTripEntities) {
                ArrayList<Trip> latestTrips = new ArrayList<>();
                for (int i = 0; i < localTripEntities.size(); ++i) {
                    Trip trip = Trip.localTripEntityToTrip(localTripEntities.get(i));
                    Log.i(TAG, i + " " + new Gson().toJson(trip.toString()));
                    latestTrips.add(trip);
                }
                trips = latestTrips;
                Collections.sort(trips, new CustomTripComparator());
                recyclerAdapter = new TripListAdapter(getContext().getApplicationContext(), trips,
                        uploadStatus, positionChanged, tripViewModel);
                recyclerView.setAdapter(recyclerAdapter);
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        app = ApplicationClass.getInstance();
        comparator = new CustomTripComparator();
        sharedPreferences = getActivity().getSharedPreferences("uploads", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
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

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(broadcastReceiver);
    }

    private void createListView(){
        if(!trips.isEmpty() && getActivity()!=null) {
            Collections.sort(trips, new CustomTripComparator());
            recyclerAdapter = new TripListAdapter(getActivity(), trips, uploadStatus, 0, tripViewModel);
            //Collections.sort(trips, new CustomTripComparator());
            recyclerView.setAdapter(recyclerAdapter);
            recyclerAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(broadcastReceiver, new IntentFilter("SET_UPLOADED_TRUE"));
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            new UpdateDataAsyncTask().execute();
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
        refreshButton = (ImageButton) v.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createListView();
                if(internetAvailable() && !trips.isEmpty())
                    Toast.makeText(getActivity(), "Refreshed", Toast.LENGTH_SHORT).show();
                else if(trips.isEmpty())
                    Toast.makeText(getActivity(), "Refreshing", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getActivity(), "Internet not available, try later", Toast.LENGTH_SHORT).show();

            }
        });

        if (broadcastReceiver != null)
            getContext().registerReceiver(broadcastReceiver, new IntentFilter("SET_UPLOADED_TRUE"));
        else
            Log.i(getClass().getSimpleName(), "broadcast receiver null");
        Log.i(getClass().getSimpleName(), "uploaded status " + uploadStatus);
        return v;
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

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
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
            SharedPreferences dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
            String newTripJson = dbPreferences.getString("newTripJson", null);
            if (newTripJson != null && tripViewModel != null) {
                Trip newTrip = new Gson().fromJson(newTripJson, Trip.class);
                tripViewModel.insert(Trip.tripToLocalTripEntity(newTrip));
                //setting SharedPreferences back to null
                dbPreferences.edit().putString("newTripJson", null).commit();
                tripViewModel.getAllTrips().observe(getActivity(), new Observer<List<LocalTripEntity>>() {
                    @Override
                    public void onChanged(@Nullable List<LocalTripEntity> localTripEntities) {
                        ArrayList<Trip> latestTrips = new ArrayList<>();
                        for (int i = 0; i < localTripEntities.size(); ++i) {
                            Trip trip = Trip.localTripEntityToTrip(localTripEntities.get(i));
                            Log.i(TAG, i + " " + new Gson().toJson(trip.toString()));
                            latestTrips.add(trip);
                        }
                        trips = latestTrips;
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            recyclerAdapter = new TripListAdapter(getActivity(), trips, uploadStatus, 0, tripViewModel);
            Collections.sort(trips, new CustomTripComparator());
            recyclerView.setAdapter(recyclerAdapter);
            recyclerAdapter.notifyDataSetChanged();
        }
    }
}
