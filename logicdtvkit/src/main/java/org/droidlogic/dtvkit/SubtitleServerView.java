package org.droidlogic.dtvkit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.GregorianCalendar;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class SubtitleServerView extends FrameLayout {
    private final String TAG = "SubtitleServerView";
    Rect displayRect;
    private final Handler mHandler;
    private final ImageView imageView;
    private final TextView textView;
    int mPauseExDraw = 0;
    boolean mTtxTransparent = false;

    static final int SUBTITLE_SUB_TYPE_TTX = 9;
    static final int SUBTITLE_SUB_TYPE_ARIB = 16;
    static final int MSG_SUBTITLE_SHOW_CLOSED_CAPTION = 5;
    protected static final int MSG_SET_TELETEXT_MIX_NORMAL = 6;
    protected static final int MSG_SET_TELETEXT_MIX_TRANSPARENT = 7;
    protected static final int MSG_SET_TELETEXT_MIX_SEPARATE = 8;

    private final AriBRunnable mAriBRunnable = new AriBRunnable();
    private final NormalRunnable mNormalRunnable = new NormalRunnable();
    private final Runnable mClearRunnable = new Runnable() {
        @Override
        public void run() {
            setBackgroundColor(Color.TRANSPARENT);
            imageView.setImageBitmap(null);
            textView.setText(null);
        }
    };

    private class AriBRunnable implements Runnable {
        int[] data;
        public void setData(int[] data) {
            this.data = data;
        }
        @Override
        public void run() {
            String text = intArrayToString(data);
            textView.setText(text);
            data = null;
            invalidate();
        }
        private String intArrayToString(int[] data) {
            if (data == null) {
                return "";
            }
            byte [] bytes = new byte [data.length];
            int numberOfValid = data.length / 4;
            int modOfChar = data.length % 4;
            for (int i = 0; i <= numberOfValid; i++) {
                if (i < numberOfValid) {
                    bytes[i*4] = (byte) (data[i] & 0xFF);
                    bytes[i*4 + 1] = (byte) ((data[i] >> 8) & 0xFF);
                    bytes[i*4 + 2] = (byte) ((data[i] >> 16) & 0xFF);
                    bytes[i*4 + 3] = (byte) ((data[i] >> 24) & 0xFF);
                } else {
                    if (modOfChar > 0) {
                        bytes[i*4] = (byte) (data[i] & 0xFF);
                        modOfChar--;
                    }
                    if (modOfChar > 0) {
                        bytes[i*4 + 1] = (byte) ((data[i] >> 8) & 0xFF);
                        modOfChar--;
                    }
                    if (modOfChar > 0) {
                        bytes[i*4 + 2] = (byte) ((data[i] >> 16) & 0xFF);
                        modOfChar--;
                    }
                }
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private class NormalRunnable implements Runnable {
        int xStart;
        int yStart;
        int srcWidth;
        int srcHeight;
        int dstWidth;
        int dstHeight;
        int parserType;
        boolean teletextStarted;
        Bitmap region;

        public void setRegion(Bitmap region) {
            this.region = region;
        }

        public void setData(int parserType, int src_width, int src_height,
                            int dst_x, int dst_y, int dst_width, int dst_height,
                            boolean teletextStarted) {
            this.parserType = parserType;
            this.srcWidth = src_width;
            this.srcHeight = src_height;
            this.xStart = dst_x;
            this.yStart = dst_y;
            this.dstWidth = dst_width;
            this.dstHeight = dst_height;
            this.teletextStarted = teletextStarted;
        }
        @Override
        public void run() {
            if (srcWidth == 0 || srcHeight == 0) {
                return;
            }
            boolean ttxPageTmpTrans = (xStart == 1); // ttx use dst_x as transparent flag
            if (parserType == SUBTITLE_SUB_TYPE_TTX) {
                xStart = 120;//ttx fixed with 480x525 shown in 720x480
                yStart = 0;
                srcWidth = 480;
                srcHeight = 525;
                dstWidth = 720;
                dstHeight = 480;
            }
            Rect overlay_dst = new Rect(0, 0, srcWidth, srcHeight);
            if ((dstWidth == 0)
                    || (dstHeight == 0)
                    || (dstWidth < (srcWidth + xStart))
                    || (dstHeight < (srcHeight + yStart))) {
                overlay_dst.left = xStart;
                overlay_dst.top = yStart;
                overlay_dst.right = displayRect.width() - overlay_dst.left;
                overlay_dst.bottom = displayRect.height() - overlay_dst.top;
            } else {
                float scaleX = (float) (displayRect.width()) / (float) (dstWidth);
                float scaleY = (float) (displayRect.height()) / (float) (dstHeight);
                overlay_dst.left = (int) (scaleX * xStart);
                overlay_dst.right = (int) (scaleX * (srcWidth + xStart));
                overlay_dst.top = (int) (scaleY * yStart);
                overlay_dst.bottom = (int) (scaleY * (srcHeight + yStart));
            }
            if (teletextStarted && !mTtxTransparent && !ttxPageTmpTrans) {
                setBackgroundColor(Color.BLACK);
            } else {
                setBackgroundColor(Color.TRANSPARENT);
            }
            if (overlay_dst.right > displayRect.right
                    || overlay_dst.bottom > displayRect.bottom) {
                Log.w(TAG, "Please check, overlay_dst " + overlay_dst);
            }
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(overlay_dst.width(), overlay_dst.height());
            imageView.setLayoutParams(lp);
            if (parserType == SUBTITLE_SUB_TYPE_TTX) {
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            } else {
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            }
            imageView.setX(overlay_dst.left);
            imageView.setY(overlay_dst.top);
            imageView.setImageBitmap(region);
            invalidate();
        }
    }

    public final DtvkitGlueClient.SubtitleListener mSubListener = new DtvkitGlueClient.SubtitleListener() {
        @Override
        public void drawEx(int parserType, int src_width, int src_height,
                           int dst_x, int dst_y, int dst_width, int dst_height, int[] data) {
            Log.v(TAG, "type= " + parserType + ", src_w= " + src_width +
                    ", src_h= " + src_height + ", x= " + dst_x + ", y= " + dst_y +
                    ", dst_w= " + dst_width + ", dst_h= " + dst_height + ", pause= " + mPauseExDraw +
                    ", " + mTtxTransparent);
            if (mPauseExDraw > 0) {
                return;
            }
            if (parserType == SUBTITLE_SUB_TYPE_ARIB) {
                mHandler.removeCallbacks(mAriBRunnable);
                mHandler.post(() -> mAriBRunnable.setData(data));
                mHandler.post(mAriBRunnable);
            } else if (src_width == 0 || src_height == 0) {
                if (dst_width == 9999) {
                    /* 9999 dst_width indicates the overlay should be cleared */
                    mHandler.removeCallbacks(mClearRunnable);
                    mHandler.post(mClearRunnable);
                }
            } else {
                Bitmap bitmap = getBitmap(src_width, src_height, data);
                if (SystemProperties.getBoolean("vendor.dtv.subtitle_save", false)) {
                    saveBitmap(bitmap, getContext());
                }
                final boolean teletextStarted = playerIsTeletextStarted();
                mHandler.removeCallbacks(mNormalRunnable);
                mHandler.post(() -> {
                    mNormalRunnable.setData(parserType, src_width, src_height,
                            dst_x, dst_y, dst_width, dst_height, teletextStarted);
                    mNormalRunnable.setRegion(bitmap);
                });
                mHandler.post(mNormalRunnable);
            }
        }

        private Bitmap getBitmap(int width, int height, int[] data) {
            Drawable drawable = null;
            Bitmap bitmap;
            boolean reuse = false;
            if (imageView != null) {
                drawable = imageView.getDrawable();
            }
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
            msg.arg1 = !bShow ? 0 : 1;
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
        imageView = new ImageView(context);
        addView(imageView);
        textView = new TextView(context);
        addView(textView);
        DtvkitGlueClient.getInstance().setSubtileListener(mSubListener);
    }

    private void initTextSubtitle() {
        float xStart = (float) displayRect.width() / 8;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(displayRect.width() * 3/4, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM;
        lp.bottomMargin = 60;
        textView.setLayoutParams(lp);
        textView.setGravity(Gravity.CENTER);
        textView.setTextColor(Color.YELLOW);
        textView.setX(xStart);
        textView.setTextSize(36f);
    }

    public void showTestSubtitle() {
        Log.i(TAG, "showTestSubtitle");
        mHandler.removeCallbacks(mNormalRunnable);
        Bitmap bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.RGB_565);
        bitmap.eraseColor(Color.GREEN);
        mHandler.post(() -> {
            mNormalRunnable.setData(5, 160, 120,
                    0, 0, 1920, 1080, false);
            mNormalRunnable.setRegion(bitmap);
        });
        mHandler.post(mNormalRunnable);
    }

    public void setSize(int width, int height) {
        displayRect = new Rect(0, 0, width, height);
        initTextSubtitle();
        postInvalidate();
    }

    public void setSize(int left, int top, int right, int bottom) {
        displayRect = new Rect(left, top, right, bottom);
        initTextSubtitle();
        postInvalidate();
    }

    public void setTeletextTransparent(boolean mode) {
        mTtxTransparent = mode;
    }

    public void clearSubtitle() {
        mNormalRunnable.setRegion(null);
        imageView.setImageBitmap(null);
        mHandler.removeCallbacks(mAriBRunnable);
        mHandler.removeCallbacks(mNormalRunnable);
        mHandler.post(mClearRunnable);
    }

    public void setOverlaySubtitleListener(DtvkitGlueClient.SubtitleListener listener) {
        DtvkitGlueClient.getInstance().setSubtileListener(listener);
    }

    public void destroy() {
        DtvkitGlueClient.getInstance().removeSubtileListener(mSubListener);
        mNormalRunnable.setRegion(null);
        imageView.setImageBitmap(null);
        mHandler.removeCallbacks(mAriBRunnable);
        mHandler.removeCallbacks(mNormalRunnable);
        mHandler.removeCallbacks(mClearRunnable);
    }

    private void saveBitmap(Bitmap bitmap, Context ct) {
        Calendar now = new GregorianCalendar();
        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String savePath = ct.getExternalFilesDir(null) + simpleDate.format(now.getTime()) + ".jpg";
        File filePic = new File(savePath);
        try {
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            Log.i(TAG, "debug: dump subtitle: " + savePath);
            FileOutputStream fos = new FileOutputStream(filePic);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Failed to saveBitmap: " + e.getMessage());
        }
    }

    @Override
    public String toString() {
        return super.toString() + "\n" +
                "TAG='" + TAG + '\'' +
                ", displayRect=" + displayRect +
                ", imageView=" + imageView +
                ", textView=" + textView +
                '}';
    }
}
