package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.IntentService;
import android.content.Intent;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;

import java.io.File;
import java.util.Calendar;


public class S3MultipleFileUploadService extends IntentService {

    private TransferManager transferManager;
    private String keyName;

    public S3MultipleFileUploadService() {
        super("S3MultipleFileUploadService");
    }

    public S3MultipleFileUploadService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        transferManager = new TransferManager(Util.getsCredProvider(this));
        String userId = getSharedPreferences("uploads", MODE_PRIVATE).getString("FIREBASE_USER_ID", null);
        keyName = userId + Calendar.getInstance().getTime().toString();
        File directory = new File(getApplicationContext().getFilesDir() + "/logs");
        transferManager.uploadDirectory(getString(R.string.s3bucketname), keyName, directory, true);
        Toast.makeText(getApplicationContext(), "Uploading all files", Toast.LENGTH_SHORT).show();
    }
}
