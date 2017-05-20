package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if(values != null) {
            String filename = values[position].getName().replaceFirst("AM.csv|PM.csv", "");
            String d = filename.substring(0, 2) + " " + filename.substring(3, 6) + " " + filename.substring(7, 11);
            String t = "ID : " + filename.substring(9);

            View rowView = inflater.inflate(R.layout.trip_list_item, parent, false);
            TextView date = (TextView) rowView.findViewById(R.id.date);
            TextView time = (TextView) rowView.findViewById(R.id.start_time);
            TextView size = (TextView) rowView.findViewById(R.id.size);
            date.setText(d);
            time.setText(t);
            size.setText(Long.toString(values[position].length() / 1000) + " kB");

            ImageButton shareButton = (ImageButton) rowView.findViewById(R.id.imageButton);
            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // file = values[position]
                    String your_file_path = values[position].getPath();
                    Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + your_file_path));
                    view.getContext().startActivity(Intent.createChooser(intent, ""));
                }
            });

            return rowView;
        }
        return null; // if no files present
    }

}
