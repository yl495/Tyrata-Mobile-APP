package edu.duke.ece651.tyrata.communication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.location.Location;
import android.support.constraint.solver.widgets.Snapshot;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import edu.duke.ece651.tyrata.datamanagement.Database;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;

import edu.duke.ece651.tyrata.R;
import edu.duke.ece651.tyrata.datamanagement.Database;
import edu.duke.ece651.tyrata.processing.GPStracker;
import edu.duke.ece651.tyrata.processing.GpsAPI;
import edu.duke.ece651.tyrata.vehicle.TireSnapshot;

public class EmptyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }



    public void goToBluetooth() {
        Intent intent = new Intent(this, BluetoothActivity.class);
        startActivity(intent);
    }

    public void testParseXml() {
        BluetoothXmlParser xmlParser = new BluetoothXmlParser();
        ArrayList<BluetoothXmlParser.DailyS11> list;
        try {
            list = xmlParser.parse(getResources().openRawResource(R.raw.xml_bluetooth_sample));
            String msg = "";
            if (list.isEmpty()) {
                msg = "No DailyS11...";
            } else {
                BluetoothXmlParser.DailyS11 dailyS11 = list.get(0);
                ArrayList<TireSnapshot> tires = dailyS11.mTires;
                msg = "Timestamp: " + dailyS11.mTimestamp;
                msg += ", Mileage: " + dailyS11.mMileage;
                msg += ", Tire #1: " + tires.get(0).getSensorId();
                msg += " S11: " + tires.get(0).getS11();
                msg += " Pressure: " + tires.get(0).getPressure();
                msg += ", Tire #2: " + tires.get(1).getSensorId();
                msg += " S11: " + tires.get(1).getS11();
                msg += " Pressure: " + tires.get(1).getPressure();
            }
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /* Updated by Zijie and Yue on 3/24/2018. */
    /* Updated by Saeed and De Lan on 3/25/2018. */
    public void getTireSnapshotListFromXml() {
        BluetoothXmlParser xmlParser = new BluetoothXmlParser();
        try {
            ArrayList<TireSnapshot> tireSnapshotList = xmlParser.parseToTireSnapshotList(
                    getResources().openRawResource(R.raw.xml_bluetooth_sample));
            if (tireSnapshotList.isEmpty()) {
                Toast.makeText(getApplicationContext(),
                        "Failed to obtain TireSnapshot from message received...",
                        Toast.LENGTH_LONG).show();
            }
            HashSet<String> NotFoundSensorSet = new HashSet<String>();
            ArrayList<Double> GPS = GpsAPI.getGPS(this);
            Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
            for (int i = 0; i < tireSnapshotList.size(); i++) {
                double s11 = tireSnapshotList.get(i).getS11();
                String timestamp = TireSnapshot.convertCalendarToString(tireSnapshotList.get(i).getTimestamp());
                double mileage = tireSnapshotList.get(i).getOdometerMileage();
                double pressure = tireSnapshotList.get(i).getPressure();
                String sensor_id = tireSnapshotList.get(i).getSensorId();

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
                //String time_to_replacement = Integer.toString(days);
                double longitude = 0;
                double lat = 0;
                try {
                    if (GPS != null) {
                        longitude = GPS.get(0);
                        lat = GPS.get(1);
                    }
                }
                catch(Exception e){
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                // init_thickness is -1 when the sensor_id is not found in database
                if(init_thickness == -1 ){
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
//                Cursor c = Database.myDatabase.rawQuery("SELECT * FROM SNAPSHOT, TIRE WHERE TIRE.ID = TIRE_ID and TIRE.SENSOR_ID =  '"+sensor_id+"'", null);
                if (c != null && c.moveToFirst()) {
                    double init_mS11 = c.getDouble(c.getColumnIndex("S11"));
                    //thickness = tireSnapshotList.get(i).calculateTreadThickness(init_mS11, init_thickness);
                    thickness = init_thickness - 12.50 * (s11 - init_mS11);
                    Log.i("initial_s11", Double.toString(init_mS11));
                    Log.i("current_s11", Double.toString(s11));
                    eol = Double.toString((thickness - 3) * 5000);
                    int days1 = (int) Double.parseDouble(eol)/20;
                    if(days1 < 30){
                        String notification = "Need to Change Your Tire within 30 Days!";
                    }
                    Calendar calendar1 = Calendar.getInstance();
                    calendar1.add(Calendar.DATE, days1);
                    time_to_replacement = formatter.format(calendar1.getTime());
                    //time_to_replacement = Integer.toString(days1);
                    c.close();
                }
                boolean isoutlier = false;
                double mean = Database.get_mean_s11(sensor_id);
                Log.i("test outlier1", String.valueOf(mean));
                Log.i("test outlier2", String.valueOf(s11));
                //Database.testSnapTable();
                //double deviation = Database.get_deviation_s11(sensor_id);
                //if((s11 < mean - 3 * deviation || s11 > mean + 3 * deviation) && mean != 0) {
                if((s11 < mean - 3 * 0.01 || s11 > mean + 3 * 0.01) && mean != 0) {
                    isoutlier = true;
                    Log.i("test min", String.valueOf(mean - 3 * 0.01));
                    Log.i("test max", String.valueOf(mean + 3 * 0.01));
                } else {
                    isoutlier = false;
                }
                Log.i("test outlier3", String.valueOf(isoutlier));
                //Log.i("test outlier4", String.valueOf(deviation));

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
                if(i > 2)
                    continue;
                String info = "Tire/Sensor ID: " + tireSnapshotList.get(i).getSensorId();
                info += "\nS11: " + tireSnapshotList.get(i).getS11();
                info += "\nPressure: " + tireSnapshotList.get(i).getPressure();
                info += "\nMileage: " + tireSnapshotList.get(i).getOdometerMileage();
                info += "\nTimestamp: " + TireSnapshot.convertCalendarToString(tireSnapshotList.get(i).getTimestamp());
                Toast.makeText(getApplicationContext(), info, Toast.LENGTH_SHORT).show();
            }
            Database.myDatabase.close();
        } catch (XmlPullParserException e) {
            notification(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            notification(e.getMessage());
            e.printStackTrace();
        }
    }

    public void getDatabaseFromXml() {
        ServerXmlParser xmlParser = new ServerXmlParser();
        try {
            xmlParser.parse_server(getResources().openRawResource(R.raw.xml_get_from_server_sample), getApplicationContext());
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void notification(String msg){
        new AlertDialog.Builder(this)
                .setTitle("NOTIFICATION")
                .setMessage(msg)
                .setPositiveButton("Yes", null)
                .show();
    }
}
