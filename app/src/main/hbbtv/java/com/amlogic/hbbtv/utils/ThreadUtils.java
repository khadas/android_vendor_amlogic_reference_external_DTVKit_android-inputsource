package com.amlogic.hbbtv.utils;

import android.os.Looper;

public class ThreadUtils {
    public static void checkMainThread() {
        if (!Looper.getMainLooper().isCurrentThread()) {
            throw new IllegalStateException("Must be called on the UI thread.");
        }
    }

    private ThreadUtils() {}
}
