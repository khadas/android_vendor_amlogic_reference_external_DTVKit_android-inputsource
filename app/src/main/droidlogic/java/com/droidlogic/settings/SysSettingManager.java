package com.droidlogic.settings;

import android.util.Log;
import android.text.TextUtils;
import android.content.Context;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import com.droidlogic.app.SystemControlManager;
import com.droidlogic.app.FileListManager;

import org.dtvkit.inputsource.R;

public class SysSettingManager {

    private static final String TAG = "SysSettingManager";
    private static final boolean DEBUG = true;

    public static final String PVR_DEFAULT_PATH = "/data/data/org.dtvkit.inputsource";

    protected SystemControlManager mSystemControlManager;
    private FileListManager mFileListManager;
    private Context mContext;

    public SysSettingManager(Context context) {
        mContext = context;
        mSystemControlManager = SystemControlManager.getInstance();
        mFileListManager = new FileListManager(context);
    }

    public String readSysFs(String sys) {
        String result = mSystemControlManager.readSysFs(sys);
        if (DEBUG) {
            Log.d(TAG, "readSysFs sys = " + sys + ", result = " + result);
        }
        return result;
    }

    public boolean writeSysFs(String sys, String val) {
        return mSystemControlManager.writeSysFs(sys, val);
    }

    public String getVideoFormatFromSys() {
        String result = "";
        String height = readSysFs(ConstantManager.SYS_HEIGHT_PATH);
        String pi = readSysFs(ConstantManager.SYS_PI_PATH);
        if (!TextUtils.isEmpty(height) && !"NA".equals(height) && !TextUtils.isEmpty(pi) && !"null".equals(pi) && !"NA".equals(pi)) {
            if (pi.startsWith(ConstantManager.CONSTANT_FORMAT_INTERLACE)) {
                result = height + ConstantManager.PI_TO_VIDEO_FORMAT_MAP.get(ConstantManager.CONSTANT_FORMAT_INTERLACE);
            } else if (pi.startsWith(ConstantManager.CONSTANT_FORMAT_PROGRESSIVE)) {
                result = height + ConstantManager.PI_TO_VIDEO_FORMAT_MAP.get(ConstantManager.CONSTANT_FORMAT_PROGRESSIVE);
            } else if (pi.startsWith(ConstantManager.CONSTANT_FORMAT_COMRPESSED)) {//Compressed may exist with progressive or interlace
                result = height + ConstantManager.PI_TO_VIDEO_FORMAT_MAP.get(ConstantManager.CONSTANT_FORMAT_PROGRESSIVE);
            } else {
                result = height + ConstantManager.PI_TO_VIDEO_FORMAT_MAP.get(ConstantManager.CONSTANT_FORMAT_PROGRESSIVE);
            }
        } else if ("NA".equals(height) && "NA".equals(pi)) {
            result = "";
        } else {
            result = height + ConstantManager.PI_TO_VIDEO_FORMAT_MAP.get(ConstantManager.CONSTANT_FORMAT_PROGRESSIVE);
        }
        if (DEBUG) {
            Log.d(TAG, "getVideoFormatFromSys result = " + result);
        }
        return result;
    }

    public List<String> getStorageDeviceNameList() {
        List<String> result = new ArrayList<String>();
        List<Map<String, Object>> mapList = getStorageDevices();
        String name = "";
        if (mapList != null && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                name = getStorageName(map);
                result.add(name);
            }
        }
        return result;
    }

    public List<String> getStorageDevicePathList() {
        List<String> result = new ArrayList<String>();
        List<Map<String, Object>> mapList = getStorageDevices();
        String name = "";
        if (mapList != null && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                name = getStoragePath(map);
                result.add(name);
            }
        }
        return result;
    }

    private List<Map<String, Object>> getStorageDevices() {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(FileListManager.KEY_NAME, mContext.getString(R.string.strSettingsPvrDefault));
        map.put(FileListManager.KEY_PATH, PVR_DEFAULT_PATH);
        result.add(map);
        result.addAll(mFileListManager.getDevices());
        return result;
    }

    public String getStorageName(Map<String, Object> map) {
        String result = null;
        if (map != null) {
            result = (String) map.get(FileListManager.KEY_NAME);
        }
        return result;
    }

    public String getStoragePath(Map<String, Object> map) {
        String result = null;
        if (map != null) {
            result = (String) map.get(FileListManager.KEY_PATH);
        }
        return result;
    }

    public String getAppDefaultPath() {
        return PVR_DEFAULT_PATH;
    }
}
