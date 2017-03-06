package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AdvancedModeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AdvancedModeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AdvancedModeFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    File dir;
    File[] logfiles;
    ListView l;
    Button submitButton;

    String CarName, ModelName;
    TextView CarNameTextView, ModelNameTextView;

    TextView curHigh, curLow;
    double bumpHighThreshold = 12.5, bumpLowThreshold = 7.5;

    EditText input_high_threshold, input_low_threshold ;

    public AdvancedModeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AdvancedModeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AdvancedModeFragment newInstance(String param1, String param2) {
        AdvancedModeFragment fragment = new AdvancedModeFragment();
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

        dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/PotholeApp/");
        logfiles = dir.listFiles();
        int n = logfiles.length;
        for(int i=0; i<n; i++){
            Log.i("File "+String.valueOf(i), logfiles[i].getName());

        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_advanced_mode, container, false);
        submitButton = (Button) v.findViewById(R.id.submit);

        CarNameTextView = (TextView) v.findViewById(R.id.car_text);
        ModelNameTextView = (TextView) v.findViewById(R.id.model_text);

        curHigh = (TextView) v.findViewById(R.id.curHighText);
        curLow = (TextView) v.findViewById(R.id.curLowText);

        input_high_threshold = (EditText) v.findViewById(R.id.inputHighThreshold);
        input_low_threshold = (EditText) v.findViewById(R.id.inputLowThreshold);


        SharedPreferences sharedPref = getActivity().getSharedPreferences("Profiles", MODE_PRIVATE);
        CarName = sharedPref.getString("CurrentCar", "Please add a car");
        CarNameTextView.setText(CarName);
        ModelName = sharedPref.getString("CurrentModel", "None");
        ModelNameTextView.setText(ModelName);
        bumpHighThreshold = sharedPref.getFloat("High", 12.5f);
        bumpLowThreshold = sharedPref.getFloat("Low", 7.5f);

        curHigh.setText(Double.toString(bumpHighThreshold));
        curLow.setText(Double.toString(bumpLowThreshold));


        input_high_threshold = (EditText) v.findViewById(R.id.inputHighThreshold);
        input_low_threshold = (EditText) v.findViewById(R.id.inputLowThreshold);
        curHigh = (TextView) v.findViewById(R.id.curHighText);
        curLow = (TextView) v.findViewById(R.id.curLowText);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String h = input_high_threshold.getText().toString();
                if(h.isEmpty()) {
                    // do nothing
                }else bumpHighThreshold = Float.parseFloat(h);

                String l = input_low_threshold.getText().toString();
                if(l.isEmpty()) {
                    // do nothing
                }else bumpLowThreshold = Float.parseFloat(l);

                curHigh.setText(String.valueOf(bumpHighThreshold));
                curLow.setText(String.valueOf(bumpLowThreshold));

                buildDialog();

                saveToPrefs((float)bumpHighThreshold, (float)bumpLowThreshold, CarName, ModelName);

                input_high_threshold.setText("");
                input_low_threshold.setText("");
            }
        });


        l = (ListView) v.findViewById(R.id.trips_list);
        l.setAdapter(new MyAdapter(getActivity(), logfiles));
        l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                /*   *********  Opens a map activity, DUMMY
                Intent intent = new Intent(getActivity(), MapsActivity.class);
                Bundle b = new Bundle();
                b.putSerializable("file", logfiles[i]);
                intent.putExtra("file", logfiles[i]);
                startActivity(intent);
                */
                // Start a map activity that plots the locations

                // Upload the related file to google drive
                String your_file_path = logfiles[i].getPath();
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + your_file_path));
                startActivity(Intent.createChooser(intent, ""));

            }
        });
        return v;
    }

    void buildDialog(){

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialoglayout = inflater.inflate(R.layout.profile_dialog_layout, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        final EditText carname = (EditText) dialoglayout.findViewById(R.id.car_name);
        final EditText modelname = (EditText) dialoglayout.findViewById(R.id.model_name);

        carname.setHint(CarName);
        modelname.setHint(ModelName);

        builder.setTitle("Car Profile");
        builder.setPositiveButton("Save",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String c = carname.getText().toString();
                        if(!c.isEmpty())
                            CarName = c;

                        String m = modelname.getText().toString();
                        if(!m.isEmpty())
                            ModelName = m;

                        CarNameTextView.setText(CarName);
                        ModelNameTextView.setText(ModelName);
                        saveToPrefs((float)bumpHighThreshold, (float)bumpLowThreshold, CarName, ModelName);
                    }
                });
        builder.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        builder.setView(dialoglayout);
        builder.show();
    }

    public void saveToPrefs(Float h, float l, String car, String model){
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("Profiles", MODE_PRIVATE).edit();
        editor.clear();
        editor.putString("CurrentCar", car);
        editor.putString("CurrentModel", model);
        editor.putFloat("High", h);
        editor.putFloat("Low", l);
        editor.commit();
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
