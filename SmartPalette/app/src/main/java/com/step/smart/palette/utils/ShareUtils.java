package com.step.smart.palette.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;

import com.blankj.utilcode.util.FileUtils;
import com.step.smart.palette.R;

import java.io.File;

/**
 * Created by max on 2018/4/13.
 */

public class ShareUtils {

    public static void shareFile(Activity c, String filePath) {
        if (c == null) {
            return;
        }
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        File file = new File(filePath);
        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(c, c.getPackageName() + ".provider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setData(uri);
        intent.setType("image/png");
        c.startActivity(Intent.createChooser(intent, c.getString(R.string.menu_share)));
    }
}
