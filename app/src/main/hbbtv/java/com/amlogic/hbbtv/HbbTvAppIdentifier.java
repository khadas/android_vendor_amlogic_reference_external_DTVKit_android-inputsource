package com.amlogic.hbbtv;


import android.text.TextUtils;

import java.util.Objects;

/**
 * @ingroup hbbtvclientapi
 * @defgroup hbbtvappidentifierapi HbbTV-AppIdentifier-API
 */
public class HbbTvAppIdentifier {

   /**
    * @ingroup hbbtvappidentifierapi
    * @brief constructor method of HbbTvAppIdentifier
    * @param appId  Identifier of the application.
    * @param orgId  Organization identifier of the application.
    * @param appUrl  URL of the application.
    * @return the instance of HbbTbAppIdentifier
    */
    public HbbTvAppIdentifier(int appId, int orgId, String appUrl) {
        mAppId = appId;
        mOrgId = orgId;
        mAppUrl = appUrl;
    }

    /**
     * @ingroup hbbtvappidentifierapi
     * @brief get the application id
     * @return the id of application.
     */
    public int getAppId() {
        return mAppId;
    }

    /**
     * @ingroup hbbtvappidentifierapi
     * @brief get the organization id
     * @return the id of organization.
     */
    public int getOrgId() {
        return mOrgId;
    }

    /**
     * @ingroup hbbtvappidentifierapi
     * @brief  judge the instances whether equals
     * @return true means the instances euqals,otherwise false
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HbbTvAppIdentifier)) {
            return false;
        }
        HbbTvAppIdentifier appIdentifier = (HbbTvAppIdentifier) o;
        if (appIdentifier.mAppId != mAppId || appIdentifier.mOrgId != mOrgId) {
            return false;
        }
        if (isUrlApp()) {
            return TextUtils.equals(mAppUrl, appIdentifier.mAppUrl);
        }
        return true;
    }

    /**
      * @ingroup hbbtvappidentifierapi
      * @brief  get the instance hash code
      * @return  the instance hash code
      */
    @Override
    public int hashCode() {
        if (isUrlApp()) {
            return Objects.hash(mAppId, mOrgId, mAppUrl);
        } else {
            return Objects.hash(mAppId, mOrgId);
        }
    }

    private boolean isUrlApp() {
        return mAppId == 0 && mOrgId == 0;
    }

    private final int mAppId;
    private final int mOrgId;
    private final String mAppUrl;
}
