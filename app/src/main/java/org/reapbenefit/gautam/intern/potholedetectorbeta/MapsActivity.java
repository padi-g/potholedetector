package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.content.Intent;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    ArrayList<LatLng> latLngs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        latLngs = new ArrayList<>();
        latLngs.add(new LatLng(22.716543, 75.855447));
        latLngs.add(new LatLng(22.713047, 75.840208));
        latLngs.add(new LatLng(22.703484, 75.861601));
        latLngs.add(new LatLng(22.710510, 75.872051));
        latLngs.add(new LatLng(22.706072, 75.878865));
        latLngs.add(new LatLng(22.693437, 75.869135));
        latLngs.add(new LatLng(22.694373, 75.862454));

        latLngs.add(new LatLng(22.686290, 75.824502));
        latLngs.add(new LatLng(22.641438, 75.807314));
        latLngs.add(new LatLng(22.630740, 75.790426));
        latLngs.add(new LatLng(22.748208, 75.912086));

        Toast.makeText(this, "This is representational data from our tests", Toast.LENGTH_LONG);


        //Intent i = getIntent();
        //File f = (File)i.getExtras().get("file");

        //latLngs = extractPoints(f);
        CameraPosition c = new CameraPosition(new LatLng(22.694373, 75.862454),0,0,0);
        CameraUpdateFactory.newCameraPosition(c);


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
//2201784907pm
        for(LatLng l : latLngs){
            mMap.addMarker(new MarkerOptions().position(l));
        }

    }

    ArrayList<LatLng> extractPoints(File file){

        BufferedReader in;
        String line, lats, longs;
        ArrayList<LatLng> mLatlng = null;
        int k = 0, t=0;

        try {
            in = new BufferedReader(new FileReader(file));
        }catch (java.io.FileNotFoundException e){
            return null;
        }
        try {
            line = in.readLine();
            while(!line.isEmpty()) {
                for (int i = 1; i < 12; i++) {
                    if(i==2){
                        t = Integer.parseInt(line.substring(1,2));
                    }
                    int a = line.indexOf(",");
                    line = line.substring(a + 1);
                }
                if(t>13) {
                    lats = line.substring(0, 10);
                    longs = line.substring(12, 21);
                    LatLng temp = new LatLng(Double.parseDouble(lats), Double.parseDouble(longs));
                    mLatlng.add(k, temp);
                }
                k++;
                line = in.readLine();
            }

        }catch (java.io.IOException e){

        }


    return mLatlng;
    }
}
