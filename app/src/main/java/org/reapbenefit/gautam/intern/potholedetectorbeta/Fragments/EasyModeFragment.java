package org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MapsActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.UploadTasksService;
import org.reapbenefit.gautam.intern.potholedetectorbeta.R;

import java.util.List;


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

    private StorageReference fileRef = null;
    private Button restartButton;

    ApplicationClass app;

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

        app = ApplicationClass.getInstance();

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
        if(app.isTripInProgress()){
            bgframe.setBackgroundResource(R.drawable.logging_bg);
            statusIndicatorText.setText(getResources().getString(R.string.detecting));
        }else if(app.isTripEnded()){
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
}
