package com.legacycodes.smarthome.data.local;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.legacycodes.smarthome.data.local.dao.ApplianceDao;
import com.legacycodes.smarthome.data.local.dao.DeviceDao;
import com.legacycodes.smarthome.data.local.dao.ProfileDao;
import com.legacycodes.smarthome.data.local.entity.ApplianceEntity;
import com.legacycodes.smarthome.data.local.entity.DeviceEntity;
import com.legacycodes.smarthome.data.local.entity.ProfileEntity;

@Database(
        entities = {
                DeviceEntity.class,
                ApplianceEntity.class,
                ProfileEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";
    private static final String DB_NAME = "smarthome.db";

    // ---- DAO accessors ----

    public abstract DeviceDao deviceDao();
    public abstract ApplianceDao applianceDao();
    public abstract ProfileDao profileDao();

    // ---- Singleton ----

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = build(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private static AppDatabase build(@NonNull Context appContext) {
        return Room.databaseBuilder(appContext, AppDatabase.class, DB_NAME)
                .addCallback(new Callback() {
                    @Override
                    public void onOpen(@NonNull SupportSQLiteDatabase db) {
                        super.onOpen(db);
                        // CRITICAL: SQLite disables foreign-key enforcement by default.
                        // Without this pragma, ON DELETE CASCADE does nothing.
                        db.execSQL("PRAGMA foreign_keys = ON;");
                        Log.d(TAG, "Database opened; foreign_keys pragma ON");
                    }

                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        Log.d(TAG, "Database created (v1)");
                    }
                })
                // Development convenience — if we ever bump the schema version
                // without writing a Migration, wipe and recreate instead of crash.
                // Remove before any real release.
                .fallbackToDestructiveMigration()
                .build();
    }
}