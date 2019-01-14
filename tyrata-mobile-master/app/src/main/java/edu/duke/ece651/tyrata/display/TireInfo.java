package edu.duke.ece651.tyrata.display;

/**
 * This class has Tireinfo display page
 * @author De Lan
 * Created by Alan on 2/27/2018.
 */
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import edu.duke.ece651.tyrata.MainActivity;
import edu.duke.ece651.tyrata.R;
import edu.duke.ece651.tyrata.calibration.TireInfoInput;
import edu.duke.ece651.tyrata.datamanagement.Database;
import edu.duke.ece651.tyrata.vehicle.Tire;
import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;

public class TireInfo extends AppCompatActivity {
    int axis_row;
    int axis_index;
    char axis_side;
    String vin;
    int vehicle_ID;
    String message_manufacturer;
    String message_sensorID;
    String message_model;
    String message_SKU;
    String message_Thickness;
    String message_S11;
    String message_Odometer;
    String message_EOL;
    String message_rep;

    private LineChartView lineChart;

    String[] date = {};//X轴的标注
    float[] score = {};//图表的数据点
    private List<PointValue> mPointValues = new ArrayList<PointValue>();
    private List<PointValue> thresholdPointValues = new ArrayList<PointValue>();
    private List<AxisValue> mAxisXValues = new ArrayList<AxisValue>();
    private List<AxisValue> mAxisYValues = new ArrayList<AxisValue>();
    private float max_point;

    @Override
    protected void onStop(){
        SharedPreferences.Editor editor = getSharedPreferences("tire_data",MODE_PRIVATE).edit();
        editor.putString("VIN",vin);
        Log.i("In TireInfo onStop",vin);
        editor.commit();
        super.onStop();
    }
    @Override
    protected void onDestroy(){
        SharedPreferences.Editor editor = getSharedPreferences("tire_data",MODE_PRIVATE).edit();
        editor.putString("VIN",vin);
        Log.i("In TireInfo onDestroy",vin);
        editor.commit();
        super.onDestroy();
    }
    @Override
    protected void onPause(){
        SharedPreferences.Editor editor = getSharedPreferences("tire_data",MODE_PRIVATE).edit();
        editor.putString("VIN",vin);
        Log.i("In TireInfo onPause",vin);
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
        if(item.getItemId() == R.id.homepage){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        else{
            Intent intent = new Intent(this, Vehicle_Info.class);
            intent.putExtra("VIN",vin);
            startActivity(intent);
        }
        return true;
    }

    /**
     * Display the tire data: display the default data if there is no processed data for this tire
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tire_info);
        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new         Intent(getApplicationContext(),Vehicle_Info.class));
            }
        });

        axis_row = intent.getIntExtra("AXIS_ROW",0);
        axis_index = intent.getIntExtra("AXIS_INDEX",0);
        axis_side = intent.getCharExtra("AXIS_SIDE",'a');
        vin = intent.getStringExtra("VIN");

        //get the tire info from the database
        Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
        vehicle_ID = Database.getVehicleID(vin);
        Tire curr_tire = Database.getTire(axis_row, axis_index, axis_side, vehicle_ID);
        Database.myDatabase.close();

        //show the tire info on the page
        if(curr_tire != null) {
            message_manufacturer = curr_tire.getManufacturer();
            message_sensorID = curr_tire.getSensor();
            message_model = curr_tire.getModel();
            message_SKU = curr_tire.getSku();
            message_Thickness = doubleToString(curr_tire.get_CURR_THCK());
            message_S11 = String.valueOf(curr_tire.getS11());
            message_Odometer = doubleToString(curr_tire.getOdometer());
            message_EOL = curr_tire.getEOL();
            message_rep = curr_tire.getRepTime();
        }

        if(message_manufacturer == null)
            message_manufacturer = "Need manufacturer";
        TextView textView_manufacturer = findViewById(R.id.textView_manufacturer);
        textView_manufacturer.setText(message_manufacturer);

        if(message_sensorID == null){
            message_sensorID = "Need sensorID";
            CardView DeleteTire = findViewById(R.id.button_ADeteleTire);
            DeleteTire.setVisibility(View.INVISIBLE);
        }
        TextView textView_sensorID = findViewById(R.id.textView_sensorID);
        textView_sensorID.setText(message_sensorID);

        if(message_model == null)
            message_model = "Need MODEL";
        TextView textView_model = findViewById(R.id.textView_model);
        textView_model.setText(message_model);

        if(message_SKU == null)
            message_SKU = "Need SKU";
        TextView textView_SKU = findViewById(R.id.textView_SKU);
        textView_SKU.setText(message_SKU);

        if(message_Thickness == null)
            message_Thickness = "";
        TextView textView_Thickness = findViewById(R.id.textView_thickness);
        textView_Thickness.setText(message_Thickness);

        if(message_Odometer == null || message_Odometer.equals("0.00"))
            message_Odometer = "";
        TextView textView_Odometer = findViewById(R.id.textView_odometer);
        textView_Odometer.setText(message_Odometer);

        if(message_EOL == null || message_EOL.equals("Default")) {
            message_EOL = "";
        }else {
            double d_eol = Double.valueOf(message_EOL);
            message_EOL = doubleToString(d_eol);
        }
        TextView textView_EOL = findViewById(R.id.textView_EOL);
        textView_EOL.setText(message_EOL);

        if(message_rep == null || message_rep.equals("Default"))
            message_rep = "";
        TextView textView_rep = findViewById(R.id.textView_replace);
        textView_rep.setText(message_rep);

        //get the line chart data from the database
        Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
        ArrayList<Pair<String, Double>> line_data = Database.get_thickness_and_timestamp(message_sensorID);
        Database.myDatabase.close();

        date = new String[line_data.size()];
        score = new float[line_data.size()];


        for(int i = 0;i<line_data.size();i++){
            date[i] = line_data.get(i).first;
            Log.i("score",String.valueOf(line_data.get(i).second));
            score[i] = line_data.get(i).second.floatValue();
        }

        //set the line chart
        TextView title = findViewById(R.id.chart_title);
        lineChart = (LineChartView)findViewById(R.id.line_chart);
        getAxisXYLables();
        getAxisPoints();
        initLineChart();
        if(curr_tire==null){
            title.setVisibility(View.GONE);
            lineChart.setVisibility(View.GONE);
        }

    }

    /**
     * Called to switch to edit: the TireInfoInput page
     */
    public void switchToEdit(View view) {
        Intent intent = new Intent(TireInfo.this, TireInfoInput.class);

        intent.putExtra("axis_IDX", axis_index);
        intent.putExtra("axis_ROW", axis_row);
        intent.putExtra("axis_SIDE", axis_side);
        intent.putExtra("VIN",vin);
        intent.putExtra("SENSOR_ID",message_sensorID);

        Log.i("NOTIFICATION","Tireinfo");
        Log.i("axis_ROW",String.valueOf(axis_row));
        Log.i("axis_IDX", String.valueOf(axis_index));
        Log.i("axis_SIDE", String.valueOf(axis_side));
        Log.i("VIN", vin);
        startActivity(intent);
    }

    /**
     * Called to get back to the vehicle page
     */
    public void BackToVehicle(View view) {
        Intent intent = new Intent(TireInfo.this, Vehicle_Info.class);
        intent.putExtra("VIN", vin);
        startActivity(intent);
    }


    /* Added by De Lan on 3/25/2018 */
    /**
     * Called to delete this tire
     */
    public void DeleteTire(final View view){
        new AlertDialog.Builder(this)
                .setTitle("NOTIFICATION")
                .setMessage("Are you sure to delete this tire from your account?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
                                Database.deleteTire(message_sensorID);
                                Database.myDatabase.close();
                                BackToVehicle(view);
                            }
                        }
                )
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Initialize the XY labels of the line chart
     * added by Ming Yang
     */
    private void getAxisXYLables(){
        for (int i = 0; i < date.length; i++) {
            mAxisXValues.add(new AxisValue(i).setLabel(date[i]));

        }
        for (int i = 0; i < 20; i++) {
            mAxisYValues.add(new AxisValue(i).setLabel(i+""));
        }
    }

    /**
     * Initialize the point data of the line chart
     * added by Ming Yang
     */
    private void getAxisPoints() {
        max_point = 0;
        for (int i = 0; i < score.length; i++) {
            if(score[i] > max_point) max_point = score[i];
            mPointValues.add(new PointValue(i, score[i]).setLabel(String.format("%.3f",score[i])));
            thresholdPointValues.add(new PointValue(i, 3));
        }
    }

    /**
     * Initialize the settings of the line chart
     * added by Ming Yang
     */
    private void initLineChart(){
        Line line = new Line(mPointValues).setColor(Color.parseColor("#00bcd4"));  //color of the line（orange）
        List<Line> lines = new ArrayList<Line>();
        line.setShape(ValueShape.CIRCLE);//shape of the dot on line  (circle) （three types ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.DIAMOND）
        line.setCubic(false);//curve or broken line
        line.setFilled(false);
        line.setHasLabels(false);
        line.setHasLabelsOnlyForSelected(true);
        line.setHasLines(true);//whether have line
        line.setHasPoints(true);
        lines.add(line);

        Line threshold_line = new Line(thresholdPointValues).setColor(Color.parseColor("#F44336"));
        threshold_line.setHasPoints(false);
        threshold_line.setCubic(false);
        threshold_line.setFilled(false);
        threshold_line.setHasLabels(false);
        threshold_line.setHasLines(true);
        lines.add(threshold_line);

        LineChartData data = new LineChartData();
        data.setLines(lines);

        //X axis
        Axis axisX = new Axis();
        axisX.setHasTiltedLabels(true);  //whether the x axis text is italic
        axisX.setTextColor(Color.WHITE);  //text color
        //axisX.setName("The thickness of the tire");  //axis name
        axisX.setTextSize(14);//text size
        axisX.setMaxLabelChars(8);
        axisX.setValues(mAxisXValues);
        data.setAxisXBottom(axisX); //x axis is at bottom
        axisX.setHasLines(true); //x axis dividing rules


        // Y axis
        Axis axisY = new Axis();
        axisY.setTextColor(Color.WHITE);
        //axisY.setName("Thickness");
        axisY.setValues(mAxisYValues);
        axisY.setTextSize(14);
        data.setAxisYLeft(axisY);  //Y axis is on the left
        axisY.setHasLines(true);

        lineChart.setInteractive(true);
        lineChart.setZoomType(ZoomType.HORIZONTAL);
        lineChart.setMaxZoom((float) 2);
        lineChart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        lineChart.setLineChartData(data);
        lineChart.setVisibility(View.VISIBLE);

        Viewport v = new Viewport(lineChart.getMaximumViewport());
        v.bottom = 0;
        v.top = max_point + 5;
        lineChart.setMaximumViewport(v);
        v.left = 0;
        v.right= 7;
        lineChart.setCurrentViewport(v);
    }

    public static String doubleToString(double num){
        return new DecimalFormat("0.00").format(num);
    }
}
