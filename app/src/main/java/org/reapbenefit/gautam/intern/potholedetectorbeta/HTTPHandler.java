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
    private static final String TAG = "HTTPHandler";
    public static void insertUser(String userID) {
        try {
            URL urlObject = new URL(userDataUrl);
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlObject.openConnection();
            httpURLConnection.setRequestProperty("User-Agent", USER_AGENT);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoOutput(true);
            UserData userData = new UserData();
            userData.setUserID(userID);
            String jsonInput = new Gson().toJson(userData);
            System.out.println("Sending data: " + jsonInput);
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
                Log.d(TAG, response.toString());
            } else
                System.out.println("POST request failed " + responseCode);
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getMessage());
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
                Log.d(TAG, response.toString());
                return response.toString();
            } else
                System.out.println("POST request failed " + responseCode);
        } catch (IOException ioException) {
            Log.e(TAG, ioException.getMessage());
        }
        return null;
    }
}
