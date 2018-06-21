package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.gson.Gson;

import org.reapbenefit.gautam.intern.potholedetectorbeta.BuildConfig;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

import java.sql.Time;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/*
TransitionReciever contains the logic for detection of the current activity. 
Sends notification to user if he is in a vehicle and also stores the current status of the activity 
(picked up by EasyModeFragment later to check for fraud data logging)
*/

public class TransitionsReceiver extends IntentService {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Intent mainIntent;
    private PendingIntent pendingIntent;
    private NotificationCompat.Builder builder;
    private NotificationManagerCompat notificationManagerCompat;
    private long timer;
    private long minutesWasted;
    private final long NOTIFICATION_TIME_THRESHOLD = 60 * 1000 * 60;

    public TransitionsReceiver() {
        super("TransitionsReceiver");
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onHandleIntent(Intent intent) {
        //setting up notification system
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        editor = sharedPreferences.edit();
        minutesWasted = sharedPreferences.getLong("minutesWasted", 0);
        timer = sharedPreferences.getLong("timer", Calendar.getInstance().getTimeInMillis());
        editor.putLong("timer", timer).commit();
        createNotificationChannel();
        mainIntent = new Intent(this, MainActivity.class);
        mainIntent.putExtra("inCar", true);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0,
                mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder = new NotificationCompat.Builder(this,
                getString(R.string.notification_channel_id))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Are you in a car?")
                .setContentText("Tap to start logging potholes.")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        notificationManagerCompat = NotificationManagerCompat.from(this);
        long currentTime = Calendar.getInstance().getTimeInMillis();
        timer = currentTime - timer;
        // Log.d("timer", timer + "");
        //check if intent contains data about an activity
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity detectedActivity = result.getMostProbableActivity();
            Log.d(getClass().getSimpleName(), detectedActivity.toString());
            //committing current activity to shared prefs
            if (detectedActivity != null) {
                editor.putString("currentActivity", new Gson().toJson(detectedActivity).toString()).commit();
            }
            if (detectedActivity.toString().contains("VEHICLE") && !ApplicationClass.getInstance().isTripInProgress()
                    && timer >= NOTIFICATION_TIME_THRESHOLD && detectedActivity.getConfidence() >= 30) {
                //sending notification to user
                notificationManagerCompat.notify(0, builder.build());
                editor.remove("timer").commit();
            } else {
                boolean tripInProgress = ApplicationClass.getInstance().isTripInProgress();
                if (detectedActivity.toString().contains("STILL") && tripInProgress) {
                    long startTrafficTime = sharedPreferences
                            .getLong(getString(R.string.traffic_time_start), Calendar.getInstance().getTimeInMillis());
                    minutesWasted += Calendar.getInstance().getTimeInMillis() - startTrafficTime;
                    startTrafficTime = Calendar.getInstance().getTimeInMillis();
                    editor.putLong(getString(R.string.traffic_time_start), startTrafficTime);
                    editor.putLong("minutesWasted", minutesWasted);
                    editor.commit();
                } else {
                    editor.remove(getString(R.string.traffic_time_start)).commit();
                }
                if (notificationManagerCompat != null) {
                    notificationManagerCompat.cancel(0);
                }
            }
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
