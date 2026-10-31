package com.legacycodes.smarthome;

import android.app.Application;

/**
 * Application class. Built once when the process starts.
 * Owns the single AppContainer instance for the app's lifetime.
 *
 * Must be registered in AndroidManifest.xml via
 *     android:name=".SmartHomeApp"
 * on the <application> tag, or getContainer() will be null everywhere.
 */
public class SmartHomeApp extends Application {

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
    }

    public AppContainer getContainer() {
        return container;
    }
}