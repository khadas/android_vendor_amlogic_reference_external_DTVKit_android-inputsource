package org.droidlogic.dtvkit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;

import org.json.JSONArray;

public class SubtitleServerView extends View {
    private final String TAG = "SubtitleServerView";
    Bitmap overlay1 = null;
    Rect src, dst;
    private Handler mHandler = null;

    int mPauseExDraw = 0;
    boolean mTtxTransparent = false;

    static final int SUBTITLE_SUB_TYPE_TTX = 9;
    static final int MSG_SUBTITLE_SHOW_CLOSED_CAPTION = 5;
    protected static final int MSG_SET_TELETEXT_MIX_NORMAL = 6;
    protected static final int MSG_SET_TELETEXT_MIX_TRANSPARENT = 7;
    protected static final int MSG_SET_TELETEXT_MIX_SEPARATE = 8;

    public final DtvkitGlueClient.SubtitleListener mSubListener = new DtvkitGlueClient.SubtitleListener() {
        @Override
        public void drawEx(int parserType, int src_width, int src_height, int dst_x, int dst_y, int dst_width, int dst_height, int[] data) {
            Log.v(TAG, "SubtitleServiceDraw: type= " + parserType + ", srcw= " + src_width +
                    ", srch= " + src_height + ", x= " + dst_x + ", y= " + dst_y +
                    ", dst_w= " + dst_width + ", dst_h= " + dst_height + ", pause= " + mPauseExDraw);
            mHandler.post(() -> {
                if (mPauseExDraw > 0) {
                    return;
                }
                /* TODO Temporary private usage of API. Add explicit methods if keeping this mechanism */
                if (src_width == 0 || src_height == 0) {
                    if (dst_width == 9999 && overlay1 != null) {
                        /* 9999 dst_width indicates the overlay should be cleared */
                        Canvas canvas = new Canvas(overlay1);
                        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                        postInvalidate();
                    }
                } else {
                    if (data.length == 0)
                        return;
                    /* Build an array of ARGB_8888 pixels as signed ints and add this part to the region */
                    if (overlay1 == null) {
                        /* TODO The overlay size should come from the tif (and be updated on onOverlayViewSizeChanged) */
                        overlay1 = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);
                        /* Clear the overlay that will be drawn initially */
                        //Canvas canvas = new Canvas(overlay1);
                        //canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    }
                    Canvas canvas = new Canvas(overlay1);
                    boolean ttxPageTmpTrans = (dst_x == 1);//ttx not use x,y as coords
                    Rect overlay_dst = new Rect(0, 0, overlay1.getWidth(), overlay1.getHeight());
                    Bitmap region = Bitmap.createBitmap(data, 0, src_width, src_width, src_height, Bitmap.Config.ARGB_8888);
                    int x = dst_x;
                    int y = dst_y;
                    int w = src_width;
                    int h = src_height;
                    int dw = dst_width;
                    int dh = dst_height;
                    if (parserType == SUBTITLE_SUB_TYPE_TTX) {
                        x = 120;//ttx fixed with 480x525 shown in 720x480
                        y = 0;
                        w = 480;
                        h = 525;
                        dw = 720;
                        dh = 480;
                    }

                    if ((dw == 0)
                            || (dh == 0)
                            || (dw < (w + x))
                            || (dh < (h + y))) {
                        overlay_dst.left = x;
                        overlay_dst.top = y;
                        overlay_dst.right = overlay1.getWidth() - overlay_dst.left;
                        overlay_dst.bottom = overlay1.getHeight() - overlay_dst.top;
                    } else {
                        if (x > 0) {
                            float scaleX = (float) (overlay1.getWidth()) / (float) (dw);
                            overlay_dst.left = (int) (scaleX * x);
                            overlay_dst.right = (int) (scaleX * (w + x));
                        }
                        if (y > 0) {
                            float scaleY = (float) (overlay1.getHeight()) / (float) (dh);
                            overlay_dst.top = (int) (scaleY * y);
                            overlay_dst.bottom = (int) (scaleY * (h + y));
                        }
                    }
                    if (playerIsTeletextStarted() && !mTtxTransparent && !ttxPageTmpTrans) {
                        canvas.drawColor(0xFF000000);
                    } else {
                        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    }
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setFilterBitmap(true);
                    canvas.drawBitmap(region, null, overlay_dst, paint);
                    region.recycle();
                    postInvalidate();
                }
            });
        }

        private boolean playerIsTeletextStarted() {
            boolean on = false;
            try {
                JSONArray args = new JSONArray();
                args.put(0);
                on = DtvkitGlueClient.getInstance().request("Player.isTeletextStarted", args).getBoolean("data");
            } catch (Exception e) {
                Log.e(TAG, "playerIsTeletextStarted = " + e.getMessage());
            }
            return on;
        }

        @Override
        public void pauseEx(int pause) {
            mPauseExDraw = pause;
        }

        @Override
        public void drawCC(boolean bShow, String json) {
            Message msg = mHandler.obtainMessage();
            msg.what = MSG_SUBTITLE_SHOW_CLOSED_CAPTION;
            msg.obj = json;
            msg.arg1 = bShow == false ? 0 : 1;
            mHandler.sendMessage(msg);
        }

        private int eventToMsg(int event) {
            switch (event) {
                case 0:
                    return MSG_SET_TELETEXT_MIX_NORMAL;
                case 1:
                    return MSG_SET_TELETEXT_MIX_TRANSPARENT;
                case 2:
                    return MSG_SET_TELETEXT_MIX_SEPARATE;
            }
            return MSG_SET_TELETEXT_MIX_NORMAL;
        }

        @Override
        public void mixVideoEvent(int event) {
            Message msg = mHandler.obtainMessage();
            msg.what = eventToMsg(event);
            mHandler.sendMessage(msg);
        }
    };

    public SubtitleServerView(Context context) {
        this(context, new Handler(Looper.getMainLooper()));
    }

    public SubtitleServerView(Context context, Handler mainHandler) {
        super(context);
        if (mainHandler == null) {
            throw new NullPointerException("handler cannot be null");
        }
        if (mainHandler.getLooper() != Looper.getMainLooper()) {
            throw new SecurityException("Must be MainLooper handler");
        }
        mHandler = mainHandler;
        DtvkitGlueClient.getInstance().setSubtileListener(mSubListener);
    }

    public void setSize(int width, int height) {
        dst = new Rect(0, 0, width, height);
        postInvalidate();
    }

    public void setSize(int left, int top, int right, int bottom) {
        dst = new Rect(left, top, right, bottom);
        postInvalidate();
    }

    public void setTeletextTransparent(boolean mode) {
        mTtxTransparent = mode;
    }

    public void clearSubtitle() {
        mHandler.post(() -> {
            if (overlay1 != null) {
                Canvas canvas = new Canvas(overlay1);
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            }
        });
    }

    public void setOverlaySubtitleListener(DtvkitGlueClient.SubtitleListener listener) {
        DtvkitGlueClient.getInstance().setSubtileListener(listener);
    }

    public void destroy() {
        DtvkitGlueClient.getInstance().removeSubtileListener(mSubListener);
        mHandler.removeCallbacksAndMessages(null);
        mHandler.post(() -> {
            if (overlay1 != null) {
                overlay1.recycle();
                overlay1 = null;
            }
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // onDraw is called in mainthread by android framework.
        super.onDraw(canvas);
        if (overlay1 != null) {
            canvas.drawBitmap(overlay1, src, dst, null);
        }
    }
}
