// ILanguageSetting.aidl
package com.droidlogic.dtvkit;

// Declare any non-default types here with import statements
import com.droidlogic.dtvkit.IDtvKitCallbackListener;

interface IDtvkitSetting {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    int getCurrentMainAudioLanguageId();

    int getCurrentMainSubtitleLangId();

    int getCurrentSecondAudioLanguageId();

    int getSecondSubtitleLangId();

    List<String> getCurrentLangNameList();

    List<String> getCurrentSecondLangNameList();

    void setPrimaryAudioLangByPosition(int position);

    void setPrimarySubtitleLangByPosition(int position);

    void setSecondaryAudioLangByPosition(int position);

    void setSecondarySubtitleLangByPosition(int position);

    String getStoragePath();

    void setStoragePath(String path);

    boolean getHearingImpairedSwitchStatus();

    void setHearingImpairedSwitchStatus(boolean on);

    void setCustomParameter(String key, String newJsonValue);

    String getCustomParameter(String key, String defaultJsonValue);

    boolean getHbbTvFeature();

    boolean getHbbTvServiceStatusForCurChannel();

    boolean getHbbTvDistinctiveIdentifierStatus();

    void setHbbTvDistinctiveIdentifierStatus(boolean status);

    boolean getHbbtvCookiesStatus();

    void setHbbTvCookiesStatus(boolean status);

    boolean getHbbTvTrackingStatus();

    void setHbbTvTrackingStatus(boolean status);

    void clearHbbTvCookies();

    void setHbbTvFeature(boolean status);

    void setHbbTvServiceStatusForCurChannel(boolean status);

    void renameRecord(String name, String uri);

    void acquireWakeLock();

    void releaseWakeLock();

    String request(String resource, String arguments);

    void registerListener(IDtvKitCallbackListener listener);

    void unregisterListener(IDtvKitCallbackListener listener);

    void updateChannelList();

    void syncDatabase();

    void setDVBChannelType(String channelType);

    String bookingAction(String action, String data);
}
