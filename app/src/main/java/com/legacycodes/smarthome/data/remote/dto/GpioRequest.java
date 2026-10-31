package com.legacycodes.smarthome.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for POST /gpio.
 *
 * Wire body: {"pin":5,"state":1}
 *
 * state is LOGICAL (1=on, 0=off). The firmware handles ACTIVE_LOW.
 */
public class GpioRequest {

    @SerializedName("pin")
    public int pin;

    @SerializedName("state")
    public int state;

    public GpioRequest() {
        // required by Gson
    }

    public GpioRequest(int pin, int state) {
        this.pin = pin;
        this.state = state;
    }
}