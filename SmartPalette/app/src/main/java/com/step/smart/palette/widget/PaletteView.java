package com.step.smart.palette.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.blankj.utilcode.util.FileUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.step.smart.palette.Constant.DrawMode;
import com.step.smart.palette.Constant.LineType;
import com.step.smart.palette.Constant.PreferenceConstant;
import com.step.smart.palette.manager.FrameSizeManager;
import com.step.smart.palette.utils.BitmapUtils;
import com.step.smart.palette.utils.Preferences;
import com.step.smart.palette.utils.StorageUtils;

/**
 * Created by weflow on 2018/3/21.
 */

public class PaletteView extends FrameLayout {

    private static final String TAG = "PaletteView";
    private PaletteSurfaceView mPaletteSurfaceView;
    private StrokeDrawView mStrokeDrawView;
    public FrameSizeManager mFrameManager;
    private PaletteInterface mPaletteInterface;
    private FrameLayout mFrame;
    private PaletteStrokeView mPaletteStrokeView;

    public PaletteView(@NonNull Context context) {
        this(context, null);
    }

    public PaletteView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PaletteView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        if (context instanceof PaletteInterface) {
            mPaletteInterface = (PaletteInterface) context;
        }
        if (mStrokeDrawView == null) {
            mStrokeDrawView = new StrokeDrawView(context);
        }
        /*if (mPaletteSurfaceView == null) {
            mPaletteSurfaceView = new PaletteSurfaceView(context);
        }*/
        if (mPaletteStrokeView == null) {
            mPaletteStrokeView = new PaletteStrokeView(context);
        }
        if (mFrame == null) {
            mFrame = new FrameLayout(context);
        }
        //mPaletteSurfaceView.setSyncDrawInterface(mStrokeDrawView);
        mPaletteStrokeView.setSyncDrawInterface(mStrokeDrawView);
        this.mStrokeDrawView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        //this.mPaletteSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        this.mPaletteStrokeView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        this.mFrame.removeAllViews();
        this.mFrame.addView(mStrokeDrawView);
        //this.mFrame.addView(mPaletteSurfaceView);
        this.mFrame.addView(mPaletteStrokeView);
        this.addView(this.mFrame);
        this.mFrame.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mFrameManager = new FrameSizeManager();
    }

    public void initDrawAreas() {
        Log.e(TAG, "initDrawAreas --> " + "\nwidth = " + mFrameManager.frameWidth + "\nheight = " + mFrameManager.frameHeight + "\nscreen_width = " + mFrameManager.wholeWidth + "\nscreen_height = " + mFrameManager.wholeHeight);
        if (this.getWidth() > 0 && this.getHeight() > 0) {
            label:
            {
                if (ScreenUtils.isLandscape() && this.getWidth() <= this.getHeight()) {
                    break label;
                }
                if (ScreenUtils.isPortrait() && this.getWidth() > this.getHeight()) {
                    break label;
                }
                mFrameManager.frameWidth = getWidth();
                mFrameManager.frameHeight = getHeight();
                initParams();
                return;
            }
        }
        this.postDelayed(new Runnable() {
            public void run() {
                PaletteView.this.initDrawAreas();
            }
        }, 100L);
    }

    private void initParams() {
        float factor = 1.0f;
        if (ScreenUtils.isTablet()) {
            factor = 1.25f;
        } else {
            factor = 1.5f;
        }
        LayoutParams params = (LayoutParams) this.mFrame.getLayoutParams();
        mFrameManager.wholeWidth = (int)(getWidth() * factor);
        params.width = mFrameManager.wholeWidth;
        mFrameManager.wholeHeight = (int)(getHeight() * factor);
        params.height = mFrameManager.wholeHeight;
        this.mFrame.setLayoutParams(params);
        mFrameManager.posX = - (mFrameManager.wholeWidth - mFrameManager.frameWidth) / 2;
        mFrameManager.posY = - (mFrameManager.wholeHeight - mFrameManager.frameHeight) / 2;
        this.mFrame.setX(mFrameManager.posX);
        this.mFrame.setY(mFrameManager.posY);
        mFrameManager.calculate();
        Preferences.saveInt(PreferenceConstant.SCREEN_WIDTH, mFrameManager.wholeWidth);
        Preferences.saveInt(PreferenceConstant.SCREEN_HEIGHT, mFrameManager.wholeHeight);
        Log.e(TAG, "initParams --> " + "\nwidth = " + mFrameManager.frameWidth + "\nheight = " + mFrameManager.frameHeight + "\nscreen_width = " + mFrameManager.wholeWidth + "\nscreen_height = " + mFrameManager.wholeHeight);
    }

    public void clear() {
        mStrokeDrawView.clear();
    }

    public void undo() {
        mStrokeDrawView.undo();
    }

    public void redo() {
        mStrokeDrawView.redo();
    }

    public boolean isEmpty() {
        return mStrokeDrawView.isEmpty();
    }

    public void setBackgroundColor(int color) {
        this.mFrame.setBackgroundColor(color);
    }

    public String screenShot(boolean wholeScreen) {
        return this.screenShot(wholeScreen, false);
    }

    public String screenShot(boolean wholeScreen, boolean isTemp) {
        long start = System.currentTimeMillis();
        //String path = getContext().getExternalCacheDir().getAbsolutePath() + File.separator + System.currentTimeMillis() + ".png";
        String dirPath = StorageUtils.getRecordImgDirPath();
        boolean existsDir = FileUtils.createOrExistsDir(dirPath);
        if (!existsDir) {
            return "";
        }
        String fileName = System.currentTimeMillis() + ".png";
        if (isTemp) {
            fileName = StorageUtils.TEMP_IMG;
        }
        String path = dirPath + fileName;
        if (this.mFrame == null || this.mFrameManager == null) {
            return "";
        }
        if (wholeScreen) {
            Bitmap bmp = Bitmap.createBitmap(this.mFrame.getWidth(), this.mFrame.getHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bmp);
            this.mFrame.draw(canvas);
            byte[] data = BitmapUtils.bitmap2Bytes(bmp, 100);
            if (data.length <= 0) {
                return "";
            }
            boolean result = BitmapUtils.saveByteData(data, path);
            if (result) {
                return path;
            }
        } else {
            Bitmap bmp = Bitmap.createBitmap(this.mFrame.getWidth(), this.mFrame.getHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bmp);
            this.mFrame.draw(canvas);
            Bitmap currBmp = Bitmap.createBitmap(this.mFrameManager.frameWidth, this.mFrameManager.frameHeight, Bitmap.Config.RGB_565);
            Canvas currCanvas = new Canvas(currBmp);
            currCanvas.drawBitmap(bmp,
                    new Rect((int)(Math.abs(this.mFrame.getX())), (int)Math.abs(this.mFrame.getY()), (int)(Math.abs(this.mFrame.getX()) + this.mFrameManager.frameWidth), (int)(Math.abs(this.mFrame.getY()) + this.mFrameManager.frameHeight)),
                    new Rect(0, 0, this.mFrameManager.frameWidth, this.mFrameManager.frameHeight),
                    null);
            byte[] data = BitmapUtils.bitmap2Bytes(currBmp, 100);
            if (data.length <= 0) {
                return "";
            }
            boolean result = BitmapUtils.saveByteData(data, path);
            if (result) {
                return path;
            }
        }
        Log.e("PaletteView", "save finish --> " + (System.currentTimeMillis() - start));
        return "";
    }

    public Bitmap screenShotBitmap(boolean wholeScreen) {
        if (wholeScreen) {
            Bitmap bmp = Bitmap.createBitmap(this.mFrame.getWidth(), this.mFrame.getHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bmp);
            this.mFrame.draw(canvas);
            return bmp;
        } else {
            Bitmap bmp = Bitmap.createBitmap(this.mFrame.getWidth(), this.mFrame.getHeight(), Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bmp);
            this.mFrame.draw(canvas);
            Bitmap currBmp = Bitmap.createBitmap(this.mFrameManager.frameWidth, this.mFrameManager.frameHeight, Bitmap.Config.ARGB_4444);
            Canvas currCanvas = new Canvas(currBmp);
            currCanvas.drawBitmap(bmp,
                    new Rect((int)(Math.abs(this.mFrame.getX())), (int)Math.abs(this.mFrame.getY()), (int)(Math.abs(this.mFrame.getX()) + this.mFrameManager.frameWidth), (int)(Math.abs(this.mFrame.getX()) + this.mFrameManager.frameHeight)),
                    new Rect(0, 0, this.mFrameManager.frameWidth, this.mFrameManager.frameHeight),
                    null);
           return currBmp;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mPaletteInterface.getCurrentMode() == DrawMode.MOVE) {
            onTouchHandle(event);
            return true;
        }
        return super.onTouchEvent(event);

    }

    private float mDownLastX;
    private float mDownLastY;
    private void onTouchHandle(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int tt = (int) ((double) (this.mFrame.getY() + (y - this.mDownLastY))/* + 0.5D*/);
        int tl = (int) ((double) (this.mFrame.getX() + (x - this.mDownLastX))/* + 0.5D*/);
        int gh = this.mFrameManager.windowTop * 2;
        int gw = this.mFrameManager.windowLeft * 2;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.mFrame.clearAnimation();
                break;
            case MotionEvent.ACTION_UP:
                if (this.mFrameManager.windowTop < 0) {
                    if (tt < gh) {
                        tt = gh;
                    } else if (tt > 0) {
                        tt = 0;
                    }
                } else if (this.mFrameManager.windowTop > 0) {
                    if (tt > gh) {
                        tt = gh;
                    } else if (tt < 0) {
                        tt = 0;
                    }
                } else {
                    tt = 0;
                }

                if (this.mFrameManager.windowLeft < 0) {
                    if (tl < gw) {
                        tl = gw;
                    } else if (tl > 0) {
                        tl = 0;
                    }
                } else if (this.mFrameManager.windowLeft > 0) {
                    if (tl > gw) {
                        tl = gw;
                    } else if (tl < 0) {
                        tl = 0;
                    }
                } else {
                    tl = 0;
                }

                this.mFrame.animate().setDuration(200L).setInterpolator(new DecelerateInterpolator()).y((float) tt).x((float) tl);
                break;
            case MotionEvent.ACTION_MOVE:
                this.mFrame.setY((float) tt);
                this.mFrame.setX((float) tl);
        }

        this.mDownLastX = x;
        this.mDownLastY = y;

    }

    public int getPicturesCount() {
        return mStrokeDrawView.getPicturesCount();
    }

    public interface PaletteInterface{

        DrawMode getCurrentMode();

        LineType getCurrStrokeType();

        float getStrokeWidth();

        int getStrokeColor();

        int getStrokeAlpha();//0 - 255;

        void onUndoRedoCountChanged(int redo, int undo);

        boolean isHighLighter();

        void onPhotoTypeExited(boolean byUser);
    }

    public void addPhotoByPath(String path) {
        mPaletteStrokeView.addPhotoByPath(path);
    }

    public void exitPhotoMode(boolean flush) {
        mPaletteStrokeView.exitPhotoMode(flush);
    }

    public void exitPhotoMode(boolean flush, boolean byUser) {
        mPaletteStrokeView.exitPhotoMode(flush, byUser);
    }
}
