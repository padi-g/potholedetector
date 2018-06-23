package org.reapbenefit.gautam.intern.potholedetectorbeta.Core;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

import java.io.File;
import java.util.List;

/**
 * Created by gautam on 13/09/17.
 */

public class UploadTasksService extends IntentService {

    private static final String ACTION_UPLOAD_NOW = "upload_now";
    private static final String UPLOAD_URI = "upload_uri";

    NotificationManager mNotifyMgr;
    int mNotificationId = 101;
    NotificationCompat.Builder mBuilder;
    int uploadProgress = 0;

    private StorageReference mStorageRef;
    private FirebaseAuth mAuth;
    private DatabaseReference db;
    private String filename;
    SharedPreferences prefs;

    //for keeping track of uploads
    private List<TransferObserver> observers;


    public UploadTasksService(){
        super("UploadService");
    }

    public UploadTasksService(String name) {
        super(name);
        setIntentRedelivery(true);  // not sure if this is what is required

    }

    @Override
    protected void onHandleIntent(Intent intent) {

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("uploads" ,MODE_PRIVATE);
        // Log.d("Preferences", "Inside onHandleIntent Found file_delete = " + prefs.getBoolean("file_delete", false));

        mBuilder = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(R.drawable.ic_upload)
                .setContentTitle("Road Quality Audit")
                .setContentText("Data is being Uploaded");

        mNotifyMgr =
                (NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

        String action = intent.getAction();
        if(action.equals(ACTION_UPLOAD_NOW)){
            Uri uploadUri = intent.getParcelableExtra(UPLOAD_URI);
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
            uploadFile(uploadUri);
        }

    }

    public void uploadFile(Uri uri){

        filename = uri.toString().substring(uri.toString().lastIndexOf('/')) ;

        StorageReference fileRef = mStorageRef.child("logs/" + mAuth.getCurrentUser().getUid() + filename);

        UploadTask uploadTask = fileRef.putFile(uri);

        // Log.d("GCPUploads", "happening");

        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                // Log.d("Uploading ", progress + "% done");
                uploadProgress = (int) progress;
                publishProgress(uploadProgress);
            }

        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                System.out.println("Upload is paused");
                publishProgress(-1);  // paused code
            }

        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                publishProgress(100);
                setIsUploaded(true);
                if(prefs.getBoolean("file_delete", false))
                    myDeleteFile();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                publishProgress(-2);  // failed code
            }
        });

    }

    public void publishProgress(int progress){
        if(progress == -1)
            mBuilder.setContentText("Upload paused");
        else if(progress == -2)
            mBuilder.setContentText("Upload failed");
        else {
            mBuilder.setProgress(100, progress, false);
            if (progress == 100)
                mBuilder.setContentText("Upload Complete");
        }
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    private void setIsUploaded(boolean a){
        db = FirebaseDatabase.getInstance().getReference();
        db = db.child(mAuth.getCurrentUser().getUid()).child(filename.replace(".csv", "").replace("/", "")).child("uploaded");
        ApplicationClass.getInstance().getTrip().setUploaded(a);
        db.setValue(a);
    }

    private void myDeleteFile(){
        File file = new File(getApplicationContext().getFilesDir(), "logs/" + filename);
        file.delete();
    }

}
