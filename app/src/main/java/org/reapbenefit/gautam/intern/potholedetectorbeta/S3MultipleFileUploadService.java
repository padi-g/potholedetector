package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
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
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class S3MultipleFileUploadService extends IntentService {

    private static final int MEGABYTES_PER_PART = 5;
    private TransferManager transferManager;
    private String keyName;
    private SharedPreferences sharedPreferences;
    private String userId;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private int mNotificationId = 101;
    private List<File> fileList = new ArrayList<>();
    private String clientRegion;
    private String filepath;
    private String bucketName;
    private AmazonS3Client s3Client;
    private final String TAG = getClass().getSimpleName();

    public S3MultipleFileUploadService() {
        super("S3MultipleFileUploadService");
    }

    public S3MultipleFileUploadService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        File folder = new File(String.valueOf(getApplicationContext().getFilesDir()));
        listFilesForFolder(folder);
        //initialising uniform upload credentials, common to all files
        Log.d(TAG, "inside onHandleIntent");
        if (isInternetAvailable()) {
            userId = getSharedPreferences("uploads", MODE_PRIVATE).getString("FIREBASE_USER_ID", null);
            clientRegion = Region.getRegion(Regions.AP_SOUTH_1).getName();
            bucketName = getString(R.string.s3bucketname);
            if (BuildConfig.DEBUG)
                keyName = "debug/" + userId;
            else
                keyName = "logs/" + userId;
            for (int i = 0; i < fileList.size(); ++i) {
                File file = fileList.get(i);
                s3Client = new AmazonS3Client(Util.getsCredProvider(this));
                List<PartETag> partETags = new ArrayList<>();
                InitiateMultipartUploadRequest initRequest = null;
                InitiateMultipartUploadResult initResult = null;
                CompleteMultipartUploadRequest completeMultipartUploadRequest = null;
                List<String> uploadIdList = new ArrayList<>();


                initRequest = new InitiateMultipartUploadRequest(bucketName, keyName);
                initResult = s3Client.initiateMultipartUpload(initRequest);

                final long contentLength = file.length();
                long partSize = MEGABYTES_PER_PART * 1024 * 1024;

                long filePosition = 0;
                for (int j = 1; filePosition < contentLength; ++j) {
                    try {
                        Log.d(getClass().getSimpleName(), "part " + j);
                        partSize = Math.min(partSize, (contentLength - filePosition));
                        UploadPartRequest uploadPartRequest = new UploadPartRequest()
                                .withBucketName(bucketName)
                                .withKey(keyName)
                                .withUploadId(initResult.getUploadId())
                                .withPartNumber(j)
                                .withFileOffset(filePosition)
                                .withFile(file)
                                .withPartSize(partSize);

                        long megabytesUploaded = (j - 1) * partSize;
                        int progress = (int) (((double) filePosition / (double) contentLength) * 100.0);
                        Log.d("progress fp", String.valueOf(filePosition));
                        Log.d("progress contentLength", String.valueOf(contentLength));
                        Log.d("progress int", String.valueOf(progress));
                        notificationBuilder.setContentText(progress + "% uploaded");
                        notificationManager.notify(mNotificationId, notificationBuilder.build());

                        UploadPartResult uploadPartResult = s3Client.uploadPart(uploadPartRequest);
                        partETags.add(uploadPartResult.getPartETag());
                        filePosition += partSize;
                        uploadIdList.add(initResult.getUploadId());
                        Log.d(getClass().getSimpleName(), partETags.toString());
                    } catch (Exception e) {
                        Log.e(getClass().getSimpleName(), e.getMessage());
                        notificationBuilder.setContentText("Upload failed");
                        notificationBuilder.setOngoing(false);
                        notificationManager.notify(mNotificationId, notificationBuilder.build());
                        break;
                    }
                }
                if (uploadIdList != null) {
                    for (int j = 0; j < uploadIdList.size(); ++j) {
                        completeMultipartUploadRequest = new CompleteMultipartUploadRequest(
                                bucketName, keyName, uploadIdList.get(j), partETags
                        );
                        Log.d(TAG + " " + j, uploadIdList.get(j));
                        s3Client.completeMultipartUpload(completeMultipartUploadRequest);
                        Log.d("progress int", "100");
                        notificationBuilder.setContentText("100% uploaded");
                        notificationBuilder.setOngoing(false);
                        notificationManager.notify(mNotificationId, notificationBuilder.build());

                        /*//initiating DB update through TripViewModel
                        Intent dbUpdateIntent = new Intent(getString(R.string.set_uploaded_true));
                        dbUpdateIntent.putExtra("tripUploaded", tripUploaded);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(dbUpdateIntent);

                        dbPreferences.edit().putString("tripUploadedId", tripUploadedId).commit();*/
                    }
                }
            }
        }
        else {
            initialiseNotification();
            notificationBuilder.setContentText("Network connection unavailable. Please try again later.");
            notificationBuilder.setOngoing(false);
            notificationManager.notify(mNotificationId, notificationBuilder.build());
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
            sharedPreferences.edit().putString("tripUploadedId", null).commit();
            stopSelf();
        }
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();
        if (!isConnected)
            return false;
        //TODO: HOW TO FIX THIS WHEN ASYNCTASK MIGHT TAKE TIME TO COMPLETE?
        //new CheckWifiNoInternetAsyncTask().execute();
        return true;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("uploads", MODE_PRIVATE);
        userId = sharedPreferences.getString("FIREBASE_USER_ID", null);
        initialiseNotification();
    }

    public void listFilesForFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                fileList.add(fileEntry);
            }
        }
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
