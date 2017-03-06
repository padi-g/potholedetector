package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

/**
 * Created by gautam on 26/01/17.
 */

public class MyAdapter extends ArrayAdapter<File> {

    private final Context context;
    private final File[] values;

    public MyAdapter(Context context, File[] values) {
        super(context, -1, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        String filename = values[position].getName().replaceFirst("AM.csv|PM.csv", "");
        String d = filename.substring(0,3) + " " + filename.substring(3,5) + " " + filename.substring(5,9);
        String t = "ID : "+filename.substring(9);

        View rowView = inflater.inflate(R.layout.trip_list_item, parent, false);
        TextView date = (TextView) rowView.findViewById(R.id.date);
        TextView time = (TextView) rowView.findViewById(R.id.start_time);
        date.setText(d);
        time.setText(t);

        return rowView;
    }

}
