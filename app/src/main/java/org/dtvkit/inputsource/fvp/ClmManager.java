package org.dtvkit.inputsource.fvp;

import android.app.Activity;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;

import com.droidlogic.dtvkit.inputsource.util.FeatureUtil;
import com.droidlogic.dtvkit.inputsource.fvp.DtvkitFvp;
import com.droidlogic.fragment.ParameterManager;

import org.droidlogic.dtvkit.DtvkitGlueClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ClmManager {
    private static final String TAG = "ClmManager";
    private static final String EVENT_USER_SELECTION_REQUEST = "fvp_clm_NotifyUserSelectionRequest";
    private static final String EVENT_USER_CODE_INPUT_REQUEST = "fvp_clm_NotifyPostCodeInputRequest";
    private static final String EVENT_USER_FINISH_SUCCESS = "fvp_clm_NotifyFinishSucceeded";
    private static final String EVENT_USER_FINISH_FAILED = "fvp_clm_NotifyFinishFailed";

    public static final int FTI_SCAN = 0;
    public static final int INTERACTIVE_RETUNE_SCAN = 1;
    public static final int BACKGROUND_SCAN = 2;
    public static final int NON_INTERACTIVE_SCAN = 3;

    private Activity mActivity;
    private Context mContext;

    private final DtvkitGlueClient.SignalHandler mClmEventHandler = this::eventHandler;
    private ClmDialogUpdateCallback mDialogCallback = null;
    private ClmFinishCallback mFinishCallback = null;
    private Handler mMainHandler = null;
    //private ClmDialogFragment mClmDialog = null;

    public ClmManager(Activity activity){
        mActivity = activity;
        if (null != activity) {
            mMainHandler = new Handler(mActivity.getMainLooper());
            mContext = (Context) activity;
        }
    }

    public boolean checkNeedIpScan() {
        boolean result = DtvkitFvp.getInstance().checkFvpNeedIpScan();
        Log.d(TAG, "checkNeedIpScan : " + result);
        return checkFvpUkCountry() && result;
    }

    public void clmHandleStart(int scanType) {
        DtvkitGlueClient.getInstance().registerSignalHandler(mClmEventHandler);
        //background scan not need dialog
        if (BACKGROUND_SCAN != scanType) {
            ClmDialogFragment clmDialog  = new ClmDialogFragment();
            clmDialog.show(mActivity.getFragmentManager(), "ClmDialogFragment");
            clmDialog.setClmManager(this);
        }
        if (false == startIpScan(scanType)) {
            exitIpScan(true);
        }
/*
        //test for UI
        mMainHandler.postDelayed(()->{
            testRegionSelection();
        }, 1000);

        mMainHandler.postDelayed(()->{
            testPostCode();
        }, 20000);

        mMainHandler.postDelayed(()->{
            testUserSelection();
        }, 30000);

        mMainHandler.postDelayed(()->{
            testFinishSuccess();
        }, 40000);
*/
    }

    public boolean startIpScan(int scanType) {
        boolean result = DtvkitFvp.getInstance().fvpStartScan(scanType);
        Log.d(TAG, "startIpScan scanType : " + scanType + " result : " + result);
        return result;
    }

    public boolean setUserSelection(int selectOrder) {
        boolean result = DtvkitFvp.getInstance().setClmUserSelectionRespondInfo(selectOrder);
        Log.d(TAG, "setUserSelection selectOrder : " + selectOrder + " result : " + result);

        //test
        //testPostCode();
        return result;
    }

    public boolean setRegionId(String postCode) {
        boolean result = DtvkitFvp.getInstance().setClmRegionIdRespondInfo(postCode);
        Log.d(TAG, "setRegionId postCode : " + postCode + " result : " + result);
        //test
        //testFinishSuccess();
        return result;
    }

    public void exitIpScan(boolean isStartError) {
        Log.d(TAG, "exitIpScan");
        if (isStartError) {
            if (null != mDialogCallback) {
                executeOnMainThread(()->{
                    mDialogCallback.clmHandleFinish();
                    mDialogCallback = null;
                });
            } else {
                Log.e(TAG, "not register mDialogCallback");
            }
        }
        if (null != mFinishCallback) {
            mFinishCallback.onClmFinishSuccess();
            mFinishCallback = null;
        } else {
            Log.e(TAG, "not register mFinishCallback");
        }
        DtvkitGlueClient.getInstance().unregisterSignalHandler(mClmEventHandler);
    }

    public void addCallback(ClmDialogUpdateCallback callback) {
        mDialogCallback = callback;
    }

    public void addFinishCallback(ClmFinishCallback clmFinishCallback) {
        mFinishCallback = clmFinishCallback;
    }

    public void stopIpScan() {
        DtvkitGlueClient.getInstance().unregisterSignalHandler(mClmEventHandler);
        mDialogCallback = null;
        if (null != mFinishCallback) {
            mFinishCallback.onClmFinishFailed(true);
        }
    }

    public void eventHandler(String signal, JSONObject data) {
        Log.d(TAG, "eventHandler signal : " + signal + " data : " + data);
        switch (signal) {
            case EVENT_USER_SELECTION_REQUEST: {
                handleUserSelectionMessage(data);
                break;
            }
            case EVENT_USER_CODE_INPUT_REQUEST: {
                handlePostCodeMessage(data);
                break;
            }
            case EVENT_USER_FINISH_SUCCESS: {
                handleFinishSuccessMessage(data);
                break;
            }
            case EVENT_USER_FINISH_FAILED: {
                handleFinishFailedMessage(data);
                break;
            }
            default:
                break;
        }
    }

    public Context getContext() {
        return mContext;
    }

    private boolean checkFvpUkCountry() {
        final String FVP_UK_COUNTRY_NAME = "gbr";
        String currentCountryName = ParameterManager.getCurrentCountryIso3Name();
        return FeatureUtil.getFeatureSupportFvp() && FVP_UK_COUNTRY_NAME.equalsIgnoreCase(currentCountryName);
    }

    private void handleUserSelectionMessage (JSONObject data) {
        if (null == data) {
            Log.e(TAG, "handlePostCodeMessage error not have message");
            return;
        }
        Log.d(TAG, "handleUserSelectionMessage data : " + data);
        try {
            int orderCount = data.getInt("count");
            Log.i(TAG, "handleUserSelectionMessage: orderCount : " + orderCount);
            if (0 == orderCount) {
                Log.e(TAG, "handleUserSelectionMessage: orderCount error ");
                return;
            }
            JSONArray orderArray = data.getJSONArray("details");
            if ((null == orderArray) || (orderCount != orderArray.length())) {
                Log.e(TAG, "handleUserSelectionMessage: orderArray error ");
                return;
            }
            Map<Integer, Pair<Integer, String>> orderMap = new HashMap<>();
            for (int i = 0; i < orderArray.length(); i++) {
                JSONObject orderContent = orderArray.getJSONObject(i);
                if (null == orderContent) {
                    Log.e(TAG, "handleUserSelectionMessage: orderContent error ");
                    return;
                }
                orderMap.put(i, new Pair<>(orderContent.getInt("order"),
                        orderContent.getString("text")));
            }
            String dialogQuestion = data.getString("question");

            if (null != mDialogCallback) {
                executeOnMainThread(()->{
                    if (dialogQuestion.contains("Select") && dialogQuestion.contains("region")) {
                        mDialogCallback.showRegionSelection(dialogQuestion, orderMap);
                    } else if (2 == orderCount) {
                        mDialogCallback.showUserSelection(dialogQuestion, orderMap);
                    } else {
                        Log.e(TAG, "not handle the selection");
                    }
                });
            } else {
                Log.e(TAG, "not register callback");
            }
        } catch (Exception e) {
            Log.e(TAG, "handleUserSelectionMessage Exception :" + e);
        }
    }

    private void handlePostCodeMessage(JSONObject data) {
        if (null != data) {
            if (null != mDialogCallback) {
                executeOnMainThread(()->{
                    mDialogCallback.showPostCode();
                });
            } else {
                Log.e(TAG, "not register callback");
            }
        } else {
            Log.e(TAG, "handlePostCodeMessage error not have message");
        }
    }

    private void handleFinishSuccessMessage (JSONObject data) {
        if (null != data) {
            if (null != mDialogCallback) {
                executeOnMainThread(()->{
                    mDialogCallback.clmHandleFinish();
                    mDialogCallback = null;
                });
            } else {
                Log.e(TAG, "not register mDialogCallback");
            }

            if (null != mFinishCallback) {
                mFinishCallback.onClmFinishSuccess();
                mFinishCallback = null;
            } else {
                Log.e(TAG, "not register mFinishCallback");
            }
            DtvkitGlueClient.getInstance().unregisterSignalHandler(mClmEventHandler);
        } else {
            Log.e(TAG, "handlePostCodeMessage error not have message");
        }
    }

    private void handleFinishFailedMessage (JSONObject data) {
        if (null != data) {
            if (null != mDialogCallback) {
                executeOnMainThread(()->{
                    mDialogCallback.clmHandleFinish();
                    mDialogCallback = null;
                });
            } else {
                Log.e(TAG, "not register callback");
            }

            if (null != mFinishCallback) {
                mFinishCallback.onClmFinishFailed(data.optBoolean("revertToBroadcastLcn", false));
                mFinishCallback = null;
            } else {
                Log.e(TAG, "not register mFinishCallback");
            }
            DtvkitGlueClient.getInstance().unregisterSignalHandler(mClmEventHandler);
        } else {
            Log.e(TAG, "handlePostCodeMessage error not have message");
        }
    }

    private void executeOnMainThread(Runnable action) {
        if (null == mMainHandler) {
            Log.e(TAG, "not create Main Handler");
            return;
        }
        if (mMainHandler.getLooper().isCurrentThread()) {
            action.run();
        } else {
            // Posts the runnable if this is not called from the main thread
            mMainHandler.post(action);
        }
    }

    public interface ClmDialogUpdateCallback {
        void showUserSelection(String dialogQuestion, Map orderMap);
        void showPostCode();
        void showRegionSelection(String dialogQuestion, Map<Integer, Pair<Integer, String>> orderMap);
        void clmHandleFinish();
    }

    public interface ClmFinishCallback {
        void onClmFinishSuccess();
        void onClmFinishFailed(boolean needRevert);
    }

    private void testUserSelection() {
        try {
            JSONObject data = new JSONObject();
            data.put("count", 2);

            JSONArray details = new JSONArray();

            JSONObject orderContent1 = new JSONObject();
            orderContent1.put("order", 1);
            orderContent1.put("text", "Yes");
            details.put(orderContent1);

            JSONObject orderContent2 = new JSONObject();
            orderContent2.put("order", 2);
            orderContent2.put("text", "No");
            details.put(orderContent2);

            data.put("details", details);
            data.put("question", "Would you like extra channels included with Freeview delivered over the internet? If so, youĐll need a broadband speed over 2Mbps and this will count towards any broadband data allowance you have");
            handleUserSelectionMessage(data);
        }catch (Exception e) {
            Log.d(TAG, "Exception : " + e);
        }
    }

    private void testPostCode() {
        mMainHandler.postDelayed(()->{
            try {
                JSONObject data = new JSONObject();
                handlePostCodeMessage(data);
            } catch(Exception e) {
                Log.d(TAG, "Exception : " + e);
            }
        }, 1000);
    }

    private void testFinishSuccess() {
        try {
            JSONObject data = new JSONObject();
            handleFinishSuccessMessage(data);
        } catch(Exception e) {
            Log.d(TAG, "Exception : " + e);
        }
    }

    private void testFinishFailed() {
        try {
            JSONObject data = new JSONObject();
            data.put("finish", true);
            handleFinishFailedMessage(data);
        } catch(Exception e) {
            Log.d(TAG, "Exception : " + e);
        }
    }

    private void testRegionSelection() {
        try {
            JSONObject data = new JSONObject();
            data.put("count", 4);

            JSONArray details = new JSONArray();

            JSONObject orderContent1 = new JSONObject();
            orderContent1.put("order", 1);
            orderContent1.put("text", "Region1,Region1,Region1,Region1,Region1,Region1");
            details.put(orderContent1);

            JSONObject orderContent2 = new JSONObject();
            orderContent2.put("order", 2);
            orderContent2.put("text", "Region2");
            details.put(orderContent2);

            JSONObject orderContent3 = new JSONObject();
            orderContent3.put("order", 3);
            orderContent3.put("text", "Region3");
            details.put(orderContent3);

            JSONObject orderContent4 = new JSONObject();
            orderContent4.put("order", 4);
            orderContent4.put("text", "Region4");
            details.put(orderContent4);

            data.put("details", details);
            data.put("question", "Select your preferred region");
            handleUserSelectionMessage(data);
        }catch (Exception e) {
            Log.d(TAG, "Exception : " + e);
        }
    }
}
