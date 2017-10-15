package org.reapbenefit.gautam.intern.potholedetectorbeta.Fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.reapbenefit.gautam.intern.potholedetectorbeta.R;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Trip;
import org.reapbenefit.gautam.intern.potholedetectorbeta.TripListAdapter;

import java.util.ArrayList;
import java.util.Comparator;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TriplistFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TriplistFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TriplistFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private TripListAdapter adapter;

    ListView l;
    ImageButton refreshButton;

    private ArrayList<Trip> trips = new ArrayList<>();

    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    FirebaseDatabase database = FirebaseDatabase.getInstance();;
    DatabaseReference myRef = database.getReference();


    public TriplistFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TriplistFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TriplistFragment newInstance(String param1, String param2) {
        TriplistFragment fragment = new TriplistFragment();
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

        try {   // thrown when the user is not signed in
            myRef = myRef.child(mAuth.getCurrentUser().getUid());

            myRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    trips = new ArrayList<Trip>();

                    for (DataSnapshot d : dataSnapshot.getChildren()) {
                        trips.add(d.getValue(Trip.class));
                    }
                    createListView();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }catch (NullPointerException e){

        }
    }

    private void createListView(){
        if(!trips.isEmpty()) {
            adapter = new TripListAdapter(getActivity(), trips);
            l.setAdapter(new TripListAdapter(getActivity(), trips));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_triplist, container, false);
        refreshButton = (ImageButton) v.findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createListView();
            }
        });


        l = (ListView) v.findViewById(R.id.trips_list);
        createListView();
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
               /*
                String your_file_path = logfiles[i].getPath();
                Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + your_file_path));
                startActivity(Intent.createChooser(intent, ""));
                */

                // Open a detailed activity with a maps fragment
            }
        });
        return v;
    }

    /*
    public void saveToPrefs(Float h, float l, String car, String model){
        SharedPreferences.Editor editor = getActivity().getSharedPreferences("Profiles", MODE_PRIVATE).edit();
        editor.clear();
        editor.putString("CurrentCar", car);
        editor.putString("CurrentModel", model);
        editor.putFloat("High", h);
        editor.putFloat("Low", l);
        editor.commit();
    }
    */

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

    class CustomComparator implements Comparator<Trip> {
        @Override
        public int compare(Trip o1, Trip o2) {
            return 0;
        }
    }
}
