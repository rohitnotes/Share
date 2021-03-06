package com.sharesmile.share.gps;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.sharesmile.share.core.Config;
import com.sharesmile.share.core.Constants;
import com.sharesmile.share.utils.Logger;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


/**
 * Created by ankitm on 22/12/16.
 */
public class GoogleLocationTracker implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, GpsStatus.Listener {

    private static final String TAG = "GoogleLocationTracker";

    private static GoogleLocationTracker uniqueInstance;

    private Context appContext;
    private GoogleApiClient googleApiClient;
    private boolean isActive = false;
    private LocationRequest locationRequest;
    private Location currentLocation;
    private State state;
    private Handler handler;
    Set<WeakReference<Listener>> listeners;
    private LocationManager locationManager;
    private int numSatellitesConnected;

    private GoogleLocationTracker(Context appContext) {
        this.appContext = appContext;
        this.handler = new Handler();
        this.listeners = new HashSet<>();
        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // All required permissions available
            state = State.PERMISSIONS_GRANTED;
            locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
            locationManager.addGpsStatusListener(this);
            connectToLocationServices();
        } else {
            //Need to get permissions
            state = State.NEEDS_PERMISSION;
        }
        appContext.registerReceiver(gpsProviderChangedReceiver,
                new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
    }

    /**
     Throws IllegalStateException if this class is not initialized

     @return unique GoogleLocationTracker instance
     */
    public static GoogleLocationTracker getInstance() {
        if (uniqueInstance == null) {
            throw new IllegalStateException(
                    "GoogleLocationTracker is not initialized, call initialize(applicationContext) " +
                            "static method first");
        }
        return uniqueInstance;
    }

    /**
     Initialize this class using application Context,
     should be called once in the beginning by any application Component

     @param appContext application context
     */
    public static void initialize(Context appContext) {
        if (appContext == null) {
            throw new NullPointerException("Provided application context is null");
        }
        if (uniqueInstance == null) {
            synchronized (GoogleLocationTracker.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new GoogleLocationTracker(appContext);
                }
            }
        }
    }

    public void registerForWorkout(Listener listener){
        setWorkoutInProgress(true);
        register(listener);
    }

    public void register(Listener listener){
        Logger.i(TAG, "register");
        if (listener != null){
            boolean toAdd = true;
            for (WeakReference<Listener> reference : listeners){
                if (listener.equals(reference.get())){
                    toAdd = false;
                }
            }
            if (toAdd){
                Logger.d(TAG, "Will register this listener for location updates");
                WeakReference<Listener> reference = new WeakReference<>(listener);
                listeners.add(reference);
                if (state == State.FETCHING_LOCATION){
                    listener.onLocationTrackerReady();
                }
            }
            if (state != State.FETCHING_LOCATION){
                startLocationTracking(true);
            }
        }
    }

    public void unregisterWorkout(Listener listener){
        setWorkoutInProgress(false);
        unregister(listener);
    }

    public void unregister(Listener listener){
        Logger.i(TAG, "unregister");
        if (listener != null){
            Iterator<WeakReference<Listener>> iterator = listeners.iterator();
            while (iterator.hasNext()){
                WeakReference<Listener> reference = iterator.next();
                if (reference.get() != null){
                    if (listener.equals(reference.get())){
                        iterator.remove();
                    }
                }else {
                    iterator.remove();
                }
            }
        }
    }

    public void stopLocationTracking(){
        Logger.d(TAG, "stopLocationTracking");
        if (workoutInProgess() == false){
            // Will stop location updates only if Workout is not in progress
            if (state == State.FETCHING_LOCATION){
                LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
                state = State.LOCATION_ENABLED;
            }
            isActive = false;
        }
    }

    public void startLocationTracking(final boolean shouldPromptUser) {
        Logger.d(TAG, "startLocationTracking");
        isActive = true;
        if (state == State.NEEDS_PERMISSION){
            Logger.i(TAG, "Needs location permission to start workout");
            if (shouldPromptUser){
                sendPermissionBroadcast();
            }
        }else if (state == State.PERMISSIONS_GRANTED){
            // Permissions granted, Google Api client not connected
            connectToLocationServices();
        }else if (state == State.API_CLIENT_CONNECTED){
            // Permissions granted, Google Api client connected, but Location not enabled
            checkForLocationSettings(shouldPromptUser);
        }else if (state == State.LOCATION_ENABLED){
            // Permissions granted, Google Api client connected, but Location enabled but updates didn't start to flow in yet
            handler.post(new Runnable() {
                @Override
                public void run() {
                    requestLocationUpdates();
                }
            });
        }
        // In all other cases, do nothing
    }

    private void requestLocationUpdates(){
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        state = State.FETCHING_LOCATION;

        Iterator<WeakReference<Listener>> iterator = listeners.iterator();
        while (iterator.hasNext()){
            WeakReference<Listener> reference = iterator.next();
            if (reference.get() != null){
                reference.get().onLocationTrackerReady();
            }
        }
    }

    public void onPermissionsGranted(){
        state = State.PERMISSIONS_GRANTED;
        try {
            locationManager = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
            locationManager.addGpsStatusListener(this);
        }catch (SecurityException se){
            // Won't happen ever, because permission just granted by the user
        }
        connectToLocationServices();
    }

    public void onPermissionsRejected(){
        state = State.NEEDS_PERMISSION;
        // Permission was denied or request was cancelled
        Logger.i(TAG, "Location Permission denied");
        Toast.makeText(appContext, "We need location permission to track Runs", Toast.LENGTH_LONG).show();
        Iterator<WeakReference<Listener>> iterator = listeners.iterator();
        while (iterator.hasNext()){
            WeakReference<Listener> reference = iterator.next();
            if (reference.get() != null){
                reference.get().onPermissionDenied();
            }
        }
    }

    private void connectToLocationServices(){
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(appContext) == ConnectionResult.SUCCESS) {
            if (googleApiClient == null){
                googleApiClient = new GoogleApiClient.Builder(appContext)
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
            }
            if (!googleApiClient.isConnected() || !googleApiClient.isConnecting()) {
                googleApiClient.connect();
            }
        } else {
            Logger.e(TAG, "unable to connect to google play services.");
            Toast.makeText(appContext, "Unable to connect to google play services", Toast.LENGTH_SHORT);
        }
    }


    private void fetchInitialLocation() {
        if (ActivityCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            currentLocation =
                    LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (currentLocation == null) {
                Logger.i(TAG, "Last Known Location couldn't be fetched");
            }
        } else {
            //No need to worry about permission unavailability, as it was already granted in constructor
        }
    }

    private void sendPermissionBroadcast(){
        Logger.d(TAG, "sendPermissionBroadcast");
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.LOCATION_TRACKER_BROADCAST_CATEGORY,
                Constants.BROADCAST_REQUEST_PERMISSION_CODE);
        Intent intent = new Intent(Constants.LOCATION_TRACKER_BROADCAST_ACTION);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.CODE_LOCATION_SETTINGS_RESOLUTION){
            if (resultCode == Activity.RESULT_OK) {
                // Can startWorkout with location requests
                state = State.LOCATION_ENABLED;
                requestLocationUpdates();
            } else {
                // Can't do nothing
            }
        }
    }

    private void checkForLocationSettings(final boolean shouldPromptUser) {
        Logger.d(TAG, "checkForLocationSettings: shouldPromptUser = " + shouldPromptUser);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient,
                        builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can
                        // initialize location requests here.
                        state = State.LOCATION_ENABLED;
                        requestLocationUpdates();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied, but this can be fixed
                        // by showing the user a dialog.
                        if (shouldPromptUser){
                            sendLocationEnableBroadcast(status);
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way
                        // to fix the settings so we won't show the dialog.
                        Logger.e(TAG, "Error: Can't enable location updates SETTINGS_CHANGE_UNAVAILABLE");
                        break;
                }
            }
        });
    }

    private void sendLocationEnableBroadcast(Status status){
        Logger.d(TAG, "sendLocationEnableBroadcast");
        Bundle bundle = new Bundle();
        bundle.putInt(Constants.LOCATION_TRACKER_BROADCAST_CATEGORY,
                Constants.BROADCAST_FIX_LOCATION_SETTINGS_CODE);
        Intent intent = new Intent(Constants.LOCATION_TRACKER_BROADCAST_ACTION);
        bundle.putParcelable(Constants.KEY_LOCATION_SETTINGS_PARCELABLE,
                status);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent);
    }

    /**
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or startWorkout periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        Logger.d(TAG, "onConnected");
        state = State.API_CLIENT_CONNECTED;
        retryAttempt = 0;
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(Config.LOCATION_UPDATE_INTERVAL); // milliseconds
        locationRequest.setSmallestDisplacement(Config.SMALLEST_DISPLACEMENT);
        locationRequest.setFastestInterval(Config.LOCATION_UPDATE_INTERVAL); // the fastest rate in milliseconds at which your app can handle location updates
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (isActive){
            checkForLocationSettings(false);
        }

        fetchInitialLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Connection temporarily suspended, onConnected will be called once connection is back
        Logger.e(TAG, "GoogleApiClient connection has been suspend");
        state = State.API_CLIENT_CONNECTION_SUSPENDED;
    }

    int retryAttempt = 0;

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Logger.e(TAG, "onConnectionFailed");
        if (retryAttempt < 3){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    connectToLocationServices();
                }
            }, 500);
            retryAttempt++;
        }
        else {
            Iterator<WeakReference<Listener>> iterator = listeners.iterator();
            while (iterator.hasNext()){
                WeakReference<Listener> reference = iterator.next();
                if (reference.get() != null){
                    reference.get().onConnectionFailure();
                }
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Logger.i(TAG, "onLocationChanged: " + location.toString());
        currentLocation = location;
        Iterator<WeakReference<Listener>> iterator = listeners.iterator();
        while (iterator.hasNext()){
            WeakReference<Listener> reference = iterator.next();
            if (reference.get() != null){
                reference.get().onLocationChanged(location);
            }
        }
    }

    public boolean isFetchingLocation(){
        return state == State.FETCHING_LOCATION;
    }

    public boolean isGpsEnabled(){
        try {
            if (locationManager != null){
                return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            }
            return false;
        }catch (SecurityException se){
            Logger.e(TAG, "Location permission not granted: " + se.getMessage());
            se.printStackTrace();
            return false;
        }
    }

    private boolean workoutInProgress = false;

    private void setWorkoutInProgress(boolean inProgress){
        workoutInProgress = inProgress;
    }

    public boolean workoutInProgess(){
        return workoutInProgress;
//        // Workout is in progress if alteast one listeners is attached to this LocationTracker
//        return (listeners.size() > 0);
    }

    public int getNumSatellites(){
        return numSatellitesConnected;
    }

    private BroadcastReceiver gpsProviderChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
                Logger.i(TAG, "GPS ENABLED");
                if (state != State.FETCHING_LOCATION && state != State.LOCATION_ENABLED){
                    startLocationTracking(false);
                    Iterator<WeakReference<Listener>> iterator = listeners.iterator();
                    while (iterator.hasNext()){
                        WeakReference<Listener> reference = iterator.next();
                        if (reference.get() != null){
                            reference.get().onGpsEnabled();
                        }
                    }
                }
            } else {
                Logger.i(TAG, "GPS DISABLED");
                if (state == State.FETCHING_LOCATION || state == State.LOCATION_ENABLED){
                    state = State.API_CLIENT_CONNECTED;
                    Iterator<WeakReference<Listener>> iterator = listeners.iterator();
                    while (iterator.hasNext()){
                        WeakReference<Listener> reference = iterator.next();
                        if (reference.get() != null){
                            reference.get().onGpsDisabled();
                        }
                    }
                }
            }
        }
    };

    @Override
    public void onGpsStatusChanged(int event) {
//        Logger.i(TAG, "onGpsStatusChanged");
        switch (event){
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                GpsStatus gpsStatus = locationManager.getGpsStatus(null);
                if(gpsStatus != null) {
                    Iterable<GpsSatellite>satellites = gpsStatus.getSatellites();
                    Iterator<GpsSatellite>sat = satellites.iterator();
                    int count=0;
                    while (sat.hasNext()) {
                        GpsSatellite satellite = sat.next();
                        count++;
                    }
                    numSatellitesConnected = count;
                    Logger.i(TAG, "GPS_EVENT_SATELLITE_STATUS: Total number of GPS satellites connected = "
                            + numSatellitesConnected);
                }
                break;
            case GpsStatus.GPS_EVENT_STARTED:
//                Logger.i(TAG, "GPS STARTED");
//                MainApplication.showToast("GPS STARTED");
                break;
            case GpsStatus.GPS_EVENT_STOPPED:
//                Logger.i(TAG, "GPS STOPPED");
//                MainApplication.showToast("GPS STOPPED");
                break;

        }
    }

    public interface Listener{
        void onLocationTrackerReady();
        void onLocationChanged(Location location);
        void onPermissionDenied();
        void onConnectionFailure();
        void onGpsEnabled();
        void onGpsDisabled();
    }

    public enum State{
        NEEDS_PERMISSION, // Needs location access permission from the user
        PERMISSIONS_GRANTED, // Location Access permissions granted by the user
        API_CLIENT_CONNECTED, // Location permissions present and GoogleApiClient connected
        API_CLIENT_CONNECTION_SUSPENDED, // Location permissions present and GoogleApiClient connection was suspended temporarily
        LOCATION_ENABLED, // Location permissions present, GoogleApiClient connected and Location enabled by the user and location updates have not started to flow in yet
        FETCHING_LOCATION, // Location permissions present, GoogleApiClient connected and Location enabled by the user and location updates flowing in already
    }

}
