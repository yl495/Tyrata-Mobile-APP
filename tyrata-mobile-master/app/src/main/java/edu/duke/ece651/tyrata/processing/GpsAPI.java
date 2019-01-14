package edu.duke.ece651.tyrata.processing;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * This class has GPS API
 * @author Cheng (cx30)
 * Created by Saeed on 4/13/2018.
 */

public class GpsAPI {

    public static ArrayList<Double> getGPS(Activity activity) {
        ArrayList<Double> ans = new ArrayList<>();
        try {
            // Check for location permission
            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                // Request permission for location
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        123);
            }

            GPStracker g = new GPStracker(activity);
            Location l = g.getLocation();
            if (l != null) {
                Double lat = l.getLatitude();
                Double lon = l.getLongitude();
                ans.add(lat);
                ans.add(lon);
                Toast.makeText(activity, "LAT: " + lat + " \n LON : " + lon, Toast.LENGTH_LONG).show();
            }
        }
        catch(Exception e){
            String msg = "The GPS information cannot be fetched!";
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
        }
        return ans;
    }
}
