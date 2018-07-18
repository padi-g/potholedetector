package org.reapbenefit.gautam.intern.potholedetectorbeta.Core;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.reapbenefit.gautam.intern.potholedetectorbeta.HTTPHandler;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;
import org.reapbenefit.gautam.intern.potholedetectorbeta.TripDataLambda;
import org.reapbenefit.gautam.intern.potholedetectorbeta.UniquePotholeDataLambda;
import org.reapbenefit.gautam.intern.potholedetectorbeta.UserData;
import org.reapbenefit.gautam.intern.potholedetectorbeta.UserPothole;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class APIService extends IntentService {

    private SharedPreferences dbPreferences;

    private String requestMethod;
    private String table;
    private String userId;
    private final String TAG = getClass().getSimpleName();

    public APIService() {
        super("APIService");
    }
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        requestMethod = intent.getStringExtra("request");
        table = intent.getStringExtra("table");
        userId = getSharedPreferences("uploads", MODE_PRIVATE).getString("FIREBASE_USER_ID", null);
        if (requestMethod.equalsIgnoreCase("GET") && table.equalsIgnoreCase(getString(R.string.user_data_table))) {
            try {
                String userJson = HTTPHandler.getUser(userId);
                if (!userJson.contains("{")) {
                    // Log.d(TAG, "Inserting new user");
                    // user not found in existing database, must send POST request to table
                    HTTPHandler.insertUser(userId);
                } else {
                    // user exists in database, must retrieve highest pothole trip and store in RoomDB
                    // TODO: RETRIEVE ALL TRIP METADATA AND UPDATE USER NET POTHOLE COUNTS AND MARKER LOCATIONS
                    String highestPotholeTripReceivedJson = HTTPHandler.getHighestPotholeTrip(userId);
                    Trip highestPotholeTripReceived = HTTPHandler.convertToTrip(new Gson().fromJson(highestPotholeTripReceivedJson, TripDataLambda.class));
                    Intent highestPotholeIntent = new Intent(getString(R.string.highest_pothole_trip_broadcast));
                    highestPotholeIntent.putExtra(getString(R.string.highest_pothole_trip_broadcast), highestPotholeTripReceived);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(highestPotholeIntent);
                }
            } catch (NullPointerException nullPointerException) {
                // Log.e(TAG, nullPointerException.getMessage());
            }
            catch (IllegalStateException illegal) {}
            catch (JsonSyntaxException json) {}
        }
        else if (requestMethod.equalsIgnoreCase("POST") && table.equalsIgnoreCase(getString(R.string.trip_data_table))) {

            try {
                Trip updatedTrip = (Trip) intent.getParcelableExtra("tripUploaded");
                if (updatedTrip != null) {
                    // must update rather than insert into TripsData table
                    HTTPHandler.updateTrip(updatedTrip);
                    return;
                }
                dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
                Set<String> tripsNotInRDS = dbPreferences.getStringSet(getString(R.string.trips_not_in_RDS),
                        new HashSet<String>());
                List<String> tripsNotInRDSList = new ArrayList<>(tripsNotInRDS);
                //sending a POST request to /trips API
                if (isInternetAvailable()) {
                    HTTPHandler.insertTrip((Trip) intent.getParcelableExtra("newTrip"));
                    updateCorrespondingUserData((Trip) intent.getParcelableExtra("newTrip"));
                    //checking for other trips whose metadata is not online
                    for (int i = 0; i < tripsNotInRDS.size(); ++i) {
                        Trip syncTrip = new Gson().fromJson(tripsNotInRDSList.get(i), Trip.class);
                        HTTPHandler.insertTrip(syncTrip);
                        tripsNotInRDS.remove(syncTrip);
                        updateCorrespondingUserData(syncTrip);
                    }
                    dbPreferences.edit().putStringSet(getString(R.string.trips_not_in_RDS), tripsNotInRDS).commit();
                } else {
                    //save trip as JSON in SharedPreferences
                    tripsNotInRDS.add(new Gson().toJson(intent.getParcelableExtra("newTrip")));
                    dbPreferences.edit().putStringSet(getString(R.string.trips_not_in_RDS), tripsNotInRDS).commit();
                }
            } catch (NullPointerException nullPointerException) {
                // Log.e(TAG, nullPointerException.getMessage());
            }
            catch (IllegalStateException illegal) {}
            catch (JsonSyntaxException json) {}
        }
        else if (requestMethod.equalsIgnoreCase("GET") && table.equalsIgnoreCase("UniquePotholes")) {
            String uniquePotholesJson = HTTPHandler.getAllPotholes();
            // Log.d(TAG, uniquePotholesJson + "");
            try {
                UniquePotholeDataLambda[] uniquePotholeDataLambdas = new Gson().fromJson(uniquePotholesJson, UniquePotholeDataLambda[].class);
                LatLng[] potholeLatLngs = new LatLng[uniquePotholeDataLambdas.length];
                int[] uniquePotholeHits = new int[uniquePotholeDataLambdas.length];
                if (uniquePotholeDataLambdas != null) {
                    for (int i = 0; i < uniquePotholeDataLambdas.length; ++i) {
                        potholeLatLngs[i] = new LatLng(uniquePotholeDataLambdas[i].getLat(),
                                uniquePotholeDataLambdas[i].getLng());
                        uniquePotholeHits[i] = uniquePotholeDataLambdas[i].getHits();
                    }
                }
                Intent potholeLatLngsIntent = new Intent(getString(R.string.global_unique_pothole_locations));
                potholeLatLngsIntent.putExtra(getString(R.string.global_unique_pothole_locations), potholeLatLngs);
                potholeLatLngsIntent.putExtra(getString(R.string.hits_unique_potholes), uniquePotholeHits);
                LocalBroadcastManager.getInstance(this).sendBroadcast(potholeLatLngsIntent);
            } catch (Exception exception) {
                // Log.e(TAG, exception.getMessage());
            }
        }
        else if (requestMethod.equalsIgnoreCase("POST") && table.equalsIgnoreCase("UserPotholes")) {
            UserPothole userPothole = (UserPothole) intent.getSerializableExtra("userPotholeObject");
            HTTPHandler.insertUserPothole(userPothole);
        }
    }

    private void updateCorrespondingUserData(Trip syncTrip) {
        UserData updatedUserData = new UserData();
        // Log.d(TAG, userId + "");
        updatedUserData.setUserID(userId);
        updatedUserData.setImprobable(syncTrip.getProbablePotholeCount());
        updatedUserData.setProbable(syncTrip.getDefinitePotholeCount());
        updatedUserData.setTotalDistance(syncTrip.getDistanceInKM());
        updatedUserData.setTotalTime((int) syncTrip.getDuration());
        HTTPHandler.insertUser(updatedUserData);
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();
        if (!isConnected)
            return false;
        //TODO: HOW TO FIX THIS WHEN ASYNCTASK MIGHT TAKE TIME TO COMPLETE?
        //new CheckWifiNoInternetAsyncTask().execute();
        return true;
    }
}
