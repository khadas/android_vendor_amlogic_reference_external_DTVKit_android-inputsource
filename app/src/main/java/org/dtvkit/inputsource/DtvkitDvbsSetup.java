package com.droidlogic.dtvkit.inputsource;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;

import com.droidlogic.dtvkit.inputsource.searchguide.DataPresenter;
import com.droidlogic.dtvkit.inputsource.searchguide.DtvkitDvbsSetupFragment;
import com.droidlogic.dtvkit.inputsource.searchguide.OnNextListener;
import com.droidlogic.dtvkit.inputsource.searchguide.SearchStageFragment;
import com.droidlogic.dtvkit.inputsource.searchguide.SimpleListFragment;
import com.droidlogic.fragment.ParameterMananer;
import com.droidlogic.fragment.ScanDishSetupFragment;
import com.droidlogic.fragment.dialog.DialogManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DtvkitDvbsSetup extends Activity {
    private static final String TAG = DtvkitDvbsSetup.class.getSimpleName();
    private SearchStageFragment fragment = null;
    private DataPresenter mDataPresenter = null;
    private boolean isM7 = false;
    private boolean manualDiseqc = false;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mDataPresenter = new DataPresenter(this);
        String tag = DataPresenter.FRAGMENT_SEARCH_UI;
        if (intent != null) {
            isM7 = intent.getBooleanExtra("M7", false);
            manualDiseqc = intent.getBooleanExtra("manual", false);
            if (manualDiseqc) {
                showFragment(DataPresenter.FRAGMENT_MANUAL_DISEQC);
            }
        }
        if (fragment == null) {
            showFragment(DataPresenter.FRAGMENT_SEARCH_UI);
        }
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (fragment != null) {
            if (fragment.handleKeyDown(keyCode, event)) {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showFragment(String title) {
        Log.d(TAG, "showFragment " + title);
        switch (title) {
            case DataPresenter.FRAGMENT_OPERATOR_LIST:
            case DataPresenter.FRAGMENT_OPERATOR_SUB_LIST:
                fragment = SimpleListFragment.newInstance(title);
                fragment.setCanBackToPrevious(false);
                break;
            case DataPresenter.FRAGMENT_SEARCH_UI:
                fragment = DtvkitDvbsSetupFragment.newInstance(title);
                fragment.setParameterManager(mDataPresenter.getParameterManager());
                fragment.setDialogManager(mDataPresenter.getDialogManager());
                Bundle bundle = new Bundle();
                bundle.putBoolean("manualDiseqc", manualDiseqc);
                fragment.setArguments(bundle);
                break;
            case DataPresenter.FRAGMENT_MANUAL_DISEQC:
                fragment = new ScanDishSetupFragment();
                fragment.setTitle(title);
                fragment.setParameterManager(mDataPresenter.getParameterManager());
                fragment.setDialogManager(mDataPresenter.getDialogManager());
                break;
            case DataPresenter.FRAGMENT_REGIONAL_CHANNELS:
                fragment = SimpleListFragment.newInstance(title, true);
                fragment.setCanBackToPrevious(false);
                break;
            default:
                return;
        }
        fragment.setM7Spec(isM7);
        fragment.setListener(mOnNextListener);
        Log.d(TAG, "showFragment setparams");
        mHandler.post(() -> {
            if (title.equals(DataPresenter.FRAGMENT_SEARCH_UI)) {
                if (!showSearchFragment(true)) {
                    if (getFragmentManager().isDestroyed()) {
                        return;
                    }
                    getFragmentManager().beginTransaction()
                            .addToBackStack(title)
                            .add(android.R.id.content, fragment)
                            .commit();
                }
            } else {
                showSearchFragment(false);
                if (getFragmentManager().isDestroyed()) {
                    return;
                }
                getFragmentManager().beginTransaction()
                        .addToBackStack(title)
                        .add(android.R.id.content, fragment)
                        .commit();
            }
        });
    }

    private boolean showSearchFragment(boolean show) {
        if (getFragmentManager().isDestroyed()) {
            return true;
        }
        List<Fragment> fragments = getFragmentManager().getFragments();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Log.i(TAG, "showSearchFragment " + fragments);
        Fragment searchFragment = null;
        for (Fragment fragment : fragments) {
            if (fragment instanceof DtvkitDvbsSetupFragment) {
                searchFragment = fragment;
                if (show) {
                    ft.show(searchFragment);
                } else {
                    ft.hide(searchFragment);
                }
            } else {
                // do not need any other fragments
                if (fragment instanceof SearchStageFragment && show) {
                    Log.i(TAG, "remove " + fragment + ", "
                            + getFragmentManager().popBackStackImmediate(
                                    ((SearchStageFragment) fragment).getTitle(), FragmentManager.POP_BACK_STACK_INCLUSIVE));
                }
            }
        }
        if (searchFragment == null && !show) {
            return false;
        }
        if (searchFragment != null) {
            Log.i(TAG, "search ui exists, " + show);
            ft.commitAllowingStateLoss();
            return true;
        }
        return false;
    }

    private final OnNextListener mOnNextListener = new OnNextListener() {
        @Override
        public void onNext(Fragment fragment, String text) {
            onNext(fragment, text, 0);
        }

        @Override
        public void onNext(Fragment fragment, String text, int pos) {
            Log.i(TAG, "from fragment:" + fragment + ", " + text);
            if (fragment instanceof ScanDishSetupFragment) {
                if (isM7) {
                    JSONArray arg = new JSONArray();
                    arg.put(0x00010001);
                    mDataPresenter.getParameterManager().dvbsScanControl(arg);
                }
                showFragment(DataPresenter.FRAGMENT_SEARCH_UI);
            } else if (fragment instanceof DtvkitDvbsSetupFragment) {
                // show next fragment
                String nextFragment = null;
                if (TextUtils.equals(text, DataPresenter.FRAGMENT_MANUAL_DISEQC)
                        || TextUtils.equals(text, DataPresenter.FRAGMENT_OPERATOR_LIST)
                        || TextUtils.equals(text, DataPresenter.FRAGMENT_OPERATOR_SUB_LIST)
                        || TextUtils.equals(text, DataPresenter.FRAGMENT_REGIONAL_CHANNELS)) {
                    nextFragment = text;
                }
                String finalNextFragment = nextFragment;
                if (finalNextFragment != null) {
                    showFragment(finalNextFragment);
                } else {
                    Log.e(TAG, "wrong text:" + text);
                }
            } else if (fragment instanceof SearchStageFragment) {
                if (!isM7) {
                    Log.w(TAG, "not M7 search");
                    return;
                }
                String title = ((SearchStageFragment) fragment).getTitle();
                // item selected in a SearchStageFragment
                if (TextUtils.equals(title, DataPresenter.FRAGMENT_OPERATOR_LIST)) {
                    JSONArray arg = new JSONArray();
                    arg.put(0x00010004);
                    arg.put(text);
                    mDataPresenter.getParameterManager().dvbsScanControl(arg);
                    showFragment(DataPresenter.FRAGMENT_OPERATOR_SUB_LIST);
                    return;
                } else if (TextUtils.equals(title, DataPresenter.FRAGMENT_OPERATOR_SUB_LIST)) {
                    JSONArray arg = new JSONArray();
                    arg.put(0x00010005);
                    arg.put(text);
                    mDataPresenter.getParameterManager().dvbsScanControl(arg);
                } else if (TextUtils.equals(title, DataPresenter.FRAGMENT_REGIONAL_CHANNELS)) {
                    // text is a hashmap toString
                    int length = text.length();
                    String next = text.substring(1, length - 1); //remove '{', '}'
                    String[] arr = next.split(",");
                    if (arr.length > 0) {
                        JSONArray out = new JSONArray();
                        out.put(0x00010007);
                        try {
                            JSONArray in = new JSONArray();
                            for (String value : arr) {
                                JSONObject object = new JSONObject();
                                String[] s = value.trim().split("=");
                                object.put("RegionName", s[0]);
                                object.put("SetRegion", s[1]);
                                in.put(object);
                            }
                            out.put(in);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } finally {
                            Log.d(TAG, "target:" + out);
                        }
                        mDataPresenter.getParameterManager().dvbsScanControl(out);
                    } else {
                        Log.e(TAG, "Error parse");
                    }
                } else {
                    Log.e(TAG, "Error info: " + ((SearchStageFragment) fragment).getTitle());
                }
                mHandler.post(() -> {
                    showSearchFragment(true);
                });
            }
        }

        @Override
        public void onFragmentReady(Fragment fragment) {
            String title = "";
            if (fragment instanceof SimpleListFragment) {
                title = ((SearchStageFragment) fragment).getTitle();
                if (TextUtils.equals(title, DataPresenter.FRAGMENT_OPERATOR_LIST)
                        || TextUtils.equals(title, DataPresenter.FRAGMENT_OPERATOR_SUB_LIST)) {
                    int pos = 0;
                    JSONArray arg = new JSONArray();
                    if (title.equals(DataPresenter.FRAGMENT_OPERATOR_LIST)) {
                        arg.put(0x00010002);
                    } else {
                        arg.put(0x00010003);
                    }
                    List<String> dataList = parseJsonObject(mDataPresenter.getParameterManager().dvbsScanControl(arg));
                    ((SimpleListFragment) fragment).updateData(dataList, pos);
                } else if (TextUtils.equals(title, DataPresenter.FRAGMENT_REGIONAL_CHANNELS)) {
                    ((SimpleListFragment) fragment).updateDataWithExtra(getRegionList());
                }
            } else {
                Log.e(TAG, "To Be Done!");
            }
        }
    };

    private List<String> parseJsonObject(JSONObject jsonObject) {
//        Log.i(TAG, "parseJsonObject " + jsonObject);
        List<String> dataList = new ArrayList<>();
        if (jsonObject != null) {
            try {
                JSONArray data = (JSONArray) jsonObject.get("data");
                if (data.length() == 0) {
                    return null;
                }
                for (int i = 0; i < data.length(); i++) {
                    String item = (String) data.get(i);
                    dataList.add(item);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return dataList;
    }

    //    "data":[{"RegionName":"CT2","RegionSrvList":["CT 2 HD","OCKO"]},
    //    {"RegionName":"CT1","RegionSrvList":["CT 1 HD","CT 1 JM HD","CT 1 SM HD"]}]
    private HashMap<String, List<String>> getRegionList() {
        HashMap<String, List<String>> result = new HashMap<>();
        JSONArray arg = new JSONArray();
        arg.put(0x00010006);
        JSONObject resultObj = mDataPresenter.getParameterManager().dvbsScanControl(arg);
        try {
            JSONArray data = (JSONArray) resultObj.get("data");
            if (data.length() == 0) {
                return result;
            }
            for (int i = 0; i < data.length(); i++) {
                String regionName = (String) (((JSONObject) (data.get(i))).get("RegionName"));
                JSONArray list = (JSONArray) (((JSONObject) (data.get(i))).get("RegionSrvList"));
                List<String> regionSrvList = new ArrayList<>();
                if (list.length() > 0) {
                    for (int j = 0; j < list.length(); j++) {
                        regionSrvList.add(list.getString(j));
                    }
                }
                result.put(regionName, regionSrvList);
            }
        } catch (JSONException e) {
            Log.e(TAG, "getRegionList failed:" + e.getMessage());
        }
        return result;
    }
}

