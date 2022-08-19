package com.droidlogic.dtvkit.inputsource;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.view.KeyEvent;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.util.TypedValue;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.droidlogic.fragment.ParameterManager;
import com.droidlogic.settings.ConstantManager;
import org.droidlogic.dtvkit.DtvkitGlueClient;
import com.droidlogic.dtvkit.inputsource.DataManager;

public class DtvkitIsdbtSetup extends Activity {
    private static final String TAG = "DtvkitIsdbtSetup";

    private boolean mIsDvbt = false;
    private DataManager mDataManager;
    private ParameterManager mParameterManager = null;
    private boolean mStartSync = false;
    private boolean mStartSearch = false;
    private boolean mFinish = false;
    private JSONArray mServiceList = null;
    private int mFoundServiceNumber = 0;
    private int mSearchManualAutoType = -1;// 0 manual 1 auto
    private int mSearchDvbcDvbtType = -1;
    private PvrStatusConfirmManager mPvrStatusConfirmManager = null;

    protected HandlerThread mHandlerThread = null;
    protected Handler mThreadHandler = null;

    private final static int MSG_START_SEARCH = 1;
    private final static int MSG_STOP_SEARCH = 2;
    private final static int MSG_FINISH_SEARCH = 3;
    private final static int MSG_ON_SIGNAL = 4;
    private final static int MSG_FINISH = 5;
    private final static int MSG_RELEASE= 6;

    private long clickLastTime;

    private final DtvkitGlueClient.SignalHandler mHandler = new DtvkitGlueClient.SignalHandler() {
        @Override
        public void onSignal(String signal, JSONObject data) {
            Map<String, Object> map = new HashMap<String,Object>();
            map.put("signal", signal);
            map.put("data", data);
            sendOnSignal(map);
        }
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
                //finish();
                sendFinish();
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mStartSync) {
            Toast.makeText(DtvkitIsdbtSetup.this, R.string.sync_tv_provider, Toast.LENGTH_SHORT).show();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mStartSearch) {
                //onSearchFinished();
                sendFinishSearch();
                return true;
            } else {
                stopMonitoringSearch();
                //stopSearch();
                sendStopSearch();
                //finish();
                sendFinish();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auto_setup);
        final String inputId;
        final String pvrStatus;
        Intent cIntent = getIntent();
        if (cIntent != null) {
            inputId = cIntent.getStringExtra(TvInputInfo.EXTRA_INPUT_ID);
            pvrStatus = cIntent.getStringExtra(ConstantManager.KEY_LIVETV_PVR_STATUS);
        } else {
            inputId = null;
            pvrStatus = null;
        }
        mParameterManager = new ParameterManager(this, DtvkitGlueClient.getInstance());
        final Button optionSet = findViewById(R.id.option_set_btn);
        optionSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentSet = new Intent();
                if (inputId != null) {
                    intentSet.putExtra(TvInputInfo.EXTRA_INPUT_ID, inputId);
                }
                String pvrFlag = PvrStatusConfirmManager.read(DtvkitIsdbtSetup.this, PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG);
                if (pvrStatus != null && PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG_FIRST.equals(pvrFlag)) {
                    intentSet.putExtra(ConstantManager.KEY_LIVETV_PVR_STATUS, pvrStatus);
                } else {
                    intentSet.putExtra(ConstantManager.KEY_LIVETV_PVR_STATUS, "");
                }
                intentSet.setClassName(DataManager.KEY_PACKAGE_NAME, DataManager.KEY_ACTIVITY_SETTINGS);
                startActivity(intentSet);
                mDataManager.saveIntParameters(DataManager.KEY_SELECT_SEARCH_ACTIVITY, DataManager.SELECT_SETTINGS);
            }
        });
        final View startSearch = findViewById(R.id.btn_terrestrial_start_search);
        EditText public_type_edit = (EditText)findViewById(R.id.edtTxt_public_type_in);

        startSearch.setEnabled(true);
        startSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                long currentTime = SystemClock.elapsedRealtime();
                if (currentTime - clickLastTime > 500) {
                    clickLastTime = currentTime;
                    int searchMode = mDataManager.getIntParameters(DataManager.KEY_PUBLIC_SEARCH_MODE);
                    boolean autoSearch = (DataManager.VALUE_PUBLIC_SEARCH_MODE_AUTO == searchMode);
                    mPvrStatusConfirmManager.setSearchType(autoSearch ? ConstantManager.KEY_DTVKIT_SEARCH_TYPE_AUTO : ConstantManager.KEY_DTVKIT_SEARCH_TYPE_MANUAL);
                    boolean checkPvr = mPvrStatusConfirmManager.needDeletePvrRecordings();
                    if (checkPvr) {
                        mPvrStatusConfirmManager.showDialogToAppoint(DtvkitIsdbtSetup.this, autoSearch);
                    } else {
                        if (mStartSearch) {
                            sendFinishSearch();
                        } else {
                            mPvrStatusConfirmManager.sendDvrCommand(DtvkitIsdbtSetup.this);
                            sendStartSearch();
                        }
                    }
                }
            }
        });
        startSearch.requestFocus();
        mDataManager = new DataManager(this);
        mPvrStatusConfirmManager = new PvrStatusConfirmManager(this, mDataManager);
        Intent intent = getIntent();
        if (intent != null) {
            mIsDvbt = intent.getBooleanExtra(DataManager.KEY_IS_DVBT, false);
            String status = intent.getStringExtra(ConstantManager.KEY_LIVETV_PVR_STATUS);
            mPvrStatusConfirmManager.setPvrStatus(status);
            Log.d(TAG, "onCreate mIsDvbt = " + mIsDvbt + ", status = " + status);
        }
        ((TextView)findViewById(R.id.dvb_search)).setText(R.string.strSearchIsdbtDescription);

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
                    s.insert(index, ".143");
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
        if (mFoundServiceNumber > 0) {
            Intent intent = new Intent();
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_TYPE_MANUAL_AUTO, mSearchManualAutoType);
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_TYPE_DVBS_DVBT_DVBC, mSearchDvbcDvbtType);
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_FOUND_SERVICE_NUMBER, mFoundServiceNumber);
            String serviceListJsonArray = (mServiceList != null && mServiceList.length() > 0) ? mServiceList.toString() : "";
            String firstServiceName = "";
            try {
                if (mServiceList != null && mServiceList.length() > 0) {
                    firstServiceName = mServiceList.getJSONObject(0).getString("name");
                    for (int i = 0; i < mServiceList.length(); i++) {
                        if (mServiceList.getJSONObject(i).getBoolean("hidden") == false) {
                            firstServiceName = mServiceList.getJSONObject(i).getString("name");
                            break;
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "finish JSONException = " + e.getMessage());
            }
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_FOUND_SERVICE_LIST, serviceListJsonArray);
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_FOUND_FIRST_SERVICE, firstServiceName);
            Log.d(TAG, "finish firstServiceName = " + firstServiceName);
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED);
        }
        super.finish();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (mStartSearch) {
            //onSearchFinished();
            sendFinishSearch();
        } else if (!mFinish) {
            stopMonitoringSearch();
            //stopSearch();
            sendStopSearch();
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
        mThreadHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Log.d(TAG, "mThreadHandler handleMessage " + msg.what + " start");
                switch (msg.what) {
                    case MSG_START_SEARCH: {
                        startSearch();
                        break;
                    }
                    case MSG_STOP_SEARCH: {
                        stopSearch();
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
            }
        });
    }

    private void sendRelease() {
        if (mThreadHandler != null) {
            mThreadHandler.removeMessages(MSG_RELEASE);
            Message mess = mThreadHandler.obtainMessage(MSG_RELEASE, 0, 0, null);
            boolean info = mThreadHandler.sendMessageDelayed(mess, 0);
            Log.d(TAG, "sendMessage MSG_RELEASE " + info);
        }
    }

    private void releaseInThread() {
        Log.d(TAG, "releaseInThread");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "releaseInThread start");
                releaseHandler();
                Log.d(TAG, "releaseInThread end");
            }
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
        LinearLayout public_type_in_container = (LinearLayout)findViewById(R.id.public_type_in_container);
        TextView dvb_search = (TextView)findViewById(R.id.dvb_search);
        View channel_holder = findViewById(R.id.channel_holder);
        TextView public_type_in = (TextView)findViewById(R.id.public_type_in_text);
        EditText public_type_edit = (EditText)findViewById(R.id.edtTxt_public_type_in);
        LinearLayout dvbt_bandwidth_container = (LinearLayout)findViewById(R.id.dvbt_bandwidth_container);
        Spinner dvbt_bandwidth_spinner = (Spinner)findViewById(R.id.dvbt_bandwidth_spinner);
        LinearLayout dvbt_mode_container = (LinearLayout)findViewById(R.id.dvbt_mode_container);
        Spinner dvbt_mode_spinner = (Spinner)findViewById(R.id.dvbt_mode_spinner);
        LinearLayout dvbt_type_container = (LinearLayout)findViewById(R.id.dvbt_type_container);
        Spinner dvbt_type_spinner = (Spinner)findViewById(R.id.dvbt_type_spinner);
        LinearLayout dvbc_mode_container = (LinearLayout)findViewById(R.id.dvbc_mode_container);
        Spinner dvbc_mode_spinner = (Spinner)findViewById(R.id.dvbc_mode_spinner);
        LinearLayout dvbc_symbol_container = (LinearLayout)findViewById(R.id.dvbc_symbol_container);
        EditText dvbc_symbol_edit = (EditText)findViewById(R.id.dvbc_symbol_edit);
        LinearLayout public_search_mode_container = (LinearLayout)findViewById(R.id.public_search_mode_container);
        Spinner public_search_mode_spinner = (Spinner)findViewById(R.id.public_search_mode_spinner);
        LinearLayout frequency_channel_container = (LinearLayout)findViewById(R.id.frequency_channel_container);
        Spinner frequency_channel_spinner = (Spinner)findViewById(R.id.frequency_channel_spinner);
        LinearLayout public_search_channel_name_container = (LinearLayout)findViewById(R.id.public_search_channel_container);
        Spinner public_search_channel_name_spinner = (Spinner)findViewById(R.id.public_search_channel_spinner);
        Button search = (Button)findViewById(R.id.btn_terrestrial_start_search);
        CheckBox nit = (CheckBox)findViewById(R.id.network);

        int isFrequencyMode = mDataManager.getIntParameters(DataManager.KEY_IS_FREQUENCY);
        if (isFrequencyMode == DataManager.VALUE_FREQUENCY_MODE) {
            public_type_in.setText(R.string.search_frequency);
            public_type_edit.setHint(R.string.search_frequency_hint);
            public_type_in_container.setVisibility(View.VISIBLE);
            public_search_channel_name_container.setVisibility(View.GONE);
        } else {
            //public_type_in.setText(R.string.search_number);
            //public_type_edit.setHint(R.string.search_number_hint);//not needed
            public_type_in_container.setVisibility(View.GONE);
            public_search_channel_name_container.setVisibility(View.VISIBLE);
            updateChannelNameContainer();
        }
        public_type_edit.setText("");
        int value = mDataManager.getIntParameters(DataManager.KEY_PUBLIC_SEARCH_MODE);
        public_search_mode_spinner.setSelection(value);
        nit.setChecked(mDataManager.getIntParameters(DataManager.KEY_NIT) == 1 ? true : false);
        if (DataManager.VALUE_PUBLIC_SEARCH_MODE_AUTO == value) {
            public_type_in_container.setVisibility(View.GONE);
            dvbt_bandwidth_container.setVisibility(View.GONE);
            dvbt_mode_container.setVisibility(View.GONE);
            dvbt_type_container.setVisibility(View.GONE);
            dvbc_mode_container.setVisibility(View.GONE);
            dvbc_symbol_container.setVisibility(View.GONE);
            frequency_channel_container.setVisibility(View.GONE);
            public_search_channel_name_container.setVisibility(View.GONE);
            nit.setVisibility(View.VISIBLE);
            search.setText(R.string.strAutoSearch);
        } else {
            search.setText(R.string.strManualSearch);
            if (isFrequencyMode == DataManager.VALUE_FREQUENCY_MODE) {
                public_type_in_container.setVisibility(View.VISIBLE);
                public_search_channel_name_container.setVisibility(View.GONE);
                nit.setVisibility(View.VISIBLE);
            } else {
                public_type_in_container.setVisibility(View.GONE);
                public_search_channel_name_container.setVisibility(View.VISIBLE);
                nit.setVisibility(View.GONE);
            }
            frequency_channel_container.setVisibility(View.VISIBLE);
            frequency_channel_spinner.setSelection(mDataManager.getIntParameters(DataManager.KEY_IS_FREQUENCY));
            if (mIsDvbt) {
                dvbt_bandwidth_container.setVisibility(View.VISIBLE);
                dvbt_mode_container.setVisibility(View.VISIBLE);
                dvbt_type_container.setVisibility(View.GONE);
                dvbc_symbol_container.setVisibility(View.GONE);
                dvbc_mode_container.setVisibility(View.GONE);
                dvbt_bandwidth_spinner.setSelection(1);
                dvbt_mode_spinner.setSelection(mDataManager.getIntParameters(DataManager.KEY_DVBT_MODE));
                dvbt_type_spinner.setSelection(mDataManager.getIntParameters(DataManager.KEY_DVBT_TYPE));
            } else {
                dvbt_bandwidth_container.setVisibility(View.GONE);
                dvbt_mode_container.setVisibility(View.GONE);
                dvbt_type_container.setVisibility(View.GONE);
                dvbc_symbol_container.setVisibility(View.VISIBLE);
                dvbc_mode_container.setVisibility(View.VISIBLE);
                dvbc_mode_spinner.setSelection(mDataManager.getIntParameters(DataManager.KEY_DVBC_MODE));
                dvbc_symbol_edit.setText(mDataManager.getIntParameters(DataManager.KEY_DVBC_SYMBOL_RATE) + "");
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
            dvbt_type_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (mDataManager.getIntParameters(DataManager.KEY_DVBT_TYPE) == position) {
                        Log.d(TAG, "dvbt_type_spinner same position = " + position);
                        return;
                    }
                    Log.d(TAG, "dvbt_type_spinner onItemSelected position = " + position);
                    mDataManager.saveIntParameters(DataManager.KEY_DVBT_TYPE, position);
                    initOrUpdateView(false);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            dvbc_mode_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    Log.d(TAG, "dvbc_mode_spinner onItemSelected position = " + position);
                    mDataManager.saveIntParameters(DataManager.KEY_DVBC_MODE, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            public_search_mode_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == mDataManager.getIntParameters(DataManager.KEY_PUBLIC_SEARCH_MODE)) {
                        Log.d(TAG, "public_search_mode_spinner select same position = " + position);
                        return;
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
                    if (mIsDvbt) {
                        mDataManager.saveIntParameters(DataManager.KEY_SEARCH_DVBT_CHANNEL_NAME, position);
                    } else {
                        mDataManager.saveIntParameters(DataManager.KEY_SEARCH_DVBC_CHANNEL_NAME, position);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // TODO Auto-generated method stub
                }
            });
            nit.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (nit.isChecked()) {
                        mDataManager.saveIntParameters(DataManager.KEY_NIT, 1);
                    } else {
                        mDataManager.saveIntParameters(DataManager.KEY_NIT, 0);
                    }
                }
            });
        }
    }

    private void updateChannelNameContainer() {
        LinearLayout public_search_channel_name_container = (LinearLayout)findViewById(R.id.public_search_channel_container);
        Spinner public_search_channel_name_spinner = (Spinner)findViewById(R.id.public_search_channel_spinner);

        JSONArray list = null;
        List<String> newList = new ArrayList<String>();
        ArrayAdapter<String> adapter = null;
        int select = mDataManager.getIntParameters(DataManager.KEY_SEARCH_ISDBT_CHANNEL_NAME);
        int county_code_Brazil = ('b' << 16 | 'r' << 8 | 'a') & 0xFFFFFF;
        list = mParameterManager.getIsdbtChannelTable(county_code_Brazil);

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
                StringBuilder builder =
                    new StringBuilder("NO." + channelNumber + " " + name + " " + freq + "Hz");
                newList.add(builder.toString());
            }
        } catch (Exception e) {
            Log.d(TAG, "got invalid channel freq table");
        }

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, newList);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        public_search_channel_name_spinner.setAdapter(adapter);
        select = (select < list.length()) ? select : 0;
        public_search_channel_name_spinner.setSelection(select);
    }

    private JSONArray initSearchParameter(JSONArray args) {
        if (args != null) {
            if (!(DataManager.VALUE_PUBLIC_SEARCH_MODE_AUTO == mDataManager.getIntParameters(DataManager.KEY_PUBLIC_SEARCH_MODE))) {
                String parameter = getParameter();
                if (!TextUtils.isEmpty(parameter)) {
                    int isFrequencySearch = mDataManager.getIntParameters(DataManager.KEY_IS_FREQUENCY);
                    if (DataManager.VALUE_FREQUENCY_MODE == isFrequencySearch) {
                        args.put(mDataManager.getIntParameters(DataManager.KEY_NIT) > 0);
                        args.put(Integer.valueOf(parameter) * 1000);//khz to hz
                        args.put(DataManager.VALUE_DVBT_BANDWIDTH_LIST[mDataManager.getIntParameters(DataManager.KEY_DVBT_BANDWIDTH)]);
                        args.put(DataManager.VALUE_DVBT_MODE_LIST[mDataManager.getIntParameters(DataManager.KEY_DVBT_MODE)]);
                    } else {
                        parameter = getChannelIndex();
                        if (parameter == null) {
                            Log.d(TAG, "initSearchParameter Isdbt search can't find channel index");
                            return null;
                        }
                        args.put(Integer.valueOf(parameter));
                    }
                    return args;
                } else {
                    return null;
                }
            } else {
                args.put(mDataManager.getIntParameters(DataManager.KEY_NIT) > 0);
                return args;
            }
        } else {
            return null;
        }
    }

    private String getParameter() {
        String parameter = null;
        EditText public_type_edit = (EditText)findViewById(R.id.edtTxt_public_type_in);
        Editable editable = public_type_edit.getText();
        int isFrequencySearch = mDataManager.getIntParameters(DataManager.KEY_IS_FREQUENCY);
        if (DataManager.VALUE_FREQUENCY_MODE != isFrequencySearch) {
            parameter = getChannelIndex();
        } else if (editable != null) {
            String value = editable.toString();
            if (!TextUtils.isEmpty(value)/* && TextUtils.isDigitsOnly(value)*/) {
                //float for frequency
                float toFloat = Float.valueOf(value);
                int toInt = (int)(toFloat * 1000.0f);//khz
                parameter = String.valueOf(toInt);
            }
        }

        return parameter;
    }

    private int getUpdatedDvbcSymbolRate() {
        int parameter = DataManager.VALUE_DVBC_SYMBOL_RATE;
        EditText symbolRate = (EditText)findViewById(R.id.dvbc_symbol_edit);
        Editable editable = symbolRate.getText();
        int isFrequencySearch = mDataManager.getIntParameters(DataManager.KEY_DVBC_SYMBOL_RATE);
        if (editable != null) {
            String value = editable.toString();
            if (!TextUtils.isEmpty(value) && TextUtils.isDigitsOnly(value)) {
                parameter = Integer.valueOf(value);
                mDataManager.saveIntParameters(DataManager.KEY_DVBC_SYMBOL_RATE, parameter);
            }
        } else {
            mDataManager.saveIntParameters(DataManager.KEY_DVBC_SYMBOL_RATE, DataManager.VALUE_DVBC_SYMBOL_RATE);
        }

        return parameter;
    }

    private String getChannelIndex() {
        String result = null;
        JSONArray list = null;
        JSONObject channelInfo = null;

        int index = mIsDvbt ? mDataManager.getIntParameters(DataManager.KEY_SEARCH_DVBT_CHANNEL_NAME) :
                mDataManager.getIntParameters(DataManager.KEY_SEARCH_DVBC_CHANNEL_NAME);
        int county_code_Brazil = ('b' << 16 | 'r' << 8 | 'a') & 0xFFFFFF;
        list = mParameterManager.getIsdbtChannelTable(county_code_Brazil);

        try {
            channelInfo = (index < list.length()) ? (JSONObject)(list.get(index)) : null;
        } catch (Exception e) {
        }
        if (channelInfo != null) {
            result = "" + channelInfo.optInt("index");
            Log.d(TAG, "getChannelIndex channel index = " +result +
                ", name = " + channelInfo.optString("name", "ch" +result) +
                ", freq = " + channelInfo.optInt("freq", 0)/1000 + "kHz");
        }

        return result;
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
        try {
            JSONArray args = new JSONArray();
            args.put(false); // Commit
            DtvkitGlueClient.getInstance().request("Isdbt.finishSearch", args);
        } catch (Exception e) {
            setSearchStatus("Failed to finish search", e.getMessage());
            return;
        }

        try {
            JSONArray args = new JSONArray();
            args.put(true); // retune
            args = initSearchParameter(args);
            if (args != null) {
                String command = null;
                int searchMode = mDataManager.getIntParameters(DataManager.KEY_PUBLIC_SEARCH_MODE);
                int isFrequencySearch = mDataManager.getIntParameters(DataManager.KEY_IS_FREQUENCY);
                if (!(DataManager.VALUE_PUBLIC_SEARCH_MODE_AUTO == searchMode)) {
                    if (isFrequencySearch == DataManager.VALUE_FREQUENCY_MODE) {
                        command = ("Isdbt.startManualSearchByFreq");
                    } else {
                        command = ("Isdbt.startManualSearchById");
                    }
                    mSearchManualAutoType = DtvkitDvbScanSelect.SEARCH_TYPE_MANUAL;
                } else {
                    command = ("Isdbt.startSearch");
                    mSearchManualAutoType = DtvkitDvbScanSelect.SEARCH_TYPE_AUTO;
                }
                if (mIsDvbt) {
                    mSearchDvbcDvbtType = DtvkitDvbScanSelect.SEARCH_TYPE_DVBT;
                } else {
                    mSearchDvbcDvbtType = DtvkitDvbScanSelect.SEARCH_TYPE_DVBC;
                }
                Log.d(TAG, "command = " + command + ", args = " + args.toString());
                DtvkitGlueClient.getInstance().request(command, args);
                mStartSearch = true;
                mParameterManager.saveChannelIdForSource(-1);
            } else {
                stopMonitoringSearch();
                setSearchStatus("parameter not complete", "");
                stopSearch();
            }
        } catch (Exception e) {
            stopMonitoringSearch();
            setSearchStatus("Failed to start search", e.getMessage());
            stopSearch();
        }
    }

    private void sendStopSearch() {
        if (mThreadHandler != null) {
            mThreadHandler.removeMessages(MSG_STOP_SEARCH);
            Message mess = mThreadHandler.obtainMessage(MSG_STOP_SEARCH, 0, 0, null);
            boolean info = mThreadHandler.sendMessageDelayed(mess, 0);
            Log.d(TAG, "sendMessage MSG_STOP_SEARCH " + info);
        }
    }

    private void stopSearch() {
        mStartSearch = false;
        updateSearchButton(true);
        try {
            JSONArray args = new JSONArray();
            args.put(true); // Commit
            DtvkitGlueClient.getInstance().request("Isdbt.finishSearch", args);
        } catch (Exception e) {
            setSearchStatus("Failed to finish search", e.getMessage());
            return;
        }
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
        runOnUiThread(() -> findViewById(R.id.btn_terrestrial_start_search).setEnabled(false));
        updateSearchButton(true);
        setSearchStatus("Finishing search", "");
        setStrengthAndQualityStatus("","");
        setSearchProgressIndeterminate(true);
        stopMonitoringSearch();
        try {
            JSONArray args = new JSONArray();
            args.put(true); // Commit
            DtvkitGlueClient.getInstance().request("Isdbt.finishSearch", args);
        } catch (Exception e) {
            setSearchStatus("Failed to finish search", e.getMessage());
            return;
        }
        //update search results as After the search is finished, the lcn will be reordered
        mServiceList = getServiceList();
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
        int searchMode = mDataManager.getIntParameters(DataManager.KEY_PUBLIC_SEARCH_MODE);
        int isFrequencySearch = mDataManager.getIntParameters(DataManager.KEY_IS_FREQUENCY);
        if (DataManager.VALUE_FREQUENCY_MODE == isFrequencySearch && DataManager.VALUE_PUBLIC_SEARCH_MODE_AUTO != searchMode) {
            parameters.putInt(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_FREQUENCY, Integer.valueOf(getParameter()) * 1000);
        }
        parameters.putString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_MODE, DataManager.VALUE_PUBLIC_SEARCH_MODE_AUTO == searchMode ? EpgSyncJobService.BUNDLE_VALUE_SYNC_SEARCHED_MODE_AUTO : EpgSyncJobService.BUNDLE_VALUE_SYNC_SEARCHED_MODE_MANUAL);
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, String.format("Search status \"%s\"", status));
                final TextView text = (TextView) findViewById(R.id.tv_search_status);
                text.setText(String.format("%s\t%s",status,description));
            }
        });
    }

    private void setStrengthAndQualityStatus(final String strengthStatus, final String qualityStatus) {
        setStrengthAndQualityStatus(strengthStatus, qualityStatus, "");
    }

    private void setStrengthAndQualityStatus(final String strengthStatus, final String qualityStatus, final String channel) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (TextUtils.isEmpty(strengthStatus) && TextUtils.isEmpty(qualityStatus)) {
                    findViewById(R.id.channel_holder).setVisibility(View.GONE);
                } else {
                    findViewById(R.id.channel_holder).setVisibility(View.VISIBLE);
                }
                Log.i(TAG, String.format("Strength: %s", strengthStatus));
                Log.i(TAG, String.format("Quality: %s", qualityStatus));
                TextView channelInfo = (TextView) findViewById(R.id.tv_scan_info);
                channelInfo.setText(strengthStatus + "\t\t" + qualityStatus + "\t\t" + channel);
            }
        });
    }

    private void setSearchProgress(final int progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ProgressBar bar = (ProgressBar) findViewById(R.id.proBar_search_progress);
                bar.setProgress(progress);
            }
        });
    }

    private void setSearchProgressIndeterminate(final Boolean indeterminate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final ProgressBar bar = (ProgressBar) findViewById(R.id.proBar_search_progress);
                bar.setIndeterminate(indeterminate);
            }
        });
    }

    private boolean isAutoSearch() {
        return DataManager.VALUE_PUBLIC_SEARCH_MODE_AUTO == mDataManager.getIntParameters(DataManager.KEY_PUBLIC_SEARCH_MODE);
    }

    private void updateSearchButton(final boolean strStart) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((Button) findViewById(R.id.btn_terrestrial_start_search)).setText(strStart ? (isAutoSearch() ? R.string.strStartSearch : R.string.strManualSearch) : R.string.strStopSearch);
            }
        });
    }

    private int getFoundServiceNumber() {
        int found = 0;
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.getNumberOfServices", new JSONArray());
            found = obj.getInt("data");
            Log.i(TAG, "getFoundServiceNumber found = " + found);
        } catch (Exception ignore) {
            Log.e(TAG, "getFoundServiceNumber Exception = " + ignore.getMessage());
        }
        return found;
    }

    private int getFoundServiceNumberOnSearch() {
        int found = 0;
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.getCategoryNumberOfServices", new JSONArray());
            JSONObject datas = obj.getJSONObject("data");
            found = datas.getInt("total_num");
            Log.i(TAG, "getFoundServiceNumberOnSearch found = " + found);
        } catch (Exception ignore) {
            Log.e(TAG, "getFoundServiceNumberOnSearch Exception = " + ignore.getMessage());
        }
        return found;
    }

    private int getSearchProcess(JSONObject data) {
        int progress = 0;
        if (data == null) {
            return progress;
        }
        try {
            //Log.e("SearchProcess", "" + data.toString());
            progress = data.getInt("progress");
        } catch (JSONException ignore) {
            Log.e(TAG, "getSearchProcess Exception = " + ignore.getMessage());
        }
        return progress;
    }

    private JSONArray getServiceList() {
        JSONArray result = null;
        try {
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.getListOfServices", new JSONArray());
            JSONArray services = obj.getJSONArray("data");
            result = services;
            for (int i = 0; i < services.length(); i++) {
                JSONObject service = services.getJSONObject(i);
                //Log.i(TAG, "getServiceList service = " + service.toString());
            }

        } catch (Exception e) {
            Log.e(TAG, "getServiceList Exception = " + e.getMessage());
        }
        return result;
    }

    private void sendOnSignal(final Map<String, Object> map) {
        if (mThreadHandler != null) {
            mThreadHandler.removeMessages(MSG_ON_SIGNAL);
            Message mess = mThreadHandler.obtainMessage(MSG_ON_SIGNAL, 0, 0, map);
            boolean info = mThreadHandler.sendMessageDelayed(mess, 0);
            Log.d(TAG, "sendMessage MSG_ON_SIGNAL " + info);
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
        if (signal != null && signal.equals("IsdbtStatusChanged")) {
            int progress = getSearchProcess(data);
            Log.d(TAG, "onSignal progress = " + progress);
            int strengthStatus = mParameterManager.getStrengthStatus();
            int qualityStatus = mParameterManager.getQualityStatus();
            if (progress < 100) {
                int found = getFoundServiceNumberOnSearch();
                setSearchProgress(progress);
                setSearchStatus(String.format(Locale.ENGLISH, "Searching (%d%%)", progress), "");
                setStrengthAndQualityStatus(String.format(Locale.ENGLISH, "Strength: %d%%", strengthStatus), String.format(Locale.ENGLISH, "Quality: %d%%", qualityStatus), String.format(Locale.ENGLISH, "Channel: %d", found));
            } else {
                //onSearchFinished();
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
