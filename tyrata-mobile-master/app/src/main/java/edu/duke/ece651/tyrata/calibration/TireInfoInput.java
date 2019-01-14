package edu.duke.ece651.tyrata.calibration;
/**
 * This class is Tireinfo input page
 * @author De Lan
 * Created by De Lan on 2/27/2018.
 */
import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.DragAndDropPermissions;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

import edu.duke.ece651.tyrata.MainActivity;
import edu.duke.ece651.tyrata.R;
import edu.duke.ece651.tyrata.datamanagement.Database;
import edu.duke.ece651.tyrata.display.TireInfo;
import edu.duke.ece651.tyrata.vehicle.Tire;

public class TireInfoInput extends AppCompatActivity {
    int axis_row;
    int axis_index;
    char axis_side;
    int vehicle_id;
    int tire_ID;
    String vin;
    String original_sensor;

    /**
     * Display the tire data if this is the edit page
     * Display the default tire data if this tire is newly added and been calibrated
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tire_info_input);
        Intent intent = getIntent();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        axis_row = intent.getIntExtra("axis_ROW",0);
        axis_index = intent.getIntExtra("axis_IDX",0);
        axis_side = intent.getCharExtra("axis_SIDE",'a');
        vin = intent.getStringExtra("VIN");

        tire_ID = -1;
        // switched from tire edit
        String sensor_id = intent.getStringExtra("SENSOR_ID");
        if(sensor_id != null && !sensor_id.equals("Need sensorID")) {
            Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
            vehicle_id = Database.getVehicleID(vin);
            Tire curr_tire = Database.getTire(axis_row, axis_index, axis_side, vehicle_id);
            tire_ID = Database.getTireID(sensor_id);
            boolean tire_snapshot_exist = Database.tireSnapshotExist(sensor_id);
            Database.myDatabase.close();
            Log.i("Tire Input edit", sensor_id);
            EditText textView_sensor = findViewById(R.id.edit_sensor_ID);
            textView_sensor.setText(sensor_id);
            EditText textView_manufacturer = findViewById(R.id.edit_manufacturer);
            textView_manufacturer.setText(curr_tire.getManufacturer());
            EditText textView_model = findViewById(R.id.edit_model);
            textView_model.setText(curr_tire.getModel());
            EditText textView_sku = findViewById(R.id.edit_SKU);
            textView_sku.setText(curr_tire.getSku());
            EditText textView_thickness = findViewById(R.id.edit_thickness);
            textView_thickness.setText(String.valueOf(curr_tire.get_INIT_THICK()));
            if(tire_snapshot_exist){
                textView_thickness.setKeyListener(null);
                String info = "This tire already has processed thickness history, you cannot edit the initial thickness.";
                Toast.makeText(getApplicationContext(), info, Toast.LENGTH_LONG).show();
            }
            original_sensor = sensor_id;
        }else{
            Log.i("Vehicle Input add_car", "add_car");
            original_sensor = "";
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.go_to_homepage, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        return true;
    }

    /**
     * Called to save the tire data into database and return to the vehicle page
     * Notify the user of invalid input
     */
    public void saveMessage(View view) {
        Intent intent = new Intent(this, TireInfo.class);
        EditText edit_manufacturer = (EditText) findViewById(R.id.edit_manufacturer);
        String message_manufacturer = edit_manufacturer.getText().toString();

        EditText edit_model = (EditText) findViewById(R.id.edit_model);
        String message_model = edit_model.getText().toString();

        EditText edit_SKU = (EditText) findViewById(R.id.edit_SKU);
        String message_SKU = edit_SKU.getText().toString();

        //TODO: calculate thickness from init_ss_id, store initial thickness
        EditText edit_thickness = (EditText) findViewById(R.id.edit_thickness);
        String message_thickness = edit_thickness.getText().toString();

        EditText edit_sensorID = (EditText) findViewById(R.id.edit_sensor_ID);
        String message_sensorID = edit_sensorID.getText().toString();

        String msg = "";
        if(message_manufacturer.equals("") || message_model.equals("") || message_SKU.equals("") || message_thickness.equals("") || message_sensorID.equals("")){
            msg = "You need to fill out all the fields!";
        }

        Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
        Log.i("tire input","new store!!!");
        Log.i("axis_ROW",String.valueOf(axis_row));
        Log.i("axis_IDX", String.valueOf(axis_index));
        Log.i("axis_SIDE", String.valueOf(axis_side));
        Log.i("sensor_ID", message_sensorID);
        Log.i("VIN", vin);
        Double thickness = 0.0;
        try {
            thickness = Double.parseDouble(message_thickness);
            if (thickness < 5.0 || thickness > 15.0) {
                msg = "The initial tire thickness need to between 5mm and 15mm!";
            }
        }
        catch (Exception e){
            msg = "Please type in valid number between 5 and 15.";
        }

        if(!msg.equals("")){
            notification(msg);
        } else {
            try {
                boolean storeTire = Database.storeTireData(original_sensor, tire_ID, message_sensorID, message_manufacturer, message_model, message_SKU, vin, axis_row, String.valueOf(axis_side), axis_index, thickness, 0, 0);
                Database.myDatabase.close();
                if (!storeTire) {
                    msg = "The Sensor ID already exists!";
                    throw new IOException();
                }
                intent.putExtra("AXIS_ROW", axis_row);
                intent.putExtra("AXIS_INDEX", axis_index);
                intent.putExtra("AXIS_SIDE", axis_side);
                intent.putExtra("VIN", vin);
                startActivity(intent);
            } catch (Exception e) {
                notification(msg);
            }
        }
    }

    private void notification(String msg){
        new AlertDialog.Builder(this)
                .setTitle("NOTIFICATION")
                .setMessage(msg)
                .setPositiveButton("Yes", null)
                .show();
    }
}
