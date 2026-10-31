package com.legacycodes.smarthome.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.legacycodes.smarthome.data.local.entity.DeviceEntity;

import java.util.List;

@Dao
public interface DeviceDao {

    // ---- Writes ----

    @Insert
    long insert(DeviceEntity device);

    @Update
    void update(DeviceEntity device);

    @Delete
    void delete(DeviceEntity device);

    @Query("DELETE FROM devices WHERE id = :deviceId")
    void deleteById(long deviceId);

    // ---- Reads (LiveData, for UI) ----

    @Query("SELECT * FROM devices ORDER BY id ASC")
    LiveData<List<DeviceEntity>> observeAll();

    @Query("SELECT * FROM devices WHERE id = :deviceId LIMIT 1")
    LiveData<DeviceEntity> observeById(long deviceId);

    @Query("SELECT * FROM devices ORDER BY id ASC LIMIT 1")
    LiveData<DeviceEntity> observePrimary();

    // ---- Reads (synchronous, background thread only) ----

    @Query("SELECT * FROM devices ORDER BY id ASC LIMIT 1")
    DeviceEntity getPrimaryBlocking();

    @Query("SELECT * FROM devices WHERE id = :deviceId LIMIT 1")
    DeviceEntity getByIdBlocking(long deviceId);

    @Query("SELECT * FROM devices WHERE ip_address = :ip LIMIT 1")
    DeviceEntity getByIpBlocking(String ip);

    @Query("SELECT COUNT(*) FROM devices")
    int countBlocking();

    // ---- Targeted updates ----

    @Query("UPDATE devices SET last_seen = :timestamp WHERE id = :deviceId")
    void updateLastSeen(long deviceId, long timestamp);

    @Query("UPDATE devices SET device_name = :deviceName, " +
            "firmware = :firmware, last_seen = :timestamp " +
            "WHERE id = :deviceId")
    void updateInfo(long deviceId, String deviceName, String firmware, long timestamp);

    @Query("UPDATE devices SET ip_address = :newIp WHERE id = :deviceId")
    void updateIp(long deviceId, String newIp);

    @Query("UPDATE devices SET available_pins_json = :pinsJson WHERE id = :deviceId")
    void updateAvailablePins(long deviceId, String pinsJson);
}