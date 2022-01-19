package com.amlogic.hbbtv.utils;


import android.os.SystemClock;
import android.view.KeyEvent;
import android.util.Log;

import com.vewd.core.shared.Device;
import com.vewd.core.shared.DeviceTypeUtils;

/**
 * @ingroup hbbtvutilsapi
 * @defgroup keyeventutilsapi Key-Event-Utils-API
 */
public class KeyEventUtils {
    private static final String TAG = "KeyEventUtils";

    /**
    * @ingroup keyeventutilsapi.
    * @brief changed the key event code
    * @param event  the KeyEvent instance
    * @param newCode  the key code new value
    * @return KeyEvent  the New KeyEvent which the code has changed
    */
    public static KeyEvent changeKeyEventCode(KeyEvent event, int newCode) {
        return new KeyEvent(event.getDownTime(),
                event.getEventTime(),
                event.getAction(),
                newCode,
                event.getRepeatCount(),
                event.getMetaState(),
                event.getDeviceId(),
                event.getScanCode(),
                event.getFlags(),
                event.getSource());
    }

    /**
    * @ingroup keyeventutilsapi.
    * @brief  Remap the function key to color key
    * @param event  the KeyEvent instance which need to remap
    * @return KeyEvent  the New KeyEvent which will be remap to other key event
    */
    public static KeyEvent remapKeyEvent(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_CLEAR:
                event = changeKeyEventCode(event, KeyEvent.KEYCODE_BACK);
                break;
            case KeyEvent.KEYCODE_BACK:
                event = changeKeyEventCode(event, KeyEvent.KEYCODE_ESCAPE);
                break;
             default:
                break;
        }
        return event;
    }

    /**
    * @ingroup keyeventutilsapi.
    * @brief  synthesize the new key event
    * @param action  the key action(up or down)
    * @param keyCode  the key code
    * @param scanCode  the scan code
    * @return KeyEvent  the New KeyEvent which has remap to color key
    */
    public static KeyEvent synthesizeKeyEvent(int action, int keyCode, int scanCode) {
        long eventTime = SystemClock.uptimeMillis();
        return new KeyEvent(
                eventTime, eventTime, action, keyCode, 0, 0, 0, scanCode);
    }

   /**
    * @ingroup keyeventutilsapi.
    * @brief   juede whether is the exit key
    * @param keyCode  the key code
    * @return ture or false
    */
    public static boolean isExitKey(KeyEvent keyEvent) {
        // Android does not define EXIT key as such, apply some heuristics here
        if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ESCAPE) {
            return true;
        }
        switch (DeviceTypeUtils.detectDevice()) {
            case Device.T22:
                return (keyEvent.getScanCode() == 174);
            default:
                return false;
        }
    }


    /**
     * @ingroup keyeventutilsapi
     * @brief  the key whether media key
     * @return true if this key is media key,othewise false
     */
    public static boolean isMediaKey(int keyCode) {
        Log.i(TAG,"isMediaKey start");
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MUTE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_RECORD:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            Log.i(TAG,"isMediaKey end");
            return true;
        }
        Log.i(TAG,"isMediaKey end");
        return false;
    }

    /**
     * @ingroup keyeventutilsapi
     * @brief  the key whether colour key
     * @return true if this key is colour key,othewise false
     */
    public static boolean isColourKey(int keyCode) {
        Log.i(TAG,"is colour key start");
        switch (keyCode) {
            case KeyEvent.KEYCODE_PROG_RED:
            case KeyEvent.KEYCODE_PROG_GREEN:
            case KeyEvent.KEYCODE_PROG_YELLOW:
            case KeyEvent.KEYCODE_PROG_BLUE:
            Log.i(TAG,"is colour key end");
            return true;
        }
        Log.i(TAG,"is colour key end");
        return false;
    }

     /**
     * @ingroup keyeventutilsapi
     * @brief  the key whether colour key
     * @return true if this key is colour key,othewise false
     */
    public static boolean isBackKey(int keyCode) {
        Log.i(TAG,"is back key start");
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
            Log.i(TAG,"is back key end");
            return true;
        }
        Log.i(TAG,"is back key end");
        return false;
    }

    private KeyEventUtils() {
    }
}
