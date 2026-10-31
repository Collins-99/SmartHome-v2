package com.legacycodes.smarthome.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Reply from POST /gpio.
 *
 * Success wire body: {"ok":true,"pin":5,"state":1}
 * Failure wire body: {"ok":false,"error":"pin not managed"}
 *
 * Non-ok fields are nullable so Gson leaves them null on failure.
 */
public class GpioResponse {

    @SerializedName("ok")
    public boolean ok;

    @SerializedName("pin")
    public Integer pin;

    @SerializedName("state")
    public Integer state;

    @SerializedName("error")
    public String error;

    public GpioResponse() {
        // required by Gson
    }
}