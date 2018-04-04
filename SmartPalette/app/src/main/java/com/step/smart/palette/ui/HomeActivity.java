package com.step.smart.palette.ui;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.SizeUtils;
import com.step.smart.palette.Constant.DrawMode;
import com.step.smart.palette.Constant.LineType;
import com.step.smart.palette.Constant.PreferenceConstant;
import com.step.smart.palette.R;
import com.step.smart.palette.utils.ColorsUtil;
import com.step.smart.palette.utils.Preferences;
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
    private int mStrokeAlpha = 255;
    private LineType mLineType = LineType.DRAW;
    private DrawMode mCurrDrawMode = DrawMode.EDIT;
    private LineType mStrokeLineType = LineType.DRAW;
    private PopupWindow mPaintPopupWindow, mEraserPopupWindow, mColorPopupWindow;

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Preferences.getInt(PreferenceConstant.SCREEN_ORIENTATION, PreferenceConstant.SCREEN_PORT) == PreferenceConstant.SCREEN_PORT) {
            ScreenUtils.setPortrait(this);
        } else {
            ScreenUtils.setLandscape(this);
        }
        mPaletteView.initDrawAreas();
        initViews();
    }

    private void initViews() {
        initPaintPop();
        initEraserPopup();
        initColorPop();
        mPaletteView.setBackgroundColor(Color.parseColor("#EEEEEE"));
        flushStrokeColor();
    }

    private void initColorPop() {
        if (mColorPopupWindow != null) {
            return;
        }
        //橡皮擦
        View v = LayoutInflater.from(this).inflate(R.layout.popup_colors, null);

        GridView grid = v.findViewById(R.id.grid);
        grid.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return ColorsUtil.sColorValues.length;
            }

            @Override
            public Object getItem(int position) {
                return ColorsUtil.sColorValues[position];
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;
                if (convertView == null) {
                    convertView = LayoutInflater.from(HomeActivity.this).inflate(R.layout.color_item, null);
                    holder = new ViewHolder();
                    holder.color = convertView.findViewById(R.id.color);
                    convertView.setTag(holder);
                }
                else {
                    holder = (ViewHolder) convertView.getTag();
                }
                holder.color.setBackgroundColor(Color.parseColor(ColorsUtil.sColorValues[position]));
                return convertView;
            }

            final class ViewHolder {
                public View color;
            }

        });

        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                mPaintColorRG.clearCheck();
                if (mColorPopupWindow != null) {
                    mColorPopupWindow.dismiss();
                }
                new Handler(getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mPenColor = Color.parseColor(ColorsUtil.sColorValues[position]);
                        flushStrokeColor();
                    }
                }, 100);
            }
        });

        //橡皮擦弹窗
        mColorPopupWindow = new PopupWindow(this);
        mColorPopupWindow.setContentView(v);//设置主体布局
        mColorPopupWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.paint_popup_width));//宽度
        //mPaintPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);//高度自适应
        mColorPopupWindow.setHeight(getResources().getDimensionPixelSize(R.dimen.paint_popup_height));//高度
        mColorPopupWindow.setFocusable(true);
        mColorPopupWindow.setBackgroundDrawable(new BitmapDrawable());//设置空白背景
        //mColorPopupWindow.setAnimationStyle(R.style.popwindow_anim_style);//动画
    }

    @OnClick({R.id.save, R.id.stroke, R.id.move, R.id.eraser, R.id.undo, R.id.redo})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save:
                if (mPaletteView.isEmpty()) {
                    return;
                }
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... voids) {
                        mPaletteView.screenShot(true);
                        return null;
                    }
                }.execute();
                break;
            case R.id.undo://撤销
                mPaletteView.undo();
                break;
            case R.id.redo://恢复
                mPaletteView.redo();
                break;
            case R.id.stroke:
                releaseSelStatus();
                flushStrokeColor();
                mStrokeView.setBackgroundResource(R.drawable.btn_sel_bg);
                mLineType = mStrokeLineType;
                if (mCurrDrawMode != DrawMode.EDIT) {
                    mCurrDrawMode = DrawMode.EDIT;
                    return;
                }
                showParamsPopupWindow(mStrokeView, 0);
                break;
            case R.id.eraser:
                releaseSelStatus();
                mEraserView.setBackgroundResource(R.drawable.btn_sel_bg);
                mLineType = LineType.ERASER;
                if (mCurrDrawMode != DrawMode.ERASER) {
                    mCurrDrawMode = DrawMode.ERASER;
                    return;
                }
                showParamsPopupWindow(mEraserView, 1);
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
        return mStrokeAlpha;
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
        switch (type) {
            case 0:
                /*mPaintPopupWindow.showAtLocation(anchor,
                        Gravity.NO_GRAVITY, location[0] - mPaintPopupWindow.getWidth() / 2 + mPaintImageView.getWidth() / 2,
                        location[1] - mPaintPopupWindow.getHeight() - mPaintImageView.getHeight() / 2);*/

                mPaintPopupWindow.showAsDropDown(mStrokeView, - mStrokeView.getLeft(), - (int)getResources().getDimension(R.dimen.paint_popup_deliver), Gravity.TOP);
                break;
            case 1:
                mEraserPopupWindow.showAsDropDown(mEraserView, - mEraserPopupWindow.getWidth() / 2 + mEraserView.getWidth() / 2, - (int)getResources().getDimension(R.dimen.paint_popup_deliver), Gravity.TOP);
                break;
            case 2:
                //mColorPopupWindow.showAsDropDown(mStrokeView, -SizeUtils.dp2px(40), - SizeUtils.dp2px(5), Gravity.TOP);
                mColorPopupWindow.showAsDropDown(mStrokeView, - mStrokeView.getLeft(), - (int)getResources().getDimension(R.dimen.paint_popup_deliver), Gravity.TOP);
                break;

        }
    }

    private ImageView mPaintWidthCircle, mPaintAlphaCircle, mMoreColorImg;
    private SeekBar mPaintWidthSeekBar, mPaintAlphaSeekBar;
    private RadioGroup mPaintColorRG;

    private void initPaintPop() {
        if (mPaintPopupWindow != null) {
            return;
        }
        //画笔弹窗
        View v = LayoutInflater.from(this).inflate(R.layout.paint_popup_layout, (ViewGroup)null);
        //画笔弹窗布局
        //画笔大小
        mPaintWidthCircle = v.findViewById(R.id.stroke_circle);
        mPaintWidthSeekBar = v.findViewById(R.id.stroke_seekbar);
        //画笔颜色
        mPaintColorRG = v.findViewById(R.id.stroke_color_radio_group);
        RadioGroup mPaintTypeRG = v.findViewById(R.id.stroke_type_radio_group);
        RadioButton strokeDraw = v.findViewById(R.id.stroke_type_rbtn_draw);
        RadioButton strokeLine = v.findViewById(R.id.stroke_type_rbtn_line);
        RadioButton strokeCircle = v.findViewById(R.id.stroke_type_rbtn_circle);
        RadioButton strokeRect = v.findViewById(R.id.stroke_type_rbtn_rectangle);
        View colorView = v.findViewById(R.id.color_sel);
        mMoreColorImg = v.findViewById(R.id.more_color_icon);
        colorView.setOnClickListener(mOnClickListener);

        mPaintAlphaCircle = v.findViewById(R.id.stroke_alpha_circle);
        mPaintAlphaSeekBar = v.findViewById(R.id.stroke_alpha_seekbar);

        //定义底部标签图片大小
        Resources res = getResources();
        int size = (int) res.getDimension(R.dimen.paint_col_img_size);
        Drawable drawableDraw = res.getDrawable(R.drawable.stroke_type_rbtn_draw);
        drawableDraw.setBounds(0, 0, size, size);//第一0是距左右边距离，第二0是距上下边距离，第三69长度,第四宽度
        strokeDraw.setCompoundDrawables(drawableDraw, null, null, null);//只放上面

        Drawable drawableLine = res.getDrawable(R.drawable.stroke_type_rbtn_line);
        drawableLine.setBounds(0, 0, size, size);
        strokeLine.setCompoundDrawables(drawableLine, null, null, null);

        Drawable drawableCircle = res.getDrawable(R.drawable.stroke_type_rbtn_circle);
        drawableCircle.setBounds(0, 0, size, size);
        strokeCircle.setCompoundDrawables(drawableCircle, null, null, null);

        Drawable drawableRect = res.getDrawable(R.drawable.stroke_type_rbtn_rectangle);
        drawableRect.setBounds(0, 0, size, size);
        strokeRect.setCompoundDrawables(drawableRect, null, null, null);
        mPaintPopupWindow = new PopupWindow(this);
        mPaintPopupWindow.setContentView(v);//设置主体布局
        mPaintPopupWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mPaintPopupWindow.setWidth(res.getDimensionPixelSize(R.dimen.paint_popup_width));//宽度
        //mPaintPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);//高度自适应
        mPaintPopupWindow.setHeight(res.getDimensionPixelSize(R.dimen.paint_popup_height));//高度
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
                flushStrokeColor();
            }
        });

        mPaintTypeRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.stroke_type_rbtn_draw:
                        mStrokeLineType = LineType.DRAW;
                        break;
                    case R.id.stroke_type_rbtn_line:
                        mStrokeLineType = LineType.LINE;
                        break;
                    case R.id.stroke_type_rbtn_circle:
                        mStrokeLineType = LineType.CIRCLE;
                        break;
                    case R.id.stroke_type_rbtn_rectangle:
                        mStrokeLineType = LineType.RECTANGLE;
                        break;
                }
                mLineType = mStrokeLineType;
                mPaintPopupWindow.dismiss();
                flushStrokeColor();
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
                ViewGroup.LayoutParams params = mPaintWidthCircle.getLayoutParams();
                params.height = 6 + (int)((16.0f / 100.0f) * progress);
                params.width = 6 + (int)((16.0f / 100.0f) * progress);
                mPaintWidthCircle.requestLayout();
                mStrokeWidth = 3 + (int)((16.0f / 100.0f) * progress);
            }
        });
        mPaintWidthSeekBar.setProgress(15);

        mPaintAlphaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float alpha = 0.3f + ((progress * 1f) / 100.0f) * 0.7f;
                mPaintAlphaCircle.setAlpha(alpha);
                mStrokeAlpha = (int) (255 * alpha);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        mPaintAlphaSeekBar.setProgress(100);
    }

    private void initEraserPopup() {
        if (mEraserPopupWindow != null) {
            return;
        }
        //橡皮擦
        View v = LayoutInflater.from(this).inflate(R.layout.popup_eraser, null);
        View eraser = v.findViewById(R.id.rl_eraser);
        View clean = v.findViewById(R.id.rl_clean);
        eraser.setOnClickListener(mOnClickListener);
        clean.setOnClickListener(mOnClickListener);
        //橡皮擦弹窗
        mEraserPopupWindow = new PopupWindow(this);
        mEraserPopupWindow.setContentView(v);//设置主体布局
        mEraserPopupWindow.setWidth(SizeUtils.dp2px(100f));//宽度200dp
//        eraserPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);//高度自适应
        mEraserPopupWindow.setHeight(SizeUtils.dp2px(55f));//高度自适应
        mEraserPopupWindow.setFocusable(true);
        mEraserPopupWindow.setBackgroundDrawable(new BitmapDrawable());//设置空白背景
        mEraserPopupWindow.setAnimationStyle(R.style.popwindow_anim_style);//动画
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.rl_eraser:
                    mEraserPopupWindow.dismiss();
                    break;
                case R.id.rl_clean:
                    mPaletteView.clear();
                    mEraserPopupWindow.dismiss();
                    break;
                case R.id.color_sel:
                    if (mPaintPopupWindow != null) {
                        mPaintPopupWindow.dismiss();
                    }
                    showParamsPopupWindow(mStrokeView, 2);
                    break;
            }
        }
    };

    private void flushStrokeColor() {
        VectorDrawableCompat vectorDrawableCompat = null;
        switch (mStrokeLineType) {
            case DRAW:
                vectorDrawableCompat = VectorDrawableCompat.create(getResources(),R.drawable.ic_pen,getTheme());
                break;
            case CIRCLE:
                vectorDrawableCompat = VectorDrawableCompat.create(getResources(),R.drawable.ic_circle,getTheme());
                break;
            case LINE:
                vectorDrawableCompat = VectorDrawableCompat.create(getResources(),R.drawable.ic_line,getTheme());
                break;
            case RECTANGLE:
                vectorDrawableCompat = VectorDrawableCompat.create(getResources(),R.drawable.ic_rect,getTheme());
                break;
        }
        if (vectorDrawableCompat != null) {
            vectorDrawableCompat.setTint(mPenColor);
            mStrokeImgView.setImageDrawable(vectorDrawableCompat);
        }
        flushMoreIconColor();
    }

    private void flushMoreIconColor() {
        VectorDrawableCompat vectorCompat = VectorDrawableCompat.create(getResources(),R.drawable.ic_more,getTheme());
        vectorCompat.setTint(mPenColor);
        if (mMoreColorImg != null) {
            mMoreColorImg.setImageDrawable(vectorCompat);
        }
    }

    @Override
    public boolean isHighLighter() {
        return false;
    }
}