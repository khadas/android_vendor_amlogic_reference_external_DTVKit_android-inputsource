package com.droidlogic.dtvkit.inputsource;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService;
import com.droidlogic.dtvkit.inputsource.DataManager;
import com.droidlogic.dtvkit.inputsource.DtvkitDvbScanSelect;
import com.droidlogic.dtvkit.inputsource.DtvkitEpgSync;
import com.droidlogic.dtvkit.inputsource.PvrStatusConfirmManager;
import com.droidlogic.fragment.ParameterManager;
import com.droidlogic.settings.ConstantManager;
import com.droidlogic.settings.PropSettingManager;

import org.droidlogic.dtvkit.DtvkitGlueClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public class DtvkitIsdbtSetup extends Activity {
    private static final String TAG = "DtvkitIsdbtSetup";

    private SEARCH_STAGE mSearchingStage = SEARCH_STAGE.NOT_START;
    private enum SEARCH_STAGE {
        NOT_START,
        DTV_STARTING,
        DTV_FINISH,
        ATV_STARTING,
        ATV_FINISH,
    }
    private int mSearchMode = DataManager.VALUE_PUBLIC_SEARCH_MODE_AUTO;
    private boolean mIsHybridSearch = false;
    private int mManualSearchTvType = 0; // atv: 0, dtv: 1
    private DataManager mDataManager;
    private ParameterManager mParameterManager = null;
    private int mAntennaType = 0;
    private boolean mStartSync = false;
    private boolean mStartSearch = false;
    private boolean mSyncFinish = false;
    private boolean mFinish = false;
    private JSONArray mServiceList = null;
    private int mFoundServiceNumber = 0;
    private int mSearchManualAutoType = -1;// 0 manual 1 auto
    private PvrStatusConfirmManager mPvrStatusConfirmManager = null;

    protected HandlerThread mHandlerThread = null;
    protected Handler mThreadHandler = null;

    private final static int MSG_START_SEARCH = 1;
    private final static int MSG_STOP_SEARCH = 2;
    private final static int MSG_FINISH_SEARCH = 3;
    private final static int MSG_ON_SIGNAL = 4;
    private final static int MSG_FINISH = 5;
    private final static int MSG_RELEASE= 6;

    private long clickLastTime = 0;
    private Button mSearchButton;

    private final DtvkitGlueClient.SignalHandler mHandler = (signal, data) -> {
        Map<String, Object> map = new HashMap<>();
        map.put("signal", signal);
        map.put("data", data);
        sendOnSignal(map);
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String status = intent.getStringExtra(EpgSyncJobService.SYNC_STATUS);
            if (status.equals(EpgSyncJobService.SYNC_FINISHED)
                    || status.equals(EpgSyncJobService.SYNC_ERROR)) {
                setSearchStatus("Finished", "");
                setStrengthAndQualityStatus("","");
                mStartSync = false;
                mSyncFinish = true;
                sendFinish();
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG, "key " + KeyEvent.keyCodeToString(keyCode));
        if (mStartSync) {
            Toast.makeText(DtvkitIsdbtSetup.this, R.string.sync_tv_provider, Toast.LENGTH_SHORT).show();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mStartSearch) {
                sendFinishSearch();
            } else {
                stopMonitoringSearch();
                //stopSearch();
                sendStopSearch(0);
                //finish();
                sendFinish();
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.isdb_setup);

        mIsHybridSearch = PropSettingManager.getBoolean("vendor.tv.hybrid.search", false);
        Log.i(TAG, "HybridSearch:" + mIsHybridSearch);
        mParameterManager = new ParameterManager(this, DtvkitGlueClient.getInstance());
        final Button optionSet = findViewById(R.id.option_set_btn);
        optionSet.setOnClickListener(v -> {
            Intent intentSet = new Intent(getIntent());
            String pvrStatus = intentSet.getStringExtra(ConstantManager.KEY_LIVETV_PVR_STATUS);
            String pvrFlag = PvrStatusConfirmManager.read(DtvkitIsdbtSetup.this, PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG);
            if (pvrStatus != null && PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG_FIRST.equals(pvrFlag)) {
                intentSet.putExtra(ConstantManager.KEY_LIVETV_PVR_STATUS, pvrStatus);
            } else {
                intentSet.putExtra(ConstantManager.KEY_LIVETV_PVR_STATUS, "");
            }
            intentSet.setClassName(DataManager.KEY_PACKAGE_NAME, DataManager.KEY_ACTIVITY_SETTINGS);
            startActivity(intentSet);
            mDataManager.saveIntParameters(DataManager.KEY_SELECT_SEARCH_ACTIVITY, DataManager.SELECT_SETTINGS);
        });
        mSearchButton = findViewById(R.id.btn_terrestrial_start_search);
        EditText public_type_edit = findViewById(R.id.edtTxt_public_type_in);

        mSearchButton.setEnabled(true);
        mSearchButton.setOnClickListener(v -> {
            long currentTime = SystemClock.elapsedRealtime();
            if (currentTime - clickLastTime > 500) {
                clickLastTime = currentTime;
                boolean autoSearch = isAutoSearch();
                mPvrStatusConfirmManager.setSearchType(autoSearch ? ConstantManager.KEY_DTVKIT_SEARCH_TYPE_AUTO : ConstantManager.KEY_DTVKIT_SEARCH_TYPE_MANUAL);
                boolean checkPvr = mPvrStatusConfirmManager.needDeletePvrRecordings();
                if (checkPvr) {
                    mPvrStatusConfirmManager.showDialogToAppoint(DtvkitIsdbtSetup.this, autoSearch);
                } else {
                    if (mStartSearch) {
                        Log.d(TAG, "mAntennaType:" + mAntennaType + ", " + mSearchingStage);
                        if (isAutoSearch() && mAntennaType == 0 && (!mIsHybridSearch) && mSearchingStage.ordinal() < SEARCH_STAGE.ATV_STARTING.ordinal()) {
                            sendStopSearch(1);
                        } else {
                            sendFinishSearch();
                        }
                    } else {
                        mPvrStatusConfirmManager.sendDvrCommand(DtvkitIsdbtSetup.this);
                        sendStartSearch();
                    }
                }
            }
        });
        mSearchButton.requestFocus();
        mDataManager = new DataManager(this);
        mPvrStatusConfirmManager = new PvrStatusConfirmManager(this, mDataManager);
        Intent intent = getIntent();
        if (intent != null) {
            String status = intent.getStringExtra(ConstantManager.KEY_LIVETV_PVR_STATUS);
            mPvrStatusConfirmManager.setPvrStatus(status);
            Log.d(TAG, "onCreate status = " + status);
        }
        initHandler();

        public_type_edit.addTextChangedListener(new TextWatcher(){
            private String mText;
            private int mCursor;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after){ }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count){
                Log.i(TAG,"text[" + text.toString() + "] start[" + start + "] count[" + count +"]");
                mCursor = start;
                mText = text.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!mText.equals("") && mCursor == 0) {
                    public_type_edit.removeTextChangedListener(this);
                    int index = public_type_edit.getSelectionStart();
                    if (isInManualATV()) {
                        s.insert(index, ".25");
                    } else {
                        s.insert(index, ".143");
                    }
                    public_type_edit.addTextChangedListener(this);
                    public_type_edit.setSelection(1);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mPvrStatusConfirmManager.registerCommandReceiver();
        initOrUpdateView(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mPvrStatusConfirmManager.unRegisterCommandReceiver();
    }

    @Override
    public void finish() {
        //send search info to livetv if found any
        Log.d(TAG, "finish");
        Intent intent = new Intent();
        intent.putExtra(DtvkitDvbScanSelect.SEARCH_TYPE_MANUAL_AUTO, mSearchManualAutoType);
        intent.putExtra(DtvkitDvbScanSelect.SEARCH_TYPE_DVBS_DVBT_DVBC, DtvkitDvbScanSelect.SEARCH_TYPE_DVBT);
        intent.putExtra(DtvkitDvbScanSelect.SEARCH_FOUND_SERVICE_NUMBER, mFoundServiceNumber);
        if (mFoundServiceNumber > 0) {
            String firstServiceName = "";
            try {
                if (mServiceList != null && mServiceList.length() > 0) {
                    firstServiceName = mServiceList.getJSONObject(0).getString("name");
                    for (int i = 0; i < mServiceList.length(); i++) {
                        if (!mServiceList.getJSONObject(i).optBoolean("hidden")) {
                            firstServiceName = mServiceList.getJSONObject(i).getString("name");
                            break;
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "finish JSONException = " + e.getMessage());
            }
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_FOUND_FIRST_SERVICE, firstServiceName);
            Log.d(TAG, "finish firstServiceName = " + firstServiceName);
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED, mSyncFinish ? intent : null);
        }
        super.finish();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (mStartSearch) {
            sendFinishSearch();
        } else if (!mFinish) {
            stopMonitoringSearch();
            sendStopSearch(0);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        releaseHandler();
        stopMonitoringSearch();
        stopMonitoringSync();
    }

    private void initHandler() {
        Log.d(TAG, "initHandler");
        mHandlerThread = new HandlerThread("DtvkitIsdbtSetup");
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper(), msg -> {
            Log.d(TAG, "mThreadHandler handleMessage " + msg.what + " start");
            switch (msg.what) {
                case MSG_START_SEARCH: {
                    startSearch();
                    break;
                }
                case MSG_STOP_SEARCH: {
                    stopSearch(msg.arg1);
                    break;
                }
                case MSG_FINISH_SEARCH: {
                    onSearchFinished();
                    break;
                }
                case MSG_ON_SIGNAL: {
                    dealOnSignal((Map<String, Object>)msg.obj);
                    break;
                }
                case MSG_FINISH: {
                    finish();
                    break;
                }
                case MSG_RELEASE: {
                    releaseInThread();
                    break;
                }
                default:
                    break;
            }
            Log.d(TAG, "mThreadHandler handleMessage " + msg.what + " over");
            return true;
        });
    }

    private void releaseInThread() {
        Log.d(TAG, "releaseInThread");
        new Thread(() -> {
            Log.d(TAG, "releaseInThread start");
            releaseHandler();
            Log.d(TAG, "releaseInThread end");
        }).start();
    }

    private void releaseHandler() {
        Log.d(TAG, "releaseHandler");
        mHandlerThread.getLooper().quitSafely();
        mThreadHandler.removeCallbacksAndMessages(null);
        mHandlerThread = null;
        mThreadHandler = null;
    }

    private void initOrUpdateView(boolean init) {
        LinearLayout manual_frequency_search = findViewById(R.id.manual_frequency_search);
        TextView public_type_in = findViewById(R.id.public_type_in_text);
        EditText public_type_edit = findViewById(R.id.edtTxt_public_type_in);
        LinearLayout dvbt_bandwidth_container = findViewById(R.id.dvbt_bandwidth_container);
        Spinner dvbt_bandwidth_spinner = findViewById(R.id.dvbt_bandwidth_spinner);
        LinearLayout dvbt_mode_container = findViewById(R.id.dvbt_mode_container);
        Spinner dvbt_mode_spinner = findViewById(R.id.dvbt_mode_spinner);

        Spinner public_search_mode_spinner = findViewById(R.id.public_search_mode_spinner);
        LinearLayout frequency_channel_container = findViewById(R.id.frequency_channel_container);
        Spinner frequency_channel_spinner = findViewById(R.id.frequency_channel_spinner);
        LinearLayout manual_number_search = findViewById(R.id.manual_number_search);
        Spinner public_search_channel_name_spinner = findViewById(R.id.public_search_channel_spinner);
        CheckBox nit = findViewById(R.id.network);

        LinearLayout public_antenna_type_container = findViewById(R.id.antenna_type_container);
        public_antenna_type_container.setVisibility(View.VISIBLE);

        LinearLayout public_adtv_type_container = findViewById(R.id.adtv_type_container);
        Spinner public_adtv_type_spinner = findViewById(R.id.adtv_type_spinner);
        public_adtv_type_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    dvbt_mode_container.setVisibility(View.GONE);
                    mSearchingStage = SEARCH_STAGE.DTV_FINISH;
                } else {
                    dvbt_mode_container.setVisibility(View.VISIBLE);
                    mSearchingStage = SEARCH_STAGE.NOT_START;
                }
                Log.i(TAG, "reset Stage:" + mSearchingStage);
                mManualSearchTvType = position;
                updateChannelNameContainer(position == 0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Spinner antenna_type_spinner = findViewById(R.id.antenna_type_spinner);
        String dtvType = mDataManager.getStringParameters(DataManager.KEY_ISDB_ANTENNA_TYPE);
        int pos = 0;
        if (TextUtils.equals(TvContract.Channels.TYPE_ATSC_C, dtvType)) {
            pos = 1;
        }
        antenna_type_spinner.setSelection(pos);
        antenna_type_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String dtvType;
                mAntennaType = position;
                if (position == 1) {
                    // cable
                    public_adtv_type_spinner.setSelection(0);
                    public_adtv_type_spinner.setEnabled(false);
                    dtvType = TvContract.Channels.TYPE_ATSC_C;
                } else {
                    public_adtv_type_spinner.setEnabled(true);
                    dtvType = TvContract.Channels.TYPE_ATSC_T;
                }
                if (!isAutoSearch()) {
                    updateChannelNameContainer(mManualSearchTvType == 0);
                }
                Log.d(TAG, "antenna_type_spinner onItemSelected position = " + position);
                mDataManager.saveStringParameters(DataManager.KEY_ISDB_ANTENNA_TYPE, dtvType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });

        int isFrequencyMode = mDataManager.getIntParameters(DataManager.KEY_IS_FREQUENCY);
        if (isFrequencyMode == DataManager.VALUE_FREQUENCY_MODE) {
            public_type_in.setText(R.string.search_frequency);
            public_type_edit.setHint(R.string.search_frequency_hint);
            manual_frequency_search.setVisibility(View.VISIBLE);
            manual_number_search.setVisibility(View.GONE);
        } else {
            manual_frequency_search.setVisibility(View.GONE);
            manual_number_search.setVisibility(View.VISIBLE);
        }
        public_type_edit.setText("");
        int value = mDataManager.getIntParameters(DataManager.KEY_PUBLIC_SEARCH_MODE);
        public_search_mode_spinner.setSelection(value);
        nit.setChecked(mDataManager.getIntParameters(DataManager.KEY_NIT) == 1);
        if (DataManager.VALUE_PUBLIC_SEARCH_MODE_AUTO == value) {
            manual_frequency_search.setVisibility(View.GONE);
            dvbt_bandwidth_container.setVisibility(View.GONE);
            dvbt_mode_container.setVisibility(View.GONE);
            frequency_channel_container.setVisibility(View.GONE);
            manual_number_search.setVisibility(View.GONE);
            public_adtv_type_container.setVisibility(View.GONE);
            nit.setVisibility(View.VISIBLE);
            mSearchButton.setText(R.string.strAutoSearch);
        } else {
            updateChannelNameContainer(isInManualATV());
            mSearchButton.setText(R.string.strManualSearch);
            if (isFrequencyMode == DataManager.VALUE_FREQUENCY_MODE) {
                manual_frequency_search.setVisibility(View.VISIBLE);
                manual_number_search.setVisibility(View.GONE);
            } else {
                manual_frequency_search.setVisibility(View.GONE);
                manual_number_search.setVisibility(View.VISIBLE);
            }
            nit.setVisibility(View.GONE);
            public_adtv_type_container.setVisibility(View.VISIBLE);
            frequency_channel_container.setVisibility(View.VISIBLE);
            frequency_channel_spinner.setSelection(mDataManager.getIntParameters(DataManager.KEY_IS_FREQUENCY));
            dvbt_bandwidth_container.setVisibility(View.GONE);
            dvbt_bandwidth_spinner.setSelection(1);
            if (public_adtv_type_spinner.getSelectedItemPosition() == 1) {
                dvbt_mode_container.setVisibility(View.VISIBLE);
                dvbt_mode_spinner.setSelection(mDataManager.getIntParameters(DataManager.KEY_DVBT_MODE));
            }
        }
        if (init) {//init one time
            dvbt_bandwidth_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "isdbt_bandwidth_spinner onItemSelected position = " + position);
                    mDataManager.saveIntParameters(DataManager.KEY_DVBT_BANDWIDTH, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            dvbt_mode_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "dvbt_mode_spinner onItemSelected position = " + position);
                    mDataManager.saveIntParameters(DataManager.KEY_DVBT_MODE, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            public_search_mode_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    mSearchMode = position;
                    if (position == mDataManager.getIntParameters(DataManager.KEY_PUBLIC_SEARCH_MODE)) {
                        Log.d(TAG, "public_search_mode_spinner select same position = " + position);
                        return;
                    }
                    if (position == DataManager.VALUE_PUBLIC_SEARCH_MODE_AUTO) {
                        // reset Stage
                        mSearchingStage = SEARCH_STAGE.NOT_START;
                        Log.i(TAG, "Reset stage to " + mSearchingStage);
                    }
                    Log.d(TAG, "public_search_mode_spinner onItemSelected position = " + position);
                    mDataManager.saveIntParameters(DataManager.KEY_PUBLIC_SEARCH_MODE, position);
                    initOrUpdateView(false);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            frequency_channel_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == mDataManager.getIntParameters(DataManager.KEY_IS_FREQUENCY)) {
                        Log.d(TAG, "frequency_channel_container select same position = " + position);
                        return;
                    }
                    Log.d(TAG, "frequency_channel_container onItemSelected position = " + position);
                    mDataManager.saveIntParameters(DataManager.KEY_IS_FREQUENCY, position);
                    initOrUpdateView(false);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            public_search_channel_name_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "public_search_channel_name_spinner onItemSelected position = " + position);
                    mDataManager.saveIntParameters(DataManager.KEY_SEARCH_DVBT_CHANNEL_NAME, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            nit.setOnClickListener(v -> {
                if (nit.isChecked()) {
                    mDataManager.saveIntParameters(DataManager.KEY_NIT, 1);
                } else {
                    mDataManager.saveIntParameters(DataManager.KEY_NIT, 0);
                }
            });
        }
    }

    private void updateChannelNameContainer(boolean isAtv) {
        Spinner public_search_channel_name_spinner = findViewById(R.id.public_search_channel_spinner);

        JSONArray list;
        List<String> newList = new ArrayList<>();
        ArrayAdapter<String> adapter;
        int select = mDataManager.getIntParameters(DataManager.KEY_SEARCH_ISDBT_CHANNEL_NAME);
        int county_code_Brazil = ('b' << 16 | 'r' << 8 | 'a') & 0xFFFFFF;
        if (isAtv) {
            list = mParameterManager.getRfChannelTable(mAntennaType, ParameterManager.DTV_TYPE_ATV, false);
        } else {
            list = mParameterManager.getIsdbtChannelTable(county_code_Brazil);
        }

        if (list == null || list.length() == 0) {
            Log.d(TAG, "updateChannelNameContainer can't find channel freq table");
            return;
        }

        try {
            for (int i = 0; i < list.length(); i++) {
                JSONObject channelTable = (JSONObject)list.get(i);
                int freq = channelTable.optInt("freq", 0);
                int channelNumber = channelTable.optInt("index", 0);
                String name = channelTable.optString("name", "ch" + channelNumber);
                newList.add("NO." + channelNumber + " " + name + " " + freq + "Hz");
            }
        } catch (Exception e) {
            Log.d(TAG, "got invalid channel freq table");
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, newList);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        public_search_channel_name_spinner.setAdapter(adapter);
        select = (select < list.length()) ? select : 0;
        public_search_channel_name_spinner.setSelection(select);
    }

    private boolean isInManualATV() {
        Spinner spinner = findViewById(R.id.adtv_type_spinner);
        if (spinner != null && spinner.getVisibility() == View.VISIBLE) {
            return 0 == spinner.getSelectedItemPosition();
        } else {
            return false;
        }
    }

    private boolean initSearchParameter(JSONArray args, int antennaType, boolean isAtvType, int isFrequencySearch) {
        Log.d(TAG, "initSearchParameter autoSearch:" + isAutoSearch()
                + ", antennaType:" + antennaType
                + ", isAtvType:" + isAtvType
                + ", isFrequencySearch:" + isFrequencySearch);
        // antennaType == 1, cable atv
        // antennaType == 0, air dtv&atv
        if (isAutoSearch()) {
            if (mIsHybridSearch) {
                args.put(mAntennaType == 1 ? "CABLE" : "AIR");
            } if (antennaType == 0 && mSearchingStage.ordinal() < SEARCH_STAGE.DTV_FINISH.ordinal()) {
                // ISDB-T DTV
                args.put(true); // reTune
                args.put(mDataManager.getIntParameters(DataManager.KEY_NIT) > 0);
            } else {
                // ATV
                args.put(antennaType);
                args.put(2000000);
            }
            return true;
        } else {
            if (mIsHybridSearch) {
                Log.e(TAG, "atv channel list not ready;");
                return false;
            } else if (antennaType == 0 && !isAtvType) {
                // manual DTV
                int parameter = getParameter();
                if (parameter >= 0) {
                    args.put(true); // reTune
                    if (DataManager.VALUE_FREQUENCY_MODE == isFrequencySearch) {
                        if (mSearchingStage.ordinal() < SEARCH_STAGE.DTV_FINISH.ordinal()) {
                            args.put(mDataManager.getIntParameters(DataManager.KEY_NIT) > 0);
                            args.put(parameter * 1000);//khz to hz
                            args.put("6MHZ");
                            args.put(DataManager.VALUE_DVBT_MODE_LIST[mDataManager.getIntParameters(DataManager.KEY_DVBT_MODE)]);
                        } else {
                            args.put(parameter * 1000);//khz to hz
                        }
                    } else {
                        int index = getChannelIndex();
                        if (index < 0) {
                            Log.e(TAG, "initSearchParameter Isdbt search can't find channel index");
                            return false;
                        }
                        args.put(index);
                    }
                    return true;
                }
            } else {
                int parameter = getParameter();
                if (parameter >= 0) {
                    // manual ATV
                    args.put(antennaType);
                    if (DataManager.VALUE_FREQUENCY_MODE == isFrequencySearch) {
                        args.put(parameter * 1000);//khz to hz
                    } else {
                        args.put(parameter); // chname
                    }
                }
                return parameter >= 0;
            }
        }
        return false;
    }

    private int getParameter() {
        int parameter = -1;
        EditText public_type_edit = findViewById(R.id.edtTxt_public_type_in);
        Editable editable = public_type_edit.getText();
        int isFrequencySearch = mDataManager.getIntParameters(DataManager.KEY_IS_FREQUENCY);
        if (DataManager.VALUE_FREQUENCY_MODE != isFrequencySearch) {
            parameter = getChannelIndex();
        } else if (editable != null) {
            String value = editable.toString();
            if (!TextUtils.isEmpty(value)/* && TextUtils.isDigitsOnly(value)*/) {
                //float for frequency
                float toFloat = Float.parseFloat(value);
                parameter = (int)(toFloat * 1000.0f);//khz
            }
        }
        return parameter;
    }

    private int getChannelIndex() {
        int index = -1;
        Spinner public_search_channel_name_spinner = findViewById(R.id.public_search_channel_spinner);
        String chName = (String) public_search_channel_name_spinner.getSelectedItem();
        if (!TextUtils.isEmpty(chName)) {
            if (mManualSearchTvType == 0) {
                index = Integer.parseInt(chName.substring(chName.indexOf(" ") + 1, chName.lastIndexOf(" "))); // chName
            } else {
                index = Integer.parseInt(chName.substring(chName.indexOf(".") + 1, chName.indexOf(" ")));
            }
            int freq = Integer.parseInt(chName.substring(chName.lastIndexOf(" ") + 1, chName.indexOf("Hz")));
            Log.d(TAG, "getChannelIndex channel index = " + index
                    + ", freq = " + freq/1000 + "kHz");
        }

        return index;
    }

    private void sendStartSearch() {
        if (mThreadHandler != null) {
            mThreadHandler.removeMessages(MSG_START_SEARCH);
            Message mess = mThreadHandler.obtainMessage(MSG_START_SEARCH, 0, 0, null);
            boolean info = mThreadHandler.sendMessageDelayed(mess, 0);
            Log.d(TAG, "sendMessage MSG_START_SEARCH " + info);
        }
    }

    private void startSearch() {
        setSearchStatus("Searching", "");
        setSearchProgressIndeterminate(false);
        startMonitoringSearch();
        mFoundServiceNumber = 0;
        updateSearchButton(false);
        runOnUiThread(() -> mSearchButton.setEnabled(true));
        if (!doFinishSearch()) {
            return;
        }
        String failReason = null;
        if (isAutoSearch()) {
            if (mAntennaType == 1 || mSearchingStage == SEARCH_STAGE.DTV_STARTING) {
                // start to atv search
                mSearchingStage = SEARCH_STAGE.DTV_FINISH;
            }
        }
        int isFrequencySearch = mDataManager.getIntParameters(DataManager.KEY_IS_FREQUENCY);
        JSONArray args = new JSONArray();
        if (initSearchParameter(args, mAntennaType, isInManualATV(), isFrequencySearch)) {
            boolean result;
            if (isAutoSearch()) {
                result = doStartAutoSearch(args);
            } else {
                result = doStartManualSearch(args, isFrequencySearch == DataManager.VALUE_FREQUENCY_MODE);
            }
            mStartSearch = result;
            if (result) {
                if (mSearchingStage == SEARCH_STAGE.NOT_START) {
                    mSearchingStage = SEARCH_STAGE.DTV_STARTING;
                } else if (mSearchingStage == SEARCH_STAGE.DTV_FINISH) {
                    mSearchingStage = SEARCH_STAGE.ATV_STARTING;
                } else {
                    Log.e(TAG, "error : mSearchingStage:" + mSearchingStage);
                    failReason = "Failed to start search:" + mSearchingStage;
                }
            } else {
                failReason = "Failed to start search";
            }
        } else {
            failReason = "parameter not complete";
        }
        final Button optionSet = findViewById(R.id.option_set_btn);
        if (failReason == null) {
            optionSet.setEnabled(false);
        } else {
            stopMonitoringSearch();
            setSearchStatus(failReason, "");
            stopSearch(0);
            optionSet.setEnabled(true);
        }
    }

    private void sendStopSearch(int goToNext) {
        if (mThreadHandler != null) {
            mThreadHandler.removeMessages(MSG_STOP_SEARCH);
            Message mess = mThreadHandler.obtainMessage(MSG_STOP_SEARCH, goToNext, 0, null);
            boolean info = mThreadHandler.sendMessageDelayed(mess, 0);
            Log.d(TAG, "sendMessage MSG_STOP_SEARCH " + info);
        }
    }

    private void stopSearch(int goToNext) {
        mStartSearch = false;
        if (goToNext == 0) {
            updateSearchButton(true);
        }
        try {
            JSONArray args = new JSONArray();
            args.put(true); // Commit
            if (mSearchingStage.ordinal() <= SEARCH_STAGE.DTV_FINISH.ordinal()) {
                DtvkitGlueClient.getInstance().request("Isdbt.finishSearch", args);
                if (isAutoSearch()) {
                    setSearchProgress(100, false);
                }
            } else {
                DtvkitGlueClient.getInstance().request("Atv.finishSearch", args);
                if (isAutoSearch()) {
                    setSearchProgress(100, true);
                }
            }
        } catch (Exception e) {
            setSearchStatus("Failed to finish search", e.getMessage());
            if (goToNext > 0) {
                return;
            }
        }
        if (goToNext > 0) {
            sendStartSearch();
        }
    }

    private boolean doFinishSearch() {
        String command;
        JSONArray args = new JSONArray();
        if (mIsHybridSearch) {
            command = "Tv.stopSearch";
        } else {
            if (mSearchingStage.ordinal() < SEARCH_STAGE.DTV_FINISH.ordinal()) {
                command = "Isdbt.finishSearch";
            } else {
                command = "Atv.finishSearch";
            }
        }
        try {
            DtvkitGlueClient.getInstance().request(command, args);
        } catch (Exception e) {
            setSearchStatus("Failed to finish search", e.getMessage());
            return false;
        }
        return true;
    }

    private boolean doStartAutoSearch(JSONArray args) {
        String command;
        if (mIsHybridSearch) {
            command = "Tv.startAutoSearch";
        } else {
            if (mSearchingStage == SEARCH_STAGE.NOT_START) {
                command = "Isdbt.startSearch";
            } else {
                command = "Atv.startAutoSearch";
            }
        }
        try {
            DtvkitGlueClient.getInstance().request(command, args);
            Log.d(TAG, "command = " + command + ", args = " + args);
            mParameterManager.saveChannelIdForSource(-1);
        } catch (Exception e) {
            setSearchStatus("Failed to finish search", e.getMessage());
            return false;
        }
        return true;
    }

    private boolean doStartManualSearch(JSONArray args, boolean byFreq) {
        String command;
        if (mIsHybridSearch) {
            command = "Tv.startManualSearchByChannelId";
        } else {
            if (byFreq) {
                if (mManualSearchTvType == 1) {
                    command = "Isdbt.startManualSearchByFreq";
                } else {
                    command = "Atv.startManualSearchByFreq";
                }
            } else {
                if (mManualSearchTvType == 1) {
                    command = ("Isdbt.startManualSearchById");
                } else {
                    command = ("Atv.startManualSearchById");
                }
            }
        }
        try {
            DtvkitGlueClient.getInstance().request(command, args);
            Log.d(TAG, "command = " + command + ", args = " + args);
        } catch (Exception e) {
            setSearchStatus("Failed to finish search", e.getMessage());
            return false;
        }
        return true;
    }

    private void sendFinishSearch() {
        if (mThreadHandler != null) {
            mThreadHandler.removeMessages(MSG_FINISH_SEARCH);
            Message mess = mThreadHandler.obtainMessage(MSG_FINISH_SEARCH, 0, 0, null);
            boolean info = mThreadHandler.sendMessageDelayed(mess, 0);
            Log.d(TAG, "sendMessage MSG_FINISH_SEARCH " + info);
        }
    }

    private void onSearchFinished() {
        mStartSearch = false;
        runOnUiThread(() -> mSearchButton.setEnabled(false));
        updateSearchButton(true);
        setSearchStatus("Finishing search", "");
        setStrengthAndQualityStatus("","");
        setSearchProgressIndeterminate(true);
        stopMonitoringSearch();
        if (!doFinishSearch()) {
            return;
        }
        //update search results as After the search is finished, the lcn will be reordered
        try {
            mServiceList = DtvkitEpgSync.getServicesList();
            DtvkitEpgSync.setServicesToSync(mServiceList);
        } catch (Exception ignored) {}
        mFoundServiceNumber = getFoundServiceNumber();
        if (mFoundServiceNumber == 0 && mServiceList != null && mServiceList.length() > 0) {
            Log.d(TAG, "mFoundServiceNumber erro use mServiceList length = " + mServiceList.length());
            mFoundServiceNumber = mServiceList.length();
        }
        setSearchStatus("Updating guide", "");
        setStrengthAndQualityStatus("","");
        startMonitoringSync();

        // If the intent that started this activity is from Live Channels app
        String inputId = this.getIntent().getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
        Log.i(TAG, String.format("inputId: %s", inputId));
        //EpgSyncJobService.requestImmediateSync(this, inputId, true, new ComponentName(this, DtvkitEpgSync.class)); // 12 hours
        Bundle parameters = new Bundle();
        int isFrequencySearch = mDataManager.getIntParameters(DataManager.KEY_IS_FREQUENCY);
        if (DataManager.VALUE_FREQUENCY_MODE == isFrequencySearch && DataManager.VALUE_PUBLIC_SEARCH_MODE_AUTO != mSearchMode) {
            parameters.putInt(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_FREQUENCY, getParameter() * 1000);
        }
        parameters.putString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_MODE, DataManager.VALUE_PUBLIC_SEARCH_MODE_AUTO == mSearchMode ? EpgSyncJobService.BUNDLE_VALUE_SYNC_SEARCHED_MODE_AUTO : EpgSyncJobService.BUNDLE_VALUE_SYNC_SEARCHED_MODE_MANUAL);
        parameters.putString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_SIGNAL_TYPE, "ISDB-T");

        Intent intent = new Intent(this, com.droidlogic.dtvkit.inputsource.DtvkitEpgSync.class);
        intent.putExtra("inputId", inputId);
        intent.putExtra(EpgSyncJobService.BUNDLE_KEY_SYNC_FROM, TAG);
        intent.putExtra(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_CHANNEL, (mFoundServiceNumber > 0));
        intent.putExtra(EpgSyncJobService.BUNDLE_KEY_SYNC_PARAMETERS, parameters);
        startService(intent);
    }

    private void startMonitoringSearch() {
        DtvkitGlueClient.getInstance().registerSignalHandler(mHandler);
    }

    private void stopMonitoringSearch() {
        DtvkitGlueClient.getInstance().unregisterSignalHandler(mHandler);
    }

    private void startMonitoringSync() {
        mStartSync = true;
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
                new IntentFilter(EpgSyncJobService.ACTION_SYNC_STATUS_CHANGED));
    }

    private void stopMonitoringSync() {
        mStartSync = false;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
    }

    private void setSearchStatus(final String status, final String description) {
        runOnUiThread(() -> {
            Log.i(TAG, String.format("Search status \"%s\"", status));
            final TextView text = findViewById(R.id.tv_search_status);
            text.setText(String.format("%s\t%s",status,description));
        });
    }

    private void setStrengthAndQualityStatus(final String strengthStatus, final String qualityStatus) {
        runOnUiThread(() -> {
            if (TextUtils.isEmpty(strengthStatus) && TextUtils.isEmpty(qualityStatus)) {
                findViewById(R.id.channel_holder).setVisibility(View.GONE);
            } else {
                findViewById(R.id.channel_holder).setVisibility(View.VISIBLE);
            }
            Log.i(TAG, String.format("Strength: %s", strengthStatus));
            Log.i(TAG, String.format("Quality: %s", qualityStatus));
            TextView channelInfo = findViewById(R.id.tv_scan_info);
            channelInfo.setText(String.format("%s\t\t%s\t\t", strengthStatus, qualityStatus));
        });
    }

    private void setTvChannelNumber(int number, boolean isAtv) {
        runOnUiThread(() -> {
            if (isAtv) {
                final TextView atvText = findViewById(R.id.atv_number);
                atvText.setText(String.format("\tATV :\t%s", number));
            } else {
                final TextView dtvText = findViewById(R.id.dtv_number);
                dtvText.setText(String.format("\tDTV :\t%s", number));
            }
        });
    }

    private void setSearchProgress(final int progress, final boolean isAtv) {
        runOnUiThread(() -> {
            final ProgressBar bar;
            if (isAtv) {
                bar = findViewById(R.id.atv_search_progress);
            } else {
                bar = findViewById(R.id.dtv_search_progress);
            }
            bar.setProgress(progress);
        });
    }

    private void setSearchProgressIndeterminate(final Boolean indeterminate) {
        runOnUiThread(() -> {
            final ProgressBar bar1 = findViewById(R.id.dtv_search_progress);
            bar1.setIndeterminate(indeterminate);
            final ProgressBar bar2 = findViewById(R.id.atv_search_progress);
            bar2.setIndeterminate(indeterminate);
        });
    }

    private boolean isAutoSearch() {
        return DataManager.VALUE_PUBLIC_SEARCH_MODE_AUTO == mSearchMode;
    }

    private void updateSearchButton(final boolean strStart) {
        runOnUiThread(() -> {
            if (strStart) {
                if (isAutoSearch()) {
                    mSearchButton.setText(R.string.strAutoSearch);
                } else {
                    mSearchButton.setText(R.string.strManualSearch);
                }
            } else {
                if (isAutoSearch() && mAntennaType == 0 && (!mIsHybridSearch) && mSearchingStage == SEARCH_STAGE.NOT_START) {
                    mSearchButton.setText(R.string.strSkip);
                } else {
                    mSearchButton.setText(R.string.strStopSearch);
                }
            }
        });
    }

    private int getFoundServiceNumber() {
        int found = 0;
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Atv.getNumberOfServices", new JSONArray());
            found = obj.getInt("data");
            Log.i(TAG, "getFoundServiceNumber found = " + found);
        } catch (Exception e) {
            Log.e(TAG, "getFoundServiceNumber Exception = " + e.getMessage());
        }
        return found;
    }

    private int[] getFoundServiceNumberOnSearch() {
        int[] found = {0, 0};
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.getCategoryNumberOfServices", new JSONArray());
            JSONObject data = obj.getJSONObject("data");
            found[0] = data.getInt("total_num");
            obj = DtvkitGlueClient.getInstance().request("Atv.getNumberOfServices", new JSONArray());
            found[1] = obj.getInt("data");
            Log.i(TAG, "getFoundServiceNumberOnSearch found = " + Arrays.toString(found));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return found;
    }

    private int[] getSearchProcess(JSONObject data) {
        int[] result = {-1, 0};
        if (data == null) {
            return result;
        }
        try {
            result[0] = data.getInt("progress");
            result[1] = data.optInt("number");
        } catch (JSONException e) {
            Log.e(TAG, "getSearchProcess Exception = " + e.getMessage());
        }
        return result;
    }

    private void sendOnSignal(final Map<String, Object> map) {
        if (mThreadHandler != null) {
            String signal = (String) map.get("signal");
            boolean valid;
            if (mIsHybridSearch) {
                valid = TextUtils.equals("TVStatusChanged", signal)
                        || TextUtils.equals("TVProgressChanged", signal);
            } else {
                valid = TextUtils.equals("IsdbtStatusChanged", signal)
                        || TextUtils.equals("AtvSearchProgress", signal);
            }
            if (valid) {
                mThreadHandler.removeMessages(MSG_ON_SIGNAL);
                Message mess = mThreadHandler.obtainMessage(MSG_ON_SIGNAL, 0, 0, map);
                boolean info = mThreadHandler.sendMessageDelayed(mess, 0);
                Log.d(TAG, "sendMessage MSG_ON_SIGNAL " + info);
            }
        }
    }

    private void dealOnSignal(final Map<String, Object> map) {
        Log.d(TAG, "dealOnSignal map = " + map);
        if (map == null) {
            Log.d(TAG, "dealOnSignal null map");
            return;
        }
        String signal = (String) map.get("signal");
        JSONObject data = (JSONObject) map.get("data");
        assert signal != null;
        assert data != null;
        int[] result = getSearchProcess(data);
        if (result[0] < 0) {
            return;
        }
        Log.d(TAG, "onSignal progress = " + result[0]);
        int strengthStatus = mParameterManager.getStrengthStatus();
        int qualityStatus = mParameterManager.getQualityStatus();
        boolean isAtvSignal = signal.toUpperCase().contains("ATV")
                || (signal.equals("TVProgressChanged") && data.optString("name").contains("atv_"));
        setSearchProgress(result[0], isAtvSignal);
        setStrengthAndQualityStatus(
                String.format(Locale.ENGLISH, "Strength: %d%%", strengthStatus),
                String.format(Locale.ENGLISH, "Quality: %d%%", qualityStatus));
        setSearchStatus(String.format(Locale.ENGLISH, "Searching (%d%%)", result[0]), "");
        if (mIsHybridSearch) {
            setTvChannelNumber(result[1], isAtvSignal);
        } else {
            int[] found = getFoundServiceNumberOnSearch();
            setTvChannelNumber(isAtvSignal ? found[1] : found[0], isAtvSignal);
        }
        if (result[0] == 100) {
            if (isAutoSearch()) {
                if (isAtvSignal) {
                    sendFinishSearch();
                } else {
                    /* continue atv search */
                    if (!mIsHybridSearch) {
                        sendStartSearch();
                    }
                }
            } else {
                sendFinishSearch();
            }
        }
    }

    private void sendFinish() {
        if (mThreadHandler != null) {
            mFinish = true;
            mThreadHandler.removeMessages(MSG_FINISH);
            Message mess = mThreadHandler.obtainMessage(MSG_FINISH, 0, 0, null);
            boolean info = mThreadHandler.sendMessageDelayed(mess, 0);
            Log.d(TAG, "sendMessage MSG_FINISH " + info);
        }
    }
}
