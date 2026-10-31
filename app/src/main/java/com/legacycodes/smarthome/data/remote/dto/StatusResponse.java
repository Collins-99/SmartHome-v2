package com.legacycodes.smarthome.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * Mirrors GET /status from the ESP32.
 *
 * Wire body:
 *   {"pins":{"4":0,"5":1,"13":0,...}}
 *
 * Gson maps numeric string keys to Integer keys automatically when
 * declared as Map<Integer, Integer>.
 */
public class StatusResponse {

    @SerializedName("pins")
    public Map<Integer, Integer> pins;

    public StatusResponse() {
        // required by Gson
    }
}