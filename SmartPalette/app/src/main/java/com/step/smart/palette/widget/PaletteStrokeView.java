package com.step.smart.palette.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;

import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.SizeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.step.smart.palette.Constant.DrawMode;
import com.step.smart.palette.Constant.LineType;
import com.step.smart.palette.R;
import com.step.smart.palette.entity.PaletteData;
import com.step.smart.palette.entity.PathEntity;
import com.step.smart.palette.utils.BitmapUtils;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by max on 2018/4/3.
 */

public class PaletteStrokeView extends View {

    private Paint mPaint;
    private Paint mEraserPaint;
    private Paint mEraserPointPaint;
    private Paint mBoardPaint;
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

    public Bitmap mirrorMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_confirm);
    public Bitmap deleteMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_delete);
    public Bitmap rotateMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_rotate);
    public Bitmap resetMarkBM = BitmapFactory.decodeResource(getResources(), R.drawable.mark_reset);
    public RectF markerCopyRect = new RectF(0, 0, mirrorMarkBM.getWidth(), mirrorMarkBM.getHeight());//镜像标记边界
    public RectF markerDeleteRect = new RectF(0, 0, deleteMarkBM.getWidth(), deleteMarkBM.getHeight());//删除标记边界
    public RectF markerRotateRect = new RectF(0, 0, rotateMarkBM.getWidth(), rotateMarkBM.getHeight());//旋转标记边界
    public RectF markerResetRect = new RectF(0, 0, resetMarkBM.getWidth(), resetMarkBM.getHeight());//旋转标记边界

    private int actionMode = ACTION_NONE;
    public static final int ACTION_NONE = 0;
    public static final int ACTION_DRAG = 1;
    public static final int ACTION_SCALE = 2;
    public static final int ACTION_ROTATE = 3;

    public static float SCALE_MAX = 4.0f;
    public static float SCALE_MIN = 0.2f;
    public static float SCALE_MIN_LEN;

    private ScaleGestureDetector mScaleGestureDetector;

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

        mBoardPaint = new Paint();
        mBoardPaint.setColor(Color.GRAY);
        mBoardPaint.setStrokeWidth(SizeUtils.dp2px(0.8f));
        mBoardPaint.setStyle(Paint.Style.STROKE);
        SCALE_MIN_LEN = SizeUtils.dp2px(20f);

        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                onScaleAction(detector);
                return true;
            }


            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {

            }
        });
    }



    private float mOriginalX;
    private float mOriginalY;
    private float mCurrX;
    private float mCurrY;
    private float mDownX;
    private float mDownY;
    private PathEntity mCurrPathEntity;
    private Set<Integer> mPointerIds = new HashSet<>();

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mPaletteInterface != null && mPaletteInterface.getCurrentMode() == DrawMode.MOVE) {
            return false;
        }
        if (mPaletteInterface != null && mPaletteInterface.getCurrentMode() == DrawMode.NONE) {
            return true;
        }
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        mCurrX = event.getX();
        mCurrY = event.getY();
        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                float downDistance = spacing(event);
                if (actionMode == ACTION_DRAG && downDistance > 10)//防止误触
                    actionMode = ACTION_SCALE;
                break;
            case MotionEvent.ACTION_DOWN:
                onTouchDown(event);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                onTouchMove(event);
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
        if (mPaletteInterface.getCurrStrokeType() != LineType.PHOTO) {
            mCurrPathEntity = new PathEntity(mPaletteInterface.getCurrStrokeType());
        }
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
            case PHOTO:
                float[] downPoint = new float[]{mOriginalX, mOriginalY};//还原点倍数
                if (isInMarkRect(downPoint)) {// 先判操作标记区域
                    return;
                }
                if (isInPhotoRect(mCurrPathEntity, downPoint)) {//再判断是否点击了当前图片
                    actionMode = ACTION_DRAG;
                } else {
                    //exitPhotoMode(false);
                    actionMode = ACTION_NONE;
                }
                break;
        }
    }

    public void exitPhotoMode(boolean flush) {
        exitPhotoMode(flush, false);
    }

    public void exitPhotoMode(boolean flush, boolean byUser) {
        if (mCurrPathEntity == null || mCurrPathEntity.type != LineType.PHOTO) {
            return;
        }
        actionMode = ACTION_NONE;
        if (mSyncDrawInterface != null) {
            mSyncDrawInterface.onPhotoTypeExited(byUser);
            mSyncDrawInterface.syncPhotoRecord(mCurrPathEntity);
        }
        mCurrPathEntity = null;
        if (flush) {
            invalidate();
        }
    }

    private void onTouchMove(MotionEvent event) {
        if (mCurrPathEntity == null) {
            return;
        }
        switch (mPaletteInterface.getCurrStrokeType()) {
            case DRAW:
                if (mCurrPathEntity.path == null) {
                    return;
                }
                mCurrPathEntity.path.quadTo(mDownX, mDownY, (mCurrX + mDownX) / 2, (mCurrY + mDownY) / 2);
                if (mCurrPathEntity.pathRect != null) {
                    mCurrPathEntity.pathRect.set(mDownX < mCurrPathEntity.pathRect.left ? mDownX : mCurrPathEntity.pathRect.left,
                            mDownY < mCurrPathEntity.pathRect.top ? mDownY : mCurrPathEntity.pathRect.top,
                            mDownX > mCurrPathEntity.pathRect.right ? mDownX : mCurrPathEntity.pathRect.right,
                            mDownY < mCurrPathEntity.pathRect.bottom ? mCurrPathEntity.pathRect.bottom : mDownY);
                }
                break;
            case ERASER:
                if (mCurrPathEntity.path == null) {
                    return;
                }
                mCurrPathEntity.path.quadTo(mDownX, mDownY, (mCurrX + mDownX) / 2, (mCurrY + mDownY) / 2);
                if (mSyncDrawInterface != null) {
                    mSyncDrawInterface.syncEraserPoint(event, mCurrPathEntity);
                }
                break;
            case LINE:
                if (mCurrPathEntity.path == null) {
                    return;
                }
                mCurrPathEntity.path.reset();
                mCurrPathEntity.path.moveTo(mOriginalX, mOriginalY);
                mCurrPathEntity.path.lineTo(mCurrX, mCurrY);
                break;
            case CIRCLE:
            case RECTANGLE:
                if (mCurrPathEntity.rect == null) {
                    return;
                }
                mCurrPathEntity.rect.set(mOriginalX < mCurrX ? mOriginalX : mCurrX, mOriginalY < mCurrY ? mOriginalY : mCurrY, mOriginalX > mCurrX ? mOriginalX : mCurrX, mOriginalY > mCurrY ? mOriginalY : mCurrY);
                break;
            case PHOTO:
                if (actionMode == ACTION_DRAG) {
                    onDragAction((mCurrX - mDownX), (mCurrY - mDownY));
                } else if (actionMode == ACTION_ROTATE) {
                    onRotateAction(mCurrPathEntity);
                } else if (actionMode == ACTION_SCALE) {
                    mScaleGestureDetector.onTouchEvent(event);
                }
                break;
        }
        mDownX = mCurrX;
        mDownY = mCurrY;
    }

    private void onTouchUp() {
        if (mCurrPathEntity != null && mCurrPathEntity.type != LineType.ERASER && mCurrPathEntity.type != LineType.PHOTO) {
            if (mSyncDrawInterface != null) {
                mSyncDrawInterface.syncStroke(mCurrPathEntity);
            }
        }
        if (mCurrPathEntity != null && mCurrPathEntity.type != LineType.PHOTO) {
            clear();
        }
        mDownX = mCurrX = 0;
        mDownY = mCurrY = 0;
    }

    private void flush(int x, int y) {
        postInvalidate(x - 1, y - 1, x + 1, y + 1);
    }

    public void clear() {
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
            switch (mCurrPathEntity.type) {
                case DRAW:
                case LINE:
                    canvas.drawPath(mCurrPathEntity.path, mCurrPathEntity.paint);
                    /*if (mCurrPathEntity.pathRect != null) {
                        canvas.drawRect(mCurrPathEntity.pathRect, mCurrPathEntity.paint);
                    }*/
                    break;
                case CIRCLE:
                    canvas.drawOval(mCurrPathEntity.rect, mCurrPathEntity.paint);
                    break;
                case RECTANGLE:
                    canvas.drawRect(mCurrPathEntity.rect, mCurrPathEntity.paint);

                    break;
                case ERASER:
                    if (mCurrX != 0 && mCurrY != 0) {
                        canvas.drawCircle(mCurrX, mCurrY, mEraserWidth / 2, mEraserPointPaint);
                    }
                    break;
                case PHOTO:
                    drawPhotoRecord(canvas);
                    break;
            }
        }
    }

    private void drawPhotoRecord(Canvas canvas) {
        if (mCurrPathEntity.bitmap != null) {
            canvas.drawBitmap(mCurrPathEntity.bitmap, mCurrPathEntity.matrix, null);
            SCALE_MAX = mCurrPathEntity.scaleMax;
            float[] photoCorners = calculateCorners(mCurrPathEntity);//计算图片四个角点和中心点
            drawBoard(canvas, photoCorners);//绘制图形边线
            drawMarks(canvas, photoCorners);//绘制边角图片
        }
    }


    private SyncDrawInterface mSyncDrawInterface;

    public void setSyncDrawInterface(SyncDrawInterface i) {
        this.mSyncDrawInterface = i;
    }

    public interface SyncDrawInterface {
        void syncStroke(PathEntity entity);

        void syncEraserPoint(MotionEvent event, PathEntity entity);

        void onPhotoTypeExited(boolean byUser);

        void syncPhotoRecord(PathEntity entity);
    }

    //--------------------------photo------------------------
    private void addPhotoByBitmap(Bitmap sampleBM) {
        if (sampleBM != null) {
            mCurrPathEntity = initPhotoRecord(sampleBM);
            invalidate();
        } else {
            ToastUtils.showShort("error path");
        }
    }

    @NonNull
    public PathEntity initPhotoRecord(Bitmap bitmap) {
        PathEntity entity = new PathEntity(LineType.PHOTO);
        entity.bitmap = bitmap;
        entity.photoRectSrc = new RectF(0, 0, entity.bitmap.getWidth(), entity.bitmap.getHeight());
        entity.scaleMax = getMaxScale(entity.photoRectSrc);//放大倍数
        entity.matrix = new Matrix();

        float scale = 1.0f;
        if (entity.bitmap.getWidth() > ScreenUtils.getScreenWidth() || entity.bitmap.getHeight() > ScreenUtils.getScreenHeight()) {
            scale = 0.5f;
            entity.matrix.postScale(scale, scale);
        }
        int translateX;
        int translateY;
        if (getParent() != null && getParent() != null) {
            FrameLayout parent = (FrameLayout) (getParent());
            translateX = Math.abs((int)parent.getX()) + ScreenUtils.getScreenWidth() / 2 - (int)(bitmap.getWidth() * scale) / 2;
            translateY = Math.abs((int)parent.getY()) + ScreenUtils.getScreenHeight() / 2 - (int)(bitmap.getHeight() * scale) / 2;
        } else {
            translateX = getWidth() / 2 - bitmap.getWidth() / 2;
            translateY = getHeight() / 2 - bitmap.getHeight() / 2;
        }
        entity.matrix.postTranslate(translateX, translateY);
        entity.srcMatrix = new Matrix(entity.matrix);
        return entity;
    }

    public float getMaxScale(RectF photoSrc) {
        return Math.max(getWidth(), getHeight()) / Math.max(photoSrc.width(), photoSrc.height());
    }

    public float[] calculateCorners(PathEntity record) {
        float[] photoCornersSrc = new float[10];//0,1代表左上角点XY，2,3代表右上角点XY，4,5代表右下角点XY，6,7代表左下角点XY，8,9代表中心点XY
        float[] photoCorners = new float[10];//0,1代表左上角点XY，2,3代表右上角点XY，4,5代表右下角点XY，6,7代表左下角点XY，8,9代表中心点XY
        RectF rectF = record.photoRectSrc;
        photoCornersSrc[0] = rectF.left;
        photoCornersSrc[1] = rectF.top;
        photoCornersSrc[2] = rectF.right;
        photoCornersSrc[3] = rectF.top;
        photoCornersSrc[4] = rectF.right;
        photoCornersSrc[5] = rectF.bottom;
        photoCornersSrc[6] = rectF.left;
        photoCornersSrc[7] = rectF.bottom;
        photoCornersSrc[8] = rectF.centerX();
        photoCornersSrc[9] = rectF.centerY();
        mCurrPathEntity.matrix.mapPoints(photoCorners, photoCornersSrc);
        return photoCorners;
    }
    //绘制图像边线（由于图形旋转或不一定是矩形，所以用Path绘制边线）
    public void drawBoard(Canvas canvas, float[] photoCorners) {
        Path photoBorderPath = new Path();
        photoBorderPath.moveTo(photoCorners[0], photoCorners[1]);
        photoBorderPath.lineTo(photoCorners[2], photoCorners[3]);
        photoBorderPath.lineTo(photoCorners[4], photoCorners[5]);
        photoBorderPath.lineTo(photoCorners[6], photoCorners[7]);
        photoBorderPath.lineTo(photoCorners[0], photoCorners[1]);
        canvas.drawPath(photoBorderPath, mBoardPaint);
    }

    //绘制边角操作图标
    public void drawMarks(Canvas canvas, float[] photoCorners) {
        float x;
        float y;
        x = photoCorners[0] - markerCopyRect.width() / 2;
        y = photoCorners[1] - markerCopyRect.height() / 2;
        markerCopyRect.offsetTo(x, y);
        canvas.drawBitmap(mirrorMarkBM, x, y, null);

        x = photoCorners[2] - markerDeleteRect.width() / 2;
        y = photoCorners[3] - markerDeleteRect.height() / 2;
        markerDeleteRect.offsetTo(x, y);
        canvas.drawBitmap(deleteMarkBM, x, y, null);

        x = photoCorners[4] - markerRotateRect.width() / 2;
        y = photoCorners[5] - markerRotateRect.height() / 2;
        markerRotateRect.offsetTo(x, y);
        canvas.drawBitmap(rotateMarkBM, x, y, null);

        x = photoCorners[6] - markerResetRect.width() / 2;
        y = photoCorners[7] - markerResetRect.height() / 2;
        markerResetRect.offsetTo(x, y);
        canvas.drawBitmap(resetMarkBM, x, y, null);
    }

    public boolean isInMarkRect(float[] downPoint) {
        if (markerRotateRect.contains(downPoint[0], (int) downPoint[1])) {//判断是否在区域内
            actionMode = ACTION_ROTATE;
            return true;
        }
        if (markerDeleteRect.contains(downPoint[0], (int) downPoint[1])) {//判断是否在区域内
            if (mCurrPathEntity != null && mCurrPathEntity.bitmap != null) {
                mCurrPathEntity.bitmap.recycle();
                mCurrPathEntity.bitmap = null;
            }
            mCurrPathEntity = null;
            if (mSyncDrawInterface != null) {
                mSyncDrawInterface.onPhotoTypeExited(true);
            }
            actionMode = ACTION_NONE;
            return true;
        }
        if (markerCopyRect.contains(downPoint[0], (int) downPoint[1])) {//判断是否在区域内
            exitPhotoMode(false, true);//外部已经刷新过
            return true;
        }
        if (markerResetRect.contains(downPoint[0], (int) downPoint[1])) {//判断是否在区域内
            mCurrPathEntity.matrix.reset();
            mCurrPathEntity.matrix.set(mCurrPathEntity.srcMatrix);
           /* mCurrPathEntity.matrix.setTranslate(getWidth() / 2 - mCurrPathEntity.photoRectSrc.width() / 2,
                    getHeight() / 2 - mCurrPathEntity.photoRectSrc.height() / 2);*/
            actionMode = ACTION_NONE;
            return true;
        }
        return false;
    }

    public boolean isInPhotoRect(PathEntity record, float[] downPoint) {
        if (record != null) {
            float[] invertPoint = new float[2];
            Matrix invertMatrix = new Matrix();
            if (record.matrix != null) {
                record.matrix.invert(invertMatrix);
            }
            invertMatrix.mapPoints(invertPoint, downPoint);
            return record.photoRectSrc.contains(invertPoint[0], invertPoint[1]);
        }
        return false;
    }

    public void onDragAction(float distanceX, float distanceY) {
        mCurrPathEntity.matrix.postTranslate((int) distanceX, (int) distanceY);
    }

    public void onRotateAction(PathEntity record) {
        float[] corners = calculateCorners(record);
        //放大
        //目前触摸点与图片显示中心距离,curX*drawDensity为还原缩小密度点数值
        float a = (float) Math.sqrt(Math.pow(mCurrX - corners[8], 2) + Math.pow(mCurrY - corners[9], 2));
        //目前上次旋转图标与图片显示中心距离
        float b = (float) Math.sqrt(Math.pow(corners[4] - corners[0], 2) + Math.pow(corners[5] - corners[1], 2)) / 2;
//        Log.e(TAG, "onRotateAction: a=" + a + ";b=" + b);
        //设置Matrix缩放参数
        double photoLen = Math.sqrt(Math.pow(record.photoRectSrc.width(), 2) + Math.pow(record.photoRectSrc.height(), 2));
        if (a >= photoLen / 2 * SCALE_MIN && a >= SCALE_MIN_LEN && a <= photoLen / 2 * SCALE_MAX) {
            //这种计算方法可以保持旋转图标坐标与触摸点同步缩放
            float scale = a / b;
            record.matrix.postScale(scale, scale, corners[8], corners[9]);
        }

        //旋转
        //根据移动坐标的变化构建两个向量，以便计算两个向量角度.
        PointF preVector = new PointF();
        PointF curVector = new PointF();
        preVector.set((mDownX - corners[8]), mDownY - corners[9]);//旋转后向量
        curVector.set(mCurrX - corners[8], mCurrY - corners[9]);//旋转前向量
        //计算向量长度
        double preVectorLen = getVectorLength(preVector);
        double curVectorLen = getVectorLength(curVector);
        //计算两个向量的夹角.
        double cosAlpha = (preVector.x * curVector.x + preVector.y * curVector.y)
                / (preVectorLen * curVectorLen);
        //由于计算误差，可能会带来略大于1的cos，例如
        if (cosAlpha > 1.0f) {
            cosAlpha = 1.0f;
        }
        //本次的角度已经计算出来。
        double dAngle = Math.acos(cosAlpha) * 180.0 / Math.PI;
        // 判断顺时针和逆时针.
        //判断方法其实很简单，这里的v1v2其实相差角度很小的。
        //先转换成单位向量
        preVector.x /= preVectorLen;
        preVector.y /= preVectorLen;
        curVector.x /= curVectorLen;
        curVector.y /= curVectorLen;
        //作curVector的逆时针垂直向量。
        PointF verticalVec = new PointF(curVector.y, -curVector.x);

        //判断这个垂直向量和v1的点积，点积>0表示俩向量夹角锐角。=0表示垂直，<0表示钝角
        float vDot = preVector.x * verticalVec.x + preVector.y * verticalVec.y;
        if (vDot > 0) {
            //v2的逆时针垂直向量和v1是锐角关系，说明v1在v2的逆时针方向。
        } else {
            dAngle = -dAngle;
        }
        record.matrix.postRotate((float) dAngle, corners[8], corners[9]);
    }

    /**
     * 获取p1到p2的线段的长度
     *
     * @return
     */
    public double getVectorLength(PointF vector) {
        return Math.sqrt(vector.x * vector.x + vector.y * vector.y);
    }

    public void onScaleAction(ScaleGestureDetector detector) {
        float[] photoCorners = calculateCorners(mCurrPathEntity);
        //目前图片对角线长度
        float len = (float) Math.sqrt(Math.pow(photoCorners[0] - photoCorners[4], 2) + Math.pow(photoCorners[1] - photoCorners[5], 2));
        double photoLen = Math.sqrt(Math.pow(mCurrPathEntity.photoRectSrc.width(), 2) + Math.pow(mCurrPathEntity.photoRectSrc.height(), 2));
        float scaleFactor = detector.getScaleFactor();
        //设置Matrix缩放参数
        if ((scaleFactor < 1 && len >= photoLen * SCALE_MIN && len >= SCALE_MIN_LEN) || (scaleFactor > 1 && len <= photoLen * SCALE_MAX)) {
            Log.e(scaleFactor + "", scaleFactor + "");
            mCurrPathEntity.matrix.postScale(scaleFactor, scaleFactor, photoCorners[8], photoCorners[9]);
        }
    }

    public float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    @SuppressLint("StaticFieldLeak")
    public void addPhotoByPath(final String path) {
        if (mCurrPathEntity != null && mCurrPathEntity.type == LineType.PHOTO) {
            if (mSyncDrawInterface != null) {
                mSyncDrawInterface.syncPhotoRecord(mCurrPathEntity);
            }
            mCurrPathEntity = null;
            invalidate();
        }
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... voids) {
                int degree = BitmapUtils.readPictureDegree(path);
                Bitmap bitmap = getSampleBitMap(path);
                if (degree > 0) {
                    return BitmapUtils.rotateToDegrees(bitmap, degree);
                } else {
                    return bitmap;
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                addPhotoByBitmap(bitmap);
            }
        }.execute();
    }

    private Bitmap getSampleBitMap(String path) {
        Bitmap sampleBM = null;
        if (path.contains(Environment.getExternalStorageDirectory().toString())) {
            sampleBM = getSDCardPhoto(path);
        } else {
            sampleBM = getAssetsPhoto(path);
        }
        return sampleBM;
    }
    private float simpleScale = 0.5f;//图片载入的缩放倍数
    private Bitmap getSDCardPhoto(String path) {
        File file = new File(path);
        if (file.exists()) {
            return BitmapUtils.decodeSampleBitMapFromFile(getContext(), path, simpleScale);
        } else {
            return null;
        }
    }

    public Bitmap getAssetsPhoto(String path) {
        return BitmapUtils.getBitmapFromAssets(getContext(), path);
    }
}
