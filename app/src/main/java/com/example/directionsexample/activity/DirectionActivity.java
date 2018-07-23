package com.example.directionsexample.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.directionsexample.R;
import com.example.directionsexample.adapter.CustomInfoWindowAdapter;
import com.example.directionsexample.adapter.PlaceAutoCompleteAdapter;
import com.example.directionsexample.model.PlaceInfo;
import com.example.directionsexample.route.AbstractRouting;
import com.example.directionsexample.route.Route;
import com.example.directionsexample.route.RouteException;
import com.example.directionsexample.route.Routing;
import com.example.directionsexample.route.RoutingListener;
import com.example.directionsexample.util.Util;
import com.example.directionsexample.view.ClearableAutoCompleteTextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Created by 2149 on 27-12-2017.
 */

public class DirectionActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.OnConnectionFailedListener, View.OnClickListener, GoogleMap.OnPolylineClickListener,
        RoutingListener, GoogleApiClient.ConnectionCallbacks, LocationListener {

    private static final String TAG = "DirectionActivity ::";

    public final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private final float DEFAULT_ZOOM = 15f;
    private final int timeInterval = 3000;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(new LatLng(-40, -168), new LatLng(71, 136));

    //widgets
    private ClearableAutoCompleteTextView starting;
    private ClearableAutoCompleteTextView destination;
    private RelativeLayout direction_lay;
    private ImageView img_car, img_cycle, img_walk, img_direction;

    private TextView text_duration, text_distance;
    private android.support.v7.app.AlertDialog dialog;


    //variables
    private int screenHeight, screenWidth;
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlaceAutoCompleteAdapter mPlaceAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private PlaceInfo mPlace;
    protected LatLng start;
    protected LatLng end;

    private ProgressDialog progressDialog;
    private List<Polyline> polylines;
    private List<Route> routes;

    //Default Values
    private AbstractRouting.TravelMode travelMode = AbstractRouting.TravelMode.DRIVING;

    private boolean isDirection, isLocationEnabled, isConnected;
    LocationManager locationManager;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        isConnected = Util.Operations.isOnline(DirectionActivity.this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        } else {
            if (!isLocationEnabled()) {
                locationRequestDialog();
            }
        }
        bindViews();

    }


    @Override
    protected void onResume() {

        super.onResume();


    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }


        mMap = googleMap;

        mMap.getUiSettings().setZoomGesturesEnabled(false);
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);
        mMap.setBuildingsEnabled(true);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        init();
        if (mLocationPermissionsGranted) {
            getDeviceLocation();
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, timeInterval, 0, this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, timeInterval, 0, this);
    }

    @Override
    public void onClick(View v) {
        // Set listeners for click events.
        switch (v.getId()) {

            case R.id.img_car:
                mMap.setOnPolylineClickListener(this);
                travelMode = AbstractRouting.TravelMode.DRIVING;
                route();
                break;
            case R.id.img_cycle:
                mMap.setOnPolylineClickListener(this);
                travelMode = AbstractRouting.TravelMode.BIKING;
                route();
                break;
            case R.id.img_walk:
                mMap.setOnPolylineClickListener(this);
                travelMode = AbstractRouting.TravelMode.WALKING;
                route();
                break;
            case R.id.img_direction:
                mMap.setOnPolylineClickListener(this);
                travelMode = AbstractRouting.TravelMode.DRIVING;
                route();
                break;
            case R.id.action_direction:
                if (!isDirection) {
                    mMap.clear();
                    direction_lay.setVisibility(View.VISIBLE);
                    isDirection = true;
                } else {
                    setDirectionLayoutToHide();
                    getDeviceLocation();
                    isDirection = false;
                }
                break;

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        isLocationEnabled = true;
                        if (mMap == null)
                            initMap();
                    }

                } else {
                    isLocationEnabled = false;

                }
                return;
            }

        }
    }


    @Override
    public void onPolylineClick(Polyline polyline) {
        for (int i = 0; i < polylines.size(); i++) {

            if (polyline.equals(polylines.get(i))) {
                polyline.setColor(getResources().getColor(R.color.route_select));
                updateTimeAndDistance(routes.get(i).getDistanceText(), routes.get(i).getDurationText());
            } else {
                polylines.get(i).setColor(getResources().getColor(R.color.route_unselect));
            }
        }
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        // The Routing request failed
        progressDialog.dismiss();
        if (e != null) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingStart() {


    }

    @Override
    public void onRoutingSuccess(List<Route> route, int shortestRouteIndex) {
        progressDialog.dismiss();
        CameraUpdate center = CameraUpdateFactory.newLatLng(start);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
        updateTransitMode();
        mMap.moveCamera(center);

        if (routes != null) {
            routes.clear();
        }
        routes = route;

        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {

            Route route_final = route.get(i);

            PolylineOptions polyOptions = new PolylineOptions();
            if (i == shortestRouteIndex) {
                polyOptions.color(getResources().getColor(R.color.route_select)).geodesic(true);
                updateTimeAndDistance(route_final.getDistanceText(), route_final.getDurationText());
            } else {
                polyOptions.color(getResources().getColor(R.color.route_unselect));
            }
            // polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10);
            polyOptions.addAll(route_final.getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polyline.setClickable(true);
            polylines.add(polyline);

            // Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
        }

        // Start marker
        MarkerOptions options = new MarkerOptions();
        options.position(start);
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_orange_24dp));
        mMap.addMarker(options);

        // End marker
        options = new MarkerOptions();
        options.position(end);
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_orange_24dp));
        mMap.addMarker(options);

    }

    @Override
    public void onRoutingCancelled() {
        Log.i(TAG, "Routing was cancelled.");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate center = CameraUpdateFactory.newLatLng(latLng);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);

        mMap.moveCamera(center);
        mMap.animateCamera(zoom);

        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(getCompleteAddressString(latLng.latitude, latLng.longitude))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_orange_24dp));
        mMap.addMarker(options);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private void bindViews() {

        //change status bar color ..
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            //      WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }

        //getting height and width
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;

        toolBarSetup();

        starting = (ClearableAutoCompleteTextView) findViewById(R.id.input_source);
        starting.getLayoutParams().height = screenHeight / 12;
        destination = (ClearableAutoCompleteTextView) findViewById(R.id.input_destination);
        destination.getLayoutParams().height = screenHeight / 12;

        direction_lay = (RelativeLayout) findViewById(R.id.direction_lay);
        direction_lay.setVisibility(View.GONE);

        img_car = (ImageView) findViewById(R.id.img_car);
        img_car.setOnClickListener(this);
        img_car.getLayoutParams().height = screenWidth / 10;
        img_car.getLayoutParams().width = screenWidth / 10;

        img_cycle = (ImageView) findViewById(R.id.img_cycle);
        img_cycle.setOnClickListener(this);
        img_cycle.getLayoutParams().height = screenWidth / 10;
        img_cycle.getLayoutParams().width = screenWidth / 10;

        img_walk = (ImageView) findViewById(R.id.img_walk);
        img_walk.setOnClickListener(this);
        img_walk.getLayoutParams().height = screenWidth / 10;
        img_walk.getLayoutParams().width = screenWidth / 10;

        img_direction = (ImageView) findViewById(R.id.img_direction);
        img_direction.setOnClickListener(this);
        img_direction.getLayoutParams().height = screenWidth / 6;
        img_direction.getLayoutParams().width = screenWidth / 6;

        text_distance = (TextView) findViewById(R.id.text_distance);
        text_duration = (TextView) findViewById(R.id.text_duration);

        dialog = new android.support.v7.app.AlertDialog.Builder(this).setTitle("Enable Location")
                .setMessage("Location access is required to show your location.")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        finish();
                    }
                }).create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCanceledOnTouchOutside(false);

        findViewById(R.id.action_direction).setOnClickListener(this);

    }

    //toolbar setUp
    private void toolBarSetup() {

        Toolbar topToolBar = (Toolbar) findViewById(R.id.toolbar);
        topToolBar.setTitle("Directions");

        setSupportActionBar(topToolBar);
        // getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void updateTimeAndDistance(String distance, String duration) {
        text_distance.setText("Distance : " + distance);
        text_duration.setText("Duration : " + duration);
    }

    private void setDirectionLayoutToHide() {

        if (direction_lay.getVisibility() == View.VISIBLE) {
            direction_lay.setVisibility(View.GONE);
            starting.setText("");
            destination.setText("");
            updateTimeAndDistance("", "");
            img_car.setColorFilter(getApplicationContext().getResources().getColor(R.color.un_select), PorterDuff.Mode.SRC_IN);
            img_cycle.setColorFilter(getApplicationContext().getResources().getColor(R.color.un_select), PorterDuff.Mode.SRC_IN);
            img_walk.setColorFilter(getApplicationContext().getResources().getColor(R.color.un_select), PorterDuff.Mode.SRC_IN);
            img_direction.setColorFilter(getApplicationContext().getResources().getColor(R.color.select), PorterDuff.Mode.SRC_IN);
            mMap.clear();
        }
    }

    private void updateTransitMode() {

        img_direction.setColorFilter(getApplicationContext().getResources().getColor(R.color.select), PorterDuff.Mode.SRC_IN);
        if (travelMode == AbstractRouting.TravelMode.DRIVING) {
            img_car.setColorFilter(getApplicationContext().getResources().getColor(R.color.select), PorterDuff.Mode.SRC_IN);
            img_cycle.setColorFilter(getApplicationContext().getResources().getColor(R.color.un_select), PorterDuff.Mode.SRC_IN);
            img_walk.setColorFilter(getApplicationContext().getResources().getColor(R.color.un_select), PorterDuff.Mode.SRC_IN);
        } else if (travelMode == AbstractRouting.TravelMode.BIKING) {
            img_car.setColorFilter(getApplicationContext().getResources().getColor(R.color.un_select), PorterDuff.Mode.SRC_IN);
            img_cycle.setColorFilter(getApplicationContext().getResources().getColor(R.color.select), PorterDuff.Mode.SRC_IN);
            img_walk.setColorFilter(getApplicationContext().getResources().getColor(R.color.un_select), PorterDuff.Mode.SRC_IN);
        } else if (travelMode == AbstractRouting.TravelMode.WALKING) {
            img_car.setColorFilter(getApplicationContext().getResources().getColor(R.color.un_select), PorterDuff.Mode.SRC_IN);
            img_cycle.setColorFilter(getApplicationContext().getResources().getColor(R.color.un_select), PorterDuff.Mode.SRC_IN);
            img_walk.setColorFilter(getApplicationContext().getResources().getColor(R.color.select), PorterDuff.Mode.SRC_IN);
        }
    }

    private void hideSoftKeyboard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
   /* @Override
    public void onPause() {
        super.onPause();
        mGoogleApiClient.stopAutoManage(this);
        mGoogleApiClient.disconnect();
    }*/
    private void init() {
        Log.d(TAG, "init: initializing");

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();

        polylines = new ArrayList<>();

        mPlaceAutocompleteAdapter = new PlaceAutoCompleteAdapter(this, mGoogleApiClient,
                LAT_LNG_BOUNDS, null);

        starting.setAdapter(mPlaceAutocompleteAdapter);
        destination.setAdapter(mPlaceAutocompleteAdapter);


        starting.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final AutocompletePrediction item = mPlaceAutocompleteAdapter.getItem(position);
                final String placeId = item.getPlaceId();

                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (!places.getStatus().isSuccess()) {
                            // Request did not complete successfully
                            places.release();
                            return;
                        }
                        // Get the Place object from the buffer.
                        final Place place = places.get(0);

                        start = place.getLatLng();
                        hideSoftKeyboard();
                    }
                });

            }
        });

        destination.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final AutocompletePrediction item = mPlaceAutocompleteAdapter.getItem(position);
                final String placeId = item.getPlaceId();

                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(mGoogleApiClient, placeId);
                placeResult.setResultCallback(new ResultCallback<PlaceBuffer>() {
                    @Override
                    public void onResult(PlaceBuffer places) {
                        if (!places.getStatus().isSuccess()) {
                            // Request did not complete successfully
                            places.release();
                            return;
                        }
                        // Get the Place object from the buffer.
                        final Place place = places.get(0);

                        end = place.getLatLng();
                        hideSoftKeyboard();
                    }
                });

            }
        });


        starting.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int startNum, int before, int count) {
                if (start != null)
                    start = null;

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        destination.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (end != null)
                    end = null;

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location  " + mLocationPermissionsGranted);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();

                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM);

                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(DirectionActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }


    private void moveCamera(LatLng latLng, float zoom, PlaceInfo placeInfo) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        mMap.clear();

        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(DirectionActivity.this));

        if (placeInfo != null) {
            try {
                String snippet = "Address: " + placeInfo.getAddress() + "\n" +
                        "Phone Number: " + placeInfo.getPhoneNumber() + "\n" +
                        "Website: " + placeInfo.getWebsiteUri() + "\n" +
                        "Price Rating: " + placeInfo.getRating() + "\n";

                MarkerOptions options = new MarkerOptions()
                        .position(latLng)
                        .title(placeInfo.getName())
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_orange_24dp))
                        .snippet(snippet);
                mMap.addMarker(options);

            } catch (NullPointerException e) {
                Log.e(TAG, "moveCamera: NullPointerException: " + e.getMessage());
            }
        } else {
            mMap.addMarker(new MarkerOptions().position(latLng));
        }

    }

    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));


        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(getCompleteAddressString(latLng.latitude, latLng.longitude))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mMap.addMarker(options);


    }

    public void route() {
        if (start == null || end == null) {
            if (start == null) {
                if (starting.getText().length() > 0) {
                    starting.setError("Choose location from dropdown.");
                } else {
                    Toast.makeText(this, "Please choose a starting point.", Toast.LENGTH_SHORT).show();
                }
            }
            if (end == null) {
                if (destination.getText().length() > 0) {
                    destination.setError("Choose location from dropdown.");
                } else {
                    Toast.makeText(this, "Please choose a destination.", Toast.LENGTH_SHORT).show();
                }
            }
            mMap.clear();
            starting.setText("");
            destination.setText("");
            updateTimeAndDistance("", "");
            img_car.setColorFilter(getApplicationContext().getResources().getColor(R.color.un_select), PorterDuff.Mode.SRC_IN);
            img_cycle.setColorFilter(getApplicationContext().getResources().getColor(R.color.un_select), PorterDuff.Mode.SRC_IN);
            img_walk.setColorFilter(getApplicationContext().getResources().getColor(R.color.un_select), PorterDuff.Mode.SRC_IN);
            img_direction.setColorFilter(getApplicationContext().getResources().getColor(R.color.select), PorterDuff.Mode.SRC_IN);
        } else {
            progressDialog = ProgressDialog.show(this, "Please wait.",
                    "Fetching route information.", true);
            Routing routing = new Routing.Builder()
                    .travelMode(travelMode)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(start, end)
                    .key(getString(R.string.google_maps_places_key))
                    .build();
            routing.execute();
        }
    }


    /*
        --------------------------- google places API autocomplete suggestions -----------------
     */

    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

            hideSoftKeyboard();

            final AutocompletePrediction item = mPlaceAutocompleteAdapter.getItem(i);
            final String placeId = item.getPlaceId();

            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.d(TAG, "onResult: Place query did not complete successfully: " + places.getStatus().toString());
                places.release();
                return;
            }
            final Place place = places.get(0);

            try {
                mPlace = new PlaceInfo();
                mPlace.setName(place.getName().toString());
                Log.d(TAG, "onResult: name: " + place.getName());
                mPlace.setAddress(place.getAddress().toString());
                Log.d(TAG, "onResult: address: " + place.getAddress());
//                mPlace.setAttributions(place.getAttributions().toString());
//                Log.d(TAG, "onResult: attributions: " + place.getAttributions());
                mPlace.setId(place.getId());
                Log.d(TAG, "onResult: id:" + place.getId());
                mPlace.setLatlng(place.getLatLng());
                Log.d(TAG, "onResult: latlng: " + place.getLatLng());
                mPlace.setRating(place.getRating());
                Log.d(TAG, "onResult: rating: " + place.getRating());
                mPlace.setPhoneNumber(place.getPhoneNumber().toString());
                Log.d(TAG, "onResult: phone number: " + place.getPhoneNumber());
                mPlace.setWebsiteUri(place.getWebsiteUri());
                Log.d(TAG, "onResult: website uri: " + place.getWebsiteUri());

                Log.d(TAG, "onResult: place: " + mPlace.toString());
            } catch (NullPointerException e) {
                Log.e(TAG, "onResult: NullPointerException: " + e.getMessage());
            }

            moveCamera(new LatLng(place.getViewport().getCenter().latitude,
                    place.getViewport().getCenter().longitude), DEFAULT_ZOOM, mPlace);

            places.release();
        }
    };

    private void initMap() {
        Log.e(TAG, "" + isServiceOk());
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        if (isServiceOk()) {
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);

        }
    }


    public boolean isServiceOk() {
        Log.d(TAG, "is ServiceOk: checking Google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(DirectionActivity.this);

        if (available == ConnectionResult.SUCCESS) {
            Log.d(TAG, "is ServiceOk: checking Google services is success working..");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            Log.d(TAG, "is ServiceOk: an error occured..");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(DirectionActivity.this, available, 9001);
            dialog.show();

        } else {
            Log.d(TAG, "You can't make map request");
        }
        return false;
    }

    private boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {

                showPermissionDialoge();

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            if (mMap == null)
                initMap();
            return true;
        }
    }

    private void locationRequestDialog() {
        if (!dialog.isShowing()) {
            dialog.show();
        }
    }

    private void showPermissionDialoge() {
        // Should we show an explanation?
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

            android.support.v7.app.AlertDialog dialog = new android.support.v7.app.AlertDialog.Builder(this)
                    .setTitle("Location Enable")
                    .setMessage("Please Turn On Your Location")
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Prompt the user once explanation has been shown
                            ActivityCompat.requestPermissions(DirectionActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    MY_PERMISSIONS_REQUEST_LOCATION);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (isLocationEnabled && mMap == null) {
            initMap();
        }
        return isLocationEnabled;
    }


    private String getCompleteAddressString(double LATITUDE, double LONGITUDE) {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder("");

                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
                }
                strAdd = strReturnedAddress.toString();
                Log.w("location address", strReturnedAddress.toString());
            } else {
                Log.w("location address", "No Address returned!");
                strAdd = "Unable get Address!";
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("location address", "Canont get Address!");
            strAdd = "Unable get Address!";
        }
        return strAdd;
    }

}
