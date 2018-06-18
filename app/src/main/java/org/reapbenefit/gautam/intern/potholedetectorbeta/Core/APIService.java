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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.reapbenefit.gautam.intern.potholedetectorbeta.HTTPHandler;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;
import org.reapbenefit.gautam.intern.potholedetectorbeta.TripDataLambda;
import org.reapbenefit.gautam.intern.potholedetectorbeta.UserData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class APIService extends IntentService {

    private SharedPreferences dbPreferences;

    private String requestMethod;
    private final String USER_TABLE_URL = "https://990rl1xx1d.execute-api.ap-south-1.amazonaws.com/Beta/users/";
    private final String TRIPS_TABLE_URL = "https://990rl1xx1d.execute-api.ap-south-1.amazonaws.com/Beta/trips/";
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
            int userIndex = -1;
            String allUsersJson = HTTPHandler.getAllUsers();
            // Log.d("allUsersJson", allUsersJson + "");
            if (allUsersJson == null) {
                startService(intent);
            }
            try {
                UserData[] allUsers = new Gson().fromJson(allUsersJson, UserData[].class);
                //searching through array for desired UserID
                for (int i = 0; i < allUsers.length; ++i) {
                    if (allUsers[i].getUserID().equals(userId)) {
                        userIndex = i;
                        break;
                    }
                }
                if (userIndex == -1) {
                    //user not found in existing database, must send POST request to table
                    HTTPHandler.insertUser(userId, false);
                } else {
                    //user exists in database, may have taken some trips that need to be stored in RoomDB
                    String allTripsJson = HTTPHandler.getAllTrips();
                    if (allTripsJson == null) {
                        startService(intent);
                    }
                    TripDataLambda[] tripDataLambdas = new Gson().fromJson(allTripsJson, TripDataLambda[].class);
                    boolean foundUserId = false;
                    List<TripDataLambda> tripDataLambdaList = new ArrayList<>();
                    for (int i = 0; i < tripDataLambdas.length; ++i) {
                        if (tripDataLambdas[i].getUserID().equals(
                                getSharedPreferences("uploads", MODE_PRIVATE).getString("FIREBASE_USER_ID", null)
                        ) && !foundUserId) {
                            foundUserId = true;
                            tripDataLambdaList.add(tripDataLambdas[i]);
                        }
                        else if (foundUserId) {
                            break;
                        }
                    }
                    //need only the trip with highest number of potholes to stay consistent across instances
                    int maxPotholes = 0;
                    int maxIndex = -1;
                    for (int i = 0; i < tripDataLambdaList.size(); ++i) {
                        if (tripDataLambdaList.get(i).getDefinitePotholeCount() + tripDataLambdaList.get(i).getProbablePotholeCount() >= maxPotholes) {
                            maxPotholes = tripDataLambdaList.get(i).getDefinitePotholeCount() + tripDataLambdaList.get(i).getProbablePotholeCount();
                            maxIndex = i;
                        }
                    }
                    Trip highestPotholeTripApi = HTTPHandler.convertToTrip(tripDataLambdaList.get(maxIndex));
                    Intent highestPotholeIntent = new Intent(getString(R.string.highest_pothole_trip_broadcast));
                    highestPotholeIntent.putExtra(getString(R.string.highest_pothole_trip_broadcast), highestPotholeTripApi);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(highestPotholeIntent);
                }
            } catch (NullPointerException nullPointerException) {
                // Log.e(TAG, nullPointerException.getMessage());
            }
            catch (IllegalStateException illegal) {}
            catch (JsonSyntaxException json) {}
        }
        else if (requestMethod.equalsIgnoreCase("POST") && table.equalsIgnoreCase(getString(R.string.trip_data_table))) {
            Trip tripUploaded = (Trip) intent.getParcelableExtra("tripUploaded");
            if (tripUploaded != null) {
                //trip was uploaded, must update rather than insert into TripsData table
                TripDataLambda tripDataLambda = HTTPHandler.convertToTripDataLambda(tripUploaded);
                tripDataLambda.setUpdateUploadedFlag(true);
                HTTPHandler.updateUploadedStatus(tripDataLambda);
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
            }
            else {
                //save trip as JSON in SharedPreferences
                tripsNotInRDS.add(new Gson().toJson(intent.getParcelableExtra("newTrip")));
                dbPreferences.edit().putStringSet(getString(R.string.trips_not_in_RDS), tripsNotInRDS).commit();
            }
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
        updatedUserData.setUpdateFlag(true);
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
