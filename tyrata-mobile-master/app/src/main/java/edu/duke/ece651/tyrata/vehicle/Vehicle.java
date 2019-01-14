package edu.duke.ece651.tyrata.vehicle;

import java.util.ArrayList;

/**
 * This class is a Vehicle object
 * used to standardize the use of vehicle across the app
 * @author Saeed Alrahma
 * Created by Saeed on 3/10/2018.
 */

public class Vehicle {
    private String mMake;
    private String mModel;
    private int mYear;
    private String mVin;
    private int mNumAxis;
    private int mNumTires;
    public ArrayList<Tire> mTires;

    /** Constructor
     *
     * @param make Make of the car
     * @param model Model of the car
     * @param year The year the car was produced
     * @param vin VIN (Vehicle Identification Number) of the car
     */
    public Vehicle(String vin, String make, String model, int year, int numAxis, int numTires) {
        this.mMake = make;
        this.mModel = model;
        this.mYear = year;
        this.mVin = vin;
        this.mNumAxis = numAxis;
        this.mNumTires = numTires;
        this.mTires = new ArrayList<>();
    }

    /** Default constructor
     *
     */
    public Vehicle() {
        this("", "", "", 0, 0, 0);
    }

    /** Add a new tire to the car
     *
     * @param tire The tire to add
     */
    public void addTire(Tire tire) {
        this.mTires.add(tire);
    }

    /** Remove a tire from the car
     *
     * @param tire The tire to remove
     * @return true if tire removed (list modified), otherwise false
     */
    public boolean removeTire(Tire tire) {
        return this.mTires.remove(tire);
    }


    public void reportAccident() {
        // @TODO

    }

    public void calibrate() {
        // @TODO

    }

    /* Getters and Setters */
    public String getMake() {
        return mMake;
    }

    public void setMake(String mMake) {
        this.mMake = mMake;
    }

    public String getModel() {
        return mModel;
    }

    public void setModel(String mModel) {
        this.mModel = mModel;
    }

    public int getYear() {
        return mYear;
    }

    public void setYear(int mYear) {
        this.mYear = mYear;
    }

    public String getVin() {
        return mVin;
    }

    public void setVin(String mVin) {
        this.mVin = mVin;
    }

    public int getNumTires() {
        return mNumTires;
    }

    public void setNumTires(int mNumTires) {
        this.mNumTires = mNumTires;
    }

    public int getNumAxis() {
        return mNumAxis;
    }

    public void setNumAxis(int mNumAxis) {
        this.mNumAxis = mNumAxis;
    }

    public ArrayList<Tire> getTires() {
        return mTires;
    }

    public void setTires(ArrayList<Tire> mTires) {
        this.mTires = mTires;
    }
}
