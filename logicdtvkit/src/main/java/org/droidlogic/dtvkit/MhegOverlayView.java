package org.droidlogic.dtvkit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

public class MhegOverlayView extends ImageView {
    private final String TAG = "MhegOverlayView";
    Rect displayRect;
    private Handler mHandler;

    public final DtvkitGlueClient.OverlayTarget mTarget =
        (src_width, src_height, dst_x, dst_y, dst_width, dst_height, data) -> {
            Log.v(TAG, "src_w= " + src_width +
                    ", src_h= " + src_height + ", x= " + dst_x + ", y= " + dst_y +
                    ", dst_w= " + dst_width + ", dst_h= " + dst_height);
            mHandler.post(() -> {
                if (src_width == 0 || src_height == 0) {
                    if (dst_width == 9999) {
                        /* 9999 dst_width indicates the overlay should be cleared */
                        setImageBitmap(null);
                    }
                } else {
                    if (data.length == 0) {
                        return;
                    }
                    Bitmap bitmap = getBitmap(src_width, src_height, data);
                    setImageBitmap(bitmap);
                }
                postInvalidate();
            });
        };

    private Bitmap getBitmap(int width, int height, int[] data) {
        Drawable drawable = getDrawable();
        Bitmap bitmap;
        boolean reuse = false;

        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
            if (bitmap != null) {
                if (!bitmap.isRecycled() && bitmap.getWidth() >= width && bitmap.getHeight() >= height) {
                    reuse = true;
                }
            }
            if (!reuse) {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            }
        } else {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        bitmap.setPixels(data, 0, width, 0, 0, width, height);
        return bitmap;
    }

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

    public void clear() {
        setImageBitmap(null);
    }

    public void setSize(int width, int height) {
        displayRect = new Rect(0, 0, width, height);
        postInvalidate();
    }

    public void setSize(int left, int top, int right, int bottom) {
        displayRect = new Rect(left, top, right, bottom);
        postInvalidate();
    }

    public void setOverlayTarget(DtvkitGlueClient.OverlayTarget target) {
        DtvkitGlueClient.getInstance().setOverlayTarget(target);
    }

    public void destroy() {
        DtvkitGlueClient.getInstance().removeOverlayTarget(mTarget);
        clear();
    }
}
