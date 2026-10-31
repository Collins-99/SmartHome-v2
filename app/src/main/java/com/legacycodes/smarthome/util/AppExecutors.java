package com.legacycodes.smarthome.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Central thread pool for the app.
 *
 * Why a single source of truth for threading:
 *   Scattered ExecutorService / new Thread() calls are the #1 cause of
 *   "why does this sometimes work?" bugs. When every DB write goes
 *   through diskIO() and every network call goes through networkIO(),
 *   the threading model is one mental model, not N.
 *
 * diskIO: single-thread. Room serializes writes anyway — a pool wouldn't
 *   help and would only cause lock contention and out-of-order updates.
 *   Single thread = strict FIFO ordering of writes.
 *
 * networkIO: 3 threads. A typical interaction fires 1-2 requests. 3 gives
 *   headroom for status polling while the user toggles something.
 *
 * mainThread: the Android main thread, accessed via Handler. This is how
 *   repositories hand results back to the UI safely.
 */
public final class AppExecutors {

    private static volatile AppExecutors INSTANCE;

    private final Executor diskIO;
    private final Executor networkIO;
    private final Executor mainThread;

    private AppExecutors() {
        this.diskIO     = Executors.newSingleThreadExecutor();
        this.networkIO  = Executors.newFixedThreadPool(3);
        this.mainThread = new MainThreadExecutor();
    }

    public static AppExecutors get() {
        if (INSTANCE == null) {
            synchronized (AppExecutors.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppExecutors();
                }
            }
        }
        return INSTANCE;
    }

    public Executor diskIO()     { return diskIO; }
    public Executor networkIO()  { return networkIO; }
    public Executor mainThread() { return mainThread; }

    private static final class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    }
}