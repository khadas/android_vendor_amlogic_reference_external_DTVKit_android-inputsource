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


public class MhegOverlayView extends View {
    private final String TAG = "MhegOverlayView";
    Bitmap overlay1 = null;
    Rect src, dst;
    private Handler mHandler = null;

    public final DtvkitGlueClient.OverlayTarget mTarget =
        (src_width, src_height, dst_x, dst_y, dst_width, dst_height, data) -> {
            Log.v(TAG, "MhegOverlayView: " + "srcw= " + src_width +
                    ", srch= " + src_height + ", x= " + dst_x + ", y= " + dst_y +
                    ", dst_w= " + dst_width + ", dst_h= " + dst_height);
            mHandler.post(() -> {
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
                    canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    Paint paint = new Paint();
                    paint.setAntiAlias(true);
                    paint.setFilterBitmap(true);
                    canvas.drawBitmap(region, null, overlay_dst, paint);
                    region.recycle();
                    postInvalidate();
                }
            });
        };


    public MhegOverlayView(Context context) {
        this(context, new Handler(Looper.getMainLooper()));
    }

    public MhegOverlayView(Context context, Handler mainHandler) {
        super(context);
        if (mainHandler == null) {
            throw new NullPointerException("handler cannot be null");
        }
        if (mainHandler.getLooper() != Looper.getMainLooper()) {
            throw new SecurityException("Must be MainLooper handler");
        }
        mHandler = mainHandler;
        DtvkitGlueClient.getInstance().setOverlayTarget(mTarget);
    }

    public void setSize(int width, int height) {
        dst = new Rect(0, 0, width, height);
        postInvalidate();
    }

    public void setSize(int left, int top, int right, int bottom) {
        dst = new Rect(left, top, right, bottom);
        postInvalidate();
    }

    public void setOverlayTarget(DtvkitGlueClient.OverlayTarget target) {
        DtvkitGlueClient.getInstance().setOverlayTarget(target);
    }

    public void destroy() {
        DtvkitGlueClient.getInstance().removeOverlayTarget(mTarget);
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
