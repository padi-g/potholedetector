package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.MyLocation;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ArrayList<LatLng> latLngs = new ArrayList<>();
    private InputStream inputStream;
    private FirebaseAnalytics mFirebaseAnalytics;
    private DatabaseReference db;

    private ProgressBar spinner;
    private TextView date, distance, duration, potholecount;
    private SeekBar accuracySeekbar;
    private Button submitButton;
    private String tripID;
    private int linesPerSec;
    private float threshold;
    private String axisOfInterest;
    private int axisIndex;
    private Trip finishedTrip;
    private HashMap<Integer, String> pointsOfInterest = new HashMap<>();
    private int accuracy_result = 0;
    // TODO : set validation rules for troublesome values like accuracy

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Trip Summary");
        Switch s = (Switch) toolbar.findViewById(R.id.stopSwitch);
        s.setVisibility(View.GONE);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        logAnalytics("map_opened");
        finishedTrip = ApplicationClass.getInstance().getTrip();
        tripID = finishedTrip.getTrip_id();
        linesPerSec = finishedTrip.getNo_of_lines()*3;
        threshold = finishedTrip.getThreshold()*8;
        axisOfInterest = finishedTrip.getAxis();

        spinner = (ProgressBar) findViewById(R.id.indeterminateBar);
        date = (TextView) findViewById(R.id.tripdate);
        duration = (TextView) findViewById(R.id.duration);
        distance = (TextView) findViewById(R.id.distance);
        potholecount = (TextView) findViewById(R.id.potholecount);
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
            while((line = bufferedReader.readLine()) != null ){
                String tokens[] = line.split(",");
                latLngs.add(new LatLng(Double.valueOf(tokens[0]), Double.valueOf(tokens[1])));
            }
        }
        catch (Exception e){

        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        // setup action bar
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


        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs.get(0), 15));

        PolylineOptions polyline = new PolylineOptions().geodesic(true).width(5).color(Color.BLUE);

        for(LatLng l : latLngs){
            polyline.add(l);
        }
        mMap.addPolyline(polyline);

    }

    private void setUserPercievedAccuracy(int a){
        db = FirebaseDatabase.getInstance().getReference();
        db = db.child(finishedTrip.getUser_id()).child(finishedTrip.getTrip_id()).child("userRating");
        ApplicationClass.getInstance().getTrip().setUserRating(a);
        db.setValue(a);
    }

    private void setPotholeCount(int a){
        db = FirebaseDatabase.getInstance().getReference();
        db = db.child(finishedTrip.getUser_id()).child(finishedTrip.getTrip_id()).child("potholeCount");
        ApplicationClass.getInstance().getTrip().setPotholeCount(a);
        db.setValue(a);
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
            } catch (Exception e) {
                System.out.println("Exception_raised " + e.toString());
            }

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
                }

                // populating our set of the points we are interested in
                while ((line = bufferedReader.readLine()) != null) {
                    String values[] = line.split(",");
                    lineNumber++;
                    if(Float.valueOf(values[axisIndex]) > threshold && lineNumber>prevLineNumber+linesPerSec){
                        // this ignores the first second of data
                        pointsOfInterest.put(lineNumber, line);
                        prevLineNumber = lineNumber;
                    }
                }

            }
            catch (Exception e){

            }

            return pointsOfInterest.size();
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            setPotholeCount(result);
            spinner.setVisibility(View.GONE);
            distance.setText("Distance Travelled : " + roundTwoDecimals(finishedTrip.getDistanceInKM()) + "km");
            duration.setText("Trip Duration : " + finishedTrip.getDuration() + " mins");
            date.setText("Date : " + finishedTrip.getStartTime().substring(0,11));
            potholecount.setText("Potholes Detected : " + result);
            distance.setVisibility(View.VISIBLE);
            duration.setVisibility(View.VISIBLE);
            date.setVisibility(View.VISIBLE);
            potholecount.setVisibility(View.VISIBLE);
            accuracySeekbar.setVisibility(View.VISIBLE);
            submitButton.setVisibility(View.VISIBLE);
        }
    }

}

