package com.amlogic.hbbtv;

import android.util.Log;
import android.content.Context;
import android.view.View;
import android.content.Context;
import android.view.KeyEvent;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;


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

//import com.droidlogic.dtvkit.inputsource.DtvkitTvInput;
//import com.droidlogic.dtvkit.inputsource.DtvkitTvInput.DtvkitTvInputSession;
import android.media.tv.TvInputService.Session;
import android.media.tv.TvInputService;




/**
 * @ingroup hbbtvclientapi
 * @defgroup hbbtvmanagerapi HbbTV-Manager-API
 */
public class HbbTvManager{
    private static final String TAG = "HbbTvManager";
    private AmlHbbTvView mAmlHbbTvView;
    //private DtvkitTvInput.DtvkitTvInputSession mSession;
    private TvInputService.Session mSession;
    private HbbtvPreferencesManager mPreferencesManager;
    private AmlTunerDelegate mAmlTunerDelegate;
    private AmlViewClient mAmlViewClient;
    private AmlChromeClient mAmlChromeClient;
    private AmlHbbTvClient mAmlHbbTvClient;
    private Context mContext;
    private String mInuputId;
    private Uri mTuneChannelUri = null;
    //private HbbTvUISetting mHbbTvUISetting;
    private final BroadcastResourceManager mBroadcastResourceManager =
            BroadcastResourceManager.getInstance();
    //private List<TvInputService.Session> sessionList = new ArrayList<>();
    private static HbbTvManager mInstance;
    private boolean mBroadcastResourceRelease = false;

   /**
    * @ingroup hbbtvmanagerapi
    * @brief constructor method of HbbTvManager
    * @param context  The context which can used to access the tv provider
    * @param session  The session which callback the method of the session
    * @inputId  The TV Input ID
    * @HbbTvManager  The instance of HbbTvManager
    */
    private HbbTvManager() {
    }

    public static HbbTvManager getInstance() {
        if (mInstance == null) {
            synchronized (HbbTvManager.class) {
                if (mInstance == null) {
                    return new HbbTvManager();
                }
            }
        }
        return mInstance;
    }

    public void setInputId(String inputId) {
        mInuputId = inputId;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void setSession(TvInputService.Session      session) {
        mSession = session;
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
        mBroadcastResourceRelease = true;
        mBroadcastResourceManager.setActive(mBroadcastResourceClient);
        initializeHbbTvView();
        Log.d(TAG,"onBrowserInitialized end");
    }

    public void initHbbTvResource() {
        Log.d(TAG,"initHbbTvResource start");
        if (mContext != null) {
            mAmlHbbTvView = new AmlHbbTvView(mContext);
        }

        if (mSession != null) {
            mAmlTunerDelegate = new AmlTunerDelegate(mContext,mSession,mInuputId,mAmlHbbTvView);
        }
        Log.d(TAG,"initHbbTvResource end");
    }

    /**
     * @ingroup hbbtvmanagerapi
     * @brief  when the browser has finish initialization, hbbtv view need init something
     * @return none
     */
    private void initializeHbbTvView() {
        Log.d(TAG,"initializeHbbTvView start");

        mPreferencesManager = new HbbtvPreferencesManager(mAmlHbbTvView);
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
    public void releaseHbbTvResource() {
        Log.i(TAG,"releaseHbbTvResource start");
        if (mPreferencesManager != null) {
            mAmlTunerDelegate.setHbbtvPreferencesManager(null);
            mPreferencesManager = null;
        }

        if (mAmlTunerDelegate != null) {
            mAmlTunerDelegate.release();
            mAmlTunerDelegate = null;
        }
        if (mBroadcastResourceManager != null && mBroadcastResourceRelease == true) {
            mBroadcastResourceManager.unregister(mBroadcastResourceClient);
            mBroadcastResourceRelease = false;
            Log.d(TAG,"unregister mBroadcastResourceClient");
        }

        if (mSession != null) {
            mSession = null;
        }

        if (mAmlHbbTvView != null) {
            if (mAmlHbbTvView.isInitialized()) {
                Log.d(TAG,"the hbbtv view dispose");
                mAmlHbbTvView.dispose();
            } else {
                Log.d(TAG,"mAmlHbbTvView  not init");
            }
        }

        Log.i(TAG,"releaseHbbTvResource end");
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

}


