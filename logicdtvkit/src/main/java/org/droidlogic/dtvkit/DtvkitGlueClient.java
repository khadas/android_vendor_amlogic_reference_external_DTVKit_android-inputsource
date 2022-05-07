package org.droidlogic.dtvkit;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;


public class DtvkitGlueClient {
    private static final String TAG = "DtvkitGlueClient";

    public static final int INDEX_FOR_MAIN = 0;
    public static final int INDEX_FOR_PIP = 1;
    public static final int DIRECT_BUFFER_SIZE = 1880;
    private static final int REQUEST_MESSAGE_TIMEOUT_SHORT_MILLIS = 1000;
    private static final int REQUEST_MESSAGE_TIMEOUT_LONG_MILLIS = 3000;
    private static final int REQUEST_MESSAGE_BOMB_MILLIS = 15000;
    private Handler mMainHandler = null;

    private static DtvkitGlueClient mSingleton = null;
    private final ArrayList<Pair<Integer, SignalHandler>> mHandlers = new ArrayList<>();
    // Notification object used to listen to the start of the rpcserver daemon.
    //private final ServiceNotification mServiceNotification = new ServiceNotification();
    //private static final int DTVKITSERVER_DEATH_COOKIE = 1000;
   // private IDTVKitServer mProxy = null;
    //private HALCallback mHALCallback;
    private OverlayTarget mTarget;
    private AudioHandler mAudioHandler;
    private ByteBuffer mDirectBuffer = null;
   // private SystemControlHandler mSysControlHandler;
    private SubtitleListener mListener;
    private PidFilterListener mPidListener;
    private native void nativeconnectdtvkit(DtvkitGlueClient client, ByteBuffer buffer);
    private native void nativedisconnectdtvkit();
    private native void nativeSetMutilSurface(int index, Surface surface);
    private native void nativeSetSurface(Surface surface);
    private native void nativeSetSurfaceToPlayer(Surface surface);
    private native String nativerequest(String resource, String json);
    private native void native_attachSubtitleCtl(int flag);
    private native void native_detachSubtitleCtl();
    private native boolean nativeIsdbtSupport();
    private native void native_UnCrypt(String src, String dest);
    private native void native_openUserData();
    private native void native_closeUserData();
    private native void native_nativeSubtitleSeekReset();
    private native void native_setRegionId(int regionId);
    static {
        System.loadLibrary("dtvkit_jni");
    }

        //native callback
    public void notifySubtitleCallback(int width, int height, int dstx, int dsty, int dstwidth, int dstheight, int[] data)
    {
         Log.d(TAG, "notifySubtitleCallBack received!!! width = " + width + ", heigth = " + height);
         if (mTarget != null) {
            mTarget.draw(width, height, dstx, dsty, dstwidth, dstheight, data);
         }
    }

    public void notifyPidFilterData() {
        //Log.d(TAG, "notifyPidFilterData received!!!");
        if (mPidListener != null) {
            mDirectBuffer.clear();
            mPidListener.onPidFilterData(mDirectBuffer);//ByteBuffer data
        }
    }

    public void notifySubtitleCallbackEx(int type, int width, int height, int dstx, int dsty, int dstwidth, int dstheight, int[] data)
    {
         Log.d(TAG, "notifySubtitleCallBackEx received!!! width = " + width + ", heigth = " + height);
         if (mListener != null) {
            mListener.drawEx(type, width, height, dstx, dsty, dstwidth, dstheight, data);
         }
    }

    public void notifySubtitleCbCtlEx(int pause)
    {
         Log.d(TAG, "notifySubtitleCbCtlEx received!!! pause = " + pause);
         if (mListener != null) {
            mListener.pauseEx(pause);
         }
    }

    public void notifyCCSubtitleCallbackEx(boolean bshow, String json) {
        Log.d(TAG, "notifyCCSubtitleCallbackEx received!!!" + json);
        if (mListener != null) {
            mListener.drawCC(bshow, json);
        }
    }

    public void notifyDvbCallback(String resource, String json, int id) {
        Log.i(TAG, "notifyDvbCallback received!!! resource" + "(" + id + ")" + ",json" + json);
        JSONObject object;
        try {
            if (json.charAt(0) == '[') {
                JSONArray array = new JSONArray(json);
                object = new JSONObject();
                object.put("result", array);
            } else {
                object = new JSONObject(json);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return;
        }
        synchronized (mHandlers) {
            for (Pair <Integer, SignalHandler> handler :mHandlers) {
                if (handler.first == id)
                    handler.second.onSignal(resource, object);
            }
        }
    }

    public void notifyMixVideoEventCallback(int event) {
        Log.d(TAG, "notifyMixVideoEventCallback received!!! event =" + event);
        if (mListener != null) {
            mListener.mixVideoEvent(event);
        }
    }

   public void doUnCrypt(String src, String dest) {
        native_UnCrypt(src, dest);
   }

   public void setMutilSurface(int index, Surface sh) {
        nativeSetMutilSurface(index, sh);
   }

   public void setDisplay(Surface sh) {
        nativeSetSurface(sh);
   }

   public void disConnectDtvkitClient() {
        nativedisconnectdtvkit();
        native_detachSubtitleCtl();
   }


/*
    final class ServiceNotification extends IServiceNotification.Stub {
        @Override
        public void onRegistration(String fqName, String name, boolean preexisting) {
            Log.i(TAG, "rpcserver HIDL service started " + fqName + " " + name);
            connectToProxy();
        }
    }

    private void connectToProxy() {
        synchronized (mLock) {
            if (mProxy != null) {
                return;
            }

            try {
                mProxy = IDTVKitServer.getService();
                mProxy.linkToDeath(new DeathRecipient(), DTVKITSERVER_DEATH_COOKIE);
                mProxy.setCallback(mHALCallback, ConnectType.TYPE_EXTEND);
            } catch (NoSuchElementException e) {
                Log.e(TAG, "connectToProxy: DTVKitServer HIDL service not found."
                        + " Did the service fail to start?", e);
            } catch (RemoteException e) {
                Log.e(TAG, "connectToProxy: DTVKitServer HIDL service not responding", e);
            }
        }

        Log.i(TAG, "connect to DTVKitServer HIDL service success");
    }

    private static class HALCallback extends IDTVKitServerCallback.Stub {
        DtvkitGlueClient DtvkitClient;
        HALCallback(DtvkitGlueClient dkgc) {
            DtvkitClient = dkgc;
    }

    public void notifyCallback(DTVKitHidlParcel parcel) {
        Log.i(TAG, "resource" + parcel.resource + ",json" + parcel.json);
        JSONObject object;
        try {
            object = new JSONObject(parcel.json);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return;
        }

        for (SignalHandler handler : DtvkitClient.mHandlers) {
            handler.onSignal(parcel.resource, object);
        }
        }
    }

    final class DeathRecipient implements HwBinder.DeathRecipient {
        DeathRecipient() {
        }

        @Override
        public void serviceDied(long cookie) {
            if (DTVKITSERVER_DEATH_COOKIE == cookie) {
                Log.e(TAG, "dtvkitserver HIDL service died cookie: " + cookie);
                synchronized (mLock) {
                    mProxy = null;
                }
            }
        }
    }
*/

    public interface AudioHandler {
       void onEvent(String signal, JSONObject data);
    }

    public interface SignalHandler {
        void onSignal(String signal, JSONObject data);
    }

    public interface OverlayTarget {
        void draw(int src_width, int src_height, int dst_x, int dst_y, int dst_width, int dst_height, int[] data);
    }

    public interface SubtitleListener {
        void drawEx(int parserType, int src_width, int src_height, int dst_x, int dst_y, int dst_width, int dst_height, int[] data);
        void pauseEx(int pause);
        void drawCC(boolean bshow, String json);
        void mixVideoEvent(int event);
    }

    public interface PidFilterListener {
        void onPidFilterData(ByteBuffer data);
    }

    protected DtvkitGlueClient() {
        // Singleton
        mDirectBuffer = ByteBuffer.allocateDirect(DIRECT_BUFFER_SIZE);
        nativeconnectdtvkit(this, mDirectBuffer);
        /*
        int debuggable = SystemProperties.getInt("ro.debuggable", 0);
        if (debuggable == 1) {
            HandlerThread thread = new HandlerThread("GlueClientThread");
            thread.start();
            mMainHandler = new Handler(thread.getLooper());
        }
        */
    }

    public synchronized static DtvkitGlueClient getInstance() {
        if (mSingleton == null) {
            mSingleton = new DtvkitGlueClient();
        }
        return mSingleton;
    }

    private void registerSignalHandlerUnlock(SignalHandler handler, int id) {
        mHandlers.add(new Pair<Integer, SignalHandler>(id, handler));
    }

    private void unregisterSignalHandlerUnlock(SignalHandler handler) {
        Iterator<Pair<Integer,SignalHandler>> it = mHandlers.iterator();
        while (it.hasNext()) {
            Pair<Integer, SignalHandler> pair = it.next();
            if (pair.second == handler)
                it.remove();
        }
    }

    public void registerSignalHandler(SignalHandler handler, int id) {
        synchronized (mHandlers) {
            unregisterSignalHandlerUnlock(handler);
            registerSignalHandlerUnlock(handler, id);
        }
    }

    public void registerSignalHandler(SignalHandler handler) {
        registerSignalHandler(handler, INDEX_FOR_MAIN);
    }

    public void unregisterSignalHandler(SignalHandler handler) {
        synchronized (mHandlers) {
            unregisterSignalHandlerUnlock(handler);
        }
    }

    public void setOverlayTarget(OverlayTarget target) {
        mTarget = target;
    }

    public void setSubtileListener(SubtitleListener Listener) {
        mListener = Listener;
    }

    public void setPidFilterListener(PidFilterListener Listener) {
        mPidListener = Listener;
    }

    public JSONObject request(String resource, JSONArray arguments) throws Exception {
        final String reason = resource + " : " + arguments;
        long startTime = System.nanoTime();
        try {
            JSONObject object = new JSONObject(nativerequest(resource, arguments.toString()));
            if (object.getBoolean("accepted")) {
                return object;
            } else {
                throw new Exception(object.getString("data"));
            }
        } catch (JSONException | RemoteException e) {
            throw new Exception(e.getMessage());
        } finally {
            long durationMs = (System.nanoTime() - startTime) / (1000 * 1000);
            if (durationMs > REQUEST_MESSAGE_TIMEOUT_LONG_MILLIS) {
                Log.e(TAG, "[critical]request (" + reason + ") took too long time (duration="
                    + durationMs + "ms)");
            } else if (durationMs > REQUEST_MESSAGE_TIMEOUT_SHORT_MILLIS) {
                Log.w(TAG, "[warning]request (" + reason + ") took a long time (duration="
                    + durationMs + "ms)");
            }
        }
    }

/*
    public String readBySysControl(int ftype, String name) {
        String value = null;
        if (mSysControlHandler != null) {
            value = mSysControlHandler.onReadSysFs(ftype, name);
        }
        return value;
    }

    public void writeBySysControl(int ftype, String name, String cmd) {
        if (mSysControlHandler != null) {
            mSysControlHandler.onWriteSysFs(ftype, name, cmd);
        }
    }

    public void setSystemControlHandler(SystemControlHandler l) {
          mSysControlHandler = l;
    }

    public interface SystemControlHandler {
        public String onReadSysFs(int ftype, String name);
        public void onWriteSysFs(int ftype, String name, String cmd);
    }
    */

    public void subtitleSeekReset() {
        native_nativeSubtitleSeekReset();
    }

    public void attachSubtitleCtl(int flag) {
        native_attachSubtitleCtl(flag);
    }

    public boolean isdbtSupport() {
        return nativeIsdbtSupport();
    }

    public void openUserData() {
        native_openUserData();
    }

    public void closeUserData() {
        native_closeUserData();
    }

    public void setRegionId(int regionId) {
        native_setRegionId(regionId);
    }

}
