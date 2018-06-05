package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Activities.MapsActivity;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.TripViewModel;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class TripListAdapter extends RecyclerView.Adapter<TripListAdapter.TripListViewHolder> {

    private Context context;
    private ArrayList<Trip> trips;
    private boolean uploadStatus;
    private int positionChanged;
    private ApplicationClass app = ApplicationClass.getInstance();
    private Uri uploadFileUri;
    private TripViewModel tripViewModel;
    private final String TAG = getClass().getSimpleName();
    private Set<String> positionChangedSet;

    /*
    static ViewHolder class to reference views for each trip component item
     */
    public static class TripListViewHolder extends RecyclerView.ViewHolder {
        public ImageButton uploadButton;
        public ImageButton uploadedTick;
        public ImageButton mapButton;
        public TextView date;
        public TextView time;
        public TextView size;
        public TextView distance;
        public TripListViewHolder(View itemView) {
            super(itemView);
            uploadButton = (ImageButton) itemView.findViewById(R.id.upload_button);
            uploadedTick = (ImageButton) itemView.findViewById(R.id.upload_tick);
            mapButton = (ImageButton) itemView.findViewById(R.id.map_button);
            date = (TextView) itemView.findViewById(R.id.count);
            time = (TextView) itemView.findViewById(R.id.start_time);
            size = (TextView) itemView.findViewById(R.id.size);
            distance = (TextView) itemView.findViewById(R.id.distance_view);
        }
    }

    public TripListAdapter(Context context, ArrayList<Trip> trips, boolean uploadStatus, int positionChanged, TripViewModel tripViewModel) {
        this.context = context;
        this.trips = trips;
        this.uploadStatus = uploadStatus;
        this.positionChanged = positionChanged;
        SharedPreferences adapterPreferences = context.getSharedPreferences("adapterPreferences", Context.MODE_PRIVATE);
        this.positionChangedSet = adapterPreferences.getStringSet("positionChangedSet", null);
        if (positionChangedSet == null)
            positionChangedSet = new HashSet<>();
        positionChangedSet.add(String.valueOf(positionChanged));
        adapterPreferences.edit().putStringSet("positionChangedSet", positionChangedSet).apply();
        this.tripViewModel = tripViewModel;
        Log.d(TAG, "positionChanged = " + positionChanged);
    }

    @Override
    public TripListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = layoutInflater.inflate(R.layout.trip_list_item, parent, false);
        String countString, timeString, durationString, distanceString;
        ImageButton uploadButton, uploadedTick, mapButton;
        TextView date, time, size, distance;
        TripListViewHolder rowViewHolder = null;
        try {
            if (!trips.isEmpty()) {
                uploadButton = rowView.findViewById(R.id.upload_button);
                uploadedTick = rowView.findViewById(R.id.upload_tick);
                mapButton = rowView.findViewById(R.id.map_button);
                date = (TextView) rowView.findViewById(R.id.count);
                time = (TextView) rowView.findViewById(R.id.start_time);
                size = (TextView) rowView.findViewById(R.id.size);
                distance = (TextView) rowView.findViewById(R.id.distance_view);
                rowViewHolder = new TripListViewHolder(rowView);
            }
            else {
                Toast.makeText(context.getApplicationContext(), "No trips made yet", Toast.LENGTH_SHORT).show();
            }
        }
        catch (NullPointerException nullPointerException) {
            Toast.makeText(context.getApplicationContext(), "No trips made yet", Toast.LENGTH_SHORT).show();
        }
        return rowViewHolder;
    }

    private boolean internetAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(
                Context.CONNECTIVITY_SERVICE
        );
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean isWifiConnected= networkInfo.isConnected();
        networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        boolean isMobileConnected = networkInfo.isConnected();
        if (isWifiConnected || isMobileConnected) {
            return true;
        }
        else
            return false;
    }


    @Override
    public void onViewRecycled(TripListViewHolder holder) {
        super.onViewRecycled(holder);

    }

    @Override
    public void onBindViewHolder(TripListViewHolder holder, final int position) {
        //this method is called for every item in the list
        holder.setIsRecyclable(false);
        try {
            Log.i(getClass().getSimpleName(), "inside onBindViewHolder");
            Log.d(TAG, "position = " + position);
            if (holder != null) {
                Log.i(getClass().getSimpleName(), "holder is not null");
                View rowView = holder.itemView;
                final Trip trip = trips.get(position);

                if (tripViewModel == null)
                    Log.e(getClass().getSimpleName(), "TripViewModel is null");

                String countString, timeString, durationString, distanceString;
                ImageButton uploadButton, uploadedTick, mapButton;
                TextView date, time, size, distance;
                countString = String.valueOf(trip.getPotholeCount()) + " potholes";
                Log.i("countString", countString);
                Log.i("timeString", trip.getStartTime() + "");
                timeString = trip.getStartTime();
                timeString = timeString.substring(4, timeString.indexOf("GMT") - 4);
                durationString = String.valueOf(trip.getDuration()) + " mins, " + humanReadableByteCount(trip.getFilesize(), true);
                distanceString = String.valueOf(roundTwoDecimals(trip.getDistanceInKM())) + "km";
                uploadButton = holder.uploadButton;
                uploadedTick = holder.uploadedTick;

                //checking if item has been uploaded
                //if (trip.isUploaded() && positionChangedSet!= null && positionChangedSet.contains(String.valueOf(position))) {
                 if (trip.isUploaded()) {
                    uploadButton.setVisibility(View.GONE);
                    uploadedTick.setVisibility(View.VISIBLE);
                }

                uploadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (internetAvailable()) {
                            uploadFileUri = Uri.fromFile(new File(context.getApplicationContext().getFilesDir() + "/logs/" + trip.getTrip_id() + ".csv"));
                            //passing object to service as JSON
                            Gson gson = new Gson();
                            String json = gson.toJson(trip);
                            positionChanged = position;
                            startUploadService(json);
                        } else {
                            Toast.makeText(context.getApplicationContext(), "Internet not available. Try again later", Toast.LENGTH_LONG).show();
                        }
                    }
                });
                mapButton = holder.mapButton;
                mapButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        File datafile = new File(context.getApplicationContext().getFilesDir(), "logs/" + trip.getTrip_id() + ".csv");
                        File file = new File(context.getApplicationContext().getFilesDir(), "analysis/" + trip.getTrip_id() + ".csv");
                        if (datafile.exists()) {
                            if (!app.isTripInProgress() && file.exists()) { // check if file of same name is available in the analytics folder
                                Intent i = new Intent(context, MapsActivity.class);
                                i.putExtra("trip", trip);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(i);
                            }
                        } else {
                            Toast.makeText(context, "Sorry, file has been deleted", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                date = holder.date;
                time = holder.time;
                size = holder.size;
                distance = holder.distance;
                date.setText(countString);
                time.setText(timeString);
                size.setText(durationString);
                distance.setText(distanceString);
            }
        }
        catch (NullPointerException nullPointer) {
            Log.i(getClass().getSimpleName(), "NullPointerException caught");
        }
    }

    private void startUploadService(String json) {
        Intent intent = new Intent(context.getApplicationContext(), S3UploadSevice.class);
        intent.setAction("upload_now");
        intent.putExtra("upload_uri", uploadFileUri);
        intent.putExtra("trip_json", json);
        intent.putExtra("trips_arraylist", trips);
        intent.putExtra("position", positionChanged);
        this.context.getApplicationContext().startService(intent);
    }

    private float roundTwoDecimals(float f) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Float.valueOf(twoDForm.format(f));
    }

    private String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f%sB", bytes / Math.pow(unit, exp), pre);
    }

    @Override
    public int getItemCount() {
        if (trips == null)
            return 0;
        else
            return trips.size();
    }
}