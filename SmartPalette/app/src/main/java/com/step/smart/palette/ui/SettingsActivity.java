package com.step.smart.palette.ui;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;

import com.blankj.utilcode.util.ScreenUtils;
import com.step.smart.palette.Constant.PreferenceConstant;
import com.step.smart.palette.R;
import com.step.smart.palette.utils.Preferences;

import butterknife.ButterKnife;

/**
 * Created by weflow on 2018/4/9.
 */

public class SettingsActivity extends BaseActivity {

    @Override
    protected int getContentViewRes() {
        return R.layout.activity_settings;
    }

    @Override
    protected void _init() {
        if (Preferences.getInt(PreferenceConstant.SCREEN_ORIENTATION, PreferenceConstant.SCREEN_PORT) == PreferenceConstant.SCREEN_PORT) {
            ScreenUtils.setPortrait(this);
        } else {
            ScreenUtils.setLandscape(this);
        }
    }
}
