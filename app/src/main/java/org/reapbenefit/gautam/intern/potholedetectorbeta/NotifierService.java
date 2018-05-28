package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MainActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.TransitionsReceiver;

public class NotifierService extends Service {
    private ActivityRecognitionClient activityRecognitionClient;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        return Service.START_STICKY;
    }
}
