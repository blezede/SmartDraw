package com.step.smart.palette.view;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.step.smart.palette.Constant.DrawMode;
import com.step.smart.palette.Constant.LineType;
import com.step.smart.palette.R;
import com.step.smart.palette.widget.PaletteView;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class HomeActivity extends AppCompatActivity implements PaletteView.PaletteInterface {

    //@BindView(R.id.palette)
    //PaletteView mPaletteView;
    @BindViews ({R.id.clear, R.id.undo, R.id.redo, R.id.line_shape, R.id.eraser, R.id.move})
    List<Button> mBtnList;
    @BindView(R.id.frame)
    PaletteView mPaletteView;

    private int mColor = Color.BLACK;
    private float mStrokeWidth = 5f;
    private LineType mLineType = LineType.DRAW;
    private DrawMode mCurrDrawMode = DrawMode.EDIT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);
        mPaletteView.initDrawAreas();
    }

    private int count = 0;
    @OnClick({R.id.clear, R.id.undo, R.id.redo, R.id.line_shape, R.id.eraser, R.id.move})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.clear:
                mPaletteView.clear();
                break;
            case R.id.undo://撤销
                mPaletteView.undo();
                break;
            case R.id.redo://恢复
                mPaletteView.redo();
                break;
            case R.id.line_shape:
                mCurrDrawMode = DrawMode.EDIT;
                if (count == 0) {
                    //mPaletteView.setLineType(LineType.DRAW);
                    mBtnList.get(3).setText("默认");
                    mLineType = LineType.DRAW;
                } else if (count == 1) {
                    //mPaletteView.setLineType(LineType.LINE);
                    mBtnList.get(3).setText("直线");
                    mLineType = LineType.LINE;
                } else if(count == 2) {
                    //mPaletteView.setLineType(LineType.CIRCLE);
                    mBtnList.get(3).setText("圆形");
                    mLineType = LineType.CIRCLE;
                } else if(count == 3) {
                    //mPaletteView.setLineType(LineType.RECTANGLE);
                    mBtnList.get(3).setText("矩形");
                    mLineType = LineType.RECTANGLE;
                    count = 0;
                    break;
                }
                count ++;
                break;
            case R.id.eraser:
                //mPaletteView.setLineType(LineType.ERASER);
                mLineType = LineType.ERASER;
                Toast.makeText(getApplicationContext(), "eraser clicked", Toast.LENGTH_SHORT).show();
                break;
            case R.id.move:
                mCurrDrawMode = DrawMode.MOVE;
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e("home", "w = " + mPaletteView.getWidth() + "\nh = " + mPaletteView.getHeight());
            }
        }, 5000);
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
}