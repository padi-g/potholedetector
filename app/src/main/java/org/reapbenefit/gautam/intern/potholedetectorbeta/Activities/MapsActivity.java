package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.reapbenefit.gautam.intern.potholedetectorbeta.MyLocation;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ArrayList<LatLng> latLngs = new ArrayList<>();
    private InputStream inputStream;
    FirebaseAnalytics mFirebaseAnalytics;

    private TextView noOfPotholes;
    private ProgressBar spinner;
    private String tripID;
    private int linesPerSec;
    private float threshold;
    private String axisOfInterest;
    private int axisIndex;
    private HashMap<Integer, String> pointsOfInterest = new HashMap<>();

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
        getSupportActionBar().setTitle(R.string.menu_about);
        Switch s = (Switch) toolbar.findViewById(R.id.stopSwitch);
        s.setVisibility(View.GONE);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        logAnalytics("map_opened");
        noOfPotholes = (TextView) findViewById(R.id.potholes);
        spinner = (ProgressBar) findViewById(R.id.indeterminateBar);
        tripID = fetchTripID();

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

    private String fetchTripID(){
        String name = "null";

        String path = "tripsIDs.csv";
        File temp = new File(getApplicationContext().getFilesDir(), path);
        try {
            InputStream inputStream = new FileInputStream(temp);
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(isr);
            name = bufferedReader.readLine();
            linesPerSec = Integer.valueOf(bufferedReader.readLine());
            threshold = Float.valueOf(bufferedReader.readLine());
            axisOfInterest = bufferedReader.readLine();
        }catch (FileNotFoundException e){

        }catch (IOException er){

        }

        return name;
    }

    public void logAnalytics(String data){
        Bundle b = new Bundle();
        b.putString("MapsActivity", data);
        mFirebaseAnalytics.logEvent(data, b);
    }

    private class ProcessFileTask extends AsyncTask<String, Void, String>{

        int lineNumber = 0, prevLineNumber = 0;
        FileInputStream is;

        @Override
        protected String doInBackground(String... params) {
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

            return String.valueOf(pointsOfInterest.size());
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            noOfPotholes.setText("We found " + result + " potholes");
            spinner.setVisibility(View.GONE);
            noOfPotholes.setVisibility(View.VISIBLE);
        }
    }

}

