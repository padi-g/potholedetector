package org.reapbenefit.gautam.intern.potholedetectorbeta.Activities;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

public class TransitionsReceiver extends IntentService {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private Intent mainIntent;
    private PendingIntent pendingIntent;
    private NotificationCompat.Builder builder;
    private NotificationManagerCompat notificationManagerCompat;
    public TransitionsReceiver() {
        super("TransitionsReceiver");
    }

    @Override
    public void onCreate() {
        //try {
            super.onCreate();
            /*sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
            editor = sharedPreferences.edit();
            createNotificationChannel();
            mainIntent = new Intent(this, MainActivity.class);
            mainIntent.putExtra("inCar", true);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            pendingIntent = PendingIntent.getActivity(this, 0,
                    mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder = new NotificationCompat.Builder(this,
                    getString(R.string.notification_channel_id))
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("Looks like you're in a car")
                    .setContentText("Tap to start logging potholes.")
                    .setPriority(NotificationManager.IMPORTANCE_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            notificationManagerCompat = NotificationManagerCompat.from(this);
        }
        catch (Exception e) {
            Log.i(getClass().getSimpleName(), "Exception found");
        }*/
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onHandleIntent(Intent intent) {
        //setting up notification system
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        editor = sharedPreferences.edit();
        createNotificationChannel();
        mainIntent = new Intent(this, MainActivity.class);
        mainIntent.putExtra("inCar", true);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0,
                mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder = new NotificationCompat.Builder(this,
                getString(R.string.notification_channel_id))
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Looks like you're in a car")
                .setContentText("Tap to start logging potholes.")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        notificationManagerCompat = NotificationManagerCompat.from(this);
       //check if intent contains data about an activity
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity detectedActivity = result.getMostProbableActivity();
            Log.i(getClass().getSimpleName(), detectedActivity.toString());
            //committing current activity to shared prefs
            editor.putString("currentActivity", detectedActivity.toString());
            editor.commit();
            if (detectedActivity.toString().contains("VEHICLE")) {
                //sending notification to user
                notificationManagerCompat.notify(0, builder.build());
            }
            else {
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
