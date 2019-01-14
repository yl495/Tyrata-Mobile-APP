package edu.duke.ece651.tyrata.communication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Set;

import edu.duke.ece651.tyrata.Common;
import edu.duke.ece651.tyrata.vehicle.TireSnapshot;

import static android.support.v4.app.ActivityCompat.startActivityForResult;

/**
 * This class has Bluetooth API
 * @author Saeed Alrahma
 * Created by Saeed on 2/25/2018.
 */

public class BluetoothAPI {
    private static BluetoothAdapter mBluetoothAdapter; // Device BluetoothAPI adapter (required for all BluetoothAPI activity)
    private static ConnectThread mConnectThread;
    private static ConnectedThread mConnectedThread;
    private static Handler mHandler; // handler that gets info from Bluetooth service

    /**
     * Enable device bluetooth (if available)
     * @param activity The activity to return results
     * @param handler The bluetooth connection handler
     * @return True if bluetooth is enabled, false otherwise
     */
    static boolean enableBt(Activity activity, Handler handler) {
        Log.v(Common.LOG_TAG_BT_API, "enableBt()");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device doesn't support BluetoothAPI
            Log.e(Common.LOG_TAG_BT_API, "Device doesn't support BluetoothAPI");
            Toast.makeText(activity.getApplicationContext(),
                    "Device doesn't support Bluetooth. Cannot connect to sensors...",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        // Check if BluetoothAPI is enabled
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(Common.LOG_TAG_BT_API, "Enabling Bluetooth...");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(activity, enableBtIntent, Common.REQUEST_ENABLE_BT, null);
            return false;
        }

        mHandler = handler;

        return true;
    }

    /**
     * Disable device bluetooth discovery and
     * close current connection
     */
    public static void disableBt() {
        // Make sure we're not doing discovery anymore
        cancelBtDiscovery();
        closeBtConnection();
    }

    /**
     * Get all paired devices
     * @return Set of paired devices
     */
    static Set<BluetoothDevice> getBtPairedDevices() {
        return mBluetoothAdapter.getBondedDevices();
    }

    /**
     * Print a list of paired devices
     */
    static void queryBtPairedDevices() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices
            // Get the name and address of each paired device.
            Log.v(Common.LOG_TAG_BT_API, "List of paired devices:");
            for (BluetoothDevice device : pairedDevices) {
                Log.v(Common.LOG_TAG_BT_API, "Device name: " + device.getName() + ", " +
                        "MAC address: " + device.getAddress());
            }
        }
    }

    /**
     * Query if device is ready (bluetooth enabled with all permissions)
     * @param activity The activity page to return results
     * @param handler The bluetooth connection handler
     * @return True if device ready to discover and connect through bluetooth, otherwise false
     */
    public static boolean isBtReady(Activity activity, Handler handler) {
        Log.d(Common.LOG_TAG_BT_API, "isBtReady()");

        // Enable Bluetooth
        if (!enableBt(activity, handler))
            return false;
        Log.v(Common.LOG_TAG_BT_API, "Bluetooth is already enabled");

        if (!Common.requestAccessCoarseLocation(activity)) {
            return false;
        }
        Log.v(Common.LOG_TAG_BT_API, "Bluetooth enabled and location access already granted");

        return true;
    }

    /**
     * Discover bluetooth devices
     * @param activity The activity to return results
     * @return True if device started discovering devices, false if discovery failed
     */
    static boolean discoverBtDevices(Activity activity) {
        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            Log.d(Common.LOG_TAG_BT_API, "cancelDiscovery()");
            mBluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        boolean discoverySuccess = mBluetoothAdapter.startDiscovery();
        Log.d(Common.LOG_TAG_BT_API,
                "startDiscovery() " + (discoverySuccess? "successful":"failed"));

        return discoverySuccess;
    }

    /**
     * Cancel bluetooth device discovery
     */
    static void cancelBtDiscovery() {
        // Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    /**
     * Connect to a bluetooth device
     * @param data Intent with device information to connect to
     */
    public static void connectBt(Intent data) {
        Log.d(Common.LOG_TAG_BT_API, "connectBt()");
        Bundle extras = data.getExtras();
        if (extras != null) {
            String address = extras.getString(BluetoothDeviceListActivity.EXTRA_DEVICE_ADDRESS);
            Log.d(Common.LOG_TAG_BT_API, "Connecting to " + address);
            BluetoothAPI.connectBt(address);
        }
        else {
            Log.w(Common.LOG_TAG_BT_API, "No device selected to connect to");
        }
    }

    /**
     * Connect to a bluetooth device
     * @param device Bluetooth device to connect to
     */
    private static void connectBt(BluetoothDevice device) {
        Log.d(Common.LOG_TAG_BT_API, "ConnectBt()");

        // Close any previous attempts to connect
        if (mConnectThread != null)
            mConnectThread.cancel();

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
    }

    /**
     * Connect to a bluetooth device
     * @param deviceAddress String with device MAC address to connect to
     */
    static void connectBt(String deviceAddress) {
        connectBt(mBluetoothAdapter.getRemoteDevice(deviceAddress));
    }

    /**
     * Cancel all bluetooth threads
     */
    private static void closeBtConnection() {
        if (mConnectedThread != null)
            mConnectedThread.cancel();
        else if (mConnectThread != null)
            mConnectThread.cancel();

        mConnectThread = null;
        mConnectedThread = null;
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    private static void connectedBt(BluetoothSocket socket, BluetoothDevice device) {
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Common.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Common.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Write to the ConnectedThread
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    static void write(byte[] out) {
        if (mConnectedThread != null) {
            Log.d(Common.LOG_TAG_BT_API, "Writing to device through Bluetooth");
            // Perform the write
            mConnectedThread.write(out);
        } else {
            Log.d(Common.LOG_TAG_BT_API, "No ConnectedThread Available");
        }
    }

    /**
     * Transfer success to connected device
     */
    private static void writeSuccess() {
        write("S".getBytes());
    }

    /**
     * Transfer failure to connected device
     */
    private static void writeFail() {
        write("F".getBytes());
    }

    /**
     * Process the received message into TireSnapshots
     * @param msg XML formatted message with TireSnapshot for multiple days
     * @return List of TireSnapshot
     */
    public static ArrayList<TireSnapshot> processMsg(String msg) {
        ArrayList<TireSnapshot> tireSnapshotList = new ArrayList<>();
        try {
            InputStream in = new ByteArrayInputStream(msg.getBytes("UTF-8"));
            // parse the message
            BluetoothXmlParser btXmlParser = new BluetoothXmlParser();
            tireSnapshotList = btXmlParser.parseToTireSnapshotList(in);
            if (tireSnapshotList.isEmpty()){
                writeFail();
                return tireSnapshotList;
            }
            writeSuccess();
        } catch (UnsupportedEncodingException e) {
            Log.e(Common.LOG_TAG_BT_API, "Bluetooth processMsg failed", e);
        } catch (XmlPullParserException e) {
            writeFail();
            Log.e(Common.LOG_TAG_BT_API, "Bluetooth processMsg failed", e);
        } catch (IOException e) {
            writeFail();
            Log.e(Common.LOG_TAG_BT_API, "Bluetooth processMsg failed", e);
        }

        return tireSnapshotList;
    }

    static String getDeviceName() {
        return mBluetoothAdapter.getName();
    }


    /**
     * Thread to establish a Bluetooth RRFCOM connection
     */
    private static class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(Common.MY_UUID);
            } catch (IOException e) {
                Log.e(Common.LOG_TAG_BT_API, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
            Log.d(Common.LOG_TAG_BT_API, "Bluetooth Socket " + mmSocket);
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                Log.d(Common.LOG_TAG_BT_API, "mmSocket.connect() successful!");
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    Log.d(Common.LOG_TAG_BT_API, "mmSocket.connect() failed", connectException);
                } catch (IOException closeException) {
                    Log.e(Common.LOG_TAG_BT_API, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedBt(mmSocket, mmDevice);
            Log.d(Common.LOG_TAG_BT_API, "ConnectThread is ConnectedThread");
        }

        // Closes the client socket and causes the thread to finish.
        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(Common.LOG_TAG_BT_API, "Could not close the client socket", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(Common.LOG_TAG_BT_API, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(Common.LOG_TAG_BT_API, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            Log.d(Common.LOG_TAG_BT_API, "Connected socket and attached i/o streams");
        }

        public void run() {
            Log.i(Common.LOG_TAG_BT_API, "BEGIN ConnectedThread");
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
//                    Log.d(Common.LOG_TAG_BT_API, "Bytes received: " + numBytes);
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = mHandler.obtainMessage(
                            Common.MESSAGE_READ, numBytes, -1, mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(Common.LOG_TAG_BT_API, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        /**
         * Write to the connected OutStream.
         *
         * @param bytes The bytes to write
         */
        void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);

                // Share the sent message with the UI activity.
                Message writtenMsg = mHandler.obtainMessage(
                        Common.MESSAGE_WRITE, bytes.length, -1, bytes);
                writtenMsg.sendToTarget();
            } catch (IOException e) {
                Log.e(Common.LOG_TAG_BT_API, "Error occurred when sending data", e);

                // Send a failure message back to the activity.
                Message writeErrorMsg =
                        mHandler.obtainMessage(Common.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString(Common.TOAST_MSG,
                        "Couldn't send data to the other device");
                writeErrorMsg.setData(bundle);
                mHandler.sendMessage(writeErrorMsg);
            }
        }

        // Call this method from the main activity to shut down the connection.
        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(Common.LOG_TAG_BT_API, "Could not close the connect socket", e);
            }
        }
    }
}
