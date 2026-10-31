package com.legacycodes.smarthome.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.legacycodes.smarthome.data.local.entity.ApplianceEntity;

import java.util.List;

@Dao
public interface ApplianceDao {

    // ---- Writes ----

    @Insert
    long insert(ApplianceEntity appliance);

    @Update
    void update(ApplianceEntity appliance);

    @Delete
    void delete(ApplianceEntity appliance);

    @Query("DELETE FROM appliances WHERE id = :applianceId")
    void deleteById(long applianceId);

    @Query("DELETE FROM appliances WHERE device_id = :deviceId")
    void deleteAllForDevice(long deviceId);

    // ---- Reads (LiveData, for UI) ----

    @Query("SELECT * FROM appliances WHERE device_id = :deviceId ORDER BY id ASC")
    LiveData<List<ApplianceEntity>> observeAllForDevice(long deviceId);

    @Query("SELECT * FROM appliances WHERE id = :applianceId LIMIT 1")
    LiveData<ApplianceEntity> observeById(long applianceId);

    /** Used by the add-screen to grey out already-used pins in the dropdown. */
    @Query("SELECT gpio_pin FROM appliances WHERE device_id = :deviceId")
    LiveData<List<Integer>> observeUsedPins(long deviceId);

    /** Live count — powers the empty-state ("no appliances yet"). */
    @Query("SELECT COUNT(*) FROM appliances WHERE device_id = :deviceId")
    LiveData<Integer> observeCount(long deviceId);

    // ---- Reads (synchronous, background thread only) ----

    @Query("SELECT * FROM appliances WHERE id = :applianceId LIMIT 1")
    ApplianceEntity getByIdBlocking(long applianceId);

    @Query("SELECT * FROM appliances WHERE device_id = :deviceId ORDER BY id ASC")
    List<ApplianceEntity> getAllForDeviceBlocking(long deviceId);

    @Query("SELECT gpio_pin FROM appliances WHERE device_id = :deviceId")
    List<Integer> getUsedPinsBlocking(long deviceId);

    // ---- Targeted updates ----

    @Query("UPDATE appliances SET state = :state WHERE id = :applianceId")
    void updateState(long applianceId, int state);

    @Query("UPDATE appliances SET state = :state WHERE device_id = :deviceId AND gpio_pin = :pin")
    void updateStateByPin(long deviceId, int pin, int state);
}