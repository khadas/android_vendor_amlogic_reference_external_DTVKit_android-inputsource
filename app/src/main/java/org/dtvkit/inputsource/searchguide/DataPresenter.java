package com.droidlogic.dtvkit.inputsource.searchguide;

import android.content.Context;
import android.media.tv.TvContract;
import android.support.annotation.MainThread;
import android.text.TextUtils;
import android.util.Log;

import com.droidlogic.dtvkit.inputsource.R;
import com.droidlogic.fragment.ParameterMananer;

import org.droidlogic.dtvkit.DtvkitGlueClient;
import com.droidlogic.dtvkit.inputsource.searchguide.OnMessageHandler;
import com.droidlogic.fragment.dialog.DialogManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataPresenter {
    private final String TAG = DataPresenter.class.getSimpleName();

    public static final String FRAGMENT_REGION = "Country";
    public static final String FRAGMENT_SOURCE_SELECTOR = "Select Source";
    public static final String FRAGMENT_SPEC = "Operator";
    public static final String FRAGMENT_AUTO_DISEQC = "Auto DiSEqC";
    public static final String FRAGMENT_MANUAL_DISEQC = "Manual DiSEqC";
    public static final String FRAGMENT_OPERATOR_LIST = "Operator List";
    public static final String FRAGMENT_OPERATOR_SUB_LIST = "Operator Sub List";
    public static final String FRAGMENT_SEARCH_UI = "Search UI";
    public static final String FRAGMENT_REGIONAL_CHANNELS = "Regional Channels";

    private final ParameterMananer mParameterManager;
    private final DialogManager mDialogManager;
    private final HashMap<String, OnMessageHandler> mHandlers = new HashMap<>();
    private final Context mContext;
    private boolean isM7;

    private final DtvkitGlueClient.SignalHandler mSignalHandler = (signal, data) -> {
        for (OnMessageHandler handler : mHandlers.values()) {
            handler.handleMessage(signal, data);
        }
    };

    public DataPresenter(Context context) {
        mContext = context;
        mParameterManager = new ParameterMananer(context, DtvkitGlueClient.getInstance());
        mDialogManager = new DialogManager(context, mParameterManager);
    }

    public ParameterMananer getParameterManager() {
        return mParameterManager;
    }

    public DialogManager getDialogManager() {
        return mDialogManager;
    }

    @MainThread
    public void addHandlers(String id, OnMessageHandler handler) {
        mHandlers.put(id, handler);
    }

    @MainThread
    public void removeHandlers(String id) {
        mHandlers.remove(id);
    }

    public boolean isM7() {
        return isM7;
    }

    public void setM7(boolean m7) {
        isM7 = m7;
    }

    void startMonitor() {
        DtvkitGlueClient.getInstance().registerSignalHandler(mSignalHandler);
    }

    void stopMonitor() {
        DtvkitGlueClient.getInstance().unregisterSignalHandler(mSignalHandler);
    }

    private List<String> getCountryList() {
        return mParameterManager.getCountryDisplayList();
    }

    private int getCurrentCountryIndex() {
        return mParameterManager.getCurrentCountryIndex();
    }

    private List<String> getOperatorsTypeList(int type) {
        final List<String> dataList = new ArrayList<>();
        JSONArray operatorsTypeArray = mParameterManager.getOperatorsTypeList(type);
        if (operatorsTypeArray != null && operatorsTypeArray.length() > 0) {
            JSONObject operaTypeObj = null;
            String name = null;
            for (int i = 0; i < operatorsTypeArray.length(); i++) {
                try {
                    operaTypeObj = operatorsTypeArray.getJSONObject(i);
                    name = operaTypeObj.getString("operators_name");
                    dataList.add(name);
                } catch (Exception e) {
                    Log.d(TAG, "get operator type data Exception = " + e.getMessage());
                }
            }
        } else {
            Log.d(TAG, "showOperatorTypeConfirmDialog no operatorsTypeArray");
        }
        return dataList;
    }

    public int getDataForFragment(String idx, List<String> dataList) {
        int pos = -1;
        switch (idx) {
            case FRAGMENT_REGION:
                pos = getCurrentCountryIndex();
                dataList.addAll(getCountryList());
                break;
            case FRAGMENT_SOURCE_SELECTOR:
                String type = dvbSourceToNoTypeString(mParameterManager.getCurrentDvbSource());
                dataList.add(mContext.getString(R.string.strDvbt));
                dataList.add(mContext.getString(R.string.strDvbc));
                dataList.add(mContext.getString(R.string.strDvbs));
                if (DtvkitGlueClient.getInstance().isdbtSupport()) {
                    dataList.add(mContext.getString(R.string.strIsdbt));
                }
                pos = 0;
                for (int i = 0; i < dataList.size(); i++) {
                    String s1 = dvbSourceToChannelType(dataList.get(i), false);
                    Log.d(TAG, " s1: " + s1 + ", type: " + type);
                    if (TextUtils.equals(s1, type)) {
                        pos = i;
                    }
                }
                break;
            case FRAGMENT_SPEC:
                pos = 0;
                dataList.addAll(getOperatorsTypeList(ParameterMananer.SIGNAL_QPSK));
                break;
            default:
                break;
        }
        if (dataList != null) {
            Log.d(TAG, "getDataForFragment pos=" + pos + ", size=" + dataList.size());
        }
        return pos;
    }

    public static String dvbSourceToChannelType(String source, boolean hasType) {
        source = source.replace('-', '_');
        Pattern p = Pattern.compile("[TYPE_]?(DVB|ISDB)_[TCS]");
        Matcher matcher = p.matcher(source);
        StringBuilder convert = new StringBuilder();
        if (matcher.find()) {
            convert.append(matcher.group());
            if (hasType && !convert.toString().contains("TYPE")) {
                convert.insert(0, "TYPE_");
            } else if (!hasType && convert.toString().contains("TYPE")) {
                convert.substring(5);
            }
        } else {
            return source;
        }
        return convert.toString();
    }

    public static String dvbSourceToNoTypeString(int source) {
        String result = TvContract.Channels.TYPE_DVB_T;

        switch (source) {
            case ParameterMananer.SIGNAL_COFDM:
                result = TvContract.Channels.TYPE_DVB_T;
                break;
            case ParameterMananer.SIGNAL_QAM:
                result = TvContract.Channels.TYPE_DVB_C;
                break;
            case ParameterMananer.SIGNAL_QPSK:
                result = TvContract.Channels.TYPE_DVB_S;
                break;
            case ParameterMananer.SIGNAL_ISDBT:
                result = TvContract.Channels.TYPE_ISDB_T;
                break;
            default:
                break;
        }
        return result.substring(5);
    }
}
