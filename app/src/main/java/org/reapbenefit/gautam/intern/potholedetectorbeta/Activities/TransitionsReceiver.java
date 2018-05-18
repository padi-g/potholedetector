package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

import org.reapbenefit.gautam.intern.potholedetectorbeta.NotificationHelper;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

public class TransitionsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(MainActivity.TRANSITIONS_RECEIVER_ACTION)) {
            Log.i("TransitionsReceiver", "Unsupported action: " + intent.getAction());
            return;
        }
        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                if (event.getActivityType() == DetectedActivity.STILL)
                    Log.i("TransitionReceiver", "STILL");
                else if (event.getActivityType() == DetectedActivity.WALKING)
                    Log.i("TransitionReceiver", "WALKING");
                else if (event.getActivityType() == DetectedActivity.IN_VEHICLE) {
                    //handling notification
                    NotificationHelper notificationHelper = new NotificationHelper(context);
                    Notification.Builder builder = notificationHelper.getNotification(
                            "Seems like you're in a vehicle",
                            "Tap to start tracking potholes!"
                    );
                    notificationHelper.notify(0, builder);
                }
                else if (event.getActivityType() == DetectedActivity.ON_FOOT) {
                    Log.i("TransitionReceiver", "ON_FOOT");
                }
                else {
                    Log.i("TransitionReceiver", "UNKNOWN");
                    //handling notification
                    NotificationHelper notificationHelper = new NotificationHelper(context);
                    Notification.Builder builder = notificationHelper.getNotification(
                            "Seems like you're in a vehicle",
                            "Tap to start tracking potholes!"
                    );
                    notificationHelper.notify(0, builder);
                }
            }
        }
    }
}
