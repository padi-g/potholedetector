package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments.EasyModeFragment.uploadFileUri;

public class MultipleFileUploadService extends IntentService {

    private List<Trip> offlineTrips = new ArrayList<>();
    private int i = 0;

    public MultipleFileUploadService() {
        super("MultipleFileUploadService");
    }

    public MultipleFileUploadService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        offlineTrips = intent.getParcelableArrayListExtra("offlineTrips");
        i = intent.getIntExtra("i", 0);
        for (i = 0; i < offlineTrips.size(); ++i) {
            Intent uploadIntent = new Intent(this, S3UploadService.class);
            uploadIntent.setAction("upload_now");
            uploadIntent.putExtra("upload_uri", uploadFileUri);
            uploadIntent.putExtra("trip_object", offlineTrips.get(i));
            startService(uploadIntent);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        rootIntent.putParcelableArrayListExtra("offlineTrips", (ArrayList<? extends Parcelable>) offlineTrips);
        rootIntent.putExtra("i", i);
        rootIntent.putExtra("wasTaskRemoved", true);
        super.onTaskRemoved(rootIntent);
    }
}
