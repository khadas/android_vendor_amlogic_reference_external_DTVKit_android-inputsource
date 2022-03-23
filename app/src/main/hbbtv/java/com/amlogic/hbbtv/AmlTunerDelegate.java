package com.amlogic.hbbtv;

import android.util.Log;
import android.net.Uri;
import android.media.tv.TvTrackInfo;
import android.graphics.Rect;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.support.v4.content.LocalBroadcastManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.media.tv.TvInputService.Session;
import android.media.tv.TvInputService;

import com.vewd.core.sdk.TunerDelegate;
import com.vewd.core.sdk.TunerDelegateClient;
import com.vewd.core.shared.Channel;
import com.vewd.core.shared.TunerDelegateChannelErrorState;
import com.vewd.core.shared.TunerDelegateVideoUnavailableReason;
import com.vewd.core.shared.TunerError;
import com.vewd.core.shared.TunerState;
import com.vewd.core.shared.TrackInfoExtra;
import com.amlogic.hbbtv.utils.BroadcastResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import android.util.ArraySet;
import com.amlogic.hbbtv.utils.AmlHbbTvTvContractUtils;
//import com.droidlogic.dtvkit.inputsource.DtvkitTvInput;
//import com.droidlogic.dtvkit.inputsource.DtvkitTvInput.DtvkitTvInputSession;

import com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService;
import org.droidlogic.dtvkit.DtvkitGlueClient;
import com.droidlogic.settings.ConstantManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import java.util.stream.Collectors;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.os.Build;
import com.droidlogic.dtvkit.inputsource.ISO639Data;
import android.text.TextUtils;
import java.nio.ByteBuffer;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Method;


public class AmlTunerDelegate implements TunerDelegate {
    private static final String TAG = "AmlTunerDelegate";

    //private Uri mCurrentChannelUri;
    private final static int INVALID_PID = 131071;//0X1FFF
    private Context mContext;
    private TvInputService.Session mSession = null;
    private Handler mThreadHandler = null;
    private String mInputId = null;
    private List<TvTrackInfo> mAllTracksInfo = null;
    private List<TvTrackInfo> mPrivateTracksInfo = new ArrayList<>();
    private Map<Integer, String> mSelectTrackInfo = null;
    private final Object mTrackLock = new Object();
    private static final int INDEX_FOR_MAIN = 0;
    private boolean mChannelListUpdate = false;
    private int mPlayState = PlayState.PLAYSTATE_INIT;
    private int mTunerState = TunerStatus.TUNERSTATUS_INIT;
    private boolean mIsFirstEnter = false;
    private final ArraySet<TunerDelegateClient> mTunerDelegateClientList = new ArraySet<>();
    private Uri  mTunedChannelUri = null;
    private boolean mVisiblility = false;
    private final BroadcastResourceManager mBroadcastResourceManager = BroadcastResourceManager.getInstance();
    private AmlHbbTvView mAmlHbbTvView;
    private boolean mOwnResourceByBr = true;
    private HbbtvPreferencesManager mPreferencesManager;
    private int mScreenSize_width = 0;
    private int mScreensize_heigh = 0;
    private boolean mWeakSignal = false;
     /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by HbbTvManager when the browser is started. construct AmlTunerDelegate.
    * @param context - The context of the application.
    * @param session - DtvkitTvInputSession.
    * @param inputId - The ID of the pass-through input to build a channels URI for.
    */
    public AmlTunerDelegate(Context context,TvInputService.Session session,
                                    String inputId, AmlHbbTvView amlHbbTvView ){
        Log.i(TAG, "new AmlTunerDelegate in");
        mContext = context;
        mSession = session;
        mInputId = inputId;
        mAmlHbbTvView = amlHbbTvView;
        mIsFirstEnter = true;
        initHandler();
        mPlayState = PlayState.PLAYSTATE_INIT;
        mTunerState = TunerStatus.TUNERSTATUS_INIT;
        DtvkitGlueClient.getInstance().registerSignalHandler(mHandler);
        mChannelListUpdate = false;
        DtvkitGlueClient.getInstance().setPidFilterListener(blistener);
        mOwnResourceByBr = true;
        Log.i(TAG, "new AmlTunerDelegate out");
    }

    private void checkFirstEnterStatus() {
        Log.i(TAG, "checkFirstEnterStatus in");
        Log.d(TAG, "checkFirstEnterStatus mIsFirstEnter = " + mIsFirstEnter);
        sendNotifyMsg(MSG.MSG_CHANNELLISTUPDATEFINISHED, 0, 0, null);
        if (mIsFirstEnter) {
            if (null != mTunedChannelUri) {
                sendNotifyMsg(MSG.MSG_CHANNELCHANGED, 0, 0, null);
                if (mTunerState == TunerStatus.TUNERSTATUS_LOCK) {
                    sendNotifyMsg(MSG.MSG_TUNERSTATECHANGED, TunerState.LOCKED, TunerError.NO_ERROR, null);
                }
                if (mPlayState == PlayState.PLAYSTATE_PLAYING) {
                    sendNotifyMsg(MSG.MSG_VIDEOAVALIABLE, 1, 0, null);
                }
            }
        }

        mIsFirstEnter = false;
        Log.i(TAG, "checkFirstEnterStatus out");
    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the browser immediately after this TunerDelegate is provided to the Vewd Core view.
    * This API allows multiple clients for a given TunerDelegate object. The provided TunerDelegateClient is to be used to
    * provide responses to the tuner-related requested received via methods of this interface.
    * @param client -  client TunerDelegateClient for this TunerDelegate.
    * @return none
    */
    @Override
    public void addClient​(TunerDelegateClient client) {
        Log.i(TAG, "addClient​ in");
        mTunerDelegateClientList.add(client);
        checkFirstEnterStatus();
        Log.i(TAG, "addClient​ out");
    }

    /**
    * This API will be deprecated
    */
    @Override
    public void setClient(TunerDelegateClient client) {
        Log.i(TAG, "setClient - in");
        Log.d(TAG, "setClient - this API will be deprecated in favour of setClient(TunerDelegateClient).");
        Log.i(TAG, "setClient - out");
    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @briefCalled by the browser when TunerDelegateClient is about to be disposed.
    * @param client -  client TunerDelegateClient for this TunerDelegate.
    * @return none
    */
    @Override
    public void removeClient(TunerDelegateClient client) {
        Log.i(TAG, "removeClient in");
        mTunerDelegateClientList.remove(client);
        Log.i(TAG, "removeClient out");
    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief  Called by the browser when the video/broadcast object is released.
    * Control of broadcast video presentation video/broadcast object is released and video shall be re-scaled
    * and re-positioned (if necessary). See OIPF v.5 specification, appendix H.2
    * @param none.
    * @return none
    */
    @Override
    public void videoBroadcastObjectReleased() {
        Log.i(TAG, "videoBroadcastObjectReleased in");

        if (mAmlHbbTvView.isControllingBroadcastApplicationRunning()) {
            Log.d(TAG, "videoBroadcastObjectReleased in BR running");

        } else if(mAmlHbbTvView.isBroadcastIndependentApplicationRunning()) {
            Log.d(TAG, "videoBroadcastObjectReleased in BI running need tuneToCurrentChannel");
            tuneToCurrentChannel();
        } else {
            Log.d(TAG, "videoBroadcastObjectReleased  BR is not running need tuneToCurrentChannel");
            tuneToCurrentChannel();
        }
        setFullScreen();
        //setVisibility​(false);
        Log.i(TAG, "videoBroadcastObjectReleased out");

    }

    public void setScreenSizeForHbbtv(int width, int height) {
        Log.i(TAG, "setScreenSizeForHbbtv in");
        mScreenSize_width = width;
        mScreensize_heigh = height;
        Log.d(TAG, "setScreenSizeForHbbtv mScreenSize_width = " + mScreenSize_width + ", mScreensize_heigh = " + mScreensize_heigh);
        Log.i(TAG, "setScreenSizeForHbbtv out");
    }

    public void setFullScreen() {
        Log.i(TAG, "setFullScreen in");
        int width = mScreenSize_width;
        int height = mScreensize_heigh;
        if (width == 0 || height == 0) {
            try {
                JSONArray args_input = new JSONArray();
                JSONObject screenSize;
                screenSize = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetScreenSize", args_input);
                if (screenSize != null) {
                    screenSize = (JSONObject)screenSize.get("data");
                    if (screenSize != null) {
                        width = screenSize.getInt("width");
                        height = screenSize.getInt("height");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "setFullScreen - HBBGetScreenSize= " + e.getMessage());
                return;
            }
        }

        if (width == 0 || height == 0) {
            Log.i(TAG, "setFullScreen out - width & height invalid");
            return;
        }
        Log.d(TAG, "setFullScreen mScreenSize_width = " + mScreenSize_width + ", mScreensize_heigh = " + mScreensize_heigh);
        Log.d(TAG, "setFullScreen width = " + width + ", height = " + height);
        try {
            JSONArray args = new JSONArray();
            args.put(INDEX_FOR_MAIN);
            args.put(0);
            args.put(0);
            args.put(width);
            args.put(height);
            DtvkitGlueClient.getInstance().request("Player.setRectangle", args);
        } catch (Exception e) {
            Log.e(TAG, "setFullScreen = " + e.getMessage());
        }
        setVideoSizeChanged(0, 0, width, height);
        Log.i(TAG, "setFullScreen out");
    }

     /**
    * @ingroup AmlTunerDelegateapi
    * @brief  Called by the browser when the video/broadcast object is released.
    * Control of broadcast video presentation video/broadcast object is released and video shall be re-scaled
    * and re-positioned (if necessary). See OIPF v.5 specification, appendix H.2
    * @param none.
    * @return none
    */
    @Override
    public void setVisibility​(boolean visible) {
        Log.i(TAG, "setVisibility​ in");
        mVisiblility = visible;
        Log.d(TAG, "setVisibility​ mVisiblility = " + mVisiblility);

        Log.i(TAG, "setVisibility​ out");
    }

    /**
    * This API will be deprecated: Use setChannel(String, int) instead
    */
    @Override
    public void setChannel​(String ccid) {
        Log.i(TAG, "setChannel - only ccid in");
        Log.d(TAG, "setChannel - Use setChannel(String, int) instead.");
        Log.i(TAG, "setChannel  - only ccid  out");
    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the browser when the running application requests the channel to be changed
    * to the one uniquely identified by the given identifier.
    * @param ccid - Unique terminal-wise identifier of the channel or null if empty channel was selected.
    * @param quietMode - The flag indicating whether the channel set operation was carried out quietly,
    * as described in clause A.2.4.3.2 of HbbTV 2.0.2 Specification.
    * @return none
    */
    @Override
    public void  setChannel​(String ccid, int quietMode) {
        Log.i(TAG, "setChannel in");
        Log.d(TAG, "setChannel ccid= " + ccid + ", quietMode= " + quietMode);
        if (null == ccid) {
            notifyChannelChangedError(null, TunerDelegateChannelErrorState.UNKNOWN_CHANNEL);
            return;
        }
        Uri channelUri =
                AmlHbbTvTvContractUtils.getChannelUriByUniqueId(mContext.getContentResolver(), mInputId, ccid);
        Log.d(TAG, "setChannel channelUri= " + channelUri);
        if (null == channelUri) {
            notifyChannelChangedError(null, TunerDelegateChannelErrorState.CHANNEL_NOT_FOUND);
            return;
          } else {
            mPlayState = PlayState.PLAYSTATE_INIT;
            switch (quietMode) {
                case ChannelChangeQuietMode.MODE_NORMAL:
                    mSession.onTune(channelUri,null);
                    mSession.notifyChannelRetuned(channelUri);
                    break;
                case ChannelChangeQuietMode.MODE_NORMAL_NO_UI_DISPLAY:
                    mSession.onTune(channelUri,null);
                    break;
                case ChannelChangeQuietMode.MODE_QUIET:
                    mSession.onTune(channelUri,null);
                    break;
                default:
                    mSession.onTune(channelUri,null);
                break;
            }
            setTuneChannelUri(channelUri);
            //notifyChannelChanged();
          }
       Log.i(TAG, "setChannel out");
    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the browser when the running application requests the channel to be changed to channel
    * that is not present in the channel list (and whose CCID is thus not known),
    * but has been specified by the given Delivery System Descriptor and Service ID.
    * @param dsd - Contents of the Delivery System Descriptor denoting the transport stream of the requested channel.
    * @param serviceId - Service ID of the requested channel.
    * @return none
    */
    @Override
    public void setChannelByDsd(byte[] dsd, int serviceId) {
        Log.i(TAG, "setChannelByDsd in");
        Log.d(TAG, "setChannelByDsd serviceId = " + serviceId);
        Uri channelUri = AmlHbbTvTvContractUtils.getChannelUriByDsd(mContext.getContentResolver(), mInputId, dsd, serviceId);
        if (null == channelUri) {
            notifyChannelChangedError(Channel.Builder.createWithDsd(dsd, serviceId),
              TunerDelegateChannelErrorState.UNKNOWN_CHANNEL);
        } else {
            mPlayState = PlayState.PLAYSTATE_INIT;
            mSession.onTune(channelUri,null);
            Log.d(TAG, "setChannelByTriplet onTune");
        }
        Log.i(TAG, "setChannelByDsd out");
    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the browser when the running application requests the channel to be changed to channel that
    * is not present in the channel list (and whose CCID is thus not known), but has been specified by the given
    * DVB triplet denoting the transport stream and service ID.
    * @param broadcastSystemType - Broadcast system type of the requested channel.
    * @param originalNetworkId - Original Network ID of the requested channel's transport stream.
    * @param transportStreamId - Transport Stream ID of the requested channel. The value of 0 indicates
    * that the TSID has not been specified.
    * @param serviceId - Service ID of the requested channel.
    * @return none
    */
    @Override
    public void setChannelByTriplet(String broadcastSystemType, int originalNetworkId,
          int transportStreamId, int serviceId) {
        Log.d(TAG, "setChannelByTriplet in");
        Log.d(TAG, "setChannelByTriplet broadcastSystemType " + broadcastSystemType  + ", originalNetworkId = " + originalNetworkId +
                ", transportStreamId = " + transportStreamId + ", serviceId = " + serviceId);
        Uri channelUri = AmlHbbTvTvContractUtils.getChannelUriByTriplet(mContext.getContentResolver(), mInputId, broadcastSystemType,
                            originalNetworkId, transportStreamId, serviceId);
        Log.d(TAG, "setChannelByTriplet channelUri= " + channelUri);
        if (null == channelUri) {
            notifyChannelChangedError(
                   Channel.Builder.createWithTriplet(
                            broadcastSystemType, originalNetworkId, transportStreamId, serviceId),
                    TunerDelegateChannelErrorState.UNKNOWN_CHANNEL);
        } else {
            mPlayState = PlayState.PLAYSTATE_INIT;
            mSession.onTune(channelUri,null);
            setTuneChannelUri(channelUri);
            notifyChannelChanged();
            Log.d(TAG, "setChannelByTriplet onTune");
        }
        Log.i(TAG, "setChannelByTriplet out");

    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the browser when the running application requests to change the channel by an offset relative
    * to the currently presenting channel.
    * @param offset - Offset relative to current channel to select new channel.
    * Offset equal to 0 is a valid parameter denoting the request to bind to the current channel as per definition in
    * OIPF v.5 specification, which includes making sure the presentation of audio and video for current channel is enabled.
    * @return none
    */
    @Override
    public void switchChannelByOffset(int offset) {
        Log.d(TAG, "switchChannelByOffset in");
        Log.d(TAG, "switchChannelByOffset offset = " + offset);
        if (0 == offset) {
            tuneToCurrentChannel();
            return;
        }  else {
            //Uri curChannelUri = mSession.getCurTuneChannelUri();
            Uri channelUri = AmlHbbTvTvContractUtils.getChannelUriByOffset(mContext.getContentResolver(), mInputId, mTunedChannelUri, offset);
            Log.d(TAG, "switchChannelByOffset mTunedChannelUri = " + mTunedChannelUri + ", channelUri = " + channelUri);
            if (null == channelUri) {
                notifyChannelChangedError(null, TunerDelegateChannelErrorState.CANNOT_NEXT_OR_PREV);
            } else {
                if (mBroadcastResourceManager.isGranted()) {
                     Log.d(TAG, "switchChannelByOffset broadcast resource manager is granted");
                     mPlayState = PlayState.PLAYSTATE_INIT;
                     mSession.onTune(channelUri, null);
                     notifyChannelChanged();
                     mSession.notifyChannelRetuned(channelUri);
                } else {
                    Log.d(TAG, "switchChannelByOffset force request resource!");
                    forceRequestResource();

                }

            }
        }
        Log.i(TAG, "switchChannelByOffset out");
    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief bind to current channel, for resource granted.
    * @param none.
    * @return none.
    */
    public void tuneToCurrentChannel() {
        Log.i(TAG, "tuneToCurrentChannel in");
        //Uri curChannelUri = mSession.getCurTuneChannelUri();
        //Log.d(TAG, "tuneToCurrentChannel curChannelUri = " + curChannelUri);
        enableSiband(false);
        if (null != mTunedChannelUri) {
            if (mBroadcastResourceManager.isGranted()) {
                Log.d(TAG, "tuneToCurrentChannel broadcast resource manager is granted. mPlayState = " + mPlayState);
                if (mPlayState != PlayState.PLAYSTATE_PLAYING
                        && mPlayState != PlayState.PLAYSTATE_CONNECTING) {
                    mPlayState = PlayState.PLAYSTATE_CONNECTING;
                    mSession.onTune(mTunedChannelUri, null);
                }
            } else {
                Log.d(TAG, "tuneToCurrentChannel force request resource!");
                forceRequestResource();
            }
        } else {
            notifyChannelChangedError(null, TunerDelegateChannelErrorState.UNKNOWN_CHANNEL);
        }
        Log.i(TAG, "tuneToCurrentChannel out");
    }

    private void forceRequestResource() {
        Log.i(TAG, "forceRequestResource in ");
        Log.i(TAG,
                "forceRequestResource; isBRAppControllingBroadcast: "
                        + mAmlHbbTvView.isControllingBroadcastApplicationRunning()
                        + "; isBIAppRunning: " + mAmlHbbTvView.isBroadcastIndependentApplicationRunning());
        if (mAmlHbbTvView.isControllingBroadcastApplicationRunning()
                || mAmlHbbTvView.isBroadcastIndependentApplicationRunning()) {
            // force request is needed here to release broadcast resources, which is granted to
            // HbbTv application. It breaks VoD but no other solution is available.
            mBroadcastResourceManager.forceRequestBroadcastResource();
        } else {
            // Do not force request here. It breaks VoD, which will not be resumed if HbbTv app
            // will survive channel switching. Instead pause HbbTv app, which will cause that
            // resource will go back to us.
            //deactivateOverlay(OverlayEventSource.INTERNAL);
        }
        Log.i(TAG, "forceRequestResource out ");
    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief set channel url.
    * @param ChannelUri.
    * @return none.
    */
    public void setTuneChannelUri(Uri ChannelUri) {
        Log.i(TAG, "setTuneChannelUri in ");
        Log.d(TAG,"setTuneChannelUri  ChannelUri = " + ChannelUri);
        mTunedChannelUri = ChannelUri;
        Log.i(TAG, "setTuneChannelUri out ");
    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the browser when the running application requests that the given TV track is selected
    * for the given track type.
    * @param type - Type of the track that is being selected. One of the following constants:
    * TvTrackInfo.TYPE_AUDIO, TvTrackInfo.TYPE_VIDEO, TvTrackInfo.TYPE_SUBTITLE.
    * @param trackId - Id of the track to select or null to unselect the currently selected track of the given type (if any).
    * @return none
    */
    @Override
    public void selectTrack(int type, String trackId) {
        Log.i(TAG, "selectTrack in");
        Log.d(TAG, "selectTrack type = " + type + ", trackId = " +  trackId);
        String destTrackId = transferTrackIdToOriginalTrackId(type, trackId);
        TvTrackInfo tmpTrackInfo = getTrackInfoBytrackId(type, trackId);
        boolean isPrivateTrack = false;
        if (tmpTrackInfo != null) {
            isPrivateTrack = tmpTrackInfo.getExtra().getBoolean(HbbTvConstantManager.KEY_TRACK_IS_PRIVATE);
        }

        Log.d(TAG, "selectTrack isPrivateTrack = " + isPrivateTrack);
         if (isPrivateTrack) {
            int pid = tmpTrackInfo.getExtra().getInt(TrackInfoExtra.TRACK_INFO_PID);
            String encoding = getEncodingForTrack(tmpTrackInfo);
            Log.d(TAG, "selectTrack - private track  type = " + type + "pid = " + pid + ", encoding = " +  encoding);
            sendNotifyMsg(MSG.MSG_SELECTPRIVATETRACK, type, pid, encoding);
        } else {
            playerSelectTrackById(type, destTrackId);
        }
        notifyTrackSelected(type, trackId);
        updateSelectTrackInfo(type, trackId);
        Log.i(TAG, "selectTrack out");
    }

    private String transferTrackIdToOriginalTrackId(int type, String trackId) {
        Log.i(TAG, "transferTrackIdToOriginalTrackId in");
        String destTrackId = null;
        String srcTrackId = trackId;

        if (null != srcTrackId && type <= TvTrackInfo.TYPE_SUBTITLE) {
            if (!TextUtils.isEmpty(srcTrackId) && !TextUtils.isDigitsOnly(srcTrackId)) {
                String[] nameValuePairs = trackId.split(":");
                if (nameValuePairs != null && nameValuePairs.length == 2) {
                    destTrackId = nameValuePairs[1];
                    Log.i(TAG, "transferTrackIdToOriginalTrackId nameValuePairs[0] = " + nameValuePairs[0] + ", nameValuePairs[1] = " + nameValuePairs[1]);
                }
            }
        }
        Log.i(TAG, "transferTrackIdToOriginalTrackId out");
        return destTrackId;
    }

    private boolean doSelectVideoTrack(int type, String trackId) {
        Log.i(TAG, "doSelectVideoTrack in");
        boolean result = false;
        Log.i(TAG, "doSelectTrack type = " + type + ", trackId = " + trackId );
        if (type == TvTrackInfo.TYPE_VIDEO) {
            result = playerSelectVideoTrackById(trackId);
        }
        Log.i(TAG, "doSelectVideoTrack out");
        return result;

    }

    private TvTrackInfo getTrackInfoBytrackId(int type, String trackId) {
        Log.i(TAG, "getTrackInfoBytrackId in");
        if (trackId == null) {
            return null;
        }
        TvTrackInfo tmpTrackInfo = null;
        synchronized (mTrackLock) {
            if (mAllTracksInfo != null && mAllTracksInfo.size() >0) {
                for (int i =0; i<mAllTracksInfo.size(); i++) {
                    if ((type == mAllTracksInfo.get(i).getType()) &&
                        (trackId != null && trackId.equals(mAllTracksInfo.get(i).getId()))) {
                            Log.i(TAG, "getTrackInfoBytrackId find same track");
                            TvTrackInfo.Builder track = new TvTrackInfo.Builder(type, trackId);
                            Bundle bundle = new Bundle();
                            bundle.putInt(TrackInfoExtra.TRACK_INFO_PID, mAllTracksInfo.get(i).getExtra().getInt(TrackInfoExtra.TRACK_INFO_PID));
                            bundle.putBoolean(HbbTvConstantManager.KEY_TRACK_IS_PRIVATE, mAllTracksInfo.get(i).getExtra().getBoolean(HbbTvConstantManager.KEY_TRACK_IS_PRIVATE));
                            bundle.putBoolean(HbbTvConstantManager.KEY_TRACK_HASCOMPONENTTAG,  mAllTracksInfo.get(i).getExtra().getBoolean(HbbTvConstantManager.KEY_TRACK_HASCOMPONENTTAG));
                            bundle.putInt(TrackInfoExtra.TRACK_INFO_COMPONENT_TAG, mAllTracksInfo.get(i).getExtra().getInt(TrackInfoExtra.TRACK_INFO_COMPONENT_TAG));
                            String encoding = getEncodingForTrack(mAllTracksInfo.get(i));
                            setEncodingForTrack(track, encoding, bundle);
                            track.setExtra(bundle);
                            tmpTrackInfo = track.build();
                        }
                }

            }
        }
        Log.i(TAG, "getTrackInfoBytrackId out");
        return tmpTrackInfo;
    }

    private boolean playerSelectTrackById(int type, String trackId) {
        Log.i(TAG, "playerSelectTrackById in");
        boolean ret = true;
        if (type == TvTrackInfo.TYPE_VIDEO) {
            ret = playerSelectVideoTrackById(trackId);
        } else if (type == TvTrackInfo.TYPE_AUDIO) {
            ret = playerSelectAudioTrackById(trackId);
        } else if (type == TvTrackInfo.TYPE_SUBTITLE) {
            ret = playerSelectSubtitleTrackById(trackId);
        }
        Log.i(TAG, "playerSelectTrackById out");
        return ret;
    }

    private boolean playerSelectSubtitleTrackById(String trackId) {
        Log.i(TAG, "playerSelectSubtitleTrackById in");
        Log.d(TAG, "playerSelectSubtitleTrackById trackId = " + trackId);
        if (trackId != null) {
            setSubtitleSwichFlagByHbbtv(false);
        } else {
            setSubtitleSwichFlagByHbbtv(true);//means off by HBBTV test
        }
        mSession.onSelectTrack(TvTrackInfo.TYPE_SUBTITLE, trackId);
        Log.i(TAG, "playerSelectSubtitleTrackById out");
        return true;
    }

    private boolean playerSelectAudioTrackById(String trackId) {
        Log.i(TAG, "playerSelectAudioTrackById in");
        Log.d(TAG, "playerSelectAudioTrackById trackId = " + trackId);
        if (trackId != null) {
            mSession.onSelectTrack(TvTrackInfo.TYPE_AUDIO, trackId);
        } else {
            try {
                JSONArray args = new JSONArray();
                DtvkitGlueClient.getInstance().request("Hbbtv.HBBUnselectAudioTrack", args);
            } catch (Exception e) {
                Log.e(TAG, "playerSelectAudioTrackById = " + e.getMessage());
                return false;
            }
        }
        Log.i(TAG, "playerSelectAudioTrackById out");
        return true;
    }

     private boolean playerSelectVideoTrackById(String trackId) {
        Log.i(TAG, "playerSelectVideoTrackById in");
        Log.d(TAG, "playerSelectVideoTrackById trackId = " + trackId);

        try {
            JSONArray args = new JSONArray();
            if (null != trackId) {
                DtvkitGlueClient.getInstance().request("Hbbtv.HBBSelectVideoTrack", args);
            } else {
                DtvkitGlueClient.getInstance().request("Hbbtv.HBBUnselectVideoTrack", args);
            }
        } catch (Exception e) {
            Log.e(TAG, "playerSelectVideoTrackById = " + e.getMessage());
            return false;
        }
        Log.i(TAG, "playerSelectVideoTrackById out");
        return true;
    }

     private boolean playerPrivateTrack(int type, int pid, String encoding) {
        Log.i(TAG, "playerPrivateTrack in");
        Log.d(TAG, "playerPrivateTrack type = " + type + "pid = " + pid + "encoding= " + encoding);

        try {
            JSONArray args = new JSONArray();
            args.put(type);
            args.put(pid);
            args.put(encoding);
            if (null != encoding) {
                DtvkitGlueClient.getInstance().request("Hbbtv.HBBSelectPrivateComponent", args);
            }
        } catch (Exception e) {
            Log.e(TAG, "playerSelectVideoTrackById = " + e.getMessage());
            return false;
        }
        Log.i(TAG, "playerSelectVideoTrackById out");
        return true;
    }

    private String getSubtitleTrackId(JSONObject compObj) {
        String trackId = null;
        try {
            if (compObj.getString("encoding").equals("TXT")) {
                int teletexttype = compObj.getInt("teletext_type");
                if (!compObj.getBoolean("hearing_impaired")) {
                    trackId = "id=" + Integer.toString(compObj.getInt("trackId")) + "&" + "type=" + "4" + "&teletext=1&hearing=0&flag=TTX";
                } else {
                    trackId = "id=" + Integer.toString(compObj.getInt("trackId")) + "&" + "type=" + "4" + "&teletext=1&hearing=1&flag=TTX-H.O.H";
                }
            } else {
                int subtitletype = compObj.getInt("subtitle_type");
                Boolean isHdSubtitle = false;
                if (subtitletype >= ConstantManager.ADB_SUBTITLE_TYPE_DVB && subtitletype <= ConstantManager.ADB_SUBTITLE_TYPE_DVB_HD) {
                    if (subtitletype == ConstantManager.ADB_SUBTITLE_TYPE_DVB_HD) {
                        isHdSubtitle = true;
                    }
                    trackId = "id=" + Integer.toString(compObj.getInt("trackId")) + "&" + "type=" + "4" + "&teletext=0&hearing=0&flag=none" + "&hd=" + (isHdSubtitle?"1":"0");//TYPE_DTV_CC
                } else if (subtitletype >= ConstantManager.ADB_SUBTITLE_TYPE_DVB_HARD_HEARING && subtitletype <= ConstantManager.ADB_SUBTITLE_TYPE_DVB_HARD_HEARING_HD) {
                    if (subtitletype == ConstantManager.ADB_SUBTITLE_TYPE_DVB_HARD_HEARING_HD) {
                        isHdSubtitle = true;
                    }
                    trackId = "id=" + Integer.toString(compObj.getInt("trackId")) + "&" + "type=" + "4" + "&teletext=0&hearing=1&flag=H.O.H" + "&hd=" + (isHdSubtitle?"1":"0");//TYPE_DTV_CC
                } else {
                    trackId = "id=" + Integer.toString(compObj.getInt("trackId")) + "&" + "type=" + "4" + "&teletext=0&hearing=0&flag=none" + "&hd=" + (isHdSubtitle?"1":"0");//TYPE_DTV_CC
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getSubtitleTrackId Exception = " + e.getMessage());
            return null;
        }
        Log.e(TAG, "getSubtitleTrackId trackId = " + trackId);
        return trackId;
    }


    private String getTrackId(int index, int trackType, JSONObject compObj){
        Log.i(TAG, "getTrackId in ");
        String trackId = null;
        if (TvTrackInfo.TYPE_AUDIO == trackType) {
            trackId = "audio" + ":" + Integer.toString(index);

        } else if (TvTrackInfo.TYPE_VIDEO == trackType) {
            trackId = "video" + ":" + Integer.toString(index);
        } else if (TvTrackInfo.TYPE_SUBTITLE == trackType) {
            trackId = "subtitle" + ":" + getSubtitleTrackId(compObj);
        }

        Log.d(TAG, "getTrackId trackId= " + trackId + ", trackType= " + trackType + ", index = " + index);
        Log.i(TAG, "getTrackId out ");
        return trackId;
    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the browser when the running application requests that the TV track is created for the A/V stream
    * signalled as private data and intended to be only appropriate for presentation under the control of a running application.
    * See ETSI TS 102 796 V1.5.1, section A.2.4.6.
    * @param tvTrack - tvTrack metadata specifying the private TvTrack.
    * @return none
    */
    @Override
    public void createPrivateTrack(TvTrackInfo tvTrack) {
        Log.i(TAG, "createPrivateTrack in ");
        if (null == tvTrack) {
            Log.w(TAG, "createPrivateTrack out - tvTrack is null ");
            return;
        }
        int componentTag = tvTrack.getExtra().getInt(TrackInfoExtra.TRACK_INFO_COMPONENT_TAG);
        int trackType = tvTrack.getType();
        TvTrackInfo privateTrack = null;
        String trackId = null;
        boolean bMatch = false;
        if ((trackType !=  TvTrackInfo.TYPE_AUDIO) && (trackType !=  TvTrackInfo.TYPE_VIDEO)
            && (trackType !=  TvTrackInfo.TYPE_SUBTITLE)) {
            Log.w(TAG, "createPrivateTrack out - unsupport track type ");
            return;
        }
        Log.d(TAG, "createPrivateTrack componentTag= " + componentTag + ", trackType= " + trackType);
        synchronized (mTrackLock) {
            int trackCount = 0;
            for (int trackIdx = 0; trackIdx< mAllTracksInfo.size(); trackIdx++) {
                if (mAllTracksInfo.get(trackIdx).getType() == trackType) {
                    trackCount ++;
                }
            }
            trackId = Integer.toString(trackCount);//getTrackId(trackCount, trackType);
            if (null == trackId) {
                Log.w(TAG, "createPrivateTrack out - trackId is error");
                return;
            }
        }
        //private tracks
        try {
            JSONArray args = new JSONArray();
            JSONObject obj = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetPrivateComponents", args);
            if (obj != null && obj.getBoolean("accepted")) {
                JSONArray privateComps = obj.getJSONArray("data");
                for (int i = 0; i < privateComps.length(); i++)
                {
                    JSONObject tmpComp = privateComps.getJSONObject(i);
                    Log.d(TAG, "getSubtitleTrackInfoList privateStream = " + tmpComp.toString());
                    if (tmpComp.getBoolean("bHasCompTag") && (componentTag == tmpComp.getInt("componentTag"))) {
                        Bundle bundle = new Bundle();
                        bundle.putInt(TrackInfoExtra.TRACK_INFO_COMPONENT_TAG, componentTag);
                        bundle.putInt(TrackInfoExtra.TRACK_INFO_PID, tmpComp.getInt("pid"));
                        bundle.putBoolean(HbbTvConstantManager.KEY_TRACK_IS_PRIVATE, true);
                        bundle.putBoolean(HbbTvConstantManager.KEY_TRACK_HASCOMPONENTTAG, true);
                        TvTrackInfo.Builder tvTmpTrack = new TvTrackInfo.Builder(trackType, trackId);
                        //tvTmpTrack.setEncoding(tvTrack.getEncoding());
                        String encoding = getEncodingForTrack(mAllTracksInfo.get(i));
                        setEncodingForTrack(tvTmpTrack, encoding, bundle);
                        tvTmpTrack.setExtra(bundle);
                        privateTrack = tvTmpTrack.build();
                        bMatch = true;
                        break;
                     }
                 }
            }
        } catch (Exception e) {
            Log.e(TAG, "createPrivateTrack Exception = " + e.getMessage());
        }
        if (bMatch) {
            synchronized (mTrackLock) {
                 mAllTracksInfo.add(privateTrack);
            }
            notifyTracksChanged();
        } else {
            Log.d(TAG, "createPrivateTrack - does not found corresponding private component");
        }
        Log.i(TAG, "createPrivateTrack out ");
    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the browser when the running application requests that the set of previously created private TV tracks
    * should be removed.
    * @param tvTrackIds - list of IDs specifying the tracks to remove.
    * @return none
    */
    @Override
    public void removePrivateTracks(List<String> tvTrackIds) {
        Log.i(TAG, "removePrivateTracks in ");
        if ((null == tvTrackIds) || (0 == tvTrackIds.size() )) {
            Log.w(TAG, "removePrivateTracks tvTrackIds is invalid");
            return;
        }
        boolean bTrackUpadate = false;
        synchronized (mTrackLock) {
            if ((null == mAllTracksInfo) || (0 == mAllTracksInfo.size())) {
                Log.w(TAG, "removePrivateTracks no track info");
                return;
            }
            Log.d(TAG, "removePrivateTracks input track size = " + tvTrackIds.size() + ", exsit track size =  " + mAllTracksInfo.size());
            for (int i = 0; i < tvTrackIds.size(); i++) {
                String trackId = tvTrackIds.get(i);
                int idx = 0;
                for (idx = 0; idx <  mAllTracksInfo.size(); idx++) {
                    String exsitTrackId = mAllTracksInfo.get(i).getId();
                    int exsitTrackType = mAllTracksInfo.get(i).getType();
                    if ((null != trackId) && (trackId.equals(exsitTrackId))) {
                        Log.d(TAG, "removePrivateTracks remove track info: idx = " + idx + ", trackId =" + trackId + ", exsitTrackType = " + exsitTrackType);
                        mAllTracksInfo.remove(idx);
                        bTrackUpadate = true;
                        break;
                    }
                }
            }
        }
        Log.d(TAG, "removePrivateTracks bTrackUpadate= " + bTrackUpadate);
        if (bTrackUpadate) {
            notifyTracksChanged();
        }
        Log.i(TAG, "removePrivateTracks out ");
    }


    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the browser when the broadcast view rectangle needs to be repositioned to given coordinates
    * and/or resized to the given dimensions.
    * @param rect - Rectangle defining the broadcast view in screen coordinates
    * @return none
    */
    @Override
    public void setVideoWindow(Rect rect) {
        Log.i(TAG, "setVideoWindow in" );
        if (null == rect) {
            Log.w(TAG, "setVideoWindow input parameter is invalid " );
            return;
        }

        int x = rect.left;
        int y = rect.top;
        int width = rect.right - rect.left;
        int height = rect.bottom - rect.top;

        Log.d(TAG, "setVideoWindow x= " + x + ", y = " + y + ",  width = " + width + ", height = " + height + ", mVisiblility = " + mVisiblility);

        if (!mVisiblility) {
            Log.i(TAG, "setVideoWindow out -mVisiblility is false, don't need resize window");
            return;
        }
        try {
            JSONArray args = new JSONArray();
            args.put(INDEX_FOR_MAIN);
            args.put(x);
            args.put(y);
            args.put(width);
            args.put(height);
            DtvkitGlueClient.getInstance().request("Player.setRectangle", args);
        } catch (Exception e) {
            Log.e(TAG, "setVideoWindow = " + e.getMessage());
        }
        setVideoSizeChanged(x, y, width, height);
        Log.i(TAG, "setVideoWindow out" );
    }

    private void setVideoSizeChanged (int x, int y, int width, int height) {
        Log.i(TAG, "setVideoSizeChanged in" );
        try {
               JSONArray args = new JSONArray();
               args.put(x);
               args.put(y);
               args.put(width);
               args.put(height);
               DtvkitGlueClient.getInstance().request("Hbbtv.HBBSetWindowSizeChanged", args);
           } catch (Exception e) {
               Log.e(TAG, "setVideoSizeChanged = " + e.getMessage());
           }
           Log.i(TAG, "setVideoSizeChanged out" );

    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the browser when the running application requests to stop the presentation of broadcast
    * In the result of this call, only video and audio presentation should be stopped,
    * as defined in OIPF v.5 specification, section 7.13.1.3.
    * This should have no effect on access to non-media broadcast resources (such as EIT information) or
    * on the state of potentially running HbbTV application.
    * @param none
    * @return none
    */
    @Override
    public void stop() {
        Log.i(TAG, "stop in" );
        boolean result = true;
        Log.d(TAG, "stop start curstate-mPlayState= " + mPlayState);

        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Hbbtv.HBBStopChannel", args);
            result = true;
        } catch (Exception e) {
            Log.e(TAG, "stop = " + e.getMessage());
            result = false;
        }
        enableSiband(true);
        mPlayState = PlayState.PLAYSTATE_STOP;
        if (result) {
            sendNotifyMsg(MSG.MSG_VIDEOUNAVALIABLE, TunerDelegateVideoUnavailableReason.UNKNOWN, 0, null);
        }
        Log.d(TAG, "stop curstate-mPlayState= " + mPlayState);
        Log.i(TAG, "stop out" );
    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the browser to determine if AmlTunerDelegate implementation supports PID filters registration.
    * @param none
    * @return whether TunerDelegate supports PID filters; true: support; false: unsupport.
    */
    @Override
    public boolean supportsPidFilters() {
        Log.d(TAG, "supportsPidFilters ");
        return true;
    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the browser to request a filter for the given PID to be started.
    * It will only be called if supportsPidFilters() returns true. The AmlTunerDelegate implementation is expected to
    * feed all data for the specified PID using TunerDelegateClient.onPidFilterData(java.nio.ByteBuffer) calls
    * until filter is stopped with stopPidFilter(int) call.
    * @param pid - PID of the table for which filter should be started.
    * @return none.
    */
    @Override
    public void startPidFilter(int pid) {
       // Log.i(TAG, "startPidFilter in ");
       // Log.d(TAG, "startPidFilter  pid= " + pid);
       try {
	       JSONArray args = new JSONArray();
	       args.put(pid);
	       DtvkitGlueClient.getInstance().request("Hbbtv.HBBStartPidFilter", args);
           } catch (Exception e) {
	          Log.e(TAG, "startPidFilter = " + e.getMessage());
           }
       // Log.i(TAG, "startPidFilter out" );
    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the browser to stop a filter for the given PID to be started.
    * It will only be called if supportsPidFilters() returns true.
    * @param pid - PID of the table for which filter should be stopped.
    * @return none.
    */
    @Override
    public void stopPidFilter(int pid) {
        //Log.i(TAG, "stopPidFilter in ");
       // Log.d(TAG, "stopPidFilter  pid= " + pid);
        try {
             JSONArray args = new JSONArray();
             args.put(pid);
             args.put(false);
             DtvkitGlueClient.getInstance().request("Hbbtv.HBBStopPidFilter", args);
            } catch (Exception e) {
                Log.e(TAG, "stopPidFilter = " + e.getMessage());
            }
       // Log.i(TAG, "stopPidFilter out ");
    }

     /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the delegate to stop all filters request from start.
    * @return none.
    */
    public void stopAllPidFilter() {
        //Log.i(TAG, "stopAllPidFilter in ");
        try {
             JSONArray args = new JSONArray();
             args.put(INVALID_PID);
             args.put(true);
             DtvkitGlueClient.getInstance().request("Hbbtv.HBBStopPidFilter", args);
            } catch (Exception e) {
                Log.e(TAG, "stopAllPidFilter = " + e.getMessage());
            }
       // Log.i(TAG, "stopAllPidFilter out ");
    }


    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the browser to enable timeline in order to report position and timeline details of VideoBroadcast bound
    * to the currently running application. If the succeeding logic following this call ends, result of this operation
    * (enabling timeline) should be sent back to browser using onTimelineEnabled callback from TunerDelegateClient object,
    * with status parameter set accordingly to the outcome. After that, it should also periodically update current position of
    * the media in Video Broadcast using onPositionObtained from TunerDelegateClient object. This procedure should be repeated
    * no more than each 150 ms and shall end as soon as disableTimeline(java.lang.String) with matching timelineSelector parameter arrives.
    * @param timelineSelector - String containing timeline selector. Possible values are listed in ETSI TS 103 286-2 V1.1.1, table 5.3.3.1.
    * @return none.
    */
    @Override
    public void enableTimeline​(String timelineSelector) {
        Log.i(TAG, "enableTimeline​ in ");
        Log.d(TAG, "enableTimeline​  timelineSelector= " + timelineSelector);
        Log.i(TAG, "enableTimeline​ in ");

    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the browser to disable specified timeline.
    * @param timelineSelector - String containing timeline selector. Possible values are listed in ETSI TS 103 286-2 V1.1.1, table 5.3.3.1.
    * @return none.
    */
    @Override
    public void disableTimeline(String timelineSelector) {
        Log.i(TAG, "disableTimeline in ");
        Log.d(TAG, "disableTimeline  timelineSelector= " + timelineSelector);
        Log.i(TAG, "disableTimeline in ");

    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the browser to obtain details related to enabled timeline. Browser will call this to obtain details related to
    * enabled timeline in order to be able to recalculate current position in the media playing in the Video Broadcast.
    * Integration should expect this call after enableTimeline(java.lang.String) is being issued.
    * The details related to the timeline, recognized via timelineSelector parameter, should be sent back to browser
    * via onTimelineDetailsObtained callback from TunerDelegateClient object.
    * @param timelineSelector - String containing timeline selector. Possible values are listed in ETSI TS 103 286-2 V1.1.1, table 5.3.3.1.
    * @return none.
    */
    @Override
    public void getTimelineDetails(String timelineSelector) {
        Log.i(TAG, "getTimelineDetails in ");
        Log.d(TAG, "getTimelineDetails  timelineSelector= " + timelineSelector);
        Log.i(TAG, "getTimelineDetails in ");

    }

    private void notifyPidFilterData​(ByteBuffer pidFilterData){
         if (mTunerDelegateClientList.size()>0) {
             for (TunerDelegateClient client : mTunerDelegateClientList) {
                client.onPidFilterData​(pidFilterData);
             }
         }
    }

    private void printPidFilterData(ByteBuffer data){
        Log.d(TAG, "Get data from native, size:" + data.capacity());
        Log.d(TAG,"**********print data in**************");
        data.flip();
        for (int i=0;i < data.limit();i++) {
            System.out.printf("%02x ",data.get());
            if (((i+1)%16) == 0)
                System.out.printf( "\n");
        }
        Log.d(TAG,"**********print data out**************");
   }

    private final DtvkitGlueClient.PidFilterListener blistener = new DtvkitGlueClient.PidFilterListener() {
        @Override
        public void onPidFilterData(ByteBuffer data){
           // Log.d(TAG, "Get data from native, size:" + data.capacity());
            notifyPidFilterData​(data);
        }
    };

    private void notifyChannelListChanged ()      {
        Log.i(TAG, "notifyChannelListChanged in ");
        setChannelListChanged();

        //List<Channel> channels = AmlHbbTvTvContractUtils.getChannelList(null, INPUT_ID);
        final List<Uri> channelUris = AmlHbbTvTvContractUtils.getChannelUrisForInput(mContext.getContentResolver(), mInputId);
        if (channelUris.size() == 0) {
            Log.i(TAG, "notifyChannelListChanged out - channel list is empty");
            return;
        }
        final List<Channel> channels = channelUris.stream()
                                               .map(channelUri -> {return AmlHbbTvTvContractUtils.getChannelByDetailsInfo(
                                                           mContext.getContentResolver(), channelUri);
                                               }).collect(Collectors.toList());
        Log.d(TAG, "notifyChannelListChanged channel number= " + channels.size());
        if (channels.size() == 0) {
            Log.w(TAG, "notifyChannelListChanged channel list is null");
        } else {
            for (TunerDelegateClient client : mTunerDelegateClientList) {
                client.onChannelListChanged(channels);
                Log.d(TAG, "notify onChannelListChanged ");
            }
        }
        Log.i(TAG, "notifyChannelListChanged out ");
    }

    private void enableSiband(boolean enable) {
        Log.i(TAG, "enableSiband in enable = " + enable);

        try {
            JSONArray args = new JSONArray();
            args.put(enable);
            DtvkitGlueClient.getInstance().request("Hbbtv.HBBEnableSideBand", args);
        } catch (Exception e) {
            Log.e(TAG, "enableSiband = " + e.getMessage());
        }
        Log.i(TAG, "enableSiband out");
    }
    private void setChannelListChanged() {
        Log.i(TAG, "setChannelListChanged in ");
        try {
            JSONArray args = new JSONArray();
            DtvkitGlueClient.getInstance().request("Hbbtv.HBBSetChannelListChange", args);
        } catch (Exception e) {
            Log.e(TAG, "setChannelListChanged = " + e.getMessage());
        }
        Log.i(TAG, "setChannelListChanged out ");
    }
    private void  notifyChannelChangedError(Channel channel, int errorState) {
        Log.i(TAG, "notifyChannelChangedError in ");
        for (TunerDelegateClient client : mTunerDelegateClientList) {
            client.onChannelChangeError(channel, errorState);
            Log.d(TAG, "notify onChannelChangeError ");
        }
        Log.i(TAG, "notifyChannelChangedError out ");
    }

    private void  notifyChannelChanged() {
        Log.i(TAG, "notifyChannelChanged in ");
         for (TunerDelegateClient client : mTunerDelegateClientList) {
            Log.d(TAG, "notifyChannelChanged mTunedChannelUri= " + mTunedChannelUri);
            if (null == mTunedChannelUri) {
                client.onChannelChanged(null);
            } else {
                client.onChannelChanged(AmlHbbTvTvContractUtils.getChannelByDetailsInfo(mContext.getContentResolver(), mTunedChannelUri));
            }
            Log.d(TAG, "notify onChannelChanged ");
        }

        Log.i(TAG, "notifyChannelChanged out ");
    }

    private void  notifyVideoAvailable() {
        Log.i(TAG, "notifyVideoAvailable in ");
        for (TunerDelegateClient client : mTunerDelegateClientList) {
            client.onVideoAvailable();
            Log.d(TAG, "notify onVideoAvailable ");
        }
        Log.i(TAG, "notifyVideoAvailable out ");
    }

    private void  notifyVideoUnavailable(int reason) {
        Log.i(TAG, "notifyVideoUnavailable in ");
        for (TunerDelegateClient client : mTunerDelegateClientList) {
            client.onVideoUnavailable​(reason);
            Log.d(TAG, "notify onVideoUnavailable reason= " + reason);
        }
        Log.i(TAG, "notifyVideoUnavailable out ");
    }

    private void  notifyTracksChanged() {
        Log.i(TAG, "notifyTracksChanged in ");
        synchronized (mTrackLock) {
            if (mAllTracksInfo != null && mAllTracksInfo.size() > 0) {
                for (TunerDelegateClient client : mTunerDelegateClientList) {
                    client.onTracksChanged(mAllTracksInfo, mSelectTrackInfo);
                    Log.d(TAG, "notify onTracksChanged tracks.size= " + mAllTracksInfo.size());
                }

            } else {
                Log.d(TAG, "notifyTracksChanged no track info");
            }
        }
        Log.i(TAG, "notifyTracksChanged out ");
    }

    private void  notifyTrackSelected(int type, String trackId) {
        Log.i(TAG, "notifyTrackSelected in ");
         for (TunerDelegateClient client : mTunerDelegateClientList) {
            client.onTrackSelected(type, trackId);
            Log.d(TAG, "notify onTrackSelected type= " + type + ", trackId=" + trackId);
        }
        Log.i(TAG, "notifyTrackSelected out ");
    }

    private void  notifyTunerStateChanged(int tunerState, int tunerError) {
        Log.i(TAG, "notifyTunerStateChanged in ");
        for (TunerDelegateClient client : mTunerDelegateClientList) {
            client.onTunerStateChanged(tunerState, tunerError);
            Log.d(TAG, "notify onTunerStateChanged tunerState = " + tunerState + ", tunerError =" + tunerError);
        }
        Log.i(TAG, "notifyTunerStateChanged out ");
    }

    private void updateSelectTrackInfo(int type, String trackId) {
        Log.i(TAG, "updateSelectTrackInfo in ");
        synchronized (mTrackLock) {
            if (mSelectTrackInfo == null) {
                 mSelectTrackInfo = new HashMap<>();
            }
            mSelectTrackInfo.put(type, trackId);
        }
        Log.i(TAG, "updateSelectTrackInfo out ");
    }

    private void setEncryptedForTrack(TvTrackInfo.Builder          track, boolean encrypted, Bundle bundle) {
        try {
                Class<?> cls = Class.forName("android.media.tv.TvTrackInfo$Builder");
                Method[] methods = cls.getMethods();
                for (Method method : methods) {
                    if (method.getName().equals("setEncrypted")) {
                        method.invoke(track, encrypted);
                        return;
                    }
                }
                bundle.putBoolean(TrackInfoExtra.TRACK_INFO_ENCRYPTED, encrypted);
            } catch(Exception e) {
                Log.d(TAG, " setEncryptedForTrack = " + e.getMessage());
        }
    }

    private void setSpokenSubtitleForTrack(TvTrackInfo.Builder            track, boolean ss, Bundle bundle) {
        try {
                Class<?> cls = Class.forName("android.media.tv.TvTrackInfo$Builder");
                Method[] methods = cls.getMethods();
                for (Method method : methods) {
                    if (method.getName().equals("setSpokenSubtitle")) {
                        method.invoke(track, ss);
                        return;
                    }
                }
                bundle.putBoolean(TrackInfoExtra.TRACK_INFO_SPOKEN_SUBTITLES, ss);
        } catch(Exception e) {
            Log.d(TAG, "setSpokenSubtitleForTrack = " + e.getMessage());
        }
    }

    private void setHardOfHearingForTrack(TvTrackInfo.Builder             track, boolean hi, Bundle bundle) {
        try {
                Class<?> cls = Class.forName("android.media.tv.TvTrackInfo$Builder");
                Method[] methods = cls.getMethods();
                for (Method method : methods) {
                    if (method.getName().equals("setHardOfHearing")) {
                        method.invoke(track, hi);
                        return;
                    }
                }
                bundle.putBoolean(TrackInfoExtra.TRACK_INFO_HEARING_IMPAIRED, hi);
        } catch(Exception e) {
            Log.d(TAG, " setHardOfHearingForTrack = " + e.getMessage());
        }
    }

    private void setAudioDescriptionForTrack(TvTrackInfo.Builder            track, boolean ad, Bundle bundle) {
        try {
                Class<?> cls = Class.forName("android.media.tv.TvTrackInfo$Builder");
                Method[] methods = cls.getMethods();
                for (Method method : methods) {
                    if (method.getName().equals("setAudioDescription")) {
                        method.invoke(track, ad);
                        return;
                    }
                }
                bundle.putBoolean(TrackInfoExtra.TRACK_INFO_AUDIO_DESCRIPTION, ad);
        } catch(Exception e) {
            Log.d(TAG, " setAudioDescriptionForTrack = " + e.getMessage());
        }
    }

    private void setEncodingForTrack(TvTrackInfo.Builder          track, String encoding, Bundle bundle) {
        try {
                Class<?> cls = Class.forName("android.media.tv.TvTrackInfo$Builder");
                Method[] methods = cls.getMethods();
                for (Method method : methods) {
                    if (method.getName().equals("setEncoding")) {
                        method.invoke(track, encoding);
                        return;
                    }
                }
                bundle.putString(TrackInfoExtra.TRACK_INFO_ENCODING, encoding);
        } catch(Exception e) {
            Log.d(TAG, " setEncodingForTrack = " + e.getMessage());
        }
    }

    private String getEncodingForTrack(TvTrackInfo track) {
        String encoding = "";
        try {
                Class<?> cls = Class.forName("android.media.tv.TvTrackInfo");
                 Method[] methods = cls.getMethods();
                for (Method method : methods) {
                    if (method.getName().equals("getEncoding")) {
                        encoding = (String)method.invoke(track);
                        return encoding;
                    }
                }
                encoding = track.getExtra().getString(TrackInfoExtra.TRACK_INFO_ENCODING);
            } catch(Exception e) {
            Log.d(TAG, " getEncodingForTrack = " + e.getMessage());
        }
        return encoding;
    }

    private List<TvTrackInfo> getAudioTrackList(JSONArray compArray) {
        Log.i(TAG, "getAudioTrackList in ");
        List<TvTrackInfo> audioTracks = new ArrayList<>();
        Log.i(TAG, "getAudioTrackList size= " + audioTracks.size());
        int i = 0;
        int id = 0;
        String trackId = null;
        try {
            Log.d(TAG, "getAudioTrackList compArray.length() = " + compArray.length());
            for (i = 0; i < compArray.length(); i++) {
                JSONObject tmpComp = compArray.getJSONObject(i);
                int componentType =  tmpComp.getInt("trackType");
                if (TvTrackInfo.TYPE_AUDIO == componentType) {
                    id = tmpComp.getInt("trackId");
                    trackId = getTrackId(id, componentType, tmpComp);
                    Log.d(TAG, "getAudioTrackList id = " + id + ", trackId = " + trackId);
                    TvTrackInfo.Builder track = new TvTrackInfo.Builder(TvTrackInfo.TYPE_AUDIO, trackId);
                    track.setLanguage(tmpComp.getString("language"));

                    Log.d(TAG, "getAudioTrackList message = " + tmpComp.getString("message"));
                    track.setDescription(tmpComp.getString("message"));
                    Bundle bundle = new Bundle();
                    setEncodingForTrack(track, tmpComp.getString("encoding"), bundle);
                    setEncryptedForTrack(track, tmpComp.getBoolean("encrypted"), bundle);
                    setSpokenSubtitleForTrack(track, tmpComp.getBoolean("ss"), bundle);
                    setAudioDescriptionForTrack(track, tmpComp.getBoolean("ad"), bundle);
                    bundle.putInt(TrackInfoExtra.TRACK_INFO_PID, tmpComp.getInt("pid"));
                    bundle.putBoolean(HbbTvConstantManager.KEY_TRACK_IS_PRIVATE, tmpComp.getBoolean("createdFromPrivateStream"));
                    bundle.putBoolean(HbbTvConstantManager.KEY_TRACK_HASCOMPONENTTAG, tmpComp.getBoolean("bHasCompTag"));
                    bundle.putInt(TrackInfoExtra.TRACK_INFO_COMPONENT_TAG, tmpComp.getInt("componentTag"));
                    bundle.putString(TrackInfoExtra.TRACK_INFO_LANGUAGE_BCP_47, tmpComp.getString("language_bcp_47"));
                    track.setExtra(bundle);
                    audioTracks.add(track.build());
                    if (tmpComp.getBoolean("selected")) {
                        updateSelectTrackInfo(TvTrackInfo.TYPE_AUDIO, trackId);
                    }
                }

            }
        } catch (Exception e) {
            Log.e(TAG, "getAllTracksInfo Exception = " + e.getMessage());
            return null;
        }
        Log.i(TAG, "getAudioTrackList size= " + audioTracks.size());
        Log.i(TAG, "getAudioTrackList out ");
        return audioTracks;

    }

    private List<TvTrackInfo> getVideoTrackList(JSONArray compArray) {
        Log.i(TAG, "getVideoTrackList in");
        List<TvTrackInfo> videoTracks = new ArrayList<>();
        int i = 0;
        int id = 0;
        String trackId = null;
        try {
            Log.d(TAG, "getVideoTrackList compArray.length() = " + compArray.length());
            for ( i = 0; i < compArray.length(); i++) {
                JSONObject tmpComp = compArray.getJSONObject(i);
                int componentType =  tmpComp.getInt("trackType");
                if (TvTrackInfo.TYPE_VIDEO == componentType) {
                    id = tmpComp.getInt("trackId");
                    trackId = getTrackId(id, componentType, tmpComp);
                    Log.d(TAG, "getVideoTrackList id = " + id + ", trackId = " + trackId);
                    TvTrackInfo.Builder track = new TvTrackInfo.Builder(TvTrackInfo.TYPE_VIDEO, trackId);
                    //track.setEncoding(tmpComp.getString("encoding"));
                    //track.setEncrypted(tmpComp.getBoolean("encrypted"));
                    if (tmpComp.getInt("aspectRatio") == AspectRatio.ASPECT_RATIO_4_3) {
                        track.setVideoPixelAspectRatio((float)1.33);
                    } else {
                        track.setVideoPixelAspectRatio((float)1.78);
                    }
                    track.setDescription("UDF");
                    Bundle bundle = new Bundle();
                    setEncodingForTrack(track, tmpComp.getString("encoding"), bundle);
                    setEncryptedForTrack(track, tmpComp.getBoolean("encrypted"), bundle);
                    bundle.putInt(TrackInfoExtra.TRACK_INFO_PID, tmpComp.getInt("pid"));
                    bundle.putBoolean(HbbTvConstantManager.KEY_TRACK_IS_PRIVATE, tmpComp.getBoolean("createdFromPrivateStream"));
                    bundle.putBoolean(HbbTvConstantManager.KEY_TRACK_HASCOMPONENTTAG, tmpComp.getBoolean("bHasCompTag"));
                    bundle.putInt(TrackInfoExtra.TRACK_INFO_COMPONENT_TAG, tmpComp.getInt("componentTag"));
                    track.setExtra(bundle);
                    videoTracks.add(track.build());
                    if (tmpComp.getBoolean("selected")) {
                        updateSelectTrackInfo(TvTrackInfo.TYPE_VIDEO, trackId);
                    }
                }

            }
        } catch (Exception e) {
            Log.e(TAG, "getAllTracksInfo Exception = " + e.getMessage());
            return null;
        }
        Log.i(TAG, "getVideoTrackList size= " + videoTracks.size());
        Log.i(TAG, "getVideoTrackList out ");
        return videoTracks;

    }

    private List<TvTrackInfo> getSubtitleTrackList(JSONArray compArray) {
        Log.i(TAG, "getSubtitleTrackList in ");
        List<TvTrackInfo> subtitleTracks = new ArrayList<>();
        int i = 0;
        int id = 0;
        String trackId = null;
        try {
            Log.d(TAG, "getSubtitleTrackList compArray.length() = " + compArray.length());
            for (i = 0; i < compArray.length(); i++) {
                JSONObject tmpComp = compArray.getJSONObject(i);
                int componentType =  tmpComp.getInt("trackType");
                if (TvTrackInfo.TYPE_SUBTITLE == componentType) {
                    id = tmpComp.getInt("trackId");
                    trackId = getTrackId(id, componentType, tmpComp);
                    Log.d(TAG, "getSubtitleTrackList id = " + id + " trackId = " + trackId);
                    TvTrackInfo.Builder track = new TvTrackInfo.Builder(TvTrackInfo.TYPE_SUBTITLE, trackId);
                    track.setLanguage(tmpComp.getString("language"));
                    //track.setEncoding(tmpComp.getString("encoding"));
                    //track.setEncrypted(tmpComp.getBoolean("encrypted"));
                    //track.setHardOfHearing(tmpComp.getBoolean("hearing_impaired"));
                    track.setDescription("UDF");
                    Bundle bundle = new Bundle();
                    setEncodingForTrack(track, tmpComp.getString("encoding"), bundle);
                    setEncryptedForTrack(track, tmpComp.getBoolean("encrypted"), bundle);
                    setHardOfHearingForTrack(track, tmpComp.getBoolean("hearing_impaired"), bundle);
                    bundle.putInt(TrackInfoExtra.TRACK_INFO_PID, tmpComp.getInt("pid"));
                    bundle.putBoolean(HbbTvConstantManager.KEY_TRACK_IS_PRIVATE, tmpComp.getBoolean("createdFromPrivateStream"));
                    bundle.putBoolean(HbbTvConstantManager.KEY_TRACK_HASCOMPONENTTAG, tmpComp.getBoolean("bHasCompTag"));
                    bundle.putInt(TrackInfoExtra.TRACK_INFO_COMPONENT_TAG, tmpComp.getInt("componentTag"));
                    bundle.putString(TrackInfoExtra.TRACK_INFO_LANGUAGE_BCP_47, tmpComp.getString("language_bcp_47"));
                    track.setExtra(bundle);
                    subtitleTracks.add(track.build());
                    if (tmpComp.getBoolean("selected")) {
                        updateSelectTrackInfo(TvTrackInfo.TYPE_SUBTITLE, trackId);
                    }
                }

            }
        } catch (Exception e) {
            Log.e(TAG, "getAllTracksInfo Exception = " + e.getMessage());
            return null;
        }
        Log.d(TAG, "getSubtitleTrackList size= " + subtitleTracks.size());
        Log.i(TAG, "getSubtitleTrackList out ");
        return subtitleTracks;
    }

    private TvTrackInfo getPrivateTracksInfo(int audioTrackSize, int videoTrackSize) {
        Log.i(TAG, "getPrivateTracksInfo in ");
        TvTrackInfo privateTrack = null;
        int componentTag =0XFF;
        int componentType = 0;
        int pid = 0;
        boolean bMatch = false;
        boolean hasPrivateTrack = false;
        boolean hasSelectPrivateTrack = false;
        int privateIdx = 0;
        String trackId = null;
        String oriTrackId = null;
        synchronized (mTrackLock) {
            if (mAllTracksInfo != null && mAllTracksInfo.size() > 0) {
                for (int i =0; i < mAllTracksInfo.size(); i++) {
                    if (mAllTracksInfo.get(i).getExtra().getBoolean(HbbTvConstantManager.KEY_TRACK_IS_PRIVATE, true)) {
                        Bundle bundle = new Bundle();
                        componentTag = mAllTracksInfo.get(i).getExtra().getInt(TrackInfoExtra.TRACK_INFO_COMPONENT_TAG);
                        bundle.putInt(TrackInfoExtra.TRACK_INFO_COMPONENT_TAG, componentTag);
                        pid = mAllTracksInfo.get(i).getExtra().getInt(TrackInfoExtra.TRACK_INFO_PID);
                        bundle.putInt(TrackInfoExtra.TRACK_INFO_PID, pid);
                        bundle.putBoolean(HbbTvConstantManager.KEY_TRACK_IS_PRIVATE, true);
                        bundle.putBoolean(HbbTvConstantManager.KEY_TRACK_HASCOMPONENTTAG, true);
                        componentType = privateTrack.getType();
                        oriTrackId = mAllTracksInfo.get(i).getId();
                        privateIdx = Integer.parseInt(transferTrackIdToOriginalTrackId(componentType, oriTrackId));
                        if ((componentType ==TvTrackInfo.TYPE_AUDIO) && (privateIdx != audioTrackSize)) {
                            //trackId = Integer.toString(audioTrackSize);
                            trackId = getTrackId(audioTrackSize, componentType, null);
                        } else if ((componentType ==TvTrackInfo.TYPE_VIDEO) && (privateIdx != videoTrackSize)) {
                            trackId = getTrackId(videoTrackSize, componentType, null);
                        } else {
                            trackId = oriTrackId;
                        }
                        TvTrackInfo.Builder tvTmpTrack = new TvTrackInfo.Builder(componentType, trackId);
                        tvTmpTrack.setDescription("UDF");
                        String encoding = getEncodingForTrack(mAllTracksInfo.get(i));
                        setEncodingForTrack(tvTmpTrack, encoding, bundle);
                        tvTmpTrack.setExtra(bundle);
                        privateTrack = tvTmpTrack.build();
                        hasPrivateTrack = true;
                        if (null != mSelectTrackInfo.get(componentType) && oriTrackId.equals(mSelectTrackInfo.get(componentType)))
                        {
                            hasSelectPrivateTrack = true;
                        }
                        break;
                    }
                }
            }
        }
        if (hasPrivateTrack) {
            try {
                JSONArray args = new JSONArray();
                JSONObject obj  = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetPrivateComponents", args);
                if (obj != null && obj.getBoolean("accepted"))
                {
                    JSONArray privateComps  = obj.getJSONArray("data");
                    for (int i = 0; i < privateComps.length(); i++)
                    {
                        JSONObject tmpComp = privateComps.getJSONObject(i);
                        Log.d(TAG, "getPrivateTracksInfo privateStream = " + tmpComp.toString());
                        if (tmpComp.getBoolean("bHasCompTag") &&
                            (componentTag == tmpComp.getInt("componentTag")) &&
                            (pid == tmpComp.getInt("pid"))) {
                            bMatch = true;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "getPrivateTracksInfo Exception = " + e.getMessage());
            }

        }
        Log.d(TAG, "getPrivateTracksInfo bMatch = " + bMatch + ", hasSelectPrivateTrack = " + hasSelectPrivateTrack + ", hasPrivateTrack =" + hasPrivateTrack);
        Log.i(TAG, "getPrivateTracksInfo out ");
        if (bMatch) {
            if (hasSelectPrivateTrack) {
                updateSelectTrackInfo(componentType, oriTrackId);
            }
            return privateTrack;
        } else {
            return null;
        }
    }

    private void checkTracksInfoUpdate(boolean forceUpdate) {
        Log.i(TAG, "checkTracksInfoUpdate in ");
        boolean updateTriggered = true;
        List<TvTrackInfo> tracks = getAllTracksInfo(updateTriggered);
        boolean bUpdate = false;

        synchronized (mTrackLock) {
            if (tracks != null && tracks.size() > 0 && mAllTracksInfo != null && mAllTracksInfo.size() > 0) {
                if (!tracks.equals(mAllTracksInfo)) {
                    mAllTracksInfo = tracks;
                    bUpdate = true;
                    Log.d(TAG, "checkTrackinfoUpdate update new tracks");
                }
            } else if (tracks != null && tracks.size() > 0 && mAllTracksInfo == null) {
                    mAllTracksInfo = tracks;
                    bUpdate = true;
                    Log.d(TAG, "checkTrackinfoUpdate sync to mAllTracksInfo");
            }
        }
        if (bUpdate || forceUpdate) {
            Log.d(TAG, "checkTracksInfoUpdate - notifyTracksChanged");
            notifyTracksChanged();
        }

        Log.i(TAG, "checkTracksInfoUpdate out ");
    }

    private List<TvTrackInfo> getAllTracksInfo(boolean updateTriggered) {
        Log.i(TAG, "getAllTracksInfo in ");
        List<TvTrackInfo> tracks = new ArrayList<>();
        JSONArray compArray;
         try {
            JSONArray args = new JSONArray();
            args.put(updateTriggered);
            JSONObject obj = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetAllComponents", args);
            if (obj != null && obj.getBoolean("accepted")) {
                compArray = obj.getJSONArray("data");
            } else {
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "getAllTracksInfo Exception = " + e.getMessage());
            return null;
        }
        int audioTracksSize = 0;
        int videoTrackSize = 0;
        List<TvTrackInfo> audioTracks = getAudioTrackList(compArray);
        List<TvTrackInfo> videoTracks = getVideoTrackList(compArray);
        List<TvTrackInfo> subtitleTracks = getSubtitleTrackList(compArray);
        if (audioTracks != null && (audioTracks.size() > 0))
        {
            tracks.addAll(audioTracks);
            audioTracksSize = audioTracks.size();
            Log.i(TAG, "getAllTracksInfo tracks.size() = " + tracks.size());
        }
        if (videoTracks != null && (videoTracks.size() > 0))
        {
            tracks.addAll(videoTracks);
            videoTrackSize = videoTracks.size();
            Log.i(TAG, "getAllTracksInfo tracks.size() = " + tracks.size());
        }
        if (subtitleTracks != null && (subtitleTracks.size() > 0))
        {
            tracks.addAll(subtitleTracks);
            Log.i(TAG, "getAllTracksInfo tracks.size() = " + tracks.size());
        }
        TvTrackInfo privateTrack = getPrivateTracksInfo(audioTracksSize, videoTrackSize);
        if (privateTrack != null)
        {
            tracks.add(privateTrack);
            Log.i(TAG, "getAllTracksInfo tracks.size() = " + tracks.size());
        }
        Log.i(TAG, "getAllTracksInfo out ");
        return tracks;
    }


    /**
    * @ingroup AmlTunerDelegateapi
    * @brief for monitor channel list update msg.
    * @param none.
    * @return none.
    */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String status = intent.getStringExtra(EpgSyncJobService.SYNC_STATUS);
            String updateReason = intent.getStringExtra(EpgSyncJobService.SYNC_REASON);
            if ((status.equals(EpgSyncJobService.SYNC_FINISHED)) &&
                ((updateReason.equals(EpgSyncJobService.SYNC_BYSCAN)) || mChannelListUpdate)) {
                Log.d(TAG, "onReceive  sendChannelListUpdateFinished updateReason = " + updateReason + ", mChannelListUpdate= " + mChannelListUpdate);
                //sendChannelListUpdateFinished();
                sendNotifyMsg(MSG.MSG_CHANNELLISTUPDATEFINISHED, 0, 0, null);
                mChannelListUpdate = false;
            }
        }
    };

    private void sendNotifyMsg(int what, int arg1, int arg2, Object obj) {
        if (mThreadHandler != null) {
            mThreadHandler.removeMessages(what);
            Message mess = mThreadHandler.obtainMessage(what, arg1, arg2, obj);
            boolean info = mThreadHandler.sendMessageDelayed(mess, 0);
            Log.d(TAG, "sendNotifyMsg status:" + info + ", msg.what = " + what);
        } else {
            Log.d(TAG, "sendNotifyMsg fail: msg.what = " + what);
        }
    }

    private void startMonitoringChannelListSync() {
        Log.i(TAG, "startMonitoringChannelListSync in ");
        LocalBroadcastManager.getInstance(mContext).registerReceiver(mReceiver,
                new IntentFilter(EpgSyncJobService.ACTION_SYNC_STATUS_CHANGED));
        Log.i(TAG, "startMonitoringChannelListSync out ");
    }

    private void stopMonitoringChannelListSync() {
        Log.i(TAG, "stopMonitoringChannelListSync in ");
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mReceiver);
        Log.i(TAG, "stopMonitoringChannelListSync out ");
    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief set the broadcast resource flag.
    * @param flag: true:broadcast resource; flase: broadband resource.
    * @return none.
    */
    public void setResourceOwnerByBrFlag(boolean flag) {
        try {
            JSONArray args = new JSONArray();
            args.put(flag);
            DtvkitGlueClient.getInstance().request("Hbbtv.HBBSetResourceOwnedByBr", args);
        } catch (Exception e) {
            Log.e(TAG, "setResourceOwnedByBr Exception = " + e.getMessage());
        }
        mOwnResourceByBr = flag;
        Log.i(TAG,"setResourceOwnedByBr flag = " + flag);
    }

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief check current resource is broadcast resource.
    * @param none
    * @return true:broadcast resource; flase: broadband resource..
    */
    public boolean checkResourceOwnedIsBr(){
        Log.i(TAG,"checkResourceOwnedIsBr mOwnResourceByBr = " + mOwnResourceByBr);
        return mOwnResourceByBr;
    }

    public void setHbbtvPreferencesManager(HbbtvPreferencesManager hbbtvPreferencesManager) {
        mPreferencesManager = hbbtvPreferencesManager;
    }

    private void syncMediaComponentsPreferences() {
        Log.d(TAG, " syncMediaComponentsPreferences in");
        if (mPreferencesManager != null) {
            mPreferencesManager.updateHbbTvMediaComponentsPreferences();
        }
        Log.d(TAG, " syncMediaComponentsPreferences out");
    }

    public void setSubtitleSwichFlagByHbbtv(boolean flag) {
        Log.i(TAG, " syncMediaComponentsPreferences in");
        if (mPreferencesManager != null) {
            mPreferencesManager.setSubtitleSwichFlagByHbbtv(flag);
        }
        Log.i(TAG, " syncMediaComponentsPreferences out");
    }

    private void initHandler() {
        Log.i(TAG, "initHandler in ");
        mThreadHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.d(TAG, "mThreadHandler handleMessage " + msg.what + " start");
                switch (msg.what) {
                    case MSG.MSG_CHANNELLISTUPDATEFINISHED: {
                        notifyChannelListChanged();
                        break;
                    }
                    case MSG.MSG_CHANNELCHANGED: {
                        notifyChannelChanged();
                        break;
                    }
                    case MSG.MSG_TRACKSCHANGED: {
                        checkTracksInfoUpdate(false);
                        break;
                    }
                    case MSG.MSG_TUNERSTATECHANGED: {
                        notifyTunerStateChanged(msg.arg1, msg.arg2);
                        break;
                    }
                    case MSG.MSG_VIDEOAVALIABLE: {
                        notifyVideoAvailable();
                        checkTracksInfoUpdate(msg.arg1>0 ? true:false);
                        break;
                    }
                    case MSG.MSG_VIDEOUNAVALIABLE: {
                        notifyVideoUnavailable(msg.arg1);
                        break;
                    }
                    case MSG.MSG_TRACKSELECTED: {
                        String trackId = null;
                        if (null != msg.obj) {
                            trackId = (String)msg.obj;
                        }
                        notifyTrackSelected(msg.arg1, trackId);
                        break;
                    }
                    case MSG.MSG_SELECTTRACK: {
                        String trackId = null;
                        if (null != msg.obj) {
                            trackId = (String)msg.obj;
                        }
                        playerSelectTrackById(msg.arg1, trackId);
                        break;
                    }

                    case MSG.MSG_SELECTPRIVATETRACK: {
                        String encoding = null;
                        if (null != msg.obj) {
                            encoding = (String)msg.obj;
                            playerPrivateTrack(msg.arg1, msg.arg2, encoding);
                        }
                        break;
                    }
                    case MSG.MSG_SUBTITLESTATUSCHANGED: {
                        syncMediaComponentsPreferences();
                        break;
                    }

                    case MSG.MSG_AUDIOLANGCHANGED: {
                        syncMediaComponentsPreferences();
                        break;
                    }

                    case MSG.MSG_WEAK_SIGANL: {
                         HbbTvManager.getInstance().terminateHbbTvApplicaitonWithoutSignal();
                         break;
                    }
                    case MSG.MSG_START_AV: {
                        HbbTvManager.getInstance().reloadApplicaition();
                        break;
                    }
                    default:
                        break;
                }
                Log.d(TAG, "mThreadHandler handleMessage " + msg.what + " over");
            }
        };
    }

    private boolean checkIsAudioChannel(){
        Log.i(TAG, "checkIsAudioChannel in ");
        //return mSession.isAudioOnlyChannel();
        return false;
    }

    private int getTunerStatus(){
        Log.d(TAG, "getTunerStatus mTunerState = " + mTunerState);
        return mTunerState ;
    }

    private void setTunerStatus(int state){
        mTunerState = state;
        Log.d(TAG, "setTunerStatus mTunerState = " + mTunerState);
    }


    private int getPlaystate(){
        Log.i(TAG, "getPlaystate need mPlayState = " + mPlayState);
        return mPlayState;
    }

    private void setPlaystate(int state){
        mPlayState = state;
        Log.i(TAG, "setPlaystate need mPlayState = " + mPlayState);
    }

    private int getVideoUnavaliableReason(int data){
        int reason;
        switch (data) {
            case VideoUnavaliableReason.HBBTV_VIDEO_INVALIADTUNERID:
                reason = TunerDelegateVideoUnavailableReason.NO_RESOURCES;
                break;
            case VideoUnavaliableReason.HBBTV_VIDEO_NOSIGNAL:
                reason = TunerDelegateVideoUnavailableReason.WEAK_SIGNAL;
                break;
            case VideoUnavaliableReason.HBBTV_VIDEO_NOTUNERRESOURCE:
                reason = TunerDelegateVideoUnavailableReason.NO_RESOURCES;
                break;
            case VideoUnavaliableReason.HBBTV_VIDEO_PR:
                reason = TunerDelegateVideoUnavailableReason.PARENTAL_RATING_LOCK;
                break;
            case VideoUnavaliableReason.HBBTV_VIDEO_SERVICESCRAMBLE:
                reason = TunerDelegateVideoUnavailableReason.ENCRYPTED_CHANNEL;
                break;
            case VideoUnavaliableReason.HBBTV_VIDEO_TUNING:
                reason = TunerDelegateVideoUnavailableReason.TUNING;
                break;
            default:
                reason = TunerDelegateVideoUnavailableReason.UNKNOWN;
                break;
        }
        Log.i(TAG, "getVideoUnavaliableReason  reason = " + reason + "input value = " + data);
        return reason;
    }


    /**
    * @ingroup AmlTunerDelegateapi
    * @brief play broadcast service when load new app.
    * @param none
    * @return
    */
    public void startAVByCheckResourceOwned() {
        Log.i(TAG, "startAVByCheckResourceOwned in");
        if (!checkResourceOwnedIsBr()) {
            setResourceOwnerByBrFlag(true);
            //tuneToCurrentChannel();
            mSession.onTune(mTunedChannelUri, null);
        }
        Log.i(TAG, "startAVByCheckResourceOwned out");
    }

    private final DtvkitGlueClient.SignalHandler mHandler = new DtvkitGlueClient.SignalHandler() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSignal(String signal, JSONObject data) {
            Log.i(TAG, "onSignal: " + signal + " : " + data.toString() );
            switch (signal) {
                case "hbbNotifyChannelChangedBegin":
                    if (getPlaystate() != PlayState.PLAYSTATE_CONNECTING) {
                        if (checkResourceOwnedIsBr()) {
                            setPlaystate(PlayState.PLAYSTATE_CONNECTING);
                        }
                    }
                    break;
                case "hbbNotifyChannelchangedSuccess":
                    sendNotifyMsg(MSG.MSG_CHANNELCHANGED, 0, 0, null);
                    if (checkResourceOwnedIsBr()) {
                        setPlaystate(PlayState.PLAYSTATE_CONNECTING);
                    }
                    break;
                case "hbbNotifyVideoUnavalible":
                    int reason = 0;
                    try {
                        reason = data.getInt("reason");
                    } catch (JSONException ignore) {
                    }
                    sendNotifyMsg(MSG.MSG_VIDEOUNAVALIABLE, getVideoUnavaliableReason(reason), 0, null);
                    if (checkResourceOwnedIsBr()) {
                        setPlaystate(PlayState.PLAYSTATE_CONNECTING);
                    }
                    break;
                case "hbbNotifyVideoAvalible":
                    if (checkResourceOwnedIsBr()) {
                        setPlaystate(PlayState.PLAYSTATE_PLAYING);
                        sendNotifyMsg(MSG.MSG_VIDEOAVALIABLE, 0, 0, null);
                    }
                    break;
                case "hbbNotifyTunerStatechanged":
                    int tunerstate = 0;
                    int tunererror = 0;
                    try {
                        tunerstate = data.getInt("tunerState");
                        tunererror = data.getInt("reason");
                    } catch (JSONException ignore) {
                    }
                    if (TunerStatus.TUNERSTATUS_LOCK == tunerstate) {
                        setTunerStatus(TunerStatus.TUNERSTATUS_LOCK);
                        sendNotifyMsg(MSG.MSG_TUNERSTATECHANGED, TunerState.LOCKED, TunerError.NO_ERROR, null);
                    } else if (TunerStatus.TUNERSTATUS_UNLOCK == tunerstate) {
                        setTunerStatus(TunerStatus.TUNERSTATUS_UNLOCK);
                        sendNotifyMsg(MSG.MSG_TUNERSTATECHANGED, TunerState.UNLOCKED, TunerError.UNKNOWN_ERROR, null);
                    }
                    break;
                case "DvbUpdatedChannelData":
                    sendNotifyMsg(MSG.MSG_TRACKSCHANGED, 0, 0, null);
                    break;
                case "DvbUpdatedChannel":
                    mChannelListUpdate = true;
                    break;
                case "hbbNotifySubtitleStatusUpdated":
                    sendNotifyMsg(MSG.MSG_SUBTITLESTATUSCHANGED, 0, 0, null);
                    break;
                case "hbbNotifyAudioLangUpdated":
                    sendNotifyMsg(MSG.MSG_AUDIOLANGCHANGED, 0, 0, null);
                    break;
                case "PlayerStatusChanged":
                    String state = null;
                     try {
                           state = data.getString("state");
                         } catch (JSONException ignore) {
                         }

                    if (state.equals("badsignal")) {
                        sendNotifyMsg(MSG.MSG_WEAK_SIGANL, 0, 0, null);
                        mWeakSignal = true;
                    }
                    if (state.equals("playing") && mWeakSignal) {
                        sendNotifyMsg(MSG.MSG_START_AV, 0, 0, null);
                        mWeakSignal = false;
                    }
                    break;
                default:
                    break;
            }
        }
    };

    /**
    * @ingroup AmlTunerDelegateapi
    * @brief Called by the HbbTvManager when the seesion is released.
    * @param none.
    * @return none.
    */
    public void release() {
        Log.i(TAG, "release in");
        stopMonitoringChannelListSync();
        DtvkitGlueClient.getInstance().unregisterSignalHandler(mHandler);
        DtvkitGlueClient.getInstance().setPidFilterListener(null);
        setResourceOwnerByBrFlag(true);
        setTuneChannelUri(null);
        sendNotifyMsg(MSG.MSG_CHANNELCHANGED, 0, 0, null);
        stopAllPidFilter();
        Log.i(TAG, "release out");

    }

    private class MSG  {
        public final static int MSG_CHANNELLISTUPDATEFINISHED = 1;
        public final static int MSG_CHANNELCHANGED = 2;
        public final static int MSG_TRACKSCHANGED = 3;
        public final static int MSG_TUNERSTATECHANGED = 4;
        public final static int MSG_VIDEOAVALIABLE = 5;
        public final static int MSG_VIDEOUNAVALIABLE = 6;
        public final static int MSG_TRACKSELECTED = 7;
        public final static int MSG_SELECTTRACK = 8;
        public final static int MSG_SELECTPRIVATETRACK = 9;
        public final static int MSG_CHANNELCHANGED_BEGIN = 10;
        public final static int MSG_SUBTITLESTATUSCHANGED = 11;
        public final static int MSG_AUDIOLANGCHANGED = 12;
        public final static int MSG_WEAK_SIGANL = 13;
        public final static int MSG_START_AV = 14;
    }

    private class PlayState  {
        public final static int PLAYSTATE_INIT = 0;
        public final static int PLAYSTATE_CONNECTING = 1;
        public final static int PLAYSTATE_PLAYING = 2;
        public final static int PLAYSTATE_STOP = 3;
    }

    private class TunerStatus  {
        public final static int TUNERSTATUS_INIT = 0;
        public final static int TUNERSTATUS_LOCK = 1;
        public final static int TUNERSTATUS_UNLOCK = 2;
    }


    private class VideoUnavaliableReason {
        public final static int HBBTV_VIDEO_UNKNOWNERROR = 0;
        public final static int HBBTV_VIDEO_NOTUNERRESOURCE = 1;
        public final static int HBBTV_VIDEO_INVALIADTUNERID = 2;
        public final static int HBBTV_VIDEO_INVALIADSERVICE = 3;
        public final static int HBBTV_VIDEO_NOSIGNAL = 4;
        public final static int HBBTV_VIDEO_SERVICESCRAMBLE = 5;
        public final static int HBBTV_VIDEO_PR = 6;
        public final static int HBBTV_VIDEO_HARDWAREERROR = 7;
        public final static int HBBTV_VIDEO_UNSUPPORTEDCODEC = 8;
        public final static int HBBTV_VIDEO_NODATA = 9;
        public final static int HBBTV_VIDEO_TUNING = 10;
    }

    private class  AspectRatio {
        public final static int ASPECT_RATIO_4_3 = 0;
        public final static int ASPECT_RATIO_16_9 = 1;
        public final static int ASPECT_UNDEFINED = 255;
    }

    private class  ChannelChangeQuietMode {
        public final static int MODE_NORMAL = 0;
        public final static int MODE_NORMAL_NO_UI_DISPLAY = 1;
        public final static int MODE_QUIET = 2;
    }

}
