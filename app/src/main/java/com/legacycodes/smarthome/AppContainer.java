package com.legacycodes.smarthome;

import android.content.Context;

import com.legacycodes.smarthome.data.local.AppDatabase;
import com.legacycodes.smarthome.data.remote.ApiClient;
import com.legacycodes.smarthome.data.remote.Esp32Api;
import com.legacycodes.smarthome.data.repository.ApplianceRepository;
import com.legacycodes.smarthome.data.repository.DeviceRepository;
import com.legacycodes.smarthome.data.repository.ProfileRepository;
import com.legacycodes.smarthome.util.AppExecutors;

/**
 * Manual dependency injection.
 *
 * Constructed once in SmartHomeApp.onCreate(). Every activity accesses
 * it via:
 *
 *   AppContainer c = ((SmartHomeApp) getApplication()).getContainer();
 *   c.applianceRepository...
 *
 * No reflection, no annotation magic, no Hilt. For an app this size,
 * this class IS the DI graph.
 */
public class AppContainer {

    public final AppDatabase database;
    public final Esp32Api api;
    public final AppExecutors executors;

    public final DeviceRepository deviceRepository;
    public final ApplianceRepository applianceRepository;
    public final ProfileRepository profileRepository;

    public AppContainer(Context appContext) {
        this.database = AppDatabase.getInstance(appContext);
        this.api = ApiClient.api();
        this.executors = AppExecutors.get();

        this.deviceRepository = new DeviceRepository(database, api, executors);
        this.applianceRepository = new ApplianceRepository(database, api, executors);
        this.profileRepository = new ProfileRepository(database, executors);
    }
}