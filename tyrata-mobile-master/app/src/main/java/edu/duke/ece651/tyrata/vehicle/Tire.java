package edu.duke.ece651.tyrata.vehicle;

/**
 * This class is a Tire object
 * used to standardize the use of tire across the app
 * @author Saeed Alrahma
 * Created by Saeed on 3/10/2018.
 */

/**
 * Updated by De Lan on 3/18/2018: with accordance with latest database design
 */
public class Tire {
    String mSensorId;
    String mManufacturer;
    String mModel;
    String mSku;
    int mAxisRow;
    char mAxisSide;
    int mAxisIndex;
    double minit_thickness;
    double mcurr_thickness;
    int INIT_SS_ID;
    int CUR_SS_ID;
    double mS11;
    double mOdometer;
    String meol;
    String mrepTime;



    /** Constructor
     *
     * @param manufacturer Manufacturer of the tire
     * @param model Model of the tire
     * @param sku SKU (Stock Keeping Unit) of the tire)
     * @param axisRow The axis/axle row number of the tire
     * @param axisSide The axis/axle side of the tires )('L' for left, 'R' for right)
     * @param axisIndex The index of the tire on the axis/axle (inner-most is 0)
     * s11Ref S11 measurement reference/baseline
     * odometerRef Odometer mileage reference/baseline
     * @param sensorId Paired sensor ID
     */
    public Tire(String sensorId, String manufacturer, String model, String sku, int axisRow, char axisSide, int axisIndex,
                double init_thickness, double curr_thickness, int INIT_SS, int CUR_SS, double s11, double odometer, String eol, String repTime) {
        this.mSensorId = sensorId;
        this.mManufacturer = manufacturer;
        this.mModel = model;
        this.mSku = sku;
        this.mAxisRow = axisRow;
        this.mAxisSide = axisSide;
        this.mAxisIndex = axisIndex;
        this.minit_thickness = init_thickness;
        this.mcurr_thickness = curr_thickness;
        this.INIT_SS_ID = INIT_SS;
        this.CUR_SS_ID = CUR_SS;
        this.mS11 = s11;
        this.mOdometer = odometer;
        this.meol = eol;
        this.mrepTime = repTime;
    }

    /** Constructor
     *
     * @param tire Tire object to copy/clone
     */
    public Tire(Tire tire) {
        this(tire.getSensorId(), tire.getManufacturer(), tire.getModel(), tire.getSku(), tire.getAxisRow(),
                tire.getAxisSide(), tire.getAxisIndex(), tire.get_INIT_THICK(), tire.get_CURR_THCK(), tire.get_INIT_SS(),
                tire.get_CUR_SS(),tire.getS11(),tire.getOdometer(),tire.getEOL(),tire.getRepTime());
    }

    /** Default constructor
     *
     */
    public Tire() {
        this("", "", "", "", 0, 'S', 0, 0, 0, 0, 0, 0, 0, "", "");
    }

    public void reportAccident() {
        // @TODO

    }

    public void calibrate() {
        // @TODO

    }

    /** Return the location of the tire (e.g. Front Left)
     *
     * @return String describing the location of the tire on the car
     */
    public String getTireLocation() {
        // @TODO use axis info to find location on car
        return "";
    }

    /* Getters and Setters */
    public String getManufacturer() {
        return mManufacturer;
    }

    public String getSensor() {
        return mSensorId;
    }

    public void setManufacturer(String mManufacturer) {
        this.mManufacturer = mManufacturer;
    }

    public String getModel() {
        return mModel;
    }

    public void setModel(String mModel) {
        this.mModel = mModel;
    }

    public String getSku() {
        return mSku;
    }

    public void setSku(String mSku) {
        this.mSku = mSku;
    }

    public int getAxisRow() {
        return mAxisRow;
    }

    public void setAxisRow(int mAxisRow) {
        this.mAxisRow = mAxisRow;
    }

    public char getAxisSide() {
        return mAxisSide;
    }

    public void setAxisSide(char mAxisSide) {
        this.mAxisSide = mAxisSide;
    }

    public int getAxisIndex() {
        return mAxisIndex;
    }

    public double get_INIT_THICK(){ return minit_thickness; }

    public double get_CURR_THCK() { return mcurr_thickness; }

    public void setMcurr_thickness(double mcurr_thickness){ this.mcurr_thickness = mcurr_thickness;}

    public int get_INIT_SS() {return INIT_SS_ID; }

    public int get_CUR_SS() {return CUR_SS_ID; }

    public double getS11() { return mS11;}

    public double getOdometer() { return mOdometer;}

    public String getEOL() { return meol;}
    public void setMeol(String meol){this.meol = meol;}

    public String getRepTime() { return mrepTime;}
    public void setMrepTime(String mrepTime){this.mrepTime = mrepTime;}


//    public void setAxisIndex(int mAxisIndex) {
//        this.mAxisIndex = mAxisIndex;
//    }
//    public double getS11Reference() {
//        return mS11Reference;
//    }

//    public void setS11Reference(double mS11Reference) {
//        this.mS11Reference = mS11Reference;
//    }

//    public int getOdometerMileageReference() {
//        return mOdometerMileageReference;
//    }

//    public void setOdometerMileageReference(int mOdometerMileageReference) {
//        this.mOdometerMileageReference = mOdometerMileageReference;
//    }

    public String getSensorId() {
        return mSensorId;
    }

    public void setSensorId(String mSensorId) {
        this.mSensorId = mSensorId;
    }
}
