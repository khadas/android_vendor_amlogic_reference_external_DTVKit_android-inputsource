package com.droidlogic.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.droidlogic.dtvkit.inputsource.R;
import com.droidlogic.fragment.dialog.DialogManager;

import org.droidlogic.dtvkit.DtvkitGlueClient;

public class ScanMainActivity extends Activity {
    private static final String TAG = "ScanMainActivity";
    private ScanFragmentManager mScanFragmentManager = null;

    public static final int REQUEST_CODE_START_SETUP_ACTIVITY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mScanFragmentManager = new ScanFragmentManager(this);
        ParameterManager parameterManager = new ParameterManager(this, DtvkitGlueClient.getInstance());
        DialogManager dialogManager = new DialogManager(this, parameterManager);

        ScanDishSetupFragment mainFragment = new ScanDishSetupFragment();
        mainFragment.setParameterManager(parameterManager);
        mainFragment.setDialogManager(dialogManager);
        mScanFragmentManager.show(mainFragment);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mScanFragmentManager.removeRunnable();
    }

    public ScanFragmentManager getScanFragmentManager() {
        return mScanFragmentManager;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown " + event);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mScanFragmentManager.isActive()) {
                mScanFragmentManager.popSideFragment();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_START_SETUP_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                setResult(RESULT_OK, data);
                finish();
            } else {
                setResult(RESULT_CANCELED);
            }
        } else {// do nothing
            Log.d(TAG, "onActivityResult other request");
        }
    }
}
