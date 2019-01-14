package edu.duke.ece651.tyrata;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.UUID;

/**
 * Class containing all common constants
 * @author Saeed Alrahma
 * Created by Saeed on 2/25/2018.
 */

public class Common {
    /* CONSTANTS */

    // Defines constants for logger keywords/tags
    public static final String LOG_TAG_BT_API = "BluetoothAPI";
    public static final String LOG_TAG_BT_ACTIVITY = "BluetoothActivity";
    public static final String LOG_TAG_BT_DEVICE_LIST_ACTIVITY = "BtDeviceListActivity";
    public static final String LOG_TAG_MAIN_ACTIVITY = "MainActivity";
    public static final String LOG_TAG_COMMON = "Common";
    public static final String LOG_TAG_AUTHENTICATION_API = "AuthenticationAPI";

    // Defines constants for bundle keywords/tags
    public static final String DEVICE_NAME = "DEVICE_NAME";
    public static final String TOAST_MSG = "TOAST_MSG";

    // Defines constant values
    public static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    public static final byte SIMULATOR_EOF = 0; // End-of-File byte

    // Defines several constants used when transmitting messages between the
    // Bluetooth service and the UI.
    public static final int MESSAGE_READ = 0;
    public static final int MESSAGE_WRITE = 1;
    public static final int MESSAGE_TOAST = 2;
    public static final int MESSAGE_DEVICE_NAME = 3;


    // Defines constants for Activity result return
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_ACCESS_COARSE_LOCATION = 2;
    public static final int REQUEST_CONNECT_BT_DEVICE = 3;

    //tags for http tasks
    public static final int GET_SALT = 1;
    public static final int GET_AUTHENTICATION = 2;
    public static final int GET_DATABASE = 3;

    /* GLOBAL VARIABLES */

    /* COMMON METHODS */
    public static boolean requestAccessCoarseLocation(Activity activity) {
        Log.v(Common.LOG_TAG_COMMON, "requestAccessCoarseLocation");

        // Check for location permission
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Request permission for location
            Log.d(Common.LOG_TAG_COMMON, "Requesting location access...");
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    Common.REQUEST_ACCESS_COARSE_LOCATION);
            return false;
        }

        return true;
    }
}
