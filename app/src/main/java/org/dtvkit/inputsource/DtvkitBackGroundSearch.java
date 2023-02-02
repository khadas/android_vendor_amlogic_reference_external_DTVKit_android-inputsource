package com.droidlogic.dtvkit.inputsource;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.view.KeyEvent;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.util.TypedValue;
import android.os.Handler;

import com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService;
import com.droidlogic.dtvkit.companionlibrary.utils.TvContractUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

import com.droidlogic.fragment.DvbsParameterManager;
import com.droidlogic.fragment.ParameterManager;
import com.droidlogic.settings.ConstantManager;
import org.droidlogic.dtvkit.DtvkitGlueClient;
import com.droidlogic.dtvkit.companionlibrary.model.Channel;

public class DtvkitBackGroundSearch {
    private static final String TAG = "DtvkitBackGroundSearch";
    private static final boolean DEBUG = true;

    private DataManager mDataManager;
    private boolean mStartSync = false;
    private boolean mStartSearch = false;
    private JSONArray mServiceList = null;
    private int mFoundServiceNumber = 0;

    private Context mContext;
    private int mCurrentDvbSource;
    private int mFrequency = -1;//hz
    private String mInputId;
    private BackGroundSearchCallback mBgCallback;
    private Handler mMainHandler = new Handler();

    public static final String SINGLE_FREQUENCY_STATUS_ITEM = "status_item";
    public static final String SINGLE_FREQUENCY_STATUS_SEARCH_START = "search_start";
    public static final String SINGLE_FREQUENCY_STATUS_SEARCH_TERMINATE = "search_terminate";
    public static final String SINGLE_FREQUENCY_STATUS_SEARCH_PROGRESS = "search_progress";
    public static final String SINGLE_FREQUENCY_STATUS_SEARCH_CHANNEL_NUMBER = "search_channel_number";
    public static final String SINGLE_FREQUENCY_STATUS_SEARCH_FINISH = "search_finish";
    public static final String SINGLE_FREQUENCY_STATUS_SAVE_START = "save_start";
    public static final String SINGLE_FREQUENCY_STATUS_SAVE_FINISH = "save_finish";
    public static final String SINGLE_FREQUENCY_CHANNEL_NAME = "channel_name";
    public static final String SINGLE_FREQUENCY_CHANNEL_DISPLAY_NUMBER = "display_number";
    public static final String SINGLE_FREQUENCY_TKGS_USER_MSG = "user_msg";

    private final DtvkitGlueClient.SignalHandler mHandler = new DtvkitGlueClient.SignalHandler() {
        @Override
        public void onSignal(final String signal, final JSONObject data) {
            mMainHandler.post(new Runnable() {
                public void run() {
                    responseOnSignal(signal, data);
                }
            });
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            mMainHandler.post(new Runnable() {
                public void run() {
                    responseOnReceive(intent);
                }
            });
        }
    };

    public DtvkitBackGroundSearch(Context context, int dvbSource, String inputId, BackGroundSearchCallback callback) {
        //auto scan mode
        mContext = context;
        mCurrentDvbSource = dvbSource;
        mInputId = inputId;
        mBgCallback = callback;
        mDataManager = new DataManager(context);
    }

    boolean isCurrentSignalSupportBackgroundSearch() {
        return (mCurrentDvbSource == ParameterManager.SIGNAL_COFDM
                || mCurrentDvbSource == ParameterManager.SIGNAL_QAM
                || (mCurrentDvbSource == ParameterManager.SIGNAL_QPSK && "TKGS".equals(mDataManager.getStringParameters(ParameterManager.DVBS_OPERATOR_MODE))));

    }

    private String getStartSearchCommand(boolean isAutoScan) {
        String ret = null;
        switch (mCurrentDvbSource) {
            case ParameterManager.SIGNAL_COFDM: {
                ret = "Dvbt.startSearch";
            }
            break;
            case ParameterManager.SIGNAL_QAM: {
                ret = "Dvbc.startSearchEx";
            }
            break;
            case ParameterManager.SIGNAL_QPSK: {
                ret = "Dvbs.startDvbUpdate";
            }
            break;
            default:
                break;
        }
        return ret;
    }

    private String getFinishSearchCommand() {
        String ret = null;
        switch (mCurrentDvbSource) {
            case ParameterManager.SIGNAL_COFDM: {
                ret = "Dvbt.finishSearch";
            }
            break;
            case ParameterManager.SIGNAL_QAM: {
                ret = "Dvbc.finishSearch";
            }
            break;
            case ParameterManager.SIGNAL_QPSK:
                ret = "Dvbs.finishSearch";
                break;
            default:
                break;
        }
        return ret;
    }

    private JSONArray initAutoScanParameter() {
        //only for dvbt/dvbc
        JSONArray args = new JSONArray();
        switch (mCurrentDvbSource) {
            case ParameterManager.SIGNAL_COFDM:
                args.put(true);
                break;
            case ParameterManager.SIGNAL_QAM:
                args.put("full");
                break;
            case ParameterManager.SIGNAL_QPSK:
                args.put("standby");
                args.put(0x0301);
            default:
                break;
        }
        return args;
    }

    public void startBackGroundAutoSearch() {
        if (!isCurrentSignalSupportBackgroundSearch()) {
            mBgCallback = null;
            return;
        }
        startMonitoringSearch();
        mFoundServiceNumber = 0;
        try {
            JSONArray args = new JSONArray();
            args.put(true); // Commit
            String finishCommand = getFinishSearchCommand();
            if (!TextUtils.isEmpty(finishCommand)) {
                DtvkitGlueClient.getInstance().request(getFinishSearchCommand(), args);
            }
        } catch (Exception e) {
            Log.i(TAG, "startBackGroundAutoSearch Failed to finish search " + e.getMessage());
            return;
        }

        try {
            JSONArray args = initAutoScanParameter();
            if (args != null) {
                String command = getStartSearchCommand(true);
                Log.d(TAG, "startBackGroundAutoSearch command = " + command + ", args = " + args.toString());
                if (!TextUtils.isEmpty(command)) {
                    DtvkitGlueClient.getInstance().request(command, args);
                    mStartSearch = true;
                }
            } else {
                stopSearch(false);
            }
        } catch (Exception e) {
            Log.i(TAG, "startBackGroundAutoSearch search Exception " + e.getMessage());
            stopSearch(false);
        }
    }

    public void stopSearch(boolean commitNewService) {
        mStartSearch = false;
        stopMonitoringSearch();
        try {
            JSONArray args = new JSONArray();
            args.put(commitNewService);
            String finishCommand = getFinishSearchCommand();
            if (!TextUtils.isEmpty(finishCommand)) {
                DtvkitGlueClient.getInstance().request(getFinishSearchCommand(), args);
            }
        } catch (Exception e) {
            Log.i(TAG, "stopSearch failed to finish search Exception " + e.getMessage());
            return;
        }
    }

    private void onSearchFinished() {
        mStartSearch = false;
        stopMonitoringSearch();
        if (mCurrentDvbSource == ParameterManager.SIGNAL_QPSK && "TKGS".equals(mDataManager.getStringParameters(ParameterManager.DVBS_OPERATOR_MODE))) {
            showTKGSUserMsg();
        }
        try {
            JSONArray args = new JSONArray();
            args.put(true); // Commit
            String finishCommand = getFinishSearchCommand();
            if (!TextUtils.isEmpty(finishCommand)) {
                DtvkitGlueClient.getInstance().request(getFinishSearchCommand(), args);
            }
        } catch (Exception e) {
            Log.i(TAG, "onSearchFinished failed to finish search Exception " + e.getMessage());
            return;
        }
        //update search results as After the search is finished, the lcn will be reordered
        mServiceList = getServiceList();
        mFoundServiceNumber = getFoundServiceNumber();
        if (mFoundServiceNumber == 0 && mServiceList != null && mServiceList.length() > 0) {
            Log.d(TAG, "mFoundServiceNumber erro use mServiceList length = " + mServiceList.length());
            mFoundServiceNumber = mServiceList.length();
        }
        startMonitoringSync();
        // By default, gets all channels and 1 hour of programs (DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS)

        // If the intent that started this activity is from Live Channels app
        Bundle parameters = new Bundle();
        parameters.putString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_MODE, EpgSyncJobService.BUNDLE_VALUE_SYNC_SEARCHED_MODE_MANUAL);
        parameters.putString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_SIGNAL_TYPE, TvContractUtils.dvbSourceToDbString(mCurrentDvbSource));

        Intent intent = new Intent(mContext, com.droidlogic.dtvkit.inputsource.DtvkitEpgSync.class);
        intent.putExtra("inputId", mInputId);
        intent.putExtra(EpgSyncJobService.BUNDLE_KEY_SYNC_FROM, TAG);
        intent.putExtra(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_CHANNEL, (mFoundServiceNumber > 0));
        intent.putExtra(EpgSyncJobService.BUNDLE_KEY_SYNC_PARAMETERS, parameters);
        mContext.startService(intent);
    }

    private void showTKGSUserMsg() {
        if (mBgCallback != null) {
            try {
                JSONObject mess = new JSONObject();
                mess.put(SINGLE_FREQUENCY_STATUS_ITEM, SINGLE_FREQUENCY_TKGS_USER_MSG);
                mBgCallback.onMessageCallback(mess);
            } catch (JSONException e) {
                Log.i(TAG, "showTKGSUserMsg JSONException " + e.getMessage());
            }
        }
    }

    private void startMonitoringSearch() {
        DtvkitGlueClient.getInstance().registerSignalHandler(mHandler);
        if (mBgCallback != null) {
            try {
                JSONObject mess = new JSONObject();
                mess.put(SINGLE_FREQUENCY_STATUS_ITEM, SINGLE_FREQUENCY_STATUS_SEARCH_START);
                mBgCallback.onMessageCallback(mess);
                Log.i(TAG, "startMonitoringSearch " + mess.toString());
            } catch (JSONException e) {
                Log.i(TAG, "startMonitoringSearch JSONException " + e.getMessage());
            }
        }
    }

    private void stopMonitoringSearch() {
        DtvkitGlueClient.getInstance().unregisterSignalHandler(mHandler);
        if (mBgCallback != null) {
            try {
                JSONObject mess = new JSONObject();
                mess.put(SINGLE_FREQUENCY_STATUS_ITEM, SINGLE_FREQUENCY_STATUS_SEARCH_FINISH);
                mBgCallback.onMessageCallback(mess);
                Log.i(TAG, "stopMonitoringSearch " + mess.toString());
            } catch (JSONException e) {
                Log.i(TAG, "stopMonitoringSearch JSONException " + e.getMessage());
            }
        }
    }

    private void onSearchTerminate() {
        DtvkitGlueClient.getInstance().unregisterSignalHandler(mHandler);
        if (mBgCallback != null) {
            try {
                JSONObject mess = new JSONObject();
                mess.put(SINGLE_FREQUENCY_STATUS_ITEM, SINGLE_FREQUENCY_STATUS_SEARCH_TERMINATE);
                mBgCallback.onMessageCallback(mess);
                Log.i(TAG, "onSearchFailed " + mess.toString());
            } catch (JSONException e) {
                Log.i(TAG, "onSearchFailed JSONException " + e.getMessage());
            }
        }
    }

    private void startMonitoringSync() {
        mStartSync = true;
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mReceiver,
                new IntentFilter(EpgSyncJobService.ACTION_SYNC_STATUS_CHANGED));
        if (mBgCallback != null) {
            try {
                JSONObject mess = new JSONObject();
                mess.put(SINGLE_FREQUENCY_STATUS_ITEM, SINGLE_FREQUENCY_STATUS_SAVE_START);
                mBgCallback.onMessageCallback(mess);
                Log.i(TAG, "startMonitoringSync " + mess.toString());
            } catch (JSONException e) {
                Log.i(TAG, "startMonitoringSync JSONException " + e.getMessage());
            }
        }
    }

    private void stopMonitoringSync(boolean error) {
        mStartSync = false;
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
        if (mBgCallback != null) {
            try {
                JSONObject mess = getFirstTwoSearchedChannel(mFrequency, error);
                mBgCallback.onMessageCallback(mess);
                Log.i(TAG, "stopMonitoringSync onMessageCallback " + mess.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private JSONObject getFirstTwoSearchedChannel(int frequency, boolean error) {
        JSONObject result = null;
        String firstServiceName = "";
        String firstServiceDisplayNumber = "";
        int foundFrequency = 0;
        int foundCount = 0;
        if (mServiceList == null || mServiceList.length() == 0 || error) {
            try {
                if (result == null) {
                    result = new JSONObject();
                    result.put(SINGLE_FREQUENCY_STATUS_ITEM, SINGLE_FREQUENCY_STATUS_SAVE_FINISH);
                }
            } catch (JSONException e) {
                Log.i(TAG, "getFirstTwoSearchedChannel JSONException1 = " + e.getMessage());
            }
        } else {
            try {
                for (int i = 0; i < mServiceList.length(); i++) {
                    firstServiceName = mServiceList.getJSONObject(i).getString("name");
                    firstServiceDisplayNumber = String.format(Locale.ENGLISH, "%d", mServiceList.getJSONObject(i).getInt("lcn"));
                    foundFrequency = mServiceList.getJSONObject(i).getInt("freq");
                    if (foundFrequency == frequency) {
                        if (result == null) {
                            result = new JSONObject();
                            result.put(SINGLE_FREQUENCY_STATUS_ITEM, SINGLE_FREQUENCY_STATUS_SAVE_FINISH);
                        }
                        result.put(SINGLE_FREQUENCY_CHANNEL_NAME + foundCount, firstServiceName);
                        result.put(SINGLE_FREQUENCY_CHANNEL_DISPLAY_NUMBER + foundCount, firstServiceDisplayNumber);
                        foundCount++;
                        if (foundCount >= 2) {
                            break;
                        }
                    }
                }
                if (result == null) {
                    result = new JSONObject();
                    result.put(SINGLE_FREQUENCY_STATUS_ITEM, SINGLE_FREQUENCY_STATUS_SAVE_FINISH);
                }
            } catch (JSONException e) {
                Log.i(TAG, "getFirstTwoSearchedChannel JSONException2 = " + e.getMessage());
            }
        }
        return result;
    }

    private int getFoundServiceNumber() {
        int found = 0;
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.getNumberOfServices", new JSONArray());
            found = obj.getInt("data");
            Log.i(TAG, "getFoundServiceNumber found = " + found);
        } catch (Exception ignore) {
            Log.i(TAG, "getFoundServiceNumber Exception = " + ignore.getMessage());
        }
        return found;
    }

    private int getSearchProcess(JSONObject data) {
        int progress = 0;
        if (data == null) {
            return progress;
        }
        try {
            progress = data.getInt("progress");
        } catch (JSONException ignore) {
            Log.i(TAG, "getSearchProcess Exception = " + ignore.getMessage());
        }
        return progress;
    }

    private JSONArray getServiceList() {
        JSONArray result = null;
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.getListOfServicesByIndex", new JSONArray());
            JSONArray services = obj.getJSONArray("data");
            result = services;
            for (int i = 0; i < services.length(); i++) {
                JSONObject service = services.getJSONObject(i);
                if (DEBUG) {
                    Log.i(TAG, "getServiceList service = " + service.toString());
                }
            }
        } catch (Exception e) {
            Log.i(TAG, "getServiceList Exception = " + e.getMessage());
        }
        return result;
    }

    private void responseOnSignal(String signal, JSONObject data) {
        if (signal.equals("DvbtStatusChanged") || signal.equals("DvbcStatusChanged") || signal.equals("DvbsStatusChanged")) {
            int progress = getSearchProcess(data);
            if (progress < 0 || progress > 100) {
                Log.d(TAG, "Invalid progress " + progress + ", low level scan has been terminated");
                onSearchTerminate();
            } else if (progress < 100) {
                Log.d(TAG, "onSignal progress = " + progress);
                if (mBgCallback != null) {
                    try {
                        JSONObject mess = new JSONObject();
                        mess.put(SINGLE_FREQUENCY_STATUS_ITEM, SINGLE_FREQUENCY_STATUS_SEARCH_PROGRESS);
                        mess.put(SINGLE_FREQUENCY_STATUS_SEARCH_PROGRESS, progress);
                        mBgCallback.onMessageCallback(mess);
                        Log.i(TAG, "onSignal search progress " + mess.toString());
                    } catch (JSONException e) {
                        Log.i(TAG, "onSignal JSONException1 " + e.getMessage());
                    }
                }
            } else {
                Log.d(TAG, "onSignal search finished");
                onSearchFinished();
            }
        }
    }

    private void responseOnReceive(Intent intent) {
        if (intent != null) {
            String from = intent.getStringExtra(EpgSyncJobService.BUNDLE_KEY_SYNC_FROM);
            String status = intent.getStringExtra(EpgSyncJobService.SYNC_STATUS);
            if (status.equals(EpgSyncJobService.SYNC_FINISHED)) {
                if (from == null || !TAG.equals(from)) {
                    Log.i(TAG, "Sync Msg is from:" + from);
                    return;
                }
                stopMonitoringSync(false);
            } else if (status.equals(EpgSyncJobService.SYNC_ERROR)) {
                if (from == null || !TAG.equals(from)) {
                    Log.i(TAG, "Sync Error Msg is from:" + from);
                    return;
                }
                stopMonitoringSync(true);
            }
        } else {
            Log.d(TAG, "responseOnReceive null");
        }
    }

    public void handleScreenOn() {
        if (!mStartSync && mStartSearch) {
           //If is in syncing, we do nothing wait sync finish
           stopSearch(false);
           onSearchTerminate();
        }
    }

    public interface BackGroundSearchCallback {
        void onMessageCallback(JSONObject mess);
    }
}
