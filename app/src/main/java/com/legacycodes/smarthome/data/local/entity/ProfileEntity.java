package com.legacycodes.smarthome.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity for the single user profile.
 *
 * This table is intentionally capped at ONE row — id is always 1.
 * A constant primary key is the simplest way to enforce "one profile
 * per install". The repository does INSERT OR REPLACE with id=1 on
 * every save.
 */
@Entity(tableName = "profile")
public class ProfileEntity {

    /** Always 1. Enforced by the repository, documented here. */
    public static final long SINGLETON_ID = 1L;

    @PrimaryKey
    public long id;

    @ColumnInfo(name = "user_name")
    public String userName;

    @ColumnInfo(name = "house_name")
    public String houseName;

    public ProfileEntity() {
        this.id = SINGLETON_ID;
        this.userName = "";
        this.houseName = "";
    }
}