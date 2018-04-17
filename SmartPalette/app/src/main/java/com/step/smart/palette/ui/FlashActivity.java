package com.step.smart.palette.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.widget.AppCompatImageView;
import android.widget.TextView;

import com.step.smart.palette.Constant.PreferenceConstant;
import com.step.smart.palette.R;
import com.step.smart.palette.utils.Preferences;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;

/**
 * Created by weflow on 2018/4/4.
 */

public class FlashActivity extends BaseActivity {

    @BindView(R.id.time_tv)
    TextView mStartTextView;
    private Timer mTimer;
    private int mTime = 3;
    @BindView(R.id.flash_icon)
    AppCompatImageView mFlashIconView;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        }
    };

    @Override
    protected int getContentViewRes() {
        return R.layout.activity_flash;
    }

    @Override
    protected void _init() {
        timer();
        typefaceSetting();
        //flushIconView();
    }

    private void typefaceSetting() {
        Typeface typeface = Typeface.createFromAsset(getAssets(), "FZTJLSJW.TTF");
        if (typeface == null) {
            return;
        }
        mStartTextView.setTypeface(typeface);
    }

    private void flushIconView() {
        VectorDrawableCompat vectorCompat = VectorDrawableCompat.create(getResources(), R.drawable.ic_launcher_foreground, getTheme());
        if (vectorCompat == null) {
            return;
        }
        vectorCompat.setTint(Color.BLACK);
        mFlashIconView.setImageDrawable(vectorCompat);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onBackPressed() {
    }

    private void timer() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mTime--;
                if (mTime <= 0) {
                    cancelTimer();
                    mHandler.post(mEnterNextRunnable);
                    return;
                }
            }
        }, new Date(), 1000);
    }

    private void cancelTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    private Runnable mEnterNextRunnable = new Runnable() {
        @Override
        public void run() {
            boolean aBoolean = Preferences.getBoolean(PreferenceConstant.SCREEN_PAGE_NOT_SHOW_AGAIN, false);
            if (aBoolean) {
                enterHomeAct();
            } else {
                enterScreenAct();
            }
        }
    };


    private void enterHomeAct() {
        Intent in = new Intent(this, HomeActivity.class);
        startActivity(in);
        finish();
    }

    private void enterScreenAct() {
        Intent in = new Intent(this, ScreenSetActivity.class);
        startActivity(in);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
    }
}
