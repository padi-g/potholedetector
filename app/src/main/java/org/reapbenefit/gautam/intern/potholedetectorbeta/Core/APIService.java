package org.reapbenefit.gautam.intern.potholedetectorbeta.Core;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;

import org.reapbenefit.gautam.intern.potholedetectorbeta.HTTPHandler;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.UserData;


public class APIService extends IntentService {

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
        userId = intent.getStringExtra("FIREBASE_USER_ID");
        if (requestMethod.equalsIgnoreCase("GET") && table.equalsIgnoreCase(getString(R.string.user_data_table))) {
            int userIndex = -1;
            String allUsersJson = HTTPHandler.getAllUsers();
            Log.d(TAG, allUsersJson + "");
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
                    HTTPHandler.insertUser(userId);
                }
            } catch (NullPointerException nullPointerException) {
                Log.e(TAG, nullPointerException.getMessage());
            }
        }
    }
}
