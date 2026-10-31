package com.legacycodes.smarthome.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.legacycodes.smarthome.data.local.AppDatabase;
import com.legacycodes.smarthome.data.local.entity.ProfileEntity;
import com.legacycodes.smarthome.util.AppExecutors;

/**
 * Repository for the single user profile.
 * One row with id = 1. Save = INSERT OR REPLACE.
 */
public class ProfileRepository {

    private static final String TAG = "ProfileRepository";

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String reason);
    }

    private final AppDatabase database;
    private final AppExecutors executors;

    public ProfileRepository(AppDatabase database, AppExecutors executors) {
        this.database = database;
        this.executors = executors;
    }

    public LiveData<ProfileEntity> observe() {
        return database.profileDao().observe();
    }

    public void save(String userName, String houseName, Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            try {
                ProfileEntity p = new ProfileEntity();
                p.id = ProfileEntity.SINGLETON_ID;
                p.userName = userName != null ? userName : "";
                p.houseName = houseName != null ? houseName : "";
                database.profileDao().upsert(p);
                executors.mainThread().execute(() -> callback.onSuccess(null));
            } catch (Exception e) {
                Log.e(TAG, "save profile failed", e);
                executors.mainThread().execute(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public ProfileEntity getBlocking() {
        return database.profileDao().getBlocking();
    }
}