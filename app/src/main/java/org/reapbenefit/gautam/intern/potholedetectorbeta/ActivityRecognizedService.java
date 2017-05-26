package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.id;

/**
 * Created by gautam on 25/01/17.
 */

public class ActivityRecognizedService extends IntentService {

    public ActivityRecognizedService() {
        super("ActivityRecognizedService");
    }

    public ActivityRecognizedService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i("ActivityRecog Intent", "Connected to IntentService");

        if(ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            handleDetectedActivities( result.getProbableActivities() );
        }
    }

    private void handleDetectedActivities(List<DetectedActivity> probableActivities) {

        for( DetectedActivity activity : probableActivities ) {
            switch( activity.getType() ) {
                case DetectedActivity.IN_VEHICLE: {
                    Log.e( "ActivityRecogition", "In Vehicle: " + activity.getConfidence() );
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

                    }
                    break;
                }
                case DetectedActivity.ON_BICYCLE: {
                    Log.e( "ActivityRecogition", "On Bicycle: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    Log.e( "ActivityRecogition", "On Foot: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.RUNNING: {
                    Log.e( "ActivityRecogition", "Running: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.STILL: {
                    Log.e( "ActivityRecogition", "Still: " + activity.getConfidence() );

                    break;
                }
                case DetectedActivity.TILTING: {
                    Log.e( "ActivityRecogition", "Tilting: " + activity.getConfidence() );
                    break;
                }
                case DetectedActivity.WALKING: {
                    Log.e( "ActivityRecogition", "Walking: " + activity.getConfidence() );

                }
                case DetectedActivity.UNKNOWN: {
                    Log.e( "ActivityRecogition", "Unknown: " + activity.getConfidence() );
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


/**
 *  Put boolean and the activityRecognition result in the Intent
 *
 * Thumbs up and thumbs down icons for notification
 *
 * */