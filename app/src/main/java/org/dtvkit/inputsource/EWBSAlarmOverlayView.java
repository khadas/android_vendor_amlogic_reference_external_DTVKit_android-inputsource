package org.dtvkit.inputsource;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.text.style.UpdateAppearance;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class EWBSAlarmOverlayView extends FrameLayout {
    private final static String TAG = EWBSAlarmOverlayView.class.getSimpleName();

    private int mScreenWidth;
    private int mScreenHeight;
    @SuppressLint("RtlHardcoded")
    public enum EnclosureStyle {
        ENCLOSURE_STYLE_NONE(0, Gravity.NO_GRAVITY),
        ENCLOSURE_STYLE_BOTTOM(1, Gravity.BOTTOM),
        ENCLOSURE_STYLE_RIGHT(1 << 1, Gravity.RIGHT),
        ENCLOSURE_STYLE_TOP(1 << 2, Gravity.TOP),
        ENCLOSURE_STYLE_LEFT(1 << 3, Gravity.LEFT);

        private final int key;
        private final int value;
        private static final Map<Integer, EnclosureStyle> map = new HashMap<>();

        static {
            for (EnclosureStyle style : EnclosureStyle.values()) {
                map.put(style.key, style);
            }
        }
        EnclosureStyle(int key, int value) {
            this.key = key;
            this.value = value;
        }
        public int getValue() {
            return value;
        }
        public static EnclosureStyle getEnclosureStyleByKey(int key) {
            return map.get(key);
        }
    }

    public enum CharStyle {
        CHAR_STYLE_NORMAL(0, Typeface.NORMAL),
        CHAR_STYLE_BOLD(1, Typeface.BOLD),
        CHAR_STYLE_ITALIC(1 << 1, Typeface.ITALIC);

        private final int key;
        private final int value;
        private static final Map<Integer, CharStyle> map = new HashMap<>();

        static {
            for (CharStyle style : CharStyle.values()) {
                map.put(style.key, style);
            }
        }
        CharStyle(int key, int value) {
            this.key = key;
            this.value = value;
        }
        public int getValue() {
            return value;
        }
        public static CharStyle getCharStyleByKey(int key) {
            return map.get(key);
        }
    }

    public EWBSAlarmOverlayView(@NonNull Context context) {
        super(context);
    }

    public void setSize(int width, int height) {
        mScreenWidth = width;
        mScreenHeight = height;
    }
    public void showEWBSJsonMsg(JSONObject data) {
        try {
            // flags=1 clear screen
            if (data.getInt("flags") == 1 && getRootView() != null) {
                removeAllViews();
            }
            // Set screen width and height
            int plane_height = data.getInt("plane_height");
            int plane_width = data.getInt("plane_width");
            int ScreenRatio = mScreenWidth / plane_width; // The ratio of the current screen to the display range
            LayoutParams layoutParams = new LayoutParams(plane_width * ScreenRatio, plane_height * ScreenRatio);
            layoutParams.gravity = Gravity.CENTER;
            setLayoutParams(layoutParams);

            int region_count = data.getInt("region_count");
            JSONArray regions = data.getJSONArray("regions");
            for (int i = 0; i < region_count; i++) {
                JSONObject regionObject = regions.getJSONObject(i);
                int char_count = regionObject.getInt("char_count");
                JSONArray charInfoArray = regionObject.getJSONArray("char_infos");
                for (int j = 0; j < char_count; j++) {
                    JSONObject charInfo = charInfoArray.getJSONObject(j);
                    String textContent = charInfo.getString("char");
                    int char_height = charInfo.getInt("char_height");
                    int char_width = charInfo.getInt("char_width");
                    int char_horizontal_spacing = charInfo.getInt("char_horizontal_spacing");
                    int char_vertical_spacing = charInfo.getInt("char_vertical_spacing");
                    int enclosureStyle = charInfo.getInt("enclosure_style");
                    int textStyle = charInfo.getInt("style");
                    int strokeColor = charInfo.getInt("stroke_color");
                    int textColor = charInfo.getInt("text_color");
                    int backgroundColor = charInfo.getInt("back_color");
                    int horizontalCoordinates = charInfo.getInt("x");
                    int verticalCoordinates = charInfo.getInt("y");
                    FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    textParams.setMargins(horizontalCoordinates * ScreenRatio, verticalCoordinates * ScreenRatio, 0, 0);
                    TextView textView = new TextView(getContext());
                    textView.setLayoutParams(textParams);
                    SpannableString spannableString = new SpannableString(textContent);
                    if (CharStyle.getCharStyleByKey(textStyle) != null && !TextUtils.isEmpty(textContent)) {
                        StyleSpan styleSpan = new StyleSpan(CharStyle.getCharStyleByKey(textStyle).getValue());
                        spannableString.setSpan(styleSpan, 0, spannableString.length() - 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    } else if (!TextUtils.isEmpty(textContent)){
                        if (textStyle == 4) { // CHAR_STYLE_UNDERLINE
                            UnderlineSpan underlineSpan = new UnderlineSpan();
                            spannableString.setSpan(underlineSpan, 0, spannableString.length() - 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                        } else if (textStyle == 8) { // CHAR_STYLE_STROKE
                            //The StrokeWidth parameter is not specified, here it is temporarily set to 2px
                            StrokeSpan strokeSpan = new StrokeSpan(2, strokeColor);
                            spannableString.setSpan(strokeSpan, 0, spannableString.length() - 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                        }
                    }
                    textView.setHeight((char_height + char_vertical_spacing) * ScreenRatio);
                    textView.setWidth((char_width + char_horizontal_spacing) * ScreenRatio);
                    textView.setTextSize(char_width * ScreenRatio * 0.4f);
                    textView.setText(spannableString);
                    textView.setBackgroundColor(getABGRbyARGB(backgroundColor));
                    textView.setGravity(EnclosureStyle.getEnclosureStyleByKey(enclosureStyle).getValue());
                    textView.setTextColor(getABGRbyARGB(textColor));
                    addView(textView);
                }
            }
        } catch (JSONException e) {
            Log.i(TAG,"showEWBSJsonMsg error ");
            e.printStackTrace();
        }
    }

    public void hideEWBSAlarmView() {
        removeAllViews();
    }

    private int getABGRbyARGB(int argbColor ) {
        int alpha = (argbColor >> 24) & 0xFF;
        int red = (argbColor >> 16) & 0xFF;
        int green = (argbColor >> 8) & 0xFF;
        int blue = argbColor & 0xFF;
        return  (alpha << 24) | (blue << 16) | (green << 8) | red;
    }

    private static class StrokeSpan extends CharacterStyle implements UpdateAppearance {
        private final float strokeWidth;
        private final int strokeColor;

        public StrokeSpan(float strokeWidth, int strokeColor) {
            this.strokeWidth = strokeWidth;
            this.strokeColor = strokeColor;
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            ds.setStyle(Paint.Style.STROKE);
            ds.setStrokeWidth(strokeWidth);
            ds.setColor(strokeColor);
        }
    }
}
