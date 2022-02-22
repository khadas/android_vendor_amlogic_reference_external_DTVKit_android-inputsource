package com.amlogic.hbbtv;

import android.content.Context;
import android.net.ConnectivityManager;
import android.content.BroadcastReceiver;
import android.net.NetworkInfo;
import android.content.Intent;
import android.util.Log;


public class NetworkChangeBroadcast extends BroadcastReceiver {
    private static final String TAG = "NetworkChangeBroadcast";
    private AmlHbbTvView mAmlHbbTvView;
    public void setHbbTvView (AmlHbbTvView amlHbbTvView){
        mAmlHbbTvView = amlHbbTvView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
               Log.d(TAG,"the network changed");
               ConnectivityManager connectivityManager = (ConnectivityManager)
                        context.getSystemService(Context.CONNECTIVITY_SERVICE);
               NetworkInfo netWorkInfo = connectivityManager.getActiveNetworkInfo();
               if (netWorkInfo != null) {
                    if (netWorkInfo.isAvailable()) {
                        Log.d(TAG,"network connected");
                        if (mAmlHbbTvView != null) {
                            if (mAmlHbbTvView.isInitialized() && !mAmlHbbTvView.isApplicationRunning()) {
                                 mAmlHbbTvView.terminateApplicationAndLaunchAutostart();
                            }
                        }
                    }

               } else {
                    Log.d(TAG, "network disconnected");
                    if (mAmlHbbTvView != null) {
                        if (mAmlHbbTvView.isInitialized() && mAmlHbbTvView.isApplicationRunning()) {
                            mAmlHbbTvView.terminateApplication();
                        }
                     }
               }
            }
        }
    }
}
