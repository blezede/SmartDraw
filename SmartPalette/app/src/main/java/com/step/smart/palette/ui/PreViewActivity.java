package com.step.smart.palette.ui;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.SizeUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.github.chrisbanes.photoview.PhotoView;
import com.step.smart.palette.Constant.PreferenceConstant;
import com.step.smart.palette.R;
import com.step.smart.palette.message.MessageEvent;
import com.step.smart.palette.utils.Preferences;
import com.wang.avi.AVLoadingIndicatorView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by max on 2018/4/9.
 */

public class PreViewActivity extends BaseActivity {

    @BindView(R.id.pre_container)
    FrameLayout mFrameContainer;
    @BindView(R.id.pre_photo)
    PhotoView mPhotoView;
    @BindView(R.id.indicator)
    AVLoadingIndicatorView mAVLoadingIndicatorView;
    private final static float mScale = 0.7f;

    @Override
    protected int getContentViewRes() {
        return R.layout.activity_preview;
    }

    @Override
    protected void _init() {
        initViews();
        prepare();
        EventBus.getDefault().register(this);
    }

    private void initViews() {
        ViewGroup.LayoutParams params = mFrameContainer.getLayoutParams();
        params.width = (int) (ScreenUtils.getScreenWidth() * mScale);
        params.height = (int) (ScreenUtils.getScreenHeight() * mScale);
        mFrameContainer.setLayoutParams(params);
        ViewGroup.LayoutParams layoutParams = mPhotoView.getLayoutParams();
        layoutParams.width = (int) (ScreenUtils.getScreenWidth() * mScale * 0.95);
        layoutParams.height = (int) (ScreenUtils.getScreenHeight() * mScale * 0.95);
        mPhotoView.setLayoutParams(layoutParams);
    }

    @OnClick({R.id.close})
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.close:
                finish();
                break;
        }
    }

    private void prepare() {
        mAVLoadingIndicatorView.show();
        MessageEvent msg = MessageEvent.obtain();
        msg.what = MessageEvent.REQUEST_PREVIEW_BITMAP;
        EventBus.getDefault().post(msg);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        switch (event.what) {
            case MessageEvent.PREVIEW_BITMAP_RESULT:
                Bitmap bitmap = (Bitmap) event.obj;
                if (bitmap == null) {
                    ToastUtils.showShort(R.string.generate_failed);
                    finish();
                } else {
                    mAVLoadingIndicatorView.hide();
                    mPhotoView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
                }
                break;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
