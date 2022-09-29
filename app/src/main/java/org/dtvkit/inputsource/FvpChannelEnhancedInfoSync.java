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


public class FvpChannelEnhancedInfoSync implements Runnable {
    private static final String TAG = "FvpChannelEnhancedInfoSync";
    public static final String DTVKIT_INPUTID = "com.droidlogic.dtvkit.inputsource/.DtvkitTvInput/HW19";
    private static final boolean DEBUG = true;
    private static final int BATCH_OPERATION_COUNT = 50;
    private Context mContext = null;

    public FvpChannelEnhancedInfoSync(Context context) {
        mContext = context;
    }

    @Override
    public void run() {
        ArrayMap<String, Long> channelsMap = new ArrayMap<>();
        List<Channel> channels = new ArrayList<>();
        int needUpdateChannelNumber = parseDataToChannelList(getChannelEnhancedInfo(), channels);
        if (0 != needUpdateChannelNumber) {
            cacheProviderChannel(channelsMap);
            updateChannelProvider(channels, channelsMap);
        }
    }

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

    private int parseDataToChannelList (JSONArray channelEnhancedArray,  List<Channel> channels) {
        int needUpdateChannelNumber = 0;
        if (null == channelEnhancedArray) {
            Log.e(TAG, "parseDataToChannelList can't get channel enhanced data");
            return needUpdateChannelNumber;
        }

        try {
            for (int i = 0; i < channelEnhancedArray.length(); i++) {
                JSONObject service = channelEnhancedArray.getJSONObject(i);
                int newtworkId = service.getInt("Onid");
                int streamId = service.getInt("Tsid");
                int serviceId = service.getInt("Sid");
                String serviceUrl = service.getString("SeviceURL");
                String logoUrl = service.getString("MediaUri");
                channels.add(new Channel.Builder()
                        .setOriginalNetworkId(newtworkId)
                        .setTransportStreamId(streamId)
                        .setServiceId(serviceId)
                        .setAppLinkIntentUri(serviceUrl)
                        .setAppLinkIconUri(logoUrl)
                        .build());
                if (DEBUG) Log.d(TAG, "Channel nid =" + newtworkId + "|Tsid = " + streamId + "|Sid = " + serviceId
                    + "|serviceUrl = " + serviceUrl + "|logoUrl = " + logoUrl);
            }
/*
            //Test
            channels.add(new Channel.Builder()
                .setOriginalNetworkId(1)
                .setTransportStreamId(65283)
                .setServiceId(28703)
                .setAppLinkIntentUri("hbbtv test uri")
                .setAppLinkIconUri("logo logo test")
                .build());
            channels.add(new Channel.Builder()
                .setOriginalNetworkId(8945)
                .setTransportStreamId(1021)
                .setServiceId(5060)
                .setAppLinkIntentUri("8945 test")
                .setAppLinkIconUri("5060 test")
                .build());
            channels.add(new Channel.Builder()
                .setOriginalNetworkId(9018)
                .setTransportStreamId(4168)
                .setServiceId(4168)
                .setAppLinkIntentUri("9018 test")
                .setAppLinkIconUri("4168 test")
                .build());
*/
        } catch (Exception e) {
            Log.e(TAG, "parseToChannelList Exception = " + e.getMessage());
        }
        needUpdateChannelNumber = channels.size();
        Log.d(TAG, "parseDataToChannelList needUpdateChannelNumber = " + needUpdateChannelNumber);
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
                    + "|link uri = " + channel.getAppLinkIntentUri() + "|link icon uri = " + channel.getAppLinkIconUri());
            }
            if (null != channelId) {
                values.put(Channels._ID, channelId);
                values.put(TvContract.Channels.COLUMN_APP_LINK_INTENT_URI, channel.getAppLinkIntentUri());
                values.put(TvContract.Channels.COLUMN_APP_LINK_ICON_URI, channel.getAppLinkIconUri());
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

