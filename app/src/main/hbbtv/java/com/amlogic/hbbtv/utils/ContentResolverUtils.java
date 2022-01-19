package com.amlogic.hbbtv.utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @ingroup hbbtvutilsapi
 * @defgroup contentresolverutilsapi Content-Resolver-Utils-API
 */
public class ContentResolverUtils {
    public static final String CHANNELS_ORDER_BY = "CAST("
            + TvContract.Channels.COLUMN_DISPLAY_NUMBER + " AS INTEGER)";
    /**
    * @ingroup contentresolverutilsapi.
    * @brief Transfer the cursor row to json object
    * @param cursor  The Cursor instance which need to be transfered to json object
    * @return JSONObject
    */
    public static JSONObject cursorRowToJsonObject(Cursor cursor)throws JSONException {
        JSONObject jsonObject = new JSONObject();
        for (int i = 0; i < cursor.getColumnCount(); ++i) {
            String columnName = cursor.getColumnName(i);
            if (columnName.equals(TvContract.Channels.COLUMN_INTERNAL_PROVIDER_FLAG1)) {
                // COLUMN_INTERNAL_PROVIDER_FLAG1 stores infos about user-hidden and user-favourite
                // channel. Its value shouldn't be exposed as raw bit field to JS-layer. Instead of
                // that, top level fields, user_hidden and user_favourite, are used in
                // internal_provider_data to share with JS
                jsonObject.put(columnName, JSONObject.NULL);
                continue;
            }
            if (columnName != null) {
                switch (cursor.getType(i)) {
                    case Cursor.FIELD_TYPE_BLOB:
                        jsonObject.put(columnName,
                                Base64.encodeToString(
                                        cursor.getBlob(i), Base64.DEFAULT));
                        break;
                    case Cursor.FIELD_TYPE_FLOAT:
                        jsonObject.put(columnName, cursor.getDouble(i));
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        jsonObject.put(columnName, cursor.getLong(i));
                        break;
                    case Cursor.FIELD_TYPE_NULL:
                        jsonObject.put(columnName, JSONObject.NULL);
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        jsonObject.put(columnName, cursor.getString(i));
                        break;
                    default:
                        throw new RuntimeException(
                                "Unsupported field type: " + cursor.getType(i));
                }
            }
        }
        return jsonObject;
    }

    /**
    * @ingroup contentresolverutilsapi.
    * @brief Transfer the cursor row to ContentValues
    * @param cursor  The Cursor instance which need to be transfered to ContentValues
    * @return ContentValues
    */
    public static ContentValues cursorRowToContentValues(Cursor cursor) {
        // It is our own version of DatabaseUtils.cursorRowToContentValues. The
        // original one treats all values as String.
        ContentValues contentValues = new ContentValues();
        for (int i = 0; i < cursor.getColumnCount(); ++i) {
            String columnName = cursor.getColumnName(i);
            if (columnName != null) {
                switch (cursor.getType(i)) {
                    case Cursor.FIELD_TYPE_BLOB:
                        contentValues.put(columnName,
                                Base64.encodeToString(
                                        cursor.getBlob(i), Base64.DEFAULT));
                        break;
                    case Cursor.FIELD_TYPE_FLOAT:
                        contentValues.put(columnName, cursor.getDouble(i));
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        contentValues.put(columnName, cursor.getLong(i));
                        break;
                    case Cursor.FIELD_TYPE_NULL:
                        contentValues.putNull(columnName);
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        contentValues.put(columnName, cursor.getString(i));
                        break;
                    default:
                        throw new RuntimeException(
                                "Unsupported field type: " + cursor.getType(i));
                }
            }
        }
        return contentValues;
    }

    private ContentResolverUtils() {
    }
}
