package edu.duke.ece651.tyrata.communication;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

import edu.duke.ece651.tyrata.datamanagement.Database;

/*Created by Ming Yang on 2018/3/24
* used to parse the message from the cloud database
*/

public class ServerXmlParser extends AppCompatActivity {
    private static final String ns = null;

    /**read in the message from the server
     *
     * @param in The message from the cloud database
     * @param context The context of the current page
     */
    public void parse_server(InputStream in, Context context) throws XmlPullParserException, IOException {
        parse(in, context);
    }

    private void parse(InputStream in, Context context) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            readFeed(parser, context);
        } finally {
            in.close();
        }
    }

    /**Process the message from the server
     *
     * @param parser transferred message
     * @param context The context of the current page
     */
    private void readFeed(XmlPullParser parser, Context context)throws XmlPullParserException, IOException{
        parser.require(XmlPullParser.START_TAG, ns, "message");
        String message;
        while (parser.next() != XmlPullParser.END_TAG){
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "download":
                    readDownload(parser, context);
                    break;
                case "ack":
                    int id = Integer.valueOf(readcontent(parser, "ack"));
                    if (id > 0) {
                        Database.myDatabase = context.openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
                        Database.deleteTrace(id);
                        Database.myDatabase.close();
                    }
                    break;
                case "error":
                    message = readcontent(parser,"error");
                    String[] temp = message.split(":");
                    int id_error = Integer.valueOf(temp[0]);
                    if (id_error > 0) {
                        Database.myDatabase = context.openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
                        Database.deleteTrace(id_error);
                        Database.myDatabase.close();
                    }
                    Toast.makeText(context, temp[1], Toast.LENGTH_SHORT).show();
                    break;
                case "authentication":
                    readAuthentication(parser, context);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
    }

    private void readAuthentication(XmlPullParser parser, Context context) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "authentication");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            SharedPreferences.Editor editor= getSharedPreferences("msg_from_server",MODE_PRIVATE).edit();
            String name = parser.getName();
            switch (name) {
                case "salt":
                    String salt = readcontent(parser, "salt");
                    editor.putString("salt",salt);
                    Toast.makeText(context,salt,Toast.LENGTH_SHORT).show();
                    editor.commit();
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

    }

    private void readDownload(XmlPullParser parser, Context context) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "download");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "user":
                    readuser(parser, context);
                    break;
                case "vehicle":
                    readvehicle(parser, context);
                    break;
                case "tire":
                    readtire(parser, context);
                    break;
                case "snapshot":
                    readsnapshot(parser, context);
                    break;
                default:
                    skip(parser);
                    break;
            }
        }

    }

    private void readuser(XmlPullParser parser,Context context) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "user");
        String username="";
        String email="";
        String phone="";

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "name":
                    username = readcontent(parser,"name");
                    break;
                case "email":
                    email = readcontent(parser,"email");
                    break;
                case "phone_num":
                    phone = readcontent(parser,"phone_num");
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        Database.myDatabase = context.openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
        Database.createTable();
        Database.storeUserData("",username, email, phone);
        Database.testUserTable();
        Database.deleteNewestTrace();
        Database.myDatabase.close();
    }



    private void readvehicle(XmlPullParser parser, Context context) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "vehicle");
        String make = "";
        String model = "";
        Integer year = 0;
        String vin = "";
        Integer axis_num = 0;
        Integer tire_num = 0;
        String email = "";

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "make":
                    make = readcontent(parser,"make");
                    Log.i("parse_vehicle_make",make);
                    break;
                case "model":
                    model = readcontent(parser,"model");
                    Log.i("parse_vehicle_model",model);
                    break;
                case "year":
                    year = Integer.valueOf(readcontent(parser,"year"));
                    Log.i("parse_vehicle_year",String.valueOf(year));
                    break;
                case "vin":
                    vin = readcontent(parser,"vin");
                    Log.i("parse_vehicle_vin",vin);
                    break;
                case "tire_num":
                    tire_num = Integer.valueOf(readcontent(parser,"tire_num"));
                    break;
                case "axis_num":
                    axis_num = Integer.valueOf(readcontent(parser,"axis_num"));
                    break;
                case "email":
                    email = readcontent(parser,"email");
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        Database.myDatabase = context.openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
        int user_id  = Database.getUserID(email);
        Database.storeVehicleData("",-1, vin, make, model, year, axis_num, tire_num, user_id);
        Database.deleteNewestTrace();
        Database.myDatabase.close();
    }

    private void readtire(XmlPullParser parser,Context context) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "tire");
        String sensorId = "";
        String manufacturer = "";
        String model = "";
        String sku = "";
        String vin = "";
        int axisRow = 0;
        String axisSide = "";
        int axisIndex = 0;
        double init_thickness = 0;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "sensorid":
                    sensorId = readcontent(parser,"sensorid");
                    break;
                case "manufacturer":
                    manufacturer = readcontent(parser,"manufacturer");
                    break;
                case "model":
                    model = readcontent(parser,"model");
                    break;
                case "sku":
                    sku = readcontent(parser,"sku");
                    break;
                case "vin":
                    vin = readcontent(parser,"vin");
                    break;
                case "axisrow":
                    axisRow = Integer.valueOf(readcontent(parser,"axisrow"));
                    break;
                case "axisside":
                    axisSide = readcontent(parser,"axisside");
                    break;
                case "axisindex":
                    axisIndex = Integer.valueOf(readcontent(parser,"axisindex"));
                    break;
                case "initthickness":
                    init_thickness = Double.valueOf(readcontent(parser,"initthickness"));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        Database.myDatabase = context.openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
        Database.storeTireData("",-1, sensorId, manufacturer, model, sku, vin, axisRow, axisSide, axisIndex, init_thickness, 0, 0);
        Database.deleteNewestTrace();
        Database.myDatabase.close();

    }

    private void readsnapshot(XmlPullParser parser,Context context) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "snapshot");
        Double s11 = 0.0;
        String timestamp = "";
        Double mileage = 0.0;
        Double pressure = 0.0;
        String sensor_id = "";
        Double thickness = 0.0;
        String eol = "";
        String time_to_replacement = "";
        Double longitutde = 0.0;
        Double lat = 0.0;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "s11":
                    s11 = Double.valueOf(readcontent(parser,"s11"));
                    break;
                case "timestamp":
                    timestamp = readcontent(parser,"timestamp");
                    break;
                case "mileage":
                    mileage = Double.valueOf(readcontent(parser,"mileage"));
                    break;
                case "pressure":
                    pressure = Double.valueOf(readcontent(parser,"pressure"));
                    break;
                case "sensorid":
                    sensor_id = readcontent(parser,"sensorid");
                    break;
                case "thickness":
                    thickness = Double.valueOf(readcontent(parser,"thickness"));
                    break;
                case "eol":
                    eol = readcontent(parser,"eol");
                    break;
                case "replacetime":
                    time_to_replacement = readcontent(parser,"replacetime");
                    break;
                case "long":
                    longitutde = Double.valueOf(readcontent(parser,"long"));
                    break;
                case "lat":
                    lat = Double.valueOf(readcontent(parser,"lat"));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        Database.myDatabase = context.openOrCreateDatabase("TyrataData", 0, null);
        Database.storeSnapshot(s11, timestamp, mileage, pressure, sensor_id, false, thickness, eol, time_to_replacement, longitutde, lat);
        Database.deleteNewestTrace();
        Database.myDatabase.close();

    }

    private String readcontent(XmlPullParser parser,String label) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, label);
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, ns, label);
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

}
