package com.amlogic.hbbtv;


import android.graphics.Rect;
import android.webkit.ConsoleMessage;
import android.graphics.Bitmap;
import android.util.Log;

import com.vewd.core.sdk.ChromeClient;
import com.vewd.core.sdk.ViewBase;
import com.vewd.core.sdk.JsResult;
import com.vewd.core.sdk.JsPromptResult;
import com.vewd.core.sdk.PersistentStorageQuotaRequestHandler;
import com.vewd.core.shared.WebContext;


/**
 * @ingroup hbbtvclientapi
 * @defgroup amlchromeclientapi Aml-Chrome-Client-API
 */
public class AmlChromeClient implements ChromeClient {
    public static final String TAG = "AmlChromeClient";
    public static final boolean DEBUG = true;

   /**
    * @ingroup amlchromeclientapi
    * @brief Notifies the client that visibility of focused area has changed and ViewBase is not able to scroll focusedRect into
    *        visibleRect internally. The client should handle this call and apply a view transform that makes the focused area visible.
    * @param view ViewBase that initiated the callback.
    * @param visibleRect Visible area of the ViewBase or null if no clipping occurs.
    * @param focusedRect Focused area that has been clipped or null if no clipping occurs.
    */
    @Override
    public void onFocusedAreaVisibilityChanged(ViewBase view,
            Rect visibleRect, Rect focusedRect) {
        Log.i(TAG,"onFocusedAreaVisibilityChanged  start");

        int offsetX = 0;
        int offsetY = 0;
        if (focusedRect != null && visibleRect != null) {
            int desiredX = (visibleRect.width() - focusedRect.width()) / 2;
            int desiredY = (visibleRect.height() - focusedRect.height()) / 2;
            offsetX = desiredX - focusedRect.left;
            offsetX = Math.min(offsetX, visibleRect.left - view.getBottom());
            offsetX = Math.max(offsetX, visibleRect.right - view.getRight());
            offsetY = desiredY - focusedRect.top;
            offsetY = Math.min(offsetY, visibleRect.top - view.getTop());
            offsetY = Math.max(offsetY, visibleRect.bottom - view.getBottom());
        }
        view.setX(offsetX);
        view.setY(offsetY);

        Log.i(TAG,"onFocusedAreaVisibilityChanged  start");
    }

    /**
     * @ingroup amlchromeclientapi
     * @brief Tell the client to show this ViewBase in fullscreen mode. This can be achieved by setting fullscreen flag in
     *        Activity's window, and if required, removing ViewBase from its current layout and putting it into window's decor view for
     *        example. Note: Any layouts used by video backend may also have to be moved.
     * @param view The ViewBase to be shown in fullscreen mode.
     */
    @Override
    public void enterFullscreenMode​(ViewBase view) {
        Log.i(TAG,"enterFullscreenMode​  start");
        Log.i(TAG,"enterFullscreenMode​  end");
    }

    /**
     * @ingroup amlchromeclientapi
     * @brief Tell the client to stop showing this ViewBase in fullscreen mode.
     * @param view The ViewBase to be switched back to normal mode.
     */
    @Override
    public void exitFullscreenMode​(ViewBase view) {
        Log.i(TAG,"exitFullscreenMode​  start");
        Log.i(TAG,"exitFullscreenMode​  end");
    }

   /**
    * @ingroup amlchromeclientapi
    * @brief Notify the host application to close the given ViewBase and remove it from the view system if necessary.
    * @param view The ViewBase that needs to be closed.
    */
    @Override
    public void onCloseWindow​(ViewBase view) {
        Log.i(TAG,"onCloseWindow​  start");
        Log.i(TAG,"onCloseWindow​  end");
    }

   /**
    * @ingroup amlchromeclientapi
    * @brief Reports a JavaScript console message to the host application. The console message is limited to 100KB
    * @param consoleMessage Object containing details of the console message.
    */
    @Override
    public void onConsoleMessage​(ConsoleMessage consoleMessage) {
        Log.i(TAG,"onConsoleMessage​  start");
        Log.i(TAG,"onConsoleMessage​  end");
    }

   /**
    * @ingroup amlchromeclientapi
    * @brief Request the host application to create a new window. If the host application chooses to honor this request, it shall
    *        return true from this method, create a new WebView to host the window and initialize it with given WebContext. If the
    *        host application chooses not to honor the request, it shall return false from this method. By default immediately
    *        returns false.
    * @param view The ViewBase from which the request for a new window originated
    * @param isDialog True if the new window should be a dialog, rather than a full-size window.
    * @param isUserGesture True if the request was initiated by a user gesture, such as the user clicking a link.
    * @param webContext The web context to initialize new WebView.
    * @return Flag indicating whether the client will handle request
    */
    @Override
    public boolean onCreateWindow​(ViewBase view, boolean isDialog, boolean isUserGesture, WebContext webContext) {
        Log.i(TAG,"onCreateWindow​  start");
        Log.i(TAG,"onCreateWindow​  end");

        return false;
    }

   /**
    * @ingroup amlchromeclientapi
    * @brief Tell the client to display a javascript alert dialog. If the client returns true, ViewBase will assume that the
    *        client will handle the dialog. If the client returns false, it will continue execution. By default immediately
    *        returns false.
    * @param view The ViewBase that initiated the callback
    * @param url  The url of the page requesting the dialog.
    * @param message Message to be displayed in the window.
    * @param result  A JsResult to confirm that the user hit enter.
    * @return boolean Whether the client will handle the alert dialog
    */
    @Override
    public boolean onJsAlert​(ViewBase view, String url, String message, JsResult result) {
        Log.i(TAG,"onJsAlert​  start");
        Log.i(TAG,"onJsAlert​  end");
        return true;
    }

   /**
    * @ingroup amlchromeclientapi
    * @brief Tell the client to display a confirm dialog to the user. If the client returns true, ViewBase will assume that the
    *        client will handle the confirm dialog and call the appropriate JsResult method. If the client returns false, a
    *        default value of false will be returned to javascript. The default behavior is to return false. By default
    *        immediately returns false.
    * @param view The ViewBase that initiated the callback
    * @param url  The url of the page requesting the dialog.
    * @param message Message to be displayed in the window.
    * @param result  A JsResult used to send the user's response to javascript.
    * @return boolean Whether the client will handle the confirm dialog
    */
    @Override
    public boolean onJsConfirm​(ViewBase view, String url, String message, JsResult result) {
        Log.i(TAG,"onJsConfirm​  start");
        Log.i(TAG,"onJsConfirm​  end");
        return true;

    }


   @Override
   public boolean onJsPrompt(ViewBase view, String url, String message,
           String defaultValue, final JsPromptResult result) {
       Log.i(TAG,"onJsPrompt start");
       Log.i(TAG,"onJsPrompt end");
       return true;
   }

   /**
    * @ingroup amlchromeclientapi
    * @brief Called when (if) a new favicon is available. Note: this API is currently provisional and may not work as expected.
    * @param view The ViewBase that this favicon corresponds to
    * @param icon The icon. May be null
    */
    @Override
    public void onReceivedIcon​(ViewBase view, Bitmap icon) {
        Log.i(TAG,"onReceivedIcon​  start");
        Log.i(TAG,"onReceivedIcon​  end");
    }

   /**
    * @ingroup amlchromeclientapi
    * @brief Request display and focus for this ViewBase. This may happen due to another ViewBase opening a link in this ViewBase
    *        and requesting that this ViewBase be displayed.
    * @param view The ViewBase that needs to be focused.
    */
    @Override
    public void onRequestFocus​(ViewBase view) {
        Log.i(TAG,"onRequestFocus​  start");
        Log.i(TAG,"onRequestFocus​  end");
    }

   /**
    * @ingroup amlchromeclientapi
    * @brief Asks the client if requested persistent storage quota should be granted. By default immediately invokes
    *        PersistentStorageQuotaRequestHandler.deny() on handler.
    * @param url Url which requests quota.
    * @param requestedQuota Amount of requested quota in bytes.
    * @param handler The PersistentStorageQuotaRequestHandler callback used to reply.
    */
    @Override
    public void onRequestPersistentStorageQuotaPermission​(String url, long requestedQuota,
            PersistentStorageQuotaRequestHandler handler) {
        Log.i(TAG,"onRequestPersistentStorageQuotaPermission​  start");

        if (DEBUG) {
            Log.d(TAG,"onRequestPersistentStorageQuotaPermission​ : url = " + url + ", requestedQuota = " + requestedQuota);
        }

        Log.i(TAG,"onRequestPersistentStorageQuotaPermission​  end");
    }

   /**
    * @ingroup amlchromeclientapi
    * @brief Client callback called to let the integration decide to keep or close the window after window.close has
    * been called from JavaScript. If false is returned then onCloseWindow(com.vewd.core.sdk.ViewBase) won't be called. By default
    * By default immediately returns true.
    * @param view The ViewBase view to be closed.
    * @return true if the view should be closed normally, false to prevent closing.
    */
    @Override
    public boolean shouldCloseWindow​(ViewBase view) {
        Log.i(TAG,"shouldCloseWindow​  start");
        Log.i(TAG,"shouldCloseWindow​  end");
        return true;
    }

   /**
    * @ingroup amlchromeclientapi
    * @brief Called to determine whether on-screen keyboard should be suppressed when focusing a form element. By default
    *        immediately returns true.
    * @param view The ViewBase that is initiating the callback.
    * @return true if on-screen keyboard should be suppressed, false otherwise.
    */
    @Override
    public boolean shouldSuppressOnScreenKeyboard​(ViewBase view) {
        Log.i(TAG,"shouldSuppressOnScreenKeyboard​  start");
        Log.i(TAG,"shouldSuppressOnScreenKeyboard​  end");
        return false;
    }
}
