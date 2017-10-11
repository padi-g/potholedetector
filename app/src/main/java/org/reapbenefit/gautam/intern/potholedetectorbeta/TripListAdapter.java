package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by gautam on 26/01/17.
 */

public class TripListAdapter extends ArrayAdapter<Trip> {

    private final Context context;

    private static ArrayList<Trip> trips;

    public TripListAdapter(Context context, ArrayList<Trip> values) {
        super(context, -1, values);
        this.context = context;
        trips = new ArrayList<>(values);


    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.trip_list_item, parent, false);

        if(!trips.isEmpty()){
            Trip trip = trips.get(position);
            String d = "Trip " + String.valueOf(position+1) + " : " + String.valueOf(trip.getDuration()) + " mins";
            String t = trip.getStartTime();
            t = t.substring(0, t.indexOf("GMT")-4);


            rowView = inflater.inflate(R.layout.trip_list_item, parent, false);
            TextView date = (TextView) rowView.findViewById(R.id.date);
            TextView time = (TextView) rowView.findViewById(R.id.start_time);
            TextView size = (TextView) rowView.findViewById(R.id.size);
            date.setText(d);
            time.setText(t);
            size.setText(humanReadableByteCount(trip.getFilesize(), true));

        }

        return rowView;

        //return null; // if no files present
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

}
