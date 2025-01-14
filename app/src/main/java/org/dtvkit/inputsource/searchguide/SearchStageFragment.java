package com.droidlogic.dtvkit.inputsource.searchguide;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.droidlogic.fragment.ParameterManager;
import com.droidlogic.fragment.dialog.DialogManager;

import com.droidlogic.dtvkit.inputsource.searchguide.OnNextListener;

import org.json.JSONObject;

public abstract class SearchStageFragment extends Fragment {
    private static final String TAG = SearchStageFragment.class.getSimpleName();
    private String mTitle = "";
    private OnNextListener mListener;
    private boolean canBackToPrevious = false;
    private ParameterManager mParameterManager;
    private DialogManager mDialogManager;

    public final static int ACTIVITY_LIFECYCLE_ON_START = 1;
    public final static int ACTIVITY_LIFECYCLE_ON_RESUME = 2;
    public final static int ACTIVITY_LIFECYCLE_ON_PAUSE = 3;
    public final static int ACTIVITY_LIFECYCLE_ON_STOP = 4;

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        this.mTitle = title;
    }

    public void setListener(OnNextListener listener) {
        mListener = listener;
    }

    public void setCanBackToPrevious(boolean back) {
        canBackToPrevious = back;
    }

    public boolean isCanBackToPrevious() {
        return canBackToPrevious;
    }

    public OnNextListener getListener() {
        return mListener;
    }

    public boolean handleKeyDown(int keyCode, KeyEvent event) {
        if (!canBackToPrevious) {
            return keyCode == KeyEvent.KEYCODE_BACK;
        }
        return false;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, this.toString() + " lifecycle onCreate");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, this.toString() + " lifecycle onCreateView");
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, this.toString() + " lifecycle onStart");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, this.toString() + " lifecycle onPause");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, this.toString() + " lifecycle onResume");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, this.toString() + " lifecycle onStop");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, this.toString() + " lifecycle onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, this.toString() + " lifecycle onDestroy");
    }

    public DialogManager getDialogManager() {
        return mDialogManager;
    }

    public void setDialogManager(DialogManager dialogManager) {
        mDialogManager = dialogManager;
    }

    public ParameterManager getParameterManager() {
        return mParameterManager;
    }

    public void setParameterManager(ParameterManager parameterManager) {
        mParameterManager = parameterManager;
    }

    public void handleMessage(String signal, JSONObject object) {

    }

    public void onLifecycleChanged(int lifecycle) {

    }

}
