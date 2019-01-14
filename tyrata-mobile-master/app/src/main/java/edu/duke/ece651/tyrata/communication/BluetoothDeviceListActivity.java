package edu.duke.ece651.tyrata.communication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Set;

import edu.duke.ece651.tyrata.Common;
import edu.duke.ece651.tyrata.R;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 * @author Saeed Alrahma
 * Created by Saeed on 3/12/2018.
 * Resources:
 *  https://developer.android.com/samples/BluetoothChat/index.html
 */
public class BluetoothDeviceListActivity extends AppCompatActivity {

    public static String EXTRA_DEVICE_ADDRESS = "device_address"; // Return Intent extra
    private ArrayAdapter<String> mNewDevicesArrayAdapter; // Newly discovered devices

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_bt_device_list);

        // Set result CANCELED in case the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to perform device discovery
        Button scanButton = findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                BluetoothAPI.discoverBtDevices(getParent());
            }
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        ArrayAdapter<String> pairedDevicesArrayAdapter =
                new ArrayAdapter<>(this, R.layout.bt_device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.bt_device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when discovery is started/finished or device is discovered.
        Log.v(Common.LOG_TAG_BT_DEVICE_LIST_ACTIVITY, "Registering receiver...");
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = BluetoothAPI.getBtPairedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.bt_none_paired).toString();
            pairedDevicesArrayAdapter.add(noDevices);
        }

        // Start discovery
        BluetoothAPI.discoverBtDevices(this);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    /**
     * The on-click listener for all devices in the ListViews
     */
    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            BluetoothAPI.cancelBtDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Log.v(Common.LOG_TAG_BT_DEVICE_LIST_ACTIVITY, "BroadcastReceiver onReceive");
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If name is null, ignore
                if (device.getName() == null) {
                    return;
                }

                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    // New device, add to list
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }

            }
            // When discovery starts, change the Activity title
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.v(Common.LOG_TAG_BT_DEVICE_LIST_ACTIVITY, "BroadcastReceiver onReceive DISCOVERY_STARTED");
                setProgressBarIndeterminateVisibility(true);
                setTitle(R.string.bt_scanning);
                // Turn on sub-title for new devices
                findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
                // Hide scan button
                Button scanButton = findViewById(R.id.button_scan);
                scanButton.setVisibility(View.GONE);
            }
            // When discovery is finished, change the Activity title
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.v(Common.LOG_TAG_BT_DEVICE_LIST_ACTIVITY, "BroadcastReceiver onReceive DISCOVERY_FINISHED");
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.bt_select_device);
//                // display no devices, if none found
//                if (mNewDevicesArrayAdapter.getCount() == 0) {
//                    String noDevices = getResources().getText(R.string.bt_none_found).toString();
//                    mNewDevicesArrayAdapter.add(noDevices);
//                }
                // Show scan button
                Button scanButton = findViewById(R.id.button_scan);
                scanButton.setVisibility(View.VISIBLE);
            }
        }
    };
}
