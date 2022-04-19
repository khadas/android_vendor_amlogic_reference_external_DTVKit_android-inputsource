/* Copyright (C) 2021 Vewd Software AS.  All rights reserved.
 *
 * This file is part of the Vewd Core.
 * It includes Vewd proprietary information and distribution is prohibited
 * without Vewd's prior, explicit and written consent.
 */

package com.amlogic.hbbtv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.vewd.core.shared.MediaComponentsPreferences;
import org.droidlogic.dtvkit.DtvkitGlueClient;


public class HbbtvPreferencesManager {

    private static final String TAG = "PreferencesManager";
    private static final String PREF_DEFAULT_LANGUAGE = "GBR";
    private static final String PREF_DEFAULT_COUNTRY  = "GBR";
    private static final int INDEX_FOR_MAIN = 0;
    private boolean mSwitchSubtitleByHbbtv = false;
    private boolean mSubtitleEnable = true;
    private static final String ACTION_SET_PREF = "com.vewd.core.service.SET_PREF";
    private Context mContext;
    public HbbtvPreferencesManager(Context context, AmlHbbTvView amlHbbTvView) {
        Log.d(TAG,"Init PreferencesManager");
        mContext = context;
        mAmlHbbTvView = amlHbbTvView;
   }

    private boolean getSubtitlesEnabled() {
        Log.d(TAG,"getSubtitlesEnabled Status");
        boolean result = true;
        try {
            JSONArray args = new JSONArray();
            result = DtvkitGlueClient.getInstance().request("Player.getSubtitleGlobalOnOff", args).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "getSubtitlesEnabled-getSubtitleGlobalOnOff = " + e.getMessage());
            return false;
        }
        Log.d(TAG,"getSubtitlesEnabled-getSubtitleGlobalOnOff:" + result);
        if (result) {
            try {
                JSONArray args = new JSONArray();
                result = DtvkitGlueClient.getInstance().request("Player.getSubtitlesOn", args).getBoolean("data");
            } catch (Exception e) {
                Log.e(TAG, "getSubtitlesEnabled-getSubtitlesOn = " + e.getMessage());
                return false;
            }
        }
        Log.d(TAG,"getSubtitlesEnabled :" + result);
        return result;
    }

    private boolean getAudioDescriptionsEnabled() {
        Log.d(TAG,"getAudioDescriptionsEnabled Status");
        boolean result = false;
        try {
            JSONArray args = new JSONArray();
            args.put(INDEX_FOR_MAIN);
            result = DtvkitGlueClient.getInstance().request("Player.getAudioDescriptionOn", args).getBoolean("data");
        } catch (Exception e) {
            Log.e(TAG, "getAudioDescriptionsEnabled = " + e.getMessage());
            return false;
        }

        Log.d(TAG,"getAudioDescriptionsEnabled :" + result);
        return result;
    }

    private ArrayList<String> getSubtitlesLanguages() {
        String primarySubtitlesLanguage = null;
        String secondarySubtitlesLanguage = null;
        JSONObject obj = null;


        try {
            JSONArray args = new JSONArray();
            obj = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetPrefSubtLang", args);
            if (obj != null && obj.getBoolean("accepted")) {
                obj = (JSONObject)obj.get("data");
                primarySubtitlesLanguage = obj.getString("primary_sub_lang");
                secondarySubtitlesLanguage = obj.getString("secondary_sub_lang");
            }
            else {
                primarySubtitlesLanguage = PREF_DEFAULT_LANGUAGE;
                secondarySubtitlesLanguage = PREF_DEFAULT_LANGUAGE;
            }
        }
        catch (Exception e) {
            Log.d(TAG, "getSubtitlesLanguages Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            return null;
        }

        Log.d(TAG,"primarySubtitlesLanguage :" + primarySubtitlesLanguage + ", secondarySubtitlesLanguage :" + secondarySubtitlesLanguage);

        ArrayList<String> list = new ArrayList<>();
        if (primarySubtitlesLanguage != null) {
            list.add(primarySubtitlesLanguage);
        }
        if (secondarySubtitlesLanguage != null) {
            list.add(secondarySubtitlesLanguage);
        }
        return list;
    }

    private ArrayList<String> getAudioLanguages() {
        String primaryAudioLanguage = null;
        String secondaryAudioLanguage = null;
        JSONObject obj = null;

        try {
            JSONArray args = new JSONArray();
            obj = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetPrefAudioLang", args);
            if (obj != null && obj.getBoolean("accepted")) {
                obj = (JSONObject)obj.get("data");
                primaryAudioLanguage = obj.getString("primary_audio_lang");
                secondaryAudioLanguage = obj.getString("secondary_audio_lang");
            }
            else {
                primaryAudioLanguage = PREF_DEFAULT_LANGUAGE;
                secondaryAudioLanguage = PREF_DEFAULT_LANGUAGE;
            }
        }
        catch (Exception e) {
            Log.d(TAG, "getAudioLanguages Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            return null;
        }

        Log.d(TAG,"primaryAudioLanguage :" + primaryAudioLanguage + ", secondaryAudioLanguage :" + secondaryAudioLanguage);

        ArrayList<String> list = new ArrayList<>();
        if (primaryAudioLanguage != null) {
            list.add(primaryAudioLanguage);
        }
        if (secondaryAudioLanguage != null) {
            list.add(secondaryAudioLanguage);
        }
        return list;
    }

    public void updateHbbTvMediaComponentsPreferences() {
        boolean enableNormalAudio = true;
        boolean timeShiftSynchronized = true;

        ArrayList<String> preferredAudioLanguages;
        ArrayList<String> preferredSubtitlesLanguages;
        boolean enableAudioDescriptions;
        boolean enableSubtitles;

        preferredAudioLanguages = getAudioLanguages();
        preferredSubtitlesLanguages = getSubtitlesLanguages();
        enableAudioDescriptions = getAudioDescriptionsEnabled();
        enableSubtitles = getSubtitlesEnabled();
        if (mSwitchSubtitleByHbbtv && mSubtitleEnable && !enableSubtitles) {
            enableSubtitles = mSubtitleEnable;
            Log.d(TAG,"updateHbbTvMediaComponentsPreferences enableSubtitles: " + enableSubtitles);
        }

        MediaComponentsPreferences prefs = buildMediaComponentsPreferences(
                preferredSubtitlesLanguages, preferredAudioLanguages, enableSubtitles,
                enableNormalAudio, enableAudioDescriptions, timeShiftSynchronized);
        setMediaComponentsPreferences(prefs);
    }

    public void setSubtitleSwichFlagByHbbtv(boolean switchFlag) {
        Log.d(TAG,"setSubtitleSwichFlagByHbbtv in");
        boolean oriflag = mSwitchSubtitleByHbbtv;
        mSwitchSubtitleByHbbtv = switchFlag;
        mSubtitleEnable = getSubtitlesEnabled();
        if (oriflag && !switchFlag) {
            updateHbbTvMediaComponentsPreferences();
        }
        Log.d(TAG,"setSubtitleSwichFlagByHbbtv switchFlag:" + switchFlag + ", mSubtitleEnable:" + mSubtitleEnable);
        Log.d(TAG,"setSubtitleSwichFlagByHbbtv out");
    }


    public void setMediaComponentsPreferences(MediaComponentsPreferences preferences) {
        if (preferences.preferredAudioLanguages != null &&
           preferences.preferredSubtitlesLanguages != null ) {
            Log.d(TAG, "AudioLanguages = " + preferences.preferredAudioLanguages.get(0));
            Log.d(TAG, "SubtitlesLanguages = " + preferences.preferredSubtitlesLanguages.get(0));
            Log.d(TAG, "enableSubtitles = " + preferences.enableSubtitles);
            mAmlHbbTvView.setMediaComponentsPreferences(preferences);
        }
    }

    private MediaComponentsPreferences buildMediaComponentsPreferences(
            ArrayList<String> preferredSubtitlesLanguages,
            ArrayList<String> preferredAudioLanguages, boolean enableSubtitles,
            boolean enableNormalAudio, boolean enableAudioDescriptions,
            boolean timeShiftSynchronized) {
        MediaComponentsPreferences prefs = new MediaComponentsPreferences();
        prefs.preferredSubtitlesLanguages = preferredSubtitlesLanguages;
        prefs.enableSubtitles = enableSubtitles;
        prefs.preferredAudioLanguages = preferredAudioLanguages;
        prefs.timeShiftSynchronized = timeShiftSynchronized;

        if (enableNormalAudio && enableAudioDescriptions) {
            prefs.audioSelection =
                    MediaComponentsPreferences.AudioSelection.NORMAL_AUDIO_AND_AUDIO_DESCRIPTIONS;
        } else if (enableNormalAudio) {
            prefs.audioSelection = MediaComponentsPreferences.AudioSelection.NORMAL_AUDIO;
        } else {
            prefs.audioSelection = MediaComponentsPreferences.AudioSelection.AUDIO_DESCRIPTIONS;
        }

        return prefs;
    }

    public void setXhrOriginCheckEnabled(boolean enableXhr) {
        Log.d(TAG, "xhr_origin_check_enabled = " + enableXhr);
        mAmlHbbTvView.setPref("xhr_origin_check_enabled", "false");
    }

    public void setDeviceUniqueNumber(String uniqueNumber) {
        Log.d(TAG, "device_unique_number = " + uniqueNumber);
        mAmlHbbTvView.setPref("device_unique_number", "123456");
    }

    public void setManufacturerSecretNumber(String secretNumber) {
        Log.d(TAG, "manufacturer_secret_number = " + secretNumber);
        mAmlHbbTvView.setPref("manufacturer_secret_number", "T950D4");
    }


    public void setConfigurationCountryid() {
        String countryCode = null;
        JSONObject obj = null;

        try {
            JSONArray args = new JSONArray();
            obj = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetGetCurCountryCode", args);
            if (obj != null && obj.getBoolean("accepted")) {
                obj = (JSONObject)obj.get("data");
                countryCode = obj.getString("countryCode");
            }
            else {
                countryCode = PREF_DEFAULT_LANGUAGE;
            }
        }
        catch (Exception e) {
            Log.d(TAG, "Exception " + e.getMessage() + ", trace=" + e.getStackTrace());
            return;
        }

        countryCode = countryCode.toUpperCase();
        Log.d(TAG,"countryCode :" + countryCode);
        mAmlHbbTvView.setPref("ooif.configuration.country_id", countryCode);
        return;
    }

    private static void setIntentTarget(Context context, Intent intent) {
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        String browserServicePackage = applicationInfo.metaData.getString(
                "com.vewd.core.browser_service_package");
        intent.setClassName(browserServicePackage,
                browserServicePackage + ".GlobalParamsReceiverForTesting");
    }

    public void setPref(String name, String value) {
        Intent intent = new Intent(ACTION_SET_PREF);
        intent.putExtra("name", name);
        intent.putExtra("value", value);
        setIntentTarget(mContext, intent);
        Log.d(TAG, "Send " + intent.getAction());
        mContext.sendBroadcast(intent);
    }

    public void setVendor_name(String vendor_name) {
        Log.d(TAG, "ooif.configuration.vendor_name = " + vendor_name);
        setPref("ooif.configuration.vendor_name", vendor_name);
    }

    public void setModel_name(String model_name) {
        Log.d(TAG, "ooif.configuration.model_name = " + model_name);
        setPref("ooif.configuration.model_name", model_name);
    }

    public void setSoftware_version(String software_version) {
        Log.d(TAG, "ooif.configuration.software_version = " + software_version);
        setPref("ooif.configuration.software_version", software_version);
    }

    public void setHardware_version(String hardware_version) {
        Log.d(TAG, "ooif.configuration.hardware_version = " + hardware_version);
        setPref("ooif.configuration.hardware_version", hardware_version);
    }

    public void setFamily_name(String family_name) {
        Log.d(TAG, "ooif.configuration.family_name = " + family_name);
        setPref("ooif.configuration.family_name", family_name);
    }

    public void setUser_agent(String user_agent) {
        Log.d(TAG, "user_agent = " + user_agent);
        mAmlHbbTvView.setPref("user_agent", user_agent);
    }

    private AmlHbbTvView mAmlHbbTvView;

}
