package com.droidlogic.dtvkit.inputsource;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import android.util.ArrayMap;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.lang.Runnable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.droidlogic.dtvkit.companionlibrary.model.Channel;
import org.droidlogic.dtvkit.DtvkitGlueClient;
import com.droidlogic.dtvkit.inputsource.fvp.DtvkitFvp;


public class FvpChannelEnhancedInfoSync implements Runnable {
    private static final String TAG = "FvpChannelEnhancedInfoSync";
    private static final boolean DEBUG = true;

    public static final String DTVKIT_INPUTID = "com.droidlogic.dtvkit.inputsource/.DtvkitTvInput/HW19";

    public static final int FVP_SYNC_CHANNEL_ENCHANCE_INFO = 0;
    public static final int FVP_SYNC_IP_CHANNEL_INFO = 1;
    public static final int FVP_SYNC_ALL_INFO = 2;

    private static final int BATCH_OPERATION_COUNT = 50;
    private Context mContext = null;
    private int mSyncType = FVP_SYNC_CHANNEL_ENCHANCE_INFO;

    public FvpChannelEnhancedInfoSync(Context context, int syncType) {
        mContext = context;
        mSyncType = syncType;
    }

    @Override
    public void run() {
        ArrayMap<String, Long> channelsMap = new ArrayMap<>();
        List<Channel> channels = new ArrayList<>();
        int needUpdateChannelNumber = 0;

        if (FVP_SYNC_CHANNEL_ENCHANCE_INFO == mSyncType) {
            needUpdateChannelNumber = parseChannelEnhancedDataToChannelList(DtvkitFvp.getInstance().getChannelEnhancedInfo(), channels);
        } else if (FVP_SYNC_IP_CHANNEL_INFO == mSyncType) {
            needUpdateChannelNumber = parseIpChannelEnhancedDataToChannelList(DtvkitFvp.getInstance().getIpChannelEnhancedInfo(), channels);
        } else if (FVP_SYNC_ALL_INFO == mSyncType) {
            needUpdateChannelNumber = parseChannelEnhancedDataToChannelList(DtvkitFvp.getInstance().getChannelEnhancedInfo(), channels);
            needUpdateChannelNumber += parseIpChannelEnhancedDataToChannelList(DtvkitFvp.getInstance().getIpChannelEnhancedInfo(), channels);
        }

        if (0 != needUpdateChannelNumber) {
            cacheProviderChannel(channelsMap);
            updateChannelProvider(channels, channelsMap);
        }
    }
/*
    private JSONArray getChannelEnhancedInfo () {
        JSONArray channelEnhancedArray = null;
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Fvp.FvpGetServiceInfo", new JSONArray());
            channelEnhancedArray = obj.getJSONArray("data");
            Log.d(TAG, "Fvp service list number =" + channelEnhancedArray.length());
        } catch (Exception e) {
            Log.e(TAG, "getChannels Exception = " + e.getMessage());
        }
        return channelEnhancedArray;
    }
*/
    private int parseChannelEnhancedDataToChannelList (JSONArray channelEnhancedArray,  List<Channel> channels) {
        int needUpdateChannelNumber = 0;
        if (null == channelEnhancedArray) {
            Log.e(TAG, "parseChannelEnhancedDataToChannelList can't get channel enhanced data");
            return needUpdateChannelNumber;
        }

        try {

            for (int i = 0; i < channelEnhancedArray.length(); i++) {
                JSONObject service = channelEnhancedArray.getJSONObject(i);
                int networkId = service.getInt("Onid");
                int streamId = service.getInt("Tsid");
                int serviceId = service.getInt("Sid");
                int onDemand = service.getInt("EnhancedOnDemand");
                String serviceUrl = service.getString("SeviceURL");
                String logoUrl = service.getString("MediaUri");
                channels.add(new Channel.Builder()
                        .setOriginalNetworkId(networkId)
                        .setTransportStreamId(streamId)
                        .setChannelOnDemand(onDemand)
                        .setServiceId(serviceId)
                        .setAppLinkIconUri(logoUrl)
                        .build());
                if (DEBUG) Log.d(TAG, "Channel nid =" + networkId + "|Tsid = " + streamId + "|Sid = " + serviceId + "|onDemand = " + onDemand
                    + "|serviceUrl = " + serviceUrl + "|logoUrl = " + logoUrl);
            }
/*
            //Test
            channels.add(new Channel.Builder()
                .setOriginalNetworkId(8468)
                .setTransportStreamId(61697)
                .setServiceId(770)
                .setAppLinkIntentUri("hbbtv test uri")
                .setAppLinkIconUri("logo logo test")
                .setChannelOnDemand(100)
                .build());
            channels.add(new Channel.Builder()
                .setOriginalNetworkId(8468)
                .setTransportStreamId(61697)
                .setServiceId(771)
                .setAppLinkIntentUri("8945 test")
                .setAppLinkIconUri("5060 test")
                .setChannelOnDemand(200)
                .build());
            channels.add(new Channel.Builder()
                .setOriginalNetworkId(8468)
                .setTransportStreamId(61697)
                .setServiceId(773)
                .setAppLinkIntentUri("9018 test")
                .setAppLinkIconUri("4168 test")
                .setChannelOnDemand(300)
                .build());
*/
        } catch (Exception e) {
            Log.e(TAG, "parseToChannelList Exception = " + e.getMessage());
        }
        needUpdateChannelNumber = channels.size();
        Log.d(TAG, "parseChannelEnhancedDataToChannelList needUpdateChannelNumber = " + needUpdateChannelNumber);
        return needUpdateChannelNumber;
    }

    private int parseIpChannelEnhancedDataToChannelList (JSONArray ipChannelEnhancedArray,  List<Channel> channels) {
        int needUpdateChannelNumber = 0;
        if (null == ipChannelEnhancedArray) {
            Log.e(TAG, "parseIpChannelEnhancedDataToChannelList can't get channel enhanced data");
            return needUpdateChannelNumber;
        }

        try {

            for (int i = 0; i < ipChannelEnhancedArray.length(); i++) {
                JSONObject service = ipChannelEnhancedArray.getJSONObject(i);
                int networkId = service.getInt("Onid");
                int streamId = service.getInt("Tsid");
                int serviceId = service.getInt("Sid");
                String serviceUrl = service.getString("MediaUri");
                channels.add(new Channel.Builder()
                        .setOriginalNetworkId(networkId)
                        .setTransportStreamId(streamId)
                        .setServiceId(serviceId)
                        .setAppLinkIntentUri(serviceUrl)
                        .build());
                if (DEBUG) Log.d(TAG, "IP Channel nid =" + networkId + "|Tsid = " + streamId + "|Sid = " + serviceId
                    + "|serviceUrl = " + serviceUrl);
            }
/*
            //Test
            channels.add(new Channel.Builder()
                .setOriginalNetworkId(8468)
                .setTransportStreamId(61697)
                .setServiceId(1030)
                .setAppLinkIntentUri("//hbbtv ip channel tune url")
                .build());
            channels.add(new Channel.Builder()
                .setOriginalNetworkId(8468)
                .setTransportStreamId(61697)
                .setServiceId(1571)
                .setAppLinkIntentUri("//hbbtv ip channel tune url")
                .build());
            channels.add(new Channel.Builder()
                .setOriginalNetworkId(8468)
                .setTransportStreamId(61697)
                .setServiceId(1742)
                .setAppLinkIntentUri("//hbbtv ip channel tune url")
                .build());
*/
        } catch (Exception e) {
            Log.e(TAG, "parseToChannelList Exception = " + e.getMessage());
        }
        needUpdateChannelNumber = channels.size();
        Log.d(TAG, "parseIpChannelEnhancedDataToChannelList needUpdateChannelNumber = " + needUpdateChannelNumber);
        return needUpdateChannelNumber;
    }

    private void cacheProviderChannel (ArrayMap<String, Long> channelsMap) {
        Uri channelsUri = TvContract.buildChannelsUriForInput(DTVKIT_INPUTID);
        String[] projection = {Channels._ID, Channels.COLUMN_TYPE, Channels.COLUMN_ORIGINAL_NETWORK_ID, Channels.COLUMN_TRANSPORT_STREAM_ID, Channels.COLUMN_SERVICE_ID};
        String selection = TvContract.Channels.COLUMN_TYPE + " =? OR " + TvContract.Channels.COLUMN_TYPE + " =? ";
        String [] selectionArgs = {"TYPE_DVB_T", "TYPE_DVB_T2"};

        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(channelsUri, projection, selection, selectionArgs, null);
            while (cursor != null && cursor.moveToNext()) {
                long rowId = cursor.getLong(0);
                String uniqueStr = getUniqueStrForChannel(cursor.getInt(2), cursor.getInt(4));//only use nid + service id check unique channel
                channelsMap.put(uniqueStr, rowId);
                if (DEBUG) Log.d(TAG, "uniqueStr =" + uniqueStr + "|rowId = " + rowId);
            }
        } catch (Exception e) {
            Log.e(TAG, "cacheProviderChannel Exception = " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void updateChannelProvider(List<Channel> channels, ArrayMap<String, Long> channelsMap) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ContentValues values = new ContentValues();
        String channelUniqueStr = null;
        for (Channel channel : channels) {
            channelUniqueStr = getUniqueStrForChannel(channel.getOriginalNetworkId(), channel.getServiceId());
            Long channelId = channelsMap.get(channelUniqueStr);
            if (DEBUG) {
                Log.d(TAG, "updateChannelProvider channelUniqueStr = " + channelUniqueStr + "|channelId = " + channelId
                    + "|link uri = " + channel.getAppLinkIntentUri() + "|link icon uri = " + channel.getAppLinkIconUri() + "|onDemand = " +channel.getOnDemand());
            }
            if (null != channelId) {
                values.put(Channels._ID, channelId);
                values.put(TvContract.Channels.COLUMN_APP_LINK_INTENT_URI, channel.getAppLinkIntentUri());
                values.put(TvContract.Channels.COLUMN_APP_LINK_ICON_URI, channel.getAppLinkIconUri());
                values.put(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1, channel.getOnDemand());
                ops.add(ContentProviderOperation.newUpdate(
                                    TvContract.buildChannelUri(channelId))
                                    .withValues(values)
                                    .build());
                channelsMap.remove(channelUniqueStr);
                values.clear();
            }
        }

        ContentResolver resolver = mContext.getContentResolver();
        for (int i = 0; i < ops.size(); i += BATCH_OPERATION_COUNT) {
            int toIndex =
                    (i + BATCH_OPERATION_COUNT) > ops.size()
                            ? ops.size()
                            : (i + BATCH_OPERATION_COUNT);
            ArrayList<ContentProviderOperation> batchOps =
                    new ArrayList<>(ops.subList(i, toIndex));
            if (DEBUG) {
                Log.d(TAG, "updateChannels from fromIndex " + i + " to " + toIndex);
            }
            try {
                resolver.applyBatch(TvContract.AUTHORITY, batchOps);
            } catch (Exception e) {
                Log.e(TAG, "updateChannels Failed = " + e.getMessage());
            }
        }
        ops.clear();
        //debugShowInfo();
    }

    private String getUniqueStrForChannel(int originalNetworkId, int serviceId) {
        String uniqueStr = String.valueOf(originalNetworkId)  + "-" + String.valueOf(serviceId);
        return uniqueStr;
    }
    private void debugShowInfo() {
        Uri channelsUri = TvContract.buildChannelsUriForInput(DTVKIT_INPUTID);
            String[] projection = {Channels._ID, Channels.COLUMN_TYPE, Channels.COLUMN_ORIGINAL_NETWORK_ID, Channels.COLUMN_SERVICE_ID, Channels.COLUMN_APP_LINK_INTENT_URI, Channels.COLUMN_APP_LINK_ICON_URI};
            String selection = TvContract.Channels.COLUMN_TYPE + " =? OR " + TvContract.Channels.COLUMN_TYPE + " =? ";
            String [] selectionArgs = {"TYPE_DVB_T", "TYPE_DVB_T2"};
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = null;
        try {
            cursor = resolver.query(channelsUri, projection, selection, selectionArgs, null);
            while (cursor != null && cursor.moveToNext()) {
                long rowId = cursor.getLong(0);
                int netWorkId = cursor.getInt(2);
                int serviceId = cursor.getInt(3);
                String SeviceURL = cursor.getString(4);
                String MediaUri = cursor.getString(5);
                String uniqueStr = getUniqueStrForChannel(netWorkId, serviceId);
                if (DEBUG) Log.d(TAG, "debug for query uniqueStr =" + uniqueStr + "|SeviceURL = " + SeviceURL + "|MediaUri = " + MediaUri);
            }
        } catch (Exception e) {
            Log.e(TAG, "cacheProviderChannel Exception = " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}

