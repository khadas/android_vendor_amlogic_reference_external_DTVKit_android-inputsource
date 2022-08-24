package com.droidlogic.dtvkit.inputsource;

import android.os.Handler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.droidlogic.dtvkit.DtvkitGlueClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DtvkitRequest {
    private static final String TAG = "DtvkitRequest";

    private final ExecutorService mSingleThread = Executors.newSingleThreadExecutor();

    public static interface RequestCallback {
        void onComplete(DtvkitResult result);
    }

    public static class DtvkitResult {

        public boolean isOk() {
            return false;
        }
    }

    public static class Success extends DtvkitResult {
        public JSONArray data;

        public Success() {}

        public Success(JSONArray data) {
            this.data = data;
        }

        public boolean isOk() {
            return true;
        }

        public JSONArray getData() {
            return data;
        }
    }

    public static class Error extends DtvkitResult {
        public Exception exception;

        public Error(Exception exception) {
            this.exception = exception;
        }

        public String getException() {
            return exception != null ? exception.getMessage() : "";
        }
    }

    private static class DtvkitRequestInstance {
        private static DtvkitRequest sInstance = new DtvkitRequest();
    }

    private DtvkitRequest() {}

    public static DtvkitRequest getInstance() {
        return DtvkitRequestInstance.sInstance;
    }

    public void request(Handler handler, final String request,
        final JSONArray args,
        final RequestCallback callback) {
        mSingleThread.execute(()->{
            try {
                DtvkitGlueClient.getInstance().request(request, args);
                handler.post(()->{
                    callback.onComplete(new Success(args));
                });
            } catch (Exception e) {
                callback.onComplete(new Error(e));
            }
        });
    }

    public void request(Handler handler, Runnable runnable,
        final RequestCallback callback) {
        mSingleThread.execute(()-> {
            try {
                runnable.run();
                if (callback != null) {
                    handler.post(()->{
                        callback.onComplete(new Success());
                    });
                }
            } catch (Exception e) {
                if (callback != null) {
                    handler.post(()->{
                        callback.onComplete(new Error(e));
                    });
                }
            }
        });
    }

}
