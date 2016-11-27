package com.emanueletonucci.etchronorace;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.multidex.MultiDex;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.maps.model.LatLng;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionResult;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
//import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Emanuele on 18/09/2016.
 */

public class MainActivity extends AppCompatActivity implements DroneListener, TowerListener {

    private InterstitialAd mInterstatial;
    private boolean mAdvertisement = true;

    private CrossingPoint cp;
    LatLng a = new LatLng(0.0, 0.0); //punto successivo
    LatLng b = new LatLng(0.0, 0.0); //punto precedente

    private ControlTower controlTower;
    private Drone drone;
    private final Handler handler = new Handler();
    //private int droneType = Type.TYPE_UNKNOWN;
    private Menu menu;

    private boolean START_CHRONO = false;
    private float MIN_PITCH = 1.0f;
    private int MAX_STEP_AFTER_CROSS=70;
    private LatLng FL=null;
    private LatLng S1=null;
    private LatLng S2=null;
    private LatLng S3=null;
    private MediaPlayer mpBest,mpFL,mpS;
    private int k=0;
    private boolean conta=false;
    private long velocita_minima = 1;

    private long init,now,time,paused;
    private long FLTime,FLTimePrec,S1Time,S2Time,S3Time,BestTime;
    private int nLap=0;
    private boolean prec = true;

    List<Laps> lapsList = new ArrayList<Laps>();

    private int counterShowGrid;


    final Runnable updater = new Runnable() {
        @Override
        public void run() {
            if (START_CHRONO) {
                now=System.currentTimeMillis();
                time=now-init;
                try {
                    TextView displayChrono = (TextView) findViewById(R.id.txtChrono);
                    displayChrono.setText("TIME: " + new SimpleDateFormat("mm:ss:SSS").format(new Date(time)));
                }
                catch(Exception e) {
                    Log.d("Error in timer: ", e.toString());
                }
                handler.postDelayed(this, 1);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        MultiDex.install(this);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        // Begin the transaction
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        // Replace the contents of the container with the new fragment
        ft.replace(R.id.map_placeholder, new TrackFragment());
        // or ft.add(R.id.your_placeholder, new FooFragment());
        // Complete the changes added above
        ft.commit();

//        FloatingActionButton telemetryView = (FloatingActionButton) findViewById(R.id.telemetryView);
//        telemetryView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
////                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
////                        .setAction("Action", null).show();
//                TableLayout tabLayoutTelemetry = (TableLayout) findViewById(R.id.tableLayoutTelemetry);
//                if (tabLayoutTelemetry.getVisibility() == View.GONE)
//                    tabLayoutTelemetry.setVisibility(View.VISIBLE);
//                else
//                    tabLayoutTelemetry.setVisibility(View.GONE);
//            }
//        });


        final FloatingActionButton chronoView = (FloatingActionButton) findViewById(R.id.chronoView);
        chronoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                counterShowGrid++;
                TableLayout tabLayoutChrono = (TableLayout) findViewById(R.id.tableLayoutChrono);
                TableLayout tabLayoutChronoRow = (TableLayout) findViewById(R.id.tableLayoutChronoRow);
                TableLayout tabLayoutTelemetry = (TableLayout) findViewById(R.id.tableLayoutTelemetry);
                if (counterShowGrid == 1){
                    tabLayoutChrono.setVisibility(View.VISIBLE);
                    tabLayoutChronoRow.setVisibility(View.VISIBLE);
                    tabLayoutTelemetry.setVisibility(View.GONE);
                }
                if (counterShowGrid == 2){
                    tabLayoutChrono.setVisibility(View.VISIBLE);
                    tabLayoutChronoRow.setVisibility(View.GONE);
                    tabLayoutTelemetry.setVisibility(View.VISIBLE);
                }
                if (counterShowGrid == 3){
                    tabLayoutChrono.setVisibility(View.VISIBLE);
                    tabLayoutChronoRow.setVisibility(View.GONE);
                    tabLayoutTelemetry.setVisibility(View.GONE);
                }
                if (counterShowGrid == 4){
                    tabLayoutChrono.setVisibility(View.GONE);
                    tabLayoutChronoRow.setVisibility(View.VISIBLE);
                    tabLayoutTelemetry.setVisibility(View.VISIBLE);
                }
                if (counterShowGrid == 5){
                    tabLayoutChrono.setVisibility(View.GONE);
                    tabLayoutChronoRow.setVisibility(View.VISIBLE);
                    tabLayoutTelemetry.setVisibility(View.GONE);
                }
                if (counterShowGrid == 6){
                    tabLayoutChrono.setVisibility(View.GONE);
                    tabLayoutChronoRow.setVisibility(View.GONE);
                    tabLayoutTelemetry.setVisibility(View.VISIBLE);
                }
                if (counterShowGrid == 7){
                    tabLayoutChrono.setVisibility(View.GONE);
                    tabLayoutChronoRow.setVisibility(View.GONE);
                    tabLayoutTelemetry.setVisibility(View.GONE);
                    chronoView.setImageResource(R.drawable.ic_add_24dp);
                }
                if (counterShowGrid == 8){
                    tabLayoutChrono.setVisibility(View.VISIBLE);
                    tabLayoutChronoRow.setVisibility(View.VISIBLE);
                    tabLayoutTelemetry.setVisibility(View.VISIBLE);
                    counterShowGrid=0;
                    chronoView.setImageResource(R.drawable.ic_sub_24dp);
                }
            }
        });



        //Per gestire il menu in altre parti del codice
        this.menu = toolbar.getMenu();

        //Per gestire l'attraversamento dei punti FL, S1, S2, S3
        this.cp = new CrossingPoint();

        mpBest = MediaPlayer.create(getApplicationContext(), R.raw.bestlap);
        mpFL = MediaPlayer.create(getApplicationContext(), R.raw.fl);
        mpS = MediaPlayer.create(getApplicationContext(), R.raw.s);

        //tl = (TableLayout) findViewById(R.id.tableLayoutChrono);


        // Initialize the service manager

        this.controlTower = new ControlTower(getApplicationContext());
        //this.serviceManager = new ServiceManager(getApplicationContext());
        this.drone = new Drone();

        LinearLayout ll = (LinearLayout) findViewById(R.id.LinearLayout02);
        ll.setOrientation(getResources().getConfiguration().orientation);

        TextView displayChrono = (TextView) findViewById(R.id.txtChrono);
        TextView displayChronoBest = (TextView) findViewById(R.id.txtChronoBest);
        Typeface type = Typeface.createFromAsset(getAssets(),"fonts/display.ttf");
        displayChrono.setTypeface(type);
        displayChronoBest.setTypeface(type);
        displayChronoBest.setVisibility(View.GONE);


        if (mAdvertisement) {
            mInterstatial = new InterstitialAd(this);
            mInterstatial.setAdUnitId("ca-app-pub-3000614334186853/8994967324");
            AdRequest request = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .addTestDevice("8543A3E73FD08FADBBFBF31399C82C54")
                    .build();
            mInterstatial.loadAd(request);

            mInterstatial.setAdListener(new AdListener() {
                public void onAdLoaded() {
                    mInterstatial.show();
                }
            });
        }

//        String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
//        String deviceId = md5(android_id).toUpperCase();
//        Log.i("device id=",deviceId);

    }


//    public String md5(String s) {
//        try {
//            // Create MD5 Hash
//            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
//            digest.update(s.getBytes());
//            byte messageDigest[] = digest.digest();
//
//            // Create Hex String
//            StringBuffer hexString = new StringBuffer();
//            for (int i=0; i<messageDigest.length; i++)
//                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
//            return hexString.toString();
//
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        }
//        return "";
//    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
//        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
//        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
//            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
//        }

        LinearLayout ll = (LinearLayout) findViewById(R.id.LinearLayout02);
        ll.setOrientation(getResources().getConfiguration().orientation);
    }

    // This method will be called when a MessageEvent is posted (in the UI thread for Toast)
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEventPoint event) {
        String code = event.message.substring(0,event.message.indexOf(":"));
        double lat = Double.parseDouble(event.message.substring(event.message.indexOf(":")+1,event.message.indexOf(";")));
        double lon = Double.parseDouble(event.message.substring(event.message.indexOf(";")+1,event.message.length()));
        switch (code){
            case "FL":
                FL = new LatLng(lat,lon);
                break;
            case "S1":
                S1 = new LatLng(lat,lon);
                break;
            case "S2":
                S2 = new LatLng(lat,lon);
                break;
            case "S3":
                S3 = new LatLng(lat,lon);
                break;
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        EventBus.getDefault().register(this);

    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(this.drone.isConnected());
        }
        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        init += System.currentTimeMillis() - paused;
    }

    public void updateConnectedButton(Boolean isConnected) {

        MenuItem conMenuItem = menu.findItem(R.id.action_connect);

        if (isConnected) {
            conMenuItem.setTitle(getResources().getString(R.string.action_disconnect));
            menu.findItem (R.id.action_connect).getSubMenu().setGroupVisible (R.id.group_1, false);
        } else {
            conMenuItem.setTitle(getResources().getString(R.string.action_connect));
            menu.findItem (R.id.action_connect).getSubMenu().setGroupVisible (R.id.group_1, true);
        }
    }
    @Override
    public void onTowerConnected() {
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {

    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {

        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                //orientationState = this.getResources().getConfiguration().orientation;
                updateConnectedButton(this.drone.isConnected());
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                //updateVehicleMode();
                updateStateMode();
                break;

            case AttributeEvent.TYPE_UPDATED:
//                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
//                if (newDroneType.getDroneType() != this.droneType) {
//                    this.droneType = newDroneType.getDroneType();
//                    updateVehicleModesForType(this.droneType);
//                }
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateStateMode();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateAltitude();
                updateSpeed();
                break;

            case AttributeEvent.HOME_UPDATED:
                updateDistanceFromHome();
                break;
            case AttributeEvent.GPS_POSITION:
                updateGPS();
                break;
            case AttributeEvent.ATTITUDE_UPDATED:
                updateAttitude();
                break;
            default:
                break;
        }
    }



    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    protected void updateStateMode() {
            State vehicleState = this.drone.getAttribute(AttributeType.STATE);
            TextView state = (TextView) findViewById(R.id.StateValue);
            if (vehicleState.isConnected()){
                state.setText("CONNECTED");
            }
            if (vehicleState.isArmed()) {
                state.setText("ARMED");
            } else if (!vehicleState.isArmed()) {
                state.setText("DISARMED");
                START_CHRONO = false;
                if (mAdvertisement) {
                    AdRequest request = new AdRequest.Builder()
                            .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                            .addTestDevice("8543A3E73FD08FADBBFBF31399C82C54")
                            .build();
                    mInterstatial.loadAd(request);
                }
            }
            if (vehicleState.isFlying()) {
                state.setText("FLYING");
            }

            VehicleMode vehicleMode = vehicleState.getVehicleMode();
            TextView mode = (TextView) findViewById(R.id.ModeValue);
            mode.setText(vehicleMode.getLabel());
    }

    protected void updateAltitude() {
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        TextView altitude = (TextView) findViewById(R.id.altitudeValue);
        altitude.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }

    protected void updateSpeed() {
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        TextView speed = (TextView) findViewById(R.id.speedValue);
        speed.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    protected void updateDistanceFromHome() {
            Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
            double vehicleAltitude = droneAltitude.getAltitude();
            Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
            LatLong vehiclePosition = droneGps.getPosition();

            double distanceFromHome;

            if (droneGps.isValid()) {
                LatLongAlt vehicle3DPosition = new LatLongAlt(vehiclePosition.getLatitude(), vehiclePosition.getLongitude(), vehicleAltitude);
                Home droneHome = this.drone.getAttribute(AttributeType.HOME);
                distanceFromHome = distanceBetweenPoints(droneHome.getCoordinate(), vehicle3DPosition);
            } else {
                distanceFromHome = 0;
            }


        TextView distance = (TextView) findViewById(R.id.distanceValue);
        distance.setText(String.format("%3.1f", distanceFromHome) + "m");
    }


    protected void updateGPS() {
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        Attitude attitude = this.drone.getAttribute(AttributeType.ATTITUDE);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        LatLong vehiclePosition = droneGps.getPosition();

        TextView latitude = (TextView) findViewById(R.id.latitudeValue);
        TextView longitude = (TextView) findViewById(R.id.longitudeValue);
        latitude.setText(String.format("%3.6f", vehiclePosition.getLatitude()));
        longitude.setText(String.format("%3.6f", vehiclePosition.getLongitude()));

        EventBus.getDefault().post(new MessageEventGPS(vehiclePosition.getLatitude()+"@"+ vehiclePosition.getLongitude()+"!"+ attitude.getYaw()));

        if (START_CHRONO && k==0){
            String CrossPoint;

            if (prec) {
                b = new LatLng(vehiclePosition.getLatitude(), vehiclePosition.getLongitude());
                prec = false;
            }
            else
            {
                a = new LatLng(vehiclePosition.getLatitude(), vehiclePosition.getLongitude());
                prec = true;

                CrossPoint = cp.TestCrossingPoint(0,a,b,droneSpeed.getGroundSpeed(), FL.latitude, FL.longitude, velocita_minima);
                if (CrossPoint.compareTo("FL")==0)
                {
//                    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
//                    toneG.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
//                    mpFL.start();
//                    TextView lap = (TextView) findViewById(R.id.txtLap);
//                    lap.setText(lap.getText()+"\n"+("TIME FL: " + new SimpleDateFormat("mm:ss:SSS").format(new Date(time))));
                    k=MAX_STEP_AFTER_CROSS;
                    conta=true;
                    Log.i("FL:", "Toccato");
                    FLTime = time;
                    addLap();
                }

                CrossPoint = cp.TestCrossingPoint(1,a,b,droneSpeed.getGroundSpeed(), S1.latitude, S1.longitude, velocita_minima);
                if (CrossPoint.compareTo("S1")==0)
                {
//                    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
//                    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                    mpS.start();
//                    TextView lap = (TextView) findViewById(R.id.txtLap);
//                    lap.setText(lap.getText()+"\n"+("TIME S1: " + new SimpleDateFormat("mm:ss:SSS").format(new Date(time))));
                    k=MAX_STEP_AFTER_CROSS;
                    conta=true;
                    Log.i("S1:", "Toccato");
                    S1Time=time;
                }

                CrossPoint = cp.TestCrossingPoint(2,a,b,droneSpeed.getGroundSpeed(), S2.latitude, S2.longitude, velocita_minima);
                if (CrossPoint.compareTo("S2")==0)
                {
//                    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
//                    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                    mpS.start();
//                    TextView lap = (TextView) findViewById(R.id.txtLap);
//                    lap.setText(lap.getText()+"\n"+("TIME S2: " + new SimpleDateFormat("mm:ss:SSS").format(new Date(time))));
                    k=MAX_STEP_AFTER_CROSS;
                    conta=true;
                    Log.i("S2:", "Toccato");
                    S2Time=time;
                }

                CrossPoint = cp.TestCrossingPoint(3,a,b,droneSpeed.getGroundSpeed(), S3.latitude, S3.longitude, velocita_minima);
                if (CrossPoint.compareTo("S3")==0)
                {
//                    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
//                    toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                    mpS.start();
//                    TextView lap = (TextView) findViewById(R.id.txtLap);
//                    lap.setText(lap.getText()+"\n"+("TIME S3: " + new SimpleDateFormat("mm:ss:SSS").format(new Date(time))));
                    k=MAX_STEP_AFTER_CROSS;
                    conta=true;
                    Log.i("S3:", "Toccato");
                    S3Time=time;
                }
            }
        }
        if (conta && (droneSpeed.getGroundSpeed() >= velocita_minima))
            k--;
        if (k==0) {
            ImageView img = (ImageView) findViewById(R.id.imageViewBest);
            img.setBackgroundColor(Color.TRANSPARENT);
            conta = false;
        }
        Log.i("K=", "" + k);

    }

    private void addLap(){

        Laps lp = new Laps();
        if (nLap==0) {
            lp.lapsNumber = nLap;
            lp.time = time;
            lp.FL = FLTime;
            lp.S1 = S1Time;
            lp.S2 = S2Time;
            lp.S3 = S3Time;
            lp.best = false;
            lapsList.add(nLap, lp);
            mpFL.start();
        }
        else {
            lp.lapsNumber = nLap;
            lp.time = time;
            lp.FL = FLTime - lapsList.get(nLap-1).time;
            if (S1Time>0)
                lp.S1 = S1Time - lapsList.get(nLap-1).time;
            else
                lp.S1 = 0;
            if (S2Time>0)
                lp.S2 = S2Time - lapsList.get(nLap-1).time;
            else
                lp.S2 = 0;
            if (S3Time>0)
                lp.S3 = S3Time - lapsList.get(nLap-1).time;
            else
                lp.S2 = 0;
            lp.best = false;
            lapsList.add(nLap, lp);

            long min = Long.MAX_VALUE;
            int pos=0;
            for (Laps l : lapsList)
            {
                if (l.lapsNumber>0)
                {
                    if (l.FL<min) {
                        min = l.FL;
                        pos = l.lapsNumber;
                    }
                }
            }
            for (Laps l : lapsList)
            {
                if (l.lapsNumber == pos) {
                    l.best = true;
                    if (l.lapsNumber == nLap){
                        ImageView img = (ImageView) findViewById(R.id.imageViewBest);
                        img.setBackgroundColor(Color.RED);
                        TextView displayChronoBest = (TextView) findViewById(R.id.txtChronoBest);
                        displayChronoBest.setVisibility(View.VISIBLE);
                        displayChronoBest.setText("BEST: " +new SimpleDateFormat("mm:ss:SSS").format(new Date(l.FL)));
                        mpBest.start();
                    }
                    else
                        mpFL.start();
                }
                else {
                    l.best = false;
                }
            }
        }

        writeTableRow();

        S1Time = 0;
        S2Time = 0;
        S3Time = 0;
        nLap++;
    }
    private void writeTableRow(){
        resetTableRow();

        TableLayout ll = (TableLayout) findViewById(R.id.tableLayoutChronoRow);


        for (Laps l : lapsList) {
            TableRow row = new TableRow(this);
            TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
            row.setBackgroundColor(Color.parseColor("#AA676767"));
            row.setLayoutParams(lp);
            ImageView img1 = new ImageView(this);
            img1.setImageResource(R.drawable.ic_best);
            ImageView img2 = new ImageView(this);
            img2.setImageResource(R.drawable.ic_not_best);
            TextView tv0 = new TextView(this);
            tv0.setTextSize(10);
            tv0.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tv0.setTextColor(Color.WHITE);
            tv0.setText("" + l.lapsNumber);
            row.addView(tv0);
            TextView tv1 = new TextView(this);
            tv1.setTextSize(10);
            tv1.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tv1.setTextColor(Color.WHITE);
            tv1.setText(new SimpleDateFormat("mm:ss:SSS").format(new Date(l.S1)));
            row.addView(tv1);
            TextView tv2 = new TextView(this);
            tv2.setTextSize(10);
            tv2.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tv2.setTextColor(Color.WHITE);
            tv2.setText(new SimpleDateFormat("mm:ss:SSS").format(new Date(l.S2)));
            row.addView(tv2);
            TextView tv3 = new TextView(this);
            tv3.setTextSize(10);
            tv3.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tv3.setTextColor(Color.WHITE);
            tv3.setText(new SimpleDateFormat("mm:ss:SSS").format(new Date(l.S3)));
            row.addView(tv3);
            TextView tv4 = new TextView(this);
            tv4.setTextSize(10);
            tv4.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tv4.setTextColor(Color.WHITE);
            tv4.setText(new SimpleDateFormat("mm:ss:SSS").format(new Date(l.FL)));
            row.addView(tv4);
            if (l.best) {
                row.addView(img1);
            }
            else
                row.addView(img2);
            ll.addView(row, l.lapsNumber+1);

            final ScrollView scroll = (ScrollView) this.findViewById(R.id.scrollViewLaps);
            scroll.post(new Runnable() {
                @Override
                public void run() {

                    scroll.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    }

    private void resetTableRow(){
        TableLayout ll = (TableLayout) findViewById(R.id.tableLayoutChronoRow);
        int count = ll.getChildCount();
        for (int i = 1; i < count; i++) {
            View child = ll.getChildAt(i);
            if (child instanceof TableRow) ((ViewGroup) child).removeAllViews();
        }
    }
    protected void updateAttitude() {
        Attitude attitude = drone.getAttribute(AttributeType.ATTITUDE);
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        float headingCurrect;
        float rollCurrect;
        float pitchCurrect;

        if (attitude == null) {
            headingCurrect = 0;
            rollCurrect = 0;
            pitchCurrect = 0;
        } else {
            headingCurrect = (float) attitude.getYaw();
            rollCurrect = (float) attitude.getRoll();
            pitchCurrect = (float) attitude.getPitch();
        }

        TextView heading = (TextView) findViewById(R.id.HeadingValue);
        TextView roll = (TextView) findViewById(R.id.RollValue);
        TextView pitch = (TextView) findViewById(R.id.PitchValue);

        heading.setText(String.format("%3.1f", headingCurrect) + "°");
        roll.setText(String.format("%3.1f", rollCurrect) + "°");
        pitch.setText(String.format("%3.1f", pitchCurrect) + "°");

        if (!START_CHRONO && pitchCurrect < -MIN_PITCH && vehicleState.isFlying()) {
            init = System.currentTimeMillis();
            handler.post(updater);
            START_CHRONO = true;
        }
        if (START_CHRONO && !vehicleState.isArmed()) {
            START_CHRONO = false;
            nLap=0;
        }
    }

//    private void addLap() {
//
//        TableRow tr_head = new TableRow(this);
//        tr_head.setId(0);
//        tl.addView(tr_head, new TableLayout.LayoutParams(
//                TableRow.LayoutParams.MATCH_PARENT,
//                TableRow.LayoutParams.WRAP_CONTENT));
//    }

    protected double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy  = pointA.getLongitude() - pointB.getLongitude();
        double dz = pointA.getAltitude() - pointB.getAltitude();
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    @Override
    public void onDroneConnectionFailed(ConnectionResult result) {

    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();

        // Handle item selection
        switch (id) {
            case R.id.usbConnection:
                newConnection("USB");
                return true;
            case R.id.udpConnection:
                newConnection("UDP");
                return true;
            case R.id.action_connect:
                newConnection("DIS");
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    public void newConnection(String mode) {
        if (this.drone.isConnected() && mode.compareTo("DIS")==0) {
            this.drone.disconnect();
            START_CHRONO = false;
            nLap=0;
        }
        if (!this.drone.isConnected() && (mode.compareTo("USB")==0 || mode.compareTo("UDP")==0)) {
            Bundle extraParams = new Bundle();
            ConnectionParameter connectionParams = null;

            switch (mode)
            {
                case "UDP":
                    extraParams.putInt(ConnectionType.EXTRA_UDP_SERVER_PORT, 14550); // Set default port to 14550
                    connectionParams = new ConnectionParameter(ConnectionType.TYPE_UDP, extraParams, null);
                    break;
                case "USB":
                    extraParams.putInt(ConnectionType.EXTRA_USB_BAUD_RATE, 57600); // Set default baud rate to 57600
                    connectionParams = new ConnectionParameter(ConnectionType.TYPE_USB, extraParams, null);
                    break;
            }
            this.drone.connect(connectionParams);
            lapsList.clear(); // All laps will be deleted
            resetTableRow(); // All old Rows will be deleted
            TextView displayChrono = (TextView) findViewById(R.id.txtChrono);
            displayChrono.setText("TIME: " + new SimpleDateFormat("mm:ss:SSS").format(new Date(0)));
            TextView displayChronoBest = (TextView) findViewById(R.id.txtChronoBest);
            displayChronoBest.setVisibility(View.GONE);
            ImageView img = (ImageView) findViewById(R.id.imageViewBest);
            img.setBackgroundColor(Color.TRANSPARENT);
            k=0;
        }
    }
}
