package com.step.smart.palette.widget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.blankj.utilcode.util.SizeUtils;

public class ShadowDrawable extends Drawable {

    private static final int DEFAULT_NORMAL_COLOR = 0xFFDA4336;
    private static final int DEFAULT_SHADOW_COLOR = 0x66000000;
    private float mWidth;
    private float mHeight;
    private View mTarget;
    private Integer[] mPaddings;
    private int mColorNormal = DEFAULT_NORMAL_COLOR;
    private int mShadowColor = DEFAULT_SHADOW_COLOR;
    private int mShadowRadius = SizeUtils.dp2px(4f);
    private int mShadowXOffset = SizeUtils.dp2px(1f);
    private int mShadowYOffset = SizeUtils.dp2px(3f);
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mErase = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mColPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF mRectF;
    private static final Xfermode PORTER_DUFF_CLEAR = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);

    public ShadowDrawable(@NonNull View target, int color) {
        this.mTarget = target;
        this.mColorNormal = color;
        this.mWidth = target.getWidth();
        this.mHeight = target.getHeight();
        this.mPaddings = new Integer[4];
        this.mPaddings[0] = target.getPaddingStart();
        this.mPaddings[1] = target.getPaddingTop();
        this.mPaddings[2] = target.getPaddingEnd();
        this.mPaddings[3] = target.getPaddingBottom();
        this.init();
    }

    public ShadowDrawable(@NonNull View target, int color, int width, int height, Integer... paddings) {
        this.mTarget = target;
        this.mColorNormal = color;
        this.mWidth = width;
        this.mHeight = height;
        this.mPaddings = paddings;
        this.init();
    }

    private void init() {
        mTarget.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mColorNormal);
        mErase.setXfermode(PORTER_DUFF_CLEAR);
        mColPaint.setStyle(Paint.Style.FILL);
        mColPaint.setColor(mColorNormal);

        if (!mTarget.isInEditMode()) {
            mPaint.setShadowLayer(mShadowRadius, mShadowXOffset, mShadowYOffset, DEFAULT_SHADOW_COLOR);
        }
        if (mPaddings != null && mPaddings.length >= 4 && mWidth > 0 && mHeight > 0) {
            mRectF = new RectF(mPaddings[0], mPaddings[1], mWidth - mPaddings[2], mHeight - mPaddings[3]);
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        if (mRectF != null) {
            canvas.drawRoundRect(mRectF, mRectF.height() / 2, mRectF.height() / 2, mPaint);
        }
        //canvas.drawRoundRect(mRectF, mRectF.height() / 2, mRectF.height() / 2, mErase);
        //canvas.save();
        //canvas.drawRoundRect(mRectF, mRectF.height() / 2, mRectF.height() / 2, mColPaint);
        //canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }
}
