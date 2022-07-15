package com.droidlogic.dtvkit.inputsource;

import android.app.Activity;
import android.content.Intent;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.Toast;
import android.view.KeyEvent;
import android.media.MediaPlayer;
import android.database.Cursor;

public class FvpScanActivity extends Activity implements UpdateScanView {
    private static final String TAG = "FvpScanActivity";

    private static final int MSG_UPDATE_CHANNEL_NUMBER = 0;
    private static final int MSG_UPDATE_SCAN_PROGRESS = 1;
    private static final int MSG_UPDATE_SCAN_STATUS = 2;
    private static final int MSG_FINISH_SCAN_VIEW = 3;

    DtvKitDVBTCScanPresenter mScanPresenter = null;
    VideoView mVideoView;
    TextView mScanStatus;
    TextView mDigitalChannelNumber;
    TextView mIpChannelNumber;
    ProgressBar mScanProgress;
    TextView mScanProgressNumber;
    Handler mMainThreadHandler = new Handler((Message msg)->{
        switch (msg.what) {
            case MSG_UPDATE_SCAN_PROGRESS:{
                mScanProgressNumber.setText(String.valueOf(msg.arg1));
                mScanProgress.setProgress(msg.arg1, true);
                break;
            }
            case MSG_UPDATE_CHANNEL_NUMBER:{
                mDigitalChannelNumber.setText(String.valueOf(msg.arg1));
                mIpChannelNumber.setText(String.valueOf(msg.arg2));
                break;
            }
            case MSG_UPDATE_SCAN_STATUS:{
                mScanStatus.setText((String)msg.obj);
                break;
            }
            case MSG_FINISH_SCAN_VIEW:{
                finish();
            }
        }
        return true;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fvp_scan);
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        triggerAutomaticScan();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (null != mScanPresenter) {
            if (mScanPresenter.isStartScan()) {
                mScanPresenter.stopScan(true);
            }
            Log.d(TAG, "onStop isStartScan = " + mScanPresenter.isStartScan() + "|isSyncTvProvider = " + mScanPresenter.isSyncTvProvider());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (null != mScanPresenter) {
            mScanPresenter.releaseScanResource();
            mScanPresenter = null;
        }
    }

    @Override
    public void finish() {
        if (mVideoView.isPlaying()) {
            mVideoView.stopPlayback();
        }
        //Send Message to mds client
        sendFvpChannelScanMessage();
        Log.d(TAG, "need do some thing when scan view finish");
        super.finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (null == mScanPresenter) {
            finish();
            return true;
        }

        if (mScanPresenter.isSyncTvProvider()) {
            Toast.makeText(this, R.string.sync_tv_provider, Toast.LENGTH_SHORT).show();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            mScanPresenter.stopScan(false);
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void updateScanProgress(int progress) {
        Log.d(TAG, "updateScanProgress progress = " + progress);
        if (100 <= progress) {
            mScanPresenter.stopScan(true);
        }
        mMainThreadHandler.removeMessages(MSG_UPDATE_SCAN_PROGRESS);
        Message msg = mMainThreadHandler.obtainMessage(MSG_UPDATE_SCAN_PROGRESS, progress, 0);
        boolean info = mMainThreadHandler.sendMessage(msg);
        Log.d(TAG, "updateScanProgress " + info);
    }

    @Override
    public void updateScanChannelNumber(int digitalChannelNumber, int ipChannelNumber) {
        Log.d(TAG, "updateScanProgress digitalChannelNumber = " + digitalChannelNumber +
                "| ipChannelNumber = " + ipChannelNumber);
        mMainThreadHandler.removeMessages(MSG_UPDATE_CHANNEL_NUMBER);
        Message msg = mMainThreadHandler.obtainMessage(MSG_UPDATE_CHANNEL_NUMBER, digitalChannelNumber, ipChannelNumber);
        boolean info = mMainThreadHandler.sendMessage(msg);
        Log.d(TAG, "updateScanChannelNumber " + info);
    }

    @Override
    public void updateScanStatus(String status) {
        Log.d(TAG, "updateScanStatus status = " + status);
        mMainThreadHandler.removeMessages(MSG_UPDATE_SCAN_STATUS);
        Message msg = mMainThreadHandler.obtainMessage(MSG_UPDATE_SCAN_STATUS, status);
        boolean info = mMainThreadHandler.sendMessage(msg);
        Log.d(TAG, "updateScanStatus " + info);
    }

    @Override
    public void finishScanView() {
        Log.d(TAG, "finishScanView");
        mMainThreadHandler.removeMessages(MSG_FINISH_SCAN_VIEW);
        Message msg = mMainThreadHandler.obtainMessage(MSG_FINISH_SCAN_VIEW);
        boolean info = mMainThreadHandler.sendMessage(msg);
        Log.d(TAG, "MSG_FINISH_SCAN_VIEW " + info);
    }

    private void initView() {
        mVideoView = findViewById(R.id.fvp_video_view);
        mScanStatus = findViewById(R.id.fvp_scan_status);
        mDigitalChannelNumber = findViewById(R.id.fvp_digital_channel_number);
        mIpChannelNumber = findViewById(R.id.fvp_ip_channel_number);
        mScanProgress = findViewById(R.id.fvp_scan_progress);
        mScanProgressNumber = findViewById(R.id.fvp_scan_progress_number);
    }

    private void triggerAutomaticScan() {
        Intent intent = getIntent();
        if (intent.getBooleanExtra("Automatic_scan", false)) {
            String uri = intent.getStringExtra("FvpVideoUrl");
            String dvbType = intent.getStringExtra("DvbType");
            String inputId = intent.getStringExtra("InputId");
            mScanPresenter = new DtvKitDVBTCScanPresenter(this, DtvKitDVBTCScanPresenter.DVB_T, inputId, this);
            mScanPresenter.startAutoScan();
            if (null != uri) {
                mVideoView.setVideoURI(Uri.parse(uri));
                mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer test) {
                        Log.d(TAG, "onPrepared = " + test);
                    }
                });
                mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                        Log.d(TAG, "onError = " + mediaPlayer + "|i = " + i + "i1=" + i1);
                    return false;
                    }
                });
                mVideoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                    @Override
                    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
                        Log.d(TAG, "onInfo = " + mediaPlayer + "|i = " + i + "i1=" + i1);
                    return false;
                    }
                });
                mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        Log.d(TAG, "onCompletion = " + mediaPlayer);
                    }
                });
                mVideoView.start();
            }
            Log.d(TAG, "triggerAutomaticScan:video uri = " + uri + "| dvbType = " + dvbType + "|inputId = " + inputId);
        } else {
            Log.d(TAG, "not need automatic scan, finish ");
            finish();
        }
    }

    private void sendFvpChannelScanMessage() {
        String application = null;
        String className = null;
        Uri systemMsgUri = Uri.parse("content://" + "uk.co.freeview.fvpconfigauth.provider.FVPConfigAuth" + "/systemmessages");
        try (Cursor c = getContentResolver().query(systemMsgUri, null, null, null, null)) {
            if (null != c && c.moveToNext()) {
                application = c.getString(1);
                className = c.getString(2);
                Log.d(TAG, "query system message application = " + application + "class = " + className);
            }
        } catch(Exception e) {
            Log.d(TAG, "query system message Exception = " + e);
        }
        if (null != application && null != className) {
            Intent intent = new Intent("uk.co.freeview.action.CHSCAN");
            intent.setComponent(new ComponentName(application, className));
            intent.setType("fvp/chscan");
            intent.putExtra("Detail", Long.toString(System.currentTimeMillis()/1000L));
            startActivity(intent);
        } else {
            Log.e(TAG, "sendFvpChannelScanMessage error can't find application or class");
        }
    }
}
