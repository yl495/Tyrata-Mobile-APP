package edu.duke.ece651.tyrata;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import edu.duke.ece651.tyrata.calibration.Report_accident;
import edu.duke.ece651.tyrata.communication.BluetoothAPI;
import edu.duke.ece651.tyrata.communication.BluetoothDeviceListActivity;
import edu.duke.ece651.tyrata.communication.EmptyActivity;
import edu.duke.ece651.tyrata.communication.HTTPsender;
import edu.duke.ece651.tyrata.communication.ServerXmlParser;
import edu.duke.ece651.tyrata.datamanagement.Database;
import edu.duke.ece651.tyrata.display.Vehicle_Info;
import edu.duke.ece651.tyrata.processing.GpsAPI;
import edu.duke.ece651.tyrata.user.Edit_user_information;
import edu.duke.ece651.tyrata.user.User;
import edu.duke.ece651.tyrata.vehicle.TireSnapshot;
import edu.duke.ece651.tyrata.vehicle.Vehicle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class MainActivity extends EmptyActivity {
    private ListView vehicle_list;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mToggle;
    private List<Map<String, Object>> list;
    private NavigationView navigationView;
    private int user_ID;
    private StringBuilder mXmlStream;
    private String trace_message = "";
    private ProgressDialog mProgressDialog;

    int notificationID = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        mToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.open, R.string.close);
        mXmlStream = new StringBuilder();

        mDrawerLayout.addDrawerListener(mToggle);
        mToggle.syncState();
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_drawer);

        navigationView = (NavigationView) findViewById(R.id.drawer_navigation) ;

        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        menuItem.setChecked(true);
                        // close drawer when item is tapped
                        mDrawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here

                        switch (menuItem.getItemId()) {
                            case R.id.edit_account:
                                main_to_edit();
                                return true;
                            case R.id.log_out:
                                main_to_login();
                                return true;
                        }
                        return true;
                    }
                });

        //set the user information in the drawer menu
        Menu menu = navigationView.getMenu();
        MenuItem menu_username = menu.findItem(R.id.d_name);
        MenuItem menu_email = menu.findItem(R.id.d_email);
        MenuItem menu_phone = menu.findItem(R.id.d_phone);

        Intent intent = getIntent();
        user_ID = intent.getIntExtra("USER_ID", 0);
        if(user_ID == 0){
            SharedPreferences editor = getSharedPreferences("user_data",MODE_PRIVATE);
            user_ID = editor.getInt("USER_ID",0);
        }
        SharedPreferences.Editor editor= getSharedPreferences("user_data",MODE_PRIVATE).edit();
        editor.putInt("USER_ID",user_ID);
        editor.commit();
        Log.i("In main, user", String.valueOf(user_ID));
        Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
        // test functions
        Database.testUserTable();
        Database.testVehicleTable();
        Database.testTireTable();
        Database.testSnapTable();
//        Database.testTraceTable();

        User curr_user = Database.getUser(user_ID);
        Database.myDatabase.close();

        TextView textView_username = findViewById(R.id.textView_user);
        textView_username.setText(curr_user.username);
        menu_username.setTitle(curr_user.username);
        menu_email.setTitle(curr_user.email);
        menu_phone.setTitle(curr_user.phone);

        vehicle_list = (ListView) findViewById(R.id.vehicle_list);

        initDataList(curr_user.mVehicles);

        String[] from = { "img", "VIN", "make","model", "year" };
        int[] to = { R.id.item_img, R.id.item_vehicle, R.id.item_make,R.id.item_model,
                R.id.item_year };

        final SimpleAdapter adapter = new SimpleAdapter(this, list,
                R.layout.main_list_view_layout, from, to);

        vehicle_list.setAdapter(adapter);
        /**
         * click
         */
        vehicle_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                Map<String, Object> map = list.get(arg2);
                String str = "";
                str += map.get("VIN");
                main_to_vehicle_info(str);
            }
        });

        Common.requestAccessCoarseLocation(this);
        checkTraceTable();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_view_navigation, menu);
        //setIconEnable(menu, true);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BluetoothAPI.disableBt();
    }

    private void initDataList(ArrayList<Vehicle> vehicles) {
        int number = vehicles.size();
        int img[] ;
        img = new int[number];
        for (int i = 0; i < number; i++) {
            int tirenum_list = vehicles.get(i).getNumTires();
            if(tirenum_list == 4){
                img[i] = R.drawable.liyue_vehicle;
            }else if(tirenum_list == 6){
                img[i] = R.drawable.vehicle_list_6tires;
            }
            else if(tirenum_list == 8){
                img[i] = R.drawable.vehicle_list3;
            }
            else if(tirenum_list == 10){
                img[i] = R.drawable.vehicle_list_10tires;
            }
            else if(tirenum_list == 14){
                img[i] = R.drawable.vehicle_list3;
            }
            else if(tirenum_list == 18){
                img[i] = R.drawable.vehicle_list_18tires;
            }
        }
        list = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < number; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("img", img[i]);
            map.put("VIN", vehicles.get(i).getVin());
            map.put("make", "Make:" + vehicles.get(i).getMake() );
            map.put("model", "Model:" + vehicles.get(i).getModel() );
            map.put("year", "Year:" + String.valueOf(vehicles.get(i).getYear()));
            list.add(map);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        //EmptyActivity emptyActivity = new EmptyActivity();
        switch (item.getItemId()) {
            case R.id.n_submenu_Database:
                getDatabaseFromXml();
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);
                return true;
            case R.id.n_submenu_tireSnapshot:
                getTireSnapshotListFromXml();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void main_to_edit() {
        Intent intent = new Intent(MainActivity.this, Edit_user_information.class);
        startActivity(intent);
    }

    public void main_to_addcar(View view) {
        Intent intent = new Intent(MainActivity.this, edu.duke.ece651.tyrata.calibration.Input_Vehicle_Info.class);
        intent.putExtra("userID", user_ID);

        startActivity(intent);
    }
    public void main_to_report(View view) {
        Intent intent = new Intent(MainActivity.this, Report_accident.class);
        intent.putExtra("userID", user_ID);

        startActivity(intent);
    }
    public void main_to_login() {
        Intent intent = new Intent(MainActivity.this, edu.duke.ece651.tyrata.user.Log_in.class);

        startActivity(intent);
    }
    public void main_to_vehicle_info(String vin) {
        Intent intent = new Intent(MainActivity.this, Vehicle_Info.class);
        intent.putExtra("VIN", vin);
        startActivity(intent);
    }

    /**
     * Discover nearby bluetooth devices and establish a connection on select
     * @param view The button that was clicked
     */
    public void main_to_communication(View view) {
        Log.d(Common.LOG_TAG_MAIN_ACTIVITY, "main_to_communication()");

        if (BluetoothAPI.isBtReady(this, mHandler)) {
            // Launch the DeviceListActivity to see devices and do scan
            Intent discoverIntent = new Intent(this, BluetoothDeviceListActivity.class);
            startActivityForResult(discoverIntent, Common.REQUEST_CONNECT_BT_DEVICE);
        } else {
            Toast.makeText(this,
                    "Enable Bluetooth and try again",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Called to process and save snapshots into database
     * @param snapshots the TireSnapshot list fetched from the simulator
     */
    private void handleReceivedSnapshots(ArrayList<TireSnapshot> snapshots) {
        // close bluetooth connection
        BluetoothAPI.disableBt();
        HashSet<String> NotFoundSensorSet = new HashSet<String>();


        if(snapshots.isEmpty()){
            Toast.makeText(getApplicationContext(),
                    "Transfer failed...",
                    Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(getApplicationContext(),
                    "Received " + snapshots.size() + " tire snapshots",
                    Toast.LENGTH_SHORT).show();
        }
        try{
            /* Updated by Zijie and Yue on 3/24/2018. */
            /* Updated by Saeed and De Lan on 4/13/2018. */
            ArrayList<Double> GPS = GpsAPI.getGPS(this);
            Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
            for (int i = 0; i < snapshots.size(); i++) {
                double s11 = snapshots.get(i).getS11();
                String timestamp = TireSnapshot.convertCalendarToString(snapshots.get(i).getTimestamp());
                double mileage = snapshots.get(i).getOdometerMileage();
                double pressure = snapshots.get(i).getPressure();
                String sensor_id = snapshots.get(i).getSensorId();
                double init_thickness = Database.getInitThickness(sensor_id); //init_thickness
                double thickness = init_thickness;
                String eol = Double.toString((init_thickness - 3) * 5000);
                int days = (int) Double.parseDouble(eol)/20;
                if(days < 30){
                    String notification = "Need to Change Your Tire within 30 Days!";
                }
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat formatter=new SimpleDateFormat("MM-dd-yyyy");
                calendar.add(Calendar.DATE, days);
                Log.i("daysleft", Integer.toString(days));
                String time_to_replacement = formatter.format(calendar.getTime());
                double longitude = 0;
                double lat = 0;
                try {
                    if (GPS != null) {
                        longitude = GPS.get(0);
                        lat = GPS.get(1);
                    }
                }
                catch(Exception e){
                    Log.e(Common.LOG_TAG_MAIN_ACTIVITY, "Error aquiring GPS", e);
                }
                if(init_thickness == -1){
                    if(!NotFoundSensorSet.contains(sensor_id)){
                        NotFoundSensorSet.add(sensor_id);
                        String info = "The sensor ID <"+sensor_id+">does not exist in local database, please check and enter valid sensor ID!";
                        Toast.makeText(getApplicationContext(), info, Toast.LENGTH_SHORT).show();
                    }
                    continue;
                }
                /* Updated by Zijie and Yue on 3/31/2018. */
                String sql = "SELECT * FROM SNAPSHOT, TIRE WHERE TIRE.ID = TIRE_ID and TIRE.SENSOR_ID = ?";
                Cursor c = Database.myDatabase.rawQuery(sql, new String[] {sensor_id});
                if (c != null && c.moveToFirst()) {
                    double init_mS11 = c.getDouble(c.getColumnIndex("S11"));
                    thickness = snapshots.get(i).calculateTreadThickness(init_mS11, init_thickness);
                    eol = Double.toString((thickness - 3) * 5000);
                    int days1 = (int) Double.parseDouble(eol)/20;
                    if(days1 < 30){
                        String notification = "Need to Change Your Tire within 30 Days!";
                    }
                    Calendar calendar1 = Calendar.getInstance();
                    calendar1.add(Calendar.DATE, days1);
                    time_to_replacement = formatter.format(calendar1.getTime());
                    c.close();
                }
                boolean isoutlier = false;
                double mean = Database.get_mean_s11(sensor_id);
                Log.i("test outlier1", String.valueOf(mean));
                Log.i("test outlier2", String.valueOf(s11));
                //Database.testSnapTable();
                double deviation = Database.get_deviation_s11(sensor_id);
                if((s11 < mean - 3 * 0.01 || s11 > mean + 3 * 0.01) && mean != 0) {
                    isoutlier = true;
                }
                Log.i("test outlier3", String.valueOf(isoutlier));
                Log.i("test outlier4", String.valueOf(deviation));
                //Database.testSnapTable();
                boolean notDupSanpShot  = Database.storeSnapshot(s11, timestamp, mileage, pressure, sensor_id, isoutlier, thickness, eol, time_to_replacement, longitude, lat);
                int outlier_num = Database.get_outlier_num(sensor_id);
                //Log.i("TEST outliers NUM",String.valueOf(outlier_num));
                if(outlier_num % 3 == 0 && isoutlier) {
                    Log.i("notification: outliers",String.valueOf(outlier_num));
                    notification("OUTLIERS FOR THREE DAYS!");
                }
                if(notDupSanpShot){
                    boolean sensorExist = Database.updateTireSSID(sensor_id);
                    if (!sensorExist) {
                        String info = "The sensor ID <"+sensor_id+">does not exist in local database snapshot!";
                        Toast.makeText(getApplicationContext(), info, Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Log.i("In Empty Activity","Dup SanpShot!");
                }
            }
            Database.myDatabase.close();
        }
        catch(Exception e){
            notification(e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Show a progress dialog with a message
     * @param msg The message to display on the dialog
     */
    private void showProgressDialog(String msg) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.setMessage(msg);
        mProgressDialog.show();
    }

    /**
     * Hide the progress dialog
     */
    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    /**
     * Called when startActivityForResult finishes
     * @param requestCode Constant integer representing the request that finished
     * @param resultCode The result of the finished activity (e.g. OK, FAILED)
     * @param data Data sent back from the finished activity
     */
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Common.REQUEST_ENABLE_BT:
                if (resultCode == RESULT_CANCELED) {
                    Log.w(Common.LOG_TAG_MAIN_ACTIVITY, "Bluetooth enable request canceled");
                    Toast.makeText(getApplicationContext(),
                            "Bluetooth request cancelled. Cannot connect to sensors...",
                            Toast.LENGTH_LONG).show();
                }
                else if (resultCode == RESULT_OK) {
                    Log.v(Common.LOG_TAG_MAIN_ACTIVITY, "Bluetooth enabled");
                    main_to_communication(null);
                }
                break;
            case Common.REQUEST_ACCESS_COARSE_LOCATION:
                if (resultCode == RESULT_CANCELED) {
                    Log.w(Common.LOG_TAG_MAIN_ACTIVITY, "Location access request cancelled");
                    Toast.makeText(getApplicationContext(),
                            "Access Coarse Location denied. Cannot use Bluetooth functions",
                            Toast.LENGTH_SHORT).show();
                }
                else if (resultCode == RESULT_OK) {
                    Log.v(Common.LOG_TAG_MAIN_ACTIVITY, "Location access granted");
                }
                break;
            case Common.REQUEST_CONNECT_BT_DEVICE:
                if (resultCode == RESULT_OK) {
                    mXmlStream = new StringBuilder();
                    Toast.makeText(getApplicationContext(),
                            "Connecting...", Toast.LENGTH_SHORT).show();
                    BluetoothAPI.connectBt(data);
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Bluetooth connection Failed...",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                Log.w(Common.LOG_TAG_MAIN_ACTIVITY, "Unknown REQUEST_CODE " + requestCode);
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
            switch (msg.what) {
                case Common.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the msg
                    String msgStr = new String(readBuf, 0, msg.arg1);
                    mXmlStream.append(msgStr);
                    if (readBuf[msg.arg1-1] == Common.SIMULATOR_EOF) { // reached end of message/file
                        Log.d(Common.LOG_TAG_MAIN_ACTIVITY, "Message is: " + mXmlStream.length() + " Bytes");
//                        hideProgressDialog();
                        showProgressDialog("Parsing message...");
                        ArrayList<TireSnapshot> snapshots = BluetoothAPI.processMsg(mXmlStream.toString());
//                        hideProgressDialog();
                        showProgressDialog("Storing tire snapshots...");
                        handleReceivedSnapshots(snapshots);
                        hideProgressDialog();
                    }
                    break;
                case Common.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMsg = new String(writeBuf, 0, msg.arg1);
                    Log.v(Common.LOG_TAG_MAIN_ACTIVITY, "Sent " + writeMsg);
//                    Toast.makeText(getApplicationContext(), "Sent " + writeMsg, Toast.LENGTH_SHORT).show();
                    hideProgressDialog();
                    break;
                case Common.MESSAGE_TOAST:
                    // save the connected device's name
                    String toastMsg = msg.getData().getString(Common.TOAST_MSG);
                    Toast.makeText(getApplicationContext(),
                            "Error occurred: " + toastMsg,
                            Toast.LENGTH_SHORT).show();
                    break;
                case Common.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    String deviceName = msg.getData().getString(Common.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + deviceName, Toast.LENGTH_SHORT).show();
                    showProgressDialog("Waiting for message...");
                    break;
                default:
                    Log.w(Common.LOG_TAG_MAIN_ACTIVITY, "Unknown message passed to handler: " + msg.what);
            }
        }
    };

    // This method open a new thread to send and receive message from server
    private void send_message(String urlStr) {
        final String url = urlStr;
        String resource;
        new Thread() {
            public void run() {
                InputStream in = null;
                Message msg = Message.obtain();
                msg.what = 1;
                try {
                    Log.i("send_url",url);
                    in = openHttpConnection(url);
                    String resource = new Scanner(in).useDelimiter("\\Z").next();
                    Log.i("test_new_method",resource);
                    Bundle b = new Bundle();
                    b.putString("get_message", resource);
                    msg.setData(b);
                    in.close();
                }catch (IOException e1) {
                    e1.printStackTrace();
                }
                traceHandler.sendMessage(msg);
            }
        }.start();
    }

    private InputStream openHttpConnection(String urlStr) {
        InputStream in = null;
        int resCode = -1;

        try {
            URL url = new URL(urlStr);
            URLConnection urlConn = url.openConnection();

            if (!(urlConn instanceof HttpURLConnection)) {
                Log.i("new_method","wrong");
                throw new IOException("URL is not an Http URL");
            }

            HttpURLConnection httpConn = (HttpURLConnection) urlConn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();
            resCode = httpConn.getResponseCode();

            if (resCode == HttpURLConnection.HTTP_OK) {
                Log.i("new_method","get");
                in = httpConn.getInputStream();
            }
        }catch (MalformedURLException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return in;
    }

    private Handler traceHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String message = msg.getData().getString("get_message");

            ServerXmlParser parser = new ServerXmlParser();
            InputStream msg_getdata = new ByteArrayInputStream(message.getBytes());
            try {
                parser.parse_server(msg_getdata, getApplicationContext());
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            checkTraceTable();
        }
    };

    private void checkTraceTable(){
        HTTPsender mSender = new HTTPsender();
        trace_message = mSender.send_to_cloud(getApplicationContext());
        if(trace_message != null) {
            Log.i("main_trace",trace_message);
            send_message(trace_message);
        }
        else{
            Log.i("main","trace is null");
        }
    }
}
