package edu.duke.ece651.tyrata.datamanagement;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.os.Trace;
import android.support.constraint.solver.widgets.Snapshot;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;

import java.io.IOError;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import edu.duke.ece651.tyrata.R;
import edu.duke.ece651.tyrata.calibration.TireInfoInput;
import edu.duke.ece651.tyrata.communication.Trace_item;
import edu.duke.ece651.tyrata.user.User;
import edu.duke.ece651.tyrata.vehicle.Tire;
import edu.duke.ece651.tyrata.vehicle.TireSnapshot;
import edu.duke.ece651.tyrata.vehicle.Vehicle;

import static edu.duke.ece651.tyrata.vehicle.TireSnapshot.convertStringToCalendar;

/**
 * Created by Yue Li and Zijie Wang on 3/4/18.
 * Updated by De Lan on 3/18/2018: getUser(), getVehicle(), getTire()
 */

public class Database extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /* Created by Zijie Wang on 3/4/2018. */
    public static SQLiteDatabase myDatabase;

    /* Created by Zijie Wang on 4/7/2018. */
    public static double get_mean_s11(String sensor_id) {
        String sql = "SELECT * FROM SNAPSHOT, TIRE WHERE TIRE.ID = TIRE_ID and TIRE.SENSOR_ID = ? and OUTLIER != 1";
        Cursor c = myDatabase.rawQuery(sql, new String[] {sensor_id});
        if (c != null && c.moveToFirst()) {
            if(c.getCount() < 10) {
                c.close();
                return 0;
            }
            c.moveToLast();
            double sum = 0;
            int count = 0;
            do {
                count++;
                sum += c.getDouble(c.getColumnIndex("S11"));
            }while(c.moveToPrevious() && count < 10);
            c.close();
            return sum/10;
        }
        return 0;
    }

    public static double get_deviation_s11(String sensor_id) {
        String sql = "SELECT * FROM SNAPSHOT, TIRE WHERE TIRE.ID = TIRE_ID and TIRE.SENSOR_ID = ? and SNAPSHOT.OUTLIER != 1";
        Cursor c = myDatabase.rawQuery(sql, new String[] {sensor_id});
        double mean = get_mean_s11(sensor_id);
        if (c != null && c.moveToFirst()) {
            if(c.getCount() < 10) {
                c.close();
                return 0;
            }
            c.moveToLast();
            double sum = 0;
            int count = 0;
            do {
                count++;
                sum += Math.pow((c.getDouble(c.getColumnIndex("S11")) - mean), 2);
            }while(c.moveToPrevious() && count < 10);
            c.close();

            return Math.sqrt(sum/10);
        }
        return 0;
    }

    public static int get_outlier_num(String sensor_id) {
        String sql = "SELECT * FROM SNAPSHOT, TIRE WHERE TIRE.ID = TIRE_ID and TIRE.SENSOR_ID = ? and SNAPSHOT.OUTLIER = 1";
        Cursor c = myDatabase.rawQuery(sql, new String[] {sensor_id});
        int ans = c.getCount();
        c.close();
        return ans;
    }


    /* Created by Yue Li and Zijie Wang on 3/4/2018. */
    /* Updated by De Lan on 3/4/2018. */
    /**
     * Called to create the database tables: USER, VEHICLE, TIRE, SNAPSHOT, ACCIDENT, TRACE
     */
    public static void createTable() {
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS USER (ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME VARCHAR, EMAIL VARCHAR, PHONE_NUMBER VARCHAR)");
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS VEHICLE (ID INTEGER PRIMARY KEY AUTOINCREMENT, VIN VARCHAR UNIQUE, MAKE VARCHAR, MODEL VARCHAR, " +
                "YEAR INT, AXIS_NUM INT, TIRE_NUM INT, USER_ID INT, FOREIGN KEY(USER_ID) REFERENCES USER(ID))");
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS TIRE(ID INTEGER PRIMARY KEY AUTOINCREMENT, SENSOR_ID VARCHAR UNIQUE, MANUFACTURER VARCHAR, MODEL VARCHAR, " +
                "SKU VARCHAR, VEHICLE_ID INT, AXIS_ROW INT, AXIS_SIDE CHAR, AXIS_INDEX INT, INIT_THICKNESS DOUBLE, INIT_SS_ID INT, CUR_SS_ID INT, FOREIGN KEY(VEHICLE_ID) REFERENCES VEHICLE(ID) ON DELETE CASCADE)");
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS SNAPSHOT(ID INTEGER PRIMARY KEY AUTOINCREMENT, S11 DOUBLE, TIMESTAMP VARCHAR, MILEAGE DOUBLE, " +
                "PRESSURE DOUBLE, TIRE_ID INT, OUTLIER BOOL, THICKNESS DOUBLE, EOL VARCHAR, TIME_TO_REPLACEMENT VARCHAR, LONGITUDE DOUBLE, LATITUDE DOUBLE, FOREIGN KEY(TIRE_ID) REFERENCES TIRE(ID) ON DELETE CASCADE)");
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS ACCIDENT(ID INTEGER PRIMARY KEY AUTOINCREMENT, DESCRIPTION VARCHAR, USER_ID INT, " +
                "FOREIGN KEY(USER_ID)REFERENCES USER(ID) ON DELETE CASCADE)");
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS TRACE(ID INTEGER PRIMARY KEY AUTOINCREMENT, METHOD_NAME VARCHAR, TABLE_NAME VARCHAR, TARGET_ID INT, ORIGINAL_INFO VARCHAR)");
    }

    /**
     * Called to drop the database tables: USER, VEHICLE, TIRE, SNAPSHOT, ACCIDENT, TRACE
     * Only for test purpose
     */
    public static void dropAllTable() {
        myDatabase.execSQL("DROP TABLE IF EXISTS TIRE");
        myDatabase.execSQL("DROP TABLE IF EXISTS SNAPSHOT");
        myDatabase.execSQL("DROP TABLE IF EXISTS VEHICLE");
        myDatabase.execSQL("DROP TABLE IF EXISTS USER");
        myDatabase.execSQL("DROP TABLE IF EXISTS ACCIDENT");
        myDatabase.execSQL("DROP TABLE IF EXISTS TRACE");
    }

    /* Created by Yue Li and Zijie Wang on 3/4/2018. */
    /* Updated by De Lan on 3/24/2018 */
    // Updated by Cheng Xing on 4/8/2018
    /**
     * Called to save user data into table USER
     * @return true if user already exists, false when insert user succeeds
     */
    public static boolean storeUserData(String original_email, String name, String email, String phone) {
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS USER (ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME VARCHAR, EMAIL VARCHAR, PHONE_NUMBER VARCHAR)");
        String sql = "SELECT * FROM USER WHERE EMAIL = ?";
        Cursor emailCursor = myDatabase.rawQuery(sql, new String[] {email});
        ContentValues contentValues = new ContentValues();
        contentValues.put("NAME", name);
        contentValues.put("EMAIL", email);
        contentValues.put("PHONE_NUMBER", phone);

        if(emailCursor != null && emailCursor.moveToFirst()){
            Log.i("storeUser DUP, email:", email);
            emailCursor.close();
            int user_ID = getUserID(original_email);
            myDatabase.update("USER", contentValues, "ID = ?", new String[]{Integer.toString(user_ID)});
            return true;
        }
        else {
            Log.i("In database", "insert user");
            long ID = myDatabase.insert("USER", null, contentValues);
            return false;
        }
    }

    /* Updated by De Lan on 3/28/2018 */
    /**
     * Called to save vehicle data into table VEHICLE
     * @return true if update or insert succeeds, false if the VIN already exists in database
     */
    public static boolean storeVehicleData(String original_vin, int vehicle_ID, String vin, String carmake, String carmodel, int tireyear, int axisnum, int tirenum, int userid) {
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS VEHICLE (ID INTEGER PRIMARY KEY AUTOINCREMENT, VIN VARCHAR UNIQUE, MAKE VARCHAR, MODEL VARCHAR, " +
                "YEAR INT, AXIS_NUM INT, TIRE_NUM INT, USER_ID INT, FOREIGN KEY(USER_ID) REFERENCES USER(ID))");
        ContentValues contentValues = new ContentValues();
        contentValues.put("VIN", vin);
        contentValues.put("MAKE", carmake);
        contentValues.put("MODEL", carmodel);
        contentValues.put("YEAR", tireyear);
        contentValues.put("AXIS_NUM", axisnum);
        contentValues.put("TIRE_NUM", tirenum);
        // Update
        if (vehicle_ID > 0) {
            String sql = "SELECT * FROM VEHICLE WHERE VIN = ?";
            Cursor c = myDatabase.rawQuery(sql, new String[] {vin});
            if(c != null && c.moveToFirst() && vehicle_ID != c.getInt(c.getColumnIndex("ID"))){
                Log.i("In storeVehicleData","Update VIN conflict");
                c.close();
                return false;
            }
            Log.i("In database", "update vehicle");
            myDatabase.update("VEHICLE", contentValues, "ID = ?", new String[]{Integer.toString(vehicle_ID)});
            updateTrace( "update", "VEHICLE", vehicle_ID,original_vin);
        }
        // Insert
        else {
            String sql = "SELECT * FROM VEHICLE WHERE VIN = ?";
            Cursor c = myDatabase.rawQuery(sql, new String[] {vin});
            if(c != null && c.moveToFirst()){
                Log.i("In storeVehicleData","Insert VIN conflict");
                c.close();
                return false;
            }
            contentValues.put("USER_ID", userid);
            Log.i("In database", "insert vehicle");
            long ID = myDatabase.insert("VEHICLE", null, contentValues);
            updateTrace( "create", "VEHICLE", ID,"");
        }
        return true;
    }

    // Updated by Yue Li and De Lan on 3/22/2018
    /**
     * Called to save tire data into table TIRE
     * @return true if update or insert succeeds, false if the sensor_id already exists in database
     */
    public static boolean storeTireData(String original_sensor, int tire_ID, String sensor_id, String manufacturer, String model, String sku, String vin, int axis_row, String axis_side, int axis_index, double init_thickness, int init_ss_id, int cur_ss_id) {
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS TIRE(ID INTEGER PRIMARY KEY AUTOINCREMENT, SENSOR_ID VARCHAR UNIQUE, MANUFACTURER VARCHAR, MODEL VARCHAR, " +
                "SKU VARCHAR, VEHICLE_ID INT, AXIS_ROW INT, AXIS_SIDE CHAR, AXIS_INDEX INT, INIT_THICKNESS DOUBLE, INIT_SS_ID INT, CUR_SS_ID INT, FOREIGN KEY(VEHICLE_ID) REFERENCES VEHICLE(ID) ON DELETE CASCADE)");
        ContentValues contentValues = new ContentValues();
        contentValues.put("MANUFACTURER", manufacturer);
        contentValues.put("MODEL", model);
        contentValues.put("SKU", sku);
        contentValues.put("INIT_THICKNESS", init_thickness);
        contentValues.put("SENSOR_ID", sensor_id);
        // Update
        if (tire_ID > 0) {
            String sql = "SELECT * FROM TIRE WHERE SENSOR_ID = ?";
            Cursor c = myDatabase.rawQuery(sql, new String[] {sensor_id});

            if(c != null && c.moveToFirst() && tire_ID != c.getInt(c.getColumnIndex("ID"))){
                Log.i("In storeTireData","Update SENSOR_ID conflict");
                c.close();
                return false;
            }
            myDatabase.update("TIRE", contentValues, "ID = ?", new String[]{Integer.toString(tire_ID)});
            updateTrace( "update", "TIRE", tire_ID,original_sensor);
        }
        // Insert
        else {
            String sql = "SELECT * FROM TIRE WHERE SENSOR_ID = ?";
            Cursor c = myDatabase.rawQuery(sql, new String[] {sensor_id});

            if(c != null && c.moveToFirst()){
                Log.i("In storeTireData","Insert SENSOR_ID conflict");
                c.close();
                return false;
            }
            int vehicle_id = getVehicleID(vin);
            contentValues.put("VEHICLE_ID", vehicle_id);
            contentValues.put("AXIS_ROW", axis_row);
            contentValues.put("AXIS_SIDE", axis_side);
            contentValues.put("AXIS_INDEX", axis_index);
            contentValues.put("INIT_SS_ID", init_ss_id);
            contentValues.put("CUR_SS_ID", cur_ss_id);
            Log.i("In database", "insert tire");
            long ID = myDatabase.insert("TIRE", null, contentValues);
            updateTrace( "create", "TIRE", ID,"");
        }
        return true;
    }

    /* Created by Zijie Wang on 3/24/2018. */
    // Updated by Cheng Xing on 4/8/2018
    /**
     * Called to get the tire init thickness
     * @return -1 if the sensor_id does not exist in database
     */
    public static double getInitThickness(String sensor_id) {
        String sql = "SELECT * FROM TIRE WHERE SENSOR_ID = ?";
        Cursor c = myDatabase.rawQuery(sql, new String[] {sensor_id});

        if(c != null && c.moveToFirst()){
            Double ans = c.getDouble(c.getColumnIndex("INIT_THICKNESS"));
            c.close();
            return ans;
        }
        else {
            Log.i("In database", "Sensor id not found");
            return -1;
        }
    }
    /* Created by Yue Li on 3/31/2018. */
    /**
     * Called to get the tire init mileage
     * @return 0 if the sensor_id does not exist in database
     */
    public static double getInitMileage(String sensor_id){
        Cursor c = myDatabase.rawQuery("SELECT * FROM SNAPSHOT WHERE TIRE_ID = '" + sensor_id + "'", null);
        if(c != null && c.moveToFirst()){
            Double mile = c.getDouble(c.getColumnIndex("MILEAGE"));
            c.close();
            return mile;
        }
        else {
            Log.i("In database", "Sensor id not found");
            return 0;
        }
    }

    // Updated by Cheng Xing on 4/8/2018
    /**
     * Called to get the thickness list
     * @return null if the sensor_id does not exist in database
     */
    public static double[] getThickness(String sensor_id){
        String sql = "SELECT THICKNESS FROM SNAPSHOT, TIRE WHERE SNAPSHOT.TIRE_ID = TIRE.ID and TIRE.SENSOR_ID = ?";
        Cursor c = myDatabase.rawQuery(sql, new String[] {sensor_id});

        double[] x = new double[60];
        int i = 0;
        if (c.moveToFirst()) {
            do {
                x[i] = c.getDouble(c.getColumnIndex("THICKNESS"));
                i++;

            } while (c.moveToNext());
            x[i] = -1;
            c.close();
            return x;
        }
        else {
            Log.i("snapshotTable", "There is nothing in snapshotTable");
            return null;
        }
    }

    /* Added by De Lan on 04/13/2018*/
    /**
     * Called to check if the tireSnapshot exists
     * @return false if the sensor_id does not exist in database
     */
    public static boolean tireSnapshotExist(String sensor_id){
        String sql = "SELECT * FROM SNAPSHOT, TIRE WHERE SNAPSHOT.TIRE_ID = TIRE.ID and TIRE.SENSOR_ID = ?";
        Cursor c = myDatabase.rawQuery(sql, new String[] {sensor_id});
        if(c.getCount() > 0){
            c.close();
            return true;
        }
        return false;
    }

    /* Created by Zijie Wang on 3/26/2018 and updated on 4/15/2018. */
    // Updated by Cheng Xing on 4/8/2018
    /**
     * Called to get the valid thickness and respective timestamp
     */
    public static ArrayList<Pair<String, Double>> get_thickness_and_timestamp (String sensor_id) {
        String sql = "SELECT TIMESTAMP, THICKNESS FROM SNAPSHOT, TIRE WHERE SNAPSHOT.TIRE_ID =  TIRE.ID and TIRE.SENSOR_ID = ? and SNAPSHOT.OUTLIER != 1";
        Cursor c = myDatabase.rawQuery(sql, new String[] {sensor_id});

        ArrayList<Pair<String, Double>> ans = new ArrayList<>();
        if(c != null && c.moveToFirst()) {
            do {
                String timestamp = c.getString(c.getColumnIndex("TIMESTAMP"));
                Double thickness = c.getDouble(c.getColumnIndex("THICKNESS"));
                Pair<String, Double> temp = new Pair<>(timestamp, thickness);
                ans.add(temp);
            }while(c.moveToNext());
            c.close();
        }
        return ans;
    }


    // Updated by De Lan on 03/23/2018
    // Updated by Cheng Xing on 4/8/2018
    /**
     * Called to store the tire's tireSnapshot in table SNAPSHOT
     * @return false if the snapshot does already exists in database
     */
    public static boolean storeSnapshot(double s11, String timestamp, double mileage, double pressure, String sensor_id, boolean outlier, double thickness, String eol, String time_to_replacement, double longitutde, double lat) {
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS SNAPSHOT(ID INTEGER PRIMARY KEY AUTOINCREMENT, S11 DOUBLE, TIMESTAMP VARCHAR, MILEAGE DOUBLE, " +
                "PRESSURE DOUBLE, TIRE_ID INT, OUTLIER BOOL, THICKNESS DOUBLE, EOL VARCHAR, TIME_TO_REPLACEMENT VARCHAR, LONGITUDE DOUBLE, LATITUDE DOUBLE, FOREIGN KEY(TIRE_ID) REFERENCES TIRE(ID) ON DELETE CASCADE)");
        // avoid duplication
        String sql = "SELECT * FROM SNAPSHOT, TIRE WHERE TIMESTAMP = ? and SNAPSHOT.TIRE_ID = TIRE.ID and TIRE.SENSOR_ID = ?";
        Cursor c = myDatabase.rawQuery(sql, new String[] {timestamp, sensor_id});

        if(c != null && c.moveToFirst()){
            Log.i("Snapshot duplication", "DUP!!!");
            c.close();
            return false;
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put("S11", s11);
        contentValues.put("TIMESTAMP", timestamp);
        contentValues.put("MILEAGE", mileage);
        contentValues.put("PRESSURE", pressure);
        int tire_id = getTireID(sensor_id);
        contentValues.put("TIRE_ID", tire_id);
        contentValues.put("OUTLIER", outlier);
        contentValues.put("THICKNESS", thickness);
        contentValues.put("EOL", eol);
        contentValues.put("TIME_TO_REPLACEMENT", time_to_replacement);
        contentValues.put("LONGITUDE", longitutde);
        contentValues.put("LATITUDE", lat);
        Log.i("Snapshot Insertion", "New insert!!!");
        long ID = myDatabase.insert("SNAPSHOT", null, contentValues);
        updateTrace( "create", "SNAPSHOT", ID,"");
        return true;
    }

    /*Created by Yue Li on 04/05/2018*/
    public static String notification(String sensor_id){
        Cursor c = myDatabase.rawQuery("SELECT * FROM SNAPSHOT WHERE SNAPSHOT.TIRE_ID = TIRE.ID and TIRE.SENSOR_ID = '"+sensor_id+"'", null);
        c.moveToLast();
        String timestamp = c.getString(c.getColumnIndex("TIMESTAMP"));
        Calendar cal = TireSnapshot.convertStringToCalendar(timestamp);
        Calendar today = Calendar.getInstance();
        SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd");
        long start = cal.getTimeInMillis();
        long end = today.getTimeInMillis();
        long res = TimeUnit.MILLISECONDS.toDays(Math.abs(end-start));
        c.close();
        if(res>30){
            String notification = "Disconnect Exceed 30 Days!";
            return notification;
        }
        return "";
    }

    /**
     * Called to store the user accident in table ACCIDENT
     */
    public static void storeAccident(String record, int userid) {
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS ACCIDENT(ID INTEGER PRIMARY KEY AUTOINCREMENT, DESCRIPTION VARCHAR, USER_ID INT, " +
                "FOREIGN KEY(USER_ID)REFERENCES USER(ID) ON DELETE CASCADE)");
        ContentValues contentValues = new ContentValues();
        contentValues.put("DESCRIPTION", record);
        contentValues.put("USER_ID", userid);
        long ID = myDatabase.insert("ACCIDENT", null, contentValues);
        updateTrace( "create", "ACCIDENT", ID,"");
    }

    // Added by De Lan on 03/23/2018
    // Updated by Cheng Xing on 4/8/2018
    /**
     * Called to update the tire's curr_SSID and init_SSID in table TIRE
     * @return false if the sensor_ID does not exist in database
     */
    public static boolean updateTireSSID(String sensor_ID){
        String sql_curr = "SELECT MAX(SNAPSHOT.ID) FROM SNAPSHOT, TIRE WHERE SNAPSHOT.TIRE_ID = TIRE.ID and TIRE.SENSOR_ID = ?";
        String sql_init = "SELECT MIN(SNAPSHOT.ID) FROM SNAPSHOT, TIRE WHERE SNAPSHOT.TIRE_ID = TIRE.ID and TIRE.SENSOR_ID = ?";
        Cursor curr = myDatabase.rawQuery(sql_curr, new String[] {sensor_ID});
        Cursor init = myDatabase.rawQuery(sql_init, new String[] {sensor_ID});
        ContentValues contentValues = new ContentValues();
        if(curr != null && curr.moveToFirst() && init != null && init.moveToFirst()){
            int curr_ss_id = curr.getInt(0);
            int init_ss_id = init.getInt(0);
            contentValues.put("INIT_SS_ID", init_ss_id);
            contentValues.put("CUR_SS_ID", curr_ss_id);
            myDatabase.update("TIRE", contentValues, "SENSOR_ID = ?", new String[]{sensor_ID});
            curr.close();
            init.close();
            Log.i("In updateTireSSID", sensor_ID+"The sensor_ID is updated in SNAPSHOT!");
            return true;
        }
        else{
            Log.i("In updateTireSSID", sensor_ID+"The sensor_ID is not found in SNAPSHOT!");
            return false;
        }
    }

    /* Added by De Lan on 3/28/2018 */
    /**
     * Called to update the table TRACE, for syncing data with the server database
     */
    private static void updateTrace( String methodName, String tableName, long ID, String originalInfo){
        ContentValues traceValues = new ContentValues();
        traceValues.put("METHOD_NAME", methodName);
        traceValues.put("TABLE_NAME", tableName);
        traceValues.put("TARGET_ID", ID);
        traceValues.put("ORIGINAL_INFO", originalInfo);
        myDatabase.insertOrThrow("TRACE", null, traceValues);
    }

    /* Updated by Yue Li and De Lan on 3/24/2018 */
    // Updated by Cheng Xing on 4/8/2018
    /**
     * Called to get the user ID
     */
    public static int getUserID(String email) {
        myDatabase.execSQL("CREATE TABLE IF NOT EXISTS USER (ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME VARCHAR, EMAIL VARCHAR, PHONE_NUMBER VARCHAR)");
        String sql = "SELECT ID FROM USER WHERE EMAIL = ?";
        Cursor c = myDatabase.rawQuery(sql, new String[] {email});

        int res = -1;
        if(c != null && c.moveToFirst()) {
            res = c.getInt(c.getColumnIndex("ID"));
            c.close();
        }
        return res;
    }

    // Updated by Cheng Xing on 4/8/2018
    /**
     * Called to get the vehicle ID
     */
    public static int getVehicleID(String vin){
        String sql = "SELECT * FROM VEHICLE WHERE VIN = ?";
        Cursor c = myDatabase.rawQuery(sql, new String[] {vin});

        int res = -1;
        if(c != null && c.moveToFirst()) {
            res = c.getInt(c.getColumnIndex("ID"));
            c.close();
        }
        return res;
    }

    // Updated by Cheng Xing on 4/8/2018
    /**
     * Called to get the tire ID with sensor_id
     */
    public static int getTireID(String sensor_id){
        String sql = "SELECT * FROM TIRE WHERE SENSOR_ID = ?";
        Cursor c = myDatabase.rawQuery(sql, new String[] {sensor_id});

        int res = -1;
        if(c != null && c.moveToFirst()) {
            res = c.getInt(c.getColumnIndex("ID"));
            c.close();
        }
        return res;
    }

    /**
     * Called to get the vehicle VIN with vehicle_id
     */
    public static String getVin(int vehicle_id){
        Cursor c = myDatabase.rawQuery("SELECT VIN FROM VEHICLE WHERE ID = '"+ vehicle_id+"'",null);
        String vin = "";
        if(c != null && c.moveToFirst()) {
            vin = c.getString(c.getColumnIndex("VIN"));
            c.close();
        }
        return vin;
    }

    /**
     * Called to get the VIN with tire_id
     */
    public static String getVinFromTire(int tire_id){
        Cursor c = myDatabase.rawQuery("SELECT VIN FROM VEHICLE, TIRE WHERE TIRE.ID = '"+ tire_id+"' and TIRE.VEHICLE_ID = VEHICLE.ID",null);
        String vin = "";
        if(c != null && c.moveToFirst()) {
            vin = c.getString(c.getColumnIndex("VIN"));
            c.close();
        }
        return vin;
    }

    /**
     * Called to get the user's email with tire_id
     */
    public static String getEmail(int vehicle_id){
        Cursor c = myDatabase.rawQuery("SELECT EMAIL FROM USER, VEHICLE WHERE VEHICLE.ID = '"+ vehicle_id+"' and VEHICLE.USER_ID = USER.ID",null);
        String email = "";
        if(c != null && c.moveToFirst()) {
            email = c.getString(c.getColumnIndex("EMAIL"));
            c.close();
        }
        return email;
    }

    /**
     * Called to get the user's email with accident_id
     */
    public static String getEmailFromAccident(int accident_id){
        Cursor c = myDatabase.rawQuery("SELECT EMAIL FROM USER, ACCIDENT WHERE ACCIDENT.ID = '"+ accident_id+"' and ACCIDENT.USER_ID = USER.ID",null);
        String email = "";
        if(c != null && c.moveToFirst()) {
            email = c.getString(c.getColumnIndex("EMAIL"));
            c.close();
        }
        return email;
    }

    /**
     * Called to get the tire with ss_id
     */
    public static String getTireFromSSid(int ss_id){
        Cursor c = myDatabase.rawQuery("SELECT SENSOR_ID FROM TIRE, SNAPSHOT WHERE SNAPSHOT.ID = '"+ ss_id+"' and SNAPSHOT.TIRE_ID = TIRE.ID",null);
        String sensor_id = "";
        if(c != null && c.moveToFirst()) {
            sensor_id = c.getString(c.getColumnIndex("SENSOR_ID"));
            c.close();
        }
        return sensor_id;
    }


    /* Created by De Lan on 3/18/2018.*/
    // Updated by Cheng Xing on 4/8/2018
    /**
     * Called to get User with user_id
     */
    public static User getUser(int user_id) {
        String sql = "SELECT * FROM USER WHERE ID = ?";
        Cursor c = myDatabase.rawQuery(sql, new String[] {String.valueOf(user_id)});
        if (!c.moveToFirst()) {
            return null;
        }
        c.moveToFirst();
        String name = c.getString(c.getColumnIndex("NAME"));
        String email = c.getString(c.getColumnIndex("EMAIL"));
        String phonenum = c.getString(c.getColumnIndex("PHONE_NUMBER"));
        User curr_user = new User(name, email, phonenum);
        c.close();
        sql = "SELECT * FROM VEHICLE WHERE USER_ID = ?";
        Cursor vehicle_cursor = myDatabase.rawQuery(sql, new String[] {String.valueOf(user_id)});

        if (vehicle_cursor.moveToFirst()) {
            do {
                String vin = vehicle_cursor.getString(vehicle_cursor.getColumnIndex("VIN"));
                String make = vehicle_cursor.getString(vehicle_cursor.getColumnIndex("MAKE"));
                String model = vehicle_cursor.getString(vehicle_cursor.getColumnIndex("MODEL"));
                int year = vehicle_cursor.getInt(vehicle_cursor.getColumnIndex("YEAR"));
                int axis_num = vehicle_cursor.getInt(vehicle_cursor.getColumnIndex("AXIS_NUM"));
                int tire_num = vehicle_cursor.getInt(vehicle_cursor.getColumnIndex("TIRE_NUM"));
                Vehicle curr_vehicle = new Vehicle(vin, make, model, year, axis_num, tire_num);
                curr_user.mVehicles.add(curr_vehicle);
            } while (vehicle_cursor.moveToNext());
        }
        vehicle_cursor.close();
        return curr_user;
    }

    /* Created by De Lan on 3/18/2018.*/
    // Updated by Cheng Xing on 4/8/2018
    /**
     * Called to get Vehicle with vin
     */
    public static Vehicle getVehicle(String vin) {
        String sql = "SELECT * FROM VEHICLE WHERE VIN = ?";
        Cursor c = myDatabase.rawQuery(sql, new String[] {vin});
        if (c == null) {
            return null;
        }
        c.moveToFirst();
        String make = c.getString(c.getColumnIndex("MAKE"));
        String model = c.getString(c.getColumnIndex("MODEL"));
        int year = c.getInt(c.getColumnIndex("YEAR"));
        int axis_num = c.getInt(c.getColumnIndex("AXIS_NUM"));
        int tire_num = c.getInt(c.getColumnIndex("TIRE_NUM"));
        Vehicle curr_vehicle = new Vehicle(vin, make, model, year, axis_num, tire_num);
        c.close();

        sql = "SELECT * FROM TIRE, VEHICLE WHERE TIRE.VEHICLE_ID = VEHICLE.ID and VEHICLE.VIN = ?";
        Cursor tire_cursor = myDatabase.rawQuery(sql, new String[] {vin});

        if (tire_cursor.moveToFirst()) {
            do {
                Tire curr_tire = tireHelper(tire_cursor);
                curr_vehicle.mTires.add(curr_tire);
            } while (tire_cursor.moveToNext());
        }
        tire_cursor.close();
        return curr_vehicle;
    }

    /**
     * Called to get all the trace data
     */
    public static Trace_item getTrace(){
        Trace_item trace_item = new Trace_item();
        Cursor c = myDatabase.rawQuery("SELECT * FROM TRACE WHERE ID = (SELECT min(ID) FROM TRACE)",null);
        if(c == null|| !c.moveToFirst()){
            return null;
        }
        Log.i("database_trace",String.valueOf(c.getInt(c.getColumnIndex("ID"))));
        trace_item.setId(c.getInt(c.getColumnIndex("ID")));
        trace_item.setMethod(c.getString(c.getColumnIndex("METHOD_NAME")));
        trace_item.setTable_name(c.getString(c.getColumnIndex("TABLE_NAME")));
        trace_item.setTarget_id(c.getInt(c.getColumnIndex("TARGET_ID")));
        trace_item.setOrigin_info(c.getString(c.getColumnIndex("ORIGINAL_INFO")));
        //trace_list.add(trace_item);
        c.close();
        return trace_item;
    }

    /* Created by De Lan on 3/18/2018.*/
    // Updated by Cheng Xing on 4/8/2018
    /**
     * Helper function to get Tire
     */
    public static Tire tireHelper(Cursor c){
        String t_sensorId = c.getString(c.getColumnIndex("SENSOR_ID"));
        String t_manufacturer = c.getString(c.getColumnIndex("MANUFACTURER"));
        String t_model = c.getString(c.getColumnIndex("MODEL"));
        String t_sku = c.getString(c.getColumnIndex("SKU"));
        int t_row = c.getInt(c.getColumnIndex("AXIS_ROW"));
        char t_side = c.getString(c.getColumnIndex("AXIS_SIDE")).charAt(0);
        int t_index = c.getInt(c.getColumnIndex("AXIS_INDEX"));
        double t_init_thickness = c.getDouble(c.getColumnIndex("INIT_THICKNESS"));
        int t_init = c.getInt(c.getColumnIndex("INIT_SS_ID"));
        int t_curr = c.getInt(c.getColumnIndex("CUR_SS_ID"));
        double s11 = 0;
        double curr_thickness = c.getDouble(c.getColumnIndex("INIT_THICKNESS"));
        double odometer = 0;
        String eol = "Default";
        String repTime = "Default";
        String sql = "SELECT * FROM SNAPSHOT WHERE ID = ?";
        Cursor curr_snap = myDatabase.rawQuery(sql, new String[] {String.valueOf(t_curr)});
        if(curr_snap.moveToFirst()){
            s11 = curr_snap.getDouble(1);
            odometer = curr_snap.getDouble(3);
            curr_thickness = curr_snap.getDouble(7);
            eol = curr_snap.getString(8);
            repTime = curr_snap.getString(9);
            curr_snap.close();
        }
        Tire curr_tire = new Tire(t_sensorId, t_manufacturer, t_model, t_sku, t_row,
                t_side, t_index, t_init_thickness, curr_thickness, t_init, t_curr, s11, odometer, eol, repTime);
        return curr_tire;
    }



    /* Updated by De Lan on 3/24/2018 */
    // Updated by Cheng Xing on 4/8/2018
    /**
     * Called to get Tire
     * @return null if the tire is not found
     */
    public static Tire getTire(int axis_row, int axis_index, char axis_side, int vehicle_ID) {
        String sql = "SELECT * FROM TIRE WHERE VEHICLE_ID = ? and AXIS_ROW = ? and AXIS_INDEX = ? and AXIS_SIDE = ?";
        Cursor c = myDatabase.rawQuery(sql, new String[] {String.valueOf(vehicle_ID), String.valueOf(axis_row), String.valueOf(axis_index), String.valueOf(axis_side)});
        if (c.moveToFirst()) {
            Tire ans = tireHelper(c);
            c.close();
            return ans;
        } else {
            Log.i("getTire", "the tire not found!!!");
            return null;
        }
    }

    /**
     * Called to get Tire with tire_id
     * @return null if the tire is not found
     */
    public static Tire getTire(int id){
        Cursor c = myDatabase.rawQuery("SELECT * FROM TIRE WHERE ID = '"+ id +"'", null);
        if (c.moveToFirst()) {
            Tire ans = tireHelper(c);
            c.close();
            return ans;
        } else {
            Log.i("getTire", "the tire not found!!!");
            return null;
        }
    }

    /**
     * Called to get Snapshot with ss_id
     * @return null if the Snapshot is not found
     */
    public static TireSnapshot getSnapshot(int ss_id){
        Cursor c = myDatabase.rawQuery("SELECT * FROM SNAPSHOT WHERE ID = '"+ ss_id +"'",null);
        if (c.moveToFirst()) {
            TireSnapshot ans = new TireSnapshot();
            ans.setS11(c.getDouble(c.getColumnIndex("S11")));
            ans.setTimestamp(convertStringToCalendar(c.getString(c.getColumnIndex("TIMESTAMP"))));
            ans.setOdometerMileage(c.getDouble(c.getColumnIndex("MILEAGE")));
            ans.setPressure(c.getDouble(c.getColumnIndex("PRESSURE")));
            ans.setMcurr_thickness(c.getDouble(c.getColumnIndex("THICKNESS")));
            ans.setMeol(c.getString(c.getColumnIndex("EOL")));
            ans.setMrepTime(c.getString(c.getColumnIndex("TIME_TO_REPLACEMENT")));
            c.close();
            return ans;
        } else {
            Log.i("getSnapshot", "the snapshot not found!!!");
            return null;
        }
    }

    /**
     * Called to get Accident with accident_id
     * @return null if the Snapshot is not found
     */
    public static String getAccident(int accident_id){
        Cursor c = myDatabase.rawQuery("SELECT DESCRIPTION FROM ACCIDENT WHERE ID = '"+ accident_id +"'",null);
        if(c.moveToNext()){
            String description = c.getString(c.getColumnIndex("DESCRIPTION"));
            c.close();
            return description;
        }else {
            Log.i("getAccident", "the accident not found!!!");
            return null;
        }
    }

    /**
     * Called to delete a Vehicle with vin
     */
    public static void deleteVehicle(String vin){
        updateTrace( "delete", "VEHICLE", 0, vin);
        String sql = "DELETE FROM VEHICLE WHERE VIN = ?";
        SQLiteStatement s = myDatabase.compileStatement(sql);
        s.bindString(1, vin);
        myDatabase.execSQL("PRAGMA foreign_keys = on;");
        s.executeUpdateDelete();
    }


    //Created by Cheng Xing on 3/25/2018, updated by De Lan.
    /**
     * Called to delete a Tire with sensor_ID
     */
    public static void deleteTire(String sensor_ID ) {
        updateTrace( "delete", "TIRE", 0, sensor_ID);
        String del = "DELETE FROM TIRE WHERE SENSOR_ID = ?";
        SQLiteStatement s = myDatabase.compileStatement(del);
        s.bindString(1, sensor_ID);
        myDatabase.execSQL("PRAGMA foreign_keys = on;");
        s.executeUpdateDelete();
    }

    /**
     * Called to delete a Trace with ss_ID
     */
    public static void deleteTrace(int ID){
        String del = "DELETE FROM TRACE WHERE ID = '" + ID + "'";
        myDatabase.execSQL("PRAGMA foreign_keys = on;");
        myDatabase.execSQL(del);
    }

    /* Created by Ming Yang*/
    public static void deleteNewestTrace(){
        String del = "DELETE FROM TRACE WHERE ID = (SELECT MAX(ID) FROM TRACE)";
        myDatabase.execSQL("PRAGMA foreign_keys = on;");
        myDatabase.execSQL(del);
    }

    /* Created by YUE LI on 3/18/2018.*/
    /* Created by ZIJIE WANG on 3/18/2018.*/
    /**
     * Called to display User table
     */
    public static void testUserTable() {
        Cursor c = myDatabase.rawQuery("SELECT * FROM USER", null);
        if (c.moveToFirst()) {
            do {
                Log.i("USER_ID", c.getString(0));
                Log.i("name", c.getString(1));
                Log.i("email", c.getString(2));
                Log.i("phone", c.getString(3));
            } while (c.moveToNext());
        } else {
            Log.i("testUserTable", "There is nothing in testUserTable");
        }
        c.close();
    }

    /* Created by De Lan on 3/25/2018.*/
    /**
     * Called to display Vehicle table
     */
    public static void testVehicleTable() {
        Cursor c = myDatabase.rawQuery("SELECT * FROM VEHICLE", null);
        if (c.moveToFirst()) {
            do {
                Log.i("VEHICLE_ID", c.getString(0));
                Log.i("vin", c.getString(1));
                Log.i("make", c.getString(2));
                Log.i("model", c.getString(3));
                Log.i("year", c.getString(4));
                Log.i("axis_num", c.getString(5));
                Log.i("tire_num", c.getString(6));
                Log.i("user_id", c.getString(7));
            } while (c.moveToNext());
        } else {
            Log.i("testVehicleTable", "There is nothing in testVehicleTable");
        }
        c.close();
    }


    /* Created by De Lan on 3/18/2018.*/
    /**
     * Called to display Tire table
     */
    public static void testTireTable() {
        Cursor c = myDatabase.rawQuery("SELECT * FROM TIRE", null);
        if (c.moveToFirst()) {
            do {
                Log.i("TIRE_ID", c.getString(0));
                Log.i("sensor_id", c.getString(1));
                Log.i("manufacturer", c.getString(2));
                Log.i("model", c.getString(3));
                Log.i("sku", c.getString(4));
                Log.i("vehicle_id", c.getString(5));
                Log.i("axis_row", c.getString(6));
                Log.i("axis_side", c.getString(7));
                Log.i("axis_index", c.getString(8));
                Log.i("init thickness", c.getString(9));
                Log.i("init ss ID", c.getString(10));
                Log.i("curr ss ID", c.getString(11));
            } while (c.moveToNext());
        } else {
            Log.i("testTireTable", "There is nothing in testTireTable");
        }
        c.close();
    }

    /* Created by De Lan on 3/23/2018.*/
    /**
     * Called to display Snapshot table
     */
    public static void testSnapTable() {
        Cursor c = myDatabase.rawQuery("SELECT * FROM SNAPSHOT", null);
        if (c.moveToFirst()) {
            do {
                Log.i("SNAPSHOT_id", c.getString(0));
                Log.i("s11", c.getString(1));
                Log.i("timestamp", c.getString(2));
                Log.i("mileage", c.getString(3));
                Log.i("pressure", c.getString(4));
                Log.i("tire_id", c.getString(5));
                Log.i("outlier", c.getString(6));
                Log.i("thickness", c.getString(7));
                Log.i("eol", c.getString(8));
                Log.i("time_to_replacement", c.getString(9));
                Log.i("longitutde", c.getString(10));
                Log.i("latitude", c.getString(11));
            } while (c.moveToNext());
        } else {
            Log.i("testSnapTable", "There is nothing in testSnapTable");
        }
        c.close();
    }

    /* Created by De Lan on 3/28/2018.*/
    /**
     * Called to display Trace table
     */
    public static void testTraceTable() {
        Cursor c = myDatabase.rawQuery("SELECT * FROM TRACE", null);
        if (c.moveToFirst()) {
            do {
                Log.i("TRACE_ID", c.getString(0));
                Log.i("METHOD_NAME", c.getString(1));
                Log.i("TABLE_NAME", c.getString(2));
                Log.i("TARGET_ID", c.getString(3));
                Log.i("DELETE_INFO", c.getString(4));
            } while (c.moveToNext());
        } else {
            Log.i("testSnapTable", "There is nothing in testTraceTable");
        }
        c.close();
    }
}

