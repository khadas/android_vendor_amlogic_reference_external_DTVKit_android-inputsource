 package com.amlogic.hbbtv;

 import org.json.JSONArray;
 import org.json.JSONException;
 import org.json.JSONObject;
 import org.droidlogic.dtvkit.DtvkitGlueClient;
 import android.util.Log;


 public class HbbTvUISetting {

    public static final String TAG = "HbbTvUISetting";

   /**
    * @ingroup hbbtvuisetting
    * @brief  get hbbtv feature
    * @return true if the system support hbbtv feature,otherwise false
    */
    public boolean getHbbTvFeature() {
        boolean hasHbbTvFeather = false;
        try {
            JSONArray args = new JSONArray();
            boolean result = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetHbbtvFeature", args).getBoolean("data");
            Log.d(TAG,"the result = " + result );
            hasHbbTvFeather = result;

        } catch (Exception e) {
            Log.e(TAG, "hasHbbTvFeather = " + e.getMessage());
        }

        return hasHbbTvFeather;
    }

   /**
    * @ingroup hbbtvuisetting
    * @brief  set hbbtv feature
    * @param  status  ture or false
    * @return none
    */
    public void setHbbTvFeature(boolean status) {
        try {
            JSONArray args = new JSONArray();
            args.put(status);
            boolean setHbbtvFeatrue = DtvkitGlueClient.getInstance().request("Hbbtv.HBBEnableHbbtvFeature", args).getBoolean("data");
            if (setHbbtvFeatrue) {
                Log.d(TAG, "set hbbtv feature sucess");
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

   /**
    * @ingroup hbbtvuisetting
    * @brief  get hbbtv service status for current channel
    * @return true if current channel support show hbbtv applicaiton,otherwise false
    */
    public boolean getHbbTvServiceStatusForCurChannel() {
        boolean serviceHasHbbTv = false;
        try {
            JSONArray args = new JSONArray();
            boolean result = DtvkitGlueClient.getInstance().request("Hbbtv.HBBGetHbbtvStatusForCurChannel", args).getBoolean("data");
            Log.d(TAG,"the result = " + result );
            serviceHasHbbTv = result;
        } catch (Exception e) {
            Log.e(TAG, "serviceHasHbbTv = " + e.getMessage());
        }
        return serviceHasHbbTv;
    }

   /**
    * @ingroup hbbtvuisetting
    * @brief  set hbbtv service status for current channel
    * @param  status  ture or false
    * @return none
    */
    public void setHbbTvServiceStatusForCurChannel(boolean status) {
        try {
            JSONArray args = new JSONArray();
            args.put(status);
            boolean setHbbtvServiceStatus = DtvkitGlueClient.getInstance().request("Hbbtv.HBBSwitchHbbtvStatusForCurChannel", args).getBoolean("data");
            if (setHbbtvServiceStatus) {
                Log.d(TAG, "set service hbbtv status sucess");
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

   /**
    * @ingroup hbbtvuisetting
    * @brief  get hbbtv tracking status
    * @return true if the system support hbbtv tracking,otherwise false
    */
    public boolean getHbbTvTrackingStatus() {
        return false;
    }

   /**
    * @ingroup hbbtvuisetting
    * @brief  set hbbtv tracking status
    * @param  status  ture or false
    * @return none
    */
    public void setHbbTvTrackingStatus(boolean status) {}

   /**
    * @ingroup hbbtvuisetting
    * @brief  get hbbtv cookies status
    * @return true if the system support hbbtv record the cookies of browser ,otherwise false
    */
    public boolean getHbbtvCookiesStatus() {
        return false;
    }

   /**
    * @ingroup hbbtvuisetting
    * @brief  set hbbtv cookies status
    * @param  status  ture or false
    * @return none
    */
    public void setHbbTvCookiesStatus(boolean status) {}

   /**
    * @ingroup hbbtvuisetting
    * @brief  clear cookies of browser
    * @return none
    */
    public void clearHbbTvCookies() {}


   /**
    * @ingroup hbbtvuisetting
    * @brief  get hbbtv Distinctive identifiers status
    * @return true if the system support hbbtv tracking,otherwise false
    */
    public boolean getHbbTvDistinctiveIdentifierStatus() {
        return false;
    }

   /**
    * @ingroup hbbtvuisetting
    * @brief  set hbbtv Distinctive identifiers status
    * @param  status  ture or false
    * @return none
    */
    public void setHbbTvDistinctiveIdentifierStatus(boolean status) {}



}
