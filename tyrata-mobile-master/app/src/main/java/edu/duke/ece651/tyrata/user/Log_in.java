package edu.duke.ece651.tyrata.user;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

public class Log_in extends AppCompatActivity {
    private String email;
    private String password;
    private int task = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);
    }

    /**
     * Authenticated user will successfully login and goes to the main page
     */
    public void switchto_main(View view) {
        EditText input_email = (EditText) findViewById(R.id.input_email);
        email = input_email.getText().toString();

        EditText inputPassword = findViewById(R.id.input_password);
        password = inputPassword.getText().toString();

        task = Common.GET_SALT;
        sendLoginRequest();
    }

    private void sendLoginRequest(){
        String login_message = "<message><authentication><email>" + email + "</email></authentication></message>";
        String myUrl = "http://vcm-2932.vm.duke.edu:9999/tyrata-team/XmlAction?xml_data=" + login_message;
        Log.i("myUrl",myUrl);
        send_message(myUrl);
        Log.i("send_new_method","success");
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
                    Log.i("send_url",url);
                    in = openHttpConnection(url);
                    if(in != null) {
                        String resource = new Scanner(in).useDelimiter("\\Z").next();
                        Log.i("test_new_method", resource);
                        Bundle b = new Bundle();
                        b.putString("get_message", resource);
                        msg.setData(b);
                        in.close();
                        if (task == Common.GET_SALT) {
                            saltHandler.sendMessage(msg);
                            task = 0;
                        } else if (task == Common.GET_AUTHENTICATION) {
                            SharedPreferences editor_get = getSharedPreferences("msg_from_server", MODE_PRIVATE);
                            String salt_get = editor_get.getString("salt", "");
                            Log.e("salt", salt_get);
                            authenticationHandler.sendMessage(msg);
                            task = 0;
                        } else {
                            databaseHandler.sendMessage(msg);
                            task = 0;
                        }
                    }else{
                        Bundle b = new Bundle();
                        b.putString("not_connect", "Connection error");
                        msg.setData(b);
                        errorHandler.sendMessage(msg);
                    }
                }catch (IOException e1) {
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
                Log.i("new_method","wrong");
                throw new IOException("URL is not an Http URL");
            }

            HttpURLConnection httpConn = (HttpURLConnection) urlConn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();
            resCode = httpConn.getResponseCode();

            if (resCode == HttpURLConnection.HTTP_OK) {
                Log.i("new_method","get");
                in = httpConn.getInputStream();
            }
        }catch (MalformedURLException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return in;
    }

    private Handler saltHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String message = msg.getData().getString("get_message");

            message = message.replace("<message><authentication>","");
            message = message.replace("</authentication></message>","");

            // get user salt and hashed password from server
            byte salt[] = new byte[message.length()/2];
            int b = 0;
            for(int i = 0; i < message.length(); i+=2){
                salt[i/2] = (byte)((Character.digit(message.charAt(i),16)<<4)+ Character.digit(message.charAt(i+1),16));
            }


            // re-calculate hashed password from user input and salt
            byte hashedPassword[] = AuthenticationAPI.hashPass(password, salt);

            StringBuilder hash_sb = new StringBuilder();
            for (byte hash_byte : hashedPassword) {
                hash_sb.append(Integer.toString((hash_byte & 0xff) + 0x100, 16).substring(1));
            }
            String hash_string = hash_sb.toString();

            String hash_info = "<message><authentication><email>" + email
                    + "</email><hash>" + hash_string
                    + "</hash></authentication></message>";
            String get_authentication = "http://vcm-2932.vm.duke.edu:9999/tyrata-team/XmlAction?xml_data=" + hash_info;
            task = Common.GET_AUTHENTICATION;
            send_message(get_authentication);
        }
    };

    private Handler authenticationHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String message = msg.getData().getString("get_message");
            Log.i("show message",message);
            Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();

            if(message.equals("<message><authentication>success</authentication></message>")){
                Intent intent = new Intent(Log_in.this, MainActivity.class);
                Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
                int user_ID = Database.getUserID(email);
                Database.myDatabase.close();
                if (user_ID != -1) {
                    Log.i("exist", String.valueOf(user_ID));
                    intent.putExtra("USER_ID", user_ID);
                    startActivity(intent);
                } else {
                    String get_message = "<message><method>get</method><user><email>" + email + "</email></user></message>";
                    String get_data = "http://vcm-2932.vm.duke.edu:9999/tyrata-team/XmlAction?xml_data=" + get_message;
                    task = Common.GET_DATABASE;
                    send_message(get_data);
                }
            }else if (message.equals("<message><authentication>failure</authentication></message>")){
                String msg_notification = "Incorrect credentials. Please enter the right information or register.";
                notification(msg_notification);
            }
        }
    };

    private Handler databaseHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String message = msg.getData().getString("get_message");

            ServerXmlParser parser = new ServerXmlParser();
            InputStream msg_getdata = new ByteArrayInputStream(message.getBytes());
            try {
                parser.parse_server(msg_getdata, getApplicationContext());
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Intent intent = new Intent(Log_in.this, MainActivity.class);
            Database.myDatabase = openOrCreateDatabase("TyrataData", MODE_PRIVATE, null);
            int user_ID = Database.getUserID(email);
            Database.myDatabase.close();
            intent.putExtra("USER_ID", user_ID);
            startActivity(intent);
        }
    };

    private Handler errorHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            String message = msg.getData().getString("not_connect");
            Toast.makeText(getApplicationContext(), " Connection error", Toast.LENGTH_LONG).show();
        }
    };

    /**
     * Switch to the register page
     */
    public void switchto_register(View view) {
        Intent intent = new Intent(Log_in.this, edu.duke.ece651.tyrata.user.Register.class);

        startActivity(intent);
        // Do something in response to button
    }

    public void notification(String msg){
        new AlertDialog.Builder(this)
                .setTitle("NOTIFICATION")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }
}
