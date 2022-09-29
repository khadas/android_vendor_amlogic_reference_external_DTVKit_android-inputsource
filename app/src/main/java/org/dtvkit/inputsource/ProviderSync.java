package com.droidlogic.dtvkit.inputsource;

import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.lang.Runnable;

public class ProviderSync {
    private static final String TAG = "ProviderSync";
    ExecutorService EXECUTOR = null;

    public ProviderSync() {
        EXECUTOR = Executors.newFixedThreadPool(1);
    }

    public void run(Runnable task) {
        if (null != EXECUTOR) {
            EXECUTOR.submit(task);
            Log.d(TAG, "ProviderSync start task submit ");
        } else {
            Log.e(TAG, "ProviderSync error EXECUTOR is null  ");
        }
    }

    public void shutDown() {
        if (null != EXECUTOR) {
            if (!EXECUTOR.isShutdown()) {
                EXECUTOR.shutdown();
                Log.d(TAG, "ProviderSync shutDown ");
            }
        }
    }
}

