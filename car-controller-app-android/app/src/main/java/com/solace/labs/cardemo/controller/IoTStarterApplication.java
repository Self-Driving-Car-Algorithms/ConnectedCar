/*******************************************************************************
 * Copyright (c) 2014-2016 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Mike Robertson - initial contribution
 *    Aldo Eisma - location update and light control fixed, updated for Android M
 *    David Wray - updated for Solace CarDemo
 *******************************************************************************/
package com.solace.labs.cardemo.controller;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.solace.labs.cardemo.controller.iot.IoTDevice;
import com.solace.labs.cardemo.controller.utils.Constants;
import com.solace.labs.cardemo.controller.utils.MyIoTCallbacks;

import java.util.*;

/**
 * Main class for the IoT Starter application. Stores values for
 * important device and application information.
 */
public class IoTStarterApplication extends Application {
    private final static String TAG = IoTStarterApplication.class.getName();

    private boolean tutorialShown = false;

    // Current activity of the application, updated whenever activity is changed
    private String currentRunningActivity;

    // Values needed for connecting to IoT
    // prepopulate with defaults for emea1
    private String organization="london.solace.com";
    private String deviceType;
    private String deviceId = "default";
    private String authToken = "default";
    private String VIN = "1234567890123456789";

    private boolean useSSL = false;

    private SharedPreferences settings;

    private MyIoTCallbacks myIoTCallbacks;

    // Application state variables
    private boolean connected = false;
    private int publishCount = 0;
    private int receiveCount = 0;
    private int unreadCount = 0;

    private int color = Color.argb(1, 58, 74, 83);
    private boolean isCameraOn = false;
    private float[] accelData = {0.0f,0.0f,0.0f};
    private boolean accelEnabled = true;

    private Camera camera;
    private String cameraId;
    private FragmentActivity displayActivity;

    // Message log for log activity
    private final ArrayList<String> messageLog = new ArrayList<String>();

    private final List<IoTDevice> profiles = new ArrayList<IoTDevice>();
    private final ArrayList<String> profileNames = new ArrayList<String>();

    //tracking stuff
    private LatLng carLocation;
    private boolean hasCrashed = false;

    /**
     * Called when the application is created. Initializes the application.
     */
    @Override
    public void onCreate() {
        Log.d(TAG, ".onCreate() entered");
        super.onCreate();

        settings = getSharedPreferences(Constants.SETTINGS, 0);

        //SharedPreferences.Editor editor = settings.edit();
        /* Start app with 0 saved settings */
        //editor.clear();
        /* Start app with tutorial never been seen */
        //editor.remove("TUTORIAL_SHOWN");
        /* Start app with original settings values */
        //editor.putString("organization", "");
        //editor.putString("deviceid", "");
        //editor.putString("authtoken", "");
        /* Start app without 'DeviceType' saved */
        //Set<String> props = new HashSet<String>();
        //props.add("name:");
        //props.add("deviceId:");
        //props.add("org:");
        //props.add("authToken:");
        //editor.putStringSet("testiot", props);
        //editor.commit();

        if (settings.getString("TUTORIAL_SHOWN", null) != null) {
            tutorialShown = true;
        }

        myIoTCallbacks = MyIoTCallbacks.getInstance(this);

        loadProfiles();
    }

    /**
     * Called when old application stored settings values are found.
     * Converts old stored settings into new profile setting.
     */
    @TargetApi(value = 11)
    private void createNewDefaultProfile() {
        Log.d(TAG, "organization not null. compat profile setup");
        // If old stored property settings exist, use them to create a new default profile.
        String organization = settings.getString(Constants.ORGANIZATION, null);
        String deviceType = Constants.DEVICE_TYPE;
        String deviceId = settings.getString(Constants.DEVICE_ID, null);
        String VIN = settings.getString(Constants.VIN, null);
        String authToken = settings.getString(Constants.AUTH_TOKEN, null);
        IoTDevice newDevice = new IoTDevice("default", organization, deviceType, deviceId, authToken, VIN);
        this.profiles.add(newDevice);
        this.profileNames.add("default");

        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= Build.VERSION_CODES.HONEYCOMB) {
            // Put the new profile into the store settings and remove the old stored properties.
            Set<String> defaultProfile = newDevice.convertToSet();

            SharedPreferences.Editor editor = settings.edit();
            editor.putStringSet(newDevice.getDeviceName(), defaultProfile);
            editor.remove(Constants.ORGANIZATION);
            editor.remove(Constants.DEVICE_ID);
            editor.remove(Constants.AUTH_TOKEN);
            editor.remove(Constants.VIN);
            //editor.apply();
            editor.commit();
        }

        this.setProfile(newDevice);
        this.setOrganization(newDevice.getOrganization());
        this.setDeviceType(newDevice.getDeviceType());
        this.setDeviceId(newDevice.getDeviceID());
        this.setAuthToken(newDevice.getAuthorizationToken());
        this.setVIN(newDevice.getVIN());
    }

    /**
     * Load existing profiles from application stored settings.
     */
    @TargetApi(value = 11)
    private void loadProfiles() {
        // Compatibility
        if (settings.getString(Constants.ORGANIZATION, null) != null) {
            createNewDefaultProfile();
            return;
        }

        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= Build.VERSION_CODES.HONEYCOMB) {
            String profileName;
            if ((profileName = settings.getString("iot:selectedprofile", null)) == null) {
                profileName = "";
                Log.d(TAG, "Last selected profile: " + profileName);
            }

            Map<String, ?> profileList = settings.getAll();
            if (profileList != null) {
                for (String key : profileList.keySet()) {
                    if (key.equals("iot:selectedprofile") || key.equals("TUTORIAL_SHOWN")) {
                        continue;
                    }
                    Set<String> profile;
                    try {
                        // If the stored property is a Set<String> type, parse the profile and add it to the list of
                        // profiles.
                        if ((profile = settings.getStringSet(key, null)) != null) {
                            Log.d(TAG, "profile name: " + key);
                            IoTDevice newProfile = new IoTDevice(profile);
                            this.profiles.add(newProfile);
                            this.profileNames.add(newProfile.getDeviceName());

                            if (newProfile.getDeviceName().equals(profileName)) {
                                this.setProfile(newProfile);
                                this.setOrganization(newProfile.getOrganization());
                                this.setDeviceType(newProfile.getDeviceType());
                                this.setDeviceId(newProfile.getDeviceID());
                                this.setAuthToken(newProfile.getAuthorizationToken());
                                this.setVIN(newProfile.getVIN());
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, ".loadProfiles() received exception:");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Overwrite an existing profile in the stored application settings.
     * @param newProfile The profile to save.
     */
    @TargetApi(value = 11)
    public void overwriteProfile(IoTDevice newProfile) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= Build.VERSION_CODES.HONEYCOMB) {
            // Put the new profile into the store settings and remove the old stored properties.
            Set<String> profileSet = newProfile.convertToSet();

            SharedPreferences.Editor editor = settings.edit();
            editor.remove(newProfile.getDeviceName());
            editor.putStringSet(newProfile.getDeviceName(), profileSet);
            //editor.apply();
            editor.commit();
        }

        for (IoTDevice existingProfile : profiles) {
            if (existingProfile.getDeviceName().equals(newProfile.getDeviceName())) {
                profiles.remove(existingProfile);
                break;
            }
        }
        profiles.add(newProfile);
    }
    /**
     * Save the profile to the application stored settings.
     * @param profile The profile to save.
     */
    @TargetApi(value = 11)
    public void saveProfile(IoTDevice profile) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= Build.VERSION_CODES.HONEYCOMB) {
            // Put the new profile into the store settings and remove the old stored properties.
            Set<String> profileSet = profile.convertToSet();

            SharedPreferences.Editor editor = settings.edit();
            editor.putStringSet(profile.getDeviceName(), profileSet);
            //editor.apply();
            editor.commit();
        }
        this.profiles.add(profile);
        this.profileNames.add(profile.getDeviceName());
    }

    /**
     * Remove all saved profile information.
     */
    public void clearProfiles() {
        this.profiles.clear();
        this.profileNames.clear();
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= Build.VERSION_CODES.HONEYCOMB) {
            SharedPreferences.Editor editor = settings.edit();
            editor.clear();
            //editor.apply();
            editor.commit();
        }
    }

    // Getters and Setters
    public String getCurrentRunningActivity() { return currentRunningActivity; }

    public void setCurrentRunningActivity(String currentRunningActivity) { this.currentRunningActivity = currentRunningActivity; }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
    public void setVIN(String VIN) {
        // Don't let VIN get replaced for now!
        this.VIN = VIN;
    }
    public String getVIN() {
        return VIN;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public int getPublishCount() {
        return publishCount;
    }

    public void setPublishCount(int publishCount) {
        this.publishCount = publishCount;
    }

    public int getReceiveCount() {
        return receiveCount;
    }

    public void setReceiveCount(int receiveCount) {
        this.receiveCount = receiveCount;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public float[] getAccelData() { return accelData; }

    public void setAccelData(float[] accelData) {
        this.accelData = accelData.clone();
    }

    public ArrayList<String> getMessageLog() {
        return messageLog;
    }

    public boolean isAccelEnabled() {
        return accelEnabled;
    }

    private void setAccelEnabled(boolean accelEnabled) {
        this.accelEnabled = accelEnabled;
    }

    public void setProfile(IoTDevice profile) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= Build.VERSION_CODES.HONEYCOMB) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("iot:selectedprofile", profile.getDeviceName());
            //editor.apply();
            editor.commit();
        }
    }

    public List<IoTDevice> getProfiles() {
        return profiles;
    }

    public ArrayList<String> getProfileNames() {
        return profileNames;
    }

    public MyIoTCallbacks getMyIoTCallbacks() {
        return myIoTCallbacks;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public boolean isTutorialShown() {
        return tutorialShown;
    }

    public void setTutorialShown(boolean tutorialShown) {
        this.tutorialShown = tutorialShown;
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("TUTORIAL_SHOWN", "yes");
        editor.commit();
    }
    public void setDisplayActivity(FragmentActivity displayActivity) {
        this.displayActivity = displayActivity;
    }
    public FragmentActivity getDisplayActivity() {
        return displayActivity;
    }

    public LatLng getCarLocation() {
        return carLocation;
    }

    public void setCarLocation(double lat, double lon) {
        this.carLocation = new LatLng(lat, lon);
    }

    public boolean hasCrashed() {
        return this.hasCrashed;
    }
    public void setCrashed(boolean b) {
        hasCrashed=b;
    }
}
