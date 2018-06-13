package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.mobileconnectors.s3.transfermanager.MultipleFileUpload;
import com.amazonaws.mobileconnectors.s3.transfermanager.PersistableUpload;
import com.amazonaws.mobileconnectors.s3.transfermanager.Transfer;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferProgress;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;

import java.io.File;
import java.util.Calendar;
import java.util.List;


public class S3MultipleFileUploadService extends IntentService {

    private TransferManager transferManager;
    private String keyName;
    private SharedPreferences sharedPreferences;
    private String userId;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private int mNotificationId = 101;

    public S3MultipleFileUploadService() {
        super("S3MultipleFileUploadService");
    }

    public S3MultipleFileUploadService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        transferManager = new TransferManager(Util.getsCredProvider(this));
        keyName = "debug/" + userId;
        File directory = new File(getApplicationContext().getFilesDir() + "/logs");
        MultipleFileUpload upload = transferManager.uploadDirectory(getString(R.string.s3bucketname), keyName + "/", directory, true);
        notificationManager.notify(mNotificationId, notificationBuilder.build());
        final TransferProgress transferProgress = upload.getProgress();
        int progress = (int)((double)transferProgress.getBytesTransferred()/(double)transferProgress.getTotalBytesToTransfer() * 100.0);
        notificationBuilder.setContentText(progress + "% uploaded");
        notificationManager.notify(mNotificationId, notificationBuilder.build());
        if (upload.getState() == Transfer.TransferState.Failed) {
            notificationBuilder.setContentText("Upload failed");
            notificationManager.notify(mNotificationId, notificationBuilder.build());
        }
        upload.addProgressListener(new ProgressListener() {
            @Override
            public void progressChanged(ProgressEvent progressEvent) {
                int progress = (int)((double)progressEvent.getBytesTransferred()/(double)transferProgress.getTotalBytesToTransfer() * 100.0);
                notificationBuilder.setContentText(progress + "% uploaded");
                if (progressEvent.getEventCode() == ProgressEvent.FAILED_EVENT_CODE) {
                    notificationBuilder.setContentText("Upload failed");
                    notificationBuilder.setOngoing(false);
                }
                notificationManager.notify(mNotificationId, notificationBuilder.build());
            }
        });
        notificationBuilder.setContentText("Upload complete");
        notificationBuilder.setOngoing(true);
        notificationManager.notify(mNotificationId, notificationBuilder.build());
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
                .setContentText("Uploading all files")
                .setOngoing(true);
        notificationManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
    }
}
