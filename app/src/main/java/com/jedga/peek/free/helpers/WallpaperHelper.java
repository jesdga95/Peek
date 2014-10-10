package com.jedga.peek.free.helpers;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class WallpaperHelper {

    private static final float RADIUS = 16f;
    private static final int OPACITY = 130;

    private WallpaperManager mWallpaperManager;
    private ImageBlurrer mBlurrer;
    private Size mDisplaySize;

    private Context mContext;

    public WallpaperHelper(Context context) {
        mContext = context;

        mWallpaperManager = WallpaperManager.getInstance(context);

        mDisplaySize = getDisplaySize(context);
    }

    public Bitmap processBackground() {
        mBlurrer = new ImageBlurrer(mContext);
        WallpaperInfo info = mWallpaperManager.getWallpaperInfo();
        if (info != null) {
            return null;
        }
        if (mWallpaperManager.getDrawable() == null) return null;
        Drawable wallpaperDrawable = mWallpaperManager.getDrawable();
        Bitmap wallpaper;
        try {
            wallpaper = getScaledBitmap(drawableToBitmap(wallpaperDrawable),
                    mDisplaySize.getWidth(), mDisplaySize.getHeight());
        } catch (RuntimeException e) {
            return null;
        }

        mBlurrer.blurBitmap(wallpaper, RADIUS);
        mBlurrer.destroy();

        Bitmap finalBitmap = adjustOpacity(wallpaper, OPACITY);
        wallpaper.recycle();

        return finalBitmap;
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private Bitmap
    getScaledBitmap(Bitmap bitmap, int newWidth, int newHeight) throws RuntimeException {
        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, newWidth, newHeight);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap,
                Math.round(newWidth / 2), Math.round(newHeight / 2), false);
        croppedBitmap.recycle();
        return scaledBitmap;
    }

    private Bitmap adjustOpacity(Bitmap bitmap, int opacity) {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        int colour = (opacity & 0xFF) << 24;
        canvas.drawColor(colour, PorterDuff.Mode.DST_IN);
        bitmap.recycle();
        return mutableBitmap;
    }

    public Size getDisplaySize(Context context) {
        final DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        display.getRealMetrics(metrics);
        int realWidth = metrics.widthPixels;
        int realHeight = metrics.heightPixels;

        boolean isLandscape = context.getResources()
                .getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (isLandscape) {
            int oldWidth = realWidth;
            realWidth = realHeight;
            realHeight = oldWidth;
        }

        return new Size(realWidth, realHeight);
    }

    public static class Size {
        int width;
        int height;

        private Size(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    public class ImageBlurrer {
        private RenderScript mRS;
        private ScriptIntrinsicBlur mSIBlur;
        private Allocation mTmpIn;
        private Allocation mTmpOut;

        public ImageBlurrer(Context context) {
            mRS = RenderScript.create(context);
            mSIBlur = ScriptIntrinsicBlur.create(mRS, Element.U8_4(mRS));
        }

        public Bitmap blurBitmap(Bitmap src, float radius) {
            if (mTmpIn != null) {
                mTmpIn.destroy();
            }
            if (mTmpOut != null) {
                mTmpOut.destroy();
            }

            mTmpIn = Allocation.createFromBitmap(mRS, src);
            mTmpOut = Allocation.createTyped(mRS, mTmpIn.getType());

            mSIBlur.setRadius((int) radius);
            mSIBlur.setInput(mTmpIn);
            mSIBlur.forEach(mTmpOut);
            mTmpOut.copyTo(src);
            return src;
        }

        public void destroy() {
            mSIBlur.destroy();
            if (mTmpIn != null) {
                mTmpIn.destroy();
            }
            if (mTmpOut != null) {
                mTmpOut.destroy();
            }
            mRS.destroy();
        }
    }
}