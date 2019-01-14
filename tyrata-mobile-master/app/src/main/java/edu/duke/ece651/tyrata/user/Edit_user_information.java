package edu.duke.ece651.tyrata.user;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import edu.duke.ece651.tyrata.Common;
import edu.duke.ece651.tyrata.MainActivity;
import edu.duke.ece651.tyrata.R;
import edu.duke.ece651.tyrata.communication.ServerXmlParser;
import edu.duke.ece651.tyrata.datamanagement.Database;

public class Edit_user_information extends AppCompatActivity {
    private  int user_ID;
    String message_username;
    String message_email;
    String message_phone;
    String message_password;
    String message_confirmPassword;
    String original_email;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user_information);

        //add the toolbar to the page
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

        //get user id
        SharedPreferences editor = getSharedPreferences("user_data",MODE_PRIVATE);
        user_ID = editor.getInt("USER_ID",0);

        //get original user info from database and show on the edittext
        Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
        User curr_user = Database.getUser(user_ID);
        Database.myDatabase.close();

        EditText textView_username = findViewById(R.id.edit_username);
        textView_username.setText(curr_user.username);

        EditText textView_phone = findViewById(R.id.edit_phonenumber);
        textView_phone.setText(curr_user.phone);

        EditText textView_email = findViewById(R.id.edit_email);
        textView_email.setText(curr_user.email);

        original_email = curr_user.email;

    }

    public void edit_to_main(View view) {
        EditText username = (EditText) findViewById(R.id.edit_username);
        message_username = username.getText().toString();

        EditText email = (EditText) findViewById(R.id.edit_email);
        message_email = email.getText().toString();

        EditText phone = (EditText) findViewById(R.id.edit_phonenumber);
        message_phone = phone.getText().toString();

        EditText password = findViewById(R.id.edit_password);
        message_password = password.getText().toString();

        EditText confirmPassword = findViewById(R.id.confirm_password);
        message_confirmPassword = confirmPassword.getText().toString();

        String msg = "";
        if (message_username.equals("")) {
            msg = "The username cannot be empty!";
        }
        if (message_email.equals("") || !isEmailValid(message_email)) {
            msg = "The email is illegal!";
        }
        if (message_phone.equals("") || !isPhoneValid(message_phone)) {
            msg = "The phone number in illegal!";
        }
        if (message_password.equals("")) {
            msg = "The password cannot be empty!";
        }
        if (!message_password.equals(message_confirmPassword)) {
            msg = "The two passwords you typed do not match!";
        }

        if (msg.equals("")) {
            editUser(message_username, message_email, message_phone, message_password);
        } else {
            notification(msg);
        }
    }

    private void notification(String msg) {
        new AlertDialog.Builder(this)
                .setTitle("NOTIFICATION")
                .setMessage(msg)
                .setPositiveButton("Yes", null)
                .show();
    }

    /**
     * Check if the email is valid
     */
    boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Check if the phone number is valid
     */
    boolean isPhoneValid(String phone_number) {
        return PhoneNumberUtils.isGlobalPhoneNumber(phone_number);
    }

    private void editUser(String username, String email, String phone, String password) {
        // Hash the password using salt
        byte salt[] = AuthenticationAPI.generateSalt();
        byte hashedPassword[] = AuthenticationAPI.hashPass(password, salt);

        StringBuilder salt_sb = new StringBuilder();
        for (byte salt_byte : salt) {
            salt_sb.append(Integer.toString((salt_byte & 0xff) + 0x100, 16).substring(1));
        }
        String salt_string = salt_sb.toString();

        Log.i(Common.LOG_TAG_AUTHENTICATION_API, "salt hex format: " + salt_sb.toString());

        StringBuilder hash_sb = new StringBuilder();
        for (byte hash_byte : hashedPassword) {
            hash_sb.append(Integer.toString((hash_byte & 0xff) + 0x100, 16).substring(1));
        }
        String hash_string = hash_sb.toString();


        String edit_user = "<message><id>0</id><method>update</method><user><name>" + username
                + "</name><email>" + email
                + "</email><phone_num>" + phone
                + "</phone_num><hash>" + hash_string
                + "</hash><salt>" + salt_string
                + "</salt></user><original_info>" + original_email
                + "</original_info></message>";

        String myUrl = "http://vcm-2932.vm.duke.edu:9999/tyrata-team/XmlAction?xml_data=" + edit_user;
        Log.i("myUrl", myUrl);

        send_message(myUrl);
        Log.i("send_new_method", "success");

    }

    private void send_message(String urlStr) {
        final String url = urlStr;
        String resource;
        new Thread() {
            public void run() {
                InputStream in = null;
                Message msg = Message.obtain();
                msg.what = 1;
                try {
                    in = openHttpConnection(url);
                    if (in != null) {
                        String resource = new Scanner(in).useDelimiter("\\Z").next();
                        Log.i("test_new_method", resource);
                        Bundle b = new Bundle();
                        b.putString("get_message", resource);
                        msg.setData(b);
                        in.close();
                        messageHandler.sendMessage(msg);
                    } else {
                        Bundle b = new Bundle();
                        b.putString("not_connect", "Internet not connect");
                        msg.setData(b);
                        errorHandler.sendMessage(msg);
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }.start();
    }

    private InputStream openHttpConnection(String urlStr) {
        InputStream in = null;
        int resCode = -1;

        try {
            URL url = new URL(urlStr);
            URLConnection urlConn = url.openConnection();

            if (!(urlConn instanceof HttpURLConnection)) {
                Log.i("new_method", "wrong");
                throw new IOException("URL is not an Http URL");
            }

            HttpURLConnection httpConn = (HttpURLConnection) urlConn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();
            resCode = httpConn.getResponseCode();

            if (resCode == HttpURLConnection.HTTP_OK) {
                Log.i("new_method", "get");
                in = httpConn.getInputStream();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return in;
    }

    private Handler messageHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String message = msg.getData().getString("get_message");

            if (message.equals("<message><ack>0</ack></message>")) {
                Intent intent = new Intent(Edit_user_information.this, edu.duke.ece651.tyrata.MainActivity.class);
                Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
                // For test, drop and create tables
                Database.storeUserData(original_email,message_username, message_email, message_phone);
                Database.myDatabase.close();
                startActivity(intent);
            } else {
                ServerXmlParser parser = new ServerXmlParser();
                InputStream message_error = new ByteArrayInputStream(message.getBytes());
                try {
                    parser.parse_server(message_error, getApplicationContext());
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private Handler errorHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String message = msg.getData().getString("not_connect");
            Toast.makeText(getApplicationContext(), " Internet Not Connected ", Toast.LENGTH_LONG).show();
        }
    };

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
}
