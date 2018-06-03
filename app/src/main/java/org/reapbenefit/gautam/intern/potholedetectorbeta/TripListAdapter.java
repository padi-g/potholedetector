package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.Network;
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
import org.reapbenefit.gautam.intern.potholedetectorbeta.LocalDatabase.LocalTripEntity;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class TripListAdapter extends RecyclerView.Adapter<TripListAdapter.TripListViewHolder> {

    private Context context;
    private ArrayList<LocalTripEntity> trips;
    private boolean uploadStatus;
    private int positionChanged;
    private ApplicationClass app = ApplicationClass.getInstance();
    private Uri uploadFileUri;
    private TripViewModel tripViewModel;

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

    public TripListAdapter(Context context, ArrayList<LocalTripEntity> trips, boolean uploadStatus, int positionChanged) {
        this.context = context;
        this.trips = trips;
        this.uploadStatus = uploadStatus;
        this.positionChanged = positionChanged;
        Log.i(getClass().getSimpleName(), "Adapter created");
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
                Toast.makeText(context.getApplicationContext(), "No trips made yet", Toast.LENGTH_SHORT);
            }
        }
        catch (NullPointerException nullPointerException) {
            Toast.makeText(context.getApplicationContext(), "No trips made yet", Toast.LENGTH_SHORT);
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
    public void onBindViewHolder(TripListViewHolder holder, int position) {
        Log.i(getClass().getSimpleName(), "inside onBindViewHolder");
        if (holder != null) {
            Log.i(getClass().getSimpleName(), "holder is not null");
            View rowView = holder.itemView;
            final LocalTripEntity trip = trips.get(position);
            tripViewModel = new TripViewModel(app);
            if (tripViewModel == null)
                Log.e(getClass().getSimpleName(), "TripViewModel is null");
            String countString, timeString, durationString, distanceString;
            ImageButton uploadButton, uploadedTick, mapButton;
            TextView date, time, size, distance;
            countString = String.valueOf(tripViewModel.getPotholeCount()) + " potholes";
            Log.i("countString", countString);
            timeString = tripViewModel.getStartTime();
            timeString = timeString.substring(4, timeString.indexOf("GMT") - 4);
            durationString = String.valueOf(tripViewModel.getDuration()) + " mins, "  +  humanReadableByteCount(tripViewModel.getFilesize(), true);
            distanceString = String.valueOf(roundTwoDecimals(tripViewModel.getDistanceInKm())) + "km";
            uploadButton = holder.uploadButton;
            uploadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(internetAvailable()) {
                        uploadFileUri = Uri.fromFile(new File(context.getApplicationContext().getFilesDir() + "/logs/" + tripViewModel.getTrip_id() + ".csv"));
                        //passing object to service as JSON
                        Gson gson = new Gson();
                        String json = gson.toJson(tripViewModel);
                        startUploadService(json);
                    }else {
                        Toast.makeText(context.getApplicationContext(), "Internet not available. Try again later", Toast.LENGTH_LONG).show();
                    }
                }
            });
            mapButton = holder.mapButton;
            mapButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    File datafile = new File(context.getApplicationContext().getFilesDir(), "logs/" + tripViewModel.getTrip_id() + ".csv");
                    File file = new File(context.getApplicationContext().getFilesDir(), "analysis/" + tripViewModel.getTrip_id() + ".csv");
                    if(datafile.exists()) {
                        if (!app.isTripInProgress() && file.exists()) { // check if file of same name is available in the analytics folder
                            Intent i = new Intent(context, MapsActivity.class);
                            i.putExtra("trip", Trip.localTripEntityToTrip(trip));
                            context.startActivity(i);
                        }
                    }
                    else {
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
        else
            Log.i(getClass().getSimpleName(), "holder is null");
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