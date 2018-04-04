package com.step.smart.palette.services;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.blankj.utilcode.util.Utils;
import com.weflow.wmeeting.meeting.R;
import com.weflow.wmeeting.meeting.education.util.ConstantUtil;
import com.weflow.wmeeting.meeting.education.util.Tools;
import com.weflow.wmeeting.meeting.im.util.storage.StorageType;
import com.weflow.wmeeting.meeting.im.util.storage.StorageUtil;

import java.io.File;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class RecordService extends Service {
    private static final String TAG = "RecordService";

    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;

    private boolean running;
    private int width = 1920;
    private int height = 1080;
    private int dpi;

    @Override
    public IBinder onBind(Intent intent) {
        return new RecordBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread serviceThread = new HandlerThread("service_thread",
                android.os.Process.THREAD_PRIORITY_BACKGROUND);
        serviceThread.start();
        running = false;
//		mediaRecorder = new MediaRecorder();
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        RecordService.RecordBinder binder = (RecordService.RecordBinder) service;
        recordService = binder.getRecordService();
        recordService.setConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void setMediaProject(MediaProjection project) {
        mediaProjection = project;
    }

    public boolean isRunning() {
        return running;
    }

    public void setConfig(int width, int height, int dpi) {
//		this.width = width;
//		this.height = height;
        this.dpi = dpi;
    }

    public boolean startRecord(String filePath) {
        if (mediaProjection == null || running) {
            return false;
        }

        try {
            initRecorder(filePath);
            createVirtualDisplay();
            mediaRecorder.start();
            running = true;
            return true;
        } catch (Exception e) {
            Log.e(TAG, "initRecorder error", e);
            running = false;
        }
        return false;
    }

    public boolean stopRecord() {
        if (!running) {
            return false;
        }
        running = false;
        try {
            mediaRecorder.stop();
            mediaRecorder.reset();
            virtualDisplay.release();
            mediaProjection.stop();
        } catch (Exception e) {
            Log.e(TAG, "stopRecord error", e);
        }

        return true;
    }

    private void createVirtualDisplay() {
        virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
    }

    private void initRecorder(String filePath) throws Exception {
        if (mediaRecorder != null) {
            mediaRecorder.release();
        }
        mediaRecorder = new MediaRecorder();
        //		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(filePath);//getSaveDirectory() + System.currentTimeMillis() + ".mp4");
        mediaRecorder.setVideoSize(width, height);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//		mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.prepare();
    }

    public String getSaveDirectory() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String rootDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "MeetingRecord" + "/";
            Log.i(TAG, "rootDir:" + rootDir);

            File file = new File(rootDir);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    return null;
                }
            }

//			Toast.makeText(getApplicationContext(), rootDir, Toast.LENGTH_SHORT).show();

            return rootDir;
        } else {
            return null;
        }
    }

    public static void broadcastRecord(Activity activity) {
        if (Build.VERSION.SDK_INT > 21) {
            boolean hasEnoughSpace = StorageUtil.hasEnoughSpaceForWrite(activity, StorageType.TYPE_VIDEO, false);
            if (!hasEnoughSpace) {
                Tools.showToast(activity, activity.getResources().getString(R.string.insufficient_storage_space), Toast.LENGTH_SHORT);
                return;
            }
            MediaProjectionManager projectionManager = (MediaProjectionManager) activity.getSystemService(MEDIA_PROJECTION_SERVICE);
            Intent captureIntent = projectionManager.createScreenCaptureIntent();
            activity.startActivityForResult(captureIntent, ConstantUtil.REQUEST_CODE_RECORD);
        }
    }

    public class RecordBinder extends Binder {
        public RecordService getRecordService() {
            return RecordService.this;
        }
    }
}