package edu.duke.ece651.tyrata.user;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.support.v7.widget.Toolbar;
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
/**
 * Created by Ming .
 * the java code of the activity_register.xml page
 */

public class Register extends AppCompatActivity {

    String message_username;
    String message_email;
    String message_phone;
    String message_password;
    String message_confirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //checkInternetConenction();
    }

    public void Register_to_login(View view) {

        EditText username = (EditText) findViewById(R.id.register_edit_username);
        message_username = username.getText().toString();

        EditText email = (EditText) findViewById(R.id.register_edit_email);
        message_email = email.getText().toString();

        EditText phone = (EditText) findViewById(R.id.register_edit_phonenumber);
        message_phone = phone.getText().toString();

        EditText password = findViewById(R.id.register_edit_password);
        message_password = password.getText().toString();

        EditText confirmPassword = findViewById(R.id.register_confirm_password);
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
            registerUser(message_username, message_email, message_phone, message_password);
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

    private void registerUser(String username, String email, String phone, String password) {
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


        String create_user = "<message><id>0</id><method>create</method><user><name>" + username
                + "</name><email>" + email
                + "</email><phone_num>" + phone
                + "</phone_num><hash>" + hash_string
                + "</hash><salt>" + salt_string
                + "</salt></user><original_info></original_info></message>";

        String myUrl = "http://vcm-2932.vm.duke.edu:9999/tyrata-team/XmlAction?xml_data=" + create_user;
        Log.i("myUrl", myUrl);

        send_message(myUrl);
        Log.i("send_new_method", "success");

    }


    private boolean checkInternetConenction() {
        // get Connectivity Manager object to check connection
        ConnectivityManager connec
                = (ConnectivityManager) getSystemService(getBaseContext().CONNECTIVITY_SERVICE);

        // Check for network connections
        if (connec.getNetworkInfo(0).getState() ==
                android.net.NetworkInfo.State.CONNECTED ||
                connec.getNetworkInfo(0).getState() ==
                        android.net.NetworkInfo.State.CONNECTING ||
                connec.getNetworkInfo(1).getState() ==
                        android.net.NetworkInfo.State.CONNECTING ||
                connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTED) {
            Toast.makeText(this, " Connected ", Toast.LENGTH_LONG).show();
            return true;
        } else if (
                connec.getNetworkInfo(0).getState() ==
                        android.net.NetworkInfo.State.DISCONNECTED ||
                        connec.getNetworkInfo(1).getState() ==
                                android.net.NetworkInfo.State.DISCONNECTED) {
            Toast.makeText(this, " Not Connected ", Toast.LENGTH_LONG).show();
            return false;
        }
        return false;
    }

    private void send_message(String urlStr) {
        final String url = urlStr;
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
                Intent intent = new Intent(Register.this, edu.duke.ece651.tyrata.user.Log_in.class);
                Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
                // For test, drop and create tables
//                Database.dropAllTable();
                Database.createTable();
                Database.storeUserData("",message_username, message_email, message_phone);
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
}
