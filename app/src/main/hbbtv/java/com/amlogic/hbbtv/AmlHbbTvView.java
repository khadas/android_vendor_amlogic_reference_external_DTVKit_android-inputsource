package com.amlogic.hbbtv;

import android.util.Log;
import android.content.Context;
import android.view.KeyEvent;
import android.widget.Toast;

import com.amlogic.hbbtv.utils.StringUtils;
import com.amlogic.hbbtv.utils.KeyEventUtils;
import com.vewd.core.sdk.HbbTvView;
import com.vewd.core.sdk.HbbTvClient;
import com.vewd.core.sdk.CookieManager;
import com.vewd.core.sdk.TunerDelegate;
import com.vewd.core.shared.ContextHandle;
import com.vewd.core.shared.KeyDescription;
import com.vewd.core.shared.HandsetCapabilities;
import com.vewd.core.sdk.HbbTvApplicationInfo;
import com.droidlogic.dtvkit.inputsource.R;



import java.util.Arrays;

/**
 * @ingroup hbbtvclientapi
 * @defgroup amlhbbtvviewapi Aml-HbbTv-View-API
 */
public class AmlHbbTvView extends HbbTvView {
    private static final String TAG = "AmlHbbTvView";
    private static final int KEY_ACTION= KeyEvent.ACTION_DOWN;
    private static final boolean DEBUG = true;
    private int mkeySet = 0;
    private Context mContext = null;

    /**
     * @ingroup amlhbbtvviewapi
     * @brief constructor method of AmlHbbTvView
     * @param context  The Context the view is running in.
     * @return AmlHbbTvView  The instance of AmlHbbTvView
     */
    public AmlHbbTvView(Context context) {
        super(context);
        mContext = context;
        Log.i(TAG, "AmlHbbTvView instance create start");
        Log.i(TAG, "AmlHbbTvView instance create end");
    }

     /**
     * @ingroup amlhbbtvviewapi
     * @brief  set the application keySet
     * @param keySet The keySet indicates what key the hbbtv view consume
     */
    public void setKeySet(int keySet) {
        Log.i(TAG, "setKeySet start");
        if (DEBUG) {
            Log.d(TAG, "setKeySet  the keySet = " + keySet);
        }

        mkeySet = keySet;
        Log.i(TAG, "setKeySet start");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Loads an AIT application. The application has to be present in activated AIT.
     * @param appId - Identifier of the AIT application.
     * @param orgId - Organization identifier of the AIT application.
     */
    @Override
    public void loadAitApplication(int appId, int orgId) {
        Log.i(TAG, "loadAitApplication start");
        if (DEBUG) {
            Log.d(TAG,"loadAitApplication: appid = " + appId + ",the orgId = " + orgId);
        }

        super.loadAitApplication(appId,orgId);
        Log.i(TAG, "loadAitApplication end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Loads a URL application
     * @param appUrl - URL pointing to the application on the network.
     */
    @Override
    public void loadUrlApplication(String appUrl) {
        Log.i(TAG, "loadUrlApplication start");
        if (DEBUG) {
            Log.d(TAG,"loadUrlApplication: appUrl = " + StringUtils.truncateUrlForLogging(appUrl));
        }

        super.loadUrlApplication(appUrl);
        Log.i(TAG, "loadUrlApplication end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Loads a URL application
     * @param appUrl - URL pointing to the application on the network.
     * @param allowAutoTransit The instruction to the application loader to allow or deny a broadcast-independent application to be auto transited
     *        to broadcast-related if all requirements are met.
     */
    @Override
    public void loadUrlApplication(String appUrl,boolean allowAutoTransit) {
        Log.i(TAG, "loadUrlApplication start");
        if (DEBUG) {
            Log.d(TAG,"loadUrlApplication: appUrl = " + StringUtils.truncateUrlForLogging(appUrl)
                + ",allowAutoTransit = " + allowAutoTransit);
        }

        super.loadUrlApplication(appUrl,allowAutoTransit);
        Log.i(TAG, "loadUrlApplication end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Sends new AIT table data to parse.
     * @param originalNetworkId  ID of network of a service carrying the AIT.
     * @param transportStreamId  ID of transport stream of a service carrying the AIT.
     * @param serviceId  ID of service carrying the AIT.
     * @param ccId  Unique identifier of the channel whose AIT is being parsed. It must match the CCID returned in UVAChannel.
     * @param aitData  AIT table data.
     */
    @Override
    public void parseAit(int orgId,int tsId, int sId, String ccId, byte[] aitData) {
        Log.i(TAG, "parseAit start");
        if (DEBUG) {
            Log.d(TAG,"parseAit: orgId = " + orgId + ", tsId = " + tsId + ", sId = "
                + sId + ", ccId = " + ccId + "aitDate = " + Arrays.toString(aitData));
        }

        super.parseAit(orgId,tsId,sId,ccId,aitData);
        Log.i(TAG, "parseAit end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Notifies about channel change operation. After such notification multiple operation like HbbTV application transition
     *         will be triggered.
     * @param originalNetworkId  Original network ID of the selected channel.
     * @param transportStreamId  Transport stream ID of the selected channel.
     * @param serviceId   Service ID of the selected channel.
     * @param ccId  Unique identifier of the channel. Integration needs to use same identifier for channels passed through UVA API.
     */
    @Override
    public void channelChanged​(int originalNetworkId, int transportStreamId, int serviceId, String ccid) {
        Log.i(TAG, "channelChanged​ start");
        if (DEBUG) {
            Log.d(TAG,"channelChange originalNetworkId = " + originalNetworkId + ", transportStreamId = " + transportStreamId
                + ", serviceId = " + serviceId + ", ccId = " + ccid);
        }

        super.channelChanged​(originalNetworkId,transportStreamId,serviceId,ccid);
        Log.i(TAG, "channelChanged​ end");
    }


     /**
      * @ingroup amlhbbtvviewapi
      * @brief  the hbbtv view whether consume the key
      * @param event  The Key Event.
      * @return  true if consume this key,otherwise false
      */
    private int determineKeyGroup(int keyCode) {
        Log.i(TAG,"determineKeyGroup start");
        if (DEBUG) {
            Log.d(TAG, "determineKeyGroup keyCode =  " + keyCode);
        }

        if (keyCode == KeyEvent.KEYCODE_PROG_RED) {
            Log.i(TAG,"determineKeyGroup end");
            return 1;
        }

        if (keyCode == KeyEvent.KEYCODE_PROG_GREEN) {
            Log.i(TAG,"determineKeyGroup end");
            return 2;
        }

        if (keyCode == KeyEvent.KEYCODE_PROG_YELLOW) {
            Log.i(TAG,"determineKeyGroup end");
            return 4;
        }

        if (keyCode == KeyEvent.KEYCODE_PROG_BLUE) {
            Log.i(TAG,"determineKeyGroup end");
            return 8;
        }

        if ((keyCode >= KeyEvent.KEYCODE_DPAD_UP && keyCode <= KeyEvent.KEYCODE_DPAD_CENTER)
            || keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_ESCAPE
            || keyCode == KeyEvent.KEYCODE_BACK) {
            Log.i(TAG,"determineKeyGroup end");
            return 16;
        }

        if ((keyCode >= KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE && keyCode <= KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
            || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            Log.i(TAG,"determineKeyGroup end");
            return 32;
        }

        if (keyCode == KeyEvent.KEYCODE_PAGE_UP || keyCode == KeyEvent.KEYCODE_PAGE_DOWN) {
            Log.i(TAG,"determineKeyGroup end");
            return 64;
        }

        if (keyCode == KeyEvent.KEYCODE_INFO) {
            Log.i(TAG,"determineKeyGroup end");
            return 128;
        }

        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            Log.i(TAG,"determineKeyGroup end");
            return 256;
        }

        if (keyCode >= KeyEvent.KEYCODE_A && keyCode <= KeyEvent.KEYCODE_Z) {
            Log.i(TAG,"determineKeyGroup end");
            return 512;
        }
        Log.i(TAG,"determineKeyGroup end");
        return 0;
  }

   /**
    * @ingroup amlhbbtvviewapi
    * @brief  the hbbtv view whether consume the key
    * @param event  The Key Event.
    * @return  true if consume this key,otherwise false
    */

   private boolean shouldConsumeKey(int keyCode) {
        Log.i(TAG,"hbbtv shouldConsumeKey start");
        if (DEBUG) {
            Log.d(TAG,"hbbtv shouldConsumeKey keyCode = " + keyCode);
        }

        int keyGroup = determineKeyGroup(keyCode);
        if (DEBUG) {
            Log.d(TAG,"the keyGroup = " + keyGroup +", the keySet = " + mkeySet);
        }

        if (keyGroup == 0 && mkeySet == 1024) {
            Log.i(TAG,"hbbtv shouldConsumeKey end");
            return true;
        }
        if ((keyGroup & mkeySet) != 0) {
            Log.i(TAG,"hbbtv shouldConsumeKey end");
            return true;
        }
        Log.i(TAG,"hbbtv shouldConsumeKey end");
        return false;
  }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Dispatch Key Event
     * @param event  The Key Event.
     * @return  true if handle this key,otherwise false
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        Log.i(TAG, "dispatchKeyEvent start");
        if (DEBUG) {
            Log.d(TAG,"dispatchKeyEvent  KeyEvent code = " + keyEvent.getKeyCode());
        }
        KeyEvent event  = KeyEventUtils.remapKeyEvent(keyEvent);
        int keyCode = event.getKeyCode();
        if (isApplicationRunning() && shouldConsumeKey(keyCode)) {
            if (KeyEventUtils.isExitKey(event) && (event.getAction() == KeyEvent.ACTION_UP)) {
                terminateApplicationAndLaunchAutostart();
                Log.i(TAG, "dispatchKeyEvent end");
                return true;
            } else {
                Log.i(TAG, "dispatchKeyEvent to super");
                return super.dispatchKeyEvent(event);
            }
        } else {
            if (DEBUG) {
                Log.d(TAG,"dispatchKeyEvent : the application has been not stared");
            }
            if (keyCode == KeyEvent.KEYCODE_MEDIA_RECORD) {
                return isConsumeRecordKey();
            }
            Log.i(TAG, "dispatchKeyEvent end");
            return false;
        }

   }

    private boolean isConsumeRecordKey() {
        Log.d(TAG,"isConsumeRecordKey start");
        HbbTvManager hbbTvManager = HbbTvManager.getInstance();
        boolean isHandleRecordKey = false;
        boolean isBroadcastOwnResource = hbbTvManager.checkIsBroadcastOwnResource();
        boolean isAppRunning = isApplicationRunning();
        Log.d(TAG,"the source is own broadcast = " + isBroadcastOwnResource + "the app status = " + isAppRunning);
        if (isBroadcastOwnResource) {
            if (isAppRunning) {
                terminateApplication();
                isHandleRecordKey =  false;
            }
        } else {
            if (isAppRunning) {
                Toast.makeText(mContext,
                    R.string.hbbtv_not_support_pvr, Toast.LENGTH_SHORT).show();
                isHandleRecordKey = true;
            }
        }
        Log.d(TAG,"isConsumeRecordKey end");
        return isHandleRecordKey;
   }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Clears the DSMCC File System Acceleration (FSA) disk cache.
     * @return none
     */
    @Override
    public void clearFsaDiskCache() {
        Log.i(TAG, "clearFsaDiskCache start");
        super.clearFsaDiskCache();
        Log.i(TAG, "clearFsaDiskCache end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Disables time-shift mode.
     * @return none
     */
    @Override
    public void disableTimeShiftMode() {
        Log.i(TAG,"disableTimeShiftMode start");
        super.disableTimeShiftMode();
        Log.i(TAG,"disableTimeShiftMode end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Enables/disables internal broadcast autoselector.
     * @param isEnabled  true for enabled, false for disabled.
     */
    @Override
    public void enableInternalBroadcastAutoselector​(boolean isEnabled) {
        Log.i(TAG,"enableInternalBroadcastAutoselector​ start");
        if (DEBUG) {
           Log.d(TAG,"enableInternalBroadcastAutoselector​  isEnabled = ");
        }

        super.enableInternalBroadcastAutoselector​(isEnabled);
        Log.i(TAG,"enableInternalBroadcastAutoselector​ end");
    }

   /**
    * @ingroup amlhbbtvviewapi
    * @brief  Enables time-shift mode.
    * @return none
    */
    @Override
    public void enableTimeShiftMode() {
        Log.i(TAG,"enableTimeShiftMode start");
        super.enableTimeShiftMode();
        Log.i(TAG,"enableTimeShiftMode end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Get time after which cicam protocol-served application times out
     * @return seconds after which application is treated as timed-out.
     */
    @Override
    public int getCicamLoadTimeout() {
        Log.i(TAG,"getCicamLoadTimeout start");
        Log.i(TAG,"getCicamLoadTimeout end");
        return super.getCicamLoadTimeout();
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Gets ContextHandle of this HbbTvView.
     * @return ContextHandle of this HbbTvView.
     */
    @Override
    public ContextHandle getContextHandle() {
        Log.i(TAG,"getContextHandle start");
        Log.i(TAG,"getContextHandle end");
        return super.getContextHandle();
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Returns CookieManager used by HbbTV apps. It's separate from a default CookieManager.
     * @return CookieManager used by HbbTV apps.
     */
    @Override
    public CookieManager getCookieManager() {
        Log.i(TAG,"getCookieManager start");
        Log.i(TAG,"getCookieManager end");
        return super.getCookieManager();
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Get time after which dvb protocol-served application times out.
     * @return seconds after which application is treated as timed-out.
     */
    @Override
    public int getDvbLoadTimeout() {
        Log.i(TAG,"getDvbLoadTimeout start");
        Log.i(TAG,"getDvbLoadTimeout end");
        return super.getDvbLoadTimeout();
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Get time after which http protocol-served application times out.
     * @return seconds after which application is treated as timed-out.
     */
    @Override
    public int getHttpLoadTimeout() {
        Log.i(TAG,"getHttpLoadTimeout start");
        Log.i(TAG,"getHttpLoadTimeout end");
        return super.getHttpLoadTimeout();
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Notifies that there is no AIT on the selected channel.
     * @return none
     */
    @Override
    public void invalidateAit() {
        Log.i(TAG,"invalidateAit start");
        super.invalidateAit();
        Log.i(TAG,"invalidateAit end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Notifies that the broadcast is blocked.
     * @return none
     */
    @Override
    public void notifyVideoBroadcastBlocked() {
        Log.i(TAG,"notifyVideoBroadcastBlocked start");
        super.notifyVideoBroadcastBlocked();
        Log.i(TAG,"notifyVideoBroadcastBlocked end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Notifies that the broadcast is unblocked.
     * @return none
     */
    @Override
    public void notifyVideoBroadcastUnblocked() {
        Log.i(TAG,"notifyVideoBroadcastUnblocked start");
        super.notifyVideoBroadcastUnblocked();
        Log.i(TAG,"notifyVideoBroadcastUnblocked end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Raises given registered stream event.
     * @param listenerId  Identifier of a listener passed during registration of a stream event.
     * @param eventName  Name of the event.
     * @param eventData  Data of the event.
     * @param eventText  Text of the event.
     * @param eventStatus  Status of the event.
     */
    @Override
    public void raiseDsmccStreamEvent​(int listenerId, String eventName,
            String eventData,String eventText, String eventStatus) {
        Log.i(TAG,"raiseDsmccStreamEvent​ start");
        if (DEBUG) {
           Log.d(TAG,"raiseDsmccStreamEvent​: listenerId = " + listenerId + ", eventName = "
               + eventName + ", eventData = " + eventData + ",eventText = " + eventText
               + ",eventStatus = " + eventStatus);
        }

        super.raiseDsmccStreamEvent​(listenerId, eventName, eventData, eventText, eventStatus);
        Log.i(TAG,"raiseDsmccStreamEvent​ end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Sets flag controlling AIT updates and requests to parse an AIT table for this HbbTvView
     * @param aitBound  flag controlling AIT updates and requests.
     */
    @Override
    public void setAitBound​(boolean aitBound) {
        Log.i(TAG,"setAitBound​​ start");
        if (DEBUG) {
            Log.d(TAG,"setAitBound​: aitBound = " + aitBound);
        }

        super.setAitBound​(aitBound);
        Log.i(TAG,"setAitBound​​ end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Sets broadband status.
     * @param status  Broadband status to be set
     */
    @Override
    public void setBroadbandStatus​(int status) {
        Log.i(TAG,"setBroadbandStatus​ start");
        if (DEBUG) {
            Log.d(TAG,"setBroadbandStatus​: status = " + status);
        }

        super.setBroadbandStatus​(status);
        Log.i(TAG,"setBroadbandStatus​ end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Set time after which cicam protocol-served application times out. The default value is 120s. This preference is
     *        global for all views.
     * @param seconds - after which application is treated as timed-out.
     */
    @Override
    public void setCicamLoadTimeout​(int seconds) {
        Log.i(TAG,"setCicamLoadTimeout​ start");
        if (DEBUG) {
            Log.d(TAG,"setCicamLoadTimeout​: seconds = " + seconds);
        }

        super.setCicamLoadTimeout​(seconds);
        Log.i(TAG,"setCicamLoadTimeout​ end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Sets CICAM status.
     * @param isAvailable  CICAM availability status to be set.
     */
    @Override
    public void setCicamStatus​(boolean isAvailable) {
        Log.i(TAG,"setCicamStatus​ start");
        if (DEBUG) {
           Log.d(TAG,"setCicamStatus​: isAvailable = " + isAvailable);
        }

        super.setCicamStatus​(isAvailable);
        Log.i(TAG,"setCicamStatus​ end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Set time after which dvb protocol-served application times out. The default value is 120s. This preference is global
     *        for all views.
     * @param seconds  after which application is treated as timed-out.
     */
    @Override
    public void setDvbLoadTimeout​(int seconds) {
        Log.i(TAG,"setDvbLoadTimeout​ start");
        if (DEBUG) {
            Log.d(TAG,"setDvbLoadTimeout​: seconds = " + seconds);
        }

        super.setDvbLoadTimeout​(seconds);
        Log.i(TAG,"setDvbLoadTimeout​ end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Sets HbbTvClient that will receive notifications concerning HbbTV.
     * @param hbbTvClient  HbbTvClient instance to be set. Must not be null.
     */
    @Override
    public void setHbbTvClient​(HbbTvClient hbbTvClient) {
        Log.i(TAG,"setHbbTvClient​ start");
        if (DEBUG) {
            Log.d(TAG,"setHbbTvClient​  hbbTvClient = " + hbbTvClient);
        }

        super.setHbbTvClient​(hbbTvClient);
        Log.i(TAG,"setHbbTvClient​ end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Set time after which http protocol-served application times out. The default value is 60s. This preference is global
     *        for all views.
     * @param seconds  after which application is treated as timed-out.
     */
    @Override
    public void setHttpLoadTimeout​(int seconds) {
        Log.i(TAG,"setHttpLoadTimeout​ start");
        if (DEBUG) {
            Log.d(TAG,"setHttpLoadTimeout​: seconds = " + seconds);
        }

        super.setHttpLoadTimeout​(seconds);
        Log.i(TAG,"setHttpLoadTimeout​ end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Sets flag controlling loading of Freeview applications when broadband connection is not available.
     * @param isHbbtvOnlyReceiver  flag marking if device is HbbTV only receiver
     */
    @Override
    public void setIsHbbtvOnlyReceiver​(boolean isHbbtvOnlyReceiver) {
        Log.i(TAG,"setIsHbbtvOnlyReceiver​ start");
        if (DEBUG) {
            Log.d(TAG,"setIsHbbtvOnlyReceiver​: isHbbtvOnlyReceiver = " + isHbbtvOnlyReceiver);
        }

        super.setIsHbbtvOnlyReceiver​(isHbbtvOnlyReceiver);
        Log.i(TAG,"setIsHbbtvOnlyReceiver​ end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Sets supported keyset for HbbTV applications. This method must be called before ViewBase.init().
     * @param descriptions  key descriptions.
     */
    @Override
    public void setKeyDescriptions​(KeyDescription[] descriptions) {
        Log.i(TAG,"setKeyDescriptions​ start");
        if (DEBUG) {
            Log.d(TAG,"setKeyDescriptions​: descriptions = " + Arrays.toString(descriptions));
        }

        super.setKeyDescriptions​(descriptions);
        Log.i(TAG,"setKeyDescriptions​ end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Sets supported keyset for HbbTV applications. This method must be called before ViewBase.init() to override default
     *        value: DEFAULT_SUPPORTED_KEYSET.
     * @param keyset  Keyset to be applied. Must be a bitwise combination of HbbTvKeyset values.
     */
    @Override
    public void setSupportedKeyset​(int keyset) {
        Log.i(TAG,"setSupportedKeyset​ start");
        if (DEBUG) {
           Log.d(TAG,"setSupportedKeyset​: keyset = " + keyset);
        }

        super.setSupportedKeyset​(keyset);
        Log.i(TAG,"setSupportedKeyset​ end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Sets supported profiles information
     * @param platformProfiles  Supported platform profiles as defined in HbbTvPlatformProfile.
     */
    @Override
    public void setSupportedProfiles​(int[] platformProfiles) {
        Log.i(TAG,"setSupportedProfiles​ start");
        if (DEBUG) {
            Log.d(TAG,"setSupportedProfiles​: platformProfiles = " + Arrays.toString(platformProfiles));
        }

        super.setSupportedProfiles​(platformProfiles);
        Log.i(TAG,"setSupportedProfiles​ end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Sets TunerDelegate. This method must be called before ViewBase.init().
     * @param tunerDelegate  Delegate to be set.
     */
    @Override
    public void setTunerDelegate​(TunerDelegate tunerDelegate) {
        Log.i(TAG,"setTunerDelegate​ start");
        if (DEBUG) {
            Log.d(TAG,"setTunerDelegate​ tunerDelegate = " + tunerDelegate);
        }

        super.setTunerDelegate​(tunerDelegate);
        Log.i(TAG,"setTunerDelegate​ end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Sets user agent suffix. This method must be called before ViewBase.init() to override default value:
     *        DEFAULT_USER_AGENT_SUFFIX.
     * @param userAgentSuffix  The user agent suffix to be set.
     */
    @Override
    public void setUserAgentSuffix​(String userAgentSuffix) {
        Log.i(TAG,"setUserAgentSuffix​ start");
        if (DEBUG) {
            Log.d(TAG,"userAgentSuffix = " + userAgentSuffix);
        }

        super.setUserAgentSuffix​(userAgentSuffix);
        Log.i(TAG,"setUserAgentSuffix​ end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Starts handset functionality.
     * @param  handsetCapabilities  configuration which should be used.
     * @return true when handset component was successfully initialized and started, false otherwise.
     */
    @Override
    public boolean startHandset​(HandsetCapabilities handsetCapabilities) {
        Log.i(TAG,"startHandset​ start");
        if (DEBUG) {
            Log.d(TAG,"startHandset​ HandsetCapabilities = " + handsetCapabilities);
        }
        Log.i(TAG,"startHandset​ end");
        return super.startHandset​(handsetCapabilities);
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief  Stops handset functionality.
     * @return none
     */
    @Override
    public void stopHandset() {
        Log.i(TAG,"startHandset start​");
        super.stopHandset();
        Log.i(TAG,"startHandset end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Terminates currently running application. There is no difference whether the application is broadcast related or
     *        broadcast independent.
     * @return none
     */
    @Override
    public void terminateApplication() {
        Log.i(TAG,"terminateApplication start");
        super.terminateApplication();
        Log.i(TAG,"terminateApplication end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Provides information about the HbbTV application which started last
     * @return Information about the HbbTV application. This is never null.
     */
    @Override
    public HbbTvApplicationInfo getMainApplicationInfo() {
        Log.i(TAG,"getMainApplicationInfo start");
        Log.i(TAG,"getMainApplicationInfo end");
        return super.getMainApplicationInfo();
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Terminates currently running application and starts the one marked as auto-start in AIT. There is no difference
     *        whether the application is broadcast related or broadcast independent.
     * @return none
     */
    @Override
    public void terminateApplicationAndLaunchAutostart() {
        Log.i(TAG,"terminateApplicationAndLaunchAutostart start");
        super.terminateApplicationAndLaunchAutostart();
        Log.i(TAG,"terminateApplicationAndLaunchAutostart end");
    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Determines whether any HbbTV application is running.
     * @return true when HbbTV application has started and it is running still, false otherwise.
     */
    @Override
    public boolean isApplicationRunning() {
        Log.i(TAG,"isApplicationRunning start");
        Log.i(TAG,"isApplicationRunning end");
        return super.isApplicationRunning();

    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Determines whether any running HbbTV application controls broadcast. An HbbTV application controls broadcast only if
     *         there is at least one video broadcast object, which is bound to the current channel..
     * @true if any HbbTV application controls broadcast, false otherwise.
     */
    @Override
    public boolean isControllingBroadcastApplicationRunning() {
        Log.i(TAG,"isControllingBroadcastApplicationRunning start");
        Log.i(TAG,"isControllingBroadcastApplicationRunning end");
        return super.isControllingBroadcastApplicationRunning();

    }

    /**
     * @ingroup amlhbbtvviewapi
     * @brief Determines whether any running broadcast independent HbbTV application is running.
     * @true if any broadcast independent HbbTV application is running, false otherwise.
     */
    @Override
    public boolean isBroadcastIndependentApplicationRunning() {
        Log.i(TAG,"isBroadcastIndependentApplicationRunning start");
        Log.i(TAG,"isBroadcastIndependentApplicationRunning end");
        return super.isBroadcastIndependentApplicationRunning();

    }


}

