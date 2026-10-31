package com.legacycodes.smarthome.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.legacycodes.smarthome.data.local.AppDatabase;
import com.legacycodes.smarthome.data.local.entity.ApplianceEntity;
import com.legacycodes.smarthome.data.local.entity.DeviceEntity;
import com.legacycodes.smarthome.data.remote.ApiClient;
import com.legacycodes.smarthome.data.remote.Esp32Api;
import com.legacycodes.smarthome.data.remote.dto.GpioRequest;
import com.legacycodes.smarthome.data.remote.dto.GpioResponse;
import com.legacycodes.smarthome.data.remote.dto.StatusResponse;
import com.legacycodes.smarthome.util.AppExecutors;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

/**
 * Repository for Appliance.
 *
 * Responsibilities:
 *   - CRUD appliance rows in Room
 *   - Toggle a pin on the ESP32 (POST /gpio)
 *   - Sync all pins from the ESP32 (GET /status) and reconcile Room
 *
 * The UI talks to this, not to Room or Retrofit.
 */
public class ApplianceRepository {

    private static final String TAG = "ApplianceRepository";

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String reason);
    }

    private final AppDatabase database;
    private final Esp32Api api;
    private final AppExecutors executors;

    public ApplianceRepository(AppDatabase database, Esp32Api api, AppExecutors executors) {
        this.database = database;
        this.api = api;
        this.executors = executors;
    }

    // ---- Reads ----

    public LiveData<List<ApplianceEntity>> observeAllForDevice(long deviceId) {
        return database.applianceDao().observeAllForDevice(deviceId);
    }

    public LiveData<List<Integer>> observeUsedPins(long deviceId) {
        return database.applianceDao().observeUsedPins(deviceId);
    }

    public LiveData<Integer> observeCount(long deviceId) {
        return database.applianceDao().observeCount(deviceId);
    }

    // ---- CRUD ----

    /**
     * Insert a new appliance. Returns the new row id on success.
     */
    public void addAppliance(ApplianceEntity entity, Callback<Long> callback) {
        executors.diskIO().execute(() -> {
            try {
                long id = database.applianceDao().insert(entity);
                executors.mainThread().execute(() -> callback.onSuccess(id));
            } catch (Exception e) {
                Log.e(TAG, "addAppliance failed", e);
                executors.mainThread().execute(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void updateAppliance(ApplianceEntity entity, Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                database.applianceDao().update(entity);
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) {
                Log.e(TAG, "updateAppliance failed", e);
                executors.mainThread().execute(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Delete an appliance. If it was on, first turns the physical pin off.
     */
    public void deleteAppliance(long applianceId, Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            ApplianceEntity entity = database.applianceDao().getByIdBlocking(applianceId);
            if (entity == null) {
                executors.mainThread().execute(() -> callback.onError("not found"));
                return;
            }
            if (entity.state != 0) {
                sendGpio(entity, 0, (ok, reason) -> {
                    database.applianceDao().deleteById(applianceId);
                    executors.mainThread().execute(() -> callback.onSuccess(null));
                });
            } else {
                database.applianceDao().deleteById(applianceId);
                executors.mainThread().execute(() -> callback.onSuccess(null));
            }
        });
    }

    // ---- Toggle (the main interaction) ----

    /**
     * Flip an appliance's state. Updates Room optimistically, sends the
     * command to the ESP32, and rolls back if the device rejects it.
     *
     * onSuccess fires with the new logical state (0 or 1) after ESP32 ack.
     * onError fires if the ESP32 rejected or is unreachable — Room rolled back.
     */
    public void toggleAppliance(long applianceId, int newState, Callback<Integer> callback) {
        executors.diskIO().execute(() -> {
            ApplianceEntity entity = database.applianceDao().getByIdBlocking(applianceId);
            if (entity == null) {
                executors.mainThread().execute(() -> callback.onError("not found"));
                return;
            }
            int previousState = entity.state;

            // Optimistic update
            database.applianceDao().updateState(applianceId, newState);

            sendGpio(entity, newState, (ok, reason) -> {
                if (ok) {
                    executors.mainThread().execute(() -> callback.onSuccess(newState));
                } else {
                    // Roll back
                    database.applianceDao().updateState(applianceId, previousState);
                    executors.mainThread().execute(() -> callback.onError(reason));
                }
            });
        });
    }

    // ---- Status sync ----

    /**
     * Fetch /status from the ESP32 and write every pin state into Room.
     * Fires onSuccess when Room is fully updated.
     */
    public void syncStatusFromDevice(long deviceId, Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            DeviceEntity device = database.deviceDao().getByIdBlocking(deviceId);
            if (device == null) {
                executors.mainThread().execute(() -> callback.onError("device not found"));
                return;
            }
            String url = "http://" + device.ipAddress + "/status";
            executors.networkIO().execute(() -> {
                try {
                    Response<StatusResponse> response = api.getStatus(url).execute();
                    if (!response.isSuccessful() || response.body() == null
                            || response.body().pins == null) {
                        String reason = "HTTP " + response.code();
                        executors.mainThread().execute(() -> callback.onError(reason));
                        return;
                    }
                    Map<Integer, Integer> pins = response.body().pins;
                    executors.diskIO().execute(() -> {
                        List<ApplianceEntity> all =
                                database.applianceDao().getAllForDeviceBlocking(deviceId);
                        for (ApplianceEntity a : all) {
                            Integer remoteState = pins.get(a.gpioPin);
                            if (remoteState != null && remoteState != a.state) {
                                database.applianceDao().updateState(a.id, remoteState);
                            }
                        }
                        executors.mainThread().execute(() -> callback.onSuccess(null));
                    });
                } catch (IOException e) {
                    Log.e(TAG, "syncStatusFromDevice failed", e);
                    executors.mainThread().execute(() -> callback.onError(e.getMessage()));
                }
            });
        });
    }

    // ---- Internal helpers ----

    private interface GpioCallback {
        void onResult(boolean ok, String reason);
    }

    /**
     * Fire POST /gpio for a single appliance. Runs on the network thread.
     */
    private void sendGpio(ApplianceEntity entity, int state, GpioCallback callback) {
        executors.diskIO().execute(() -> {
            DeviceEntity device = database.deviceDao().getByIdBlocking(entity.deviceId);
            if (device == null) {
                executors.mainThread().execute(() ->
                        callback.onResult(false, "device not found"));
                return;
            }
            String url = "http://" + device.ipAddress + "/gpio";
            GpioRequest body = new GpioRequest(entity.gpioPin, state);
            executors.networkIO().execute(() -> {
                try {
                    Response<GpioResponse> response = api.setGpio(url, body).execute();
                    if (!response.isSuccessful() || response.body() == null) {
                        executors.mainThread().execute(() ->
                                callback.onResult(false, "HTTP " + response.code()));
                        return;
                    }
                    GpioResponse gpio = response.body();
                    if (gpio.ok) {
                        executors.mainThread().execute(() ->
                                callback.onResult(true, null));
                    } else {
                        String reason = gpio.error != null ? gpio.error : "rejected";
                        executors.mainThread().execute(() ->
                                callback.onResult(false, reason));
                    }
                } catch (IOException e) {
                    Log.e(TAG, "sendGpio failed", e);
                    executors.mainThread().execute(() ->
                            callback.onResult(false, e.getMessage()));
                }
            });
        });
    }
}