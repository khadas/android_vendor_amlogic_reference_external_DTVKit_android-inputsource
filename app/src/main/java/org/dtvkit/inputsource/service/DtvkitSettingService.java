package com.droidlogic.dtvkit.inputsource.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.RemoteCallbackList;
import android.provider.Settings;
import android.util.Log;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.droidlogic.dtvkit.IDtvkitSetting;
import com.droidlogic.dtvkit.companionlibrary.utils.TvContractUtils;
import com.droidlogic.fragment.ParameterMananer;
import org.droidlogic.dtvkit.DtvkitGlueClient;
import com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService;
import com.droidlogic.dtvkit.inputsource.DtvkitEpgSync;
import com.droidlogic.settings.SysSettingManager;
import com.amlogic.hbbtv.HbbTvUISetting;
import com.droidlogic.dtvkit.IMGRCallbackListener;

import java.io.File;
import java.util.List;

public class DtvkitSettingService extends Service {
    private static final String TAG = "DtvkitSettingService";
    private static final String DTVKIT_INPUTID = "com.droidlogic.dtvkit.inputsource/.DtvkitTvInput/HW19";
    private static final int SYNC_FINISHED = 0x01;
    private static final int SYNC_RUNNING  = 0x02;
    private static final int SYNC_ONSIGNAL = 0x03;
    public static final String EPGSYNC_STOPPED = "EPGSYNC_STOPPED";
    public static final String EPGSYNC_RUNNING = "EPGSYNC_RUNNING";
    public static final String ONSIGNAL        = "ONSIGNAL";
    protected ParameterMananer mParameterManager;
    protected HbbTvUISetting mHbbTvUISetting;

    @Override
    public void onCreate() {
        super.onCreate();
        mParameterManager = new ParameterMananer(this, DtvkitGlueClient.getInstance());
        mHbbTvUISetting   = new HbbTvUISetting();
        DtvkitGlueClient.getInstance().registerSignalHandler(mSignalHandler);
    }

    @Override
    public IBinder onBind(Intent intent) {
        startMonitoringSync();
        return new DtvkitSettingBinder();
    }

    private DtvkitGlueClient.SignalHandler mSignalHandler= new DtvkitGlueClient.SignalHandler() {
        @Override
        public void onSignal(String signal, JSONObject data) {
            //Log.d(TAG,"signal:"+signal+",json:"+data.toString());
            Message mess = mHandler.obtainMessage(SYNC_ONSIGNAL, 0, 0, ONSIGNAL);
            Bundle b = new Bundle();
            b.putString("signal",  signal);
            b.putString("data",  data.toString());
            mess.setData(b);
            boolean info = mHandler.sendMessage(mess);
        }
    };

    private RemoteCallbackList<IMGRCallbackListener> mListenerList = new RemoteCallbackList<>();

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            //MGRMessage message = new MGRMessage(msg.what, msg.arg1, (String)(msg.obj));
            String message = (String)(msg.obj);;
            String signal = null;
            String data   = null;
            if (msg.what == SYNC_ONSIGNAL) {
                Bundle b = msg.getData();
                signal   = b.getString("signal");
                data     = b.getString("data");
            } else {
                message = (String)(msg.obj);
            }
            try{
                int count = mListenerList.beginBroadcast();
                for (int i = 0; i < count; i++) {
                    mListenerList.getBroadcastItem(i).onRespond(message, signal, data);
                }
                mListenerList.finishBroadcast();
            } catch (RemoteException e){
                e.printStackTrace();
            }
        }
    };

    private class DtvkitSettingBinder extends IDtvkitSetting.Stub {

        @Override
        public int getCurrentMainAudioLanguageId() throws RemoteException {
            return mParameterManager.getCurrentMainAudioLangId();
        }

        @Override
        public int getCurrentMainSubtitleLangId() throws RemoteException {
            return mParameterManager.getCurrentMainSubLangId();
        }

        @Override
        public int getCurrentSecondAudioLanguageId() throws RemoteException {
            return mParameterManager.getCurrentSecondAudioLangId();
        }

        @Override
        public int getSecondSubtitleLangId() throws RemoteException {
            return mParameterManager.getCurrentSecondSubLangId();
        }

        @Override
        public List<String> getCurrentLangNameList() throws RemoteException {
            return mParameterManager.getCurrentLangNameList();
        }

        @Override
        public List<String> getCurrentSecondLangNameList() throws RemoteException {
            return mParameterManager.getCurrentSecondLangNameList();
        }

        @Override
        public void setPrimaryAudioLangByPosition(int position) throws RemoteException {
            mParameterManager.clearUserAudioSelect();
            mParameterManager.setPrimaryAudioLangByPosition(position);
            updatingGuide();
        }

        @Override
        public void setPrimarySubtitleLangByPosition(int position) throws RemoteException {
            mParameterManager.setPrimaryTextLangByPosition(position);
            updatingGuide();
        }

        @Override
        public void setSecondaryAudioLangByPosition(int position) throws RemoteException {
            mParameterManager.clearUserAudioSelect();
            mParameterManager.setSecondaryAudioLangByPosition(position);
            updatingGuide();
        }

        @Override
        public void setSecondarySubtitleLangByPosition(int position) throws RemoteException {
            mParameterManager.setSecondaryTextLangByPosition(position);
            updatingGuide();
        }

        @Override
        public String getStoragePath() throws RemoteException {
            String savedPath = mParameterManager.getStringParameters(ParameterMananer.KEY_PVR_RECORD_PATH);
            if (SysSettingManager.isMediaPath(savedPath)) {
                savedPath = SysSettingManager.convertMediaPathToMountedPath(savedPath);
            }
            if (savedPath != null && savedPath.indexOf(SysSettingManager.PVR_DEFAULT_FOLDER) != -1) {
                savedPath = savedPath.substring(0, savedPath.indexOf(SysSettingManager.PVR_DEFAULT_FOLDER) - 1);
            }
            Log.d(TAG, "getStoragePath = " + savedPath);
            return savedPath;
        }

        @Override
        public void setStoragePath(String path) throws RemoteException {
            Log.d(TAG, "setStoragePath = " + path);
            if (SysSettingManager.isStoragePath(path)) {
                path = SysSettingManager.convertStoragePathToMediaPath(path);
            }
            if (path != null && !path.endsWith(SysSettingManager.PVR_DEFAULT_FOLDER)) {
                path += "/" + SysSettingManager.PVR_DEFAULT_FOLDER;
            }
            if (path != null) {
                mParameterManager.saveStringParameters(ParameterMananer.KEY_PVR_RECORD_PATH, path);
            } else {
                Log.d(TAG, "setStoragePath invalid /storage path");
            }
        }

        @Override
        public boolean getHearingImpairedSwitchStatus() throws RemoteException {
            return mParameterManager.getHearingImpairedSwitchStatus();
        }

        @Override
        public void setHearingImpairedSwitchStatus(boolean on) throws RemoteException {
            mParameterManager.setHearingImpairedSwitchStatus(on);
        }

        @Override
        public String getCustomParameter(String key, String newJsonValue) throws RemoteException {
            return mParameterManager.getCustomParameter(key, newJsonValue);
        }

        @Override
        public void setCustomParameter(String key, String defaultJsonValue) throws RemoteException {
            mParameterManager.setCustomParameter(key, defaultJsonValue);
        }

        @Override
        public void registerListener(IMGRCallbackListener listener) throws RemoteException {
            mListenerList.register(listener);
        }

        @Override
        public void unregisterListener(IMGRCallbackListener listener) throws RemoteException {
            mListenerList.unregister(listener);
        }

        @Override
        public void updateChannelList() throws RemoteException {
            updateChannelListAndCheckLcn(false);
        }

        @Override
        public void syncDatabase() throws RemoteException {
            updatingGuide();
        }

        @Override
        public void setDVBChannelType(String channelType) throws RemoteException {
            setChannelTypeFilter(channelType);
        }

        @Override
        public boolean getHbbTvFeature() {
            return mHbbTvUISetting.getHbbTvFeature();
        }

        @Override
        public void setHbbTvFeature(boolean status) {
            mHbbTvUISetting.setHbbTvFeature(status);
        }

        @Override
        public boolean getHbbTvServiceStatusForCurChannel() {
            return mHbbTvUISetting.getHbbTvServiceStatusForCurChannel();
        }

        @Override
        public void setHbbTvServiceStatusForCurChannel(boolean status) {
            mHbbTvUISetting.setHbbTvServiceStatusForCurChannel(status);
        }

        @Override
        public boolean getHbbTvDistinctiveIdentifierStatus() {
            return mHbbTvUISetting.getHbbTvDistinctiveIdentifierStatus();
        }

        @Override
        public void setHbbTvDistinctiveIdentifierStatus(boolean status) {
            mHbbTvUISetting.setHbbTvDistinctiveIdentifierStatus(status);
        }

        @Override
        public boolean getHbbtvCookiesStatus() {
            return mHbbTvUISetting.getHbbtvCookiesStatus();
        }

        @Override
        public void setHbbTvCookiesStatus(boolean status) {
            mHbbTvUISetting.setHbbTvCookiesStatus(status);
        }

        @Override
        public boolean getHbbTvTrackingStatus() {
            return mHbbTvUISetting.getHbbTvTrackingStatus();
        }

        @Override
        public void setHbbTvTrackingStatus(boolean status) {
            mHbbTvUISetting.setHbbTvTrackingStatus(status);
        }

        @Override
        public void clearHbbTvCookies() {
            mHbbTvUISetting.clearHbbTvCookies();
        }

        @Override
        public void renameRecord(String name, String uri) throws RemoteException {
            mParameterManager.renameRecord(name, uri);
        }

        @Override
        public String request(String resource, String arguments) {
            JSONObject obj = null;
            try {
                JSONArray args = new JSONArray(arguments);
                obj = DtvkitGlueClient.getInstance().request(resource, args);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                return "";
            }
            if (obj != null) {
                return obj.toString();
            } else {
                return "";
            }
        }
    }

    private void updatingGuide() {
        EpgSyncJobService.cancelAllSyncRequests(this);
        String inputId = "com.droidlogic.dtvkit.inputsource/.DtvkitTvInput/HW19";
        EpgSyncJobService.requestImmediateSync(this, inputId, true, new ComponentName(this, DtvkitEpgSync.class));
    }

    private boolean checkAndCreatPvrFolderInStorage(String storagePath) {
        boolean folderReady = false;
        try {
            File file = new File(storagePath);
            if (!file.exists()) {
                folderReady = file.mkdirs();
                Log.d(TAG, "checkAndCreatPvrFolderInStorage folderReady = " + folderReady + ", storagePath = " + storagePath);
            } else {
                folderReady = true;
            }
        } catch (Exception e) {
            folderReady = false;
            Log.d(TAG, "checkAndCreatPvrFolderInStorage Exception = " + e.getMessage());
        }
        return folderReady;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String status = intent.getStringExtra(EpgSyncJobService.SYNC_STATUS);
            if (status.equals(EpgSyncJobService.SYNC_FINISHED)) {
                //mStartSync = false;
                Log.d(TAG,"EpgSyncJobService.SYNC_FINISHED");
                //MGRMessage message = new MGRMessage(SYNC_FINISHED, 0 , "SYNC_FINISHED");
                String message = EPGSYNC_STOPPED;
                sendMGRMessage(SYNC_FINISHED,0,0,message);
            }else if (status.equals(EpgSyncJobService.SYNC_STARTED)) {
                Log.d(TAG,"EpgSyncJobService.SYNC_STARTED");
                String message = EPGSYNC_RUNNING;
                sendMGRMessage(SYNC_RUNNING,0,0,message);
            }
        }
    };

    @Override
    public void onDestroy() {
        stopMonitoringSync();
        DtvkitGlueClient.getInstance().unregisterSignalHandler(mSignalHandler);
        super.onDestroy();
    }

    private void setChannelTypeFilter(String channelType) {
        Log.i(TAG, "setChannelTypeFilter channelType = " + channelType);
        EpgSyncJobService.setChannelTypeFilter(channelType);
    }

    private void startMonitoringSync() {
        //mStartSync = true;
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                new IntentFilter(EpgSyncJobService.ACTION_SYNC_STATUS_CHANGED));
    }

    private void stopMonitoringSync() {
        //mStartSync = false;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    private void updateChannelListAndCheckLcn(boolean needCheckLcn){
        JSONArray mServiceList  = getServiceList();
        int mFoundServiceNumber = getFoundServiceNumber();
        if (mFoundServiceNumber == 0 && mServiceList != null && mServiceList.length() > 0) {
            Log.d(TAG, "mFoundServiceNumber erro use mServiceList length = " + mServiceList.length());
            mFoundServiceNumber = mServiceList.length();
        }
        EpgSyncJobService.cancelAllSyncRequests(this);
        Bundle parameters = new Bundle();
        if (needCheckLcn) {
            parameters.putBoolean(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_LCN_CONFLICT, false);
        }
        EpgSyncJobService.requestImmediateSyncSearchedChannelWitchParameters(this, DTVKIT_INPUTID, (mFoundServiceNumber > 0),
            new ComponentName(this, DtvkitEpgSync.class), parameters);
    }

    private int getFoundServiceNumber() {
        int found = 0;
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.getNumberOfServices", new JSONArray());
            found = obj.getInt("data");
            Log.i(TAG, "getFoundServiceNumber found = " + found);
        } catch (Exception ignore) {
            Log.e(TAG, "getFoundServiceNumber Exception = " + ignore.getMessage());
        }
        return found;
    }

    private JSONArray getServiceList() {
        JSONArray result = null;
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.getListOfServices", new JSONArray());
            JSONArray services = obj.getJSONArray("data");
            result = services;
            Log.i(TAG, "getServiceList services = " + services.length());
            /*for (int i = 0; i < services.length(); i++) {
                JSONObject service = services.getJSONObject(i);
                //Log.i(TAG, "getServiceList service = " + service.toString());
            }*/
        } catch (Exception e) {
            Log.e(TAG, "getServiceList Exception = " + e.getMessage());
        }
        return result;
    }

    private void sendMGRMessage(int what,int arg1,int arg2,Object message) {
        if (mHandler != null) {
            mHandler.removeMessages(what);
            Message mess = mHandler.obtainMessage(what, arg1, arg2, message);
            boolean info = mHandler.sendMessageDelayed(mess, 0);
            Log.d(TAG, "sendMGRMessage info= " + info);
        }
    }

}
