package com.legacycodes.smarthome.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity for one appliance.
 *
 * Foreign key to devices — an appliance cannot exist without a device.
 * ON DELETE CASCADE removes appliances when their device is deleted.
 *
 * UNIQUE(device_id, gpio_pin) is a hard database guarantee that two
 * appliances on the same ESP32 can't fight over the same pin.
 *
 * The type is stored as a String (enum NAME, e.g. "LIGHT"), never
 * as an ordinal — safe against enum reordering.
 */
@Entity(
        tableName = "appliances",
        foreignKeys = @ForeignKey(
                entity = DeviceEntity.class,
                parentColumns = "id",
                childColumns = "device_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = "device_id"),
                @Index(value = { "device_id", "gpio_pin" }, unique = true)
        }
)
public class ApplianceEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "device_id")
    public long deviceId;

    @ColumnInfo(name = "name")
    public String name;

    /** ApplianceType enum name, e.g. "LIGHT". Never the ordinal. */
    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "gpio_pin")
    public int gpioPin;

    @ColumnInfo(name = "room")
    public String room;

    /** Logical state: 1 = on, 0 = off. Last known. */
    @ColumnInfo(name = "state")
    public int state;

    public ApplianceEntity() {
        this.room = "";
        this.state = 0;
    }
}