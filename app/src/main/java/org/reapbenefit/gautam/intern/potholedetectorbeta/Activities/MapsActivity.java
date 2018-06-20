package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DatabaseReference;
import com.google.gson.Gson;

import org.reapbenefit.gautam.intern.potholedetectorbeta.BuildConfig;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.SpeedWithLocation;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    MapFragment mapFragment;
    private ArrayList<LatLng> latLngs = new ArrayList<>();
    private ArrayList<LatLng> probablePotholeLatLngs = new ArrayList<>();
    private ArrayList<LatLng> definitePotholeLatLngs = new ArrayList<>();
    private InputStream inputStream;
    private FirebaseAnalytics mFirebaseAnalytics;
    private DatabaseReference db;
    private final float DEFINITE_THRESHOLD_SPEED_METRES_PER_SECOND = 5.55f;
    private final float PROBABLE_THRESHOLD_SPEED_METRES_PER_SECOND = 1.38f;
    private ProgressBar spinner;
    private TextView date, distance, duration, probablePotholeCountTextView, textview, trafficTime;
    private SeekBar accuracySeekbar;
    private Button submitButton;
    private String tripID;
    private int linesPerPeriod;
    private float threshold;
    private String axisOfInterest;
    private int axisIndex, locIndex, speedIndex;
    private Trip finishedTrip;
    private HashMap<Integer, String> probablePointsOfInterest = new HashMap<>();
    private HashMap<Integer, String> definitePointsOfInterest = new HashMap<>();
    private int accuracy_result = 0;
    private GridLayout resultGrid;
    private TextView accuracyLowTime;
    private SharedPreferences tripStatsPreferences;
    private SharedPreferences.Editor tripStatsEditor;
    private Set<String> tripIdSet;
    private LinkedHashMap<Integer, SpeedWithLocation> speedWithLocationHashMap;
    private final String TAG = getClass().getSimpleName();

    private SharedPreferences dbPreferences;
    private SharedPreferences.Editor dbPreferencesEditor;
    private TextView definitePotholeCountTextView;

    private BroadcastReceiver displayHighestPotholeTripReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //reading SharedPreferences to get highestPotholeTrip data
            String highestPotholeTripJson = dbPreferences.getString("highestPotholeTrip", null);
            if (highestPotholeTripJson != null) {
                Trip highestPotholeTrip = new Gson().fromJson(highestPotholeTripJson, Trip.class);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        tripStatsPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        tripStatsEditor = tripStatsPreferences.edit();

        dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        dbPreferencesEditor = dbPreferences.edit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Trip Summary");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        logAnalytics("map_opened");

        Intent i = getIntent();
        finishedTrip = i.getParcelableExtra("trip");
        if (finishedTrip == null) {
            finishedTrip = ApplicationClass.getInstance().getTrip();
        }

        tripID = finishedTrip.getTrip_id();
        linesPerPeriod = finishedTrip.getNo_of_lines() * 3;
        threshold = finishedTrip.getThreshold() * 9;
        axisOfInterest = finishedTrip.getAxis();

        resultGrid = (GridLayout) findViewById(R.id.map_result_grid);
        spinner = (ProgressBar) findViewById(R.id.indeterminateBar);
        date = (TextView) findViewById(R.id.tripdate);
        duration = (TextView) findViewById(R.id.duration);
        distance = (TextView) findViewById(R.id.distance);
        probablePotholeCountTextView = (TextView) findViewById(R.id.probablepotholecount);
        definitePotholeCountTextView = (TextView) findViewById(R.id.definitepotholecount);
        textview = (TextView) findViewById(R.id.how_accurate_text);
        trafficTime = (TextView) findViewById(R.id.traffic_time);
        accuracyLowTime = (TextView) findViewById(R.id.accuracy_low_time);
        accuracySeekbar = (SeekBar) findViewById(R.id.accuracy_seek);
        setUserPercievedAccuracy(-1); // To have a non 0 value when the user does not submit
        submitButton = (Button) findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                accuracy_result = accuracySeekbar.getProgress();
                setUserPercievedAccuracy(accuracy_result);
                submitButton.setVisibility(View.GONE);
                accuracySeekbar.setVisibility(View.GONE);
                textview.setVisibility(View.GONE);
            }
        });


        ProcessFileTask task = new ProcessFileTask();
        task.execute(tripID);
        File file = new File(getApplicationContext().getFilesDir(), "analysis/" + tripID + ".csv");

        // Log.d("maps", file.toString());

        //File file = new File(getApplicationContext().getFilesDir(), "locs/"+trip.getTrip_id()+".txt");

        try {
            inputStream = new FileInputStream(file);
        } catch (Exception e) {
            System.out.println("Exception_raised " + e.toString());
        }

        InputStreamReader isr = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(isr);
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                String tokens[] = line.split(",");
                latLngs.add(new LatLng(Double.valueOf(tokens[0]), Double.valueOf(tokens[1])));
            }
        } catch (Exception e) {

        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.trip_map);

        mapFragment.getMapAsync(this);

        // setup action bar
        }



    public void populatePotholeMarkerPoints(){

        Iterator it = probablePointsOfInterest.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            probablePotholeLatLngs.add(extractLatLng((String) pair.getValue()));
            it.remove(); // avoids a ConcurrentModificationException
        }

        it = definitePointsOfInterest.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            definitePotholeLatLngs.add(extractLatLng((String) pair.getValue()));
            it.remove();
        }

    }

    public LatLng extractLatLng(String line){
        String vals[] = line.split(",");
        LatLng l = new LatLng(Double.valueOf(vals[locIndex]), Double.valueOf(vals[locIndex+1]));
        return l;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if(!latLngs.isEmpty()) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs.get(0), 15));

            PolylineOptions polyline = new PolylineOptions().geodesic(true).width(5).color(Color.BLUE);

            for (LatLng l : latLngs) {
                polyline.add(l);
            }
            mMap.addPolyline(polyline);
            Set<String> probablePotholeStringSet = tripStatsPreferences.getStringSet(getString(R.string.probable_pothole_location_set), new HashSet<String>());
            Set<String> definitePotholeStringSet = tripStatsPreferences.getStringSet(getString(R.string.definite_pothole_location_set), new HashSet<String>());
            if (!probablePotholeLatLngs.isEmpty()) {

                for (LatLng l : probablePotholeLatLngs) {
                    probablePotholeStringSet.add(new Gson().toJson(l));
                }
                for (LatLng l: definitePotholeLatLngs) {
                    mMap.addMarker(new MarkerOptions().position(l).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    definitePotholeStringSet.add(new Gson().toJson(l));

                }
                tripStatsEditor.putStringSet(getString(R.string.probable_pothole_location_set), probablePotholeStringSet);
                tripStatsEditor.putStringSet(getString(R.string.definite_pothole_location_set), definitePotholeStringSet);
                tripStatsEditor.commit();
                //sending broadcast to TriplistFragment to confirm if location set belongs to highestPotholeTrip
                Intent highestPotholeCheckIntent = new Intent(getString(R.string.highest_pothole_latlngs_check));
                highestPotholeCheckIntent.putExtra(getString(R.string.definite_pothole_location_set), (Parcelable) definitePotholeStringSet);
                LocalBroadcastManager.getInstance(this).sendBroadcast(highestPotholeCheckIntent);
            }
        }else {
            textview.setText("No locations found");
        }
    }

    private void setUserPercievedAccuracy(int a){
        ApplicationClass.getInstance().getTrip().setUserRating(a);
        dbPreferencesEditor.putInt("userPerceivedAccuracy", a);
    }

    private void setProbablePotholeCount(int a){
        ApplicationClass.getInstance().getTrip().setProbablePotholeCount(a);
        //data required by TLF for updating TripViewModel instance
        dbPreferencesEditor.putInt("probablePotholeCount", a);
    }

    private void setDefinitePotholeCount(int a) {
        ApplicationClass.getInstance().getTrip().setDefinitePotholeCount(a);
        dbPreferencesEditor.putInt("definitePotholeCount", a);
        dbPreferencesEditor.commit();
    }

    public void logAnalytics(String data){
        Bundle b = new Bundle();
        b.putString("MapsActivity", data);
        mFirebaseAnalytics.logEvent(data, b);
    }

    float roundTwoDecimals(float f) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Float.valueOf(twoDForm.format(f));
    }

    private class ProcessFileTask extends AsyncTask<String, Void, String>{

        int lineNumber = 0, prevLineNumber = 0;
        FileInputStream is;
        private float[] speedArray;

        private boolean didSpeedOscillate(float arr[]) {
            if (arr[0] > arr[1] && arr[1] < arr[2])
                return true;
            else
                return false;
        }


        private int findClosestKeyValue(int lineNumber) {
            int minDifference = Integer.MAX_VALUE;
            int closestKeyValue = -1;
            for (int key: speedWithLocationHashMap.keySet()) {
                int diff = Math.abs(closestKeyValue - lineNumber);
                if (diff < minDifference) {
                    minDifference = diff;
                    closestKeyValue = key;
                }
            }
            return closestKeyValue;
        }

        @Override
        protected String doInBackground(String... params) {
            speedArray = new float[]{-1, -1, -1};
            File file = new File(getApplicationContext().getFilesDir(), "logs/" + tripID + ".csv");
            try {
                is = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader bufferedReader = new BufferedReader(isr);
                String line;
                try {
                    // extracting the index of the value to be compared in a given line of data
                    line = bufferedReader.readLine();
                    String tokens[] = line.split(",");
                    for (int i = 0; i < tokens.length; i++) {
                        if (tokens[i].contains(axisOfInterest)) {
                            axisIndex = i;
                        }
                        if(tokens[i].contains("latitude")){
                            locIndex = i;
                        }
                        if (tokens[i].contains("speed")) {
                            speedIndex = i;
                            // Log.d("speedIndex", String.valueOf(i));
                        }
                    }

                    if (MapsActivity.this.getIntent().getParcelableExtra(getString(R.string.speed_with_location_hashmap)) != null) {
                        speedWithLocationHashMap = new LinkedHashMap<>();
                        //populating the set of the points we are interested in
                        while ((line = bufferedReader.readLine()) != null) {
                            String values[] = line.split(",");
                            lineNumber++;
                            if(Float.valueOf(values[axisIndex]) > threshold && lineNumber>prevLineNumber+ linesPerPeriod){
                                // this ignores the first period of data
                                int closestKey = findClosestKeyValue(lineNumber);
                                ArrayList<Integer> keys = new ArrayList<>(speedWithLocationHashMap.keySet());
                                Log.d("KeyArrayList", keys.toString());
                                Log.d("closestKey", String.valueOf(closestKey));
                                int belowClosestKey = keys.get(keys.indexOf(closestKey) - 1);
                                int aboveClosestKey = keys.get(keys.indexOf(closestKey) + 1);
                                Log.d("belowClosestKey", String.valueOf(belowClosestKey));
                                Log.d("aboveClosestKey", String.valueOf(aboveClosestKey));
                                float[] speedValues = new float[]{speedWithLocationHashMap.get(belowClosestKey).getSpeed(), speedWithLocationHashMap.get(closestKey).getSpeed(), speedWithLocationHashMap.get(aboveClosestKey).getSpeed()};
                                if (Float.valueOf(values[speedIndex]) > DEFINITE_THRESHOLD_SPEED_METRES_PER_SECOND && didSpeedOscillate(speedValues))
                                    definitePointsOfInterest.put(lineNumber, line);
                                else if (Float.valueOf(values[speedIndex]) > PROBABLE_THRESHOLD_SPEED_METRES_PER_SECOND && didSpeedOscillate(speedValues))
                                    probablePointsOfInterest.put(lineNumber, line);
                                prevLineNumber = lineNumber;
                            }
                        }
                    }
                    else if (speedWithLocationHashMap.size() < finishedTrip.getNo_of_lines()/2) {
                        Toast.makeText(getApplicationContext(), "Your device was unable to detect speed quickly enough", Toast.LENGTH_SHORT).show();
                        populateDataPoints(line, bufferedReader);
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Device speed could not be measured.", Toast.LENGTH_SHORT).show();
                        populateDataPoints(line, bufferedReader);
                    }
                }
                catch (Exception e){

                }
                // Log.d("result before return", definitePointsOfInterest.size() + " " + probablePointsOfInterest.size());
                return definitePointsOfInterest.size() + " " + probablePointsOfInterest.size();
            } catch (FileNotFoundException e) {
                return null;
            }

        }

        private void populateDataPoints(String line, BufferedReader bufferedReader) {
            // populating our set of the points we are interested in
            try {
                while ((line = bufferedReader.readLine()) != null) {
                    String values[] = line.split(",");
                    lineNumber++;
                    if (Float.valueOf(values[axisIndex]) > threshold && lineNumber > prevLineNumber + linesPerPeriod) {
                        // this ignores the first period of data
                        if (Float.valueOf(values[speedIndex]) > DEFINITE_THRESHOLD_SPEED_METRES_PER_SECOND)
                            definitePointsOfInterest.put(lineNumber, line);
                        else if (Float.valueOf(values[speedIndex]) > PROBABLE_THRESHOLD_SPEED_METRES_PER_SECOND)
                            probablePointsOfInterest.put(lineNumber, line);
                        prevLineNumber = lineNumber;
                    }
                }
            } catch (IOException ioException) {
                Log.e(TAG, ioException.getMessage());
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            int indexOfSpace = result.indexOf(' ');
            int definitePotholeCount = Integer.parseInt(result.substring(0, indexOfSpace));
            int probablePotholeCount = Integer.parseInt(result.substring(indexOfSpace + 1));
            // Log.d("probableCount", probablePotholeCount + "");
            // Log.d("definiteCount", definitePotholeCount + "");
            setProbablePotholeCount(probablePotholeCount);
            setDefinitePotholeCount(definitePotholeCount);
            spinner.setVisibility(View.GONE);
            duration.setText(finishedTrip.getDuration() + " minutes");
            date.setText(finishedTrip.getStartTime().substring(0,11));
            trafficTime.setText(finishedTrip.getMinutesWasted() + " minutes");
            accuracyLowTime.setText(finishedTrip.getMinutesAccuracyLow() + " minutes");
            resultGrid.setVisibility(View.VISIBLE);
            accuracySeekbar.setVisibility(View.VISIBLE);
            submitButton.setVisibility(View.VISIBLE);
            if(finishedTrip.getDistanceInKM() < 0.5 && !BuildConfig.DEBUG){
                distance.setText(" < 0.5 km");
                probablePotholeCountTextView.setText("Sorry, you must travel at least 0.5 km");
                definitePotholeCountTextView.setText("Sorry, you must travel at least 0.5 km");
            }else{
                distance.setText(roundTwoDecimals(finishedTrip.getDistanceInKM()) + " km");
                probablePotholeCountTextView.setText(Integer.toString(probablePotholeCount));
                definitePotholeCountTextView.setText(Integer.toString(definitePotholeCount));
                populatePotholeMarkerPoints();
                tripIdSet = tripStatsPreferences.getStringSet("tripIdSet", new HashSet<String>());
                if (!tripIdSet.contains(finishedTrip.getTrip_id())) {
                    // Log.d("MapsActivity", tripID + "");
                    int validTrips = tripStatsPreferences.getInt("validTrips", 0);
                    tripStatsEditor.putInt("validTrips", validTrips + 1);
                    int sharedPrefsProbablePotholes = tripStatsPreferences.getInt("probablePotholes", 0);
                    tripStatsEditor.putInt("probablePotholes", sharedPrefsProbablePotholes + probablePotholeCount);
                    int sharedPrefsDefinitePotholes = tripStatsPreferences.getInt("definitePotholes", 0);
                    tripStatsEditor.putInt("definitePotholes", sharedPrefsDefinitePotholes + definitePotholeCount);
                }
                tripIdSet.add(finishedTrip.getTrip_id());
                tripStatsEditor.putStringSet("tripIdSet", tripIdSet);
                tripStatsEditor.commit();
                //only registering trip in database if it is useful to us
                Intent newTripIntent = new Intent(getString(R.string.new_trip_insert));
                newTripIntent.putExtra("trip_object", finishedTrip);
                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(MapsActivity.this);
                localBroadcastManager.sendBroadcast(newTripIntent);
                mapFragment.getMapAsync(MapsActivity.this);
            }
        }
    }
}

/**
*
*  Check if first gear start stop scenarios have a concrete signature in terms of waveform
*
*  Include a  Help required section -> Machine learning on time series data, backend development?
*
**/