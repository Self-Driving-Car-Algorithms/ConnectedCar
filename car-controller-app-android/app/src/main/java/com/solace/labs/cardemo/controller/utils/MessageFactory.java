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
 *    Aldo Eisma - add bearing and speed to acceleration message
 *    David Wray - updated for Solace CarDemo
 *******************************************************************************/
package com.solace.labs.cardemo.controller.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Build messages to be published by the application.
 * This class is currently unused.
 */
public class MessageFactory {

    /**
     * Construct a JSON formatted string accel event message
     * @param G Float array with accelerometer x, y, z data
     * @param O Float array with gyroscope roll, pitch data
     * @param yaw Float representing gyroscope yaw value
     * @param lon Double containing device longitude
     * @param lat Double containing device latitude
     * @param heading Float containing device heading
     * @param speed Float containing device speed in km/h
     * @param tripId Long containing trip identifier
     * @return String containing JSON formatted message
     */
    public static String getAccelMessage(float G[], float O[], float yaw, double lon, double lat, float heading, float speed, long tripId) {
        // Android does not support the X pattern, so use Z and insert ':' if required.
        DateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
//        isoDateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String isoTimestamp = isoDateTimeFormat.format(new Date());
        if (!isoTimestamp.endsWith("Z")) {
            int pos = isoTimestamp.length() - 2;
            isoTimestamp = isoTimestamp.substring(0, pos) + ':' + isoTimestamp.substring(pos);
        }
        return "{ \"d\": {" +
                "\"acceleration_x\":" + G[0] + ", " +
                "\"acceleration_y\":" + G[1] + ", " +
                "\"acceleration_z\":" + G[2] + ", " +
                "\"roll\":" + O[2] + ", " +
                "\"pitch\":" + O[1] + ", " +
                "\"yaw\":" + yaw + ", " +
                "\"longitude\":" + lon + ", " +
                "\"latitude\":" + lat + ", " +
                "\"heading\":" + heading + ", " +
                "\"speed\":" + speed + ", " +
                "\"trip_id\": \"" + tripId + "\", " +
                "\"timestamp\":\"" + isoTimestamp + "\" " +
                "} }";
    }

    /**
     * Construct a JSON formatted string text event message
     * @param text String of text message to send
     * @return String containing JSON formatted message
     */
    public static String getTextMessage(String text) {
        return "{\"d\":{" +
                "\"text\":\"" + text + "\"" +
                " } }";
    }

    public static String getStartEngineMessage() {
        return "{\"d\":{" +
                "\"engine\":\"" + "start" + "\"" +
                " } }";
    }
    public static String getToggleLightsMessage(boolean on) {
        String offOn = null;
        if (on) {
            offOn = "on";
        } else {
            offOn = "off";
        }
        return "{\"d\":{" +
                "\"lights\":\"" + offOn + "\"" +
                " } }";
    }
    public static String getToggleLockMessage(boolean locked) {
        String lockedUnlocked = null;
        if (locked) {
            lockedUnlocked = Constants.LOCK_COMMAND_LOCKED;
        } else {
            lockedUnlocked = Constants.LOCK_COMMAND_UNLOCKED;
        }
        return "{\"d\":{" +
                "\"lock\":\"" + lockedUnlocked + "\"" +
                " } }";
    }
    public static String getSoundHornMessage() {
        return "{\"d\":{" +
                "\"horn\":\"" + "sound" + "\"" +
                " } }";
    }
    public static String getAmbienceMessage(String colour) {
        String newColour = colour.toLowerCase();
        String RGBValue = null;
        if (newColour.contentEquals("red")) {
            RGBValue = "\"r\":255,\"g\":0,\"b\":0,\"alpha\":1.0";
        } else if (newColour.contentEquals("green")) {
            RGBValue = "\"r\":0,\"g\":255,\"b\":0,\"alpha\":1.0";
        } else if (newColour.contentEquals("blue")) {
            RGBValue = "\"r\":0,\"g\":0,\"b\":255,\"alpha\":1.0";
        }

        return "{\"d\":{"+ RGBValue + " } }";
    }

}
