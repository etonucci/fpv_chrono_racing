package com.emanueletonucci.etchronorace;

import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import static android.content.Context.LOCATION_SERVICE;


/**
 * Created by Emanuele on 23/09/2016.
 */

public class TrackFragment extends Fragment implements GoogleMap.OnMapLongClickListener,GoogleMap.OnMapClickListener,GoogleMap.OnMarkerDragListener {

    private static final String ARG_SECTION_NUMBER = "section_number";

    MapView mMapView;
    private GoogleMap googleMap;
    private Marker marker_drone;
    private Marker marker_drone_fl;
    private Marker marker_drone_S1;
    private Marker marker_drone_S2;
    private Marker marker_drone_S3;
    private Marker marker_drone_site;
    private float mZoom = 18;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_track, container, false);

        mMapView = (MapView) rootView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {

                googleMap = mMap;
                googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

                googleMap.setOnMarkerDragListener(TrackFragment.this);
                googleMap.setOnMapLongClickListener(TrackFragment.this);
                googleMap.setOnMapClickListener(TrackFragment.this);

                /*

                The logic behind how the markers are anchored is something like this:


                        0,0      0.5,0.0        1,0
                        *-----+-----+-----+-----*
                        |     |     |     |     |
                        |     |     |     |     |
                  0,0.5 +-----+-----+-----+-----+ 1,0.5
                        |     |     |   X |     |           (U, V) = (0.7, 0.6)
                        |     |     |     |     |
                        *-----+-----+-----+-----*
                        0,1      0.5,1.0        1,1

                Also take into consideration that based on your bitmap resource,
                it could be positioned a little different than you would expect,
                because they actually approximate to the nearest snap position.
                So in the example above, your anchor points will snap to this position:
                 *-----+-----+-----+-----*
                 |     |     |     |     |
                 |     |     |     |     |
                 +-----+-----+-----X-----+   (X, Y) = (3, 1)
                 |     |     |     |     |
                 |     |     |     |     |
                 *-----+-----+-----+-----*

                 */

                // read last good posistion
                LatLng eq = getLocation();

                if (eq == null)
                    eq = new LatLng(43.832806, 13.021026);


                MarkerOptions mo = new MarkerOptions().anchor(0.5f, 0.5f).position(eq).title("SITE").snippet("@" + String.format("%.6f", eq.latitude) + ";" + String.format("%.6f", eq.longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.user_location));
                marker_drone_site = googleMap.addMarker(mo);
                marker_drone_site.setTag("SITE");

                eq = new LatLng(eq.latitude+0.0003, eq.longitude+0.00005);
                mo = new MarkerOptions().draggable(true).anchor(0.5f, 0.5f).position(eq).title("FINISH LINE").snippet("@" + String.format("%.6f", eq.latitude) + ";" + String.format("%.6f", eq.longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_wp_fl));
                marker_drone_fl = googleMap.addMarker(mo);
                marker_drone_fl.setTag("FL");
                //Send to MainActivity FL otherwise it will be null
                EventBus.getDefault().post(new MessageEventPoint("FL:"+eq.latitude+";"+eq.longitude));

                eq = new LatLng(eq.latitude, eq.longitude+0.0005);
                mo = new MarkerOptions().draggable(true).anchor(0.5f, 0.5f).position(eq).title("SPLIT 1").snippet("@" + String.format("%.6f", eq.latitude) + ";" + String.format("%.6f", eq.longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_wp_s1));
                marker_drone_S1 = googleMap.addMarker(mo);
                marker_drone_S1.setTag("S1");
                //Send to MainActivity S1 otherwise it will be null
                EventBus.getDefault().post(new MessageEventPoint("S1:"+eq.latitude+";"+eq.longitude));

                eq = new LatLng(eq.latitude-0.0005, eq.longitude);
                mo = new MarkerOptions().draggable(true).anchor(0.5f, 0.5f).position(eq).title("SPLIT 2").snippet("@" + String.format("%.6f", eq.latitude) + ";" + String.format("%.6f", eq.longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_wp_s2));
                marker_drone_S2 = googleMap.addMarker(mo);
                marker_drone_S2.setTag("S2");
                //Send to MainActivity S1 otherwise it will be null
                EventBus.getDefault().post(new MessageEventPoint("S2:"+eq.latitude+";"+eq.longitude));

                eq = new LatLng(eq.latitude, eq.longitude-0.0005);
                mo = new MarkerOptions().draggable(true).anchor(0.5f, 0.5f).position(eq).title("SPLIT 3").snippet("@" + String.format("%.6f", eq.latitude) + ";" + String.format("%.6f", eq.longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_wp_s3));
                marker_drone_S3 = googleMap.addMarker(mo);
                marker_drone_S3.setTag("S3");
                //Send to MainActivity S1 otherwise it will be null
                EventBus.getDefault().post(new MessageEventPoint("S3:"+eq.latitude+";"+eq.longitude));

                eq = new LatLng(eq.latitude+0.0002, eq.longitude-0.00001);
                mo = new MarkerOptions().anchor(0.5f, 0.5f).position(eq).title("YOUR DRONE").snippet("@" + String.format("%.6f", eq.latitude) + ";" + String.format("%.6f", eq.longitude)).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_drone));
                marker_drone = googleMap.addMarker(mo);
                marker_drone.setTag("DRONE");
                marker_drone.setZIndex(1000);

                //For zooming automatically to the location of the marker
                CameraPosition cameraPosition = new CameraPosition.Builder().target(eq).zoom(mZoom).build();
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                if (googleMap != null && getActivity() != null) {
                    UiSettings settings = TrackFragment.this.googleMap.getUiSettings();
                    settings.setZoomControlsEnabled(false);
                    settings.setMyLocationButtonEnabled(false);
                    settings.setMapToolbarEnabled(false);
                    settings.setCompassEnabled(true);
                    settings.setZoomGesturesEnabled(true);
                    settings.setAllGesturesEnabled(true);
                }
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(marker_drone_site.getPosition());
                builder.include(marker_drone_fl.getPosition());
                builder.include(marker_drone.getPosition());


//                LatLngBounds bounds = builder.build();
//
//                int width = getResources().getDisplayMetrics().widthPixels;
//                int height = getResources().getDisplayMetrics().heightPixels;
//                int padding = (int) (width * 0.10); // offset from edges of the map 12% of screen
//
//                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
//
//                mMap.animateCamera(cu);

           }
        });

        mMapView.onResume();

        return rootView;
    }

    private LatLng getLocation()
    {

        // create class object
        GPSTracker gps = new GPSTracker(getContext());

        // check if GPS enabled
        if(gps.canGetLocation()){

            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();

            // \n is for new line
            Toast.makeText(getContext(), "Your Location is:\nLat: " + latitude + " Long: " + longitude, Toast.LENGTH_LONG).show();
        }else{
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }
        Double lat,lon;
        try {
            lat = gps.getLatitude();
            lon = gps.getLongitude();
            gps.stopUsingGPS();
            return new LatLng(lat, lon);
        }
        catch (NullPointerException e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onMarkerDrag(Marker arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMarkerDragEnd(Marker arg0) {
        // TODO Auto-generated method stub
        LatLng dragPosition = arg0.getPosition();
        double dragLat = dragPosition.latitude;
        double dragLong = dragPosition.longitude;
        Log.i("info", "on drag end :" + dragLat + " dragLong :" + dragLong);
        //Toast.makeText(getContext(), "Marker Dragged..!", Toast.LENGTH_LONG).show();
        //marker_drone_fl.setPosition(dragPosition);
        if (arg0.getTag().equals("FL"))
        {

            marker_drone_fl.setSnippet("@"+String.format("%.6f", dragLat) + ";" + String.format("%.6f", dragLong));
            EventBus.getDefault().post(new MessageEventPoint("FL:"+dragLat+";"+dragLong));
        }
        if (arg0.getTag().equals("S1"))
        {
            marker_drone_S1.setSnippet("@"+String.format("%.6f", dragLat) + ";" + String.format("%.6f", dragLong));
            EventBus.getDefault().post(new MessageEventPoint("S1:"+dragLat+";"+dragLong));
        }
        if (arg0.getTag().equals("S2"))
        {
            marker_drone_S2.setSnippet("@"+String.format("%.6f", dragLat) + ";" + String.format("%.6f", dragLong));
            EventBus.getDefault().post(new MessageEventPoint("S2:"+dragLat+";"+dragLong));
        }
        if (arg0.getTag().equals("S3"))
        {
            marker_drone_S3.setSnippet("@"+String.format("%.6f", dragLat) + ";" + String.format("%.6f", dragLong));
            EventBus.getDefault().post(new MessageEventPoint("S3:"+dragLat+";"+dragLong));
        }

    }

    @Override
    public void onMarkerDragStart(Marker arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onMapClick(LatLng arg0) {
        // TODO Auto-generated method stub
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(arg0));
    }

    @Override
    public void onMapLongClick(LatLng arg0) {
        // TODO Auto-generated method stub

        //create new marker when user long clicks
//        googleMap.addMarker(new MarkerOptions()
//                .position(arg0)
//                .draggable(true));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_map_hybrid){
            this.googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            return true;
        }
        if(id == R.id.action_map_normal){
            this.googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            return true;
        }
        if(id == R.id.action_map_satellite){
            this.googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            return true;
        }
        if(id == R.id.action_map_terrain){
            this.googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static TrackFragment newInstance(int sectionNumber) {
        TrackFragment fragment = new TrackFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEventGPS event) {
        String mes = event.message.toString();
        double lat = 0;
        double lng = 0;
        float yaw = 0;

        lat = Double.parseDouble(mes.substring(0,mes.indexOf("@")));
        lng = Double.parseDouble(mes.substring(mes.indexOf("@")+1,mes.indexOf("!")));
        yaw = Float.parseFloat(mes.substring(mes.indexOf("!")+1,mes.length()));

        LatLng current_latlng = new LatLng(lat, lng);
        marker_drone.setPosition(current_latlng);
        marker_drone.setAnchor(0.5f,0.5f);
        marker_drone.setRotation(yaw);
        marker_drone.setSnippet("@"+String.format("%.6f", lat) + ";" + String.format("%.6f", lng));

        googleMap.animateCamera(CameraUpdateFactory.newLatLng(current_latlng));

    }


    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }
}
