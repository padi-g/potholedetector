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
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.google.gson.Gson;
import com.microsoft.azure.storage.blob.BlockEntry;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.core.Base64;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.APIService;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;


public class S3UploadService extends IntentService {

    private NotificationCompat.Builder notificationBuilder;
    private NotificationManager notificationManager;
    private static final int KILOBYTES_PER_PART = 512;
    private String clientRegion;
    private String bucketName;
    private String keyName;
    private String filepath;
    private CloudBlobClient blobClient;
    private Uri uploadUri;
    private SharedPreferences sharedPreferences;
    int mNotificationId = 101;
    private static final String ACTION_UPLOAD_NOW = "upload_now";
    private static final String UPLOAD_URI = "upload_uri";
    private String userId;
    private final String TAG = getClass().getSimpleName();
    private String tripUploadedId;
    private SharedPreferences dbPreferences;
    private SharedPreferences.Editor dbPreferencesEditor;
    private SharedPreferences settingsPreferences;
    private Trip tripUploaded;
    private List<Trip> tripList;
    private boolean isWifiConnected;
    private boolean fileDelete;

    public S3UploadService() {
        super("S3UploadService");
    }

    public S3UploadService(String name) {
        super(name);
    }

    @Override
    public void onDestroy() {
        dbPreferencesEditor.putBoolean("batchUpload", false).commit();
        super.onDestroy();
    }

    private void stopProgressBar() {
        Intent stopProgressBarIntent = new Intent(getString(R.string.upload_start_notifier_intent));
        dbPreferencesEditor.remove("tripUploadedId").commit();
    }

    private void startProgressBar(String tripUploadingId) {
        Intent startProgressBarIntent = new Intent(getString(R.string.upload_start_notifier_intent));
        dbPreferencesEditor.remove("tripUploadedId");
    }

    private void notifyConnectionTimeout() {
        initialiseNotification();
        notificationBuilder.setContentText("Network connection unavailable. Please try again later.");
        notificationBuilder.setOngoing(false);
        notificationManager.notify(mNotificationId, notificationBuilder.build());
        dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
        dbPreferencesEditor = dbPreferences.edit();
        dbPreferencesEditor.putString("tripUploadedId", null).commit();
        stopProgressBar();
        stopSelf();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            // Log.d(getClass().getSimpleName(), "insideOnHandleIntent");
            settingsPreferences = getSharedPreferences("uploads", MODE_PRIVATE);
            fileDelete = settingsPreferences.getBoolean(getString(R.string.file_delete_setting), false);
            dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());
            dbPreferencesEditor = dbPreferences.edit();
            settingsPreferences = getSharedPreferences("uploads", MODE_PRIVATE);
            tripList = (List<Trip>) intent.getSerializableExtra("trip_arrayList");
            if (tripList != null && !tripList.isEmpty()) {
                tripUploaded = tripList.get(0);
                // Log.d("tripUploaded", tripUploaded.getTrip_id());
                if (tripUploaded == null)
                    tripUploaded = new Gson().fromJson(dbPreferences.getString("uploadedTripJson", null), Trip.class);
                tripUploadedId = tripUploaded.getTrip_id();
                //startProgressBar(tripUploadedId);
                dbPreferencesEditor.putString("uploadedTripJson", new Gson().toJson(tripUploaded)).commit();
                uploadUri = intent.getParcelableExtra(UPLOAD_URI);
                dbPreferencesEditor.putString("uploadUriJson", new Gson().toJson(uploadUri)).commit();
                if (uploadUri == null)
                    uploadUri = Uri.fromFile(new File(getApplicationContext().getFilesDir() + "/logs/" + tripUploadedId + ".csv"));
                initialiseNotification();
                if (isInternetAvailable()) {

                    sharedPreferences = getSharedPreferences("uploads", MODE_PRIVATE);
                    userId = sharedPreferences.getString("FIREBASE_USER_ID", null);
                    clientRegion = Region.getRegion(Regions.AP_SOUTH_1).getName();
                    bucketName = getString(R.string.s3bucketname);
                    filepath = uploadUri.toString().substring(uploadUri.toString().lastIndexOf('/'));

                    dbPreferencesEditor.putString("tripUploadedJson", tripUploadedId).commit();
                    // Log.d(TAG, "tripUploaded " + tripUploadedId);

                    try {
                        blobClient = BlobClientProvider.getBlobClientReference();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                    }

                    CloudBlobContainer cloudBlobContainer = null;

                    if (BuildConfig.DEBUG)
                        cloudBlobContainer = blobClient.getContainerReference("debug");
                    else
                        cloudBlobContainer = blobClient.getContainerReference("logs");

                    cloudBlobContainer.createIfNotExists();

                    CloudBlockBlob blockBlob = cloudBlobContainer.getBlockBlobReference(userId + filepath);

                    FileInputStream fileInputStream = null;
                    try {
                        fileInputStream = new FileInputStream(uploadUri.getPath());

                        // splitting the file into blocks of 512 KB each
                        int blockNum = 0;
                        String blockId = null;
                        String blockIdEncoded = null;
                        int totalBlocks = (int) (Math.ceil((double) (fileInputStream.available()) / (double) (KILOBYTES_PER_PART * 1024)));
                        ArrayList<BlockEntry> blockList = new ArrayList<>();

                        while (fileInputStream.available() > (KILOBYTES_PER_PART * 1024)) {
                            try {
                                blockId = String.format("%05d", blockNum);
                                blockIdEncoded = Base64.encode(blockId.getBytes());
                                blockBlob.uploadBlock(blockIdEncoded, fileInputStream, (KILOBYTES_PER_PART * 1024));
                                blockList.add(new BlockEntry(blockIdEncoded));
                                blockNum++;
                                int progress = (int) (((double) blockNum / (double) totalBlocks) * 100.0);
                                notificationBuilder.setProgress(100, progress, false);
                                notificationManager.notify(mNotificationId, notificationBuilder.build());
                            } catch (Exception e) {
                                notifyConnectionTimeout();
                                break;
                            }
                        }
                        // uploading last block
                        blockId = String.format("%05d", blockNum);
                        blockIdEncoded = Base64.encode(blockId.getBytes());
                        blockBlob.uploadBlock(blockIdEncoded, fileInputStream, fileInputStream.available());
                        blockList.add(new BlockEntry(blockIdEncoded));

                        // Commit the blocks
                        blockBlob.commitBlockList(blockList);

                        // notifying the user that upload is complete
                        notificationBuilder.setProgress(0, 0, false).setContentText("Upload complete")
                                .setOngoing(false);
                        notificationManager.notify(mNotificationId, notificationBuilder.build());

                        //initiating DB update through TripViewModel
                        Intent dbUpdateIntent = new Intent(getString(R.string.set_uploaded_true));
                        dbUpdateIntent.putExtra("tripUploaded", tripUploaded);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(dbUpdateIntent);

                        //in case TLF is not in memory, BroadcastReceiver won't work
                        dbPreferencesEditor.putString("tripUploaded", new Gson().toJson(tripUploaded).toString());
                        dbPreferencesEditor.putString("tripUploadedId", tripUploadedId).commit();
                        tripList.remove(tripUploaded);

                        File fileToBeDeleted = new File(uploadUri.getPath());
                        fileToBeDeleted.delete();

                        //updating RDS
                        Intent apiIntent = new Intent(this, APIService.class);
                        apiIntent.putExtra("request", "POST");
                        apiIntent.putExtra("table", getString(R.string.trip_data_table));
                        apiIntent.putExtra("tripUploaded", tripUploaded);
                        startService(apiIntent);

                        //if batch upload was requested, there will be more files in the list
                        if (tripList.size() > 0) {
                            Intent uploadAllIntent = new Intent(this, S3UploadService.class);
                            uploadAllIntent.putExtra("trip_arrayList", (Serializable) tripList);
                            startService(uploadAllIntent);
                        }

                    } catch (Throwable t) {
                        Log.e(TAG, t.getMessage());
                        notificationBuilder.setContentText("Upload failed");
                        notificationBuilder.setProgress(0, 0, false);
                        notificationBuilder.setOngoing(false);
                        notificationManager.notify(mNotificationId, notificationBuilder.build());
                        stopProgressBar();
                    } finally {
                        if (fileInputStream != null) {
                            try {
                                fileInputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            notifyConnectionTimeout();
        }
    }
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (isInternetAvailable()) {
            Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
            restartServiceIntent.putExtra("upload_uri", uploadUri);
            restartServiceIntent.putExtra("trip_arrayList", (Serializable) tripList);
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
            stopProgressBar();
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
                // Log.e(TAG, "WifiConnectionTest threw Exception");
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