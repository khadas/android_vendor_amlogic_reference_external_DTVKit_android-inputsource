package com.amlogic.hbbtv.utils;

import android.util.ArraySet;
import android.util.Log;

import com.amlogic.hbbtv.utils.ThreadUtils;
import com.vewd.core.sdk.BroadcastResource;
import com.vewd.core.sdk.BroadcastResourceClient;
import com.vewd.core.sdk.BroadcastResourceReleaseRequest;

public class BroadcastResourceManager {
    private static final String TAG = "tvsdk_BroadcastResourceManager";

    public static BroadcastResourceManager getInstance() {
        return sManager;
    }

    public void register(BroadcastResourceClient client) {
        ThreadUtils.checkMainThread();
        if (mClients.isEmpty()) {
            mBroadcastResource = new BroadcastResource();
            mBroadcastResource.init();
            mBroadcastResource.setResourceNeeded();
            mBroadcastResource.setBroadcastResourceClient(mBroadcastResourceClient);
        }
        mClients.add(client);
    }

    public void unregister(BroadcastResourceClient client) {
        ThreadUtils.checkMainThread();
        mClients.remove(client);
        if (client == mActiveClient) {
            mActiveClient = null;
        }
        if (mBroadcastResource != null && mClients.isEmpty()) {
            mBroadcastResource.dispose();
            mBroadcastResource = null;
            mIsGranted = false;
        }
    }

    public void setActive(BroadcastResourceClient client) {
        ThreadUtils.checkMainThread();
        if (!mClients.contains(client)) {
            throw new RuntimeException("Client not registered");
        }
        mActiveClient = client;
    }

    public boolean isGranted() {
        ThreadUtils.checkMainThread();
        return mIsGranted;
    }

    public void forceRequestBroadcastResource() {
        ThreadUtils.checkMainThread();
        mBroadcastResource.forceRequestBroadcastResource();
    }

    private BroadcastResourceManager(){};

    private final BroadcastResourceClient mBroadcastResourceClient =
            new BroadcastResourceClient() {
        @Override
        public void onResourceGranted() {
            Log.d(TAG, "Resource granted");
            ThreadUtils.checkMainThread();
            mIsGranted = true;
            if (mActiveClient != null) {
                mActiveClient.onResourceGranted();
            }
        }

        @Override
        public void onResourceReleaseRequested(BroadcastResourceReleaseRequest request) {
            Log.d(TAG, "Resource release requested");
            ThreadUtils.checkMainThread();
            mIsGranted = false;
            if (mActiveClient != null) {
                mActiveClient.onResourceReleaseRequested(request);
            } else {
                request.confirm();
            }
        }
    };

    private static BroadcastResourceManager sManager = new BroadcastResourceManager();
    private BroadcastResource mBroadcastResource;
    private boolean mIsGranted;
    private ArraySet<BroadcastResourceClient> mClients = new ArraySet<>();
    private BroadcastResourceClient mActiveClient;
}
