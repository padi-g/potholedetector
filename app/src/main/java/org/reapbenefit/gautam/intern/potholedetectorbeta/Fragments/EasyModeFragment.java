package org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MapsActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;
import org.reapbenefit.gautam.intern.potholedetectorbeta.UploadTasksService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static android.content.Context.NOTIFICATION_SERVICE;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link EasyModeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link EasyModeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EasyModeFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    static public boolean tripStatus;
    static public Uri uploadFileUri;
    private View bgframe;
    private TextView statusIndicatorText;
    private StorageReference mStorageRef;

    private StorageReference fileRef = null;
    private Button restartButton;

    private FirebaseAuth mAuth;

    public EasyModeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EasyModeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EasyModeFragment newInstance(String param1, String param2) {
        EasyModeFragment fragment = new EasyModeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

    }

    private final BroadcastReceiver b = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        // How to access the outside variables here?
            tripStatus = intent.getBooleanExtra("LoggingStatus", false);
            if(tripStatus){
                bgframe.setBackgroundResource(R.drawable.logging_bg);
                statusIndicatorText.setText(getResources().getString(R.string.detecting));
            }else {
                uploadFileUri = intent.getParcelableExtra("filename");
                if(uploadFileUri == null){
                    restartButton.setVisibility(View.VISIBLE);
                    statusIndicatorText.setText("Sorry, we could not detect your location accurately");
                }else {
                    Log.d("Upload", "file received is" + String.valueOf(uploadFileUri));
                    statusIndicatorText.setText("Thanks for your contribution! \n\n Come back again");
                    restartButton.setVisibility(View.VISIBLE);
                    //new MyUploadTask().execute(uploadFileUri);
                    startUploadService();
                    openMap();
                }
                bgframe.setBackgroundResource(R.drawable.notlogging_bg);

            }
        }
    };

    public void startUploadService(){
        Intent intent = new Intent(getContext(), UploadTasksService.class);
        intent.setAction("upload_now");
        intent.putExtra("upload_uri", uploadFileUri);
        this.getContext().startService(intent);

    }

    private void openMap(){
        Intent i = new Intent(this.getActivity(), MapsActivity.class);
        startActivity(i);
    }


    // TODO : Just noticed that location may not update when hotspot is on. Check whether this is true even when user is outdoors and not using wifi routers to find location.



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        LocalBroadcastManager l = LocalBroadcastManager.getInstance(getActivity());
        l.registerReceiver(b, new IntentFilter("tripstatus"));

        View v = inflater.inflate(R.layout.fragment_easy_mode, container, false);

        bgframe = (RelativeLayout) v.findViewById(R.id.easyframe);
        statusIndicatorText = (TextView) v.findViewById(R.id.easytext);
        restartButton = (Button) v.findViewById(R.id.restart);
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finish();
                System.exit(0);
            }
        });
        if(ApplicationClass.tripInProgress){
            bgframe.setBackgroundResource(R.drawable.logging_bg);
            statusIndicatorText.setText(getResources().getString(R.string.detecting));
        }else if(ApplicationClass.tripEnded){
            statusIndicatorText.setText("Thanks for your contribution! \n\n Come back again");
            restartButton.setVisibility(View.VISIBLE);
        }

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager l = LocalBroadcastManager.getInstance(getActivity());
        l.unregisterReceiver(b);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If there's an upload in progress, save the reference so you can query it later
        if (fileRef != null) {
            outState.putString("reference", fileRef.toString());
            System.out.println("Uploads : Storing reference");
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if(savedInstanceState == null)
            return;

        // If there was an upload in progress, get its reference and create a new StorageReference
        final String stringRef = savedInstanceState.getString("reference");
        if (stringRef == null) {
            System.out.println("Uploads : no references found");
            return;
        }
        fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(stringRef);

        // Find all UploadTasks under this StorageReference (in this example, there should be one)
        List<UploadTask> tasks = fileRef.getActiveUploadTasks();
        if (tasks.size() > 0) {
            // Get the task monitoring the upload
            UploadTask task = tasks.get(0);

            for (UploadTask t: tasks) {
                System.out.println("Uploads Remaining tasks "+ t.getSnapshot().getUploadSessionUri().toString());
            }

            // Add new listeners to the task using an Activity scope
            task.addOnSuccessListener(getActivity(), new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot state) {
                    System.out.println("Uploads : restored and uploaded");
                    //call a user defined function to handle the event.
                }
            });
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private class MyUploadTask extends AsyncTask<Uri, Integer, Void>{

        NotificationManager mNotifyMgr;
        int mNotificationId = 101;
        NotificationCompat.Builder mBuilder;
        int uploadProgress = 0;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();


             mBuilder = new NotificationCompat.Builder(getContext())
                            .setSmallIcon(R.drawable.ic_upload)
                            .setContentTitle("Road Quality Audit")
                            .setContentText("Data is being Uploaded");

            mNotifyMgr =
                    (NotificationManager) getActivity().getSystemService(NOTIFICATION_SERVICE);
            mNotifyMgr.notify(mNotificationId, mBuilder.build());

        }

        @Override
        protected Void doInBackground(Uri... files) {

            mNotifyMgr.notify(mNotificationId, mBuilder.build());
            uploadFile(files[0]);
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if(values[0] == -1)
                mBuilder.setContentText("Upload paused");
            else if(values[0] == -2)
                mBuilder.setContentText("Upload failed");
            else {
                mBuilder.setProgress(100, values[0], false);
                if (values[0] == 100)
                    mBuilder.setContentText("Upload Complete");
            }
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }

        public void uploadFile(Uri uri){

            String filename = uri.toString().substring(uri.toString().lastIndexOf('/')) ;

            StorageReference fileRef = mStorageRef.child("logs/" + mAuth.getCurrentUser().getUid() + filename);

            UploadTask uploadTask = fileRef.putFile(uri);

            uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                    Log.d("Uploading ", progress + "% done");
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
                    // update to database object uploaded = true

                    }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    publishProgress(-2);  // failed code
                }
            });

        }

    }
}

/// TODO: Fix uploads
