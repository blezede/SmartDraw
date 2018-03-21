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
        mPaint.setColor(mColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setMaskFilter(new BlurMaskFilter(0.8F, BlurMaskFilter.Blur.SOLID));

        mEraserPaint = new Paint(mPaint);
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
        if (mCurrDrawMode == DrawMode.PHOTO || mCurrDrawMode == DrawMode.MOVE) {
            return false;
        }
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        mCurrX = event.getX();
        mCurrY = event.getY();
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_DOWN:
                onTouchDown();
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove();
                break;
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
        }
        flush();
        return true;
    }

    private void onTouchDown() {
        mOriginalX = mDownX = mCurrX;
        mOriginalY = mDownY = mCurrY;
        mCurrPathEntity = new PathEntity(mCurrentLineType);
        switch (mCurrentLineType) {
            case DRAW:
            case LINE:
                mCurrPathEntity.paint = new Paint(mPaint);
                mCurrPathEntity.path = new Path();
                mCurrPathEntity.path.moveTo(mCurrX, mCurrY);
                break;
            case CIRCLE:
            case RECTANGLE:
                RectF rect = new RectF(mCurrX, mCurrY, mCurrX, mCurrY);
                mCurrPathEntity.rect = rect;
                mCurrPathEntity.paint = new Paint(mPaint);
                break;
            case ERASER:
                mCurrPathEntity.paint = mEraserPaint;
                mCurrPathEntity.path = new Path();
                mCurrPathEntity.path.moveTo(mCurrX, mCurrY);
                break;
        }
        mPaletteData.pathList.add(mCurrPathEntity);
    }

    private void onTouchMove() {
        switch (mCurrentLineType) {
            case DRAW:
            case ERASER:
                mCurrPathEntity.path.quadTo(mDownX, mDownY, (mCurrX + mDownX) / 2, (mCurrY + mDownY) / 2);
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

    private void flush() {
        if (mDrawHandler == null) {
            return;
        }
        mDrawHandler.removeMessages(0);
        mDrawHandler.sendEmptyMessage(0);
    }

    /**
     * set current stroke with.
     * @param width
     */
    public void setStrokeWith(float width) {
        this.mStrokeWidth = width;
    }

    /**
     * set current mode.
     * @param mode
     */
    public void setDrawMode(DrawMode mode) {
        this.mCurrDrawMode = mode;
    }

    public void setLineType(LineType type) {
        this.mCurrentLineType = type;
    }

    /**
     * get current mode.
     *
     */
    public DrawMode getDrawMode() {
        return this.mCurrDrawMode;
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
        if(mPaletteData == null) {
            return;
        }
        mPaletteData.pathList.clear();
        flush();
    }

    public void undo() {
    }

    public void redo() {
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
                for (int i = 0; i < mPaletteData.pathList.size(); i++) {
                    PathEntity p = mPaletteData.pathList.get(i);
                    if (p.type == LineType.DRAW || p.type == LineType.LINE || p.type == LineType.ERASER) {
                        mCanvas.drawPath(p.path, p.paint);
                    } else if(p.type == LineType.CIRCLE) {
                        mCanvas.drawOval(p.rect, p.paint);
                    } else if(p.type == LineType.RECTANGLE) {
                        mCanvas.drawRect(p.rect, p.paint);
                    }
                }
                /*for (PathEntity p : mPaletteData.pathList) {
                    if (p.type == LineType.DRAW || p.type == LineType.LINE || p.type == LineType.ERASER) {
                        mCanvas.drawPath(p.path, p.paint);
                    } else if(p.type == LineType.CIRCLE) {
                        mCanvas.drawOval(p.rect, p.paint);
                    } else if(p.type == LineType.RECTANGLE) {
                        mCanvas.drawRect(p.rect, p.paint);
                    }
                    *//*switch (p.type) {
                        case DRAW:
                        case LINE:
                        case ERASER:
                            mCanvas.drawPath(p.path, p.paint);
                            break;
                        case CIRCLE:
                            mCanvas.drawOval(p.rect, p.paint);
                            break;
                        case RECTANGLE:
                            mCanvas.drawRect(p.rect, p.paint);
                            break;
                    }*//*
                }*/
            } finally {
                if (mCanvas != null) {
                    mSurfaceHolder.unlockCanvasAndPost(mCanvas);
                }
            }
        }
    }
}