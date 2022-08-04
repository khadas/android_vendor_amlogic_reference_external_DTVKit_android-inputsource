package com.droidlogic.dtvkit.inputsource;

import android.app.ActivityManager;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;

import android.text.TextUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.droidlogic.dtvkit.DtvkitGlueClient;

public class DtvKitScheduleManager {
    private static final String TAG = "DtvKitScheduleManager";

    private static final String LIVE_TV_PACKAGE = "com.droidlogic.android.tv";
    private static final String RECEIVER = "com.android.tv.receiver.ScheduleManagerReceiver";

    private static final int PREPARE_START_SCHEDULE = 1;
    private static final int START_SCHEDULE = 2;
    private static final int STOP_SCHEDULE = 3;

    private static final String BOOKING_ID = "bookingid";

    private DtvkitGlueClient mDtvkitGlueClient = null;
    private Context mContext = null;
    private final DtvkitGlueClient.SignalHandler mScheduleManagerHandler = (signal, data) -> {
        //Log.d(TAG, "SignalHandler = " + signal);
        if (signal.contains("TimerPrepareStart")) {
            try {
                int bookingId = data.getInt(BOOKING_ID);
                Log.i(TAG, "TimerPrepareStart, booking id = " + bookingId);
                handlePrepareStartScheduleEvent(bookingId);
            } catch (JSONException ignore) {
                Log.e(TAG, ignore.getMessage());
            }

        } else if (signal.contains("TimerStart")) {
            try {
                int bookingId = data.getInt(BOOKING_ID);
                Log.i(TAG, "TimerStart, booking id = " + bookingId);
                handleStartScheduleEvent(bookingId);
            } catch (JSONException ignore) {
                Log.e(TAG, ignore.getMessage());
            }

        } else if (signal.contains("TimerStop")) {
            try {
                int bookingId = data.getInt(BOOKING_ID);
                Log.i(TAG, "TimerStop, booking id = " + bookingId);
                handleStopScheduleEvent(bookingId);
            } catch (JSONException ignore) {
                Log.e(TAG, ignore.getMessage());
            }
        }
    };

    public DtvKitScheduleManager(Context context, DtvkitGlueClient client, boolean needRegisterReceiver){
        mDtvkitGlueClient = client;
        mContext = context;
        if (needRegisterReceiver) {
            initScheduleEventHandle();
        }
    }

    public String addBooking(String bookData) {
        String result = null;
        try {
            JSONArray args = new JSONArray(bookData);
            int handle = DtvkitGlueClient.getInstance().request("Booking.addBooking", args).getInt("data");
            result = String.valueOf(handle);
        } catch (Exception e) {
            Log.i(TAG, "addBooking Exception = " + e.getMessage());
        } finally {
            return result;
        }
    }

    public void deleteBooking(String data) {
        try {
            int bookHandle = 0;
            if (null != data) {
                bookHandle = Integer.parseInt(data);
            } else {
                Log.d(TAG, "deleteBooking handle error = " + data);
                return;
            }
            JSONArray args = new JSONArray();
            args.put(bookHandle);
            DtvkitGlueClient.getInstance().request("Booking.deleteBooking", args);
        } catch (Exception e) {
            Log.i(TAG, "deleteBooking Exception = " + e.getMessage());
        }
    }

    public String getListOfBookings(String type) {
        JSONArray bookingList = null;
        try {
            JSONArray args = new JSONArray();
            args.put(type);
            bookingList = DtvkitGlueClient.getInstance().request("Booking.getListOfBookings", args).getJSONArray("data");
        } catch (Exception e) {
            Log.i(TAG, "getListOfBookings Exception = " + e.getMessage());
        } finally {
            if (null != bookingList) {
                return bookingList.toString();
            } else {
                return null;
            }
        }
    }

    public void release() {
        Log.d(TAG, "release ScheduleManager");
        mDtvkitGlueClient.unregisterSignalHandler(mScheduleManagerHandler);
        mDtvkitGlueClient = null;
        mContext = null;
    }

    private void initScheduleEventHandle () {
        Log.d(TAG, "initScheduleEventHandle");
        mDtvkitGlueClient.registerSignalHandler(mScheduleManagerHandler);
    }

    private void handlePrepareStartScheduleEvent(int bookingId) {
        //1.live tv in foreground
        //2.send boardcast to start schedule dialog
        //if (isAppForeground()) {
        sendScheduleBroadcast(bookingId, PREPARE_START_SCHEDULE);
        //}
        //TBD:live tv in background start background record
    }

    private void handleStartScheduleEvent(int bookingId) {
        sendScheduleBroadcast(bookingId, START_SCHEDULE);
    }

    private void handleStopScheduleEvent(int bookingId) {
        //1.current schedule is runing and is record
        //2.stop current record
        sendScheduleBroadcast(bookingId, STOP_SCHEDULE);
    }

    private boolean isAppForeground() {
        //com.droidlogic.android.tv/com.android.tv.MainActivity
        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processInfoList = am.getRunningAppProcesses();
        Log.d(TAG, "processInfoList = " + processInfoList);
        for (ActivityManager.RunningAppProcessInfo processInfo : processInfoList) {
            if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    && processInfo.pkgList[0].equals(LIVE_TV_PACKAGE)) {
                return true;
            }
        }
        return false;
    }

    private void sendScheduleBroadcast(int bookId, int flag) {
        Log.d(TAG, "sendScheduleBroadcast bookId = " + bookId + "|flag = " + flag);
        Intent intent = new Intent("droidlogic.intent.action.droid_schedule_manager");
        intent.setComponent(new ComponentName(LIVE_TV_PACKAGE, RECEIVER));
        intent.putExtra("scheduleId", bookId);
        intent.putExtra("scheduleEvent", flag);
        mContext.sendBroadcast(intent);
    }
}
