package com.step.smart.palette.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created  by  weflow  on  2017/12/26.
 */

public class SmartFileUtils {

    /**
     * Try to return the absolute file path from the given Uri
     *
     * @param context
     * @param uri
     * @return the file path or null
     */
    public static String getRealFileInfo(final Context context, final Uri uri) {
        if (null == uri) return "";
        final String scheme = uri.getScheme();
        String data = "";
        if (scheme == null) {
            data = uri.getPath();
            if (isFileExist(data)) {
                return data;
            }
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
            if (isFileExist(data)) {
                return data;
            }
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, FILE_PROJECTION, null, null, null);
                if (null != cursor) {
                    if (cursor.moveToFirst()) {
                        String path = cursor.getString(cursor.getColumnIndexOrThrow(FILE_PROJECTION[0]));
                        String name = cursor.getString(cursor.getColumnIndexOrThrow(FILE_PROJECTION[1]));
                        long size = cursor.getLong(cursor.getColumnIndexOrThrow(FILE_PROJECTION[2]));
                        if (isFileExist(path)) {
                            return path;
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                return "";
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return "";
    }

    private static final String[] FILE_PROJECTION = {
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns._ID};

    public static void getSpecifiedTypeFiles(Context context, String[] extension) {
        //从外存中获取
        Uri fileUri = MediaStore.Files.getContentUri("external");
        //筛选列，这里只筛选了：文件路径和不含后缀的文件名
        String[] projection = new String[]{
                MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.TITLE
        };
        //构造筛选语句
        String selection = "";
        for (int i = 0; i < extension.length; i++) {
            if (i != 0) {
                selection = selection + "  OR  ";
            }
            selection = selection + MediaStore.Files.FileColumns.DATA + "  LIKE  '%" + extension[i] + "'";
        }
        //按时间递增顺序对结果进行排序;待会从后往前移动游标就可实现时间递减
        String sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED;
        //获取内容解析器对象
        ContentResolver resolver = context.getContentResolver();
        //获取游标
        Cursor cursor = resolver.query(fileUri, projection, selection, null, sortOrder);
        if (cursor == null)
            return;
        //游标从最后开始往前递减，以此实现时间递减顺序（最近访问的文件，优先显示）
        if (cursor.moveToLast()) {
            do {
                //输出文件的完整路径
                String data = cursor.getString(0);
                Log.e("tag", data);
            } while (cursor.moveToPrevious());
        }
        cursor.close();
    }

    public static String formatSize(Context c, long target_size) {
        if (c == null) {
            return "";
        }
        return Formatter.formatFileSize(c, target_size);
    }

    public static String formatTime(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd  HH:mm", Locale.getDefault());
        return sdf.format(new Date(time));
    }

    public static String formatTime(Date time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd  HH:mm", Locale.getDefault());
        return sdf.format(time);
    }

    public static long getToDayZeroTime() {
        Calendar current = Calendar.getInstance();
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, current.get(Calendar.YEAR));
        calendar.set(Calendar.MONTH, current.get(Calendar.MONTH));
        calendar.set(Calendar.DAY_OF_MONTH, current.get(Calendar.DAY_OF_MONTH));

        //  Calendar.HOUR——12小时制的小时数 Calendar.HOUR_OF_DAY——24小时制的小时数
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        return calendar.getTime().getTime();
    }

    public static boolean isInDays(long timestamp, int days) {
        long currentSeconds = System.currentTimeMillis();//系统当前时间
        String timeStr = null;
        long currentStart = currentSeconds - currentSeconds % (24 * 60 * 60 * 1000);
        long timeGap = (currentStart - timestamp) / 1000;//      与当前凌晨时间相差秒数
        if (timeGap <= 0) {//今天
            return true;
        } else if (timeGap > 0 && timeGap <= (days - 1) * 24 * 60 * 60) {
            return true;
        }
        return false;
    }

    //格式化显示时间
    public static String displayTime(long timestamp) {

        SimpleDateFormat mh = new SimpleDateFormat("MM-dd      HH:mm:ss");
        SimpleDateFormat hm = new SimpleDateFormat("HH:mm:ss");
        long currentSeconds = System.currentTimeMillis();//系统当前时间
        String timeStr = null;
        long currentStart = currentSeconds - currentSeconds % (24 * 60 * 60 * 1000);
        long timeGap = (currentStart - timestamp) / 1000;//      与当前凌晨时间相差秒数
        if (timeGap <= 0)//今天
        {
            timeStr = hm.format(timestamp);
        } else if (timeGap > 0 && timeGap <= 24 * 60 * 60) {
            timeStr = "昨天" + hm.format(timestamp);//      昨天
        } else if (timeGap > 24 * 60 * 60 && timeGap <= 2 * 24 * 60 * 60) {
            timeStr = "前天" + hm.format(timestamp);//      前天
        } else {
            timeStr = mh.format(timestamp);
        }
        return timeStr;
    }

    /**
     * 获得文件的mimeType
     *
     * @param
     * @return
     */
    public static String getMIMEType(String end) {
        String type = "*/*";
        //  取得扩展名
        String s = end.toLowerCase();
        if (s.equals("")) return type;
        //在MIME和文件类型的匹配表中找到对应的MIME类型。
        for (int i = 0; i < MIME_MapTable.length; i++) {
            if (s.equals(MIME_MapTable[i][0]))
                type = MIME_MapTable[i][1];
        }
        return type;
    }

    public static String[][] MIME_MapTable = {
            //{后缀名，        MIME类型}
            {".3gp", "video/3gpp"},
            {".apk", "application/vnd.android.package-archive"},
            {".asf", "video/x-ms-asf"},
            {".avi", "video/x-msvideo"},
            {".bin", "application/octet-stream"},
            {".bmp", "image/bmp"},
            {".c", "text/plain"},
            {".class", "application/octet-stream"},
            {".conf", "text/plain"},
            {".cpp", "text/plain"},
            {".doc", "application/msword"},
            {".docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
            {".xls", "application/vnd.ms-excel"},
            {".xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
            {".exe", "application/octet-stream"},
            {".gif", "image/gif"},
            {".gtar", "application/x-gtar"},
            {".gz", "application/x-gzip"},
            {".h", "text/plain"},
            {".htm", "text/html"},
            {".html", "text/html"},
            {".jar", "application/java-archive"},
            {".java", "text/plain"},
            {".jpeg", "image/jpeg"},
            {".jpg", "image/jpeg"},
            {".js", "application/x-javascript"},
            {".log", "text/plain"},
            {".m3u", "audio/x-mpegurl"},
            {".m4a", "audio/mp4a-latm"},
            {".m4b", "audio/mp4a-latm"},
            {".m4p", "audio/mp4a-latm"},
            {".m4u", "video/vnd.mpegurl"},
            {".m4v", "video/x-m4v"},
            {".mov", "video/quicktime"},
            {".mp2", "audio/x-mpeg"},
            {".mp3", "audio/x-mpeg"},
            {".mp4", "video/mp4"},
            {".mpc", "application/vnd.mpohun.certificate"},
            {".mpe", "video/mpeg"},
            {".mpeg", "video/mpeg"},
            {".mpg", "video/mpeg"},
            {".mpg4", "video/mp4"},
            {".mpga", "audio/mpeg"},
            {".msg", "application/vnd.ms-outlook"},
            {".ogg", "audio/ogg"},
            {".pdf", "application/pdf"},
            {".png", "image/png"},
            {".pps", "application/vnd.ms-powerpoint"},
            {".ppt", "application/vnd.ms-powerpoint"},
            {".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"},
            {".prop", "text/plain"},
            {".rc", "text/plain"},
            {".rmvb", "audio/x-pn-realaudio"},
            {".rtf", "application/rtf"},
            {".sh", "text/plain"},
            {".tar", "application/x-tar"},
            {".tgz", "application/x-compressed"},
            {".txt", "text/plain"},
            {".wav", "audio/x-wav"},
            {".wma", "audio/x-ms-wma"},
            {".wmv", "audio/x-ms-wmv"},
            {".wps", "application/vnd.ms-works"},
            {".xml", "text/plain"},
            {".z", "application/x-compress"},
            {".zip", "application/x-zip-compressed"},
            {"", "*/*"}
    };

    public static String getFileName(String path) {
        if (TextUtils.isEmpty(path)) {
            return "";
        }
        int start = path.lastIndexOf("/");
        if (start != -1) {
            return path.substring(start + 1);
        } else {
            return "";
        }
    }

    public static String getExtension(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot);
            }
        }
        return filename;
    }

    public static String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }

    public static boolean reName(File f, String name, String extension) {
        String newPath = f.getParent() + "/" + name + extension;
        if (f.getParentFile().canWrite()) {
            return f.renameTo(new File(newPath));
        }
        return false;
    }

    public static void scanFile(String path, Context c) {
        System.out.println(path + " " + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= 19) {
            MediaScannerConnection.scanFile(c, new String[]{path}, null, new MediaScannerConnection.OnScanCompletedListener() {

                @Override
                public void onScanCompleted(String path, Uri uri) {

                }
            });
        } else {
            Uri contentUri = Uri.fromFile(new File(path));
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
            c.sendBroadcast(mediaScanIntent);
        }
    }

    public static String saveBitmapAsFile(Bitmap b, String dstDirPath, String name) {
        String result = null;
        if (b == null) {
            return result;
        }
        if (TextUtils.isEmpty(dstDirPath) || TextUtils.isEmpty(name)) {
            return result;
        }
        FileOutputStream ops = null;
        File file = new File(dstDirPath);
        if (!file.exists()) {
            boolean mkdirs = file.mkdirs();
        }
        String dstPath = dstDirPath + File.separator + name;
        try {
            ops = new FileOutputStream(dstPath);
            b.compress(Bitmap.CompressFormat.PNG, 100, ops);
            result = dstPath;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                ops.flush();
                ops.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static String md5(File file) {
        if (file == null || !file.isFile() || !file.exists()) {
            return "";
        }
        FileInputStream in = null;
        String result = "";
        byte buffer[] = new byte[8192];
        int len;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer)) != -1) {
                md5.update(buffer, 0, len);
            }
            byte[] bytes = md5.digest();

            for (byte b : bytes) {
                String temp = Integer.toHexString((b & 0xff) + 0x128).substring(1);
                result += temp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * Try to return the absolute file path from the given Uri
     *
     * @param context
     * @param uri
     * @return the file path or null
     */
    public static String getRealFilePath(final Context context, final Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Files.FileColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }


    public static boolean isFileExist(String realPath) {
        if (!TextUtils.isEmpty(realPath)) {
            return new File(realPath).exists();
        }
        return false;
    }
}