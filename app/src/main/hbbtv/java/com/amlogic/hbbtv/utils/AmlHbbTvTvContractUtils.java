package com.amlogic.hbbtv.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.net.Uri;
import android.util.Log;

import com.vewd.core.shared.Channel;
import org.droidlogic.dtvkit.DtvkitGlueClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import com.droidlogic.dtvkit.companionlibrary.utils.TvContractUtils;
import com.droidlogic.dtvkit.companionlibrary.model.InternalProviderData;

/**
 * @ingroup hbbtvutilsapi
 * @defgroup amlhbbtvtvcontractutilsapi Aml-HbbTv-TvContract-Utils-API
 */
public class AmlHbbTvTvContractUtils {
    private static final String TAG = "AmlHbbTvTvContractUtils";
    private static final int CCIDLEN = 5;
    private static final String[] PROJECTION = {
        TvContract.Channels._ID,
        TvContract.Channels.COLUMN_TYPE,
        TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID,
        TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID,
        TvContract.Channels.COLUMN_SERVICE_ID,
        TvContract.Channels.COLUMN_DISPLAY_NAME,
        TvContract.Channels.COLUMN_DISPLAY_NUMBER,
        TvContract.Channels.COLUMN_SERVICE_TYPE,
        TvContract.Channels.COLUMN_DESCRIPTION,
        TvContract.Channels.COLUMN_LOCKED,
        TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA,
    };

   /**
    * @ingroup amlhbbtvtvcontractutilsapi.
    * @brief get channel uris with the tv input id
    * @param contentResolver  The content resolver which used to query the provider
    * @param inputId  The Tv input id
    * @return List<Uri>  The Uri arrsyList
    */
    public static List<Uri> getChannelUrisForInput(ContentResolver contentResolver, String inputId) {
        Log.i(TAG, "getChannelUrisForInput in");
        List<Uri> channels = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(TvContract.buildChannelsUriForInput(inputId),
                    new String[] {
                            TvContract.Channels._ID,
                    },
                    null, null, ContentResolverUtils.CHANNELS_ORDER_BY);
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    channels.add(TvContract.buildChannelUri(cursor.getLong(0)));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getChannelUrisForInput Unable to get channels for input " + inputId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.i(TAG, "getChannelUrisForInput out");
        return channels;
    }


    /**
     * @ingroup amlhbbtvtvcontractutilsapi.
     * @brief get channel uri with the tv input id and ccid
     * @param contentResolver  The content resolver which used to query the provider
     * @param inputId  The Tv input id
     * @param ccid  The unique identify of the channel
     * @return Uri  The Uri that points to a specific channel
     */
    public static Uri getChannelUriForCcid(ContentResolver contentResolver, String inputId, String ccid) {
        Log.i(TAG, "getChannelUriForCcid in");
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(TvContract.buildChannelsUriForInput(inputId),
                    new String[] {TvContract.Channels._ID},
                    TvContract.Channels.COLUMN_INTERNAL_PROVIDER_ID + " = ?", new String[] {ccid},
                    null);
            if (cursor != null && cursor.getCount() > 0) {
                cursor.moveToNext();
                return TvContract.buildChannelUri(cursor.getLong(0));
            }
        } catch (Exception e) {
            Log.e(TAG, "getChannelUriForCcid Unable to get channels for input " + inputId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.i(TAG, "getChannelUriForCcid out");
        return null;
    }

    /**
     * @ingroup amlhbbtvtvcontractutilsapi.
     * @brief get channel  with the channel uri
     * @param contentResolver  The content resolver which used to query the provider
     * @param channelUri  The Uri that points to a specific channel
     * @return Channel   The channel which the uri pointed to
     */
    public static Channel getChannel(ContentResolver contentResolver, Uri channelUri) {
        Log.i(TAG, "getChannel in");
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(channelUri, Channel.PROJECTION, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToNext();
            return new Channel(ContentResolverUtils.cursorRowToContentValues(cursor));
        } catch (Exception e) {
            Log.e(TAG, "getChannel Error parsing channel " + channelUri);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static int getNetworkId(int originalNetworkId, int transportStreamId, int serviceId) {
        Log.i(TAG, "getNetworkId in");
        int networkId = 0;

        try {
            JSONArray args = new JSONArray();
            args.put(originalNetworkId);
            args.put(transportStreamId);
            args.put(serviceId);

            JSONObject jsonObj = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetChannelByTriplet", args);
            JSONObject data = null;
            if (jsonObj != null) {
                data = (JSONObject)jsonObj.get("data");
            }
            if (data == null || data.length() == 0) {
                networkId = 0;
            } else {
                networkId = (int)(data.get("nid"));
            }
        } catch (Exception e) {
            Log.e(TAG, "getNetworkId = " + e.getMessage());
            return networkId;
        }
        Log.i(TAG, "getNetworkId out networkId = " + networkId);
        return networkId;

    }

    private static String makeccid(int originalNetworkId, int transportStreamId, int serviceId, int broadcastChannelNumber)
    {
        String ccid = "ccid";
        String result = null;
        ccid = ccid + "." + String.valueOf(originalNetworkId) + "." + String.valueOf(transportStreamId) + "." + String.valueOf(serviceId) +"." + String.valueOf(broadcastChannelNumber);
        Log.i(TAG, "makeccid: ccid = " + ccid);
        return ccid;
    }

    /**
     * @ingroup amlhbbtvtvcontractutilsapi.
     * @brief get channel with the channel uri which includes more info, such as ccid
     * @param contentResolver  The content resolver which used to query the provider
     * @param channelUri  The Uri that points to a specific channel
     * @return Channel   The channel which the uri pointed to
     */
    public static Channel getChannelByDetailsInfo(ContentResolver contentResolver, Uri channelUri) {
        Log.i(TAG, "getChannelByDetailsInfo in");
        Cursor cursor = null;
        if (null == channelUri) {
            Log.w(TAG, "getChannelByDetailsInfo channelUri is invalid");
            return null;
        }
        try {
            cursor = contentResolver.query(channelUri, PROJECTION, null, null, null);
            if (cursor == null || cursor.getCount() == 0) {
                return null;
            }
            cursor.moveToNext();
            long rowId = cursor.getLong(cursor.getColumnIndex(TvContract.Channels._ID));
            String channelType = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_TYPE));
            int originalNetworkId = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID));
            int transportStreamId = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID));
            int serviceId = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_ID));
            int networkId = getNetworkId(originalNetworkId, transportStreamId, serviceId);
            String name = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NAME));
            String shortServiceName = "";
            String number = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER));
            int broadcastChannelNumber =  Integer.valueOf(number);
            byte[] dsd = new byte[0];
            String serviceType = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_TYPE));
            String broadcastSystemType = channelType;
            String description = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DESCRIPTION));
            boolean isLocked = false;//cursor.getBoolean(cursor.getColumnIndex(TvContract.Channels.COLUMN_LOCKED));
            InternalProviderData internalProviderData = null;
            byte[] internalProviderByteData = null;
            int sqi = 0;
            boolean isHd = false;
            boolean hidden = false;
            boolean numericSelectable = true;
            boolean isEncrypted = false;
            String epgServiceCcid = "";
            int type = cursor.getType(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA));
            int frequency = 0;
            String ciNumber = " ";
            String rawDisplayNumber = null;
            String ccid = null;

            if (type == Cursor.FIELD_TYPE_BLOB) {
                internalProviderByteData = cursor.getBlob(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA));
                if (internalProviderByteData != null) {
                    internalProviderData = new InternalProviderData(internalProviderByteData);
                }
            } else {
                Log.d(TAG, "getChannelByDetailsInfo COLUMN_INTERNAL_PROVIDER_DATA other type");
            }
            if (internalProviderData != null) {
                Log.d(TAG, "getChannelByDetailsInfo internalProviderData = " + internalProviderData.toString());
                frequency = Integer.valueOf((String)internalProviderData.get("frequency"));
                ciNumber = (String)internalProviderData.get("ci_number");
                if (null == ciNumber) {
                    ciNumber = "none";
                }
                rawDisplayNumber = (String)internalProviderData.get("raw_displaynumber");
            }

            ccid = makeccid(originalNetworkId, transportStreamId, serviceId, broadcastChannelNumber);
            //ccid = TvContractUtils.getUniqueStrForChannel(internalProviderData, channelType, originalNetworkId, transportStreamId, serviceId, frequency, ciNumber, rawDisplayNumber);
            if (ccid == null) {
                Log.w(TAG, "getChannelByDetailsInfo ccid is null");
                return null;
            }
            //dsd = intToByteArray(frequency, 4);
            Log.i(TAG, "getChannelByDetailsInfo channel info details: ccid= " + ccid + ", channelnumber= " + broadcastChannelNumber + ", name = " + name + ", frequency = " + frequency
             + ", originalNetworkId = " + originalNetworkId + ", transportStreamId= " + transportStreamId + ", serviceId =" + serviceId + ", channelType" + channelType);
            Channel.Builder tmpBuilder = new Channel.Builder();
            tmpBuilder.withCcid(ccid);
            //tmpBuilder.withColumnDisplayNumber(broadcastChannelNumber);
            tmpBuilder.withColumnDisplayName(name);
            tmpBuilder.withDescription(description);
            tmpBuilder.withOriginalNetworkId(originalNetworkId);
            tmpBuilder.withServiceType(serviceType);
            tmpBuilder.withBroadcastSystemType(broadcastSystemType);
            tmpBuilder.withTransportStreamId(transportStreamId);
            tmpBuilder.withServiceId(serviceId);
            tmpBuilder.withHiddenCollumn(hidden);
            tmpBuilder.withColumnSearchable(numericSelectable);
            tmpBuilder.withLockedColumn(isLocked);
            //tmpBuilder.withVideoDefinition(isHd);
            tmpBuilder.withBroadcastChannelNumber(broadcastChannelNumber);
            tmpBuilder.withShortServiceName(shortServiceName);
            tmpBuilder.withDsd(dsd);
            tmpBuilder.withNetworkId(networkId);
            tmpBuilder.withSqi(sqi);
            tmpBuilder.withIsEncrypted(isEncrypted);
            tmpBuilder.withEpgServiceCcid(epgServiceCcid);
            return tmpBuilder.build();

           /* return new Channel(ccid,
               number,
               broadcastChannelNumber,
               name,
               shortServiceName,
               description,
               dsd,
               serviceType,
               broadcastSystemType,
               networkId,
               originalNetworkId,
               transportStreamId,
               serviceId,
               sqi,
               isHd,
               hidden,
               numericSelectable,
               isEncrypted,
               epgServiceCcid);*/
        } catch (Exception e) {
            Log.e(TAG, "getChannelByDetailsInfo Error parsing channel " + channelUri);
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * @ingroup amlhbbtvtvcontractutilsapi.
     * @brief get channel uri with the unique id(inputId and ccid)
     * @param contentResolver  The content resolver which used to query the provider
     * @param inputId  The Tv input id
     * @param ccid  The unique identify of the channel
     * @return Uri   The channel which the uri pointed to
     */
    public static Uri getChannelUriByUniqueId(ContentResolver contentResolver, String inputId, String ccid) {
        Log.i(TAG, "getChannelUriByUniqueId in");
        if (null == ccid) {
            Log.w(TAG, "getChannelUriByUniqueId ccid is null");
            return null;
        }
        Log.d(TAG, "getChannelUriByUniqueId ccid = " + ccid);

        //final String DELIMITER = "-";
        String[] subs = ccid.split("\\.");//(DELIMITER);
        if (subs.length != CCIDLEN) {
            Log.d(TAG, "getChannelUriByUniqueId subs.length = " + subs.length);
            return null;
        }
        String chType = subs[0];

        int onid = Integer.valueOf(subs[1]);
        int tsid = Integer.valueOf(subs[2]);
        int sid = Integer.valueOf(subs[3]);
        int channelNo = Integer.valueOf(subs[4]);
        Log.d(TAG, "getChannelUriByUniqueId channelNo = " + channelNo + " onid = " + onid + " tsid =" + tsid + " sid = " + sid);

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(TvContract.buildChannelsUriForInput(inputId),
                    PROJECTION, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    long rowId = cursor.getLong(cursor.getColumnIndex(TvContract.Channels._ID));
                    int originalNetworkId = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID));
                    int transportStreamId = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID));
                    int serviceId = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_ID));
                    String number = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_DISPLAY_NUMBER));
                    int broadcastChannelNumber =  Integer.valueOf(number);
                    if ((onid == originalNetworkId) && (tsid == transportStreamId) && (sid == serviceId)
                        && (broadcastChannelNumber == channelNo)) {
                        return TvContract.buildChannelUri(rowId);
                    }

                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getChannelUriByUniqueId Unable to get channels for input " + inputId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.i(TAG, "getChannelUriByUniqueId out");
        return null;
    }

    /**
     * @ingroup amlhbbtvtvcontractutilsapi.
     * @brief get channel uri with the offset
     * @param contentResolver  The content resolver which used to query the provider
     * @param inputId  The Tv input id
     * @param channelUri  The channel which the uri pointed to
     * @param offset  the channel up means offest +1,the channel down means offest -1
     * @return Uri   The channel which the uri pointed to
     */
    public static Uri getChannelUriByOffset(ContentResolver contentResolver, String inputId, Uri channelUri,int offset) {
        Log.i(TAG, "getChannelUriByOffset in");
        Log.d(TAG, "getChannelUriByOffset offset = " + offset);
        if ((0 == offset) || (null == channelUri)) {
            Log.w(TAG, "getChannelByDetailsInfo input para is invalid");
            return null;
        }
        final List<Uri> channels = getChannelUrisForInput(contentResolver, inputId);
        int channelCount = channels.size();
        if (channelCount < 0) {
            return null;
        }
        int channelIndex = 0;
        int curChannelIndex = 0;
        curChannelIndex = channels.indexOf(channelUri);
        channelIndex = curChannelIndex;
        if (channelIndex != -1) {
           channelIndex += offset;
           channelIndex %= channelCount;
           if (channelIndex < 0) {
                channelIndex += channelCount;
            }
        } else {
           channelIndex = 0;
        }
        Uri retUri = channels.get(channelIndex);

        Log.d(TAG, "getChannelUriByOffset retUri = " + retUri + ", curChannelIndex = " + curChannelIndex + ", channelIndex= " + channelIndex + ", channelCount = " + channelCount);
        Log.i(TAG, "getChannelUriByOffset out");
        return retUri;
    }

    /**
     * @ingroup amlhbbtvtvcontractutilsapi.
     * @brief get channel uri with the originalNetworkId,transportStreamId and serviceId
     * @param contentResolver  The content resolver which used to query the provider
     * @param inputId  The Tv input id
     * @param broadcastSystemType  The broadcast system type
     * @param originalNetworkId  The original network id
     * @param transportStreamId  The transport stream id
     * @param serviceId   The service id
     * @return Uri   The channel which the uri pointed to
     */
    public static Uri getChannelUriByTriplet(ContentResolver contentResolver, String inputId, String broadcastSystemType, int originalNetworkId,
          int transportStreamId, int serviceId) {
            Log.i(TAG, "getChannelUriByTriplet in");
            Log.d(TAG, "getChannelUriByTriplet broadcastSystemType " + broadcastSystemType + ", broadcastSystemType " + ", originalNetworkId = " + originalNetworkId +
                ", transportStreamId = " + transportStreamId + ",, serviceId = " + serviceId);

            Cursor cursor = null;
            try {
                cursor = contentResolver.query(TvContract.buildChannelsUriForInput(inputId),
                        PROJECTION, null, null, null);
                if (cursor != null && cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        long rowId = cursor.getLong(cursor.getColumnIndex(TvContract.Channels._ID));
                        String channelType = cursor.getString(cursor.getColumnIndex(TvContract.Channels.COLUMN_TYPE));
                        int onid = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_ORIGINAL_NETWORK_ID));
                        int tsid = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_TRANSPORT_STREAM_ID));
                        int sid = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_ID));

                        if ((onid == originalNetworkId) && (tsid == transportStreamId) && (sid == serviceId)) {
                            return TvContract.buildChannelUri(rowId);
                        }

                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "getChannelUriByTriplet Unable to get channels for input " + inputId, e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            Log.i(TAG, "getChannelUriByTriplet out");
            return null;

    }

    /**
     * @ingroup amlhbbtvtvcontractutilsapi.
     * @brief get channel uri with dsd
     * @param contentResolver  The content resolver which used to query the provider
     * @param inputId  The Tv input id
     * @param dsd   The byte array data
     * @param serviceId   The service id
     * @return Uri   The channel which the uri pointed to
     */
    public static Uri getChannelUriByDsd(ContentResolver contentResolver, String inputId, byte[] dsd, int serviceId) {
        Log.i(TAG, "getChannelUriByDsd in");
        if (null == dsd) {
            Log.w(TAG, "getChannelUriByDsd input para is invalid");
            return null;
        }
        int freq = 0;//bytesToInt(dsd);
        Log.d(TAG, "getChannelUriByTriplet serviceId = " + serviceId + ", freq = " + freq);
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(TvContract.buildChannelsUriForInput(inputId),
                    PROJECTION, null, null, null);
            if (cursor != null && cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    long rowId = cursor.getLong(cursor.getColumnIndex(TvContract.Channels._ID));
                    int sid = cursor.getInt(cursor.getColumnIndex(TvContract.Channels.COLUMN_SERVICE_ID));
                    InternalProviderData internalProviderData = null;
                    byte[] internalProviderByteData = null;
                    int frequency = 0;
                    int type = cursor.getType(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA));
                    if (type == Cursor.FIELD_TYPE_BLOB) {
                        internalProviderByteData = cursor.getBlob(cursor.getColumnIndex(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_DATA));
                        if (internalProviderByteData != null) {
                            internalProviderData = new InternalProviderData(internalProviderByteData);
                        }
                    } else {
                        Log.i(TAG, "getChannelUriByUniqueId COLUMN_INTERNAL_PROVIDER_DATA other type");
                    }
                    if (internalProviderData != null) {
                        frequency = Integer.valueOf((String)internalProviderData.get("frequency"));
                        freq = frequency;
                    }

                    if ((sid == serviceId) && (frequency == freq)) {
                        return TvContract.buildChannelUri(rowId);
                    }

                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getChannelUriByTriplet Unable to get channels for input " + inputId, e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.i(TAG, "getChannelUriByDsd out");
        return null;

    }

}
