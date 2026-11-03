package com.legacycodes.smarthome.ui.addedit;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.legacycodes.smarthome.AppContainer;
import com.legacycodes.smarthome.SmartHomeApp;
import com.legacycodes.smarthome.data.local.entity.ApplianceEntity;
import com.legacycodes.smarthome.data.local.entity.DeviceEntity;
import com.legacycodes.smarthome.data.repository.ApplianceRepository;
import com.legacycodes.smarthome.data.repository.DeviceRepository;
import com.legacycodes.smarthome.domain.model.ApplianceType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddEditViewModel extends AndroidViewModel {

    private final ApplianceRepository applianceRepo;
    private final DeviceRepository deviceRepo;

    private final MutableLiveData<Long> deviceIdLive = new MutableLiveData<>();
    private final MutableLiveData<Long> applianceIdLive = new MutableLiveData<>();

    private final LiveData<DeviceEntity> deviceLive;
    private final LiveData<List<Integer>> availablePinsLive;

    private ApplianceType selectedType;
    private int selectedPin = -1;
    private String selectedName = "";
    private int preservedState = 0;

    public AddEditViewModel(@NonNull Application application) {
        super(application);

        AppContainer c = ((SmartHomeApp) application).getContainer();
        applianceRepo = c.applianceRepository;
        deviceRepo = c.deviceRepository;

        deviceLive = deviceRepo.observePrimary();

        availablePinsLive = Transformations.switchMap(deviceIdLive, id -> {
            if (id == null || id <= 0) {
                return new MutableLiveData<>(Collections.emptyList());
            }
            return Transformations.map(
                    applianceRepo.observeUsedPins(id),
                    usedPins -> computeAvailablePins(usedPins));
        });

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

    public LiveData<DeviceEntity> getDevice() {
        return deviceLive;
    }

    public LiveData<List<Integer>> getAvailablePins() {
        return availablePinsLive;
    }

    public void init(long applianceId) {
        applianceIdLive.setValue(applianceId);
    }

    public boolean isEditMode() {
        Long id = applianceIdLive.getValue();
        return id != null && id > 0;
    }

    public long getApplianceId() {
        Long id = applianceIdLive.getValue();
        return id != null ? id : 0L;
    }

    public void setSelectedType(ApplianceType type) {
        this.selectedType = type;
    }

    public void setSelectedPin(int pin) {
        this.selectedPin = pin;
    }

    public void setSelectedName(String name) {
        this.selectedName = name != null ? name.trim() : "";
    }

    public void setPreservedState(int state) {
        this.preservedState = state;
    }

    public interface SaveCallback {
        void onSuccess(long applianceId);
        void onError(String reason);
    }

    public void save(SaveCallback callback) {
        if (selectedType == null) {
            callback.onError("Choose an appliance type");
            return;
        }
        if (selectedName == null || selectedName.trim().isEmpty()) {
            callback.onError("Enter a name");
            return;
        }
        if (selectedPin <= 0) {
            callback.onError("Select a GPIO pin");
            return;
        }

        DeviceEntity device = deviceLive.getValue();
        if (device == null) {
            callback.onError("No device configured");
            return;
        }

        ApplianceEntity entity = new ApplianceEntity();
        entity.deviceId = device.id;
        entity.name = selectedName.trim();
        entity.type = selectedType.name();
        entity.gpioPin = selectedPin;
        entity.room = "";

        if (isEditMode()) {
            entity.id = getApplianceId();
            entity.state = preservedState;
            applianceRepo.updateAppliance(entity, new ApplianceRepository.Callback<Void>() {
                @Override
                public void onSuccess(Void ignored) {
                    callback.onSuccess(entity.id);
                }

                @Override
                public void onError(String reason) {
                    callback.onError(reason);
                }
            });
        } else {
            entity.state = 0;
            applianceRepo.addAppliance(entity, new ApplianceRepository.Callback<Long>() {
                @Override
                public void onSuccess(Long newId) {
                    callback.onSuccess(newId);
                }

                @Override
                public void onError(String reason) {
                    callback.onError(reason);
                }
            });
        }
    }

    public void prefill(ApplianceEntity existing) {
        if (existing == null) return;
        this.selectedType = ApplianceType.fromName(existing.type);
        this.selectedName = existing.name != null ? existing.name : "";
        this.selectedPin = existing.gpioPin;
        this.preservedState = existing.state;
    }

    private List<Integer> computeAvailablePins(List<Integer> usedPins) {
        DeviceEntity device = deviceLive.getValue();
        if (device == null || device.availablePinsJson == null) {
            return Collections.emptyList();
        }
        List<Integer> all = deviceRepo.parseAvailablePins(device);
        if (all.isEmpty()) return Collections.emptyList();

        List<Integer> filtered = new ArrayList<>(all);
        if (usedPins != null) {
            filtered.removeAll(usedPins);
        }
        return filtered;
    }
}