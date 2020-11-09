package com.github.navisensfindme;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.navisens.motiondnaapi.MotionDna;
import com.navisens.motiondnaapi.MotionDnaSDK;
import com.navisens.motiondnaapi.MotionDnaSDKListener;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements MotionDnaSDKListener, OnMapReadyCallback {
    MotionDnaSDK navisens_sdk = null;
    private static ZeroMQClient client = new ZeroMQClient();
    private static boolean navisensStarted = false;
    private boolean clientInitialized = false;
    private GoogleMap map;
    private Marker userMarker;
    private TextView navisens_info;
    User user;

    // Android functions
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.appbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.FindMeSettings) {
            Log.d("appbar", "Clicked on settings.");
            Intent intent = new Intent(this, FindMeSettings.class);
            startActivity(intent);
            return true;
        } else {
            // If we got here, the user's action was not recognized. Invoke the superclass to handle it.
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        navisens_info = findViewById(R.id.navisens_info);
        // Toolbar for Preferences
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        // Google Maps SDK
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (MotionDnaSDK.checkMotionDnaPermissions(this)) {
            Log.i("navisens","Navisens permissions okay.");
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap){
        this.map = googleMap;
        this.map.setMinZoomPreference(18);

        user = new User();
    }

    @Override
    protected void onDestroy() {
        // Shuts downs the MotionDna Core, if it was started to begin with.
        if (navisens_sdk != null){
            navisens_sdk.stop();
            super.onDestroy();
        }
    }

    // Navisens or app-specific functions.
    private String getDevKey() {
        return this.getString(R.string.navapi);
    }

    public void sendTestMQ(View view){
        if (clientInitialized){
            String[] test_message = {"Test"};
            client.sendMessage(test_message);
            Toast mq_toast = Toast.makeText(getApplicationContext(), "Test message sent.", Toast.LENGTH_SHORT);
            mq_toast.show();
        }
        else {
            Toast mq_toast = Toast.makeText(getApplicationContext(), "ZeroMQ not initialized.", Toast.LENGTH_SHORT);
            mq_toast.show();
        }
    }

    public void resetNavisens(View view){
        Log.i("locationData","Resetting");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
        boolean reset_xyz = sharedPreferences.getBoolean("resetXYZNavisens",false);
        boolean reset_heading = sharedPreferences.getBoolean("resetHeadingNavisens",false);
        String initial_x = sharedPreferences.getString("xInitNavisens", "0");
        String initial_y = sharedPreferences.getString("yInitNavisens", "0");
        String initial_lng = sharedPreferences.getString("lngInitNavisens", "0");
        String initial_lat = sharedPreferences.getString("latInitNavisens", "0");
        String heading = sharedPreferences.getString("headingInitNavisens", "0");
        user.init(Double.parseDouble(initial_lat),Double.parseDouble(initial_lng));

        if(!navisensStarted){
            Toast reset_toast = Toast.makeText(getApplicationContext(), "Navisens not even started.", Toast.LENGTH_SHORT);
            reset_toast.show();
        }
        else if(!reset_xyz & !reset_heading){
            Toast reset_toast = Toast.makeText(getApplicationContext(), "Nothing to reset, have you checked your settings?", Toast.LENGTH_SHORT);
            reset_toast.show();
        }
        else {
            Toast reset_toast = Toast.makeText(getApplicationContext(), "Performing reset...", Toast.LENGTH_SHORT);
            reset_toast.show();
            if(reset_xyz){
                navisens_sdk.setGlobalPosition(Double.parseDouble(initial_x),Double.parseDouble(initial_y));
            }
            if(reset_heading){
                navisens_sdk.setCartesianHeading(Double.parseDouble(heading));
            }
            reset_toast = Toast.makeText(getApplicationContext(), "Reset done.", Toast.LENGTH_SHORT);
            reset_toast.show();
        }
    }

    public void handleNavisens(View view){
        if(!navisensStarted) {
            Log.i("locationData","Starting");
            // Reading from preferences...
            navisens_sdk = new MotionDnaSDK(this.getApplicationContext(),this);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this /* Activity context */);
            boolean init_zmq = sharedPreferences.getBoolean("initZMQ",false);
            String ip = sharedPreferences.getString("ipZMQ", "");
            String navisensModel = sharedPreferences.getString("navisensModel", "standard");

            String navisens_init_text = "Initializing... (" + MotionDnaSDK.SDKVersion() + ")";
            Toast init_toast = Toast.makeText(getApplicationContext(), navisens_init_text, Toast.LENGTH_SHORT);
            init_toast.show();

            String devKey = this.getDevKey();
            Log.d("navisens", "devKey = " + devKey);

            // Configure model. **** or standard.
            HashMap<String, Object> configuration = new HashMap<>();
            Log.d("navisens", "model = " + navisensModel);
            configuration.put("model", navisensModel);
            configuration.put("gps",false);
            configuration.put("corrected_trajectory",false);
            configuration.put("callback",40);
            configuration.put("logging",true);
            navisens_sdk.run(devKey, configuration);

            String navisens_done = MotionDnaSDK.SDKVersion() + " ready.";

            navisensStarted = true;
            Toast toast_done = Toast.makeText(getApplicationContext(), navisens_done, Toast.LENGTH_SHORT);
            toast_done.show();

            // After initialization, change "Initialize Navisens" on button to "Stop Navisens".
            Button init_button = findViewById(R.id.init_button);
            init_button.setText(this.getString(R.string.deinitButton));

            // Initializing zeroMQ client.
            if(init_zmq){
                if(!clientInitialized){
                    client.initializeClient(ip);
                    clientInitialized = true;
                }
            }
        }
        else {
            Log.i("locationData","Stopping");
            navisens_sdk.stop();
            navisensStarted = false;
            String navisens_stopped = MotionDnaSDK.checkSDKVersion() + " stopped.";
            Toast toast_stopped = Toast.makeText(getApplicationContext(), navisens_stopped, Toast.LENGTH_SHORT);
            toast_stopped.show();
            Button init_button = findViewById(R.id.init_button);
            init_button.setText(this.getString(R.string.initButton));

            String dashboardDisplay = this.getString(R.string.navisens_info);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    navisens_info.setText(dashboardDisplay);
                }
            });

            if(clientInitialized){
                client.stopClient();
            }
        }
    }

    private void mapUser(){
        LatLng userPosition = new LatLng(user.getCurrentLat(),user.getCurrentLong());
        if (userMarker != null){
            Log.i("navisens","Lat: " + user.getCurrentLat() + "Lng: " + user.getCurrentLong());
            userMarker.setPosition(userPosition);
            userMarker.setRotation((float)user.getCurrentHeading());
        }
        else {
            userMarker = this.map.addMarker(new MarkerOptions()
                    .position(userPosition)
                    .anchor(0.5f, 0.5f)
                    .rotation((float)user.getCurrentHeading())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.navigation))
                    .title("User"));
        }
        this.map.moveCamera(CameraUpdateFactory.newLatLng(userPosition));
    }

    @Override
    public void receiveMotionDna(MotionDna motionDna) {
        String navisens_header = MotionDnaSDK.SDKVersion();
        MotionDna.GlobalLocation latlong = motionDna.getLocation().global;
        MotionDna.CartesianLocation location = motionDna.getLocation().cartesian;

        double global_latitude = latlong.latitude;
        double global_longitude = latlong.longitude;
        double x = location.x;
        double y = location.y;
        double z = location.z;
        double heading = motionDna.getLocation().global.heading;
        String motionType = motionDna.getClassifiers().get("motion").prediction.label;
        String dashboardDisplay = navisens_header
                + "\n\nGlobal Latitude: "
                + global_latitude + "\nGlobal Longitude: "
                + global_longitude
                + "\n\nX: " + x
                + "\nY: " + y
                + "\nZ: " + z
                + "\n Heading: " + heading
                + "\n\nMotion Type: " + motionType;

        Log.i("locationData","x,"+ x + ",y,"+y);
        navisens_info.setTextColor(Color.BLACK);
        if(clientInitialized){
            client.sendMessage(NavisensZMQAdapter.pack_data(x,y));
        }
        user.addMetersToDegree(x,y);
        user.setCurrentHeading(heading);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                navisens_info.setText(dashboardDisplay);
                mapUser();
            }
        });
    }

    @Override
    public void reportStatus(MotionDnaSDK.Status status, String s) {
        switch (status) {
            case AuthenticationFailure:
                System.out.println("Error: Authentication Failed " + s);
                break;
            case AuthenticationSuccess:
                System.out.println("Status: Authentication Successful " + s);
                break;
            case ExpiredSDK:
                System.out.println("Status: SDK expired " + s);
                break;
            case PermissionsFailure:
                System.out.println("Status: permissions not granted " + s);
                break;
            case MissingSensor:
                System.out.println("Status: sensor missing " + s);
                break;
            case SensorTimingIssue:
                System.out.println("Status: sensor timing " + s);
                break;
            case Configuration:
                System.out.println("Status: configuration " + s);
                break;
            case None:
                System.out.println("Status: None " + s);
                break;
            default:
                System.out.println("Status: Unknown " + s);
        }
    }
}