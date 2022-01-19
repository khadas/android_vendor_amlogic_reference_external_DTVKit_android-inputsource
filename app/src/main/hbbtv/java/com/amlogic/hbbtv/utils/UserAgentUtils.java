package com.amlogic.hbbtv.utils;


import android.os.Build;

import com.vewd.core.sdk.Browser;
import com.vewd.core.shared.Device;
import com.vewd.core.shared.DeviceTypeUtils;

/**
 * @ingroup hbbtvutilsapi
 * @defgroup useragentutils User-Agent-Utils-API
 */
public class UserAgentUtils {

    /**
    * @ingroup useragentutils.
    * @brief Get the vendor user agent suffix
    * @return String  the vendor user agent suffix
    */
    public static String getVendorUserAgentSuffix() {
        switch (DeviceTypeUtils.detectDevice()) {
            case Device.T22: {
                final String chromiumVersion =
                    Browser.getInstance().getBrowserInfo().getChromiumVersion();
                return String.format("smarttv_%s_Build_%s_Chromium_%s",
                    Build.MODEL, Build.VERSION.INCREMENTAL, chromiumVersion);
            }
            default:
                return "";
        }
    }

    private UserAgentUtils() {
    }
}
