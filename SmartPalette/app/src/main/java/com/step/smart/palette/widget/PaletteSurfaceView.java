package com.step.smart.palette.widget;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.step.smart.palette.Constant.DrawMode;
import com.step.smart.palette.Constant.LineType;
import com.step.smart.palette.entity.PathEntity;
import com.step.smart.palette.entity.PaletteData;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by weflow on 2018/3/19.
 */

public class PaletteSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private SurfaceHolder mSurfaceHolder;
    private HandlerThread mDrawThread;
    private DrawHandler mDrawHandler;
    private Paint mPaint;
    private Paint mEraserPaint;
    private Canvas mCanvas;
    private boolean mIsDraw = false;
    private Map<String, PathEntity> mCurrentPathMap = Collections.synchronizedMap(new HashMap<String, PathEntity>());
    private PaletteData mPaletteData = new PaletteData();
    private float mStrokeWidth = 5f;
    private float mEraserWidth = 140f;
    private DrawMode mCurrDrawMode = DrawMode.EDIT;
    private LineType mCurrentLineType = LineType.DRAW;
    private int mColor = Color.BLACK;
    private PaletteView.PaletteInterface mPaletteInterface;

    public PaletteSurfaceView(Context context) {
        this(context, null);
    }

    public PaletteSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PaletteSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        _init(context);
    }

    private void _init(Context context) {
        if (context instanceof PaletteView.PaletteInterface) {
            mPaletteInterface = (PaletteView.PaletteInterface) context;
        }
        mSurfaceHolder = getHolder();
        setZOrderOnTop(true);
        mSurfaceHolder.setFormat(PixelFormat.TRANSLUCENT);
        mSurfaceHolder.addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        this.setKeepScreenOn(true);

        //画笔
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setStrokeWidth(5f);
        mPaint.setColor(mColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setXfermode(null);
        mPaint.setMaskFilter(new BlurMaskFilter(0.8F, BlurMaskFilter.Blur.SOLID));

        mEraserPaint = new Paint();
        mEraserPaint.setStyle(Paint.Style.STROKE);
        mEraserPaint.setStrokeJoin(Paint.Join.ROUND);
        mEraserPaint.setStrokeCap(Paint.Cap.ROUND);//线冒
        mEraserPaint.setStrokeWidth(mEraserWidth);
        mEraserPaint.setColor(Color.WHITE);
        mEraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));//关键代码
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.mIsDraw = true;
        this.mDrawThread = new HandlerThread("SurfaceView_Draw");
        this.mDrawThread.setPriority(Thread.MAX_PRIORITY);
        this.mDrawThread.start();
        this.mDrawHandler = new DrawHandler(mDrawThread.getLooper());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.mIsDraw = false;
        this.mDrawThread.quitSafely();
        this.mDrawHandler = null;
    }

    private float mOriginalX;
    private float mOriginalY;
    private float mCurrX;
    private float mCurrY;
    private float mDownX;
    private float mDownY;
    private PathEntity mCurrPathEntity;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mPaletteInterface != null && (mPaletteInterface.getCurrentMode() == DrawMode.PHOTO || mPaletteInterface.getCurrentMode() == DrawMode.MOVE)) {
            return false;
        }
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        mCurrX = event.getX();
        mCurrY = event.getY();
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_DOWN:
                onTouchDown(event);
                flush();
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(event);
                flush();
                break;
            case MotionEvent.ACTION_UP:
                onTouchUp();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
        }
        return true;
    }

    private void onTouchDown(MotionEvent event) {
        mOriginalX = mDownX = mCurrX;
        mOriginalY = mDownY = mCurrY;
        mCurrPathEntity = new PathEntity(mPaletteInterface.getCurrStrokeType());
        switch (mPaletteInterface.getCurrStrokeType()) {
            case DRAW:
            case LINE:
                mCurrPathEntity.paint = new Paint(mPaint);
                mCurrPathEntity.paint.setColor(mPaletteInterface.getStrokeColor());
                mCurrPathEntity.paint.setStrokeWidth(mPaletteInterface.getStrokeWidth());
                mCurrPathEntity.paint.setAlpha(mPaletteInterface.getStrokeAlpha());
                mCurrPathEntity.path = new Path();
                mCurrPathEntity.path.moveTo(mCurrX, mCurrY);
                break;
            case CIRCLE:
            case RECTANGLE:
                RectF r = new RectF(mCurrX, mCurrY, mCurrX, mCurrY);
                mCurrPathEntity.rect = r;
                mCurrPathEntity.paint = new Paint(mPaint);
                mCurrPathEntity.paint.setColor(mPaletteInterface.getStrokeColor());
                mCurrPathEntity.paint.setAlpha(mPaletteInterface.getStrokeAlpha());
                mCurrPathEntity.paint.setStrokeWidth(mPaletteInterface.getStrokeWidth());
                break;
            case ERASER:
                mCurrPathEntity.paint = new Paint(mEraserPaint);
                mCurrPathEntity.path = new Path();
                mCurrPathEntity.path.moveTo(mCurrX, mCurrY);
                if (mSyncDrawInterface != null) {
                    mSyncDrawInterface.syncEraserPoint(event, mCurrPathEntity);
                }
                break;
        }
    }

    private void onTouchMove(MotionEvent event) {
        switch (mPaletteInterface.getCurrStrokeType()) {
            case DRAW:
                mCurrPathEntity.path.quadTo(mDownX, mDownY, (mCurrX + mDownX) / 2, (mCurrY + mDownY) / 2);
                break;
            case ERASER:
                mCurrPathEntity.path.quadTo(mDownX, mDownY, (mCurrX + mDownX) / 2, (mCurrY + mDownY) / 2);
                if (mSyncDrawInterface != null) {
                    mSyncDrawInterface.syncEraserPoint(event, mCurrPathEntity);
                }
                break;
            case LINE:
                mCurrPathEntity.path.reset();
                mCurrPathEntity.path.moveTo(mOriginalX, mOriginalY);
                mCurrPathEntity.path.lineTo(mCurrX, mCurrY);
                break;
            case CIRCLE:
            case RECTANGLE:
                mCurrPathEntity.rect.set(mOriginalX < mCurrX ? mOriginalX : mCurrX, mOriginalY < mCurrY ? mOriginalY : mCurrY, mOriginalX > mCurrX ? mOriginalX : mCurrX, mOriginalY > mCurrY ? mOriginalY : mCurrY);
                break;
        }
        mDownX = mCurrX;
        mDownY = mCurrY;
    }

    private void onTouchUp() {
        if (mCurrPathEntity.type != LineType.ERASER) {
            if (mSyncDrawInterface != null) {
                mSyncDrawInterface.syncStroke(mCurrPathEntity);
            }
        }
        mDownX = mCurrX = 0;
        mDownY = mCurrY = 0;
        mCurrPathEntity = null;
        clear();
        Log.e("SurfaceView", "w = " + getWidth() + ", h = " + getHeight());
    }

    private void flush() {
        if (mDrawHandler == null) {
            return;
        }
        mDrawHandler.removeMessages(0);
        mDrawHandler.sendEmptyMessage(0);
    }

    /**
     * new page data
     */
    public void setPaletteData(PaletteData data) {
        this.mPaletteData = data;
        flush();
    }

    /**
     * clear screen
     */
    public void clear() {
        if (mPaletteData == null) {
            return;
        }
        mPaletteData.pathList.clear();
        postDelayed(this, 100);
    }

    @Override
    public void run() {
        flush();
    }

    class DrawHandler extends Handler {

        public DrawHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (!mIsDraw) {
                return;
            }
            try {
                long start = System.currentTimeMillis();
                mCanvas = mSurfaceHolder.lockCanvas();
                if (mCanvas == null) {
                    return;
                }
                mCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                if (mCurrPathEntity != null) {
                    if (mCurrPathEntity.type == LineType.DRAW || mCurrPathEntity.type == LineType.LINE) {
                        mCanvas.drawPath(mCurrPathEntity.path, mCurrPathEntity.paint);
                    } else if (mCurrPathEntity.type == LineType.CIRCLE) {
                        mCanvas.drawOval(mCurrPathEntity.rect, mCurrPathEntity.paint);
                    } else if (mCurrPathEntity.type == LineType.RECTANGLE) {
                        mCanvas.drawRect(mCurrPathEntity.rect, mCurrPathEntity.paint);
                    }
                }
            } finally {
                if (mCanvas != null) {
                    mSurfaceHolder.unlockCanvasAndPost(mCanvas);
                }
            }
        }
    }

    private SyncDrawInterface mSyncDrawInterface;

    public void setSyncDrawInterface(SyncDrawInterface i) {
        this.mSyncDrawInterface = i;
    }

    public interface SyncDrawInterface {
        void syncStroke(PathEntity mCurrPathEntity);

        void syncEraserPoint(MotionEvent event, PathEntity entity);
    }
}