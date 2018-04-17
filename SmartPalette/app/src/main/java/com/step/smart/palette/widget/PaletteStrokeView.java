package com.step.smart.palette.widget;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.blankj.utilcode.util.SizeUtils;
import com.step.smart.palette.Constant.DrawMode;
import com.step.smart.palette.Constant.LineType;
import com.step.smart.palette.entity.PaletteData;
import com.step.smart.palette.entity.PathEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by weflow on 2018/4/3.
 */

public class PaletteStrokeView extends View {

    private Paint mPaint;
    private Paint mEraserPaint;
    private Paint mEraserPointPaint;
    private Canvas mCanvas;
    private boolean mIsDraw = false;
    private Map<String, PathEntity> mCurrentPathMap = Collections.synchronizedMap(new HashMap<String, PathEntity>());
    private PaletteData mPaletteData = new PaletteData();
    private float mStrokeWidth = 5f;
    private float mEraserWidth = SizeUtils.dp2px(40f);
    private DrawMode mCurrDrawMode = DrawMode.EDIT;
    private LineType mCurrentLineType = LineType.DRAW;
    private int mColor = Color.BLACK;
    private PaletteView.PaletteInterface mPaletteInterface;
    private BlurMaskFilter mDefaultBlur = new BlurMaskFilter(0.8F, BlurMaskFilter.Blur.SOLID);
    private BlurMaskFilter mHighLightBlur = new BlurMaskFilter(10F, BlurMaskFilter.Blur.OUTER);
    public PaletteStrokeView(Context context) {
        this(context, null);
    }

    public PaletteStrokeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PaletteStrokeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        _init(context);
    }

    private void _init(Context context) {
        if (context instanceof PaletteView.PaletteInterface) {
            mPaletteInterface = (PaletteView.PaletteInterface) context;
        }
        this.setBackgroundColor(Color.TRANSPARENT);

        //画笔
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setStrokeWidth(5f);
        mPaint.setColor(mColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setXfermode(null);
        mPaint.setMaskFilter(mDefaultBlur);

        mEraserPaint = new Paint();
        mEraserPaint.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mEraserPaint.setStyle(Paint.Style.STROKE);
        mEraserPaint.setStrokeJoin(Paint.Join.ROUND);
        mEraserPaint.setStrokeCap(Paint.Cap.ROUND);//线冒
        mEraserPaint.setStrokeWidth(mEraserWidth);
        mEraserPaint.setColor(Color.WHITE);
        mEraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));//关键代码

        mEraserPointPaint = new Paint(mEraserPaint);
        mEraserPointPaint.setStyle(Paint.Style.FILL);
        mEraserPointPaint.setXfermode(null);
        mEraserPointPaint.setMaskFilter(new BlurMaskFilter(5F, BlurMaskFilter.Blur.SOLID));
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
                //flush((int) mCurrX, (int) mCurrY);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(event);
                //flush((int) mCurrX, (int) mCurrY);
                invalidate();
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
                RectF pathR = new RectF(mCurrX, mCurrY, mCurrX, mCurrY);
                mCurrPathEntity.paint = new Paint(mPaint);
                mCurrPathEntity.paint.setColor(mPaletteInterface.getStrokeColor());
                mCurrPathEntity.paint.setStrokeWidth(mPaletteInterface.getStrokeWidth());
                mCurrPathEntity.paint.setAlpha(mPaletteInterface.getStrokeAlpha());
                mCurrPathEntity.paint.setMaskFilter(mPaletteInterface.isHighLighter() ? mHighLightBlur : mDefaultBlur);
                mCurrPathEntity.path = new Path();
                mCurrPathEntity.path.moveTo(mCurrX, mCurrY);
                mCurrPathEntity.pathRect = pathR;
                break;
            case CIRCLE:
            case RECTANGLE:
                RectF r = new RectF(mCurrX, mCurrY, mCurrX, mCurrY);
                mCurrPathEntity.rect = r;
                mCurrPathEntity.paint = new Paint(mPaint);
                mCurrPathEntity.paint.setColor(mPaletteInterface.getStrokeColor());
                mCurrPathEntity.paint.setAlpha(mPaletteInterface.getStrokeAlpha());
                mCurrPathEntity.paint.setStrokeWidth(mPaletteInterface.getStrokeWidth());
                mCurrPathEntity.paint.setMaskFilter(mPaletteInterface.isHighLighter() ? mHighLightBlur : mDefaultBlur);
                mCurrPathEntity.pathRect = r;
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
                if(mCurrPathEntity.pathRect != null) {
                    mCurrPathEntity.pathRect.set(mDownX < mCurrPathEntity.pathRect.left ? mDownX : mCurrPathEntity.pathRect.left,
                            mDownY < mCurrPathEntity.pathRect.top ? mDownY : mCurrPathEntity.pathRect.top,
                            mDownX > mCurrPathEntity.pathRect.right ? mDownX : mCurrPathEntity.pathRect.right,
                            mDownY < mCurrPathEntity.pathRect.bottom ? mCurrPathEntity.pathRect.bottom : mDownY);
                }
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
        clear();
    }

    private void flush(int x, int y) {
        postInvalidate(x - 1, y - 1, x + 1, y + 1);
    }

    public void clear() {
        mDownX = mCurrX = 0;
        mDownY = mCurrY = 0;
        mCurrPathEntity = null;
        if (mPaletteData == null) {
            return;
        }
        mPaletteData.pathList.clear();
        invalidate();
    }

    private Path mEraserPointPath = new Path();
    @Override
    protected void onDraw(Canvas canvas) {
        if (mCurrPathEntity != null) {
            if (mCurrPathEntity.type == LineType.DRAW || mCurrPathEntity.type == LineType.LINE) {
                canvas.drawPath(mCurrPathEntity.path, mCurrPathEntity.paint);
                if (mCurrPathEntity.pathRect != null) {
                    canvas.drawRect(mCurrPathEntity.pathRect, mCurrPathEntity.paint);
                }
            } else if (mCurrPathEntity.type == LineType.CIRCLE) {
                canvas.drawOval(mCurrPathEntity.rect, mCurrPathEntity.paint);
            } else if (mCurrPathEntity.type == LineType.RECTANGLE) {
                canvas.drawRect(mCurrPathEntity.rect, mCurrPathEntity.paint);
            } else if(mCurrPathEntity.type == LineType.ERASER) {
                if (mCurrX != 0 && mCurrY != 0) {
                    canvas.drawCircle(mCurrX, mCurrY, mEraserWidth / 2, mEraserPointPaint);
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
