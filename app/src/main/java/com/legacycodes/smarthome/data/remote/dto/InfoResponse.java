package com.legacycodes.smarthome.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Mirrors GET /info from the ESP32.
 *
 * Wire body:
 *   {"name":"HomeController","firmware":"0.2.0","activeLow":true,
 *    "availablePins":[4,5,13,14,25,26,27,32,33]}
 */
public class InfoResponse {

    @SerializedName("name")
    public String name;

    @SerializedName("firmware")
    public String firmware;

    @SerializedName("activeLow")
    public boolean activeLow;

    @SerializedName("availablePins")
    public List<Integer> availablePins;

    public InfoResponse() {
        // required by Gson
    }
}