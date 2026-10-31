package com.legacycodes.smarthome.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.legacycodes.smarthome.data.local.AppDatabase;
import com.legacycodes.smarthome.data.local.entity.DeviceEntity;
import com.legacycodes.smarthome.data.remote.ApiClient;
import com.legacycodes.smarthome.data.remote.Esp32Api;
import com.legacycodes.smarthome.data.remote.dto.InfoResponse;
import com.legacycodes.smarthome.util.AppExecutors;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import retrofit2.Response;

/**
 * Repository for Device (the ESP32).
 *
 * The UI never sees Retrofit, Room, or threads — it sees LiveData and
 * callbacks. This class is the only thing Phase 3+ will talk to for
 * device data.
 */
public class DeviceRepository {

    private static final String TAG = "DeviceRepository";
    private static final Type PIN_LIST_TYPE = new TypeToken<List<Integer>>(){}.getType();

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String reason);
    }

    private final AppDatabase database;
    private final Esp32Api api;
    private final AppExecutors executors;
    private final Gson gson = new Gson();

    public DeviceRepository(AppDatabase database, Esp32Api api, AppExecutors executors) {
        this.database = database;
        this.api = api;
        this.executors = executors;
    }

    // ---- Reads ----

    public LiveData<DeviceEntity> observePrimary() {
        return database.deviceDao().observePrimary();
    }

    // ---- Save / update ----

    /**
     * Insert or update the primary device. Called from the Setup screen.
     */
    public void saveDevice(String name, String ip, Callback<Long> callback) {
        executors.diskIO().execute(() -> {
            try {
                DeviceEntity existing = database.deviceDao().getPrimaryBlocking();
                long rowId;
                if (existing == null) {
                    DeviceEntity d = new DeviceEntity();
                    d.name = name;
                    d.ipAddress = ip;
                    d.lastSeen = 0L;
                    rowId = database.deviceDao().insert(d);
                } else {
                    database.deviceDao().updateIp(existing.id, ip);
                    rowId = existing.id;
                }
                final long finalId = rowId;
                executors.mainThread().execute(() -> callback.onSuccess(finalId));
            } catch (Exception e) {
                Log.e(TAG, "saveDevice failed", e);
                executors.mainThread().execute(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Hit /info, save firmware + availablePins into the device row.
     * Makes the Phase 4 add-appliance dropdown work offline.
     */
    public void fetchAndSaveInfo(long deviceId, Callback<InfoResponse> callback) {
        executors.diskIO().execute(() -> {
            DeviceEntity device = database.deviceDao().getByIdBlocking(deviceId);
            if (device == null) {
                executors.mainThread().execute(() -> callback.onError("device not found"));
                return;
            }
            String url = "http://" + device.ipAddress + "/info";
            executors.networkIO().execute(() -> {
                try {
                    Response<InfoResponse> response = api.getInfo(url).execute();
                    if (!response.isSuccessful() || response.body() == null) {
                        String reason = "HTTP " + response.code();
                        executors.mainThread().execute(() -> callback.onError(reason));
                        return;
                    }
                    InfoResponse info = response.body();
                    String pinsJson = gson.toJson(info.availablePins);
                    long now = System.currentTimeMillis();

                    executors.diskIO().execute(() -> {
                        database.deviceDao().updateInfo(deviceId, info.name, info.firmware, now);
                        database.deviceDao().updateAvailablePins(deviceId, pinsJson);
                        executors.mainThread().execute(() -> callback.onSuccess(info));
                    });
                } catch (IOException e) {
                    Log.e(TAG, "fetchAndSaveInfo failed", e);
                    executors.mainThread().execute(() -> callback.onError(e.getMessage()));
                }
            });
        });
    }

    // ---- Blocking read for other repositories ----

    /** Caller MUST be on a background thread. */
    public DeviceEntity getPrimaryBlocking() {
        return database.deviceDao().getPrimaryBlocking();
    }

    // ---- Helpers ----

    public List<Integer> parseAvailablePins(DeviceEntity device) {
        if (device == null || device.availablePinsJson == null) {
            return Collections.emptyList();
        }
        try {
            List<Integer> pins = gson.fromJson(device.availablePinsJson, PIN_LIST_TYPE);
            return pins != null ? pins : Collections.emptyList();
        } catch (Exception e) {
            Log.w(TAG, "failed to parse availablePinsJson", e);
            return Collections.emptyList();
        }
    }
}