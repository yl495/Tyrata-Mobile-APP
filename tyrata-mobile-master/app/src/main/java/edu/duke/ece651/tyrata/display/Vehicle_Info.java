package edu.duke.ece651.tyrata.display;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.duke.ece651.tyrata.MainActivity;
import edu.duke.ece651.tyrata.R;
import edu.duke.ece651.tyrata.calibration.Input_Vehicle_Info;
import edu.duke.ece651.tyrata.datamanagement.Database;
import edu.duke.ece651.tyrata.vehicle.Tire;
import edu.duke.ece651.tyrata.vehicle.Vehicle;

/**
 * Created by Ming .
 * the java code of the activity_vehicle_info.xml page
 */

public class Vehicle_Info extends AppCompatActivity {
    private Integer buttonnumber = 0;
    private Vehicle curr_vehicle;
    private String vin;
    private int user_id;
    private ListView tire_list;
    private List<Map<String, Object>> list;
    private int axis_row;
    private char axis_side;
    private int axis_index;

    @Override
    protected  void onStop(){
        SharedPreferences.Editor editor = getSharedPreferences("user_data",MODE_PRIVATE).edit();
        editor.putInt("USER_ID",user_id);
        Log.i("In Vehicle Stop", String.valueOf(user_id));
        editor.commit();
        super.onStop();
    }
    @Override
    protected  void onDestroy(){
        SharedPreferences.Editor editor = getSharedPreferences("user_data",MODE_PRIVATE).edit();
        editor.putInt("USER_ID",user_id);
        Log.i("In Vehicle onDestroy", String.valueOf(user_id));
        editor.commit();
        super.onDestroy();
    }
    @Override
    protected  void onPause(){
        SharedPreferences.Editor editor = getSharedPreferences("user_data",MODE_PRIVATE).edit();
        editor.putInt("USER_ID",user_id);
        Log.i("In Vehicle onPause", String.valueOf(user_id));
        editor.commit();
        super.onPause();
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
     * Display the Vehicle data: display the default data if there is no saved data for this vehicle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle__info);

        //add toolbar to the page
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new         Intent(getApplicationContext(),MainActivity.class));
            }
        });


        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        vin = intent.getStringExtra("VIN");

        if(vin == null){
            SharedPreferences editor = getSharedPreferences("tire_data",MODE_PRIVATE);
            vin = editor.getString("VIN","");
            Log.i("In VehicleInfo",vin);
        }
        //getVehicle
        Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
        SharedPreferences editor = getSharedPreferences("user_data",MODE_PRIVATE);
        user_id = editor.getInt("USER_ID",0);
        curr_vehicle = Database.getVehicle(vin);
        Database.myDatabase.close();

        Log.i("In vehicle info, VIN", vin);
        Log.i("In vehicle info, user", String.valueOf(user_id));

        //show the info of the vehicle
        String message_make = curr_vehicle.getMake();
        TextView textView_make = findViewById(R.id.textView_make);
        textView_make.setText(message_make);

        String message_model = curr_vehicle.getModel();
        TextView textView_model = findViewById(R.id.textView_model);
        textView_model.setText(message_model);

        String message_year = String.valueOf(curr_vehicle.getYear());
        TextView textView_year = findViewById(R.id.textView_year);
        textView_year.setText(message_year);

        TextView textView_vin = findViewById(R.id.textView_vin);
        textView_vin.setText(vin);

        String message_tirenumber = String.valueOf(curr_vehicle.getNumTires()) ;
        if(message_tirenumber.equals("")){
            message_tirenumber = "4";
        }
        TextView textView_tirenumber = findViewById(R.id.textView_tirenumber);
        textView_tirenumber.setText(message_tirenumber);

        //set the image of the vehicle
        ImageView imageView= findViewById(R.id.image_vehicle);
        ImageView vehicle_image = findViewById(R.id.vehicle_image);
        if(curr_vehicle.getNumTires() == 4){
            imageView.setImageResource(R.drawable.four_wheel2);
            vehicle_image.setImageResource(R.drawable.liyue_vehicle);
        }
        else if(curr_vehicle.getNumTires() == 6){
            imageView.setImageResource(R.drawable.six_wheel);
            vehicle_image.setImageResource(R.drawable.vehicle_list_6tires);
        }
        else if(curr_vehicle.getNumTires() == 8){
            imageView.setImageResource(R.drawable.eight_wheel);
            vehicle_image.setImageResource(R.drawable.vehicle_list_6tires);
        }
        else if(curr_vehicle.getNumTires() == 10){
            imageView.setImageResource(R.drawable.ten_wheel);
            vehicle_image.setImageResource(R.drawable.vehicle_list_10tires);
        }
        else if(curr_vehicle.getNumTires() == 14){
            imageView.setImageResource(R.drawable.fourteen_wheel);
            vehicle_image.setImageResource(R.drawable.vehicle_list_6tires);
        }
        else if(curr_vehicle.getNumTires() == 18){
            imageView.setImageResource(R.drawable.eighteen_wheel);
            vehicle_image.setImageResource(R.drawable.vehicle_list_18tires);
        }
        else{
            imageView.setImageResource(R.drawable.four_wheel);
        }

        buttonnumber=Integer.parseInt(message_tirenumber);

        //set the list of the tires
        tire_list = (ListView) findViewById(R.id.tire_list);
        initDataList(buttonnumber,curr_vehicle.mTires);

        String[] from = { "img", "tire number","replace" };

        int[] to = { R.id.item_img, R.id.item_tire, R.id.item_replace };
        final SimpleAdapter adapter = new SimpleAdapter(this, list,
                R.layout.list_view_layout, from, to);

        tire_list.setAdapter(adapter);

        tire_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                Map<String, Object> map = list.get(arg2);

                String str = "";
                String str2="";
                str += map.get("tire number");
                for(int i=0;i<str.length();i++) {
                    if (str.charAt(i) >= 48 && str.charAt(i) <= 57) {
                        str2 += str.charAt(i);
                    }
                }
                int tire_num = Integer.valueOf(str2);
                calculate_location(buttonnumber,tire_num);
                vehicle_to_tire();
            }
        });
    }

    /**
<<<<<<< HEAD
     * Calculate the location of the tire according to the tire index
     *
     * @param tirenum the number of the tires of the vehicle
     * @param index the index of the tire that user selected
=======
     * Called to calculate the relative tire location
     * @param tirenum is the total tire numbers for this vehicle
     * @param index is the tire index for the chosen tire
>>>>>>> 1e04ad23abe545af951e8ce590f47d880e267e2c
     */
    private void calculate_location(int tirenum, int index){
        int side = -1;   //left-1,right-0
        if(tirenum == 4 || tirenum == 6 || tirenum == 8){
            axis_row = (index-1)/2+1;
            axis_index = 1;
            side = index%2;
        }
        if(tirenum == 10 || tirenum == 14 || tirenum == 18) {
            axis_row = (index + 1) / 4 + 1;
            if (index == 1) {
                axis_index = 1;
                side = 1;
            } else if (index == 2) {
                axis_index = 1;
                side = 0;
            } else {
                if (index % 4 == 0 || index % 4 == 1) {
                    axis_index = 1;
                } else {
                    axis_index = 2;
                }
                if (index % 4 == 0 || index % 4 == 3) {
                    side = 1;
                } else {
                    side = 0;
                }
            }
        }
        if(side == 1){
            axis_side = 'L';
        }
        else {
            axis_side = 'R';
        }
        Log.i("axis", Character.toString(axis_side));
        Log.i("index", Integer.toString(axis_index));
        Log.i("row", Integer.toString(axis_row));
    }

    /**
     * Initialize the list of the tires
     *
     * @param number the number of the tires
     * @param tires the arraylist of the info of the tires
     */
    private void initDataList(int number, ArrayList<Tire> tires) {
        int img[] = null;
        img = new int[number];
        for(int i = 0;i < number; i++) {
            img[i] = R.drawable.tire;
        }
        list = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < number; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("img", img[i]);
            map.put("tire number", "tire" + (i+1));
            calculate_location(buttonnumber,i+1);
            Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
            int vehicle_ID = Database.getVehicleID(vin);
            Tire curr_tire = Database.getTire(axis_row, axis_index, axis_side, vehicle_ID);
            Database.myDatabase.close();
            String reptime;
            if(curr_tire==null){
                reptime = "";
            }
            else{
                reptime = curr_tire.getRepTime();
            }
            map.put("replace","Replace before: "+reptime );
            list.add(map);
        }
    }

    /** Switch to the main page
     * @param view called after clicking "yes" of the dialogue (delete the vehicle)
     * Called to get back to main page
     */
    public void BackToMain(View view) {
        Intent intent = new Intent(Vehicle_Info.this, MainActivity.class);
        intent.putExtra("USER_ID", user_id);
        startActivity(intent);
    }

    /** Switch to the input vehicle info page
     * @param view called by the button "edit"
     * Called to switch to the edit page: Input_Vehicle_Info
     */
    public void switchToEdit(View view) {
        Intent intent = new Intent(Vehicle_Info.this, Input_Vehicle_Info.class);
        intent.putExtra("userID", user_id);
        intent.putExtra("VIN", vin);
        startActivity(intent);
    }

    /** Switch to the tire info page
     *   called by clicking the item in the vehicle list
     *   Called to get into the tire page: TireInfo
    */
    public void vehicle_to_tire () {
        Intent intent = new Intent(Vehicle_Info.this, TireInfo.class);
        intent.putExtra("AXIS_ROW", axis_row);
        intent.putExtra("AXIS_INDEX",axis_index);
        intent.putExtra("AXIS_SIDE", axis_side);
        intent.putExtra("VIN", vin);

        startActivity(intent);
    }

    /* Added by De Lan on 3/25/2018 */
    /**
     * Called to delete the vehicle
     */
    public void delete_vehicle(final View view){
        new AlertDialog.Builder(this)
                .setTitle("NOTIFICATION")
                .setMessage("Are you sure to delete this vehicle from your account?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
                                Database.deleteVehicle(vin);
                                Database.myDatabase.close();
                                BackToMain(view);
                            }
                        }
                        )
                .setNegativeButton("No", null)
                .show();
    }

}
