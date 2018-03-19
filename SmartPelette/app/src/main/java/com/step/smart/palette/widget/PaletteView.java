package com.step.smart.palette.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
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

public class PaletteView extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder mSurfaceHolder;
    private HandlerThread mDrawThread;
    private DrawHandler mDrawHandler;
    private Paint mPaint;
    private Canvas mCanvas;
    private boolean mIsDraw = false;
    Map<String, PathEntity> mCurrentPathMap = Collections.synchronizedMap(new HashMap<String, PathEntity>());
    private PaletteData mPaletteData = new PaletteData();
    private float mStrokeWith = 5f;
    private DrawMode mCurrDrawMode = DrawMode.DRAW;

    public PaletteView(Context context) {
        this(context, null);
    }

    public PaletteView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PaletteView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        _init();
    }

    private void _init() {
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
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
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

    private float mCurrX;
    private float mCurrY;
    private float mDownX;
    private float mDownY;
    private PathEntity mCurrPathEntity;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        mCurrX = event.getX();
        mCurrY = event.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mCurrPathEntity = new PathEntity(LineType.DRAW);
                Paint paint = new Paint(mPaint);
                Path path = new Path();
                mDownX = mCurrX;
                mDownY = mCurrY;
                mCurrPathEntity.paint = paint;
                mCurrPathEntity.path = path;
                mPaletteData.pathList.add(mCurrPathEntity);
                mCurrPathEntity.path.moveTo(mCurrX, mCurrY);
                break;
            case MotionEvent.ACTION_MOVE:
                mCurrPathEntity.path.quadTo(mDownX, mDownY, (mCurrX + mDownX) / 2, (mCurrY + mDownY) / 2);
                mDownX = mCurrX;
                mDownY = mCurrY;
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        flush();
        return true;
    }

    private void flush() {
        if (mDrawHandler == null) {
            return;
        }
        mDrawHandler.removeMessages(0);
        mDrawHandler.sendEmptyMessage(0);
    }

    private void setStrokeWith(float width) {
        this.mStrokeWith = width;
    }

    private void setDrawMode(DrawMode mode) {
        this.mCurrDrawMode = mode;
    }

    public void clear() {
        if(mPaletteData == null) {
            return;
        }
        mPaletteData.pathList.clear();
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
                for (PathEntity p : mPaletteData.pathList) {
                    mCanvas.drawPath(p.path, p.paint);
                }
            } finally {
                if (mCanvas != null) {
                    mSurfaceHolder.unlockCanvasAndPost(mCanvas);
                }
            }
        }
    }
}