package com.legacycodes.smarthome;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.legacycodes.smarthome.data.local.entity.ApplianceEntity;
import com.legacycodes.smarthome.data.local.entity.DeviceEntity;
import com.legacycodes.smarthome.data.remote.dto.InfoResponse;
import com.legacycodes.smarthome.util.AppExecutors;

/**
 * TEMPORARY SELF-TEST for Phase 2.
 *
 * This replaces the ping UI with an end-to-end exercise of the data layer.
 * Once Phase 2 is confirmed working, this file gets replaced by the real
 * Phase 3 home screen.
 *
 * Change ESP32_IP below to match your current ESP32 serial monitor output.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Phase2SelfTest";
    private static final String ESP32_IP = "192.168.137.219";

    private AppContainer container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        container = ((SmartHomeApp) getApplication()).getContainer();

        runSelfTest();
    }

    private void runSelfTest() {
        Log.d(TAG, "=== PHASE 2 SELF TEST START ===");

        // STEP 1 — save device
        Log.d(TAG, "STEP 1: saving device with ip=" + ESP32_IP);
        container.deviceRepository.saveDevice("Home", ESP32_IP,
                new com.legacycodes.smarthome.data.repository.DeviceRepository.Callback<Long>() {
                    @Override
                    public void onSuccess(Long deviceId) {
                        Log.d(TAG, "STEP 1 OK: deviceId=" + deviceId);
                        step2FetchInfo(deviceId);
                    }

                    @Override
                    public void onError(String reason) {
                        Log.e(TAG, "STEP 1 FAILED: " + reason);
                    }
                });
    }

    private void step2FetchInfo(long deviceId) {
        Log.d(TAG, "STEP 2: fetching /info...");
        container.deviceRepository.fetchAndSaveInfo(deviceId,
                new com.legacycodes.smarthome.data.repository.DeviceRepository.Callback<InfoResponse>() {
                    @Override
                    public void onSuccess(InfoResponse info) {
                        Log.d(TAG, "STEP 2 OK: firmware=" + info.firmware
                                + " pins=" + info.availablePins);
                        step3AddAppliance(deviceId);
                    }

                    @Override
                    public void onError(String reason) {
                        Log.e(TAG, "STEP 2 FAILED: " + reason);
                    }
                });
    }

    private void step3AddAppliance(long deviceId) {
        Log.d(TAG, "STEP 3: inserting test appliance...");
        ApplianceEntity a = new ApplianceEntity();
        a.deviceId = deviceId;
        a.name = "Test Light";
        a.type = "LIGHT";
        a.gpioPin = 5;
        a.room = "";
        a.state = 0;

        container.applianceRepository.addAppliance(a,
                new com.legacycodes.smarthome.data.repository.ApplianceRepository.Callback<Long>() {
                    @Override
                    public void onSuccess(Long applianceId) {
                        Log.d(TAG, "STEP 3 OK: applianceId=" + applianceId);
                        step4ToggleOn(applianceId, deviceId);
                    }

                    @Override
                    public void onError(String reason) {
                        Log.e(TAG, "STEP 3 FAILED: " + reason);
                    }
                });
    }

    private void step4ToggleOn(long applianceId, long deviceId) {
        Log.d(TAG, "STEP 4: toggling ON (watch GPIO 5!)...");
        container.applianceRepository.toggleAppliance(applianceId, 1,
                new com.legacycodes.smarthome.data.repository.ApplianceRepository.Callback<Integer>() {
                    @Override
                    public void onSuccess(Integer newState) {
                        Log.d(TAG, "STEP 4 OK: state=" + newState);
                        step5ToggleOff(applianceId, deviceId);
                    }

                    @Override
                    public void onError(String reason) {
                        Log.e(TAG, "STEP 4 FAILED: " + reason);
                    }
                });
    }

    private void step5ToggleOff(long applianceId, long deviceId) {
        Log.d(TAG, "STEP 5: toggling OFF...");
        container.applianceRepository.toggleAppliance(applianceId, 0,
                new com.legacycodes.smarthome.data.repository.ApplianceRepository.Callback<Integer>() {
                    @Override
                    public void onSuccess(Integer newState) {
                        Log.d(TAG, "STEP 5 OK: state=" + newState);
                        step6SyncStatus(deviceId, applianceId);
                    }

                    @Override
                    public void onError(String reason) {
                        Log.e(TAG, "STEP 5 FAILED: " + reason);
                    }
                });
    }

    private void step6SyncStatus(long deviceId, long applianceId) {
        Log.d(TAG, "STEP 6: syncing /status...");
        container.applianceRepository.syncStatusFromDevice(deviceId,
                new com.legacycodes.smarthome.data.repository.ApplianceRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void ignored) {
                        Log.d(TAG, "STEP 6 OK: Room states match ESP32");
                        step7Cleanup(applianceId, deviceId);
                    }

                    @Override
                    public void onError(String reason) {
                        Log.e(TAG, "STEP 6 FAILED: " + reason);
                    }
                });
    }

    private void step7Cleanup(long applianceId, long deviceId) {
        Log.d(TAG, "STEP 7: cleanup...");
        AppExecutors.get().diskIO().execute(() -> {
            container.database.applianceDao().deleteById(applianceId);
            container.database.deviceDao().deleteById(deviceId);
            AppExecutors.get().mainThread().execute(() -> {
                Log.d(TAG, "STEP 7 OK: cleaned up");
                Log.d(TAG, "=== PHASE 2 SELF TEST COMPLETE ===");
                Toast.makeText(this, "Phase 2 self-test passed!", Toast.LENGTH_LONG).show();
            });
        });
    }
}