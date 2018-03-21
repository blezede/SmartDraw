package com.step.smart.palette;

import android.app.Application;

import com.blankj.utilcode.util.Utils;

/**
 * Created by weflow on 2018/3/21.
 */

public class SmartApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
    }
}
