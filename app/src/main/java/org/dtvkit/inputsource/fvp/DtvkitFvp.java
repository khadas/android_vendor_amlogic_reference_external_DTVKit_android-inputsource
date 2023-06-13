package com.droidlogic.dtvkit.inputsource.fvp;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.droidlogic.dtvkit.DtvkitGlueClient;

public class DtvkitFvp {
    private static final String TAG = "DtvkitFvp";
    private static DtvkitFvp mSingleton = null;

    public synchronized static DtvkitFvp getInstance() {
        if (mSingleton == null) {
            mSingleton = new DtvkitFvp();
        }
        return mSingleton;
    }

    private DtvkitFvp(){}

    public boolean checkFvpNeedIpScan() {
        boolean isNeedIpScan = false;
        try {
            isNeedIpScan = DtvkitGlueClient.getInstance().request("Fvp.FVPNeedIPScan", new JSONArray()).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "checkFvpNeedIpScan = " + e.getMessage());
        }
        return isNeedIpScan;
    }

    public boolean fvpStartScan(int scanMode) {
        boolean result = false;
        Log.d(TAG, "fvpStartScan: " + scanMode);
        try {
            JSONArray args = new JSONArray();
            args.put(scanMode);
            result = DtvkitGlueClient.getInstance().request("Fvp.FVPStartIPScan", args).getBoolean("data");
            if (!result) {
                Log.d(TAG, "fvpStartScan not ok");
            }
        } catch (Exception e) {
            Log.e(TAG, "fvpStartScan Exception = " + e.getMessage());
        }
        return result;
    }

    public boolean checkClmAdviseInteractiveScanStatus() {
        boolean status = false;
        try {
            status = DtvkitGlueClient.getInstance().request("Fvp.FVPCheckClmAdviseInteractiveScanStatus", new JSONArray()).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "checkClmAdviseInteractiveScanStatus = " + e.getMessage());
        }
        return status;
    }

    public boolean setClmAdviseInteractiveScanStatus(int scanStatus) {
        boolean result = false;
        Log.d(TAG, "setClmAdviseInteractiveScanStatus: " + scanStatus);
        try {
            JSONArray args = new JSONArray();
            args.put(scanStatus);
            result = DtvkitGlueClient.getInstance().request("Fvp.FVPSetClmAdviseInteractiveScanStatus", args).getBoolean("data");
            if (!result) {
                Log.d(TAG, "setClmAdviseInteractiveScanStatus not ok");
            }
        } catch (Exception e) {
            Log.e(TAG, "setClmAdviseInteractiveScanStatus Exception = " + e.getMessage());
        }
        return result;
    }

    public JSONArray getClmIpServicesInfo() {
        JSONArray serviceInfoArray = null;
        try {
            JSONObject resp = DtvkitGlueClient.getInstance().request("Fvp.FvpGetClmIpServicesInfo", new JSONArray());
            if (null != resp) {
                if (resp.getBoolean("accepted")) {
                    serviceInfoArray = resp.getJSONArray("data");
                } else {
                    String error = resp.getString("data");
                    Log.d(TAG, "getClmIpServicesInfo fail:" + error);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "FvpGetClmIpServicesInfo Exception = " + e.getMessage());
        }
        return serviceInfoArray;
    }

    public boolean setClmUserSelectionRespondInfo(int selectOrder) {
        boolean result = false;
        Log.d(TAG, "setClmUserSelectionRespondInfo: " + selectOrder);
        try {
            JSONArray args = new JSONArray();
            args.put(selectOrder);
            result = DtvkitGlueClient.getInstance().request("Fvp.FvpSetClmUserSelectionRespondInfo", args).getBoolean("data");
            if (!result) {
                Log.d(TAG, "setClmUserSelectionRespondInfo not ok");
            }
        } catch (Exception e) {
            Log.e(TAG, "setClmUserSelectionRespondInfo Exception = " + e.getMessage());
        }
        return result;
    }

    public boolean setClmRegionIdRespondInfo(String postCode) {
        boolean result = false;
        Log.d(TAG, "setClmRegionIdRespondInfo: " + postCode);
        try {
            JSONArray args = new JSONArray();
            args.put(postCode);
            result = DtvkitGlueClient.getInstance().request("Fvp.FvpSetClmRegionIdRespondInfo", args).getBoolean("data");
            if (!result) {
                Log.d(TAG, "setClmRegionIdRespondInfo not ok");
            }
        } catch (Exception e) {
            Log.e(TAG, "setClmRegionIdRespondInfo Exception = " + e.getMessage());
        }
        return result;
    }

    public JSONArray getChannelEnhancedInfo () {
        JSONArray channelEnhancedArray = null;
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Fvp.FvpGetServiceInfo", new JSONArray());
            channelEnhancedArray = obj.getJSONArray("data");
            Log.d(TAG, "Fvp service list number =" + channelEnhancedArray.length());
        } catch (Exception e) {
            Log.e(TAG, "getChannels Exception = " + e.getMessage());
        }
        return channelEnhancedArray;
    }

    public JSONArray getIpChannelEnhancedInfo () {
        JSONArray ipChannelEnhancedArray = null;
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Fvp.FvpGetLinearIPServiceInfo", new JSONArray());
            ipChannelEnhancedArray = obj.getJSONArray("data");
            Log.d(TAG, "Fvp IP service list number =" + ipChannelEnhancedArray.length());
        } catch (Exception e) {
            Log.e(TAG, "getChannels Exception = " + e.getMessage());
        }
        return ipChannelEnhancedArray;
    }
}
