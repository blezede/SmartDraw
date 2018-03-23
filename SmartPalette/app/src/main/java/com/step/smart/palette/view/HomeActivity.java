package com.step.smart.palette.view;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.step.smart.palette.Constant.DrawMode;
import com.step.smart.palette.Constant.LineType;
import com.step.smart.palette.R;
import com.step.smart.palette.widget.PaletteView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class HomeActivity extends AppCompatActivity implements PaletteView.PaletteInterface {

    @BindView(R.id.frame)
    PaletteView mPaletteView;
    @BindView(R.id.tools)
    LinearLayout mLinearLayout;
    @BindView(R.id.save)
    RelativeLayout mSaveView;
    @BindView(R.id.stroke)
    RelativeLayout mStrokeView;
    @BindView(R.id.move)
    RelativeLayout mMoveView;
    @BindView(R.id.eraser)
    RelativeLayout mEraserView;
    @BindView(R.id.undo)
    RelativeLayout mUndoView;
    @BindView(R.id.redo)
    RelativeLayout mRedoView;

    private int mColor = Color.BLACK;
    private float mStrokeWidth = 5f;
    private LineType mLineType = LineType.DRAW;
    private DrawMode mCurrDrawMode = DrawMode.EDIT;
    private LineType mStrokeLineType = LineType.DRAW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        mPaletteView.initDrawAreas();
    }

    @OnClick({R.id.save, R.id.stroke, R.id.move, R.id.eraser, R.id.undo, R.id.redo})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save:
                break;
            case R.id.undo://撤销
                mPaletteView.undo();
                break;
            case R.id.redo://恢复
                mPaletteView.redo();
                break;
            case R.id.stroke:
                releaseSelStatus();
                mStrokeView.setBackgroundResource(R.drawable.btn_sel_bg);
                if (mCurrDrawMode != DrawMode.EDIT) {
                    mCurrDrawMode = DrawMode.EDIT;
                }
                mLineType = LineType.DRAW;
                break;
            case R.id.eraser:
                releaseSelStatus();
                mEraserView.setBackgroundResource(R.drawable.btn_sel_bg);
                if (mCurrDrawMode != DrawMode.EDIT) {
                    mCurrDrawMode = DrawMode.EDIT;
                }
                mLineType = LineType.ERASER;
                break;
            case R.id.move:
                releaseSelStatus();
                mMoveView.setBackgroundResource(R.drawable.btn_sel_bg);
                if (mCurrDrawMode != DrawMode.MOVE) {
                    mCurrDrawMode = DrawMode.MOVE;
                } else {
                    break;
                }
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public DrawMode getCurrentMode() {
        return mCurrDrawMode;
    }

    @Override
    public LineType getCurrStrokeType() {
        return mLineType;
    }

    @Override
    public float getStrokeWidth() {
        return mStrokeWidth;
    }

    @Override
    public int getStrokeColor() {
        return mColor;
    }

    @Override
    public int getStrokeAlpha() {
        return 255;
    }

    private void releaseSelStatus() {
        mStrokeView.setBackgroundColor(Color.TRANSPARENT);
        mMoveView.setBackgroundColor(Color.TRANSPARENT);
        mEraserView.setBackgroundColor(Color.TRANSPARENT);
    }
}