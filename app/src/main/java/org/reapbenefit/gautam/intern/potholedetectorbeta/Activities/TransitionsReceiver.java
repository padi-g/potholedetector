package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;

public class TransitionsReceiver extends IntentService {
    public TransitionsReceiver() {
        super("TransitionsReceiver");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onHandleIntent(Intent intent) {
       //check if intent contains data about an activity
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            //getting ArrayList of possible activities
            DetectedActivity detectedActivity = result.getMostProbableActivity();
            Log.i(getClass().getSimpleName(), detectedActivity.toString());
        }
    }
}
