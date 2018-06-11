package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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

import java.io.File;
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

    public S3UploadService() {
        super("S3UploadService");
    }

    public S3UploadService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
            Log.d(getClass().getSimpleName(), "insideOnHandleIntent");
            sharedPreferences = getSharedPreferences("uploads", MODE_PRIVATE);
            userId = sharedPreferences.getString("FIREBASE_USER_ID", null);
            clientRegion = Region.getRegion(Regions.AP_SOUTH_1).getName();
            bucketName = getString(R.string.s3bucketname);
            uploadUri = intent.getParcelableExtra(UPLOAD_URI);
            filepath = uploadUri.toString().substring(uploadUri.toString().lastIndexOf('/'));

            if (BuildConfig.DEBUG)
                keyName = "debug/" + userId + filepath;
            else
                keyName = "logs/" + userId + filepath;

            Log.d(getClass().getSimpleName(), keyName);
            Log.d(getClass().getSimpleName(), filepath);
            Log.d(getClass().getSimpleName(), bucketName);

            initialiseNotification();

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
                    Trip tripUploaded = intent.getParcelableExtra("trip_object");
                    Intent dbUpdateIntent = new Intent(getString(R.string.set_uploaded_true));
                    dbUpdateIntent.putExtra("tripUploaded", tripUploaded);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(dbUpdateIntent);
            }
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
}