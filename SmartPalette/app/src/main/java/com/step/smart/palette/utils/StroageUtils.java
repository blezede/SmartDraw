package com.step.smart.palette.utils;

import android.os.Environment;
import android.os.StatFs;

/**
 * Created by weflow on 2018/4/8.
 */

public class StroageUtils {

    public final static long K = 1024;
    public final static long M = 1024 * 1024;

    public static long getResidualSpace(String directoryPath) {
        try {
            StatFs sf = new StatFs(directoryPath);
            long blockSize = sf.getBlockSize();
            long availCount = sf.getAvailableBlocks();
            long availCountByte = availCount * blockSize;
            return availCountByte;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean hasEnoughSpaceForWrite(long size) {
        long residualSpace = getResidualSpace(Environment.getExternalStorageDirectory().getAbsolutePath());
        return residualSpace > size;
    }
}
