package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.DetectedActivity;
import com.google.gson.Gson;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TrafficTimeService extends IntentService {
    private Date startTime;
    private Date newTime;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private String currentActivity;
    private final String TAG = getClass().getSimpleName();
    private long minutesWasted;
    public TrafficTimeService() {
        super("TrafficTimeService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String start = intent.getStringExtra("startTime");
        if (start != null)
            startTime = new Gson().fromJson(start, Date.class);
        else
            Log.i(TAG, "startTime is null");
        //getting current activity from TransitionReceiver
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        editor = sharedPreferences.edit();
        currentActivity = sharedPreferences.getString("currentActivity", null);
        if (currentActivity != null) {
            DetectedActivity activity = new Gson().fromJson(currentActivity, DetectedActivity.class);
            Log.i(TAG, currentActivity.toString());
            if (currentActivity.toString().contains("STILL") && activity.getConfidence() >= 60) {
                newTime = Calendar.getInstance().getTime();
                minutesWasted += newTime.getTime() - startTime.getTime();
                Log.i("minutesWasted", minutesWasted + "");
                startTime = newTime;
                editor.putLong("minutesWasted", minutesWasted);
                editor.commit();
            }
        }
        else
            Log.i(TAG, "currentActivity is null");
    }
}