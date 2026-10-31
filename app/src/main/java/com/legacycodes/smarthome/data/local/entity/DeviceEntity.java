package com.legacycodes.smarthome.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity for a single ESP32 / home.
 *
 * The IP is stored as a String exactly as the user typed it
 * (e.g. "192.168.137.219"). We never store a full URL — the repository
 * builds "http://<ip>" per request so the IP is truly dynamic.
 *
 * availablePinsJson caches the /info response so the add-appliance
 * dropdown works even when the ESP32 is temporarily unreachable.
 * Stored as a JSON array string, e.g. "[4,5,13,14,25,26,27,32,33]".
 */
@Entity(
        tableName = "devices",
        indices = { @Index(value = "ip_address", unique = true) }
)
public class DeviceEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "ip_address")
    public String ipAddress;

    /** Epoch millis of last successful /ping, 0 if never. */
    @ColumnInfo(name = "last_seen")
    public long lastSeen;

    /** Populated from /info. */
    @ColumnInfo(name = "device_name")
    public String deviceName;

    @ColumnInfo(name = "firmware")
    public String firmware;

    /**
     * JSON array string of available GPIO pins, cached from /info.
     * Null until the first successful fetchAndSaveInfo call.
     */
    @ColumnInfo(name = "available_pins_json")
    public String availablePinsJson;

    public DeviceEntity() {
        this.lastSeen = 0L;
    }
}