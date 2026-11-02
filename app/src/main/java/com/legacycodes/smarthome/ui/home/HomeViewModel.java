package com.legacycodes.smarthome.ui.home;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.legacycodes.smarthome.AppContainer;
import com.legacycodes.smarthome.SmartHomeApp;
import com.legacycodes.smarthome.data.local.entity.ApplianceEntity;
import com.legacycodes.smarthome.data.local.entity.DeviceEntity;
import com.legacycodes.smarthome.data.local.entity.ProfileEntity;
import com.legacycodes.smarthome.data.repository.ApplianceRepository;
import com.legacycodes.smarthome.data.repository.DeviceRepository;
import com.legacycodes.smarthome.data.repository.ProfileRepository;

import java.util.Collections;
import java.util.List;

/**
 * ViewModel for the home screen.
 *
 * This class is the only bridge between UI and repositories.
 * It exposes:
 *   - LiveData the activity observes (appliances, device, profile)
 *   - Actions the activity calls (toggle, sync, refresh)
 *
 * It never touches Room, Retrofit, threads, or DTOs directly — that's
 * the repository's job. It just orchestrates.
 *
 * Extends AndroidViewModel because we need the Application to reach
 * the AppContainer (manual DI). Not the "purest" pattern, but simple
 * and honest for an app of this size.
 */
public class HomeViewModel extends AndroidViewModel {

    private static final String TAG = "HomeViewModel";

    private final ApplianceRepository applianceRepo;
    private final DeviceRepository deviceRepo;
    private final ProfileRepository profileRepo;

    private final MutableLiveData<Long> deviceIdLive = new MutableLiveData<>();
    private final LiveData<List<ApplianceEntity>> appliancesLive;
    private final LiveData<Integer> applianceCountLive;
    private final LiveData<DeviceEntity> deviceLive;
    private final LiveData<ProfileEntity> profileLive;

    // One-shot message channel for Toasts / Snackbars ("Failed to toggle...")
    private final MutableLiveData<String> messageLive = new MutableLiveData<>();

    // Online / offline flag driven by the last network operation
    private final MutableLiveData<Boolean> connectedLive = new MutableLiveData<>(false);

    public HomeViewModel(@NonNull Application application) {
        super(application);

        AppContainer c = ((SmartHomeApp) application).getContainer();
        applianceRepo = c.applianceRepository;
        deviceRepo    = c.deviceRepository;
        profileRepo   = c.profileRepository;

        // Watch the device row from Room. When it appears (or its id changes),
        // switch which appliance list we're observing.
        deviceLive = deviceRepo.observePrimary();

        appliancesLive = Transformations.switchMap(deviceIdLive, id -> {
            if (id == null || id <= 0) {
                return new MutableLiveData<>(Collections.emptyList());
            }
            return applianceRepo.observeAllForDevice(id);
        });

        applianceCountLive = Transformations.switchMap(deviceIdLive, id -> {
            if (id == null || id <= 0) {
                return new MutableLiveData<>(0);
            }
            return applianceRepo.observeCount(id);
        });

        profileLive = profileRepo.observe();

        // Bridge deviceLive → deviceIdLive so switchMap recomputes when a device appears.
        deviceLive.observeForever(device -> {
            if (device != null) {
                Long current = deviceIdLive.getValue();
                if (current == null || current != device.id) {
                    deviceIdLive.setValue(device.id);
                }
            } else {
                deviceIdLive.setValue(0L);
            }
        });
    }

    // ---- LiveData the activity observes ----

    public LiveData<List<ApplianceEntity>> getAppliances() {
        return appliancesLive;
    }

    public LiveData<Integer> getApplianceCount() {
        return applianceCountLive;
    }

    public LiveData<DeviceEntity> getDevice() {
        return deviceLive;
    }

    public LiveData<ProfileEntity> getProfile() {
        return profileLive;
    }

    public LiveData<String> getMessage() {
        return messageLive;
    }

    public LiveData<Boolean> getConnected() {
        return connectedLive;
    }

    // ---- Actions the activity calls ----

    /**
     * Called when a user flips a toggle.
     * Repository handles optimistic update + rollback, and notifies us of failure.
     */
    public void toggleAppliance(ApplianceEntity appliance, boolean newCheckedState) {
        int newState = newCheckedState ? 1 : 0;
        if (appliance.state == newState) {
            return; // no-op if already in desired state (guard against double-tap)
        }
        applianceRepo.toggleAppliance(appliance.id, newState,
                new ApplianceRepository.Callback<Integer>() {
                    @Override
                    public void onSuccess(Integer result) {
                        connectedLive.setValue(true);
                    }

                    @Override
                    public void onError(String reason) {
                        connectedLive.setValue(false);
                        messageLive.setValue("Failed to toggle: " + reason);
                    }
                });
    }

    /**
     * Pull-to-refresh: hits /status on the ESP32 and reconciles Room.
     */
    public void refreshFromDevice() {
        Long id = deviceIdLive.getValue();
        if (id == null || id <= 0) {
            messageLive.setValue("No device configured");
            return;
        }
        applianceRepo.syncStatusFromDevice(id,
                new ApplianceRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void ignored) {
                        connectedLive.setValue(true);
                    }

                    @Override
                    public void onError(String reason) {
                        connectedLive.setValue(false);
                        messageLive.setValue("Sync failed: " + reason);
                    }
                });
    }

    /**
     * Called once after the app first loads an IP for the device.
     * Fetches /info and stores pins/firmware in Room.
     */
    public void fetchDeviceInfoIfNeeded() {
        DeviceEntity d = deviceLive.getValue();
        if (d == null) return;
        if (d.availablePinsJson != null && !d.availablePinsJson.isEmpty()) {
            return; // already have it
        }
        deviceRepo.fetchAndSaveInfo(d.id,
                new DeviceRepository.Callback<com.legacycodes.smarthome.data.remote.dto.InfoResponse>() {
                    @Override
                    public void onSuccess(com.legacycodes.smarthome.data.remote.dto.InfoResponse result) {
                        connectedLive.setValue(true);
                    }

                    @Override
                    public void onError(String reason) {
                        connectedLive.setValue(false);
                        Log.w(TAG, "fetchInfo failed: " + reason);
                    }
                });
    }

    /** Clear a one-shot message after the UI has shown it. */
    public void clearMessage() {
        messageLive.setValue(null);
    }
}