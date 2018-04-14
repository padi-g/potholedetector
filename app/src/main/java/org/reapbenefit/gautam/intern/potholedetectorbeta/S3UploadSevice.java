package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;


import java.io.File;

public class S3UploadSevice extends IntentService {
    private TransferUtility transferUtility;

    NotificationManager mNotifyMgr;
    int mNotificationId = 101;
    NotificationCompat.Builder mBuilder;
    int uploadProgress = 0;
    SharedPreferences prefs;
    private static final String ACTION_UPLOAD_NOW = "upload_now";
    private static final String UPLOAD_URI = "upload_uri";
    private Util util;
    private String filename;

    public S3UploadSevice() {
        super("UploadService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        prefs = getSharedPreferences("uploads", MODE_PRIVATE);
        Log.d("Preferences", "Inside onHandleIntent Found file_delete = " + prefs.getBoolean("file_delete", false));

        mBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_upload)
                .setContentTitle("Road Quality Audit")
                .setContentText("Data is being Uploaded");

        mNotifyMgr =
                (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

        String action = intent.getAction();
        if(action.equals(ACTION_UPLOAD_NOW)){
            //initialising TransferUtility
            util = new Util();
            transferUtility = Util.getsTransferUtility(this);
            Uri uploadUri = intent.getParcelableExtra(UPLOAD_URI);
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
            uploadFile(uploadUri);
        }
    }

    private void uploadFile(Uri uri) {
        filename = uri.toString().substring(uri.toString().lastIndexOf('/')) ;
        File file = new File(uri.getPath());
        transferUtility.upload("***REMOVED***", file.toURI().getPath(),
                file);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    //tracking file upload progress
    private class UploadListener implements TransferListener {

        @Override
        public void onStateChanged(int id, TransferState state) {
            Log.d("S3UploadService", "onStateChanged: " + id + ", " + state);
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            Log.d("S3UploadService", String.format("onProgressChanged: %d, total: %d, current: %d",
                    id, bytesTotal, bytesCurrent));
        }

        @Override
        public void onError(int id, Exception ex) {
            Log.e("S3UploadService", "Error during upload: " + id, ex);
        }
    }
}
