package org.droidlogic.dtvkit;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;

/**
 * The type Dtvkit audio manager.
 */
public class DtvkitAudioManager {
    private static final String TAG = "DtvkitAudioManager";
    private Context mContext;
    private final String PREFS_NAME = "dtvkit_audio";
    AudioManager audioManager;
    public enum EntryType {
        DIALOGUE_ENHANCEMENT_LEVEL,
    }

    public DtvkitAudioManager(Context context) {
        audioManager = context.getSystemService(AudioManager.class);
        mContext = context;
    }

    /**
     * Switch on.
     *
     * @param type   the special type to switch on
     * @param params the params when switch on
     */
    public void switchOn(EntryType type, Bundle params) {
        switch (type) {
            case DIALOGUE_ENHANCEMENT_LEVEL:
                if (params != null) {
                    String level = params.getString("level", "null");
                    if (("high").equals(level)) {
                        audioManager.setParameters("ms12_runtime=-dap_dialogue_enhancer 1,12");
                        audioManager.setParameters("ms12_runtime=-ac4_de 12");
                    } else if (("medium").equals(level)) {
                        audioManager.setParameters("ms12_runtime=-dap_dialogue_enhancer 1,8");
                        audioManager.setParameters("ms12_runtime=-ac4_de 8");
                    } else if (("slow").equals(level)) {
                        audioManager.setParameters("ms12_runtime=-dap_dialogue_enhancer 1,4");
                        audioManager.setParameters("ms12_runtime=-ac4_de 4");
                    } else {
                        throw new IllegalArgumentException("level in params is invalid:" + level);
                    }
                    setAudioInfo(type.toString(), level);
                } else {
                    throw new IllegalArgumentException("params is null");
                }
                break;
            default:
                Log.e(TAG, "Unsupported switchOn " + type);
                break;
        }
    }

    /**
     * Switch off.
     *
     * @param type the special type to switch off
     */
    public void switchOff(EntryType type) {
        switch (type) {
            case DIALOGUE_ENHANCEMENT_LEVEL:
                audioManager.setParameters("ms12_runtime=-dap_dialogue_enhancer 0,0");
                audioManager.setParameters("ms12_runtime=-dap_dialogue_enhancer 1,0");
                audioManager.setParameters("ms12_runtime=-ac4_de 0");
                setAudioInfo(type.toString(), null);
                break;
            default:
                Log.e(TAG, "Unsupported switchOff " + type);
                break;
        }
    }

    /**
     * getValue of the spec type.
     *
     * @param type the special type to get
     */
    public Bundle getValue(EntryType type) {
        Bundle b = new Bundle();
        switch (type) {
            case DIALOGUE_ENHANCEMENT_LEVEL:
                String _switch = getAudioInfo(type.toString());
                if (_switch == null) {
                    b.putString("switch", "off");
                } else {
                    if (TextUtils.equals("off", _switch)) {
                        b.putString("switch", "off");
                    } else {
                        b.putString("switch", "on");
                        b.putString("level", _switch);
                    }
                }
                return b;
            default:
                Log.e(TAG, "Unsupported switchOff " + type);
                break;
        }
        return null;
    }

    /**
     * setMute.
     *
     * @param tvMute set mute to TV Stream
     */
    public void setMute(boolean tvMute) {
        try {
            JSONArray args = new JSONArray();
            args.put(0);
            args.put(tvMute);
            DtvkitGlueClient.getInstance().request("Player.setMute", args);
        } catch (Exception e) {
            Log.e(TAG, "SetMute failed, error: " + e.getMessage());
        }
    }

    /**
     * setMute.
     *
     * @param mmMute set mute to Music Stream
     * @param tvMute set mute to TV Stream
     */
    public void setMute(boolean mmMute, boolean tvMute) {
        audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                mmMute ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE,
                0);
        try {
            JSONArray args = new JSONArray();
            args.put(0);
            args.put(tvMute);
            DtvkitGlueClient.getInstance().request("Player.setMute", args);
        } catch (Exception e) {
            Log.e(TAG, "SetMute failed, error: " + e.getMessage());
        }
    }

    private String getAudioInfo(String key){
        SharedPreferences sp = mContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sp.getString(key, null);
    }

    private void setAudioInfo(String key, String value){
        SharedPreferences sp = mContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        if (value == null) {
            editor.remove(key);
        } else {
            editor.putString(key, value);
        }
        editor.apply();
    }
}
