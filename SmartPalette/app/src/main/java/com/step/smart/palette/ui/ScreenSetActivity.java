package com.step.smart.palette.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;

import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.step.smart.palette.Constant.PreferenceConstant;
import com.step.smart.palette.R;
import com.step.smart.palette.utils.Preferences;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by weflow on 2018/4/4.
 */

public class ScreenSetActivity extends BaseActivity {

    @BindView(R.id.screen_container)
    View mContainerView;
    @BindView(R.id.port_cb)
    CheckBox mPortraitCheckBox;
    @BindView(R.id.land_cb)
    CheckBox mLandCheckBox;
    @BindView(R.id.enter_text)
    TextView mEnterTextView;
    @BindView(R.id.agree)
    View mAgreeView;
    @BindView(R.id.agree_cb)
    CheckBox mAgreeCheckBox;
    @BindView(R.id.agree_tv)
    TextView mAgreeTextView;
    private Resources mResources;

    @Override
    protected int getContentViewRes() {
        return R.layout.activity_screen;
    }

    @Override
    protected void _init() {
        mResources = getResources();
        mEnterTextView.setTextColor(mResources.getColor(R.color.color_enter_disable));
        mPortraitCheckBox.setChecked(false);
        mLandCheckBox.setChecked(false);
        mAgreeCheckBox.setChecked(false);
        reSize();
    }

    @OnClick({R.id.port_l, R.id.land_l, R.id.enter, R.id.agree})
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.port_l:
                selectPortrait();
                break;
            case R.id.land_l:
                selectLand();
                break;
            case R.id.enter:
                enter();
                break;
            case R.id.agree:
                agree();
                break;
        }
    }

    private void selectPortrait() {
        if (mPortraitCheckBox.isChecked()) {
            return;
        }
        if (mLandCheckBox.isChecked()) {
            mLandCheckBox.setChecked(false);
        }
        mPortraitCheckBox.setChecked(true);
        Preferences.saveInt(PreferenceConstant.SCREEN_ORIENTATION, PreferenceConstant.SCREEN_PORT);
        mEnterTextView.setTextColor(mResources.getColor(R.color.color_enter_enable));
    }

    private void selectLand() {
        if (mLandCheckBox.isChecked()) {
            return;
        }
        if (mPortraitCheckBox.isChecked()) {
            mPortraitCheckBox.setChecked(false);
        }
        mLandCheckBox.setChecked(true);
        Preferences.saveInt(PreferenceConstant.SCREEN_ORIENTATION, PreferenceConstant.SCREEN_LAND);
        mEnterTextView.setTextColor(mResources.getColor(R.color.color_enter_enable));
    }

    private void enter() {
        if(!mPortraitCheckBox.isChecked() && !mLandCheckBox.isChecked()) {
            ToastUtils.showShort(R.string.screen_choose_tip);
            return;
        }
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void agree() {
        if (!mPortraitCheckBox.isChecked() && !mLandCheckBox.isChecked()) {
            ToastUtils.showShort(R.string.screen_choose_tip);
            return;
        }
        mAgreeCheckBox.setChecked(!mAgreeCheckBox.isChecked());
        Preferences.saveBoolean(PreferenceConstant.SCREEN_PAGE_NOT_SHOW_AGAIN, mAgreeCheckBox.isChecked());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        reSize();
    }

    private void reSize() {
        ViewGroup.LayoutParams params = mContainerView.getLayoutParams();
        params.width = (int)((ScreenUtils.getScreenWidth() * 2.0f) / 3.0f);
        mContainerView.setLayoutParams(params);
    }
}