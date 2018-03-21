package com.step.smart.palette.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.step.smart.palette.Constant.DrawMode;
import com.step.smart.palette.Constant.LineType;
import com.step.smart.palette.manager.FrameSizeManager;

/**
 * Created by weflow on 2018/3/21.
 */

public class PaletteFrameLayout extends FrameLayout {

    private static final String TAG = "PaletteFrameLayout";
    private PaletteView mPaletteView;
    private ContentView mContentView;
    private FrameSizeManager mFrameManager;
    private DrawMode mCurrDrawMode = DrawMode.EDIT;
    private LineType mCurrStrokeType = LineType.DRAW;

    public PaletteFrameLayout(@NonNull Context context) {
        this(context, null);
    }

    public PaletteFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PaletteFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        if (mContentView == null) {
            mContentView = new ContentView(getContext());
        }
        if (mPaletteView == null) {
            mPaletteView = new PaletteView(getContext());
        }
        this.mContentView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        this.mPaletteView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        this.removeAllViews();
        this.addView(mContentView);
        this.addView(mPaletteView);
        this.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mFrameManager = new FrameSizeManager();
    }

    public void initDrawAreas() {
        if (this.getWidth() > 0 && this.getHeight() > 0) {
            label:
            {
                if (this.getWidth() <= this.getHeight()) {
                    break label;
                }
                Log.e(TAG, "initDrawAreas --> 3 w = " + getWidth() + "\nh = " + getHeight());
                mFrameManager.frameWidth = getWidth();
                mFrameManager.frameHeight = getHeight();
                initParams();
                return;
            }
        }
        this.postDelayed(new Runnable() {
            public void run() {
                PaletteFrameLayout.this.initDrawAreas();
            }
        }, 100L);
    }

    private void initParams() {
        LayoutParams params = (LayoutParams) this.getLayoutParams();
        mFrameManager.wholeWidth = (int)(getWidth() * 1.5);
        params.width = mFrameManager.wholeWidth;
        mFrameManager.wholeHeight = (int)(getHeight() * 1.5);
        params.height = mFrameManager.wholeHeight;
        setLayoutParams(params);
        mFrameManager.posX = - (mFrameManager.wholeWidth - mFrameManager.frameWidth) / 2;
        mFrameManager.posY = - (mFrameManager.wholeHeight - mFrameManager.frameHeight) / 2;
        setX(mFrameManager.posX);
        setY(mFrameManager.posY);
    }

    private void setCurrentMode(DrawMode mode) {
        this.mCurrDrawMode = mode;
    }

    private void setStrokeType(LineType type) {
        this.mCurrStrokeType = type;
    }
}
