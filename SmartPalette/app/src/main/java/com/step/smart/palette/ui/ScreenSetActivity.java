package com.step.smart.palette.ui;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;

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

public class ScreenSetActivity extends AppCompatActivity {

    @BindView(R.id.port_cb)
    CheckBox mPortraitCheckBox;
    @BindView(R.id.land_cb)
    CheckBox mLandCheckBox;
    @BindView(R.id.enter_text)
    TextView mEnterTextView;
    private Resources mResources;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen);
        ButterKnife.bind(this);
        _init();
    }

    private void _init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        mResources = getResources();
        mEnterTextView.setTextColor(mResources.getColor(R.color.color_enter_disable));
        mPortraitCheckBox.setChecked(false);
        mLandCheckBox.setChecked(false);
    }

    @OnClick({R.id.port_l, R.id.land_l, R.id.enter})
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

}
