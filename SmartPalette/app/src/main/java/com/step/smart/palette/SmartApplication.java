package com.step.smart.palette;

import android.app.Application;

import com.blankj.utilcode.util.Utils;
import com.umeng.commonsdk.UMConfigure;

/**
 * Created by weflow on 2018/3/21.
 */

public class SmartApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.init(this);
        initUmeng();
    }

    /**
     * 初始化common库
     * 参数1:上下文，不能为空
     * 参数2:【友盟+】 AppKey
     * 参数3:【友盟+】 Channel
     * 参数4:设备类型，UMConfigure.DEVICE_TYPE_PHONE为手机、UMConfigure.DEVICE_TYPE_BOX为盒子，默认为手机
     * 参数5:Push推送业务的secret
     */
    private void initUmeng() {
        UMConfigure.init(this, UMConfigure.DEVICE_TYPE_PHONE, null);
    }
}
