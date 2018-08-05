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
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

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
import com.google.maps.GeoApiContext;
import com.google.maps.RoadsApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.SnappedPoint;

import org.reapbenefit.gautam.intern.potholedetectorbeta.BuildConfig;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.APIService;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;
import org.reapbenefit.gautam.intern.potholedetectorbeta.UserPothole;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ch.hsr.geohash.GeoHash;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int PAGINATION_OVERLAP = 5;
    public static final int GEOHASH_LENGTH = 8;
    private static final int PAGE_SIZE_LIMIT = 100;
    private static final float LOWER_MSE_THRESHOLD = 6.6f;
    private static final float UPPER_MSE_THRESHOLD = 13.0633f;
    private static final float UPPER_SPEED_THRESHOLD = 2.7778f;
    private GoogleMap mMap;
    MapFragment mapFragment;
    private ArrayList<com.google.android.gms.maps.model.LatLng> latLngs = new ArrayList<>();
    private ArrayList<com.google.android.gms.maps.model.LatLng> probablePotholeLatLngs = new ArrayList<>();
    private ArrayList<com.google.android.gms.maps.model.LatLng> definitePotholeLatLngs = new ArrayList<>();
    private InputStream inputStream;
    private FirebaseAnalytics mFirebaseAnalytics;
    private DatabaseReference db;
    private final float DEFINITE_THRESHOLD_SPEED_METRES_PER_SECOND = 5.55f;
    private final float PROBABLE_THRESHOLD_SPEED_METRES_PER_SECOND = 1.38f;
    private ProgressBar spinner;
    private TextView date, distance, duration, probablePotholeCountTextView, textview, trafficTime;
    private RatingBar accuracyRatingBar;

    private String tripID;
    private Trip finishedTrip;
    private int accuracy_result = 0;
    private GridLayout resultGrid;
    private TextView accuracyLowTime;
    private SharedPreferences tripStatsPreferences;
    private SharedPreferences.Editor tripStatsEditor;
    private Set<String> tripIdSet;
    private final String TAG = getClass().getSimpleName();
    private Trip highestPotholeTrip;
    private SharedPreferences dbPreferences;
    private SharedPreferences.Editor dbPreferencesEditor;
    private TextView definitePotholeCountTextView;
    private boolean isViewingHighestPotholeTrip;
    private long tripDurationInSeconds;
    private Snackbar tweetSnackbar;
    private boolean didUserRateTrip;
    private GeoApiContext geoApiContext;
    private final String API_KEY = "***REMOVED***";
    private Set<String> definitePotholeStringSet;
    private Set<String> probablePotholeStringSet;


    @Override
    protected void onDestroy() {
        if (didUserRateTrip) {
            // updating user rating when user exits activity
            Intent updateUserRatingIntent = new Intent(MapsActivity.this, APIService.class);
            updateUserRatingIntent.putExtra("request", "POST");
            updateUserRatingIntent.putExtra("table", getString(R.string.trip_data_table));
            finishedTrip.setUserRating(accuracy_result);
            // Log.d(TAG, accuracy_result + "");
            updateUserRatingIntent.putExtra(getString(R.string.trip_with_user_rating), finishedTrip);
            startService(updateUserRatingIntent);
        }
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

        tripDurationInSeconds = getIntent().getLongExtra(getString(R.string.duration_in_seconds), 0);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        logAnalytics("map_opened");

        Intent i = getIntent();
        finishedTrip = i.getParcelableExtra("trip");
        if (finishedTrip == null) {
            finishedTrip = ApplicationClass.getInstance().getTrip();
        }

        tripID = finishedTrip.getTrip_id();

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
        accuracyRatingBar = (RatingBar) findViewById(R.id.accuracy_seek);
        setUserPercievedAccuracy(-1); // To have a non 0 value when the user does not submit
        accuracyRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float v, boolean b) {
                // execute submit button related actions
                didUserRateTrip = true;
                accuracy_result = (int) accuracyRatingBar.getRating();
                setUserPercievedAccuracy(accuracy_result);
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
        } else {
            //reading highestPotholeTrip data from SharedPreferences
            String highestPotholeTripJson = dbPreferences.getString("highestPotholeTrip", null);
            if (highestPotholeTripJson != null) {
                highestPotholeTrip = new Gson().fromJson(highestPotholeTripJson, Trip.class);
                drawInformationalUI(highestPotholeTrip);
                //populating marker locations on map
                Set<String> definitePotholeLocationSet = dbPreferences.getStringSet(getString(R.string.definite_pothole_location_set), new HashSet<String>());
                Iterator iterator = definitePotholeLocationSet.iterator();
                while (iterator.hasNext() && mMap != null) {
                    com.google.android.gms.maps.model.LatLng definitePotholeLocation = new Gson().fromJson(iterator.next().toString(), com.google.android.gms.maps.model.LatLng.class);
                    mMap.addMarker(new MarkerOptions().position(definitePotholeLocation).icon(BitmapDescriptorFactory.defaultMarker()));
                }
            }
        }
        // setup action bar
        geoApiContext = new GeoApiContext.Builder().apiKey(API_KEY).build();
    }


    public void populatePotholeMarkerPoints() {
        if (mMap != null) {
            //changing sets to read values of highest pothole trips
            probablePotholeStringSet = tripStatsPreferences.getStringSet(getString(R.string.highest_pothole_trip_probable_potholes), new HashSet<String>());
            definitePotholeStringSet = tripStatsPreferences.getStringSet(getString(R.string.highest_pothole_trip_definite_potholes), new HashSet<String>());
            if (!probablePotholeLatLngs.isEmpty()) {
                for (LatLng l : probablePotholeLatLngs) {
                    probablePotholeStringSet.add(new Gson().toJson(l));
                    updateUserPotholeTable(0, l);
                }
            }
            if (!definitePotholeLatLngs.isEmpty()) {
                for (LatLng l : definitePotholeLatLngs) {
                    if (mMap != null) {
                        mMap.addMarker(new MarkerOptions().position(l));
                    }
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
        }
    }

    private void drawInformationalUI(Trip trip) {
        spinner.setVisibility(View.GONE);
        duration.setText(trip.getDuration() + " minutes");
        date.setText(trip.getStartTime().substring(0, 11));
        trafficTime.setText(trip.getMinutesWasted() + " minutes");
        accuracyLowTime.setText(trip.getMinutesAccuracyLow() + " minutes");
        resultGrid.setVisibility(View.VISIBLE);
        accuracyRatingBar.setVisibility(View.VISIBLE);
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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (!latLngs.isEmpty()) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngs.get(0), 15));
            try {
                PolylineOptions polylineOptions = new PolylineOptions().geodesic(true).width(5).color(Color.BLACK);
                for (LatLng l : latLngs) {
                    polylineOptions.add(l);
                }
                mMap.addPolyline(polylineOptions);
            } catch (Exception e) {
                // Log.e(TAG, "Could not show polyline");
                logAnalytics("Could not show map polyline");
            }
            probablePotholeStringSet = new HashSet<>();
            definitePotholeStringSet = new HashSet<>();
            if (!isViewingHighestPotholeTrip) {
                probablePotholeStringSet = tripStatsPreferences.getStringSet(getString(R.string.probable_pothole_location_set), new HashSet<String>());
                definitePotholeStringSet = tripStatsPreferences.getStringSet(getString(R.string.definite_pothole_location_set), new HashSet<String>());
            }
        } else if (!isViewingHighestPotholeTrip) {
            textview.setText("No locations found");
        }
    }

    private void updateUserPotholeTable(int classification, LatLng latLng) {
        // Log.d(TAG, "Updating user pothole table for classification " + classification);
        //updating table in RDS
        Intent addDefinitePotholeIntent = new Intent(this, APIService.class);
        addDefinitePotholeIntent.putExtra("request", "POST");
        addDefinitePotholeIntent.putExtra("table", "UserPotholes");
        UserPothole userPothole = new UserPothole();
        userPothole.setUserID(getSharedPreferences("uploads", MODE_PRIVATE).getString("FIREBASE_USER_ID", null));
        userPothole.setPotLong((float) latLng.longitude);
        userPothole.setPotLat((float) latLng.latitude);
        userPothole.setClassification(classification);
        userPothole.setGeoHash(GeoHash.geoHashStringWithCharacterPrecision(latLng.latitude, latLng.longitude, GEOHASH_LENGTH));
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date currentDate = new Date();
        String hitDate = simpleDateFormat.format(currentDate);
        userPothole.setHitDate(hitDate);
        addDefinitePotholeIntent.putExtra("userPotholeObject", userPothole);
        startService(addDefinitePotholeIntent);
    }

    private void setUserPercievedAccuracy(int a) {
        ApplicationClass.getInstance().getTrip().setUserRating(a);
        dbPreferencesEditor.putInt("userPerceivedAccuracy", a);
    }

    private void setProbablePotholeCount(int a) {
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

    public void logAnalytics(String data) {
        Bundle b = new Bundle();
        b.putString("MapsActivity", data);
        mFirebaseAnalytics.logEvent(data, b);
    }

    float roundTwoDecimals(float f) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Float.valueOf(twoDForm.format(f));
    }

    private class ProcessFileTask extends AsyncTask<String, Void, String> {

        private FileInputStream is;
        private int definitePotholeCount;
        private int probablePotholeCount;
        private ArrayList<LatLng> definitePotholeLocations = new ArrayList<>();
        private ArrayList<LatLng> probablePotholeLocations = new ArrayList<>();
        private Set<String> geoHashSet = new HashSet<>();

        @Override
        protected String doInBackground(String... params) {
            File file = new File(getApplicationContext().getFilesDir(), "modelErrors/" + tripID + ".csv");
            try {
                is = new FileInputStream(file);
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader bufferedReader = new BufferedReader(isr);
                String line = bufferedReader.readLine();

                while ((line = bufferedReader.readLine()) != null) {
                    String[] values = line.split(",");
                    float readingSum = 0.0f;
                    for (int i = 0; i < 4; ++i) {
                        readingSum += Float.valueOf(values[i]);
                    }
                    double latitude = Double.valueOf(values[4]);
                    double longitude = Double.valueOf(values[5]);
                    latLngs.add(new LatLng(latitude, longitude));
                    float meanSquaredError = readingSum/4.0f;
                    float speed = Float.valueOf(values[values.length - 1]);
                    if (speed >= UPPER_SPEED_THRESHOLD) {
                        // limits pothole recognition to speeds above 10 km/h
                        if (meanSquaredError >= LOWER_MSE_THRESHOLD && meanSquaredError <= UPPER_MSE_THRESHOLD) {
                            // recognised definite pothole
                            definitePotholeLocations.add(new LatLng(Double.valueOf(values[4]), Double.valueOf(values[5])));
                            try {
                                geoHashSet.add(GeoHash.geoHashStringWithCharacterPrecision(Double.valueOf(values[4]), Double.valueOf(values[5]), 7));
                            } catch (Exception e) {
                                // in case the set's uniqueness constraint forces a crash
                                // Log.e(TAG, e.getMessage());
                            }
                        } else if (meanSquaredError >= UPPER_MSE_THRESHOLD) {
                            // could be a serious pothole, needs cross-validation from other people
                            // outliers will not get cross-validated
                            ++probablePotholeCount;
                            probablePotholeLocations.add(new LatLng(Double.valueOf(values[4]), Double.valueOf(values[5])));
                        }
                    }
                }

                Iterator iterator = geoHashSet.iterator();
                while (iterator.hasNext()) {
                    String geoHash = iterator.next().toString();
                    definitePotholeLatLngs.add(new LatLng(GeoHash.fromGeohashString(geoHash).getPoint().getLatitude(),
                            GeoHash.fromGeohashString(geoHash).getPoint().getLongitude()));
                    ++definitePotholeCount;
                }

                // finished processing error file, can delete it from device
                file.delete();

            } catch (Exception exception) {
                // Log.e(TAG, exception.getMessage());
            }
            return definitePotholeCount + " " + probablePotholeCount;
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
                    Snackbar.LENGTH_LONG);
            snackbar.setAction("Tweet", new TweetButtonListener());
            snackbar.show();
        }
    }

    private class TweetButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            try {
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
                    } else if (info.activityInfo.packageName.toLowerCase().startsWith("com.android.chrome")) {
                        intent.setPackage(info.activityInfo.packageName);
                        startActivity(intent);
                        break;
                    } else {
                        tweetSnackbar.setText("Twitter-compatible app not found");
                    }
                }
            } catch (Exception e) {
                // Log.e(TAG, e.getMessage());
            }
        }
    }

    private class SnapToRoadTask extends AsyncTask<Void, Void, List<LatLng>> {
        private boolean didRoadSnapException;
        private SnappedPoint[] pointsPerRequest;
        private List<LatLng> allSnappedPoints = new ArrayList<>();
        @Override
        protected List<LatLng> doInBackground(Void... voids) {
            try {
                Set<String> geoHashSet = new TreeSet<>();
                List<com.google.maps.model.LatLng> uniqueLatLngs = new ArrayList<>();
                for (LatLng latLng: latLngs) {
                    String geoHash = GeoHash.geoHashStringWithCharacterPrecision(latLng.latitude, latLng.longitude, 7);
                    geoHashSet.add(geoHash);
                }
                Iterator geoHashIterator = geoHashSet.iterator();
                while (geoHashIterator.hasNext()) {
                    String geoHash = (String) geoHashIterator.next();
                    uniqueLatLngs.add(new com.google.maps.model.LatLng(GeoHash.fromGeohashString(geoHash).getPoint().getLatitude(), GeoHash.fromGeohashString(geoHash).getPoint().getLongitude()));
                }
                try {
                    int offsets = 0;
                    for (int i = 0; i < uniqueLatLngs.size(); i += PAGE_SIZE_LIMIT) {
                        if (offsets >= PAGINATION_OVERLAP) {
                            offsets -= PAGINATION_OVERLAP;
                        }
                        com.google.maps.model.LatLng[] path = new com.google.maps.model.LatLng[Math.min(i + PAGE_SIZE_LIMIT - offsets, uniqueLatLngs.size() - i)];
                        int j = 0;
                        for (com.google.maps.model.LatLng latLng: uniqueLatLngs.subList(i, Math.min(i + PAGE_SIZE_LIMIT - offsets, uniqueLatLngs.size() - i))) {
                            path[j++] = latLng;
                        }
                        // sending requests with overlapping pointsPerRequest, with 100 hashed locations each request
                        pointsPerRequest = RoadsApi.snapToRoads(geoApiContext, true, path).await();
                        for (SnappedPoint snappedPoint: pointsPerRequest) {
                            allSnappedPoints.add(new LatLng(snappedPoint.location.lat, snappedPoint.location.lng));
                        }
                    }
                } catch (ApiException e) {
                    logAnalytics("RoadsApiException");
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    logAnalytics("RoadsApiInterruptedException");
                    e.printStackTrace();
                } catch (IOException e) {
                    logAnalytics("RoadsApiIOException");
                    e.printStackTrace();
                }
                return allSnappedPoints;
            } catch (Exception e) {
                // road snapping exception occurred, display polyline without snapping to road
                // Log.e(TAG, String.valueOf(e.getLocalizedMessage()));
                didRoadSnapException = true;
            }
            return null;
    }

        @Override
        protected void onPostExecute(List<LatLng> points) {
            if (didRoadSnapException) {
                // drawing standard polyline in UI thread
                PolylineOptions polyline = new PolylineOptions().geodesic(true).width(5).color(Color.BLUE);
                if (latLngs != null) {
                    for (LatLng l: latLngs) {
                        polyline.add(l);
                    }
                    if (mMap != null) {
                        mMap.addPolyline(polyline);
                    }
                }
                super.onPostExecute(points);
                return;
            }
            // drawing snapped polyline in UI thread
            if (points != null) {
                LatLng[] polyLinePath = new LatLng[points.size()];
                for (int i = 0; i < points.size(); ++i) {
                    polyLinePath[i] = new LatLng(points.get(i).latitude, points.get(i).longitude);
                }
                if (mMap != null) {
                    mMap.addPolyline(new PolylineOptions().add(polyLinePath).color(Color.BLUE).geodesic(true).width(5));
                }
            }
            super.onPostExecute(points);
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