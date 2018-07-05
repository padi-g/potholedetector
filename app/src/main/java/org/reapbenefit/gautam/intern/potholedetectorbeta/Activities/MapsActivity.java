package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.APIService;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.SpeedWithLocation;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;
import org.reapbenefit.gautam.intern.potholedetectorbeta.UserPothole;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
    private TreeMap<Integer, SpeedWithLocation> speedWithLocationTreeMap = new TreeMap<>();
    private final String TAG = getClass().getSimpleName();
    private Trip highestPotholeTrip;
    private SharedPreferences dbPreferences;
    private SharedPreferences.Editor dbPreferencesEditor;
    private TextView definitePotholeCountTextView;
    private boolean isViewingHighestPotholeTrip;
    private long tripDurationInSeconds;
    private Snackbar tweetSnackbar;


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


        Map<Integer, SpeedWithLocation> simpleMap = (Map<Integer, SpeedWithLocation>) getIntent().getSerializableExtra(getString(R.string.speed_with_location_hashmap));
        if (simpleMap != null) {
            speedWithLocationTreeMap = new TreeMap<>(simpleMap);
        }
        else {
            speedWithLocationTreeMap = null;
        }

        tripDurationInSeconds = getIntent().getLongExtra(getString(R.string.duration_in_seconds), 0);

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
                Intent updateUserRatingIntent = new Intent(MapsActivity.this, APIService.class);
                updateUserRatingIntent.putExtra("request", "POST");
                updateUserRatingIntent.putExtra("table", getString(R.string.trip_data_table));
                finishedTrip.setUserRating(accuracy_result);
                // Log.d(TAG, accuracy_result + "");
                updateUserRatingIntent.putExtra(getString(R.string.trip_with_user_rating), finishedTrip);
                startService(updateUserRatingIntent);
            }
        });

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.trip_map);

        mapFragment.getMapAsync(this);

        isViewingHighestPotholeTrip = getIntent().getBooleanExtra(getString(R.string.is_viewing_highest_pothole_trip), false);

        if (!isViewingHighestPotholeTrip) {
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
        }
        else {
            //reading highestPotholeTrip data from SharedPreferences
            String highestPotholeTripJson = dbPreferences.getString("highestPotholeTrip", null);
            if (highestPotholeTripJson != null) {
                highestPotholeTrip = new Gson().fromJson(highestPotholeTripJson, Trip.class);
                drawInformationalUI(highestPotholeTrip);
                //populating marker locations on map
                Set<String> definitePotholeLocationSet = dbPreferences.getStringSet(getString(R.string.definite_pothole_location_set), new HashSet<String>());
                Iterator iterator = definitePotholeLocationSet.iterator();
                while (iterator.hasNext() && mMap != null) {
                    LatLng definitePotholeLocation = new Gson().fromJson(iterator.next().toString(), LatLng.class);
                    mMap.addMarker(new MarkerOptions().position(definitePotholeLocation).icon(BitmapDescriptorFactory.defaultMarker()));
                }
            }
        }
        // setup action bar
        }

    private void drawInformationalUI(Trip trip) {
        spinner.setVisibility(View.GONE);
        duration.setText(trip.getDuration() + " minutes");
        date.setText(trip.getStartTime().substring(0, 11));
        trafficTime.setText(trip.getMinutesWasted() + " minutes");
        accuracyLowTime.setText(trip.getMinutesAccuracyLow() + " minutes");
        resultGrid.setVisibility(View.VISIBLE);
        accuracySeekbar.setVisibility(View.VISIBLE);
        submitButton.setVisibility(View.VISIBLE);
        if (finishedTrip.getDistanceInKM() < 0.5 && !BuildConfig.DEBUG) {
            distance.setText(" < 0.5 km");
            probablePotholeCountTextView.setText("Sorry, you must travel at least 0.5 km");
            definitePotholeCountTextView.setText("Sorry, you must travel at least 0.5 km");
        } else {
            distance.setText(roundTwoDecimals(trip.getDistanceInKM()) + " km");
            probablePotholeCountTextView.setText(trip.getProbablePotholeCount() + "");
            definitePotholeCountTextView.setText(trip.getDefinitePotholeCount() + "");
        }
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
            Set<String> probablePotholeStringSet = new HashSet<>();
            Set<String> definitePotholeStringSet = new HashSet<>();
            if (!isViewingHighestPotholeTrip) {
                probablePotholeStringSet = tripStatsPreferences.getStringSet(getString(R.string.probable_pothole_location_set), new HashSet<String>());
                definitePotholeStringSet = tripStatsPreferences.getStringSet(getString(R.string.definite_pothole_location_set), new HashSet<String>());
            }
            else {
                //changing sets to read values of highest pothole trips
                probablePotholeStringSet = tripStatsPreferences.getStringSet(getString(R.string.highest_pothole_trip_probable_potholes), new HashSet<String>());
                definitePotholeStringSet = tripStatsPreferences.getStringSet(getString(R.string.highest_pothole_trip_definite_potholes), new HashSet<String>());
            }
            if (!probablePotholeLatLngs.isEmpty()) {
                for (LatLng l : probablePotholeLatLngs) {
                    probablePotholeStringSet.add(new Gson().toJson(l));
                    updateUserPotholeTable(0, l);
                }
            }
            if (!definitePotholeLatLngs.isEmpty()) {
                for (LatLng l: definitePotholeLatLngs) {
                    mMap.addMarker(new MarkerOptions().position(l).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                    definitePotholeStringSet.add(new Gson().toJson(l));
                    updateUserPotholeTable(1, l);
                }
                tripStatsEditor.putStringSet(getString(R.string.probable_pothole_location_set), probablePotholeStringSet);
                tripStatsEditor.putStringSet(getString(R.string.definite_pothole_location_set), definitePotholeStringSet);
                tripStatsEditor.commit();
                //sending broadcast to TriplistFragment to confirm if location set belongs to highestPotholeTrip
                Intent highestPotholeCheckIntent = new Intent(getString(R.string.highest_pothole_latlngs_check));
                highestPotholeCheckIntent.putExtra(getString(R.string.highest_pothole_trip_definite_potholes), (Serializable) definitePotholeStringSet);
                highestPotholeCheckIntent.putExtra(getString(R.string.highest_pothole_trip_probable_potholes), (Serializable) probablePotholeStringSet);
                LocalBroadcastManager.getInstance(this).sendBroadcast(highestPotholeCheckIntent);
            }
        }else if (!isViewingHighestPotholeTrip){
            textview.setText("No locations found");
        }
    }

    private void updateUserPotholeTable(int classification, LatLng latLng) {
        // Log.d(TAG, "Called once");
        //updating table in RDS
        Intent addDefinitePotholeIntent = new Intent(this, APIService.class);
        addDefinitePotholeIntent.putExtra("request", "POST");
        addDefinitePotholeIntent.putExtra("table", "UserPotholes");
        UserPothole userPothole = new UserPothole();
        userPothole.setUserID(getSharedPreferences("uploads", MODE_PRIVATE).getString("FIREBASE_USER_ID", null));
        userPothole.setPotLong((float) latLng.longitude);
        userPothole.setPotLat((float) latLng.latitude);
        userPothole.setClassification(classification);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date currentDate = new Date();
        String hitDate = simpleDateFormat.format(currentDate);
        userPothole.setHitDate(hitDate);
        addDefinitePotholeIntent.putExtra("userPotholeObject", userPothole);
        startService(addDefinitePotholeIntent);
    }

    private void setUserPercievedAccuracy(int a){
        ApplicationClass.getInstance().getTrip().setUserRating(a);
        dbPreferencesEditor.putInt("userPerceivedAccuracy", a);
    }

    private void setProbablePotholeCount(int a){
        ApplicationClass.getInstance().getTrip().setProbablePotholeCount(a);
        finishedTrip.setProbablePotholeCount(a);
        //data required by TLF for updating TripViewModel instance
        dbPreferencesEditor.putInt("probablePotholeCount", a);
    }

    private void setDefinitePotholeCount(int a) {
        ApplicationClass.getInstance().getTrip().setDefinitePotholeCount(a);
        finishedTrip.setDefinitePotholeCount(a);
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
        private boolean showInaccurateToast;

        private boolean didSpeedOscillate(float arr[]) {
            if (arr[0] > arr[1] && arr[1] < arr[2])
                return true;
            else
                return false;
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

                    if (speedWithLocationTreeMap != null && speedWithLocationTreeMap.size() >= tripDurationInSeconds/3) {
                        // Log.d(TAG, new Gson().toJson(speedWithLocationTreeMap.toString()));
                        //populating the set of the points we are interested in
                        while ((line = bufferedReader.readLine()) != null) {
                            String values[] = line.split(",");
                            lineNumber++;
                            if(Float.valueOf(values[axisIndex]) > threshold && lineNumber>prevLineNumber+ linesPerPeriod){
                                // this ignores the first period of data
                                int[] closestKeyValues = findClosestKeyValues(lineNumber);

                                float speedValues[] = new float[]{speedWithLocationTreeMap.get(closestKeyValues[0]).getSpeed(),
                                speedWithLocationTreeMap.get(closestKeyValues[1]).getSpeed(),
                                speedWithLocationTreeMap.get(closestKeyValues[2]).getSpeed()};
                                // Log.d("speedValues", speedValues.toString());
                                if (Float.valueOf(values[speedIndex]) > DEFINITE_THRESHOLD_SPEED_METRES_PER_SECOND && didSpeedOscillate(speedValues))
                                    definitePointsOfInterest.put(lineNumber, line);
                                else if (Float.valueOf(values[speedIndex]) > PROBABLE_THRESHOLD_SPEED_METRES_PER_SECOND && didSpeedOscillate(speedValues))
                                    probablePointsOfInterest.put(lineNumber, line);
                                prevLineNumber = lineNumber;
                            }
                        }
                    }
                    else {
                        // Log.d(TAG, "inside else");
                        showInaccurateToast = true;
                        // populating our set of the points we are interested in
                        while ((line = bufferedReader.readLine()) != null) {
                            String values[] = line.split(",");
                            // Log.d(TAG, "speedmap null");
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

        private int[] findClosestKeyValues(int lineNumber) {
            int minDifference = Integer.MAX_VALUE;
            int closestKeyValue = -1;
            Set<Integer> keySet = speedWithLocationTreeMap.keySet();
            int baseKey = findClosestKeyValue(lineNumber);
            List<Integer> keyList = new ArrayList<>(keySet);
            int[] arr = new int[3];
            int indexOfBaseKey = keyList.indexOf(baseKey);
            arr[0] = keyList.get(indexOfBaseKey - 1);
            arr[1] = keyList.get(indexOfBaseKey);
            arr[2] = keyList.get(indexOfBaseKey + 1);
            return arr;
        }

        private int findClosestKeyValue(int lineNumber) {
            int closestKey = -1;
            try {
                Iterator iterator = speedWithLocationTreeMap.entrySet().iterator();
                int tempNumber = lineNumber;
                int minDifference = Integer.MAX_VALUE;
                while (iterator.hasNext()) {
                    Map.Entry pair = (Map.Entry) iterator.next();
                    int diff = Math.abs((int)pair.getKey() - lineNumber);
                    if (diff < minDifference) {
                        minDifference = diff;
                        closestKey = (int) pair.getKey();
                    }
                }
            } catch (Exception exception) {
                // Log.e(TAG, exception.getMessage());
            }
            return closestKey;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (showInaccurateToast) {
                Toast.makeText(MapsActivity.this.getApplicationContext(), R.string.speed_updates_not_enough, Toast.LENGTH_LONG).show();
            }
            int indexOfSpace = result.indexOf(' ');
            int definitePotholeCount = Integer.parseInt(result.substring(0, indexOfSpace));
            int probablePotholeCount = Integer.parseInt(result.substring(indexOfSpace + 1));
            // Log.d("probableCount", probablePotholeCount + "");
            // Log.d("definiteCount", definitePotholeCount + "");
            setProbablePotholeCount(probablePotholeCount);
            setDefinitePotholeCount(definitePotholeCount);

            drawInformationalUI(finishedTrip);
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

            //initiating service call to update RDS with pothole count
            Intent updateTripPotholeCountIntent = new Intent(MapsActivity.this, APIService.class);
            updateTripPotholeCountIntent.putExtra("request", "POST");
            updateTripPotholeCountIntent.putExtra("table", getString(R.string.trip_data_table));
            updateTripPotholeCountIntent.putExtra(getString(R.string.processed_trip), finishedTrip);
            MapsActivity.this.startService(updateTripPotholeCountIntent);

            // drawing Snackbar and adding Tweet button
            Snackbar snackbar = Snackbar.make(findViewById(R.id.maps_linear_layout), R.string.tweet_snackbar,
                    Snackbar .LENGTH_LONG);
            snackbar.setAction("Tweet", new TweetButtonListener());
            snackbar.show();
        }
    }

    private class TweetButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            String text = "Found " + definitePotholeCountTextView.getText().toString()
                    + " " + getString(R.string.tweet_content);
            String tweetText = String.format("https://twitter.com/intent/tweet?text=%s", text);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tweetText));
            // looking for official Twitter app
            List<ResolveInfo> matches = getPackageManager().queryIntentActivities(intent, 0);
            for (ResolveInfo info : matches) {
                if (info.activityInfo.packageName.toLowerCase().startsWith("com.twitter")) {
                    intent.setPackage(info.activityInfo.packageName);
                    startActivity(intent);
                    break;
                }
                else if (info.activityInfo.packageName.toLowerCase().startsWith("com.android.chrome")) {
                    intent.setPackage(info.activityInfo.packageName);
                    startActivity(intent);
                    break;
                }
            }
            tweetSnackbar.setText("Twitter-compatible app not found");
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