package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MainActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.TransitionsReceiver;

public class NotifierService extends IntentService {
    private ActivityRecognitionClient activityRecognitionClient;

    public NotifierService() {
        super("NotifierService");
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        activityRecognitionClient = new ActivityRecognitionClient(getApplicationContext());
        //connecting to ARS every 3 seconds, checking for activity
        Task<Void> task = activityRecognitionClient.requestActivityUpdates(
                3000, PendingIntent.getService(this, 0,
                        new Intent(this, TransitionsReceiver.class),
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
