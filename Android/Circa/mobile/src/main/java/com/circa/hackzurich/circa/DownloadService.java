package com.circa.hackzurich.circa;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public class DownloadService extends Service {
    public DownloadService() {
    }

    Handler handler;

    @Override
    public void onCreate() {
        // Handler will get associated with the current thread,
        // which is the main thread.
        handler = new Handler();
        super.onCreate();
    }

    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Circa", "download_service_on");
        GPSTracker gps = new GPSTracker(DownloadService.this);


        // check if GPS enabled
        if (gps.canGetLocation()) {
            final double latitude = gps.getLatitude();
            final double longitude = gps.getLongitude();
            //Log.d("Circa", "lat: " + latitude);
            //Log.d("Circa", "long: " + longitude);

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    HttpClient client = new DefaultHttpClient();
                    try {
                        String url = DescConstants.SERVER_NAME +
                                        + latitude + "/" + longitude + "/";
                        String gsonResult = "";

                        // Create Request to server and get response
                        HttpGet httpget = new HttpGet(url);
                        ResponseHandler<String> responseHandler = new BasicResponseHandler();
                        gsonResult = client.execute(httpget, responseHandler);
//                        Log.d("Circa", "Result: " + gsonResult);
                        Gson gson = new Gson();

                        LocalDB db = new LocalDB(getApplicationContext());
                        db.removeAllPlaces();
                        ArrayList<Place> places = new ArrayList<Place>(Arrays.asList(gson.fromJson(gsonResult, Place[].class)));
                        for (Place place : places) {
                            db.addPlace(place);
                        }
                        db.close();
                        //Log.d("Circa", "Result123: " + gsonResult);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();

        } else {
            Log.e("Circa", "GPS or Network is not enabled");
        }
        return Service.START_NOT_STICKY;
    }
}
