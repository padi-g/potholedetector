package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MainActivity;

import java.util.List;

/**
 * Created by gautam on 25/01/17.
 */

public class ActivityRecognizedService extends IntentService {
    private boolean inCar;
    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }

    public ActivityRecognizedService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Log.i("ARS", "Connected to IntentService");

        if(ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            handleDetectedActivities( result.getProbableActivities() );
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {
        SharedPreferences sharedPreferences = getSharedPreferences("ARS", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        for( DetectedActivity activity : probableActivities ) {
            switch( activity.getType() ) {
                case DetectedActivity.IN_VEHICLE: {
                    // Log.i( "ActivityRecogition", "In Vehicle: " + activity.getConfidence() );
                    if( activity.getConfidence() >= 90 ) {

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
                        builder.setContentText( "Car motion detected. Start Trip?" );
                        builder.setSubText("Swipe if you are not in a car");
                        builder.setSmallIcon( R.mipmap.ic_launcher );
                        builder.setContentTitle( getString( R.string.app_name ) );
                        builder.setAutoCancel(true);

                        Intent resultIntent = new Intent(this, MainActivity.class);
                        resultIntent.putExtra("CarMode", true);
                        resultIntent.putExtra("ActivityType", activity.getType());
                        resultIntent.putExtra("Confidence", activity.getConfidence());


                        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                        stackBuilder.addParentStack(MainActivity.class);
                        stackBuilder.addNextIntent(resultIntent);
                        PendingIntent resultPendingIntent =
                                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                        builder.setContentIntent(resultPendingIntent);
                        NotificationManager mNotificationManager =
                                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                        mNotificationManager.notify(0, builder.build());

                        //to allow pothole logging
                        editor.putBoolean("inCar", inCar);
                        editor.commit();
                    }
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    // Log.i( "ActivityRecogition", "On Bicycle: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    // Log.i( "ActivityRecogition", "On Foot: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.RUNNING: {
                    // Log.i( "ActivityRecogition", "Running: " + activity.getConfidence() );
                    break;

                }
                case DetectedActivity.STILL: {
                    // Log.i( "ActivityRecogition", "Still: " + activity.getConfidence() );

                    break;
                }
                case DetectedActivity.TILTING: {
                    // Log.i( "ActivityRecogition", "Tilting: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.WALKING: {
                    // Log.i( "ActivityRecogition", "Walking: " + activity.getConfidence() );

                }
                case DetectedActivity.UNKNOWN: {
                    // Log.i( "ActivityRecogition", "Unknown: " + activity.getConfidence() );
                    break;
                }
            }
        }

    }
}


/**
 * If the notification is answered positively
 *
 * and in car
 *      and if the user is not driving, then ask the user to help with the marking. Add a do not show again option
 *          if the user is driving, set it in auto mode.
 * FOR LATER, in the contribute section, add an option for pedestrians to contribute to the app while walking
 *
 *      Create two different layouts
 */