package org.reapbenefit.gautam.intern.potholedetectorbeta.Core;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.TransitionsReceiver;

/**
 * Created by kanis on 23-05-2018.
 */

public class TransitionAlarm extends BroadcastReceiver {
    private ActivityRecognitionClient activityRecognitionClient;
    private final String TAG = getClass().getSimpleName();

    @SuppressLint("RestrictedApi")
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Firing TransitionReceiver");
        activityRecognitionClient = new ActivityRecognitionClient(context);
        //connecting to ARS every 3 seconds, checking for activity
        Task<Void> task = activityRecognitionClient.requestActivityUpdates(
                10000, PendingIntent.getService(context, 0,
                        new Intent(context, TransitionsReceiver.class),
                        PendingIntent.FLAG_UPDATE_CURRENT)
        );
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i(getClass().getSimpleName(), "Polling for activity successful");
            }
        });
    }
}
