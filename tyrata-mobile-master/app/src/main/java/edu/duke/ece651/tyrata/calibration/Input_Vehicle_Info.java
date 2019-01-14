package edu.duke.ece651.tyrata.calibration;
import android.app.AlertDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.List;
import edu.duke.ece651.tyrata.MainActivity;
import edu.duke.ece651.tyrata.R;
import edu.duke.ece651.tyrata.datamanagement.Database;
import edu.duke.ece651.tyrata.vehicle.Vehicle;

/*Created by Ming Yang
 * the java code of the activity_input_vehicle_info.xml page
 */

public class Input_Vehicle_Info extends AppCompatActivity {
    private Spinner spinner_Tirenumber;
    private List<String> dataList;
    private ArrayAdapter<String> adapter;
    private int user_ID;
    private int vehicle_ID;
    String tirenumber;
    String original_vin;

    /**
     * Display the vehicle data if this is the edit page
     * Display the default vehicle data if this vehicle is newly added and been calibrated
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input__vehicle__info);

        //add toolbar to the page
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        Intent intent = getIntent();
        user_ID = intent.getIntExtra("userID", 1);

        String vin = intent.getStringExtra("VIN");
        vehicle_ID = -1;

        //if the vehicle already exist, show the origin info on the edittext
        if (vin != null) {
            Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
            Vehicle cur_vehicle = Database.getVehicle(vin);
            vehicle_ID = Database.getVehicleID(vin);
            Database.myDatabase.close();
            Log.i("Vehicle Input edit", vin);
            EditText textView_vin = findViewById(R.id.edit_vin);
            textView_vin.setText(vin);
            EditText textView_make = findViewById(R.id.edit_make);
            textView_make.setText(cur_vehicle.getMake());
            EditText textView_model = findViewById(R.id.edit_model);
            textView_model.setText(cur_vehicle.getModel());
            EditText textView_year = findViewById(R.id.edit_year);
            textView_year.setText(String.valueOf(cur_vehicle.getYear()));
            original_vin = vin;
        }
        //if the vehicle doesn't exist, add a new vehicle
        else {
            Log.i("Vehicle Input add_car", "add_car");
            original_vin = "";
        }

        //set the spinner for the tire number
        spinner_Tirenumber = (Spinner) findViewById(R.id.spinner_tirenumber);

        dataList = new ArrayList<String>();
        dataList.add("4");
        dataList.add("6");
        dataList.add("8");
        dataList.add("10");
        dataList.add("14");
        dataList.add("18");

        adapter = new ArrayAdapter<String>(this, R.layout.my_spinner, dataList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_Tirenumber.setAdapter(adapter);
        spinner_Tirenumber.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                adapter.getItem(position);
                tirenumber = dataList.get(position);
                parent.setVisibility(View.VISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.go_to_homepage, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.homepage) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        return true;
    }

    /**
     * Called to save the vehicle data into database
     * Notify the user of invalid input
     */
    public void saveMessage(View view) {
        String msg = "";
        Intent intent = new Intent(this, MainActivity.class);
        EditText edit_make = (EditText) findViewById(R.id.edit_make);
        String message_make = edit_make.getText().toString();

        EditText edit_model = (EditText) findViewById(R.id.edit_model);
        String message_model = edit_model.getText().toString();

        EditText edit_year = (EditText) findViewById(R.id.edit_year);
        String message_year = edit_year.getText().toString();

        EditText edit_vin = (EditText) findViewById(R.id.edit_vin);
        String message_vin = edit_vin.getText().toString();

        if (message_make.equals("") || message_model.equals("") || message_year.equals("") || message_vin.equals("")) {
            msg = "You need to fill out all the fields!";
        }

        int num = Integer.parseInt(tirenumber);
        int axis_num = 0;
        if (num == 4) {
            axis_num = 2;
        }
        else if(num == 6 || num == 10) {
            axis_num = 3;
        }
        else if(num == 8 || num == 14){
            axis_num = 4;
        }
        else if(num == 18){
            axis_num = 5;
        }

        if (!msg.equals("")) {
            notification(msg);
        } else {
            try {
                Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
                boolean noConflict = Database.storeVehicleData(original_vin, vehicle_ID, message_vin, message_make, message_model, Integer.parseInt(message_year), axis_num, Integer.parseInt(tirenumber), user_ID);
                Database.myDatabase.close();
                if (!noConflict) {
                    msg = "The VIN already exists!";
                    notification(msg);
                } else {
                    startActivity(intent);
                }
            } catch (Exception e) {
                msg = "Please type in valid information!";
                notification(msg);
            }
        }
    }

    /**Show the notification
     *
     * @param msg the message string that will show on the notification dialogue
     */
    private void notification(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("NOTIFICATION")
                .setMessage(msg)
                .setPositiveButton("Yes", null)
                .show();
    }
}
