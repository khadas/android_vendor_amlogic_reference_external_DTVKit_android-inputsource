package com.amlogic.hbbtv;

import android.util.Log;
import android.content.Context;
import android.view.View;
import android.content.Context;
import android.view.KeyEvent;
import android.net.Uri;

import com.vewd.core.sdk.HbbTvView;
import com.vewd.core.sdk.Browser;
import com.vewd.core.sdk.BrowserClient;
import com.vewd.core.sdk.BroadcastResourceClient;
import com.vewd.core.sdk.BroadcastResourceReleaseRequest;
import com.vewd.core.shared.MediaComponentsPreferences;

import com.amlogic.hbbtv.utils.StringUtils;
import com.amlogic.hbbtv.utils.UserAgentUtils;
import com.amlogic.hbbtv.utils.KeyEventUtils;
import com.amlogic.hbbtv.utils.BroadcastResourceManager;

import com.droidlogic.dtvkit.inputsource.DtvkitTvInput;
import com.droidlogic.dtvkit.inputsource.DtvkitTvInput.DtvkitTvInputSession;

/**
 * @ingroup hbbtvclientapi
 * @defgroup hbbtvmanagerapi HbbTV-Manager-API
 */
public class HbbTvManager{
    private static final String TAG = "HbbTvManager";
    private AmlHbbTvView mAmlHbbTvView;
    private DtvkitTvInput.DtvkitTvInputSession mSession;
    private HbbtvPreferencesManager mPreferencesManager;
    private AmlTunerDelegate mAmlTunerDelegate;
    private AmlViewClient mAmlViewClient;
    private AmlChromeClient mAmlChromeClient;
    private AmlHbbTvClient mAmlHbbTvClient;
    private Context mContext;
    private String mInuputId;
    private Uri mTuneChannelUri = null;
    private HbbTvUISetting mHbbTvUISetting;
    private final BroadcastResourceManager mBroadcastResourceManager =
            BroadcastResourceManager.getInstance();

   /**
    * @ingroup hbbtvmanagerapi
    * @brief constructor method of HbbTvManager
    * @param context  The context which can used to access the tv provider
    * @param session  The session which callback the method of the session
    * @inputId  The TV Input ID
    * @HbbTvManager  The instance of HbbTvManager
    */
    public HbbTvManager(Context context,DtvkitTvInput.DtvkitTvInputSession session,String inputId) {
        mContext = context;
        mSession = session;
        mInuputId = inputId;
        mAmlHbbTvView = new AmlHbbTvView(mContext);
        mAmlTunerDelegate = new AmlTunerDelegate(mContext,mSession,mInuputId,mAmlHbbTvView);
        mPreferencesManager = new HbbtvPreferencesManager(mAmlHbbTvView);
        mHbbTvUISetting = new HbbTvUISetting();
    }

   /**
    * @ingroup hbbtvmanagerapi
    * @brief   get the instance of AmlHbbTvView
    * @return  the instance of AmlHbbTvView
    */
    public AmlHbbTvView getHbbTvView() {
        Log.i(TAG,"getHbbTvView start");
        Log.i(TAG,"getHbbTvView end");
        return mAmlHbbTvView;
    }

    /**
     * @ingroup hbbtvmanagerapi
     * @brief   set the sutitle view to hbbtv in order to handle which display on top
     * @return  none
     */
    public void setSubtitleView(View view) {
        Log.i(TAG,"setSubtitleView start");
        Log.i(TAG,"setSubtitleView end");
        if (null != mAmlTunerDelegate) {
            mAmlTunerDelegate.setSubtitleView(view);
        }
    }

   /**
    * @ingroup hbbtvmanagerapi
    * @brief  when the browser has finish initialization, hbbtv need register brocaset cast client
    * @return none
    */
    private void onBrowserInitialized() {
        Log.d(TAG,"onBrowserInitialized start");
        mBroadcastResourceManager.register(mBroadcastResourceClient);
        mBroadcastResourceManager.setActive(mBroadcastResourceClient);
        initializeHbbTvView();
        Log.d(TAG,"onBrowserInitialized end");
    }

    /**
     * @ingroup hbbtvmanagerapi
     * @brief  when the browser has finish initialization, hbbtv view need init something
     * @return none
     */
    private void initializeHbbTvView() {
        Log.d(TAG,"initializeHbbTvView start");
        mAmlHbbTvClient = new AmlHbbTvClient(mAmlHbbTvView, mAmlTunerDelegate);
        mAmlChromeClient = new AmlChromeClient();
        mAmlViewClient = new AmlViewClient();
        mAmlHbbTvView.setFocusable(false);
        mAmlHbbTvView.setFocusableInTouchMode(false);
        mAmlHbbTvView.setVisibility(View.INVISIBLE);
        mAmlHbbTvView.setViewClient(mAmlViewClient);
        mAmlHbbTvView.setTunerDelegate(mAmlTunerDelegate);
        mAmlHbbTvView.setChromeClient(mAmlChromeClient);
        mAmlHbbTvView.setHbbTvClient(mAmlHbbTvClient);
        mAmlHbbTvView.setUserAgentSuffix(UserAgentUtils.getVendorUserAgentSuffix());
        mAmlHbbTvView.init();
        mAmlHbbTvView.requestFocus();

        //set references
        mPreferencesManager.setDeviceUniqueNumber(null);
        mPreferencesManager.setManufacturerSecretNumber(null);
        mPreferencesManager.updateHbbTvMediaComponentsPreferences();
        mAmlTunerDelegate.setHbbtvPreferencesManager(mPreferencesManager);
        //mAmlHbbTvView.loadUrlApplication("http://itv.mit-xperts.com/hbbtvtest");
        Log.d(TAG,"initializeHbbTvView end");
    }

   /**
    * @ingroup hbbtvmanagerapi
    * @brief   when the session relase,the hbbtv resource need to release
    * @return none
    */
    public void onDestroy() {
        Log.i(TAG,"onDestroy start");
        if (mPreferencesManager != null) {
            mAmlTunerDelegate.setHbbtvPreferencesManager(null);
            mPreferencesManager = null;
        }
        if (mAmlHbbTvView != null) {
            if (mAmlHbbTvView.isInitialized()) {
                Log.d(TAG,"onDestroy  destroying");
                mAmlHbbTvView.dispose();
            } else {
                Log.d(TAG,"mAmlHbbTvView  not init");
            }
            mAmlHbbTvView = null;
        }
        if (mAmlTunerDelegate != null) {
            mAmlTunerDelegate.release();
            mAmlTunerDelegate = null;
        }
        if (mBroadcastResourceManager != null) {
            mBroadcastResourceManager.unregister(mBroadcastResourceClient);
        }
        Log.i(TAG,"onDestroy end");
    }

    private final BroadcastResourceClient mBroadcastResourceClient = new BroadcastResourceClient() {
        @Override
        public void onResourceGranted() {
            Log.i(TAG, "Resource granted");
            if (!mAmlHbbTvView.isControllingBroadcastApplicationRunning()
                    && !mAmlHbbTvView.isBroadcastIndependentApplicationRunning()) {
                mAmlTunerDelegate.tuneToCurrentChannel();
                setResourceOwnedByBr(true);
            }
        }

        @Override
        public void onResourceReleaseRequested(BroadcastResourceReleaseRequest request) {
            Log.i(TAG, "Resource release requested");
            mAmlTunerDelegate.stop();
            setResourceOwnedByBr(false);
            request.confirm();
        }
    };

   /**
    * @ingroup hbbtvmanagerapi
    * @brief  when the session creating, need to init browser,include setting the browser
    * client and browser initialize
    * @return none
    */
    public void initBrowser()     {
        if (Browser.getInstance().isInitialized()) {
            Log.d(TAG,"browser has been initialized!");
            onBrowserInitialized();
            return;
        } else {
            Log.d(TAG,"browser not initialized!");
        }

        Browser.getInstance().setBrowserClient(new BrowserClient() {
            @Override
            public void onBrowserProcessGone() {
                Log.d(TAG,"on Browser Process Gone");
            }
        });

        try {
            Browser.getInstance().initialize(mContext,
                new Browser.InitializationCallback() {
                @Override
                public void onInitialized() {
                    onBrowserInitialized();
                    Log.d(TAG,"browser initialized finish!");
                }

                @Override
                public void onError(final Exception exception) {
                    Log.d(TAG,"init browser error and the exception : " + exception);
                }
            });
        } catch(Exception e) {
            Log.e(TAG, "not support hbbtv feather");
        }
    }

    /**
     * @ingroup hbbtvmanagerapi
     * @brief  handle the key down event
     * @return true if consume this key,otherwise false
     */
    public boolean handleKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG,"handleKeyDown keyCode = " + keyCode);
        return mAmlHbbTvView.dispatchKeyEvent(event);
    }

    /**
     * @ingroup hbbtvmanagerapi
     * @brief  handle the key up event
     * @return true if consume this key,otherwise false
     */
    public boolean handleKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG,"handleKeyUp keyCode = " + keyCode);
        return mAmlHbbTvView.dispatchKeyEvent(event);
    }

    /**
     * @ingroup hbbtvmanagerapi
     * @brief  set channel tune uri
     * @return none
     */
     public void setTuneChannelUri(Uri channelUri) {
        Log.i(TAG,"setTuneChannelUri start");
        Log.d(TAG, "setTuneChannelUri channelUri =  " + channelUri);
        if (mAmlTunerDelegate != null) {
            mAmlTunerDelegate.setTuneChannelUri(channelUri);
        }
        Log.i(TAG,"setTuneChannelUri end");
    }

     /**
     * @ingroup hbbtvmanagerapi
     * @brief  set Audio Descriptions
     * @return none
     */
     public void setAudioDescriptions() {
        Log.i(TAG,"setAudioDescriptions start");
        mPreferencesManager.updateHbbTvMediaComponentsPreferences();
        Log.i(TAG,"setAudioDescriptions end");
    }

    public boolean checkIsBroadcastOwnResource() {
        Log.i(TAG,"checkIsBroadcastOwnResource start");
        boolean resourceOwnedByBroadcast = true;
        boolean hbbtvRunning = false;
        if (mAmlHbbTvView != null) {
            hbbtvRunning = mAmlHbbTvView.isApplicationRunning();
        }
        if (hbbtvRunning && !isResourceOwnedByBr()) {
            resourceOwnedByBroadcast = false;
        } else {
            resourceOwnedByBroadcast = true;
        }
        Log.d(TAG,"checkIsBroadcastOwnResource resourceOwnedByBroadcast = " + resourceOwnedByBroadcast
            + " and hbbtvRunning = " + hbbtvRunning);
        Log.i(TAG,"checkIsBroadcastOwnResource start");
        return resourceOwnedByBroadcast;
    }

    public boolean isApplicationRunning() {
        if (mAmlHbbTvView.isInitialized()) {
            Log.d(TAG,"isApplicationRunning");
            return mAmlHbbTvView.isApplicationRunning();
        } else {
            return false;
        }
    }

    private boolean isResourceOwnedByBr() {
        boolean ret = true;
        if (mAmlTunerDelegate != null) {
            ret = mAmlTunerDelegate.checkResourceOwnedIsBr();
        }
        return ret;
    }

    private void setResourceOwnedByBr(boolean flag) {
        if (mAmlTunerDelegate != null) {
            mAmlTunerDelegate.setResourceOwnerByBrFlag(flag);
        }
        Log.i(TAG,"setResourceOwnedByBr flag = " + flag);
    }

    public boolean getHbbTvFeature() {
       return mHbbTvUISetting.getHbbTvFeature();
    }

    public void setHbbTvFeature(boolean status) {
        mHbbTvUISetting.setHbbTvFeature(status);
    }

    public boolean getHbbTvServiceStatusForCurChannel() {
        return mHbbTvUISetting.getHbbTvServiceStatusForCurChannel();
    }


    public void setHbbTvServiceStatusForCurChannel(boolean status) {
        mHbbTvUISetting.setHbbTvServiceStatusForCurChannel(status);
    }

     public boolean getHbbTvTrackingStatus() {
        return mHbbTvUISetting.getHbbTvTrackingStatus();
     }


    public void setHbbTvTrackingStatus(boolean status) {
        mHbbTvUISetting.setHbbTvTrackingStatus(status);
    }

    public boolean getHbbtvCookiesStatus() {
        return mHbbTvUISetting.getHbbtvCookiesStatus();
    }

    public void setHbbTvCookiesStatus(boolean status) {
        mHbbTvUISetting.setHbbTvCookiesStatus(status);
    }

    public void clearHbbTvCookies() {
        mHbbTvUISetting.clearHbbTvCookies();
    }

    public boolean getHbbTvDistinctiveIdentifierStatus() {
        return mHbbTvUISetting.getHbbTvDistinctiveIdentifierStatus();
    }

    public void setHbbTvDistinctiveIdentifierStatus(boolean status) {
        mHbbTvUISetting.setHbbTvDistinctiveIdentifierStatus(status);
    }
}

