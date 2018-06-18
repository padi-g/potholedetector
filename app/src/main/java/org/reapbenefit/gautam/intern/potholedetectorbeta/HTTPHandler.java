package org.reapbenefit.gautam.intern.potholedetectorbeta;

import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.amazonaws.http.HttpHeader.USER_AGENT;

public class HTTPHandler {
    private static final String userDataUrl = "https://990rl1xx1d.execute-api.ap-south-1.amazonaws.com/Beta/users/";
    private static final String tripsDataUrl = "https://990rl1xx1d.execute-api.ap-south-1.amazonaws.com/Beta/trips/";
    private static final String TAG = "HTTPHandler";

    public static void insertUser(String userID, boolean updateFlag) {
        try {
            URL urlObject = new URL(userDataUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlObject.openConnection();
            httpURLConnection.setRequestProperty("User-Agent", USER_AGENT);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            UserData userData = new UserData();
            userData.setUserID(userID);
            userData.setUpdateFlag(updateFlag);
            String jsonInput = new Gson().toJson(userData);
            // Log.d(TAG, "Sending user data: " + jsonInput);
            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(jsonInput.getBytes());
            outputStream.flush();
            outputStream.close();
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == 200) { //success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        httpURLConnection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                // print result
                // Log.d(TAG, response.toString());
            } else
                System.out.println("POST request failed " + responseCode);
        } catch (IOException ioException) {
            // Log.e(TAG, ioException.getMessage());
        }
    }

    public static void insertUser(UserData userData) {
        try {
            URL urlObject = new URL(userDataUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlObject.openConnection();
            httpURLConnection.setRequestProperty("User-Agent", USER_AGENT);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            String jsonInput = new Gson().toJson(userData);
            // Log.d(TAG, "Sending user data: " + jsonInput);
            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(jsonInput.getBytes());
            outputStream.flush();
            outputStream.close();
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == 200) { //success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        httpURLConnection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                // print result
                // Log.d(TAG, response.toString());
            } else
                System.out.println("POST request failed " + responseCode);
        } catch (IOException ioException) {
            // Log.e(TAG, ioException.getMessage());
        }
    }

    public static String getAllUsers() {
        try {
            URL urlObject = new URL(userDataUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlObject.openConnection();
            httpURLConnection.setRequestProperty("User-Agent", USER_AGENT);
            httpURLConnection.setRequestMethod("GET");
            InputStream inputStream = httpURLConnection.getInputStream();
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == 200) { //success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        httpURLConnection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                // Log.d(TAG, response.toString());
                return response.toString();
            } else
                System.out.println("POST request failed " + responseCode);
        } catch (IOException ioException) {
            // Log.e(TAG, ioException.getMessage());
        }
        return null;
    }

    public static String getAllTrips() {
        try {
            URL urlObject = new URL(tripsDataUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlObject.openConnection();
            httpURLConnection.setRequestProperty("User-Agent", USER_AGENT);
            httpURLConnection.setRequestMethod("GET");
            InputStream inputStream = httpURLConnection.getInputStream();
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == 200) { //success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        httpURLConnection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                // Log.d(TAG, response.toString());
                return response.toString();
            } else
                System.out.println("POST request failed " + responseCode);
        } catch (IOException ioException) {
            // Log.e(TAG, ioException.getMessage());
        }
        return null;
    }

    public static void insertTrip(Trip newTrip) {
        try {
            URL urlObject = new URL(tripsDataUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlObject.openConnection();
            httpURLConnection.setRequestProperty("User-Agent", USER_AGENT);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            TripDataLambda tripDataLambda = convertToTripDataLambda(newTrip);
            String jsonInput = new Gson().toJson(tripDataLambda);
            // Log.d(TAG, "Sending data: " + jsonInput);
            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(jsonInput.getBytes());
            outputStream.flush();
            outputStream.close();
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == 200) { //success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        httpURLConnection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                // print result
                // Log.d(TAG, response.toString());
            } else
                System.out.println("POST request failed " + responseCode);
        } catch (IOException ioException) {
            // Log.e(TAG, ioException.getMessage());
        }
    }

    public static TripDataLambda convertToTripDataLambda(Trip newTrip) {
        TripDataLambda tripDataLambda = new TripDataLambda();
        tripDataLambda.setTripID(newTrip.getTrip_id());
        tripDataLambda.setUserID(newTrip.getUser_id());
        tripDataLambda.setFilesize(newTrip.getFilesize());
        tripDataLambda.setUploaded(newTrip.isUploaded()? 1 : 0);
        tripDataLambda.setStartTime(newTrip.getStartTime());
        tripDataLambda.setEndTime(newTrip.getEndTime());
        tripDataLambda.setStartLat((float) newTrip.getStartLoc().getLatitude());
        tripDataLambda.setStartLong((float) newTrip.getStartLoc().getLongitude());
        tripDataLambda.setEndLat((float) newTrip.getEndLoc().getLatitude());
        tripDataLambda.setEndLong((float) newTrip.getEndLoc().getLongitude());
        tripDataLambda.setNoOfLines(newTrip.getNo_of_lines());
        tripDataLambda.setDistanceInKM(newTrip.getDistanceInKM());
        tripDataLambda.setDuration(newTrip.getDuration());
        tripDataLambda.setDevice(newTrip.getDevice());
        tripDataLambda.setUserRating(newTrip.getUserRating());
        tripDataLambda.setAxis(newTrip.getAxis());
        tripDataLambda.setThreshold(newTrip.getThreshold());
        tripDataLambda.setProbablePotholeCount(newTrip.getProbablePotholeCount());
        tripDataLambda.setDefinitePotholeCount(newTrip.getDefinitePotholeCount());
        tripDataLambda.setMinutesWasted(newTrip.getMinutesWasted());
        tripDataLambda.setMinutesAccuracyLow(newTrip.getMinutesAccuracyLow());
        return tripDataLambda;
    }

    public static Trip convertToTrip(TripDataLambda tripDataLambda) {
        Trip trip = new Trip();
        trip.setTrip_id(tripDataLambda.getTripID());
        trip.setTrip_id(tripDataLambda.getUserID());
        trip.setUploaded(tripDataLambda.getUploaded() == 1? true : false);
        trip.setStartTime(String.valueOf(tripDataLambda.getStartTime()));
        trip.setEndTime(String.valueOf(tripDataLambda.getEndTime()));
        trip.setStartLoc(new MyLocation(tripDataLambda.getStartLat(), tripDataLambda.getStartLong(), tripDataLambda.getStartAcc()));
        trip.setFilesize(tripDataLambda.getFilesize());
        trip.setEndLoc(new MyLocation(tripDataLambda.getEndLat(), tripDataLambda.getEndLong(), tripDataLambda.getEndAcc()));
        trip.setNo_of_lines(tripDataLambda.getNoOfLines());
        trip.setDistanceInKM(tripDataLambda.getDistanceInKM());
        trip.setDuration(tripDataLambda.getDuration());
        trip.setDevice(tripDataLambda.getDevice());
        trip.setUserRating(tripDataLambda.getUserRating());
        trip.setAxis(tripDataLambda.getAxis());
        trip.setThreshold(tripDataLambda.getThreshold());
        trip.setProbablePotholeCount(tripDataLambda.getProbablePotholeCount());
        trip.setDefinitePotholeCount(tripDataLambda.getDefinitePotholeCount());
        trip.setMinutesWasted(tripDataLambda.getMinutesWasted());
        trip.setMinutesAccuracyLow(tripDataLambda.getMinutesAccuracyLow());
        return trip;
    }

    public static void updateUploadedStatus(TripDataLambda tripDataLambda) {
        try {
            URL urlObject = new URL(tripsDataUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlObject.openConnection();
            httpURLConnection.setRequestProperty("User-Agent", USER_AGENT);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            String jsonInput = new Gson().toJson(tripDataLambda);
            // Log.d(TAG, "Sending data: " + jsonInput);
            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(jsonInput.getBytes());
            outputStream.flush();
            outputStream.close();
            int responseCode = httpURLConnection.getResponseCode();
            if (responseCode == 200) { //success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        httpURLConnection.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                // print result
                // Log.d(TAG, response.toString());
            } else
                System.out.println("POST request failed " + responseCode);
        } catch (IOException ioException) {
            // Log.e(TAG, ioException.getMessage());
        }
    }
}
