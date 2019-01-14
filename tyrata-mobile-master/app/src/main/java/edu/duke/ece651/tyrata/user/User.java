package edu.duke.ece651.tyrata.user;

import java.util.ArrayList;

import edu.duke.ece651.tyrata.vehicle.Vehicle;

/**
 * Created by Saeed on 3/10/2018.
 */

public class User {
    public String username;
    public String email;
    public String phone;
    public ArrayList<Vehicle> mVehicles;

    /** Constructor
     *
     * @param name User  name
     * @param email User email
     * @param number User phone number
     */
    public User(String name, String email, String number) {
        this.username = name;
        this.email = email;
        this.phone = number;
        /* @TODO include the rest of the attributes here */
        this.mVehicles = new ArrayList<>();
    }

    /** Default constructor
     *
     */
    public User() {
        this("", "", "");
    }

    /** Add new vehicle to user
     *
     * @param vehicle The vehicle to add
     */
    public void addVehicle(Vehicle vehicle) {
        mVehicles.add(vehicle);
    }

    /** Remove vehicle from user
     *
     * @param vehicle The vehicle to remove
     * @return true if vehicle removed (list modified), otherwise false
     */
    public boolean removeVehicle(Vehicle vehicle) {
        return mVehicles.remove(vehicle);
    }

    public boolean register() {
        // @TODO

        return false;
    }

    public boolean login() {
        // @TODO

        return false;
    }

    public boolean logout() {
        // @TODO

        return false;
    }

    public boolean changePassword() {
        // @TODO

        return false;
    }

    public void sendFeedback() {
        // @TODO

    }

    public void pushNotification() {
        // @TODO

    }
}
