 package com.amlogic.hbbtv;

 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 import org.droidlogic.dtvkit.DtvkitGlueClient;
 import android.util.Log;
 import com.vewd.core.sdk.CookieManager;


 public class HbbTvUISetting {

    public static final String TAG = "HbbTvUISetting";

   /**
    * @ingroup hbbtvuisetting
    * @brief  get hbbtv feature
    * @return true if the system support hbbtv feature,otherwise false
    */
    public boolean getHbbTvFeature() {
        Log.i(TAG,"getHbbTvFeature start");
        boolean hasHbbTvFeather = false;
        try {
            JSONArray args = new JSONArray();
            boolean result = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetHbbtvFeature", args).getBoolean("data");
            Log.d(TAG,"the result = " + result );
            hasHbbTvFeather = result;

        } catch (Exception e) {
            Log.e(TAG, "hasHbbTvFeather = " + e.getMessage());
        }
        Log.i(TAG,"getHbbTvFeature end");
        return hasHbbTvFeather;
    }

   /**
    * @ingroup hbbtvuisetting
    * @brief  set hbbtv feature
    * @param  status  ture or false
    * @return none
    */
    public void setHbbTvFeature(boolean status) {
        Log.i(TAG,"setHbbTvFeature start");
        Log.d(TAG,"setHbbTvFeature status = " + status);
        try {
            JSONArray args = new JSONArray();
            args.put(status);
            boolean setHbbtvFeatrue = DtvkitGlueClient.getInstance().request("Hbbtv.HBBEnableHbbtvFeature", args).getBoolean("data");
            if (setHbbtvFeatrue) {
                Log.d(TAG, "set hbbtv feature sucess");
            }
            HbbTvManager mHbbTvManager = HbbTvManager.getInstance();
            mHbbTvManager.setHbbTvApplication(status);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        Log.i(TAG,"setHbbTvFeature end");
    }

   /**
    * @ingroup hbbtvuisetting
    * @brief  get hbbtv service status for current channel
    * @return true if current channel support show hbbtv applicaiton,otherwise false
    */
    public boolean getHbbTvServiceStatusForCurChannel() {
        Log.i(TAG,"getHbbTvServiceStatusForCurChannel start");
        boolean serviceHasHbbTv = false;
        try {
            JSONArray args = new JSONArray();
            boolean result = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetHbbtvStatusForCurChannel", args).getBoolean("data");
            Log.d(TAG,"the result = " + result );
            serviceHasHbbTv = result;
        } catch (Exception e) {
            Log.e(TAG, "serviceHasHbbTv = " + e.getMessage());
        }
        Log.i(TAG,"getHbbTvServiceStatusForCurChannel end");
        return serviceHasHbbTv;
    }

   /**
    * @ingroup hbbtvuisetting
    * @brief  set hbbtv service status for current channel
    * @param  status  ture or false
    * @return none
    */
    public void setHbbTvServiceStatusForCurChannel(boolean status) {
        Log.i(TAG,"setHbbTvServiceStatusForCurChannel start");
        Log.d(TAG,"setHbbTvServiceStatusForCurChannel status = " + status);
        try {
            JSONArray args = new JSONArray();
            args.put(status);
            boolean setHbbtvServiceStatus = DtvkitGlueClient.getInstance().request("Hbbtv.HBBSwitchHbbtvStatusForCurChannel", args).getBoolean("data");
            if (setHbbtvServiceStatus) {
                Log.d(TAG, "set service hbbtv status sucess");
            }
            //HbbTvManager mHbbTvManager = HbbTvManager.getInstance();
            //mHbbTvManager.setHbbTvApplication(setHbbtvServiceStatus);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        Log.i(TAG,"setHbbTvServiceStatusForCurChannel end");
    }

   /**
    * @ingroup hbbtvuisetting
    * @brief  get hbbtv tracking status
    * @return true if the system support hbbtv tracking,otherwise false
    */
    public boolean getHbbTvTrackingStatus() {
        Log.i(TAG,"getHbbTvTrackingStatus start");
        boolean trackingStatus = false;
        try {
            JSONArray args = new JSONArray();
            boolean result = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetTrackingStatus", args).getBoolean("data");
            Log.d(TAG,"the result = " + result );
            trackingStatus = result;
        } catch (Exception e) {
            Log.e(TAG, "trackingStatus = " + e.getMessage());
        }
        Log.i(TAG,"getHbbTvTrackingStatus end");
        return trackingStatus;
    }

   /**
    * @ingroup hbbtvuisetting
    * @brief  set hbbtv tracking status
    * @param  status  ture or false
    * @return none
    */
    public void setHbbTvTrackingStatus(boolean status) {
        Log.i(TAG,"setHbbTvTrackingStatus start");
        Log.d(TAG,"setHbbTvTrackingStatus status = " + status);
        try {
            JSONArray args = new JSONArray();
            args.put(status);
            boolean hbbtvTrackingStatus = DtvkitGlueClient.getInstance().request("Hbbtv.HBBEnableTrackingStatus", args).getBoolean("data");
            if (hbbtvTrackingStatus) {
                Log.d(TAG, "hbbtv tracking status set sucessfully");
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        Log.i(TAG,"setHbbTvTrackingStatus end");
    }

   /**
    * @ingroup hbbtvuisetting
    * @brief  get hbbtv cookies status
    * @return true if the system support hbbtv record the cookies of browser ,otherwise false
    */
    public boolean getHbbtvCookiesStatus() {
        Log.i(TAG,"getHbbtvCookiesStatus start");
        boolean cookieStatus = false;
        try {
            JSONArray args = new JSONArray();
            boolean result = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetCookiesStatus", args).getBoolean("data");
            Log.d(TAG,"the result = " + result );
            cookieStatus = result;
        } catch (Exception e) {
            Log.e(TAG, "cookieStatus = " + e.getMessage());
        }
        Log.i(TAG,"getHbbtvCookiesStatus end");
        return cookieStatus;
    }

   /**
    * @ingroup hbbtvuisetting
    * @brief  set hbbtv cookies status
    * @param  status  ture or false
    * @return none
    */
    public void setHbbTvCookiesStatus(boolean status) {
        Log.i(TAG,"setHbbTvCookiesStatus start");
        Log.d(TAG,"setHbbTvCookiesStatus status = " + status);
        try {
            JSONArray args = new JSONArray();
            args.put(status);
            boolean hbbtvCookieStatus = DtvkitGlueClient.getInstance().request("Hbbtv.HBBEnableCookiesStatus", args).getBoolean("data");
            if (hbbtvCookieStatus) {
                Log.d(TAG, "hbbtv cookies status set sucessfully");
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        Log.i(TAG,"setHbbTvCookiesStatus end");
    }

   /**
    * @ingroup hbbtvuisetting
    * @brief  clear cookies of browser
    * @return none
    */
    public void clearHbbTvCookies() {
        Log.i(TAG,"clearHbbTvCookies start");
        HbbTvManager hbbTvManager = HbbTvManager.getInstance();
        AmlHbbTvView amlHbbtvView = hbbTvManager.getHbbTvView();
        if (amlHbbtvView != null && amlHbbtvView.isInitialized()) {
            Log.d(TAG,"clear cookies");
            CookieManager cookieManager = amlHbbtvView.getCookieManager();
            cookieManager.removeAllCookies(null);
        }
        Log.i(TAG,"clearHbbTvCookies end");
    }


   /**
    * @ingroup hbbtvuisetting
    * @brief  get hbbtv Distinctive identifiers status
    * @return true if the system support hbbtv tracking,otherwise false
    */
    public boolean getHbbTvDistinctiveIdentifierStatus() {
        Log.i(TAG,"getHbbTvDistinctiveIdentifierStatus start");
        boolean hbbtvDistinctiveIdStatus = false;
        try {
            JSONArray args = new JSONArray();
            boolean result = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetDeviceIDStatus", args).getBoolean("data");
            Log.d(TAG,"the result = " + result );
            hbbtvDistinctiveIdStatus = result;
        } catch (Exception e) {
            Log.e(TAG, "hbbtvDistinctiveIdStatus = " + e.getMessage());
        }
        Log.i(TAG,"getHbbTvDistinctiveIdentifierStatus end");
        return hbbtvDistinctiveIdStatus;
    }

   /**
    * @ingroup hbbtvuisetting
    * @brief  set hbbtv Distinctive identifiers status
    * @param  status  ture or false
    * @return none
    */
    public void setHbbTvDistinctiveIdentifierStatus(boolean status) {
        Log.i(TAG,"setHbbTvDistinctiveIdentifierStatus start");
        Log.d(TAG,"setHbbTvDistinctiveIdentifierStatus status = " + status);
        try {
            JSONArray args = new JSONArray();
            args.put(status);
            boolean hbbtvDistinctiveIdStatus = DtvkitGlueClient.getInstance().request("Hbbtv.HBBEnableDeviceIDStatus", args).getBoolean("data");
            if (hbbtvDistinctiveIdStatus) {
                Log.d(TAG, "hbbtv distinctive id status set sucessfully");
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        Log.i(TAG,"setHbbTvDistinctiveIdentifierStatus end");
    }



}
