package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.ApplicationClass;
import org.reapbenefit.gautam.intern.potholedetectorbeta.Core.TripViewModel;

import java.io.File;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class TripListAdapter extends RecyclerView.Adapter<TripListAdapter.TripListViewHolder> {

    private String tripId;
    private Context baseContext;
    private Context context;
    private ArrayList<Trip> trips;
    private boolean uploadStatus;
    private int positionChanged;
    private ApplicationClass app = ApplicationClass.getInstance();
    private Uri uploadFileUri;
    private TripViewModel tripViewModel;
    private final String TAG = getClass().getSimpleName();
    private SharedPreferences dbPreferences;
    private boolean batchUpload;
    private String tripUploadingId;

    String countString, timeString, durationString, distanceString;
    ImageButton uploadButton;
    ImageButton uploadedTick;
    TextView date, time, size, distance;
    ProgressBar uploadProgressBar;


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
        public ProgressBar uploadProgressBar;
        public TripListViewHolder(View itemView) {
            super(itemView);
            uploadButton = (ImageButton) itemView.findViewById(R.id.upload_button);
            uploadedTick = (ImageButton) itemView.findViewById(R.id.upload_tick);
            date = (TextView) itemView.findViewById(R.id.count);
            time = (TextView) itemView.findViewById(R.id.start_time);
            size = (TextView) itemView.findViewById(R.id.size);
            distance = (TextView) itemView.findViewById(R.id.distance_view);
            uploadProgressBar = (ProgressBar) itemView.findViewById(R.id.upload_progress_bar);
        }
    }

    public TripListAdapter(Context context, ArrayList<Trip> trips, boolean uploadStatus, String tripId, String tripUploadingId, TripViewModel tripViewModel, Context baseContext) {
        // Log.d("Constructor", "Hello");
        this.context = context;
        this.trips = trips;
        this.uploadStatus = uploadStatus;
        this.tripId = tripId;
        this.tripViewModel = tripViewModel;
        this.baseContext = baseContext;
        this.tripUploadingId = tripUploadingId;
    }

    @Override
    public TripListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        dbPreferences = PreferenceManager.getDefaultSharedPreferences(ApplicationClass.getInstance());

        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = layoutInflater.inflate(R.layout.trip_list_item, parent, false);
        String countString, timeString, durationString, distanceString;
        ImageButton uploadButton, uploadedTick, mapButton;
        ProgressBar uploadProgressBar;
        TextView date, time, size, distance;
        TripListViewHolder rowViewHolder = null;
        try {
            if (!trips.isEmpty()) {
                uploadButton = rowView.findViewById(R.id.upload_button);
                uploadedTick = rowView.findViewById(R.id.upload_tick);
                date = (TextView) rowView.findViewById(R.id.count);
                time = (TextView) rowView.findViewById(R.id.start_time);
                size = (TextView) rowView.findViewById(R.id.size);
                distance = (TextView) rowView.findViewById(R.id.distance_view);
                uploadProgressBar = (ProgressBar) rowView.findViewById(R.id.upload_progress_bar);
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
    public void onBindViewHolder(final TripListViewHolder holder, final int position) {
        holder.setIsRecyclable(false);
        //this method is called for every item in the list
        // Log.i(getClass().getSimpleName(), "inside onBindViewHolder");
        // Log.d(TAG, "position = " + position);
        if (holder != null) {
            // Log.i(getClass().getSimpleName(), "holder is not null");
            View rowView = holder.itemView;
            final Trip trip = trips.get(position);
            countString = String.valueOf(trip.getDefinitePotholeCount() + trip.getProbablePotholeCount()) + " potholes";
            // Log.i("countString", countString);
            // Log.i("timeString", trip.getStartTime() + "");
            timeString = trip.getStartTime();
            timeString = timeString.substring(4, timeString.indexOf("GMT") - 4);
            durationString = String.valueOf(trip.getDuration()) + " mins, " + humanReadableByteCount(trip.getFilesize(), true);
            distanceString = String.valueOf(roundTwoDecimals(trip.getDistanceInKM())) + "km";
            uploadButton = holder.uploadButton;
            uploadedTick = holder.uploadedTick;
            uploadProgressBar = holder.uploadProgressBar;
            batchUpload = dbPreferences.getBoolean("batchUpload", false);

            if ((uploadStatus && trip.getTrip_id().equals(tripId)) || batchUpload) {
                uploadProgressBar.setIndeterminate(true);
                uploadProgressBar.setVisibility(View.VISIBLE);
                uploadButton.setVisibility(View.GONE);
            }

            Log.d("TripListAdapter1", trip.getTrip_id());
            Log.d("TripListAdapter2", tripUploadingId + "");

            if (tripUploadingId != null) {
                if ((uploadStatus && trip.getTrip_id().equals(tripUploadingId)) || batchUpload) {
                    uploadProgressBar.setIndeterminate(true);
                    uploadProgressBar.setVisibility(View.VISIBLE);
                    uploadButton.setVisibility(View.GONE);
                }
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
                        startUploadService(json, trip);
                        holder.uploadProgressBar.setIndeterminate(true);
                        holder.uploadProgressBar.setVisibility(View.VISIBLE);
                        holder.uploadButton.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(context.getApplicationContext(), "Internet not available. Try again later", Toast.LENGTH_LONG).show();
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

    private void startUploadService(String json, Trip trip) {
        Intent intent = new Intent(context.getApplicationContext(), S3UploadService.class);
        intent.setAction("upload_now");
        intent.putExtra("upload_uri", uploadFileUri);
        List<Trip> tripList = new ArrayList<>();
        tripList.add(trip);
        intent.putExtra("trip_arrayList", (Serializable) tripList);
        context.getApplicationContext().startService(intent);
    }

    public static float roundTwoDecimals(float f) {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        return Float.valueOf(twoDForm.format(f));
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
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