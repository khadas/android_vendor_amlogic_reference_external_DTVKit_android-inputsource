package com.amlogic.hbbtv.utils;


/**
 * @ingroup hbbtvutilsapi
 * @defgroup stringutilsapi String-Utils-API
 */
public class StringUtils {
    private static final int DATA_URL_LENGTH_LIMIT = 100;

     /**
     * @ingroup stringutilsapi.
     * @brief Truncate the url for logging.
     * @param url  The url of application.
     * @return String  the truncated url.
     */
    public static String truncateUrlForLogging(String url) {
        if (url != null) {
            if (url.length() > DATA_URL_LENGTH_LIMIT && url.startsWith("data:")) {
                return url.substring(0, DATA_URL_LENGTH_LIMIT) + "... (data URL, " + url.length()
                    + " bytes)";
            }
            return url;
        } else {
            return null;
        }

    }

}
