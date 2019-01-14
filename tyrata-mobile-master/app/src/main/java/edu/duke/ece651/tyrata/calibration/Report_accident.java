package edu.duke.ece651.tyrata.calibration;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import edu.duke.ece651.tyrata.MainActivity;
import edu.duke.ece651.tyrata.R;
import edu.duke.ece651.tyrata.datamanagement.Database;

/*Created by Ming Yang
 * the java code of the activity_report_accident.xml page
 */

public class Report_accident extends AppCompatActivity {
    int user_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_accident);

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

        //get user id from the main page
        Intent intent = getIntent();
        user_ID = intent.getIntExtra("userID", 1);
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
     * Save the report data into database and get back to main page
     */
    public void sendReportToMain(View view) {
        Intent intent = new Intent(this, edu.duke.ece651.tyrata.MainActivity.class);

        //get the message from the edittext
        EditText edit_report = (EditText) findViewById(R.id.report_editText);
        String message_report = edit_report.getText().toString();

        //Store the accident into the database.
        Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
        Database.storeAccident(message_report, user_ID);
        Database.myDatabase.close();

        //switch to main page
        startActivity(intent);
    }

}