package edu.duke.ece651.tyrata.communication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import edu.duke.ece651.tyrata.Common;
import edu.duke.ece651.tyrata.R;
import edu.duke.ece651.tyrata.vehicle.TireSnapshot;

/**
 * (NOT USED IN PRODUCTION)
 * Example Activity using Bluetooth API
 * @author Saeed Alrahma
 * Created by Saeed on 2/25/2018.
 */

public class BluetoothActivity extends AppCompatActivity {

    /* CONSTANTS */
    private byte EOF = 0; // End-of-File byte

    /* GLOBAL VARIABLES */
    private TextView mTextViewReceived;
    private TextView mTextViewParsed;
    private StringBuilder mXmlStream;
    private String mParsedMsg;
    private int mCount = 0;
    private int mSize = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        mTextViewReceived = findViewById(R.id.textView_s0_bt);
        mTextViewParsed = findViewById(R.id.textView_s1_bt);

        mXmlStream = new StringBuilder();
//        mXmlStream = "";
        mParsedMsg = "";

        // Enable Bluetooth
        Log.v(Common.LOG_TAG_BT_ACTIVITY, "Enabling Bluetooth...");
        BluetoothAPI.enableBt(this, mHandler);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BluetoothAPI.disableBt();
    }

    public void discoverBluetooth(View view) {
        Log.d(Common.LOG_TAG_BT_ACTIVITY, "discoverBluetooth()");

        // Launch the DeviceListActivity to see devices and do scan
        Intent serverIntent = new Intent(this, BluetoothDeviceListActivity.class);
        startActivityForResult(serverIntent, Common.REQUEST_CONNECT_BT_DEVICE);
    }

    public void connectBluetooth(Intent data) {
        Log.d(Common.LOG_TAG_BT_ACTIVITY, "connectBluetooth()");
        Bundle extras = data.getExtras();
        if (extras != null) {
            String address = extras.getString(BluetoothDeviceListActivity.EXTRA_DEVICE_ADDRESS);
            Log.d(Common.LOG_TAG_BT_ACTIVITY, "Connecting to " + address);
            Toast.makeText(getApplicationContext(),
                    "Connecting to " + address, Toast.LENGTH_SHORT).show();
            BluetoothAPI.connectBt(address);
        }
        else {
            Log.w(Common.LOG_TAG_BT_ACTIVITY, "No device selected to connect to");
            Toast.makeText(getApplicationContext(),
                    "No device selected to connect to", Toast.LENGTH_LONG).show();
        }
    }

    public void cancelBtConnection(View view) {
        BluetoothAPI.disableBt();
    }

    public void sampleDataTest(View view) {
        try {
            InputStream in = getResources().openRawResource(R.raw.xml_bluetooth_sample);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            processMsg(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeSuccess() {
        BluetoothAPI.write("S".getBytes());
    }

    private void writeFail() {
        BluetoothAPI.write("F".getBytes());
    }

    public void displayMsg(String msg, TextView textView) {
        textView.setText(msg);
    }

    public void processMsg(String msg) {
        Toast.makeText(getApplicationContext(), "Received " +msg.length() + " bytes.",
                Toast.LENGTH_SHORT).show();
        if (msg.length() > 20000) {
            String shortMsg = msg.substring(0,8192);
            shortMsg += "\n\n...\n\n";
            shortMsg += msg.substring(msg.length()-8192);
            displayMsg(shortMsg,mTextViewReceived);
        } else {
            displayMsg(msg, mTextViewReceived);
        }

        try {
            InputStream in = new ByteArrayInputStream(msg.getBytes("UTF-8"));
            // parse the message
            BluetoothXmlParser btXmlParser = new BluetoothXmlParser();
            ArrayList<TireSnapshot> tireSnapshotList = btXmlParser.parseToTireSnapshotList(in);
            StringBuilder parsedMsg = new StringBuilder();
            parsedMsg.append("Parsed " + tireSnapshotList.size() + " tires!\n");
            if (tireSnapshotList.isEmpty()){
                writeFail();
                Toast.makeText(getApplicationContext(),
                        "Failed to obtain TireSnapshot from message received...",
                        Toast.LENGTH_LONG).show();
            }
            writeSuccess();
            if (tireSnapshotList.size()<32) {
                for (int i=0; i<tireSnapshotList.size(); i++) {
                    parsedMsg.append("Tire/Sensor ID: " + tireSnapshotList.get(i).getSensorId());
                    parsedMsg.append( "\nS11: " + tireSnapshotList.get(i).getS11());
                    parsedMsg.append( "\nPressure: " + tireSnapshotList.get(i).getPressure());
                    parsedMsg.append( "\nMileage: " + tireSnapshotList.get(i).getOdometerMileage());
                    parsedMsg.append( "\nTimestamp: " + TireSnapshot.convertCalendarToString(tireSnapshotList.get(i).getTimestamp()));
                    parsedMsg.append( "\n\n");
                }
            }
            displayMsg(parsedMsg.toString(), mTextViewParsed);
        } catch (UnsupportedEncodingException e) {
            displayMsg(e.toString(), mTextViewParsed);
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            writeFail();
            displayMsg(e.toString(), mTextViewParsed);
            e.printStackTrace();
        } catch (IOException e) {
            writeFail();
            displayMsg(e.toString(), mTextViewParsed);
            e.printStackTrace();
        }
    }


    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        Log.v(Common.LOG_TAG_BT_ACTIVITY, "onActivityResult with code " + requestCode);
        switch (requestCode) {
            case Common.REQUEST_ENABLE_BT:
                if (resultCode == RESULT_CANCELED) {
                    Log.w(Common.LOG_TAG_BT_ACTIVITY, "Bluetooth enable request canceled");
                    Toast.makeText(getApplicationContext(),
                            "Bluetooth request cancelled. Cannot connect to sensors...",
                            Toast.LENGTH_LONG).show();
                }
                else if (resultCode == RESULT_OK) {
                    Log.v(Common.LOG_TAG_BT_ACTIVITY, "Bluetooth enabled");
                }
                break;
            case Common.REQUEST_ACCESS_COARSE_LOCATION:
                if (resultCode == RESULT_CANCELED) {
                    Log.w(Common.LOG_TAG_BT_ACTIVITY, "Location access request cancelled");
                    Toast.makeText(getApplicationContext(),
                            "Location access request cancelled. Cannot discover Bluetooth devices...",
                            Toast.LENGTH_LONG).show();
                }
                else if (resultCode == RESULT_OK) {
                    Log.v(Common.LOG_TAG_BT_ACTIVITY, "Location access granted");
                    //@todo this is not tested. Might not work here
                    BluetoothAPI.discoverBtDevices(this);
                }
                break;
            case Common.REQUEST_CONNECT_BT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    connectBluetooth(data);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Connection Bluetooth Device Failed...",
                            Toast.LENGTH_LONG).show();
                }
                break;
            default:
                Log.w(Common.LOG_TAG_BT_ACTIVITY, "Unknown REQUEST_CODE " + requestCode);
                Toast.makeText(getApplicationContext(),
                        "Something went wrong (Unknown REQUEST_CODE)...",
                        Toast.LENGTH_LONG).show();
        }
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    // @todo fix "This handler class should be static or leaks might occur" warning
    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
//            Log.i(Common.LOG_TAG_BT_ACTIVITY, "mHandler with type " + msg.what);
            switch (msg.what) {
                case Common.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the msg
                    String msgStr = new String(readBuf, 0, msg.arg1);
                    mXmlStream.append(msgStr);
//                    mXmlStream += msgStr;
                    if (readBuf[msg.arg1-1] == EOF) { // reached end of message/file
                        Log.d(Common.LOG_TAG_BT_ACTIVITY, "Message is: " + mXmlStream.length() + " Bytes");
                        processMsg(mXmlStream.toString());
                    }
                    mCount++;
                    mSize += msg.arg1;
                    break;
                case Common.MESSAGE_WRITE:
//                    Toast.makeText(getApplicationContext(), "Sent message with " + msg.arg1
//                            + " Bytes!", Toast.LENGTH_LONG).show();
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMsg = new String(writeBuf, 0, msg.arg1);
                    Toast.makeText(getApplicationContext(), "Sent " + writeMsg, Toast.LENGTH_SHORT).show();
                    break;
                case Common.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), "Some error occurred", Toast.LENGTH_SHORT).show();
                    break;
                case Common.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    String deviceName = msg.getData().getString(Common.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + deviceName, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Log.w(Common.LOG_TAG_BT_ACTIVITY, "Unknown message passed to handler: " + msg.what);
            }
        }
    };

}
