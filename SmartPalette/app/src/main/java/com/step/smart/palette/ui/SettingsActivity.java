package com.step.smart.palette.ui;

import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import com.blankj.utilcode.util.ScreenUtils;
import com.kyleduo.switchbutton.SwitchButton;
import com.step.smart.palette.Constant.PreferenceConstant;
import com.step.smart.palette.R;
import com.step.smart.palette.utils.Preferences;
import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by weflow on 2018/4/9.
 */

public class SettingsActivity extends BaseActivity {

    @BindView(R.id.container)
    LinearLayout mContainerView;
    @BindView(R.id.screen_set)
    View mScreenSetView;
    @BindView(R.id.switch_btn)
    SwitchButton mScreenSwitch;

    @Override
    protected int getContentViewRes() {
        return R.layout.activity_settings;
    }

    @Override
    protected void _init() {
        initViews();
    }

    private void initViews() {
        ViewGroup.LayoutParams params = mContainerView.getLayoutParams();
        params.width = (int) (ScreenUtils.getScreenWidth() * 0.8f);
        params.height = (int) (ScreenUtils.getScreenHeight() * 0.8f);
        mContainerView.setLayoutParams(params);
        mScreenSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Preferences.saveBoolean(PreferenceConstant.SCREEN_PAGE_NOT_SHOW_AGAIN, !isChecked);
            }
        });
        mScreenSwitch.setChecked(!Preferences.getBoolean(PreferenceConstant.SCREEN_PAGE_NOT_SHOW_AGAIN, false));
    }

    @OnClick({R.id.close, R.id.screen_set})
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.close:
                finish();
                break;
            case R.id.screen_set:
                mScreenSwitch.setChecked(!mScreenSwitch.isChecked());
                break;
        }
    }
}
