package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MainActivity;

/**
 * Created by kanis on 19-05-2018.
 */

public class NotificationHelper extends ContextWrapper {
    private final String channel_id = "CHANNEL_ID";
    private NotificationManager notificationManager;
    private Context context;
    public NotificationHelper(Context context) {
        super(context);
        this.context = context;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    channel_id, "Channel ID", NotificationManager.IMPORTANCE_DEFAULT
            );
            //configuring channel's initial settings
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationChannel.enableVibration(true);
            notificationChannel.setShowBadge(true);
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
    public void notify(int id, Notification.Builder builder) {
        if (notificationManager != null) {
            notificationManager.notify(id, builder.build());
        }
        else
            Log.i(getClass().getSimpleName(), "Notification error");
    }
    public Notification.Builder getNotification(String title, String body) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(getApplicationContext(), channel_id)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setContentIntent(getPendingIntent());
        }
        return new Notification.Builder(getApplicationContext())
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(getPendingIntent());
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        //creating backstack for normal operation
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack((Activity) context);
        stackBuilder.addNextIntent(intent);
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
