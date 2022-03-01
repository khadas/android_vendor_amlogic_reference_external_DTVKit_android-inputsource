package com.amlogic.hbbtv;

import android.util.Log;
import android.media.tv.TvContentRating;
import android.view.View;

import com.vewd.core.sdk.HbbTvClient;
import com.vewd.core.sdk.HbbTvFileCallback;
import com.vewd.core.sdk.AppUrlLoadRequestCallback;
import com.vewd.core.sdk.AitAppLoadRequestCallback;
import com.vewd.core.sdk.DsmccCarouselIdentificationCallback;
import com.vewd.core.sdk.HbbTvApplicationLoadTimedOutCallback;
import com.vewd.core.sdk.HbbTvRequestAccessToDistinctiveIdentifierCallback;
import com.vewd.core.shared.DvbLocator;
import com.vewd.core.shared.HbbTvApplication;

import java.util.Arrays;

import com.amlogic.hbbtv.utils.StringUtils;
import org.droidlogic.dtvkit.DtvkitGlueClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * @ingroup hbbtvclientapi
 * @defgroup amlhbbtvclientapi Aml-HbbTv-Client-API
 */
public class AmlHbbTvClient implements HbbTvClient {
    private static final String TAG = "AmlHbbTvClient";
    private static final boolean DEBUG = true;
    private AmlHbbTvView mAmlHbbTvView;
    private boolean isApplicationStarted = false;
    private String mUrl = null;
    private HbbTvApplication[] mApplications;
    private static final int INDEX_FOR_MAIN = 0;
    private int mCouneter = 0;
    private AmlTunerDelegate mAmlTunerDelegate;

    /**
     * @ingroup amlhbbtvclientapi
     * @brief constructor method of AmlHbbTvClient
     * @param amlHbbTvView  The instance of AmlHbbTvView
     * @return AmlHbbTvClient  the instance of AmlHbbTvClient
     */
    public AmlHbbTvClient(AmlHbbTvView amlHbbTvView, AmlTunerDelegate amlTunerDelegate) {
        Log.i(TAG,"AmlHbbTvClient instance create start");

        if (DEBUG) {
            Log.d(TAG,"The AmlHbbTvView is " + amlHbbTvView);
        }

        mAmlHbbTvView = amlHbbTvView;
        mAmlTunerDelegate = amlTunerDelegate;
        Log.i(TAG,"AmlHbbTvClient  instance create end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when an autostart HbbTV application could not be started. This might happen as a result of parsing AIT when
     *        either any of the available autostart applications could not be launched for some reason, or there was no autostart
     *        application in the AIT chunk. The call might give a hint to the platform to start e.g. MHEG application. Note: This
     *        method is called also when there is no AIT.
     * @param error  Error as one of ApplicationLaunchError values.
     * @param appId  Identifier of the application.
     * @param orgId  Organization identifier of the application.
     * @param appUrl  URL of the application.
     * @param isBroadcastRelated  Indicates whether the application is broadcast-related
     */
    @Override
    public void onAitApplicationNotStarted​(int error, int appId, int orgId, String appUrl,
            boolean isBroadcastRelated){
        Log.i(TAG,"onAitApplicationNotStarted​  start");

        if (DEBUG) {
            Log.d(TAG,"onAitApplicationNotStarted​ error = " + error + ", appId = " + appId + ", orgId = "
                + ", appUrl = " + StringUtils.truncateUrlForLogging(appUrl) + ", isBroadcastRelated = "
                + isBroadcastRelated);
        }

        Log.i(TAG,"onAitApplicationNotStarted​  end");

    }

    private boolean isHbbTVLoadUrl() {
        boolean isLoadUrl = true;
        try {
            JSONArray args = new JSONArray();
            boolean result = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetLoadUrlStatus", args).getBoolean("data");
            Log.d(TAG,"the result = " + result );
            isLoadUrl = result;

        } catch (Exception e) {
            Log.e(TAG, "isHbbTVLoadUrl = " + e.getMessage());
        }

        return isLoadUrl;

    }


    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called to determine whether an HbbTV application should be allow to start based on the application's url, app id, org
     *        id and parental rating. By default immediately invokes AitAppLoadRequestCallback.allow()
     * @param appId  Identifier of the application.
     * @param orgId  Organization identifier of the application.
     * @param appUrl  URL of the application.
     * @param isBroadcastRelated  Indicates whether the application is broadcast-related
     * @param appOrigin  Application origin
     * @param autostartLaunching  Flag indicating whether the application is going to be started due to auto-start signaling in AIT
     * @param ratings - Array containing tv content rating information for the application.
     * @param baseUrl  Part of the app url.
     * @param callback  Callback interface that must be used to notify result of the operation.
     */
    @Override
    public void onAitAppLoadRequest​(int appId, int orgId, String appUrl, boolean isBroadcastRelated, int appOrigin,
            boolean autostartLaunching, TvContentRating[] ratings, String baseUrl,AitAppLoadRequestCallback callback) {
        Log.i(TAG,"onAitAppLoadRequest​  start");

        if (DEBUG) {
            Log.d(TAG,"onAitAppLoadRequest​: appId = " + appId + ", orgId=" + orgId + ", appUrl="
                + StringUtils.truncateUrlForLogging(appUrl) + ", isBroadcastRelated="
                + isBroadcastRelated + ", autostartLaunching=" + autostartLaunching);
        }

        if (getHbbTvFeature()) {
            callback.allow();
            Log.d(TAG,"onAitAppLoadRequest​  url allow to load");
        } else {
            callback.deny(AitAppLoadRequestCallback.REASON_APP_URL);
            Log.d(TAG,"onAitAppLoadRequest​  url deny to load");
        }

        Log.i(TAG,"onAitAppLoadRequest​  end");
   }

   private boolean getHbbTvFeature() {
        Log.i(TAG,"getHbbTvFeature start");
        boolean hasHbbTvFeather = false;
        try {
            JSONArray args = new JSONArray();
            boolean result = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetHbbtvFeature", args).getBoolean("data");
            Log.d(TAG,"the result = " + result );
            hasHbbTvFeather = result;

        } catch (Exception e) {
            Log.e(TAG, "hasHbbTvFeather = " + e.getMessage());
        }
        Log.i(TAG,"getHbbTvFeature end");
        return hasHbbTvFeather;
    }

   private boolean isInBacKList(String url) {
        boolean isUrlInBlackList=false;
        try {
            JSONArray args = new JSONArray();
            args.put(url);
            isUrlInBlackList = DtvkitGlueClient.getInstance().request("Hbbtv.HBBIsURLInBlackList", args).getBoolean("data");
            Log.i(TAG, "isInBacKList, isUrlInBlackList:" + isUrlInBlackList);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return isUrlInBlackList;
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when there is no AIT loaded currently, which means that the hosting application should feed the HbbTV library
     *        with a new, valid AIT as soon as possible.
     * @return none
     */
    @Override
    public void onAitInvalidated() {
        Log.i(TAG,"onAitInvalidated  start");
        mApplications = null;
        Log.i(TAG,"onAitInvalidated  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when AIT parsing has been completed successfully.
     * @return none
     */
    @Override
    public void onAitParsingCompleted() {
        Log.i(TAG,"onAitParsingCompleted  start");
        Log.i(TAG,"onAitParsingCompleted  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when AIT parsing has been aborted due to an error.
     * @return none
     */
    @Override
    public void onAitParsingError() {
        Log.i(TAG,"onAitParsingError  start");
        Log.i(TAG,"onAitParsingError  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when a new chunk of AIT data has been parsed.
     * @param hbbTvApplications  Array containing HbbTvApplications available in the AIT.
     */
    @Override
    public void onAitUpdated​(HbbTvApplication[] hbbTvApplications) {
        Log.i(TAG,"onAitUpdated​  start");

        if (DEBUG) {
            Log.d(TAG,"onAitUpdated​ : hbbTvApplications = " + Arrays.toString(hbbTvApplications));
        }

        mApplications = hbbTvApplications;

        Log.i(TAG,"onAitUpdated​  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when an HbbTV application becomes active/inactive.
     * @param appId  Identifier of the application.
     * @param orgId  Organization identifier of the application.
     * @param appUrl  URL of the application.
     * @param isBroadcastRelated  Indicates whether the application is broadcast-related
     * @param isActive  A new active state, as defined in the HBBTV specification.
     */
    @Override
    public void onApplicationActiveStateChanged​(int appId, int orgId, String appUrl,
            boolean isBroadcastRelated, boolean isActive){
        Log.i(TAG,"onApplicationActiveStateChanged​  start");

        if (DEBUG) {
            Log.d(TAG,"onApplicationActiveStateChanged​: appId=" + appId + ", orgId=" + orgId + ", appUrl="
                + StringUtils.truncateUrlForLogging(appUrl) + ", isBroadcastRelated="
                + isBroadcastRelated + ", isActive=" + isActive);
        }


        Log.i(TAG,"onApplicationActiveStateChanged​  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when an HbbTV application has assigned a new keyset.
     * @param appId  Identifier of the application.
     * @param orgId  Organization identifier of the application.
     * @param appUrl  URL of the application.
     * @param isBroadcastRelated  Indicates whether the application is broadcast-related
     * @param keyset - A new keyset.
     * @param otherKeys - An array containing other keys codes allowed for the application
     */
    @Override
    public void onApplicationKeysetChanged​(int appId, int orgId, String appUrl,
            boolean isBroadcastRelated, int keyset, int[] otherKeys) {
        Log.i(TAG, "onApplicationKeysetChanged​  start");

        if (DEBUG) {
            Log.d(TAG,"onApplicationKeysetChanged​: appId=" + appId + ", orgId=" + orgId + ", appUrl="
                + StringUtils.truncateUrlForLogging(appUrl) + ", isBroadcastRelated="+ isBroadcastRelated
                + ", keyset=" + keyset + ", otherKeys = " + Arrays.toString(otherKeys));
        }

        mAmlHbbTvView.setKeySet(keyset);

        Log.i(TAG, "onApplicationKeysetChanged​  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called to notify of HbbTV Application that is loading for too long. By default aborts further execution of
     *        indefinitely loading application.
     * @param appId  Identifier of the application.
     * @param orgId  Organization identifier of the application.
     * @param appUrl  URL of the application.
     * @param isBroadcastRelated  Indicates whether the application is broadcast-related
     * @param callback  Callback interface that must be used to decide if application loading process should be aborted.
     */
    public void onApplicationLoadTimedOut​(int appId, int orgId, String appUrl,
            boolean isBroadcastRelated, HbbTvApplicationLoadTimedOutCallback callback) {
        Log.i(TAG,"onApplicationLoadTimedOut​  start");

        if (DEBUG) {
            Log.d(TAG,"onApplicationLoadTimedOut​: appId=" + appId + ", orgId=" + orgId + ", appUrl="
                + StringUtils.truncateUrlForLogging(appUrl) + ", isBroadcastRelated="+ isBroadcastRelated);
        }

        callback.abort();

        Log.i(TAG,"onApplicationLoadTimedOut​  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when application could not be started for some reason,(i.e. the application's url is blocked by url filter).
              This call might give a hint to the platform to start e.g. MHEG application.
     * @param error  Error as one of ApplicationLaunchError values.
     * @param appId  Identifier of the application.
     * @param orgId  Organization identifier of the application.
     * @param appUrl  URL of the application.
     * @param isBroadcastRelated  Indicates whether the application is broadcast-related
     */
    @Override
    public void onApplicationNotStarted​(int error, int appId, int orgId,
            String appUrl, boolean isBroadcastRelated) {
        Log.i(TAG,"onApplicationNotStarted​  start");

        if (DEBUG) {
            Log.d(TAG,"onApplicationNotStarted​: error="+ error+ ",appId = " + appId
                + ", orgId="+ orgId + ", appUrl=" + StringUtils.truncateUrlForLogging(appUrl)
                + ", isBroadcastRelated="+ isBroadcastRelated);
        }

        Log.i(TAG,"onApplicationNotStarted​  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when OIPFConfiguration's requestAccessToDistinctiveIdentifier is called by the application
     * @param appId  Identifier of the application.
     * @param orgId  Organization identifier of the application.
     * @param origin  Origin of the application's URL which makes this request.
     * @param callback  Callback interface that must be used to notify result of the operation.
     */
    @Override
    public void onApplicationRequestAccessToDistinctiveIdentifier​(int appId, int orgId, String origin,
            HbbTvRequestAccessToDistinctiveIdentifierCallback callback) {
        Log.i(TAG,"onApplicationRequestAccessToDistinctiveIdentifier​  start");

        if (DEBUG) {
           Log.d(TAG,"onApplicationRequestAccessToDistinctiveIdentifier​: appId=" + appId + ", orgId=" + orgId
               + ", origin="+ origin);
        }

        callback.allow();

        Log.i(TAG,"onApplicationRequestAccessToDistinctiveIdentifier​  end");

    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when an HbbTV application has been started.
     * @param appId  Identifier of the application.
     * @param orgId  Organization identifier of the application.
     * @param appUrl  URL of the application.
     * @param isBroadcastRelated  Indicates whether the application is broadcast-related
     * @param isStartedAutomatically  Flag indicating whether an application has been started due to auto-start signaling in AIT.
     * @param isTrusted  Flag indicating application's security policy.
     * @param isActive  Flag indicating application active state in terms of HbbTV specification.
     */
    @Override
    public void onApplicationStarted(int appId, int orgId, String appUrl,
            boolean isBroadcastRelated, boolean isStartedAutomatically,
            boolean isTrusted, boolean isActive) {
        Log.i(TAG,"onApplicationStarted  start");

        if (DEBUG) {
            Log.d(TAG,"onApplicationStarted: appId=" + appId + ", orgId=" + orgId
                + ", appUrl=" + StringUtils.truncateUrlForLogging(appUrl)
                + ", isBroadcastRelated=" + isBroadcastRelated
                + ", isStartedAutomatically=" + isStartedAutomatically
                + ", isTrusted=" + isTrusted + ", isActive=" + isActive);
        }

        isApplicationStarted = true;
        mAmlHbbTvView.setApplicationStartedStatus(isApplicationStarted);

        Log.i(TAG,"onApplicationStarted  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when an HbbTV application changed its status. The type of application might be changed between broadcast-
     *        independent and broadcast-related.
     * @param appId  Identifier of the application.
     * @param orgId  Organization identifier of the application.
     * @param appUrl  URL of the application.
     * @param isBroadcastRelated  Indicates whether the application is broadcast-related
     */
    @Override
    public void onApplicationStatusChanged​(int appId, int orgId, String appUrl, boolean isBroadcastRelated) {
        Log.i(TAG,"onApplicationStatusChanged​  start");

        if (DEBUG) {
            Log.d(TAG,"onApplicationStatusChanged​: appId=" + appId + ", orgId=" + orgId + ", appUrl="
                + StringUtils.truncateUrlForLogging(appUrl) + ", isBroadcastRelated="+ isBroadcastRelated);
        }

        Log.i(TAG,"onApplicationStatusChanged​  end");

    }


    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when an HbbTV application has been stopped.
     * @param appId  Identifier of the application.
     * @param orgId  Organization identifier of the application.
     * @param appUrl  URL of the application.
     * @param isBroadcastRelated  Indicates whether the application is broadcast-related
     * @param willLoadNewApp  Indicates whether a new application load is already pending.
     */
    @Override
    public void onApplicationStopped(int appId, int orgId, String appUrl,
            boolean isBroadcastRelated, boolean willLoadNewApp) {
        Log.i(TAG,"onApplicationStopped  start");

        if (DEBUG) {
            Log.d(TAG,"onApplicationStopped: appId=" + appId + ", orgId=" + orgId + ", appUrl="
                + StringUtils.truncateUrlForLogging(appUrl) + ", isBroadcastRelated="
                + isBroadcastRelated + ", willLoadNewApp=" + willLoadNewApp);
        }
        mAmlTunerDelegate.startAVByCheckResourceOwned();
        mAmlTunerDelegate.setResourceOwnerByBrFlag(true);
        mAmlTunerDelegate.setFullScreen();
        mAmlHbbTvView.setKeySet(0);

        Log.i(TAG,"onApplicationStopped  end");
    }


    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when an HbbTV application has been shown or hidden.
     * @param appId  Identifier of the application.
     * @param orgId  Organization identifier of the application.
     * @param appUrl  URL of the application.
     * @param isBroadcastRelated  Indicates whether the application is broadcast-related
     * @param visible  Indicates whether the application is visible.
     */
    public void onApplicationVisibilityChanged​(int appId, int orgId, String appUrl,
            boolean isBroadcastRelated, boolean visible) {
        Log.i(TAG,"onApplicationVisibilityChanged​  start");

        if (DEBUG) {
            Log.d(TAG,"onApplicationVisibilityChanged​: appId=" + appId + ", orgId=" + orgId + ", appUrl="
                + StringUtils.truncateUrlForLogging(appUrl) + ", isBroadcastRelated="
                + isBroadcastRelated + ", visible=" + visible);
        }

        if (visible) {
            mCouneter++;
        } else {
            mCouneter--;
        }

        if (mCouneter == 0) {
            Log.d(TAG,"hbbtv view invisiable");
            if (mAmlHbbTvView.getFocusable()!= View.NOT_FOCUSABLE) {
                mAmlHbbTvView.setFocusable(false);
            }
            if (mAmlHbbTvView.getVisibility() != View.INVISIBLE)  {
                mAmlHbbTvView.setVisibility(View.INVISIBLE);
            }

        }

        if (mCouneter > 0) {
            Log.d(TAG,"hbbtv view visiable");
            if (mAmlHbbTvView.getFocusable() != View.FOCUSABLE) {
                mAmlHbbTvView.setFocusable(true);
            }
            if (mAmlHbbTvView.getVisibility() != View.VISIBLE)  {
                mAmlHbbTvView.setVisibility(View.VISIBLE);
            }
        }

        Log.i(TAG,"onApplicationVisibilityChanged​  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called to determine if HbbTV application url should be loaded
     * @param appUrl  URL of the application.
     * @param appOrigin  Application origin
     * @param callback  Callback interface that must be used to notify result of the operation.
     */
    @Override
    public void onAppUrlLoadRequest(String appUrl, int appOrigin, AppUrlLoadRequestCallback callback) {
        Log.i(TAG,"onAppUrlLoadRequest  start");

        if (DEBUG) {
            Log.d(TAG,"onAppUrlLoadRequest appurl = "+ StringUtils.truncateUrlForLogging(appUrl)
                + "appOrigin = " + appOrigin);
        }

        callback.allow();

        Log.i(TAG,"onAppUrlLoadRequest  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when a file from CICAM is requested. By default the request is aborted.
     * @param appId  Identifier of the application.
     * @param orgId  Organization identifier of the application.
     * @param domain  Domain string in the CI rul
     * @param path  Path component of the CI URL.
     * @param origin  Origin of the application's URL which makes this request.
     * @param isInsideBoundary  true if origin of requested resource is inside application boundary, false otherwise.
     * @param callback  Callback interface that must be used to notify result of the operation.
     */
    @Override
    public void onCicamFileRequested​(int appId, int orgId, String domain, String path, String origin,
            boolean isInsideBoundary, HbbTvFileCallback callback) {
        Log.i(TAG,"onCicamFileRequested​  start");

        if (DEBUG) {
            Log.d(TAG,"onCicamFileRequested​: appId=" + appId + ", orgId=" + orgId + ", domain="
                + domain + ", path=" + path + ", origin=" + origin + ", isInsideBoundary = " + isInsideBoundary);
        }

        callback.notifyFailure();

        Log.i(TAG,"onCicamFileRequested​  start");

    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called to determine whether given DvbLocators represent the same DSM-CC carousel.
     * @param dvbLocator1  First DvbLocator..
     * @param dvbLocator2  Second DvbLocator.
     * @param callback  Callback interface that must be used to notify result of the operation.
     */
    @Override
    public void onDsmccCarouselIdentificationRequested​(DvbLocator dvbLocator1, DvbLocator dvbLocator2,
            DsmccCarouselIdentificationCallback callback) {
        Log.i(TAG,"onDsmccCarouselIdentificationRequested​  start");

        if (DEBUG) {
            Log.d(TAG,"onDsmccCarouselIdentificationRequested​: dvbLocator1=" + dvbLocator1+ ", dvbLocator2=" + dvbLocator2);
        }

        callback.notifyOtherCarousel();

        Log.i(TAG,"onDsmccCarouselIdentificationRequested​  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called to register a DSM-CC stream event listener.
     * @param isXmlRegistration  Flag indicating whether stream event was registered using an XML file.
     * @param path  Path of the DSM-CC stream event object.
     * @param eventName  Name of the event.
     * @param eventId  Identifier of event.
     * @param originalNetworkId  Original network id of the transport stream containing the events.
     * @param transportStreamId  Transport stream id of the transport stream containing the events.
     * @param serviceId  Identifier of service that carries the events.
     * @param componentTag  Component tag of service that carries the events.
     * @param videoId  Identifier of video broadcast object corresponding to the registration
     * @param listenerId  Identifier of a listener matching this registration.
     */
    @Override
    public void onDsmcccSubscribeStreamEvent​(boolean isXmlRegistration, String path, String eventName, int eventId,
            int originalNetworkId, int transportStreamId, int serviceId, int componentTag, int videoId, int listenerId) {
        Log.i(TAG,"onDsmcccSubscribeStreamEvent​  start");

        if (DEBUG) {
            Log.d(TAG,"onDsmcccSubscribeStreamEvent​: isXmlRegistration=" + isXmlRegistration + ", path=" + path + ", eventName="
                + eventName + ", eventId=" + eventId + ", originalNetworkId=" + originalNetworkId + ", transportStreamId = "
                + transportStreamId + ", serviceId" + serviceId + ", componentTag = " + componentTag  + ", videoId = "
                + videoId + ", listenerId = " + listenerId);
        }

        Log.i(TAG,"onDsmcccSubscribeStreamEvent​  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when a file from DSM-CC carousel is requested. By default the request is aborted.
     * @param appId  Identifier of the application.
     * @param orgId  Organization identifier of the application.
     * @param dvbLocator  DvbLocator identifying a DSM-CC carousel containing the requested file.
     * @param path  Path of the DVB URL.
     * @param queryString  Query string of the DVB URL.queryString - Query string of the DVB URL.
     * @param origin  Origin of the application's URL which makes this request.
     * @param isInsideBoundary  true if origin of requested resource is inside application boundary, falseotherwise.
     * @param callback  Callback interface that must be used to notify result of the operation.
     */
    @Override
    public void onDsmccFileRequested​(int appId, int orgId, DvbLocator dvbLocator, String path,
            String queryString, String origin, boolean isInsideBoundary, HbbTvFileCallback callback) {
        Log.i(TAG,"onDsmccFileRequested​  start");

        if (DEBUG) {
            Log.d(TAG,"onDsmccFileRequested​: appId=" + appId + ", orgId=" + orgId + ", dvbLocator="
                + dvbLocator + ", path=" + path + ", origin=" + origin + ", isInsideBoundary = " + isInsideBoundary
                + ", queryString = " + queryString);
        }

        callback.notifyFailure();

        Log.i(TAG,"onDsmccFileRequested​  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called at the beginning of application loading.
     * @param applicationProfile  Application profile which should be used for DSMCC purposes.
     */
    @Override
    public void onDsmccSetApplicationProfile​(int applicationProfile) {
        Log.i(TAG,"onDsmccSetApplicationProfile​  start");

        if (DEBUG) {
            Log.d(TAG,"onDsmccSetApplicationProfile​: applicationProfile=" + applicationProfile);
        }

        Log.i(TAG,"onDsmccSetApplicationProfile​  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when FSA cache limit preference changes.
     * @param fsaCacheLimit  New FSA cache limit in MB
     */
    @Override
    public void onDsmccSetFsaCacheLimit​(int fsaCacheLimit) {
        Log.i(TAG,"onDsmccSetFsaCacheLimit​  start");

        if (DEBUG) {
            Log.d(TAG,"onDsmccSetFsaCacheLimit​: fsaCacheLimit=" + fsaCacheLimit);
        }

        Log.i(TAG,"onDsmccSetFsaCacheLimit​  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when FSA cache limit preference changes.
     * @param major  Major version number of HbbTV profile that should be used by DSMCC
     * @param minor  Minor version number of HbbTV profile that should be used by DSMCC
     * @param micro  Micro version number of HbbTV profile that should be used by DSMCC
     */
    @Override
    public void onDsmccSetPlatformProfile​(int major, int minor, int micro) {
        Log.i(TAG,"onDsmccSetPlatformProfile​  start");

        if (DEBUG) {
            Log.d(TAG,"onDsmccSetPlatformProfile​: major=" + major + ", minor=" + minor + ", micro=" + micro);
        }

        Log.i(TAG,"onDsmccSetPlatformProfile​  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called to unregister all DSM-CC stream event listeners that were registered with the given videoId.
     * @param videoId  Identifier of video broadcast object whose stream events should be unregistered.
     */
    @Override
    public void onDsmccUnsubscribeAllStreamEvents​(int videoId) {
        Log.i(TAG,"onDsmccUnsubscribeAllStreamEvents​  start");

        if (DEBUG) {
            Log.d(TAG,"onDsmccSetPlatformProfile​: videoId=" + videoId);
        }

        Log.i(TAG,"onDsmccUnsubscribeAllStreamEvents​  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called to unregister the specific DSM-CC stream event listener.
     * @param eventId  Identifier of event.
     * @param originalNetworkId  Original network id of the transport stream containing the events.
     * @param transportStreamId  Transport stream id of the transport stream containing the events.
     * @param serviceId  Identifier of service that carries the events.
     * @param componentTag  Component tag of service that carries the events.
     * @param path  Path of the DSM-CC stream event object.
     * @param eventName  Name of the event.
     * @param videoId  Identifier of video broadcast object corresponding to the registration
     * @param listenerId  Identifier of a listener matching this registration.
     */
    @Override
    public void onDsmccUnsubscribeStreamEvent​(int originalNetworkId, int transportStreamId,
            int serviceId,int componentTag, String path, String eventName, int listenerId) {
        Log.i(TAG,"onDsmccUnsubscribeStreamEvent​  start");

        if (DEBUG) {
            Log.d(TAG,"onDsmccUnsubscribeStreamEvent​: originalNetworkId=" + originalNetworkId
                + ", transportStreamId = " + transportStreamId + ", serviceId = "+ serviceId
                + ", componentTag = "+ componentTag + ", path = " + path
                + ", eventName = " + eventName + "listenerId = " + listenerId);
        }
        Log.i(TAG,"onDsmccUnsubscribeStreamEvent​  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called to notify that Media Synchronizer and Companion Screen module also known as Handset module has started.
     * @param success  true if handset started successfully. Otherwise in case of fail on start false is passed.
     */
    @Override
    public void onHandsetStarted​(boolean success) {
        Log.i(TAG,"onHandsetStarted​  start");

        if (DEBUG) {
            Log.d(TAG,"onHandsetStarted​: success=" + success);
        }

        Log.i(TAG,"onHandsetStarted​  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called when the detected MHEG5 application state changes.This is only advisory.
     * @param originalNetworkId  Original network id of the transport stream where the state has changed.
     * @param transportStreamId  Transport stream id of the transport stream where the state has changed.
     * @param serviceId  Identifier of service where the state has changed.
     * @param mheg5ApplicationPresent  Whether an MHEG5 application is present
     */
    @Override
    public void onMheg5ApplicationStatusChanged​(int originalNetworkId, int transportStreamId,
            int serviceId, boolean mheg5ApplicationPresent) {
        Log.i(TAG,"onMheg5ApplicationStatusChanged​  start");

        if (DEBUG) {
            Log.d(TAG,"onMheg5ApplicationStatusChanged​: originalNetworkId=" + originalNetworkId
                + ", transportStreamId = " + transportStreamId + ", serviceId = "+ serviceId
                + ", mheg5ApplicationPresent = "+ mheg5ApplicationPresent);
        }

        Log.i(TAG,"onMheg5ApplicationStatusChanged​  end");
    }
    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called to notify that an HbbTV Application has taken control of the broadcast by binding to the current channel in a
     *        video broadcast object.
     * @param videoBroadcastId  Identifier of the video broadcast.
     * @param appId  Identifier of the application.
     * @param orgId  Organization identifier of the application.
     * @param appUrl  URL of the application.
     * @param isBroadcastRelated  Indicates whether the application is broadcast-related.
     */

    @Override
    public void onVideoBroadcastActivated​(int videoBroadcastId, int appId, int orgId,
            String appUrl, boolean isBroadcastRelated) {
        Log.i(TAG,"onVideoBroadcastActivated​  start");

        if (DEBUG) {
            Log.d(TAG,"onVideoBroadcastActivated​: videoBroadcastId=" + videoBroadcastId + ", appId = " + appId
                + ", orgId=" + orgId + ", appUrl="+ StringUtils.truncateUrlForLogging(appUrl)
                + ", isBroadcastRelated="+ isBroadcastRelated);
        }

        Log.i(TAG,"onVideoBroadcastActivated​  end");
    }

    /**
     * @ingroup amlhbbtvclientapi
     * @brief Called to notify that an HbbTV Application has released control of the broadcast by unbinding from the current
     *        channel in a video broadcast object.
     * @param videoBroadcastId  Identifier of the video broadcast.
     * @param appId  Identifier of the application.
     * @param orgId  Organization identifier of the application.
     * @param appUrl  URL of the application.
     */

    @Override
    public void onVideoBroadcastDeactivated​(int videoBroadcastId, int appId, int orgId, String appUrl) {
        Log.i(TAG,"onVideoBroadcastDeactivated​  start");

        if (DEBUG) {
            Log.d(TAG,"onVideoBroadcastDeactivated​: videoBroadcastId=" + videoBroadcastId + ", appId = " + appId
                + ", orgId=" + orgId + ", appUrl="+ StringUtils.truncateUrlForLogging(appUrl));
        }

        Log.i(TAG,"onVideoBroadcastDeactivated​  end");
    }

    public HbbTvApplication[] getApplications() {
        return mApplications;
    }


}
