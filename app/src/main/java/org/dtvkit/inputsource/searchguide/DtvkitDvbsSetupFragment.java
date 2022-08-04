package com.droidlogic.dtvkit.inputsource.searchguide;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.tv.TvInputInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.droidlogic.app.DataProviderManager;
import com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService;
import com.droidlogic.dtvkit.inputsource.DataMananer;
import com.droidlogic.dtvkit.inputsource.DtvkitDvbScanSelect;
import com.droidlogic.dtvkit.inputsource.DtvkitEpgSync;
import com.droidlogic.dtvkit.inputsource.PvrStatusConfirmManager;
import com.droidlogic.dtvkit.inputsource.R;
import com.droidlogic.fragment.DvbsParameterManager;
import com.droidlogic.fragment.ParameterMananer;
import com.droidlogic.settings.ConstantManager;

import com.droidlogic.dtvkit.inputsource.searchguide.DataPresenter;
import com.droidlogic.dtvkit.inputsource.searchguide.DtvkitDvbsSetupFragment;
import com.droidlogic.dtvkit.inputsource.searchguide.OnNextListener;
import com.droidlogic.dtvkit.inputsource.searchguide.SearchStageFragment;
import com.droidlogic.dtvkit.inputsource.searchguide.SimpleListFragment;

import org.droidlogic.dtvkit.DtvkitGlueClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

public class DtvkitDvbsSetupFragment extends SearchStageFragment {
    private final String TAG = DtvkitDvbsSetupFragment.class.getSimpleName();
    private static final int REQUEST_CODE_SET_UP_SETTINGS = 1;
    private DataMananer mDataManager;
    private boolean mStartSync = false;
    private boolean mStartSearch = false;
    private int mFoundServiceNumber = 0;
    private JSONArray mServiceList = null;
    private int mSearchType = -1;// 0 manual 1 auto
    private int mSearchDvbsType = -1;
    private PvrStatusConfirmManager mPvrStatusConfirmManager = null;
    private ParameterMananer mParameterManager;

    protected HandlerThread mHandlerThread = null;
    protected Handler mThreadHandler = null;

    private final static int MSG_FINISH_SEARCH = 1;
    private final static int MSG_ON_SIGNAL = 2;

    private long clickLastTime;
    private final String inputId = com.droidlogic.dtvkit.inputsource.service.DtvkitSettingService.DTVKIT_INPUTID;;
    private String pvrStatus;

    // UI to Update
    private LinearLayout mChannelHolder;
    private TextView mSearchStatus;
    private Button mSetup;
    private Button mSearch;
    private TextView mChannelInfo;
    private ProgressBar mSearchProgress;

    private final DtvkitGlueClient.SignalHandler mHandler = new DtvkitGlueClient.SignalHandler() {
        @Override
        public void onSignal(String signal, JSONObject data) {
            Log.d(TAG, "onSignal = " + signal + ", " + data);
            if (signal.equals("DvbsStatusChanged")) {
                int progress = getSearchProcess(data);
                Log.d(TAG, "onSignal progress = " + progress);
                if (mThreadHandler != null) {
                    mThreadHandler.removeMessages(MSG_ON_SIGNAL);
                    Message msg = mThreadHandler.obtainMessage(MSG_ON_SIGNAL, progress, 0, null);
                    boolean info = mThreadHandler.sendMessageDelayed(msg, 0);
                }
            } else if (signal.equals("DiseqcConfirmRequired")) {
                if (getListener() != null) {
                    getListener().onNext(DtvkitDvbsSetupFragment.this, DataPresenter.FRAGMENT_MANUAL_DISEQC);
                }
            } else if (signal.equals("M7SelectOperatorRequired")) {
                if (getListener() != null) {
                    getListener().onNext(DtvkitDvbsSetupFragment.this, DataPresenter.FRAGMENT_OPERATOR_LIST);
                }
            } else if (signal.equals("M7SelectSublistRequired")) {
                if (getListener() != null) {
                    getListener().onNext(DtvkitDvbsSetupFragment.this, DataPresenter.FRAGMENT_OPERATOR_SUB_LIST);
                }
            } else if (signal.equals("M7SelectRegionRequired")) {
                if (getListener() != null) {
                    getListener().onNext(DtvkitDvbsSetupFragment.this, DataPresenter.FRAGMENT_REGIONAL_CHANNELS);
                }
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            String status = intent.getStringExtra(EpgSyncJobService.SYNC_STATUS);
            if (status.equals(EpgSyncJobService.SYNC_FINISHED)) {
                updateSearchUi(true, true, "Finished");
                mStartSync = false;
                finish();
            }
        }
    };

    public static DtvkitDvbsSetupFragment newInstance(String title) {
        DtvkitDvbsSetupFragment fragment = new DtvkitDvbsSetupFragment();
        fragment.setTitle(title);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataManager = new DataMananer(getActivity().getApplicationContext());
        mPvrStatusConfirmManager = new PvrStatusConfirmManager(getActivity().getApplicationContext(), mDataManager);
        mParameterManager = new ParameterMananer(getActivity().getApplicationContext(), DtvkitGlueClient.getInstance());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initHandler();
        Intent cIntent = getActivity().getIntent();
        if (cIntent != null) {
            pvrStatus = cIntent.getStringExtra(ConstantManager.KEY_LIVETV_PVR_STATUS);
            mPvrStatusConfirmManager.setPvrStatus(pvrStatus);
            Log.d(TAG, "onCreate pvr status = " + pvrStatus);
        } else {
            pvrStatus = null;
        }
//        if (isM7Spec()) {
//            updateSearchUi(false, false,"Searching");
//            mThreadHandler.postDelayed(this::startSearch, 1000);
//        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.satsetup, container, false);
        final Button optionSet = view.findViewById(R.id.option_set_btn);
        optionSet.setOnClickListener(v -> {
            Intent intentSet = new Intent();
            if (inputId != null) {
                intentSet.putExtra(TvInputInfo.EXTRA_INPUT_ID, inputId);
            }
            String pvrFlag = PvrStatusConfirmManager.read(getActivity(), PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG);
            if (pvrStatus != null && PvrStatusConfirmManager.KEY_PVR_CLEAR_FLAG_FIRST.equals(pvrFlag)) {
                intentSet.putExtra(ConstantManager.KEY_LIVETV_PVR_STATUS, pvrStatus);
            } else {
                intentSet.putExtra(ConstantManager.KEY_LIVETV_PVR_STATUS, "");
            }
            intentSet.setClassName(DataMananer.KEY_PACKAGE_NAME, DataMananer.KEY_ACTIVITY_SETTINGS);
            startActivity(intentSet);
            mDataManager.saveIntParameters(DataMananer.KEY_SELECT_SEARCH_ACTIVITY, DataMananer.SELECT_SETTINGS);
        });

        mChannelHolder = view.findViewById(R.id.channel_holder);
        mSearchStatus = view.findViewById(R.id.searchstatus);
        mSetup = view.findViewById(R.id.setup);
        mSearch = view.findViewById(R.id.startsearch);
        mChannelInfo = view.findViewById(R.id.tv_scan_info);
        mSearchProgress = view.findViewById(R.id.searchprogress);

        final Button search = (Button) view.findViewById(R.id.startsearch);
        final Button setup = (Button) view.findViewById(R.id.setup);
        final Button importSatellite = (Button) view.findViewById(R.id.import_satellite);
        search.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                long currentTime = SystemClock.elapsedRealtime();
                if (currentTime - clickLastTime > 500) {
                    clickLastTime = currentTime;
                    mPvrStatusConfirmManager.setSearchType(ConstantManager.KEY_DTVKIT_SEARCH_TYPE_MANUAL);
                    boolean checkPvr = mPvrStatusConfirmManager.needDeletePvrRecordings();
                    if (checkPvr) {
                        mPvrStatusConfirmManager.showDialogToAppoint(getActivity(), false);
                    } else {
                        if (mStartSearch) {
                            sendFinishSearch(true);
                            updateSearchUi(true, true, "Finishing search");
                        } else {
                            mPvrStatusConfirmManager.sendDvrCommand(getActivity());
                            sendFinishSearch(false); // stop before start
                            updateSearchUi(false, false, "Searching");
                            startSearch();
                        }
                    }
                }
            }
        });
        search.requestFocus();

        setup.setOnClickListener(v -> {
            mPvrStatusConfirmManager.setSearchType(ConstantManager.KEY_DTVKIT_SEARCH_TYPE_MANUAL);
            boolean checkPvr = mPvrStatusConfirmManager.needDeletePvrRecordings();
            if (checkPvr) {
                mPvrStatusConfirmManager.showDialogToAppoint(getActivity(), false);
            } else {
                mPvrStatusConfirmManager.sendDvrCommand(getActivity());
                setUp();
            }
        });

        //ui set visibility gone as will be excuted after first boot
        importSatellite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mParameterManager.importDatabase(ConstantManager.DTVKIT_SATELLITE_DATA);
            }
        });

        TextView dvb_search = (TextView) view.findViewById(R.id.dvb_search);
        View channel_holder = view.findViewById(R.id.channel_holder);

        CheckBox nit = (CheckBox) view.findViewById(R.id.network);
        CheckBox clear = (CheckBox) view.findViewById(R.id.clear_old);
        CheckBox dvbs2 = (CheckBox) view.findViewById(R.id.dvbs2);
        Spinner searchmode = (Spinner) view.findViewById(R.id.search_mode);
        Spinner fecmode = (Spinner) view.findViewById(R.id.fec_mode);
        Spinner modulationmode = (Spinner) view.findViewById(R.id.modulation_mode);
        final LinearLayout blindContainer = (LinearLayout) view.findViewById(R.id.blind_frequency_container);
        EditText edit_start_freq = (EditText) view.findViewById(R.id.edit_start_freq);
        EditText edit_end_freq = (EditText) view.findViewById(R.id.edit_end_freq);
        edit_start_freq.setText(String.valueOf(DataMananer.VALUE_BLIND_DEFAULT_START_FREQUENCY));
        edit_end_freq.setText(String.valueOf(DataMananer.VALUE_BLIND_DEFAULT_END_FREQUENCY));

        nit.setChecked(mDataManager.getIntParameters(DataMananer.KEY_DVBS_NIT) == 1);
        clear.setChecked(mDataManager.getIntParameters(DataMananer.KEY_CLEAR) == 1);
        dvbs2.setChecked(mDataManager.getIntParameters(DataMananer.KEY_DVBS2) == 1);
        nit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (nit.isChecked()) {
                    mDataManager.saveIntParameters(DataMananer.KEY_DVBS_NIT, 1);
                } else {
                    mDataManager.saveIntParameters(DataMananer.KEY_DVBS_NIT, 0);
                }
            }
        });
        clear.setOnClickListener(v -> {
            if (clear.isChecked()) {
                mDataManager.saveIntParameters(DataMananer.KEY_CLEAR, 1);
            } else {
                mDataManager.saveIntParameters(DataMananer.KEY_CLEAR, 0);
            }
        });
        dvbs2.setOnClickListener(v -> {
            if (dvbs2.isChecked()) {
                mDataManager.saveIntParameters(DataMananer.KEY_DVBS2, 1);
            } else {
                mDataManager.saveIntParameters(DataMananer.KEY_DVBS2, 0);
            }
        });
        searchmode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "searchmode onItemSelected position = " + position);
                mDataManager.saveIntParameters(DataMananer.KEY_SEARCH_MODE, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        fecmode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "fecmode onItemSelected position = " + position);
                mDataManager.saveIntParameters(DataMananer.KEY_FEC_MODE, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        modulationmode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "modulationmode onItemSelected position = " + position);
                mDataManager.saveIntParameters(DataMananer.KEY_MODULATION_MODE, position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
        int searchmodeValue = mDataManager.getIntParameters(DataMananer.KEY_SEARCH_MODE);
        searchmode.setSelection(searchmodeValue);

        fecmode.setSelection(mDataManager.getIntParameters(DataMananer.KEY_FEC_MODE));
        modulationmode.setSelection(mDataManager.getIntParameters(DataMananer.KEY_MODULATION_MODE));
        Spinner channelTypeSpinner = (Spinner) view.findViewById(R.id.channel_type);
        channelTypeSpinner.setSelection(getCurrentChannelType());
        channelTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                updateChannelType(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        Spinner serviceTypeSpinner = (Spinner) view.findViewById(R.id.service_type);
        serviceTypeSpinner.setSelection(getCurrentServiceType());
        serviceTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                updateServiceType(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        if (isM7Spec()) {
            // auto start
            Log.d(TAG, "disable UI widget");
            nit.setEnabled(false);
            optionSet.setEnabled(false);
            setup.setEnabled(false);
            searchmode.setEnabled(false);
            fecmode.setEnabled(false);
            serviceTypeSpinner.setEnabled(false);
            channelTypeSpinner.setEnabled(false);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        releaseHandler();
        stopMonitoringSync();
    }

    private void initHandler() {
        Log.d(TAG, "initHandler");
        mHandlerThread = new HandlerThread("DtvkitDvbtSetup");
        mHandlerThread.start();
        mThreadHandler = new Handler(mHandlerThread.getLooper(), msg -> {
            Log.d(TAG, "mThreadHandler handleMessage " + msg.what + " start");
            switch (msg.what) {
                case MSG_FINISH_SEARCH: {
                    updateSearchUi(true, true, "Finishing search");
                    onSearchFinished();
                    break;
                }
                case MSG_ON_SIGNAL: {
                    dealOnSignal(msg.arg1);
                    break;
                }
                default:
                    break;
            }
            Log.d(TAG, "mThreadHandler handleMessage " + msg.what + " over");
            return true;
        });
    }

    public boolean handleKeyDown(int keyCode, KeyEvent event) {
        if (mStartSync) {
            Toast.makeText(getActivity(), R.string.sync_tv_provider, Toast.LENGTH_SHORT).show();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            updateSearchUi(true, true, "Finishing search");
            if (mStartSearch) {
                sendFinishSearch(false);
            } else {
                finish();
            }
            return true;
        }
        return false;
    }

    private void updateSearchUi(boolean stop, boolean progressBarUi, String reason) {
        if (getActivity() != null) {
            updateSearchButton(stop);
            enableSetupButton(stop);
            setSearchStatus(reason);
            setStrengthAndQualityStatus("", "");
            setProgressBar(progressBarUi);
        }
    }

    private void dealOnSignal(int progress) {
        Log.d(TAG, "onSignal progress = " + progress);
        int found = getFoundServiceNumberOnSearch();
        setSearchProgress(progress);
        int sstatus = mParameterManager.getStrengthStatus();
        int qstatus = mParameterManager.getQualityStatus();
        setSearchStatus(String.format(Locale.ENGLISH, "Searching (%d%%)", progress), "");
        setStrengthAndQualityStatus(String.format(Locale.getDefault(), "Strength: %d%%", sstatus), String.format(Locale.getDefault(), "Quality: %d%%", qstatus), String.format(Locale.getDefault(), "Channel: %d", found));
        if (progress >= 100) {
            sendFinishSearch(true);
        }
    }

    private void sendFinishSearch(boolean sync) {
        String ret = "true";
        mStartSearch = false;
        stopMonitoringSearch();
        try {
            JSONArray args = new JSONArray();
            args.put(true); // Commit
            DtvkitGlueClient.getInstance().request("Dvbs.finishSearch", args);
        } catch (Exception e) {
            ret = e.getMessage();
        } finally {
            if (sync && mThreadHandler != null) {
                mThreadHandler.removeMessages(MSG_FINISH_SEARCH);
                Message mess = mThreadHandler.obtainMessage(MSG_FINISH_SEARCH, 0, 0, ret);
                mThreadHandler.sendMessageDelayed(mess, 1000);
            }
        }
    }

    private void setSearchProgress(final int progress) {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(() -> {
            final ProgressBar bar = (ProgressBar) getActivity().findViewById(R.id.searchprogress);
            bar.setProgress(progress);
        });
    }

    private void releaseHandler() {
        Log.d(TAG, "releaseHandler");
        if (mThreadHandler != null) {
            mThreadHandler.removeCallbacksAndMessages(null);
        }
        if (mHandlerThread != null) {
            mHandlerThread.getLooper().quitSafely();
        }
        mThreadHandler = null;
        mHandlerThread = null;
    }

    private int getFilterServiceTypeInSearch() {
        int ret = 0;
        try {
            JSONArray array = new JSONArray();
            array.put(ParameterMananer.SIGNAL_QPSK);
            JSONObject obj = DtvkitGlueClient.getInstance().request("Dvb.GetFilterServiceTypeInSearch", array);
            if (obj != null) {
                ret = obj.optInt("data", 0);
            }
        } catch (Exception e) {
            Log.w(TAG, "Dvb.GetFilterServiceTypeInSearch failed");
        }
        return ret;
    }

    private void SetFilterServiceTypeInSearch(int type) {
        if (type == 0x1f || type == 0x18 || type == 0x07) {
            //filter all, just equals no filter
            type = 0;
        }
        try {
            JSONArray array = new JSONArray();
            array.put(ParameterMananer.SIGNAL_QPSK);
            array.put(type);
            DtvkitGlueClient.getInstance().request("Dvb.SetFilterServiceTypeInSearch", array);
        } catch (Exception e) {
            Log.w(TAG, "Dvb.SetFilterServiceTypeInSearch failed");
        }
    }

    /*
     * Filter Type Free channel: 0x01
     * Filter Type Scamble channel: 0x02
     * Filter Type network :        0x04 //not support in ui now
     * */
    private int getCurrentChannelType() {
        int ret = 0;//match ui type
        int filterType = getFilterServiceTypeInSearch();
        int channelType = filterType >> 3;
        if ((channelType & 0x03) == 0x03 || channelType == 0) {
            ret = 0;
        } else {
            ret = (channelType & 0x03);
        }
        return ret;
    }

    /*param range: 0 - 2*/
    private void updateChannelType(int type) {
        if (type == 0) {
            type = 0x03;
        }
        int filterType = getFilterServiceTypeInSearch();
        if (type != 7 && filterType == 0) {
            filterType = 0x1f;
        }
        SetFilterServiceTypeInSearch((filterType & 0x07) + (type << 3));
    }

    /*
     * Filter Type service tv: 0x01
     * Filter Type service radio: 0x02
     * Filter Type service other : 0x04 //not support in ui now
     * */
    private int getCurrentServiceType() {
        int ret = 0;
        int filterType = getFilterServiceTypeInSearch();
        int serviceType = filterType & 0x07;
        if ((serviceType & 0x03) == 0x03 || serviceType == 0) {
            ret = 0;
        } else {
            ret = (serviceType & 0x03);
        }
        return ret;
    }

    /*param range: 0 - 2*/
    private void updateServiceType(int type) {
        if (type == 0) {
            type = 0x07;
        }
        int filterType = getFilterServiceTypeInSearch();
        if (type != 7 && filterType == 0) {
            filterType = 0x1f;
        }
        SetFilterServiceTypeInSearch((filterType & 0x18) + type);
    }

    public void finish() {
        //send search info to livetv if found any
        Log.d(TAG, "finish");
        if (mFoundServiceNumber > 0) {
            Intent intent = new Intent();
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_TYPE_MANUAL_AUTO, mSearchType);
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_TYPE_DVBS_DVBT_DVBC, mSearchDvbsType);
            intent.putExtra(DtvkitDvbScanSelect.SEARCH_FOUND_SERVICE_NUMBER, mFoundServiceNumber);

            String firstServiceName = "";
            try {
                if (mServiceList != null && mServiceList.length() > 0) {
                    firstServiceName = mServiceList.getJSONObject(0).getString("name");
                    for (int i = 0; i < mServiceList.length(); i++) {
                        if (!mServiceList.getJSONObject(i).getBoolean("hidden")) {
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
            getActivity().setResult(RESULT_OK, intent);
        } else {
            getActivity().setResult(RESULT_CANCELED);
        }
        getActivity().finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        if (isM7Spec()) {
            updateSearchUi(false, false, "Searching");
            startSearch();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        mPvrStatusConfirmManager.registerCommandReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mPvrStatusConfirmManager.unRegisterCommandReceiver();
    }

    private void setUp() {
        if (getListener() != null) {
            getListener().onNext(DtvkitDvbsSetupFragment.this, DataPresenter.FRAGMENT_MANUAL_DISEQC);
        }
    }

    private JSONArray initSearchParameter() {
        JSONArray args = new JSONArray();
        if (isM7Spec()) {
            Bundle bundle = getArguments();
            args.put("m7fast");
            args.put(bundle != null && bundle.getBoolean("manualDiseqc"));
            args.put("fti");
        } else {
            /*[scanmode, network, {lnblist: [{lnb:1},{lnb:2},..]}]*/
            String searchmode = DataMananer.KEY_SEARCH_MODE_LIST[mDataManager.getIntParameters(DataMananer.KEY_SEARCH_MODE)];
            args.put(searchmode);//arg1
            args.put(mDataManager.getIntParameters(DataMananer.KEY_DVBS_NIT) == 1);//arg2
            List<String> lnbList = DvbsParameterManager.getInstance(getActivity()).getLnbWrap().getLnbIdList();
            JSONObject lnbArgs = new JSONObject();
            JSONArray lnbArgs_array = new JSONArray();
            Log.i(TAG, "initSearchParameter searchmode = " + searchmode);
            Log.i(TAG, "initSearchParameter lnbList = " + lnbList);
            try {
                for (String id : lnbList) {
                    JSONObject obj = new JSONObject();
                    if (DvbsParameterManager.getInstance(getActivity())
                            .getSatelliteNameListSelected(id).size() > 0) {
                        obj.put("lnb", id);
                        lnbArgs_array.put(obj);
                    }
                }
                Log.i(TAG, "initSearchParameter lnbArgs_array = " + lnbArgs_array);
                if (lnbArgs_array.length() > 0) {
                    lnbArgs.put("lnblist", lnbArgs_array);
                } else {
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
            args.put(lnbArgs.toString());//arg3
        }
        return args;
    }

    private void startSearch() {
        if (mStartSearch) {
            Log.w(TAG, "Search is going on");
            return;
        }
        try {
            JSONArray args = initSearchParameter();
            if (args != null) {
                Log.i(TAG, "search parameter:" + args.toString());
                //DtvkitGlueClient.getInstance().request("Dvbs.startSearch", args);
                //prevent ui not fresh on time
                doSearchByThread(args);
            } else {
                updateSearchUi(true, false, getString(R.string.invalid_parameter));
            }
        } catch (Exception e) {
            updateSearchUi(true, false, e.getMessage());
        }
    }

    private void doSearchByThread(final JSONArray args) {
        new Thread(() -> {
            try {
                startMonitoringSearch();
                mSearchDvbsType = DtvkitDvbScanSelect.SEARCH_TYPE_DVBS;
                DtvkitGlueClient.getInstance().request("Dvbs.startSearchEx", args);
                DataProviderManager.putBooleanValue(getActivity(), ConstantManager.KEY_IS_SEARCHING, true);
                mStartSearch = true;
                mFoundServiceNumber = 0;
                mServiceList = null;
                mParameterManager.saveChannelIdForSource(-1);
            } catch (Exception e) {
                updateSearchUi(true, false, e.getMessage());
                stopMonitoringSearch();
            }
        }).start();
    }

    private void enableSetupButton(boolean enable) {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(() -> {
            if (mSetup != null) {
                mSetup.setEnabled(enable);
            }
        });
    }

    private void updateSearchButton(boolean strStart) {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(() -> {
            if (mSearch != null) {
                mSearch.setText(strStart ? R.string.strStartSearch : R.string.strStopSearch);
            }
        });
    }

    private void onSearchFinished() {
        //update search results as After the search is finished, the lcn will be reordered
        mServiceList = getServiceList();
        mFoundServiceNumber = getFoundServiceNumber();
        if (mFoundServiceNumber == 0 && mServiceList != null && mServiceList.length() > 0) {
            Log.d(TAG, "mFoundServiceNumber erro use mServiceList length = " + mServiceList.length());
            mFoundServiceNumber = mServiceList.length();
        }
        mStartSync = true;
        setSearchStatus("Updating guide");
        startMonitoringSync();
        // By default, gets all channels and 1 hour of programs (DEFAULT_IMMEDIATE_EPG_DURATION_MILLIS)
        EpgSyncJobService.cancelAllSyncRequests(getActivity());
        Log.i(TAG, String.format("inputId: %s", inputId));
        //EpgSyncJobService.requestImmediateSync(this, inputId, true, new ComponentName(this, DtvkitEpgSync.class)); // 12 hours
        Bundle parameters = new Bundle();
        parameters.putString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_MODE, EpgSyncJobService.BUNDLE_VALUE_SYNC_SEARCHED_MODE_MANUAL);
        parameters.putString(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_SIGNAL_TYPE, "DVB-S");
        EpgSyncJobService.requestImmediateSyncSearchedChannelWitchParameters(getActivity(), inputId, (mFoundServiceNumber > 0), new ComponentName(getActivity(), DtvkitEpgSync.class), parameters);
        DataProviderManager.putBooleanValue(getActivity(), ConstantManager.KEY_IS_SEARCHING, false);
    }

    private void startMonitoringSearch() {
        DtvkitGlueClient.getInstance().registerSignalHandler(mHandler);
    }

    private void stopMonitoringSearch() {
        DtvkitGlueClient.getInstance().unregisterSignalHandler(mHandler);
    }

    private void startMonitoringSync() {
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver,
                new IntentFilter(EpgSyncJobService.ACTION_SYNC_STATUS_CHANGED));
    }

    private void stopMonitoringSync() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
    }

    private void setSearchStatus(final String status) {
        setSearchStatus(status, "");
    }

    private void setSearchStatus(final String status, final String description) {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, String.format("Search status \"%s\"", status));
                if (mSearchStatus != null) {
                    mSearchStatus.setText(String.format("%s\t%s", status, description));
                }
            }
        });
    }

    private void setProgressBar(boolean status) {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(() -> {
            if (mSearchProgress != null) {
                mSearchProgress.setIndeterminate(status);
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
            progress = data.getInt("progress");
        } catch (JSONException e) {
            Log.e(TAG, "getSearchProcess Exception = " + e.getMessage());
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

    private void setStrengthAndQualityStatus(final String sstatus, final String qstatus) {
        setStrengthAndQualityStatus(sstatus, qstatus, "");
    }

    private void setStrengthAndQualityStatus(final String sstatus, final String qstatus, final String channel) {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(() -> {
            if (TextUtils.isEmpty(sstatus) && TextUtils.isEmpty(qstatus)) {
                if (mChannelHolder != null) {
                    mChannelHolder.setVisibility(View.GONE);
                }
            } else {
                if (mChannelHolder != null) {
                    mChannelHolder.setVisibility(View.VISIBLE);
                }
            }
            if (mChannelInfo != null) {
                mChannelInfo.setText(sstatus + "\t\t" + qstatus + "\t\t" + channel);
            }
        });
    }

    @Override
    public void onLifecycleChanged(int lifecycle) {
        super.onLifecycleChanged(lifecycle);
        Log.e(TAG, "onLifecycleChanged => " + lifecycle);
        if (lifecycle == SearchStageFragment.ACTIVITY_LIFECYCLE_ONSTOP) {
            if (mStartSearch) {
                sendFinishSearch(false);
            }
            releaseHandler();
        }
    }
}
