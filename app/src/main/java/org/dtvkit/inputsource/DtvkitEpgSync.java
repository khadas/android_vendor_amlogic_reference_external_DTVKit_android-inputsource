package com.droidlogic.dtvkit.inputsource;

import android.media.tv.TvContract;
import android.media.tv.TvContentRating;
import android.net.Uri;
import android.util.Log;
import android.text.TextUtils;

import com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService;
import com.droidlogic.dtvkit.companionlibrary.model.Channel;
import com.droidlogic.dtvkit.companionlibrary.model.EventPeriod;
import com.droidlogic.dtvkit.companionlibrary.model.InternalProviderData;
import com.droidlogic.dtvkit.companionlibrary.model.Program;
import org.droidlogic.dtvkit.DtvkitGlueClient;
import com.droidlogic.settings.PropSettingManager;
import com.droidlogic.fragment.ParameterMananer;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;

public class DtvkitEpgSync extends EpgSyncJobService {
    private static final String TAG = "EpgSyncJobService";

    public static final int SIGNAL_QPSK = 1; // digital satellite
    public static final int SIGNAL_COFDM = 2; // digital terrestrial
    public static final int SIGNAL_QAM   = 4; // digital cable
    public static final int SIGNAL_ISDBT  = 5;
    public static final int SIGNAL_ANALOG = 8;
    private ParameterMananer mParameterMananer = new ParameterMananer(mContext, DtvkitGlueClient.getInstance());
    private boolean mIsUK = "gbr".equals(mParameterMananer.getCurrentCountryIso3Name());

    @Override
    public List<Channel> getChannels(boolean syncCurrent) {
        List<Channel> channels = new ArrayList<>();

        Log.i(TAG, "Get channels for epg sync, current: " + syncCurrent);

        try {
            JSONArray services = null;
            JSONArray param = new JSONArray();
            param.put(false);
            if (!syncCurrent)
                param.put("all");//signal_type all
            else
                param.put("cur");
            param.put("all");//tv/radio all
            JSONObject serviceNumberObj = DtvkitGlueClient.getInstance().request("Dvb.getNumberOfServices", param);
            int channelNumber = serviceNumberObj.optInt("data", 0);
            Log.i(TAG, "Total " + channelNumber + " channels to sync");
            if (channelNumber <= 0) {
                return channels;
            }
            if (syncCurrent && TextUtils.isEmpty(getChannelTypeFilter())) {
                setChannelTypeFilter(dvbSourceToChannelTypeString(getCurrentDvbSource()));
            }
            int index = 0;
            int remainChannels = channelNumber;
            int maxTransChannelsSize = 512;
            if ((channelNumber <= maxTransChannelsSize) && syncCurrent) {
                JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.getListOfServices", new JSONArray());
                services = obj.getJSONArray("data");
            } else {
                services = new JSONArray();
                while (remainChannels > 0) {
                    JSONArray param1 = new JSONArray();
                    if (syncCurrent == true)
                        param1.put("cur");
                    else
                        param1.put("all");//signal_type current or all
                    param1.put("all");//tv_radio type
                    param1.put(index);
                    param1.put(maxTransChannelsSize);
                    JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.getListOfServicesByIndex", param1);
                    JSONArray tmpServices = obj.getJSONArray("data");
                    remainChannels = remainChannels - tmpServices.length();
                    index = index + tmpServices.length();
                    for (int i=0;i<tmpServices.length();i++) {
                        services.put(tmpServices.get(i));
                    }
                    Log.i(TAG, "Get " + tmpServices.length() + " channels and cached");
                }
            }

            //Log.i(TAG, "getChannels=" + obj.toString());
            Log.i(TAG, "Finally getChannels size=" + services.length());

            for (int i = 0; i < services.length(); i++)
            {
                JSONObject service = services.getJSONObject(i);
                String uri = service.getString("uri");

                InternalProviderData data = new InternalProviderData();
                data.put("dvbUri", uri);
                data.put("hidden", service.getBoolean("hidden"));
                data.put("network_id", service.getInt("network_id"));
                data.put("frequency", service.getInt("freq"));
                JSONObject satellite = new JSONObject();
                satellite.put("satellite_info_name", service.getString("sate_name"));
                data.put("satellite_info", satellite.toString());
                JSONObject transponder = new JSONObject();
                String tranponderDisplay = service.getString("transponder");
                transponder.put("transponder_info_display_name", tranponderDisplay);
                if (tranponderDisplay != null) {
                    String[] splitTransponder = tranponderDisplay.split("/");
                    if (splitTransponder != null && splitTransponder.length == 3) {
                        transponder.put("transponder_info_satellite_name", service.getString("sate_name"));
                        transponder.put("transponder_info_frequency", splitTransponder[0]);
                        transponder.put("transponder_info_polarity", splitTransponder[1]);
                        transponder.put("transponder_info_symbol", splitTransponder[2]);
                    }
                }
                data.put("transponder_info", transponder.toString());
                data.put("video_pid", service.getInt("video_pid"));
                data.put("video_codec", service.getString("video_codec"));
                data.put("is_data", service.getBoolean("is_data"));
                data.put("channel_signal_type", service.getString("sig_name"));
                data.put("scrambled", service.getBoolean("scrambled")?1:0);//to match with droidlogic_tv.jar
                if (service.has("is_hdtv")) {
                    data.put("is_hdtv", service.getBoolean("is_hdtv")?1:0);
                } else {
                    data.put("is_hdtv", 0);
                }
                if (service.has("mod")) {
                    data.put("modulation", parseModulationInt(service.getString("mod")));
                } else {
                    data.put("modulation", parseModulationInt("auto"));
                }
                if (service.has("fec")) {
                    data.put("fec", service.getString("fec"));
                } else {
                    data.put("fec", "auto");
                }
                if (PropSettingManager.getBoolean(PropSettingManager.CI_PROFILE_ADD_TEST, false) && (i % 4 != 0)) {
                    int countFlag = i % 4;
                    data.put("ci_number", countFlag);
                    data.put("profile_name", "profile_name" + countFlag);
                    data.put("slot_id", "slot_id" + countFlag);
                    data.put("tune_quietly", 0);
                    if (countFlag == 1) {
                        if (i < 4) {
                            data.put("is_virtual_channel", true);
                        } else {
                            data.put("is_virtual_channel", false);
                        }
                        data.put("profile_selectable", "true");
                        data.put("service_type_label", "op test");
                    } else if (countFlag == 2) {
                        data.put("profile_ver", "v1");
                        if (i < 4) {
                            data.put("profile_selectable", "true");
                        } else {
                            data.put("profile_selectable", "false");
                        }
                    } else if (countFlag == 3) {
                        data.put("profile_ver", "v2");
                        if (i < 4) {
                            data.put("profile_selectable", "true");
                        } else {
                            data.put("profile_selectable", "false");
                        }
                    }
                } else {
                    tryToPutStringToInternalProviderData(data, "ci_number", service, "ci_number");
                    tryToPutStringToInternalProviderData(data, "profile_name", service, "profile_name");
                    tryToPutStringToInternalProviderData(data, "profile_selectable", service, "profile_selectable");
                    tryToPutStringToInternalProviderData(data, "slot_id", service, "slot_id");
                    tryToPutIntToInternalProviderData(data, "tune_quietly", service, "tune_quietly");
                    tryToPutStringToInternalProviderData(data, "profile_ver", service, "profile_ver");
                    tryToPutBooleanToInternalProviderData(data, "is_virtual_channel", service, "is_virtual_channel");
                    tryToPutStringToInternalProviderData(data, "service_type_label", service, "service_type_label");
                }
                if (service.has("raw_name")) {
                    data.put("raw_displayname", service.getString("raw_name"));
                } else {
                    data.put("raw_displayname", service.getString("name"));
                }
                if (service.has("raw_lcn")) {
                    data.put("raw_displaynumber", String.format(Locale.ENGLISH, "%d", service.getInt("raw_lcn")));
                } else {
                    data.put("raw_displaynumber", String.format(Locale.ENGLISH, "%d", service.getInt("lcn")));
                }
                String channelType = TvContract.Channels.TYPE_OTHER;
                String signal_type = service.optString("sig_name", "TYPE_OTHER");
                switch (signal_type) {
                    case "DVB-T":
                        channelType = TvContract.Channels.TYPE_DVB_T;
                        break;
                    case "DVB-T2":
                        channelType = TvContract.Channels.TYPE_DVB_T2;
                        break;
                    case "DVB-C":
                        channelType = TvContract.Channels.TYPE_DVB_C;
                        break;
                    case "DVB-S":
                        channelType = TvContract.Channels.TYPE_DVB_S;
                        break;
                    case "DVB-S2":
                        channelType = TvContract.Channels.TYPE_DVB_S2;
                        break;
                    case "ISDB-T":
                        channelType = TvContract.Channels.TYPE_ISDB_T;
                        break;
                    default:
                        break;
                }
                channels.add(new Channel.Builder()
                        .setDisplayName(service.getString("name"))
                        .setType(channelType)
                        .setDisplayNumber(String.format(Locale.ENGLISH, "%d", service.getInt("lcn")))
                        .setServiceType(service.getBoolean("is_data") ? TvContract.Channels.SERVICE_TYPE_OTHER : (service.getBoolean("radio") ? TvContract.Channels.SERVICE_TYPE_AUDIO :
                                TvContract.Channels.SERVICE_TYPE_AUDIO_VIDEO))
                        .setOriginalNetworkId(Integer.parseInt(uri.substring(6, 10), 16))
                        .setTransportStreamId(Integer.parseInt(uri.substring(11, 15), 16))
                        .setServiceId(Integer.parseInt(uri.substring(16, 20), 16))
                        .setInternalProviderData(data)
                        .setLocked(service.optBoolean("blocked")?1:0)
                        .build());
            }

        } catch (Exception e) {
            Log.e(TAG, "getChannels Exception = " + e.getMessage());
        }

        return channels;
    }

    /*param signalType should not null*/
    @Override
    public boolean checkSignalTypesMatch(String signalType) {
        String currentSignalType = dvbSourceToChannelTypeString(getCurrentDvbSource());
        if (signalType.contains("TYPE_")) {
            return currentSignalType.equals(signalType);
        } else {
            return currentSignalType.equals(searchSignalTypeToChannelType(signalType));
        }
    }

    @Override
    public List<Program> getProgramsForChannel(Uri channelUri, Channel channel, long startMs, long endMs) {
        List<Program> programs = new ArrayList<>();
        long starttime, endtime;
        int parental_rating;
        int content_value;
        String content_level_1;
        String content_level_2;
        try {
            String dvbUri = getDtvkitChannelUri(channel);

            Log.i(TAG, String.format("Get channel programs for epg sync. Uri %s, startMs %d, endMs %d",
                dvbUri, startMs, endMs));

            JSONArray args = new JSONArray();
            args.put(dvbUri); // uri
            args.put(startMs/1000);
            args.put(endMs/1000);
            JSONArray events = DtvkitGlueClient.getInstance().request("Dvb.getListOfEvents", args).getJSONArray("data");

            for (int i = 0; i < events.length(); i++)
            {
                JSONObject event = events.getJSONObject(i);

                InternalProviderData data = new InternalProviderData();
                data.put("dvbUri", dvbUri);
                starttime = event.getLong("startutc") * 1000;
                endtime = event.getLong("endutc") * 1000;
                parental_rating = event.getInt("rating");
                content_value = event.optInt("content_value");
                if (starttime >= endMs || endtime <= startMs) {
                    Log.i(TAG, "Skip##  startMs:endMs=["+startMs+":"+endMs+"]  event:startT:endT=["+starttime+":"+endtime+"]");
                    continue;
                }else{
                    content_level_1 = event.getString("content_level_1");
                    content_level_2 = event.getString("content_level_2");
                    String[] genres = getGenres(event.getString("genre"), content_value);
                    String genre_str;
                    if (genres.length == 0) {
                        genre_str = (content_value <= 0xff) ? content_level_1 : content_level_2;
                        if (!TextUtils.isEmpty(genre_str)) {
                            data.put("genre", genre_str);
                        }
                    }

                    if (!TextUtils.isEmpty(event.getString("guidance"))) {
                        data.put("guidance",event.getString("guidance"));
                    }

                    Program pro = new Program.Builder()
                            .setChannelId(channel.getId())
                            .setTitle(event.getString("name"))
                            .setStartTimeUtcMillis(starttime)
                            .setEndTimeUtcMillis(endtime)
                            .setDescription(event.getString("description"))
                            .setCanonicalGenres(genres)
                            .setInternalProviderData(data)
                            .setContentRatings(parental_rating == 0 ? null : parseParentalRatings(parental_rating, event.getString("name")))
                            .build();
                    programs.add(pro);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return programs;
    }

    @Override
    public List<Program> getAllProgramsForChannel(Uri channelUri, Channel channel) {
        List<Program> programs = new ArrayList<>();
        int parental_rating;

        try {
            String dvbUri = getDtvkitChannelUri(channel);

            //Log.i(TAG, String.format("Get channel programs for epg sync. Uri %s", dvbUri));

            JSONArray args = new JSONArray();
            args.put(dvbUri); // uri
            //starttime\endtime use min and max.
            JSONArray events = DtvkitGlueClient.getInstance().request("Dvb.getListOfEvents", args).getJSONArray("data");

            for (int i = 0; i < events.length(); i++)
            {
                JSONObject event = events.getJSONObject(i);

                InternalProviderData data = new InternalProviderData();
                data.put("dvbUri", dvbUri);
                int content_value = event.optInt("content_value");
                String content_level_1 = event.getString("content_level_1");
                String content_level_2 = event.getString("content_level_2");

                String[] genres = getGenres(event.getString("genre"), content_value);
                String genre_str;
                //Log.e(TAG, "getGenres length = " + genres.length);
                if (genres.length == 0) {
                    genre_str = (content_value <= 0xff) ? content_level_1 : content_level_2;
                    if (!TextUtils.isEmpty(genre_str)) {
                        data.put("genre", genre_str);
                    }
                }

                if (!TextUtils.isEmpty(event.getString("guidance"))) {
                    data.put("guidance",event.getString("guidance"));
                }

                parental_rating = event.getInt("rating");
                Program pro = new Program.Builder()
                        .setChannelId(channel.getId())
                        .setTitle(event.getString("name"))
                        .setStartTimeUtcMillis(event.getLong("startutc") * 1000)
                        .setEndTimeUtcMillis(event.getLong("endutc") * 1000)
                        .setDescription(event.getString("description"))
                        .setLongDescription(event.getString("description_extern"))
                        .setCanonicalGenres(genres)
                        .setInternalProviderData(data)
                        .setContentRatings(parental_rating == 0 ? null : parseParentalRatings(parental_rating, event.getString("name")))
                        .build();
                programs.add(pro);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        if (programs.size() > 0) {
            Log.i(TAG, "## programs["+ programs.size() +"] ##, Uri:" + getDtvkitChannelUri(channel));
        }

        return programs;
    }

    public List<Program> getNowNextProgramsForChannel(Uri channelUri, Channel channel) {
        List<Program> programs = new ArrayList<>();

        try {
            String dvbUri = getDtvkitChannelUri(channel);

            Log.i(TAG, "Get channel now next programs for epg sync. Uri " + dvbUri);

            JSONArray args = new JSONArray();
            args.put(dvbUri); // uri
            JSONObject events = DtvkitGlueClient.getInstance().request("Dvb.getNowNextEvents", args).getJSONObject("data");

            JSONObject now = events.optJSONObject("now");
            if (now != null) {
                InternalProviderData data = new InternalProviderData();
                data.put("dvbUri", dvbUri);
                int content_value = now.optInt("content_value");
                String content_level_1 = now.getString("content_level_1");
                String content_level_2 = now.getString("content_level_2");

                String[] genres = getGenres(now.getString("genre"), content_value);
                String genre_str;
                if (genres.length == 0) {
                    genre_str = (content_value <= 0xff) ? content_level_1 : content_level_2;
                    if (!TextUtils.isEmpty(genre_str)) {
                        data.put("genre", genre_str);
                    }
                }

                if (!TextUtils.isEmpty(now.getString("guidance"))) {
                    data.put("guidance",now.getString("guidance"));
                }

                programs.add(new Program.Builder()
                        .setChannelId(channel.getId())
                        .setTitle(now.getString("name"))
                        .setStartTimeUtcMillis(now.getLong("startutc") * 1000)
                        .setEndTimeUtcMillis(now.getLong("endutc") * 1000)
                        .setDescription(now.getString("description"))
                        .setCanonicalGenres(genres)
                        .setInternalProviderData(data)
                        .build());
            }

            JSONObject next = events.optJSONObject("next");
            if (next != null) {
                InternalProviderData data = new InternalProviderData();
                data.put("dvbUri", dvbUri);
                int content_value = next.optInt("content_value");
                String content_level_1 = next.getString("content_level_1");
                String content_level_2 = next.getString("content_level_2");

                String[] genres = getGenres(next.getString("genre"), content_value);
                String genre_str;
                if (genres.length == 0) {
                    genre_str = (content_value <= 0xff) ? content_level_1 : content_level_2;
                    if (!TextUtils.isEmpty(genre_str)) {
                        data.put("genre", genre_str);
                    }
                }

                if (!TextUtils.isEmpty(next.getString("guidance"))) {
                    data.put("guidance",next.getString("guidance"));
                }

                programs.add(new Program.Builder()
                        .setChannelId(channel.getId())
                        .setTitle(next.getString("name"))
                        .setStartTimeUtcMillis(next.getLong("startutc") * 1000)
                        .setEndTimeUtcMillis(next.getLong("endutc") * 1000)
                        .setDescription(next.getString("description"))
                        .setCanonicalGenres(genres)
                        .setInternalProviderData(data)
                        .build());
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return programs;
    }

    @Override
    public List<EventPeriod> getListOfUpdatedEventPeriods()
    {
        List<EventPeriod> eventPeriods = new ArrayList<>();

        Log.i(TAG, "getListOfUpdatedEventPeriods");

        try {
            JSONArray periods = DtvkitGlueClient.getInstance().request("Dvb.getListOfUpdatedEventPeriods", new JSONArray()).getJSONArray("data");

            Log.i(TAG, periods.toString());

            for (int i = 0; i < periods.length(); i++)
            {
                JSONObject period = periods.getJSONObject(i);

                eventPeriods.add(new EventPeriod(period.getString("uri"),period.getLong("startutc"),period.getLong("endutc")));
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return eventPeriods;
    }

    private String[] getGenres(String genre, int content_value) {
        if (content_value > 0xff) {
            //tv provider don't support level 2
            return new String[]{};
        } else {
            switch (genre)
            {
                case "movies":
                    return new String[]{TvContract.Programs.Genres.MOVIES};
                case "news":
                    return new String[]{TvContract.Programs.Genres.NEWS};
                case "entertainment":
                    return new String[]{TvContract.Programs.Genres.ENTERTAINMENT};
                case "sport":
                    return new String[]{TvContract.Programs.Genres.SPORTS};
                case "childrens":
                    return new String[]{TvContract.Programs.Genres.FAMILY_KIDS};
                case "music":
                    if (mIsUK) {
                        return new String[]{TvContract.Programs.Genres.ENTERTAINMENT};
                    } else {
                        return new String[]{TvContract.Programs.Genres.MUSIC};
                    }
                case "arts":
                    return new String[]{TvContract.Programs.Genres.ARTS};
                case "social":
                    if (mIsUK) {
                        return new String[]{TvContract.Programs.Genres.NEWS};
                    } else {
                        return new String[]{TvContract.Programs.Genres.LIFE_STYLE};
                    }
                case "education":
                    return new String[]{TvContract.Programs.Genres.EDUCATION};
                case "leisure":
                    return new String[]{TvContract.Programs.Genres.LIFE_STYLE};
                default:
                    return new String[]{};
            }
        }
    }

    private TvContentRating[] parseParentalRatings(int parentalRating, String title)
    {
        TvContentRating ratings_arry[];
        String ratingSystemDefinition = "DVB";
        String ratingDomain = "com.android.tv";
        String DVB_ContentRating[] = {"DVB_4", "DVB_5", "DVB_6", "DVB_7", "DVB_8", "DVB_9", "DVB_10", "DVB_11", "DVB_12", "DVB_13", "DVB_14", "DVB_15", "DVB_16", "DVB_17", "DVB_18"};

        ratings_arry = new TvContentRating[1];
        parentalRating += 3; //minimum age = rating + 3 years
        Log.d(TAG, "parseParentalRatings parentalRating:"+ parentalRating + ", title = " + title);
        if (parentalRating >= 4 && parentalRating <= 18) {
            TvContentRating r = TvContentRating.createRating(ratingDomain, ratingSystemDefinition, DVB_ContentRating[parentalRating-4], (String) null);
            if (r != null) {
                ratings_arry[0] = r;
                Log.d(TAG, "parse ratings add rating:"+r.flattenToString()  + ", title = " + title);
            }
        }else {
            ratings_arry = null;
        }

        return ratings_arry;
    }

    private boolean tryToPutStringToInternalProviderData(InternalProviderData data, String key1, JSONObject obj, String key2) {
        boolean result = false;
        if (data != null && obj != null && key1 != null && key2 != null) {
            try {
                data.put(key1, obj.getString(key2));
                result = true;
            } catch (Exception e) {
                //Log.e(TAG, "tryToPutStringToInternalProviderData key2 = " + key2 + "Exception = " + e.getMessage());
            }
        }
        return result;
    }

    private boolean tryToPutIntToInternalProviderData(InternalProviderData data, String key1, JSONObject obj, String key2) {
        boolean result = false;
        if (data != null && obj != null && key1 != null && key2 != null) {
            try {
                data.put(key1, obj.getInt(key2));
                result = true;
            } catch (Exception e) {
                //Log.e(TAG, "tryToPutIntToInternalProviderData key2 = " + key2 + "Exception = " + e.getMessage());
            }
        }
        return result;
    }

    private boolean tryToPutBooleanToInternalProviderData(InternalProviderData data, String key1, JSONObject obj, String key2) {
        boolean result = false;
        if (data != null && obj != null && key1 != null && key2 != null) {
            try {
                data.put(key1, obj.getBoolean(key2));
                result = true;
            } catch (Exception e) {
                //Log.e(TAG, "tryToPutBooleanToInternalProviderData key2 = " + key2 + "Exception = " + e.getMessage());
            }
        }
        return result;
    }

    private String getDtvkitChannelUri(Channel channel) {
        String result = null;
        if (channel != null) {
            try {
                InternalProviderData data = channel.getInternalProviderData();
                if (data != null) {
                    result = (String)data.get(Channel.KEY_DTVKIT_URI);
                } else {
                    result = String.format("dvb://%04x.%04x.%04x", channel.getOriginalNetworkId(), channel.getTransportStreamId(), channel.getServiceId());
                }
            } catch (Exception e) {
                Log.e(TAG, "getDtvkitChannelUri Exception = " + e.getMessage());
            }
        }
        return result;
    }

    private int getCurrentDvbSource() {
        int source = SIGNAL_COFDM;
        try {
            JSONObject sourceReq = DtvkitGlueClient.getInstance().request("Dvb.GetDvbSource", new JSONArray());
            if (sourceReq != null) {
                source = sourceReq.optInt("data");
            }
        } catch (Exception e) {
        }
        return source;
    }

    private String dvbSourceToChannelTypeString(int source) {
        String result = "TYPE_DVB_T";
        switch (source) {
            case SIGNAL_COFDM:
                result = "TYPE_DVB_T";
                break;
            case SIGNAL_QAM:
                result = "TYPE_DVB_C";
                break;
            case SIGNAL_QPSK:
                result = "TYPE_DVB_S";
                break;
            case SIGNAL_ISDBT:
                result = "TYPE_ISDB_T";
                break;
            default:
                break;
        }
        return result;
    }

    private static String searchSignalTypeToChannelType(String searchSignalType) {
        String result = null;

        if (TextUtils.isEmpty(searchSignalType)) {
            return null;
        }
        switch (searchSignalType) {
            case "DVB-T":
            case "DVB_T":
                result = "TYPE_DVB_T";
                break;
            case "DVB-C":
            case "DVB_C":
                result = "TYPE_DVB_C";
                break;
            case "DVB-S":
            case "DVB_S":
                result = "TYPE_DVB_S";
                break;
            case "ISDB-T":
            case "ISDB_T":
                result = "TYPE_ISDB_T";
                break;
        }
        return result;
    }

    private int parseModulationInt(String modulation) {
        /*modulation int values refs to: droidlogic_tv.jar: TvChannelParams.java*/
        int mod = 6;//MODULATION_QAM_AUTO
        if (TextUtils.isEmpty(modulation)) {
            return mod;
        }
        switch (modulation) {
            case "qpsk":
                mod = 0;//MODULATION_QPSK
                break;
            case "16qam":
                mod = 1;//MODULATION_QAM_16
                break;
            case "8psk":
                mod = 9;//MODULATION_PSK_8
                break;
            case "16apsk":
                mod = 10;//MODULATION_APSK_16
                break;
            case "32apsk":
                mod = 11;//MODULATION_APSK_32
                break;
            case "dqpsk":
                mod = 12;//MODULATION_DQPSK
                break;
            case "32qam":
                mod = 2;//MODULATION_QAM_32
                break;
            case "64qam":
                mod = 3;//MODULATION_QAM_64
                break;
            case "128qam":
                mod = 4;//MODULATION_QAM_128
                break;
            case "256qam":
                mod = 5;//MODULATION_QAM_256
                break;
            default:
                mod = 6;//MODULATION_QAM_AUTO
                break;
        }
        return mod;
    }
}
