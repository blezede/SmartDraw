package com.step.smart.palette.view;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import com.blankj.utilcode.util.ScreenUtils;
import com.step.smart.palette.Constant.DrawMode;
import com.step.smart.palette.Constant.LineType;
import com.step.smart.palette.R;
import com.step.smart.palette.widget.PaletteFrameLayout;
import com.step.smart.palette.widget.PaletteView;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class HomeActivity extends AppCompatActivity {

    //@BindView(R.id.palette)
    //PaletteView mPaletteView;
    @BindViews ({R.id.clear, R.id.undo, R.id.redo, R.id.line_shape, R.id.eraser})
    List<Button> mBtnList;
    @BindView(R.id.frame)
    PaletteFrameLayout mPaletteFrameLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);
        mPaletteFrameLayout.initDrawAreas();
    }

    private int count = 0;
    @OnClick({R.id.clear, R.id.undo, R.id.redo, R.id.line_shape, R.id.eraser})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.clear:
                //mPaletteView.clear();
                break;
            case R.id.undo://撤销
                //mPaletteView.undo();
                break;
            case R.id.redo://恢复
                //mPaletteView.redo();
                break;
            case R.id.line_shape:
                if (count == 0) {
                    //mPaletteView.setLineType(LineType.DRAW);
                    mBtnList.get(3).setText("默认");
                } else if (count == 1) {
                    //mPaletteView.setLineType(LineType.LINE);
                    mBtnList.get(3).setText("直线");
                } else if(count == 2) {
                    //mPaletteView.setLineType(LineType.CIRCLE);
                    mBtnList.get(3).setText("圆形");
                } else if(count == 3) {
                    //mPaletteView.setLineType(LineType.RECTANGLE);
                    mBtnList.get(3).setText("矩形");
                    count = 0;
                    break;
                }
                count ++;
                break;
            case R.id.eraser:
                //mPaletteView.setLineType(LineType.ERASER);
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e("home", "w = " + mPaletteFrameLayout.getWidth() + "\nh = " + mPaletteFrameLayout.getHeight());
            }
        }, 5000);
    }
}