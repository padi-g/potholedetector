package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

public class TransitionsReceiver extends IntentService {
    public TransitionsReceiver() {
        super("TransitionsReceiver");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onHandleIntent(Intent intent) {
       //check if intent contains data about an activity
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            //getting ArrayList of possible activities
            DetectedActivity detectedActivity = result.getMostProbableActivity();
            Log.i(getClass().getSimpleName(), detectedActivity.toString());
            if (detectedActivity.equals(DetectedActivity.IN_VEHICLE) && detectedActivity.getConfidence() >= 75) {
                //sending notification to user
                createNotificationChannel();
                Intent mainIntent = new Intent(this, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                        mainIntent, 0);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                        getString(R.string.notification_channel_id))
                        .setSmallIcon(R.drawable.cast_ic_notification_small_icon)
                        .setContentTitle("Looks like you're in a car")
                        .setContentText("Tap to start logging ")
                        .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
                notificationManagerCompat.notify(1337, builder.build());

                //committing changes to shared prefs
                SharedPreferences sharedPreferences = getSharedPreferences("ARS", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("inCar", true);
                editor.commit();
            }
            else
                Log.i(getClass().getSimpleName(), "Not in vehicle");
        }
    }

    private void createNotificationChannel() {
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(getString(R.string.notification_channel_id),
                    "TransitionsReceiver", importance);
            channel.setDescription("TransitionsReceiver");
            //registering channel with system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
