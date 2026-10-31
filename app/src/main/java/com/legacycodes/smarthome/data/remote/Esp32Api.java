package com.legacycodes.smarthome.data.remote;

import com.legacycodes.smarthome.data.remote.dto.GpioRequest;
import com.legacycodes.smarthome.data.remote.dto.GpioResponse;
import com.legacycodes.smarthome.data.remote.dto.InfoResponse;
import com.legacycodes.smarthome.data.remote.dto.StatusResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Url;

/**
 * Retrofit interface for the ESP32 firmware (Phase 1).
 *
 * Every method takes a full @Url — "http://192.168.137.219/info", etc.
 * This is deliberate: the IP is dynamic (Windows hotspot hands out new
 * IPs after every reboot), and keeping the URL at the call site means
 * nothing in this interface needs to be rebuilt when the IP changes.
 *
 * All methods return Call<T>. Callers use enqueue() (or execute() on a
 * background thread) so requests stay off the main thread.
 */
public interface Esp32Api {

    @GET
    Call<InfoResponse> getInfo(@Url String url);

    @GET
    Call<StatusResponse> getStatus(@Url String url);

    @POST
    Call<GpioResponse> setGpio(@Url String url, @Body GpioRequest body);
}