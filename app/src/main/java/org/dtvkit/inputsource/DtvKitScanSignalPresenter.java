package com.droidlogic.dtvkit.inputsource;

import android.content.Context;
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


import com.droidlogic.fragment.ParameterMananer;

public class DtvKitScanSignalPresenter {
    private static final String TAG = "DtvKitScanSignalPresenter";

    private static final String DVBT_TRY_LOCK_COMMAND = "Dvbt.tunerTryLock";
    private static final String DVBC_TRY_LOCK_COMMAND = "Dvbc.tunerTryLock";
    private static final String DVBS_TRY_LOCK_COMMAND = "Dvbc.tunerTryLock";

    private static final int FREQ_SCAN_MODE = 0;
    private static final int CHANNEL_SCAN_MODE = 1;
    private static final int MSG_CHECK_SIGNAL = 1;
    private static final int MSG_FREQ_TRY_LOCK = 2;
    private static final int MSG_NUMBER_TRY_LOCK = 3;
    private static final int MSG_DVBC_SYMBOL_TRY_LOCK = 4;
    private static final int MSG_DELAY_TIME = 2000;//ms
    private static final int INVALID_CHANNEL_INDEX = 0xFFFFFFFF;
    private static final int INVALID_FREQUENCE = 10 * 1000 * 1000; //10M

    private ParameterMananer mParameterMananer = null;
    private DataMananer mDataMananer = null;
    private HandlerThread mSignalCheckThread = null;
    private Handler mSignalCheckHandler = null;
    private UpdateView mUpdateView = null;
    private int mStrength = 0xFFFFFFFF;
    private int mQuality = 0xFFFFFFFF;
    private boolean mUpdateFlag = false;
    private boolean mIsDVBT = true;

    public DtvKitScanSignalPresenter(ParameterMananer parameterMananer, DataMananer dataMananer, boolean isDVBT){
        mParameterMananer = parameterMananer;
        mDataMananer = dataMananer;
        mIsDVBT = isDVBT;
        initSignalCheckThread();
    }

    public void startMonitorSignal(){
        sendUpdateSignalMessage();
        mUpdateFlag = true;
    }

    public void stopMonitorSignal(){
        if (null != mSignalCheckHandler) {
            mSignalCheckHandler.removeMessages(MSG_CHECK_SIGNAL);
        }
        mUpdateFlag = false;
        Log.d(TAG, "stopMonitorSignal ");
    }

    public void registerUpdateView(UpdateView updateView){
        mUpdateView = updateView;
    }

    public void unregisterUpdateView(){
        mUpdateView = null;
    }

    public void releaseSignalCheckResource(){
        stopMonitorSignal();
        unregisterUpdateView();
        if (null != mSignalCheckHandler) {
            mSignalCheckHandler.removeCallbacksAndMessages(null);
            mSignalCheckHandler = null;
        }
        if (null != mSignalCheckThread) {
            mSignalCheckThread.getLooper().quitSafely();
            mSignalCheckThread = null;
        }
    }

    public void dvbcSymbolTryLock(String symbol, String freq){
        if (null != mSignalCheckHandler) {
            mSignalCheckHandler.removeMessages(MSG_DVBC_SYMBOL_TRY_LOCK);
            Message msg = mSignalCheckHandler.obtainMessage(MSG_DVBC_SYMBOL_TRY_LOCK);
            msg.arg1 = getDvbcSymbol(symbol);
            msg.arg2 = getInputFreq(freq);
            mSignalCheckHandler.sendMessage(msg);
        }
    }

    public void freqTunerTryLock(String freq){
        if (null != mSignalCheckHandler) {
            mSignalCheckHandler.removeMessages(MSG_FREQ_TRY_LOCK);
            Message msg = mSignalCheckHandler.obtainMessage(MSG_FREQ_TRY_LOCK);
            msg.arg1 = getInputFreq(freq);
            mSignalCheckHandler.sendMessage(msg);
        }
    }

    public void channelTunerTryLock(){
        if (null != mSignalCheckHandler) {
            mSignalCheckHandler.removeMessages(MSG_NUMBER_TRY_LOCK);
            Message msg = mSignalCheckHandler.obtainMessage(MSG_NUMBER_TRY_LOCK);
            mSignalCheckHandler.sendMessage(msg);
        }
    }

    private void innerDVBCSymbolTryLock(int symbol, int freq){
        JSONArray args = new JSONArray();
        if (freq < INVALID_FREQUENCE) {
            Log.e(TAG, "dvbSymbolTryLock freq error = " + freq);
            //return;
        }
        args.put(FREQ_SCAN_MODE);
        args.put(freq);
        args.put(DataMananer.VALUE_DVBC_MODE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBC_MODE)]);
        args.put(symbol);
        tunerTryLock(DVBC_TRY_LOCK_COMMAND, args);
    }

    private void innerFreqTunerTryLock(int freq) {
        JSONArray args = new JSONArray();

        if (freq < INVALID_FREQUENCE) {
            Log.e(TAG, "freqTunerTryLock freq error = " + freq);
            //return;
        }
        args.put(FREQ_SCAN_MODE); //scan mode 0 for freq tunerlock
        args.put(freq);
        if (mIsDVBT) {
            args.put(DataMananer.VALUE_DVBT_BANDWIDTH_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBT_BANDWIDTH)]);
            args.put(DataMananer.VALUE_DVBT_MODE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBT_MODE)]);
            args.put(DataMananer.VALUE_DVBT_TYPE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBT_TYPE)]);
            tunerTryLock(DVBT_TRY_LOCK_COMMAND, args);
        } else {
            args.put(DataMananer.VALUE_DVBC_MODE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBC_MODE)]);
            args.put(mDataMananer.getIntParameters(DataMananer.KEY_DVBC_SYMBOL_RATE));
            tunerTryLock(DVBC_TRY_LOCK_COMMAND, args);
        }
    }

    private void innerChannelTunerTryLock(){
        JSONArray args = new JSONArray();
        int channelIndex = getChannelIndex(mIsDVBT);

        if (INVALID_CHANNEL_INDEX == channelIndex) {
            Log.e(TAG, "channelTunerTryLock channelIndex error = " + channelIndex);
            return;
        }
        args.put(CHANNEL_SCAN_MODE); //scan mode 0 for freq tunerlock
        args.put(channelIndex);
        if (mIsDVBT) {
            tunerTryLock(DVBT_TRY_LOCK_COMMAND, args);
        } else {
            tunerTryLock(DVBC_TRY_LOCK_COMMAND, args);
        }
    }

/*
    public void dvbtTunerTryLock(int scanMode, String scanPara){
        JSONArray args = new JSONArray();
        int scanMode = mDataMananer.getIntParameters(DataMananer.KEY_IS_FREQUENCY);
        args.put(scanMode);
        if (DataMananer.VALUE_FREQUENCY_MODE == scanMode) {
            args.put(getInputFreq(scanPara));
            args.put(DataMananer.VALUE_DVBT_BANDWIDTH_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBT_BANDWIDTH)]);
            args.put(DataMananer.VALUE_DVBT_MODE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBT_MODE)]);
            args.put(DataMananer.VALUE_DVBT_TYPE_LIST[mDataMananer.getIntParameters(DataMananer.KEY_DVBT_TYPE)]);
        } else {

        }
        tunerTryLock(DVBT_TRY_LOCK_COMMAND, args);
    }

    public void dvbcTunerTryLock(int scanMode, String scanPara){
        JSONArray args = new JSONArray();
        tunerTryLock(DVBC_TRY_LOCK_COMMAND, args);
    }

    public void dvbsTunerTryLock(int scanMode, int scanPara){
        //TBD add DVBS
    }
*/
    private int getDvbcSymbol(String symbol){
        int dvbcSymbol = 0;
        if (!TextUtils.isEmpty(symbol) && TextUtils.isDigitsOnly(symbol)) {
            try {
                dvbcSymbol = Integer.parseInt(symbol);
            } catch (Exception e) {
            }
            Log.d(TAG, "input dvbcSymbol: " + dvbcSymbol);
        } else {
            dvbcSymbol = mDataMananer.getIntParameters(DataMananer.KEY_DVBC_SYMBOL_RATE);
        }
        Log.d(TAG, "getDvbcSymbol: " + dvbcSymbol);
        return dvbcSymbol;
    }

    private int getInputFreq(String inputFreq){
        int freq = 0;
        if (!TextUtils.isEmpty(inputFreq) && TextUtils.isDigitsOnly(inputFreq)) {
            float toFloat = Float.valueOf(inputFreq);
            freq = (int)(toFloat * 1000000.0f);//hz
        }
        Log.d(TAG, "getInputFreq: " + freq);
        return freq;
    }

    private int getChannelIndex(boolean isDvbT) {
        int index = 0;
        int channelIndex = INVALID_CHANNEL_INDEX;

        if (isDvbT) {
            index = mDataMananer.getIntParameters(DataMananer.KEY_SEARCH_DVBT_CHANNEL_NAME);
        } else {
            index = mDataMananer.getIntParameters(DataMananer.KEY_SEARCH_DVBC_CHANNEL_NAME);
        }
        List<String> list = mParameterMananer.getChannelTable(mParameterMananer.getCurrentCountryCode(), isDvbT, mDataMananer.getIntParameters(DataMananer.KEY_DVBT_TYPE) == 1);

        String channelInfo = (index < list.size()) ? list.get(index) : null;
        if (channelInfo != null) {
            String[] parameter = channelInfo.split(",");//first number, second string, third number
            if (parameter != null && parameter.length == 3 && TextUtils.isDigitsOnly(parameter[0])) {
                channelIndex = Integer.valueOf(parameter[0]);
                Log.d(TAG, "getChannelIndex channel index = " + parameter[0] + ", name = " + parameter[1] + ", freq = " + parameter[2]);
            }
        }
        return channelIndex;
    }

    private void tunerTryLock(String command, JSONArray args){
        JSONObject result = null;
        try {
            Log.d(TAG, "tunerTryLock: command = " + command + " args = " + args.toString());
            result = DtvkitGlueClient.getInstance().request(command, args);
            Log.d(TAG, "tunerTryLock: " + result.toString());
        } catch (Exception e) {
            //TBD
            //need stop check singla
            Log.d(TAG, "tunerTryLock Exception e : " + e);
        }
        //TBD
        //need check result?
    }

    private void initSignalCheckThread(){
        mSignalCheckThread = new HandlerThread("DtvKitScanSignalPresenter");
        mSignalCheckThread.start();
        mSignalCheckHandler = new Handler(mSignalCheckThread.getLooper(), (msg)->{
            Log.d(TAG, "mSignalCheckHandler msg.what : " + msg.what + "|arg1 = " + msg.arg1 + "|arg2 = " + msg.arg2);
            switch (msg.what) {
                case MSG_CHECK_SIGNAL:
                    updateSignalStatus();
                    break;
                case MSG_FREQ_TRY_LOCK:
                    innerFreqTunerTryLock(msg.arg1);
                    break;
                case MSG_NUMBER_TRY_LOCK:
                    innerChannelTunerTryLock();
                    break;
                case MSG_DVBC_SYMBOL_TRY_LOCK:
                    innerDVBCSymbolTryLock(msg.arg1, msg.arg2);
                    break;
                default:
                    break;
            }
            return true;
        });
    }

    private void sendUpdateSignalMessage(){
        if (null != mSignalCheckHandler) {
            mSignalCheckHandler.removeMessages(MSG_CHECK_SIGNAL);
            Message mess = mSignalCheckHandler.obtainMessage(MSG_CHECK_SIGNAL);
            boolean info = mSignalCheckHandler.sendMessageDelayed(mess, MSG_DELAY_TIME);
            Log.d(TAG, "sendUpdateSignalMessage ");
        }
    }

    private void updateSignalStatus(){
        int strength = mParameterMananer.getStrengthStatus();
        int quality = mParameterMananer.getQualityStatus();

        if (mStrength != strength || mQuality != quality) {
            mStrength = strength;
            mQuality = quality;
            if (null != mUpdateView) {
                mUpdateView.updateSignalView(strength, quality);
            } else {
                Log.e(TAG, "please register UpdateView");
            }
        } else {
            Log.d(TAG, "strength and quality not change");
        }

        if (mUpdateFlag) {
            sendUpdateSignalMessage();
        }

        Log.d(TAG, "updateSignalStatus strength = " + strength + "| quality = " + quality + "| mUpdateFlag = " + mUpdateFlag);
    }

    public interface UpdateView{
        void updateSignalView(int strength, int quality);
    }
}