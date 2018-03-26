package com.step.smart.palette.view;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.blankj.utilcode.util.SizeUtils;
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
    @BindView(R.id.redo_img)
    ImageView mRedoImageView;
    @BindView(R.id.undo_img)
    ImageView mUndoImageView;
    @BindView(R.id.stroke_img)
    ImageView mStrokeImgView;

    private int mPenColor = Color.BLACK;
    private float mStrokeWidth = 5f;
    private LineType mLineType = LineType.DRAW;
    private DrawMode mCurrDrawMode = DrawMode.EDIT;
    private LineType mStrokeLineType = LineType.DRAW;
    private PopupWindow mPaintPopupWindow;

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
        initViews();
    }

    private void initViews() {
        initPaintPop();
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
                if (mCurrDrawMode != DrawMode.EDIT) {
                    mCurrDrawMode = DrawMode.EDIT;
                    mLineType = mStrokeLineType;
                    return;
                }
                showParamsPopupWindow(mStrokeView, 0);
                mLineType = mStrokeLineType;
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
        return mPenColor;
    }

    @Override
    public int getStrokeAlpha() {
        return 255;
    }

    @Override
    public void onUndoRedoCountChanged(int redo, int undo) {
        if (redo > 0) {
            mRedoImageView.setImageResource(R.drawable.redo);
        } else {
            mRedoImageView.setImageResource(R.drawable.redo_sel);
        }
        if (undo > 0) {
            mUndoImageView.setImageResource(R.drawable.undo);
        } else {
            mUndoImageView.setImageResource(R.drawable.undo_sel);
        }
    }

    private void releaseSelStatus() {
        mStrokeView.setBackgroundColor(Color.TRANSPARENT);
        mMoveView.setBackgroundColor(Color.TRANSPARENT);
        mEraserView.setBackgroundColor(Color.TRANSPARENT);
    }

    private void showParamsPopupWindow(View anchor, int type) {
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        //switch (type) {
            //case PAINT_POPUP:
                /*mPaintPopupWindow.showAtLocation(anchor,
                        Gravity.NO_GRAVITY, location[0] - mPaintPopupWindow.getWidth() / 2 + mPaintImageView.getWidth() / 2,
                        location[1] - mPaintPopupWindow.getHeight() - mPaintImageView.getHeight() / 2);*/
                mPaintPopupWindow.showAsDropDown(mStrokeView, -SizeUtils.dp2px(40), - SizeUtils.dp2px(5), Gravity.TOP);
                //break;


       // }
    }

    private ImageView mPaintWidthCircle;
    private SeekBar mPaintWidthSeekBar;
    private RadioGroup mPaintColorRG;

    private void initPaintPop() {
        if (mPaintPopupWindow != null) {
            return;
        }
        //画笔弹窗
        View v = LayoutInflater.from(this).inflate(R.layout.paint_popup_layout, (ViewGroup)null);
        //画笔弹窗布局
        //画笔大小
        mPaintWidthCircle = (ImageView) (v.findViewById(R.id.stroke_circle));
        mPaintWidthSeekBar = (SeekBar) (v.findViewById(R.id.stroke_seekbar));
        //画笔颜色
        mPaintColorRG = (RadioGroup) v.findViewById(R.id.stroke_color_radio_group);
        RadioGroup mPaintTypeRG = v.findViewById(R.id.stroke_type_radio_group);
        RadioButton strokeDraw = v.findViewById(R.id.stroke_type_rbtn_draw);
        RadioButton strokeLine = v.findViewById(R.id.stroke_type_rbtn_line);
        RadioButton strokeCircle = v.findViewById(R.id.stroke_type_rbtn_circle);
        RadioButton strokeRect = v.findViewById(R.id.stroke_type_rbtn_rectangle);
        //定义底部标签图片大小
        int px = SizeUtils.dp2px(25);
        Drawable drawableDraw = getResources().getDrawable(R.drawable.stroke_type_rbtn_draw);
        drawableDraw.setBounds(0, 0, px, px);//第一0是距左右边距离，第二0是距上下边距离，第三69长度,第四宽度
        strokeDraw.setCompoundDrawables(drawableDraw, null, null, null);//只放上面

        Drawable drawableLine = getResources().getDrawable(R.drawable.stroke_type_rbtn_line);
        drawableLine.setBounds(0, 0, px, px);
        strokeLine.setCompoundDrawables(drawableLine, null, null, null);

        Drawable drawableCircle = getResources().getDrawable(R.drawable.stroke_type_rbtn_circle);
        drawableCircle.setBounds(0, 0, px, px);
        strokeCircle.setCompoundDrawables(drawableCircle, null, null, null);

        Drawable drawableRect = getResources().getDrawable(R.drawable.stroke_type_rbtn_rectangle);
        drawableRect.setBounds(0, 0, px, px);
        strokeRect.setCompoundDrawables(drawableRect, null, null, null);
        mPaintPopupWindow = new PopupWindow(this);
        mPaintPopupWindow.setContentView(v);//设置主体布局
        mPaintPopupWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mPaintPopupWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.paint_popup_width));//宽度
        //mPaintPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);//高度自适应
        mPaintPopupWindow.setHeight(getResources().getDimensionPixelSize(R.dimen.paint_popup_height));//高度
        mPaintPopupWindow.setFocusable(true);
        mPaintPopupWindow.setBackgroundDrawable(new BitmapDrawable());//设置空白背景
        mPaintPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
            }
        });
        mPaintPopupWindow.setAnimationStyle(R.style.popwindow_anim_style);//动画
        mPaintColorRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Resources res = getResources();
                if (checkedId == R.id.stroke_color_black) {
                    mPenColor = Color.BLACK;
                } else if (checkedId == R.id.stroke_color_red) {
                    mPenColor = res.getColor(R.color.color_red_paint);
                } else if (checkedId == R.id.stroke_color_green) {
                    mPenColor = res.getColor(R.color.color_green_paint);
                } else if (checkedId == R.id.stroke_color_orange) {
                    mPenColor = res.getColor(R.color.color_yellow_paint);
                } else if (checkedId == R.id.stroke_color_blue) {
                    mPenColor = res.getColor(R.color.color_blue_paint);
                }
                mPaintPopupWindow.dismiss();
            }
        });

        mPaintTypeRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.stroke_type_rbtn_draw:
                        mStrokeLineType = LineType.DRAW;
                        mStrokeImgView.setBackgroundResource(R.drawable.pen);
                        break;
                    case R.id.stroke_type_rbtn_line:
                        mStrokeLineType = LineType.LINE;
                        mStrokeImgView.setBackgroundResource(R.drawable.line);
                        break;
                    case R.id.stroke_type_rbtn_circle:
                        mStrokeLineType = LineType.CIRCLE;
                        mStrokeImgView.setBackgroundResource(R.drawable.circle_line);
                        break;
                    case R.id.stroke_type_rbtn_rectangle:
                        mStrokeLineType = LineType.RECTANGLE;
                        mStrokeImgView.setBackgroundResource(R.drawable.rect);
                        break;
                }
                mLineType = mStrokeLineType;
                mPaintPopupWindow.dismiss();
            }
        });
        //画笔宽度拖动条
        mPaintWidthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {

            }
        });
        mPaintWidthSeekBar.setProgress(30);
    }
}