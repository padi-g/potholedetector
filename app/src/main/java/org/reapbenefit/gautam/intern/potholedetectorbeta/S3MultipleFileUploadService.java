package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;

import java.io.File;
import java.util.Calendar;


public class S3MultipleFileUploadService extends IntentService {

    private TransferManager transferManager;
    private String keyName;
    private SharedPreferences sharedPreferences;
    private String userId;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;

    public S3MultipleFileUploadService() {
        super("S3MultipleFileUploadService");
    }

    public S3MultipleFileUploadService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        transferManager = new TransferManager(Util.getsCredProvider(this));
        keyName = userId + Calendar.getInstance().getTime();
        File directory = new File(getApplicationContext().getFilesDir() + "/logs");
        transferManager.uploadDirectory(getString(R.string.s3bucketname), keyName, directory, true);
        
    }


    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("uploads", MODE_PRIVATE);
        userId = sharedPreferences.getString("FIREBASE_USER_ID", null);
        initialiseNotification();
    }

    private void initialiseNotification() {
        notificationBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_upload)
                .setContentTitle("Road Quality Audit")
                .setContentText("Uploading pothole data")
                .setOngoing(true);
        notificationManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
    }
}
