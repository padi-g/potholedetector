package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;


import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.google.gson.Gson;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MainActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;


import java.io.File;

public class S3UploadSevice extends IntentService {
    private TransferUtility transferUtility;
    private TransferObserver observer;
    NotificationManager mNotifyMgr;
    int mNotificationId = 101;
    NotificationCompat.Builder mBuilder;
    int uploadProgress = 0;
    SharedPreferences prefs;
    private static final String ACTION_UPLOAD_NOW = "upload_now";
    private static final String UPLOAD_URI = "upload_uri";
    private Util util;
    private String filename;
    private TransferManager transferManager;

    private String tripJson;

    private Handler mHandler;
    public S3UploadSevice() {
        super("UploadService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
    }

    private void initialiseNotification() {
        mBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_upload)
                .setContentTitle("Road Quality Audit")
                .setContentText("Data is being Uploaded")
                .setOngoing(true);

        mNotifyMgr =
                (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        prefs = getSharedPreferences("uploads", MODE_PRIVATE);
        Log.d("Preferences", "Inside onHandleIntent Found file_delete = " + prefs.getBoolean("file_delete", false));

        transferManager = new TransferManager(Util.getsCredProvider(getApplicationContext()));

        tripJson = intent.getStringExtra("trip_json");
        Log.d("TripJSON", tripJson);

        String action = intent.getAction();
        if (action.equals(ACTION_UPLOAD_NOW)) {
            //initialising TransferUtility
            util = new Util();
            transferUtility = Util.getsTransferUtility(this);
            Uri uploadUri = intent.getParcelableExtra(UPLOAD_URI);
            //mNotifyMgr.notify(mNotificationId, mBuilder.build());
            uploadFile(uploadUri);
        }
    }

    private void uploadFile(Uri uri) {
        filename = uri.toString().substring(uri.toString().lastIndexOf('/'));
        File file = new File(uri.getPath());
        String userID = prefs.getString("FIREBASE_USER_ID", null);
        Log.d("User ID", userID);
        observer = null;
        if (BuildConfig.DEBUG) {
            try {
                observer = transferUtility.upload("***REMOVED***",
                        "debug/" + userID + filename,
                        file);
                initialiseNotification();
            }
            catch (Exception e){
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(S3UploadSevice.this, "File not found on device", Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }
        } else {
            String[] debug_ids = getResources().getStringArray(R.array.debugging_ids);
            //checking if current user ID is a debug ID
            boolean debug_id_flag = false;
            for (int i = 0; i < debug_ids.length; ++i) {
                if (debug_ids[i] == userID) {
                    try {
                        observer = transferUtility.upload("***REMOVED***",
                                "debug/" + userID + filename,
                                file);
                        debug_id_flag = true;
                        break;
                    }
                    catch (Exception e) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(S3UploadSevice.this, "File not found on device", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                }
            }
            if (!debug_id_flag) {
                try {
                    observer = transferUtility.upload("***REMOVED***",
                            "logs/" + userID + filename,
                            file);
                }
                catch (Exception e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(S3UploadSevice.this, "File not found on device", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
            }
        }
        //adding listener to monitor progress of upload
        observer.setTransferListener(new UploadListener());
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void publishProgress(int progress) {
        Log.i("Progress", "old method: " + progress);
        if (progress == -1)
            mBuilder.setContentText("Upload paused");
        else if (progress == -2)
            mBuilder.setContentText("Upload failed");
        else if (progress == -3)
            mBuilder.setContentText("Waiting for network");
        else if (progress == -4)
            mBuilder.setContentText("Network connection interrupted. Resuming upload...");
        else {
            mBuilder.setProgress(100, progress, false);
            if (progress == 100) {
                mBuilder.setContentText("Upload Complete");
                mBuilder.setOngoing(false);//notification can now be swiped away
                /*mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Gson gson = new Gson();
                        Trip trip = gson.fromJson(tripJson, Trip.class);
                        Log.d("TripID", trip.getTrip_id() + "");
                        trip.setUploaded(true);
                    }
                });*/
                Gson gson = new Gson();
                Trip trip = gson.fromJson(tripJson, Trip.class);
                Log.d("TripID", trip.getTrip_id() + "");
                trip.setUploaded(true);
            } else {
                mBuilder.setContentText(progress + "% done");
                mBuilder.setOngoing(true);
            }
        }
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    private class UploadListener implements TransferListener {

        @Override
        public void onStateChanged(int id, TransferState state) {
            Log.d("S3UploadService", "onStateChanged: " + id + ", " + state);
            boolean tryPause = false;
            if (state == TransferState.PAUSED) {
                publishProgress(-1);
                //saving state of upload
                tryPause = transferUtility.pause(id);
            }
            else if (state == TransferState.WAITING_FOR_NETWORK) {
                publishProgress(-3);
                tryPause = transferUtility.pause(id);
            }
            else if (state == TransferState.PART_COMPLETED) {
                publishProgress(-4);
                tryPause = transferUtility.pause(id);
            }
            else if (state == TransferState.IN_PROGRESS) {
                TransferObserver resumed = transferUtility.resume(id);
                observer.setTransferListener(new UploadListener());
            }
        }

        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            Log.d("S3UploadService", String.format("onProgressChanged: %d, total: %d, current: %d",
                    id, observer.getBytesTotal(), observer.getBytesTransferred()));
            bytesCurrent = observer.getBytesTransferred();
            bytesTotal = observer.getBytesTotal();
            uploadProgress = (int) (((double) bytesCurrent / (double) bytesTotal) * 100.0);
            publishProgress(uploadProgress);
        }

        @Override
        public void onError(int id, Exception ex) {
            Log.e("S3UploadService", "Error during upload: " + id, ex);
            publishProgress(-2);
        }
    }
}
