package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

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

        String dateString, timeString, sizeString;

        if(!trips.isEmpty()){
            try {
                Trip trip = trips.get(position);
                dateString = "Trip " + String.valueOf(position + 1) + " : " + String.valueOf(trip.getDuration()) + " mins";
                timeString = trip.getStartTime();
                timeString = timeString.substring(0, timeString.indexOf("GMT") - 4);
                sizeString = humanReadableByteCount(trip.getFilesize(), true);
            }catch (NullPointerException e){
                dateString = "null";
                timeString = "null";
                sizeString = "null";
            }

            rowView = inflater.inflate(R.layout.trip_list_item, parent, false);
            TextView date = (TextView) rowView.findViewById(R.id.date);
            TextView time = (TextView) rowView.findViewById(R.id.start_time);
            TextView size = (TextView) rowView.findViewById(R.id.size);
            date.setText(dateString);
            time.setText(timeString);
            size.setText(sizeString);

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
