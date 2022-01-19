package com.amlogic.hbbtv.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @ingroup hbbtvutilsapi
 * @defgroup intentutilsapi Intent-Utils-API
 */
public class IntentUtils {
    private static final String TAG = "IntentUtils";

    private static String getStringFromIntentQuietly(Intent intent, String key) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return null;
        }
        Object extra = extras.get(key);
        if (extra instanceof String) {
            return (String)extra;
        }
        return null;
    }

    /**
    * @ingroup intentutilsapi.
    * @brief Get Boolean instance from intent
    * @param intent  The intent which contain the data
    * @param key  The key of data
    * @param defaultValue  The default value of data
    * @return String  return if has the key value otherwise the default value
    */
    public static boolean getBooleanFromIntent(Intent intent, String key,
            boolean defaultValue) {
        if (intent == null) {
            return defaultValue;
        }
        String value = getStringFromIntentQuietly(intent, key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        } else {
            return intent.getBooleanExtra(key, defaultValue);
        }
    }

    /**
    * @ingroup intentutilsapi.
    * @brief Get int  from intent
    * @param intent  The intent which contain the data
    * @param key  The key of data
    * @param defaultValue  The default value of data
    * @return int  return if has the key value otherwise the default value
    */

    public static int getIntFromIntent(Intent intent, String key,
            int defaultValue) {
        if (intent == null) {
            return defaultValue;
        }
        String value = getStringFromIntentQuietly(intent, key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Invalid value for '" + key + "': " + value);
            }
        } else {
            return intent.getIntExtra(key, defaultValue);
        }
    }

    /**
    * @ingroup intentutilsapi.
    * @brief Get String instance from intent
    * @param intent  The intent which contain the data
    * @param key  The key of data
    * @param defaultValue  The default value of data
    * @return String  return if has the key value otherwise the default value
    */
    public static String getStringFromIntent(Intent intent, String key,
            String defaultValue) {
        if (intent == null) {
            return defaultValue;
        }
        String value = intent.getStringExtra(key);
        if (value != null) {
            return value;
        }

        return defaultValue;
    }

    /**
    * @ingroup intentutilsapi.
    * @brief Get String array from intent
    * @param intent  The intent which contain the data
    * @param key  The key of data
    * @param defaultValue  The default value of data
    * @return String[]  if has the key value otherwise return the default value
    */
    public static String[] getStringArrayFromIntent(Intent intent, String key,
            String[] defaultValue) {
        if (intent == null) {
            return defaultValue;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return defaultValue;
        }
        Object extra = extras.get(key);
        if (extra == null) {
            return defaultValue;
        }
        // am start --esa key val1,val2,...
        // intent.getStringArrayExtra(key) spams loudl on a type mismatch :(
        if (extra instanceof String[]) {
            return (String[])extra;
        }
        // am start --esal key val1,val2,...
        if (extra instanceof ArrayList) {
            ArrayList<String> arrayList = intent.getStringArrayListExtra(key);
            if (arrayList != null) {
                return arrayList.toArray(new String[0]);
            }
        }
        // am start -e key "val1 val2"
        String value = intent.getStringExtra(key);
        if (value != null) {
            // Split on the space character, but only if that space has zero or
            // an even number of double quotes in ahead of it.
            // Reference:
            // http://stackoverflow.com/questions/1757065/java-splitting-a-comma-separated-string-but-ignoring-commas-in-quotes
            // Note: this makes it impossible to pass an argument that will
            // contain an actual double quote character, as it will be split,
            // but things like --foo="bar baz" will work as expected.
            String regex = " (?=([^\"]*\"[^\"]*\")*[^\"]*$)";
            String[] tokens = value.split(regex);

            // strip double quotes
            for (int i = 0; i < tokens.length; i++) {
                tokens[i] = tokens[i].replaceAll("\"", "");
            }

            return tokens;
        }

        return defaultValue;
    }

    /**
    * @ingroup intentutilsapi.
    * @brief Get String map from intent
    * @param intent  The intent which contain the data
    * @param key  The key of data
    * @param defaultValue  The default value of data
    * @return Map  if has the key value otherwise return the default value
    */
    public static Map<String, String> getStringMapFromIntent(Intent intent,
            String key, Map<String, String> defaultValue) {
        if (intent == null) {
            return defaultValue;
        }

        String[] stringArray = getStringArrayFromIntent(intent, key, null);
        if (stringArray != null) {
            ArrayList<String> arrayListValue =
                    intent.getStringArrayListExtra(key);
            if (arrayListValue != null) {
                stringArray = arrayListValue.toArray(new String[0]);
            }
        }
        if (stringArray != null) {
            Map<String, String> stringMap = new HashMap<>();

            for (String parameter : stringArray) {
                String[] tokens = parameter.split("=");

                if (tokens.length != 2) {
                    throw new RuntimeException(
                            "Invalid parameter: " + parameter);
                }

                stringMap.put(tokens[0], tokens[1]);
            }

            return stringMap;

        }

        return defaultValue;
    }

    /**
    * @ingroup intentutilsapi.
    * @brief transfer the intent to bundle
    * @param intent  The intent which contain the data
    * @return Bundle
    */
    public static Bundle intentToBundle(Intent intent) {
        Bundle bundle = new Bundle();
        if (intent.getDataString() != null) {
            bundle.putString("data", intent.getDataString());
        }
        if (intent.getExtras() != null) {
            bundle.putBundle("extras", intent.getExtras());
        }
        return bundle;
    }

   /* public static void startActivity(Context context, String action,
            String packageName, String className, Uri data,
            Map<String, String> extras) {
        Log.i(TAG,
                "Activity start request: action=" + action + "package=" + packageName
                        + " class=" + className + " data=" + data + " extras=" + extras);
        Intent intent = new Intent(action);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (!packageName.isEmpty() && !className.isEmpty()) {
            intent.setClassName(packageName, className);
        }
        if (data != null) {
            intent.setData(data);
        }
        if (extras != null) {
            for (Map.Entry<String, String> entry : extras.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }
        try {
            context.startActivity(intent);
        } catch (SecurityException e) {
            Log.e(TAG, "Activity start error: " + e);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG,
                    "Activity not found: action=" + action + ", package=" + packageName
                            + ", class=" + className);
        }
    }*/


    /**
    * @ingroup intentutilsapi.
    * @brief Get the response intent
    * @param originalIntent  The original intent
    * @param responseAction  The response action
    * @param result  The default value of data
    * @param extraResult  The extral bundle result data
    * @return Intent  The intent incluede action and data
    */
    public static Intent getResponseIntent(Intent originalIntent,
            String responseAction, boolean result, Bundle extraResult) {
        Log.i(TAG,
                "Creating response for " + originalIntent.getAction()
                        + " result=" + result);
        Intent responseIntent = new Intent(responseAction);
        responseIntent.putExtra("result", result);
        responseIntent.putExtra("originalAction", originalIntent.getAction());
        if (extraResult != null) responseIntent.putExtras(extraResult);
        return responseIntent;
    }

    private IntentUtils() {
    }
}
