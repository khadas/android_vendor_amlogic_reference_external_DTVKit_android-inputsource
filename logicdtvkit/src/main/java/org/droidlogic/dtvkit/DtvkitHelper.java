package org.droidlogic.dtvkit;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Dtvkit tool class.
 */
public class DtvkitHelper {
    private static final String TAG = "DtvkitHelper";

    public DtvkitHelper() {
    }

    /**
     * setDtvTimeSource.
     *
     * @Return if DVB is playing, setTime success; DVB is not playing, setTime failed;
     */
    public static boolean setDtvTimeSource() {
        return setDtvTimeSource(0);
    }

    /**
     * setDtvTimeSource.
     *
     * @param mode: different mode to get tv stream time. 0:TDT, 1:TOT
     * @Return if DVB is playing, setTime success; DVB is not playing, setTime failed;
     */
    public static boolean setDtvTimeSource(int mode) {
        boolean ret = playerIsValidStatus();
        if (ret) {
            SystemClock.setCurrentTimeMillis(playerGetTvTime(mode));
        }
        return ret;
    }

    private static boolean playerIsValidStatus() {
        JSONObject response;
        try {
            JSONArray args = new JSONArray();
            args.put(0);
            response = DtvkitGlueClient.getInstance().request("Player.getStatus", args).getJSONObject("data");

            if (response.has("state")) {
                String state = response.getString("state");
                if (TextUtils.equals(state, "playing")
                        || TextUtils.equals(state, "blocked")
                        || TextUtils.equals(state, "scambled")) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "playerGetStatus = " + e.getMessage());
        }
        return false;
    }

    private static long playerGetTvTime(int mode) {
        long time = 0;
        try {
            JSONArray args = new JSONArray();
            args.put(0);
            if (mode == 0) {
                time = DtvkitGlueClient.getInstance()
                        .request("Dvb.GetDTVRealTime", args).getLong("data");
            } else if (mode == 1) {
                time = DtvkitGlueClient.getInstance()
                        .request("Dvb.GetBroadcastTime", args).getLong("data");
            } else {
                time = DtvkitGlueClient.getInstance()
                        .request("Dvb.GetDTVRealUTCTime", args).getLong("data");
            }
        } catch (Exception e) {
            Log.e(TAG, "playerGetTvTime = " + e.getMessage());
        }
        return time;
    }
}
