package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.google.gson.Gson;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class S3UploadService extends IntentService {

    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private static final int MEGABYTES_PER_PART = 5;
    private String clientRegion;
    private String bucketName;
    private String keyName;
    private String filepath;
    private AmazonS3Client s3Client;
    private Uri uploadUri;
    private SharedPreferences sharedPreferences;
    int mNotificationId = 101;
    private static final String ACTION_UPLOAD_NOW = "upload_now";
    private static final String UPLOAD_URI = "upload_uri";
    private String userId;
    private final String TAG = getClass().getSimpleName();
    private String tripUploadedId;
    private SharedPreferences dbPreferences;
    private Trip tripUploaded;
    private boolean isWifiConnected;

    public S3UploadService() {
        super("S3UploadService");
    }

    public S3UploadService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(getClass().getSimpleName(), "insideOnHandleIntent");
        dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        tripUploaded = intent.getParcelableExtra("trip_object");
        if (tripUploaded == null)
            tripUploaded = new Gson().fromJson(dbPreferences.getString("uploadedTripJson", null), Trip.class);
        tripUploadedId = tripUploaded.getTrip_id();
        dbPreferences.edit().putString("uploadedTripJson", new Gson().toJson(tripUploaded)).commit();
        uploadUri = intent.getParcelableExtra(UPLOAD_URI);
        dbPreferences.edit().putString("uploadUriJson", new Gson().toJson(uploadUri)).commit();
        if (uploadUri == null)
            uploadUri = new Gson().fromJson(dbPreferences.getString("uploadUriJson", null), Uri.class);
        initialiseNotification();
        if (isInternetAvailable()) {

            sharedPreferences = getSharedPreferences("uploads", MODE_PRIVATE);
            userId = sharedPreferences.getString("FIREBASE_USER_ID", null);
            clientRegion = Region.getRegion(Regions.AP_SOUTH_1).getName();
            bucketName = getString(R.string.s3bucketname);
            if (uploadUri == null)
                return;
            filepath = uploadUri.toString().substring(uploadUri.toString().lastIndexOf('/'));

            dbPreferences.edit().putString("tripUploadedJson", tripUploadedId).commit();
            Log.d(TAG, "tripUploaded " + tripUploadedId);

            if (BuildConfig.DEBUG)
                keyName = "debug/" + userId + filepath;
            else
                keyName = "logs/" + userId + filepath;

            Log.d(getClass().getSimpleName(), keyName);
            Log.d(getClass().getSimpleName(), filepath);
            Log.d(getClass().getSimpleName(), bucketName);

            s3Client = new AmazonS3Client(Util.getsCredProvider(this));

            //each ETag identifies the different parts into which the file has been split
            List<PartETag> partETags = new ArrayList<>();
            InitiateMultipartUploadRequest initRequest = null;
            InitiateMultipartUploadResult initResult = null;
            File file = new File(uploadUri.getPath());
            CompleteMultipartUploadRequest completeMultipartUploadRequest = null;
            List<String> uploadIdList = new ArrayList<>();


            initRequest = new InitiateMultipartUploadRequest(bucketName, keyName);
            initResult = s3Client.initiateMultipartUpload(initRequest);

            final long contentLength = file.length();
            long partSize = MEGABYTES_PER_PART * 1024 * 1024;

            long filePosition = 0;
            for (int i = 1; filePosition < contentLength; ++i) {
                try {
                    Log.d(getClass().getSimpleName(), "part " + i);
                    partSize = Math.min(partSize, (contentLength - filePosition));
                    UploadPartRequest uploadPartRequest = new UploadPartRequest()
                            .withBucketName(bucketName)
                            .withKey(keyName)
                            .withUploadId(initResult.getUploadId())
                            .withPartNumber(i)
                            .withFileOffset(filePosition)
                            .withFile(file)
                            .withPartSize(partSize);

                    long megabytesUploaded = (i - 1) * partSize;
                    int progress = (int)(((double)filePosition/(double)contentLength) * 100.0);
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
                for (int i = 0; i < uploadIdList.size(); ++i) {
                    completeMultipartUploadRequest = new CompleteMultipartUploadRequest(
                            bucketName, keyName, uploadIdList.get(i), partETags
                    );
                    Log.d(TAG + " " + i, uploadIdList.get(i));
                    s3Client.completeMultipartUpload(completeMultipartUploadRequest);
                    Log.d("progress int", "100");
                    notificationBuilder.setContentText("100% uploaded");
                    notificationBuilder.setOngoing(false);
                    notificationManager.notify(mNotificationId, notificationBuilder.build());

                    //initiating DB update through TripViewModel
                    Intent dbUpdateIntent = new Intent(getString(R.string.set_uploaded_true));
                    dbUpdateIntent.putExtra("tripUploaded", tripUploaded);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(dbUpdateIntent);

                    dbPreferences.edit().putString("tripUploadedId", tripUploadedId).commit();
                }
            }
        }
        else {
            initialiseNotification();
            notificationBuilder.setContentText("Network connection unavailable. Please try again later.");
            notificationBuilder.setOngoing(false);
            notificationManager.notify(mNotificationId, notificationBuilder.build());
            dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
            dbPreferences.edit().putString("tripUploadedId", null).commit();
            stopSelf();
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (isInternetAvailable()) {
            Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
            restartServiceIntent.putExtra("upload_uri", uploadUri);
            restartServiceIntent.putExtra("trip_object", tripUploaded);
            restartServiceIntent.setPackage(getPackageName());
            PendingIntent restartServicePendingIntent = PendingIntent.getService(getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_ONE_SHOT);
            AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            alarmService.set(
                    AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + 1000,
                    restartServicePendingIntent
            );
        }
        super.onTaskRemoved(rootIntent);
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


    private void confirmConnectionNotification() {
        if (!isWifiConnected) {
            notificationBuilder.setOngoing(false)
                    .setContentText("Upload timed out. Check your network connection.");
            notificationManager.notify(mNotificationId, notificationBuilder.build());
            stopSelf();
        }
    }


    private void initialiseNotification() {
        notificationBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_upload)
                .setContentTitle("Road Quality Audit")
                .setContentText("Uploading pothole data")
                .setOngoing(true);
        notificationManager = (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
    }

    private class CheckWifiNoInternetAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                HttpURLConnection httpURLConnection = null;
                httpURLConnection = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
                httpURLConnection.setRequestProperty("User-Agent", "Test");
                httpURLConnection.setRequestProperty("Connection", "close");
                httpURLConnection.setConnectTimeout(1000);
                httpURLConnection.connect();
                isWifiConnected = (httpURLConnection.getResponseCode() == 200);
            }
            catch (IOException ioException) {
                isWifiConnected = false;
                Log.e(TAG, "WifiConnectionTest threw Exception");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            S3UploadService.this.confirmConnectionNotification();
        }
    }
}