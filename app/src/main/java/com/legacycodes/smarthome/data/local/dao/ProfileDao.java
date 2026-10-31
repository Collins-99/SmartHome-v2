package com.legacycodes.smarthome.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.legacycodes.smarthome.data.local.entity.ProfileEntity;

@Dao
public interface ProfileDao {

    /** Insert or replace — profile is a single row with id = 1. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(ProfileEntity profile);

    @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
    LiveData<ProfileEntity> observe();

    @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
    ProfileEntity getBlocking();

    @Query("DELETE FROM profile")
    void clear();
}