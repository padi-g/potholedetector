package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


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
    View bgframe;
    TextView statusIndicatorText;
    private StorageReference mStorageRef;
    private Button restartButton;

    ProgressBar progressBar;

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
            uploadFileUri = intent.getParcelableExtra("filename");
            Log.d("Upload", "file received is"+String.valueOf(uploadFileUri));
            if(tripStatus){
                bgframe.setBackgroundResource(R.drawable.logging_bg);
                statusIndicatorText.setText(getResources().getString(R.string.detecting));
            }else {
                bgframe.setBackgroundResource(R.drawable.notlogging_bg);
                statusIndicatorText.setText(getResources().getString(R.string.not_detecting));
                progressBar.setVisibility(View.VISIBLE);
                statusIndicatorText.setText(getResources().getString(R.string.uploading));
                uploadFile(uploadFileUri);

            }


        }
    };

    public void uploadFile(Uri uri){

        String filename = uri.toString().substring(uri.toString().lastIndexOf('/')) ;

        StorageReference sRef = mStorageRef.child("logs/" + mAuth.getCurrentUser().getUid() + filename);

        UploadTask uploadTask = sRef.putFile(uri);

        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                System.out.println("Upload is " + progress + "% done");
                int currentprogress = (int) progress;
                progressBar.setProgress(currentprogress);
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                                   @Override
                                   public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                                       System.out.println("Upload is paused");
                                   }
                               }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                statusIndicatorText.setText("Uploaded");
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                Log.d("Upload","Download file from "+downloadUrl.toString());
                Toast.makeText(getActivity(), "Please restart the app to start a new trip", Toast.LENGTH_LONG).show();
                restartButton.setVisibility(View.VISIBLE);
                restartButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getActivity().finish();
                        System.exit(0);
                    }
                });
            }
        });


    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        LocalBroadcastManager l = LocalBroadcastManager.getInstance(getActivity());
        l.registerReceiver(b, new IntentFilter("tripstatus"));

        View v = inflater.inflate(R.layout.fragment_easy_mode, container, false);

        bgframe = (RelativeLayout) v.findViewById(R.id.easyframe);
        statusIndicatorText = (TextView) v.findViewById(R.id.easytext);
        progressBar = (ProgressBar) v.findViewById(R.id.upload_progressbar);
        restartButton = (Button) v.findViewById(R.id.restart);
        if(ApplicationClass.tripInProgress){
            bgframe.setBackgroundResource(R.drawable.logging_bg);
            statusIndicatorText.setText(getResources().getString(R.string.detecting));
        }

        progressBar.setVisibility(View.GONE);
        progressBar.setMax(100);
        progressBar.setProgress(6);

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
}
