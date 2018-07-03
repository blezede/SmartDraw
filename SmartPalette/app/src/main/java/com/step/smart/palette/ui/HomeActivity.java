package com.step.smart.palette.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.widget.AppCompatImageView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.SizeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.step.smart.palette.Constant.DrawMode;
import com.step.smart.palette.Constant.LineType;
import com.step.smart.palette.Constant.PreferenceConstant;
import com.step.smart.palette.R;
import com.step.smart.palette.message.MessageEvent;
import com.step.smart.palette.services.RecordService;
import com.step.smart.palette.utils.ColorsUtil;
import com.step.smart.palette.utils.Preferences;
import com.step.smart.palette.utils.ShareUtils;
import com.step.smart.palette.utils.SmartFileUtils;
import com.step.smart.palette.widget.PaletteView;
import com.step.smart.palette.widget.ShadowDrawable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import butterknife.BindView;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.PermissionUtils;
import permissions.dispatcher.RuntimePermissions;

import static com.step.smart.palette.services.RecordService.Helper.RECORD_CODE;

@RuntimePermissions
public class HomeActivity extends BaseActivity implements PaletteView.PaletteInterface, ColorChooserDialog.ColorCallback {

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
    AppCompatImageView mStrokeImgView;
    @BindView(R.id.menu)
    FloatingActionMenu mFloatingMenu;
    @BindView(R.id.fab1)
    FloatingActionButton mRecordFloatingBtn;
    @BindView(R.id.record_status)
    View mRecordView;
    @BindView(R.id.record_time)
    TextView mRecordTimeTextView;
    @BindView(R.id.choose_bg_btn)
    FloatingActionButton mColorFloatingBtn;
    @BindView(R.id.container)
    View mContainerView;
    @BindView(R.id.tool_lt)
    LinearLayout mLeftToolsView;

    private int mPenColor = Color.BLACK;
    private float mStrokeWidth = 5f;
    private int mStrokeAlpha = 255;
    private LineType mLineType = LineType.DRAW;
    private DrawMode mCurrDrawMode = DrawMode.EDIT;
    private LineType mStrokeLineType = LineType.DRAW;
    private PopupWindow mPaintPopupWindow, mEraserPopupWindow, mColorPopupWindow;
    private RecordService.Helper mHelper;
    private int mDefaultBgColor = Color.parseColor("#FAFAFA");
    private static final int REQUEST_CAMERA = 101;
    private static final int REQUEST_PICTURE = 102;
    private File mTmpFile;
    private ImageView mPaintWidthCircle, mPaintAlphaCircle;
    private SeekBar mPaintWidthSeekBar, mPaintAlphaSeekBar;
    private RadioGroup mPaintColorRG;
    private AppCompatImageView mMoreColorImg;

    @Override
    protected int getContentViewRes() {
        return R.layout.activity_home;
    }

    @Override
    protected void _init() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Preferences.getInt(PreferenceConstant.SCREEN_ORIENTATION, PreferenceConstant.SCREEN_PORT) == PreferenceConstant.SCREEN_PORT) {
            ScreenUtils.setPortrait(this);
        } else {
            ScreenUtils.setLandscape(this);
        }
        mPaletteView.initDrawAreas();
        initViews();
        mHelper = new RecordService.Helper(this, null);
        mHelper.bindService();
        EventBus.getDefault().register(this);
        HomeActivityPermissionsDispatcher.requestStoragePermissionsWithPermissionCheck(this);
        registerReceiver(mBroadcastReceiver, generateIntentFilter());
    }

    private IntentFilter generateIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        return filter;
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mHelper != null && mHelper.isRecording()) {
                stopRecord();
            }
        }
    };

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE})
    void requestStoragePermissions() {

    }

    @OnShowRationale({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE})
    void showRationaleForStorage(final PermissionRequest request) {
        new MaterialDialog.Builder(this)
                .content(R.string.storage_access)
                .canceledOnTouchOutside(false)
                .positiveText(R.string.agree)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        request.proceed();
                    }
                })
                .negativeText(R.string.disagree)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        request.cancel();
                    }
                })
                .show();
    }

    @OnPermissionDenied({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE})
    void showDeniedForStorage() {
    }

    @OnNeverAskAgain({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE})
    void showNeverAskForStorage() {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        HomeActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    private void initViews() {
        initPaintPop();
        initEraserPopup();
        initColorPop();
        mPaletteView.setBackgroundColor(mDefaultBgColor);
        flushStrokeColor();
        initMenu();

        ViewTreeObserver observer = mLinearLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mLinearLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                mLinearLayout.setBackground(new ShadowDrawable(mLinearLayout,
                        /*getResources().getColor(R.color.color_tools_bg)*/Color.WHITE));
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mRecordView.setBackground(new ShadowDrawable(mRecordView, getResources().getColor(R.color.color_record_bg)));
            }
        }, 2000);
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
                } else {
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

        mColorPopupWindow = new PopupWindow(this);
        mColorPopupWindow.setContentView(v);//设置主体布局
        mColorPopupWindow.setWidth(getResources().getDimensionPixelSize(R.dimen.dimen_230dp));//宽度
        //mPaintPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);//高度自适应
        mColorPopupWindow.setHeight(getResources().getDimensionPixelSize(R.dimen.dimen_230dp));//高度
        mColorPopupWindow.setFocusable(true);
        mColorPopupWindow.setBackgroundDrawable(new BitmapDrawable());//设置空白背景
        //mColorPopupWindow.setAnimationStyle(R.style.popwindow_anim_style);//动画
    }

    @OnClick({R.id.save, R.id.stroke, R.id.move, R.id.eraser, R.id.undo, R.id.redo, R.id.fab1, R.id.fab2, R.id.fab3, R.id.fab4, R.id.record_status, R.id.choose_bg_btn, R.id.choose_photo})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save:
                mPaletteView.exitPhotoMode(true, true);
                if (!PermissionUtils.hasSelfPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})) {
                    ToastUtils.showShort(R.string.need_storage_mission);
                    return;
                }
                if (mPaletteView.isEmpty()) {
                    ToastUtils.showShort(R.string.not_draw_yet);
                    return;
                }
                showSaveDialog();
                break;
            case R.id.undo://撤销
                mPaletteView.undo();
                break;
            case R.id.redo://恢复
                mPaletteView.redo();
                break;
            case R.id.stroke:
                mPaletteView.exitPhotoMode(true);
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
                mPaletteView.exitPhotoMode(true);
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
                mPaletteView.exitPhotoMode(true);
                releaseSelStatus();
                mMoveView.setBackgroundResource(R.drawable.btn_sel_bg);
                if (mCurrDrawMode != DrawMode.MOVE) {
                    mCurrDrawMode = DrawMode.MOVE;
                } else {
                    break;
                }
                break;
            case R.id.fab1://recording
                mFloatingMenu.close(true);
                if (!PermissionUtils.hasSelfPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})) {
                    ToastUtils.showShort(R.string.need_storage_mission);
                    return;
                }
                if (mHelper.isRecording()) {
                    ToastUtils.showShort(R.string.recording);
                    return;
                }
                mHelper.requestRecord();
                break;
            case R.id.fab2://preview
                mFloatingMenu.close(true);
                startActivity(new Intent(this, PreViewActivity.class));
                break;
            case R.id.fab3://settings
                mFloatingMenu.close(true);
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.fab4://share
                mFloatingMenu.close(true);
                if (!PermissionUtils.hasSelfPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})) {
                    ToastUtils.showShort(R.string.need_storage_mission);
                    return;
                }
                showShareDialog();
                break;
            case R.id.record_status:
                stopRecord();
                break;
            case R.id.choose_bg_btn:
                showColorChooseDialog();
                break;
            case R.id.choose_photo:
                int count = checkPicturesNum();
                if (count >= 10) {
                    ToastUtils.showShort(R.string.over_img_limit);
                    return;
                }
                pickPicture();
                break;
        }
    }

    private void stopRecord() {
        if (mRecordView != null) {
            mRecordView.setVisibility(View.INVISIBLE);
        }
        if (mHelper != null) {
            boolean result = mHelper.stopRecord();
            if (result) {
                ToastUtils.showShort(R.string.save_success);
                String videoPath = mHelper.getVideoPath();
                if (!TextUtils.isEmpty(videoPath)) {
                    ToastUtils.showLong(R.string.save_location, videoPath);
                }
            }
        }
        if (mRecordTimeTextView != null) {
            mRecordTimeTextView.setText(R.string.record_time_def);
        }
    }

    private void initMenu() {
        mFloatingMenu.setClosedOnTouchOutside(true);
        //mFloatingMenu.hideMenuButton(false);
        mFloatingMenu.setOnMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFloatingMenu.toggle(true);
            }
        });
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            mRecordFloatingBtn.showButtonInMenu(false);
        } else {
            mRecordFloatingBtn.hideButtonInMenu(false);
        }
        //flushBgIconColor();
        Resources res = getResources();
        if (res != null) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) mLeftToolsView.getLayoutParams();
            layoutParams.setMargins((int) res.getDimension(R.dimen.dimen_10dp), getStatusBarHeight(), 0, 0);
            mLeftToolsView.setLayoutParams(layoutParams);
        }
    }

    private int getStatusBarHeight() {
        int height = 0;
        Resources resources = this.getResources();
        int resourceId = resources.getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId != 0) {
            height = resources.getDimensionPixelSize(resourceId);
        } else {
            height = (int) resources.getDimension(R.dimen.dimen_24dp);
        }
        return height;
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mPaintPopupWindow.showAsDropDown(mStrokeView, -mStrokeView.getLeft(), -Math.abs((int) getResources().getDimension(R.dimen.dimen_10dp)) - Math.abs((int) getResources().getDimension(R.dimen.dimen_230dp)) - mStrokeView.getHeight(), Gravity.NO_GRAVITY);
                } else {
                    mPaintPopupWindow.showAtLocation(mContainerView, Gravity.NO_GRAVITY, location[0] - mStrokeView.getLeft(), location[1] - Math.abs((int) getResources().getDimension(R.dimen.dimen_10dp)) - Math.abs((int) getResources().getDimension(R.dimen.dimen_230dp)));
                }
                break;
            case 1:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mEraserPopupWindow.showAsDropDown(mEraserView, -mEraserPopupWindow.getWidth() / 2 + mEraserView.getWidth() / 2, -Math.abs((int) getResources().getDimension(R.dimen.dimen_10dp)) - mEraserPopupWindow.getHeight() - mEraserView.getHeight(), Gravity.NO_GRAVITY);
                } else {
                    mEraserPopupWindow.showAtLocation(mContainerView, Gravity.NO_GRAVITY, location[0] - mEraserPopupWindow.getWidth() / 2 + mEraserView.getWidth() / 2, location[1] - Math.abs((int) getResources().getDimension(R.dimen.dimen_10dp)) - mEraserPopupWindow.getHeight());
                }
                break;
            case 2:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mColorPopupWindow.showAsDropDown(mStrokeView, -mStrokeView.getLeft(), -Math.abs((int) getResources().getDimension(R.dimen.dimen_10dp)) - Math.abs((int) getResources().getDimension(R.dimen.dimen_230dp)) - mStrokeView.getHeight(), Gravity.NO_GRAVITY);
                } else {
                    mColorPopupWindow.showAtLocation(mContainerView, Gravity.NO_GRAVITY, location[0] - mStrokeView.getLeft(), location[1] - Math.abs((int) getResources().getDimension(R.dimen.dimen_10dp)) - Math.abs((int) getResources().getDimension(R.dimen.dimen_230dp)));
                }
                break;
        }
    }

    private void initPaintPop() {
        if (mPaintPopupWindow != null) {
            return;
        }
        //画笔弹窗
        View v = LayoutInflater.from(this).inflate(R.layout.paint_popup_layout, (ViewGroup) null);
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
        int size = (int) res.getDimension(R.dimen.dimen_25dp);
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
        mPaintPopupWindow.setWidth(res.getDimensionPixelSize(R.dimen.dimen_230dp));//宽度
        //mPaintPopupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);//高度自适应
        mPaintPopupWindow.setHeight(res.getDimensionPixelSize(R.dimen.dimen_230dp));//高度
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
                params.height = 6 + (int) ((16.0f / 100.0f) * progress);
                params.width = 6 + (int) ((16.0f / 100.0f) * progress);
                mPaintWidthCircle.requestLayout();
                mStrokeWidth = 3 + (int) ((16.0f / 100.0f) * progress);
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
        View v = LayoutInflater.from(this).inflate(R.layout.popup_eraser, (ViewGroup) null);
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
                vectorDrawableCompat = VectorDrawableCompat.create(getResources(), R.drawable.ic_pen, getTheme());
                break;
            case CIRCLE:
                vectorDrawableCompat = VectorDrawableCompat.create(getResources(), R.drawable.ic_circle, getTheme());
                break;
            case LINE:
                vectorDrawableCompat = VectorDrawableCompat.create(getResources(), R.drawable.ic_line, getTheme());
                break;
            case RECTANGLE:
                vectorDrawableCompat = VectorDrawableCompat.create(getResources(), R.drawable.ic_rect, getTheme());
                break;
        }
        if (vectorDrawableCompat != null) {
            vectorDrawableCompat.setTint(mPenColor);
            mStrokeImgView.setImageDrawable(vectorDrawableCompat);
        }
        flushMoreIconColor();
    }

    private void flushMoreIconColor() {
        VectorDrawableCompat vectorCompat = VectorDrawableCompat.create(getResources(), R.drawable.ic_more, getTheme());
        if (vectorCompat == null) {
            return;
        }
        vectorCompat.setTint(mPenColor);
        if (mMoreColorImg != null) {
            mMoreColorImg.setImageDrawable(vectorCompat);
        }
    }

    private void flushBgIconColor() {
        VectorDrawableCompat vectorCompat = VectorDrawableCompat.create(getResources(), R.drawable.ic_droplet, getTheme());
        if (vectorCompat == null) {
            return;
        }
        vectorCompat.setTint(Color.parseColor("#1781d2"));
        if (mColorFloatingBtn != null) {
            mColorFloatingBtn.setImageDrawable(vectorCompat);
        }
    }

    @Override
    public boolean isHighLighter() {
        return false;
    }

    @Override
    protected void onDestroy() {
        mHelper.unbindService();
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        unregisterReceiver(mBroadcastReceiver);
    }

    @SuppressLint("StaticFieldLeak")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        switch (event.what) {
            case MessageEvent.REQUEST_PREVIEW_BITMAP:
                new AsyncTask<Void, Void, Bitmap>() {

                    @Override
                    protected Bitmap doInBackground(Void... voids) {
                        return HomeActivity.this.getScreenShotBitmap();
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        MessageEvent msg = MessageEvent.obtain();
                        msg.what = MessageEvent.PREVIEW_BITMAP_RESULT;
                        msg.obj = bitmap;
                        EventBus.getDefault().post(msg);
                    }
                }.execute();
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RECORD_CODE:
                if (resultCode != RESULT_OK) {
                    return;
                }
                boolean result = mHelper.record(resultCode, data);
                mHelper.registerTimeCallback(new RecordService.TimeCallback() {
                    @Override
                    public void onTimeChange(String time) {
                        if (!TextUtils.isEmpty(time)) {
                            mRecordTimeTextView.setText(time);
                        }
                    }
                });
                if (result) {
                    mRecordView.setVisibility(View.VISIBLE);
                } else {
                    mRecordView.setVisibility(View.INVISIBLE);
                }
                mRecordTimeTextView.setText(R.string.record_time_def);
                break;
            case REQUEST_CAMERA:
                if (resultCode == RESULT_OK && mTmpFile.exists()) {
                    mLineType = LineType.PHOTO;
                    mPaletteView.addPhotoByPath(mTmpFile.getAbsolutePath());
                }
                break;
            case REQUEST_PICTURE:
                if (resultCode != RESULT_OK) {
                    return;
                }
                Uri uri = data.getData();
                String path = SmartFileUtils.getRealFileInfo(this, uri);
                if (!TextUtils.isEmpty(path)) {
                    mLineType = LineType.PHOTO;
                    mCurrDrawMode = DrawMode.PHOTO;
                    releaseStatus();
                    mPaletteView.addPhotoByPath(path);
                }
                break;
        }
    }

    public Bitmap getScreenShotBitmap() {
        if (mPaletteView == null) {
            return null;
        }
        return mPaletteView.screenShotBitmap(true);
    }

    @Override
    public void onBackPressed() {
        if (mFloatingMenu.isOpened()) {
            mFloatingMenu.close(true);
            return;
        }
        //super.onBackPressed();
        showExitDialog();
    }

    private void showSaveDialog() {
        if (mPaletteView.isEmpty()) {
            return;
        }
        new MaterialDialog.Builder(this)
                .title(R.string.save_type)
                .items(R.array.save_sel)
                //.itemsDisabledIndices(1)
                .itemsCallbackSingleChoice(
                        0, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                if (which == 0) {
                                    saveRecordImg(false);
                                } else if (which == 1) {
                                    saveRecordImg(true);
                                }
                                return true;
                            }
                        })
                .positiveText(R.string.confirm)
                .show();
    }

    private void showColorChooseDialog() {
        new ColorChooserDialog.Builder(this, R.string.palette_bg_color)
                .titleSub(R.string.palette_bg_colors)
                .preselect(mDefaultBgColor)
                .allowUserColorInput(false)
                .backButton(R.string.back)
                .doneButton(R.string.done)
                .cancelButton(R.string.cancel)
                .build()
                .show(this.getSupportFragmentManager());
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, int selectedColor) {
        mPaletteView.setBackgroundColor(selectedColor);
        mDefaultBgColor = selectedColor;
        //flushBgIconColor();
    }

    @Override
    public void onColorChooserDismissed(@NonNull ColorChooserDialog dialog) {

    }

    private MaterialDialog mProgressDislog;

    private void showProgressDialog(@StringRes int stringId) {
        mProgressDislog = new MaterialDialog.Builder(this)
                //.title(R.string.progress_dialog)
                .canceledOnTouchOutside(false)
                .content(stringId)
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .show();
    }

    private void dismissProgressDialog() {
        if (mProgressDislog != null) {
            mProgressDislog.dismiss();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void saveRecordImg(final boolean wholeCanvas) {
        showProgressDialog(R.string.saving);
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                if (mPaletteView == null) {
                    return "";
                }
                return mPaletteView.screenShot(wholeCanvas);
            }

            @Override
            protected void onPostExecute(String aVoid) {
                dismissProgressDialog();
                if (TextUtils.isEmpty(aVoid)) {
                    ToastUtils.showShort(R.string.save_failed);
                } else {
                    ToastUtils.showShort(R.string.save_success);
                    ToastUtils.showLong(R.string.save_location, aVoid);
                }
            }
        }.execute();
    }

    private void showExitDialog() {
        new MaterialDialog.Builder(this)
                .content(R.string.exit_tip)
                .positiveText(R.string.confirm)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        finish();
                    }
                })
                .negativeText(R.string.cancel)
                .show();
    }

    private void showShareDialog() {
        new MaterialDialog.Builder(this)
                .title(R.string.share_type)
                .items(R.array.share_sel)
                //.itemsDisabledIndices(1)
                .itemsCallbackSingleChoice(
                        0, new MaterialDialog.ListCallbackSingleChoice() {
                            @Override
                            public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                if (which == 0) {
                                    shareRecordImg(false);
                                } else if (which == 1) {
                                    shareRecordImg(true);
                                }
                                return true;
                            }
                        })
                .positiveText(R.string.confirm)
                .show();
    }

    @SuppressLint("StaticFieldLeak")
    private void shareRecordImg(final boolean wholeCanvas) {
        showProgressDialog(R.string.generating);
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                if (mPaletteView == null) {
                    return "";
                }
                return mPaletteView.screenShot(wholeCanvas, true);
            }

            @Override
            protected void onPostExecute(String aVoid) {
                dismissProgressDialog();
                if (TextUtils.isEmpty(aVoid)) {
                    ToastUtils.showShort(R.string.generate_failed);
                } else {
                    ShareUtils.shareFile(HomeActivity.this, aVoid);
                }
            }
        }.execute();
    }

    @Override
    public void onPhotoTypeExited(boolean byUser) {
        mLineType = mStrokeLineType;
        mCurrDrawMode = DrawMode.NONE;
        if (byUser) {
            mCurrDrawMode = DrawMode.EDIT;
            releaseSelStatus();
            flushStrokeColor();
            mStrokeView.setBackgroundResource(R.drawable.btn_sel_bg);
        }

    }

    private void pickPicture() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(intent, getString(R.string.choose_photo)), REQUEST_PICTURE);
    }

    private void releaseStatus() {
        mStrokeView.setBackgroundColor(Color.TRANSPARENT);
        mMoveView.setBackgroundColor(Color.TRANSPARENT);
        mEraserView.setBackgroundColor(Color.TRANSPARENT);
    }

    private int checkPicturesNum() {
        if (mPaletteView == null) {
            return 0;
        }
        return mPaletteView.getPicturesCount();
    }
}