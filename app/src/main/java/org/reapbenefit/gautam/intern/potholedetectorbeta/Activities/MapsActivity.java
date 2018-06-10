package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    MapFragment mapFragment;
    private ArrayList<LatLng> latLngs = new ArrayList<>();
    private ArrayList<LatLng> potholelatLngs = new ArrayList<>();
    private InputStream inputStream;
    private FirebaseAnalytics mFirebaseAnalytics;
    private DatabaseReference db;

    private ProgressBar spinner;
    private TextView date, distance, duration, potholecount, textview, trafficTime;
    private SeekBar accuracySeekbar;
    private Button submitButton;
    private String tripID;
    private int linesPerPeriod;
    private float threshold;
    private String axisOfInterest;
    private int axisIndex, locIndex;
    private Trip finishedTrip;
    private HashMap<Integer, String> pointsOfInterest = new HashMap<>();
    private int accuracy_result = 0;
    private GridLayout resultGrid;
    private TextView accuracyLowTime;

    private SharedPreferences dbPreferences;
    private SharedPreferences.Editor dbPreferencesEditor;

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


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
        potholecount = (TextView) findViewById(R.id.potholecount);
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

        Log.d("maps", file.toString());

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

        Iterator it = pointsOfInterest.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            potholelatLngs.add(extractLatLng((String) pair.getValue()));
            it.remove(); // avoids a ConcurrentModificationException
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

            if (!potholelatLngs.isEmpty()) {
                for (LatLng l : potholelatLngs) {
                    mMap.addMarker(new MarkerOptions()
                            .position(l));
                }
            }
        }else {
            textview.setText("No locations found");
        }
    }

    private void setUserPercievedAccuracy(int a){
        ApplicationClass.getInstance().getTrip().setUserRating(a);
        dbPreferencesEditor.putInt("userPerceivedAccuracy", a);
    }

    private void setPotholeCount(int a){
        ApplicationClass.getInstance().getTrip().setPotholeCount(a);
        //data required by TLF for updating TripViewModel instance
        dbPreferencesEditor.putInt("potholeCount", a);
        dbPreferencesEditor.apply();
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

    private class ProcessFileTask extends AsyncTask<String, Void, Integer>{

        int lineNumber = 0, prevLineNumber = 0;
        FileInputStream is;

        @Override
        protected Integer doInBackground(String... params) {
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
                    }

                    // populating our set of the points we are interested in
                    while ((line = bufferedReader.readLine()) != null) {
                        String values[] = line.split(",");
                        lineNumber++;
                        if(Float.valueOf(values[axisIndex]) > threshold && lineNumber>prevLineNumber+ linesPerPeriod){
                            // this ignores the first period of data
                            pointsOfInterest.put(lineNumber, line);
                            prevLineNumber = lineNumber;
                        }
                    }

                }
                catch (Exception e){

                }

                return pointsOfInterest.size();
            } catch (FileNotFoundException e) {
                return 0;
            }

        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            setPotholeCount(result);
            spinner.setVisibility(View.GONE);
            duration.setText(finishedTrip.getDuration() + " mins");
            date.setText(finishedTrip.getStartTime().substring(0,11));
            trafficTime.setText(finishedTrip.getMinutesWasted() + " minutes");
            accuracyLowTime.setText(finishedTrip.getMinutesAccuracyLow() + " minutes");
            resultGrid.setVisibility(View.VISIBLE);
            accuracySeekbar.setVisibility(View.VISIBLE);
            submitButton.setVisibility(View.VISIBLE);
            if(finishedTrip.getDistanceInKM() < 0.5){
                distance.setText(" < 0.5km");
                potholecount.setText("sorry, you must travel at least 0.5km");
            }else {
                distance.setText(roundTwoDecimals(finishedTrip.getDistanceInKM()) + "km");
                potholecount.setText(Integer.toString(result));
                populatePotholeMarkerPoints();
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