package com.amlogic.hbbtv;

import android.util.Log;
import android.content.Context;
import android.view.View;
import android.content.Context;
import android.view.KeyEvent;
import android.net.Uri;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import com.vewd.core.sdk.HbbTvView;
import com.vewd.core.sdk.Browser;
import com.vewd.core.sdk.BrowserClient;
import com.vewd.core.sdk.BroadcastResourceClient;
import com.vewd.core.sdk.BroadcastResourceReleaseRequest;
import com.vewd.core.shared.MediaComponentsPreferences;
import com.vewd.core.sdk.HbbTvApplicationInfo;

import com.amlogic.hbbtv.utils.StringUtils;
import com.amlogic.hbbtv.utils.UserAgentUtils;
import com.amlogic.hbbtv.utils.KeyEventUtils;
import com.amlogic.hbbtv.utils.BroadcastResourceManager;

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
    private final BroadcastResourceManager mBroadcastResourceManager =
            BroadcastResourceManager.getInstance();
    private static HbbTvManager mInstance;
    private boolean mBroadcastResourceRelease = false;
    private NetworkChangeBroadcast mNetworkChangeBroadcast;

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
                    mInstance = new HbbTvManager();
                }
            }
        }
        return mInstance;
    }

    public void setHbbTvManagerParams(TvInputService.Session              session,String inputId,Context context) {
        Log.d(TAG,"setHbbTvManagerParams start");
        mContext = context;
        mInuputId = inputId;
        mSession = session;
        Log.d(TAG,"setHbbTvManagerParams end");
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
        mAmlHbbTvView.setZOrderOnTop(true);
        mAmlHbbTvView.init();
        mAmlHbbTvView.requestFocus();
        mNetworkChangeBroadcast = new NetworkChangeBroadcast();
        regeisterNetWorkBroadcastReceiver();

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
        if (mNetworkChangeBroadcast != null) {
            unRegeisterNetWorkBroadcastReceiver();
            mNetworkChangeBroadcast = null;
        }
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
        if (mAmlHbbTvView != null && mAmlHbbTvView.isInitialized()) {
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

    private void regeisterNetWorkBroadcastReceiver() {
           IntentFilter intentFilter = new IntentFilter();
           intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
           mContext.registerReceiver(mNetworkChangeBroadcast, intentFilter);
       }

    private void unRegeisterNetWorkBroadcastReceiver() {
           mContext.unregisterReceiver(mNetworkChangeBroadcast);
     }

    public void setHbbTvApplication(boolean status) {
        Log.d(TAG,"the hbbtv feather status  = " + status);
        boolean appStatus = status;
        if (status) {
            reloadApplicaition();
        } else {
            terminateHbbTvApplicaiton();
        }

    }

    public void reloadApplicaition() {
        Log.i(TAG,"reloadApplicaition start");
        if (mAmlHbbTvView != null && mAmlHbbTvView.isInitialized()) {
            if (!mAmlHbbTvView.isApplicationRunning()
                    && mAmlHbbTvClient.getApplicationStatus() == AmlHbbTvClient.ApplicaitonStatus.APP_STOPPED) {
                 mAmlHbbTvView.terminateApplicationAndLaunchAutostart();
            }
        }
        Log.i(TAG,"reloadApplicaition end ");
    }

    public void terminateHbbTvApplicaitonWithoutNetwork() {
        Log.i(TAG,"terminateHbbTvApplicaitonWithoutNetwork start");
        if (mAmlHbbTvView != null && mAmlHbbTvView.isInitialized()) {
            if (mAmlHbbTvView.isApplicationRunning() && checkIsBroadcastOwnResource()) {
                if (!isDmsccURL() && !mAmlHbbTvView.isBroadcastIndependentApplicationRunning()) {
                    mAmlHbbTvView.terminateApplication();
                }
            }
        }
        Log.i(TAG,"terminateHbbTvApplicaitonWithoutNetwork end");
    }

    public void terminateHbbTvApplicaitonWithoutSignal() {
            Log.i(TAG,"terminateHbbTvApplicaitonWithoutSignal start");
            if (mAmlHbbTvView != null && mAmlHbbTvView.isInitialized()) {
                if (mAmlHbbTvView.isApplicationRunning() && checkIsBroadcastOwnResource()) {
                    if (!mAmlHbbTvView.isBroadcastIndependentApplicationRunning()) {
                         mAmlHbbTvView.terminateApplication();
                    }
                }
            }
            Log.i(TAG,"terminateHbbTvApplicaitonWithoutSignal end");
        }



    public void terminateHbbTvApplicaiton() {
        Log.i(TAG,"terminateHbbTvApplicaiton start");
        if (mAmlHbbTvView != null && mAmlHbbTvView.isInitialized()) {
            if (mAmlHbbTvView.isApplicationRunning()) {
                mAmlHbbTvView.terminateApplication();
            }
        }
        Log.i(TAG,"terminateHbbTvApplicaiton end");
    }


    private boolean isDmsccURL () {
        Log.i(TAG,"isDmsccURL start");
        boolean result = false;
        HbbTvApplicationInfo appInfo = mAmlHbbTvView.getMainApplicationInfo();
        String url = appInfo.getUrl();
        Log.d(TAG,"the url = " + url);
        String str = url.substring(0,3);
        if (str.equals("dvb")) {
            result = true;
        } else {
            result =false;
        }
        Log.d(TAG,"isDmsccURL result = " + result);
        Log.i(TAG,"isDmsccURL end");
        return result;
    }

    public boolean isHbbTvApplicationRunning() {
        Log.i(TAG,"isHbbTvApplicationRunning start");
        boolean isHbbTvRunning = false;
        if (mAmlHbbTvView != null && mAmlHbbTvView.isInitialized()) {
            isHbbTvRunning =  mAmlHbbTvView.isApplicationRunning();
            Log.d(TAG,"The HbbTv application status isHbbTvRunning = " + isHbbTvRunning);
        }
        Log.i(TAG,"isHbbTvApplicationRunning end");
        return isHbbTvRunning;
    }
}


