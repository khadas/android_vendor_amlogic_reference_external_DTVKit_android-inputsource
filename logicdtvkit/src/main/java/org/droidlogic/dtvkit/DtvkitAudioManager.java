package org.droidlogic.dtvkit;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;

import org.droidlogic.dtvkit.DtvkitGlueClient;
import org.json.JSONArray;

/**
 * The type Dtvkit audio manager.
 */
public class DtvkitAudioManager {
    private static final String TAG = "DtvkitAudioManager";
    AudioManager audioManager;

    public enum EntryType {
        DIALOGUE_ENHANCEMENT_LEVEL,
    }

    public DtvkitAudioManager(Context context) {
        audioManager = context.getSystemService(AudioManager.class);
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
                        Log.e(TAG, "invalid level:" + level + " switchOn " + type);
                    }
                } else {
                    Log.e(TAG, "params should putString(\"level\", [\"slow\"|\"medium\"|\"high\"]) when switchOn " + type);
                }
                break;
            default:
                Log.e(TAG, "Unsupport switchOn " + type);
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
                break;
            default:
                Log.e(TAG, "Unsupport switchOff " + type);
                break;
        }
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

}
