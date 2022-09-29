package com.droidlogic.dtvkit.inputsource;

import android.content.Context;
import android.widget.FrameLayout;
import android.view.View;
import android.view.KeyEvent;
import com.amlogic.hbbtv.HbbTvManager;

public class HbbTvOverlayView extends FrameLayout {
    HbbTvManager mHbbTvManager = null;
    //View mHbbTvView = null;

    public HbbTvOverlayView(Context context) {
        super(context);
    }

    public void addHbbTvView (View hbbTvView, HbbTvManager hbbTvManager) {
        addView(hbbTvView);
        mHbbTvManager = hbbTvManager;
    }

    public boolean handleKeyDown(int keyCode, KeyEvent event) {
        if (null != mHbbTvManager) {
            return mHbbTvManager.handleKeyDown(keyCode, event);
        }
        return false;
    }

    public boolean handleKeyUp(int keyCode, KeyEvent event) {
        if (null != mHbbTvManager) {
            return mHbbTvManager.handleKeyUp(keyCode, event);
        }
        return false;
    }
}
