package com.amlogic.hbbtv;


import android.net.http.SslError;
import android.util.Log;
import android.webkit.ValueCallback;
import android.view.InputEvent;
import android.text.InputType;
import android.os.Bundle;

import com.vewd.core.sdk.SslErrorHandler;
import com.vewd.core.sdk.ViewBase;
import com.vewd.core.sdk.ViewClient;
import com.vewd.core.sdk.ParentalControlQuery;
import com.vewd.core.sdk.HttpAuthHandler;

import com.vewd.core.shared.WebResourceRequest;
import com.vewd.core.shared.WebResourceResponse;
import com.vewd.core.shared.ClientCertRequest;

import com.amlogic.hbbtv.utils.StringUtils;


/**
 * @ingroup hbbtvclientapi
 * @defgroup amlviewclientapi Aml-View-Client-API
 */
public class AmlViewClient implements ViewClient {
    private static final String TAG = "AmlViewClient";
    private static final boolean DEBUG = true;


    /**
     * @ingroup amlviewclientapi
     * @brief Called on each outgoing network request when dynamic URL filtering is active. The client must respond by calling
     *        ValueCallback.onReceiveValue(T) to decide how to proceed:
     *        passing the unmodified url allows the request to proceed as normal (default behaviour of this method);
     *        passing null denies (blocks) the request;
     *        passing any other value redirects the request to the provided URL.
     *        The client should avoid heavy processing here as this method function might be called very often. The default
     *        behavior is to accept the request. Note: support for data:// and other special URI schemes is not guaranteed.
     * @param url  The url of the request that can be filtered.
     * @param callback Response callback.
     */
    @Override
    public void onBeforeUrlRequest​(String url, ValueCallback<String> callback){
        Log.i(TAG,"onBeforeUrlRequest​  start");

        if (DEBUG) {
            Log.d(TAG,"onBeforeUrlRequest​ : url = " + StringUtils.truncateUrlForLogging(url));
        }

        Log.i(TAG,"onBeforeUrlRequest​  end");
    }

   /**
    * @ingroup amlviewclientapi
    * @brief Notify the host application that a page has started loading.
    * @param view ViewBase that initiated the callback.
    * @param url The url of the page.
    */
    @Override
    public void onPageStarted(ViewBase view, String url) {
        Log.i(TAG,"onPageStarted​  start");

        if (DEBUG) {
            Log.d(TAG,"onPageStarted : url = " + StringUtils.truncateUrlForLogging(url));
        }

        Log.i(TAG,"onPageStarted​  end");
    }

   /**
    * @ingroup amlviewclientapi
    * @brief Notify the host application that a page has finished loading.
    * @param view ViewBase that initiated the callback.
    * @param url The url of the page.
    */
    @Override
    public void onPageFinished(ViewBase view, String url) {
        Log.i(TAG,"onPageFinished  start");

        if (DEBUG) {
            Log.d(TAG,"onPageFinished : url = " + StringUtils.truncateUrlForLogging(url));
        }

        Log.i(TAG,"onPageFinished  end");
    }

    /**
     * @ingroup amlviewclientapi
     * @brief Notify the host application that the ViewBase received parental control query for media playback. This can be
     *        parental control query from:
     *        DVB-DASH specification (9.1.2.3 Parental Rating).
     *        There are two ways to respond:
     *        ParentalControlQuery.allow()
     *        ParentalControlQuery.deny()
     *        Client should choose one response and call it exactly once. By default immediately invokes
     *        ParentalControlQuery.allow() on request.
     * @param view ViewBase that initiated the callback.
     * @param query  An instance of ParentalControlQuery.
     */
    @Override
    public void onParentalControlQuery​(ViewBase view, ParentalControlQuery query) {
        Log.i(TAG,"onParentalControlQuery​  start");

        if (DEBUG) {
            Log.d(TAG,"onParentalControlQuery​ : queryId = " + query.getQueryId());
        }
        query.allow();
        Log.i(TAG,"onParentalControlQuery​  end");
    }

    /**
     * @ingroup amlviewclientapi
     * @brief Notify the host application that the ViewBase received an SSL client certificate request.
     *        There are two ways to respond:
     *        ClientCertRequest.proceed(java.security.PrivateKey, java.security.cert.X509Certificate[]),
     *        ClientCertRequest.cancel(),
     *        ViewBase remembers the response if ClientCertRequest.proceed(java.security.PrivateKey, java.security.cert.
     *        X509Certificate[]) is called and does not trigger onReceivedClientCertRequest(com.vewd.core.sdk.ViewBase, com.vewd.
     *        core.sdk.ClientCertRequest) again for the same host and port pair. ViewBase does not remember the response if
     *        ClientCertRequest.cancel() is called. During the callback, the connection is suspended. The default implementation
     *        immediately cancels the request (see ClientCertRequest.cancel()).
     * @param view ViewBase that initiated the callback.
     * @param request An instance of ClientCertRequest.
     */
    @Override
    public void onReceivedClientCertRequest​(ViewBase view, ClientCertRequest request) {
        Log.i(TAG,"onReceivedClientCertRequest​ start");
        Log.d(TAG, "onReceivedClientCertRequest(): host=" + request.getHost()
            +", port=" + request.getPort());
        //this code don't handle. when need to handle this

        /*Bundle bundle = new Bundle();
        bundle.putString("client-cert-path", "/data/data/com.vewd.core.service/client-cert/1693731657.p12");
        bundle.putString("client-cert-password", "test");
        ClientCertificateUtils.handleClientCertRequest(view.getContext(), request, bundle);
        //request.cancel();*/
        Log.i(TAG,"onReceivedClientCertRequest​ end");
    }

    /**
     * @ingroup amlviewclientapi
     * @brief Report an error to the host application. These errors are unrecoverable (i.e. the main resource is unavailable). The
     *        errorCode parameter corresponds to one of the ERROR_* constants.
     * @param view ViewBase that initiated the callback.
     * @param errorCode The error code corresponding to an ERROR_* value.
     * @param description A String describing the error. Deprecated and unused (always "Error")
     * @param failingUrl The url that failed to load.
     */
    @Override
    public void onReceivedError(ViewBase view, int errorCode,
            String description, String failingUrl) {
        Log.i(TAG,"onReceivedError  start");
        Log.e(TAG, "Received errorCode: " + errorCode + " ,description= " + description
            + ",failingUrl = " + failingUrl);
        Log.i(TAG,"onReceivedError  end");
    }

    /**
     * @ingroup amlviewclientapi
     * @brief Notify the host application that an SSL error occurred while loading a resource. The host application must call
     *        either handler.cancel() or handler.proceed(). Note that the decision may be retained for use in response to future
     *        SSL errors. The default behavior is to cancel the load. By default immediately invokes SslErrorHandler.cancel() on
     *        handler.
     * @param view  ViewBase that initiated the callback.
     * @param handler  An SslErrorHandler used to set the response.
     * @param error  The SSL error object.
     */
    @Override
    public void onReceivedSslError(
            ViewBase view, SslErrorHandler handler, SslError error) {
        Log.i(TAG,"onReceivedSslError  start");
        Log.e(TAG, "Received SSL error: " + error);
        Log.i(TAG,"onReceivedSslError  end");
    }

    /**
     * @ingroup amlviewclientapi
     * @brief Called when a renderer process connected to a view is terminated unexpectedly.
     * @param view ViewBase that initiated the callback.
     */
    @Override
    public void onRendererProcessGone(ViewBase view) {
        Log.i(TAG,"onRendererProcessGone  start");
        Log.e(TAG, "Renderer process died");
        Log.i(TAG,"onRendererProcessGone  end");
    }

    /**
     * @ingroup amlviewclientapi
     * @brief Notifies the host application that the ViewBase received an HTTP authentication request. The host application can use
     *        the supplied HttpAuthHandler to set the ViewBase's response to the request. The default behavior is to cancel the
     *        request.
     * @param view ViewBase that initiated the callback.
     * @param handler The HttpAuthHandler used to set the ViewBase's response.
     * @param host The host requiring authentication.
     * @param realm The realm for which authentication is required.
     */
    @Override
    public void onReceivedHttpAuthRequest​(ViewBase view, HttpAuthHandler handler,
            String host, String realm) {
        Log.i(TAG,"onReceivedHttpAuthRequest​  start");

        if (DEBUG) {
            Log.d(TAG,"onReceivedHttpAuthRequest​: host=" + host + ", realm=" + realm);
        }

        Log.i(TAG,"onReceivedHttpAuthRequest​  end");

    }

    /**
     * @ingroup amlviewclientapi
     * @brief Notify the host application that an input event was not handled by the current ViewBase
     * @param view ViewBase that initiated the callback.
     * @param event The InputEvent that was unhandled by the current page.
     */
    @Override
    public void onUnhandledInputEvent​(ViewBase view, InputEvent event) {
        Log.i(TAG,"onUnhandledInputEvent​  start");

        if (DEBUG) {
            Log.d(TAG,"onUnhandledInputEvent​: inputEvent=" + event);
        }

        Log.i(TAG,"onUnhandledInputEvent​  end");
    }

    /**
     * @ingroup amlviewclientapi
     * @brief Notify the host application that an URL has changed. This is different from onPageStarted(com.vewd.core.sdk.ViewBase,
     *        java.lang.String) in the sense that an URL change initiated by a non-loading navigation action (e.g., anchor) will
     *        also trigger this callback. The method is called on the following events: loading a new URL, redirects, anchor
     *        navigation, history navigation.
     * @param view ViewBase that initiated the callback.
     * @param url The url that has been navigated to.
     */
    @Override
    public void onUrlChanged​(ViewBase view, String url) {
        Log.i(TAG,"onUrlChanged​  start");

        if (DEBUG) {
            Log.d(TAG,"onUrlChanged​ : url = " + StringUtils.truncateUrlForLogging(url));
        }

        Log.i(TAG,"onUrlChanged​  end");
    }

}
