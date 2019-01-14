package edu.duke.ece651.tyrata.vehicle;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import edu.duke.ece651.tyrata.datamanagement.Database;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


/**
 * This class is a TireSnapshot object
 * used to standardize the use of tire snapshots across the app
 * Tire snapshot is the tire's data in an instant/moment as received from the sensor
 * @author Saeed Alrahma
 * Created by Saeed on 3/10/2018.
 */

public class TireSnapshot extends Tire {
    private double mS11;
    private double mOdometerMileage;
    private Calendar mTimestamp; /* @TODO DateFormat might be a better type */
    private double mPressure;

    /** Constructor
     *
     * @param tire Tire (parent object)
     * @param s11 Tire S11 measurement from sensor
     * @param odoMileage Vehicle odometer mileage
     * @param timestamp Timestamp of snapshot
     * @param pressure Tire pressure
     */
    public TireSnapshot(Tire tire, double s11, double odoMileage, Calendar timestamp, double pressure) {
        super(tire);
        this.mS11 = s11;
        this.mOdometerMileage = odoMileage;
        this.mTimestamp = timestamp;
        this.mPressure = pressure;
    }

    /** Constructor
     *
     * @param s11 Tire S11 measurement from sensor
     * @param odoMileage Vehicle odometer mileage
     * @param timestamp Timestamp of snapshot
     * @param pressure Tire pressure
     */
    public TireSnapshot(double s11, double odoMileage, Calendar timestamp, double pressure) {
        super();
        this.mS11 = s11;
        this.mOdometerMileage = odoMileage;
        this.mTimestamp = timestamp;
        this.mPressure = pressure;
    }

    /** Default constructor
     *
     */
    public TireSnapshot() {
        this(0, 0, null, 0);
    }

    /* @TODO implement processing and calcualtion methods here */
    /* Updated by Zijie and Yue on 3/24/2018. */
    public double calculateTreadThickness(double init_mS11, double init_thickness) {
        /* implement tread thickness calcualtion/formaul */
        Log.i("initial_s11", Double.toString(init_mS11));
        Log.i("current_s11", Double.toString(mS11));
        return init_thickness - 12.50 * (mS11 - init_mS11);
    }

    public double calculateEol() {
        // @TODO

        return 0;
    }


    public Calendar calculateReplaceTime() {
        // @TODO

        return null;
    }

    public boolean filterOutlier() {
        // @TODO

        return false;
    }

    public void storeSnapshot() {
        // @TODO

    }

    public void processSnapshot() {
        // @TODO

    }

    /*Create by Zijie on 3/23/2018.*/
    public void LinearRegression(String sensor_id) {
        int MAXN = 60;
        int n = 0;
        double[] y = new double[MAXN];

        // first pass: read in data, compute xbar and ybar
        double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
        Database.myDatabase = SQLiteDatabase.openOrCreateDatabase("TyrataData", null);
        double[] x = Database.getThickness(sensor_id);
        if (x != null) {
            while (x[n] != -1) {
                y[n] = n;
                sumx += x[n];
                sumx2 += x[n] * x[n];
                sumy += y[n];
                n++;
            }
            double xbar = sumx / n;
            double ybar = sumy / n;

            // second pass: compute summary statistics
            double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
            for (int i = 0; i < n; i++) {
                xxbar += (x[i] - xbar) * (x[i] - xbar);
                yybar += (y[i] - ybar) * (y[i] - ybar);
                xybar += (x[i] - xbar) * (y[i] - ybar);
            }
            double beta1 = xybar / xxbar;
            double beta0 = ybar - beta1 * xbar;
            //y = beta1 * x + beta0
            Database.myDatabase.close();
        }
    }

    /**
     * Convert/Parse String with format "2018-03-22" (or "year-month-day") to Calendar object
     * @param date String with date formatted as "2018-03-22" (or "year-month-day")
     * @return Calendar object of parsed date
     */
    public static Calendar convertStringToCalendar(String date) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        try {
            cal.setTime(sdf.parse(date));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return cal;
    }

    /**
     * Convert/Parse Calendar object to String with format "2018-03-22" (or "year-month-day")
     * @param cal Calendar object
     * @return String with date formatted as "2018-03-22" (or "year-month-day")
     */
    public static String convertCalendarToString(Calendar cal) {
        return cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.DAY_OF_MONTH);
    }

    /* Getters and Setters */
    public double getS11() {
        return mS11;
    }

    public void setS11(double mS11) {
        this.mS11 = mS11;
    }

    public double getOdometerMileage() {
        return mOdometerMileage;
    }

    public void setOdometerMileage(double mOdometerMileage) {
        this.mOdometerMileage = mOdometerMileage;
    }

    public Calendar getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(Calendar mTimestamp) {
        this.mTimestamp = mTimestamp;
    }

    public double getPressure() {
        return mPressure;
    }

    public void setPressure(double mPressure) {
        this.mPressure = mPressure;
    }
}
