package com.step.smart.palette.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.blankj.utilcode.util.AppUtils;
import com.blankj.utilcode.util.Utils;

public class Preferences {

    public static boolean getBoolean(String key, boolean value) {
        return getSharedPreferences().getBoolean(key, value);
    }

    public static void saveBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static void saveString(String key, String value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String getString(String key) {
        return getSharedPreferences().getString(key, null);
    }

    public static void saveInt(String key, int value) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static int getInt(String key, int value) {
        return getSharedPreferences().getInt(key, value);
    }

    static SharedPreferences getSharedPreferences() {
        return Utils.getApp().getSharedPreferences(AppUtils.getAppPackageName() + "_SharedPreferences", Context.MODE_PRIVATE);
    }

}
