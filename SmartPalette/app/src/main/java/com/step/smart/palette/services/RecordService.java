package com.step.smart.palette.services;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
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
import com.step.smart.palette.utils.StroageUtils;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

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

    Timer checkSpaceTimer;
    void startCheckStorageSpaceTimer() {
        if (checkSpaceTimer != null) {
            return;
        }
        checkSpaceTimer = new Timer();
        checkSpaceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                boolean hasEnoughSpace = StroageUtils.hasEnoughSpaceForWrite(50 * StroageUtils.M);
                if (!hasEnoughSpace) {
                    stopRecord();
                }
            }
        }, 30 * 1000, 30 * 1000);
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
            if (checkSpaceTimer != null) {
                checkSpaceTimer.cancel();
                checkSpaceTimer = null;
            }
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
            String rootDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "StrokeRecord" + "/";
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

    public void broadcastRecord(Activity activity, int code) {
        if (Build.VERSION.SDK_INT > 21) {
            boolean hasEnoughSpace = StroageUtils.hasEnoughSpaceForWrite(50 * StroageUtils.M);
            if (!hasEnoughSpace) {
                return;
            }
            MediaProjectionManager projectionManager = (MediaProjectionManager) activity.getSystemService(MEDIA_PROJECTION_SERVICE);
            Intent captureIntent = projectionManager.createScreenCaptureIntent();
            activity.startActivityForResult(captureIntent, code);
        }
    }

    public class RecordBinder extends Binder {
        public RecordService getRecordService() {
            return RecordService.this;
        }
    }

    public static class Helper {

        public static final int RECORD_CODE = 1001;

        private Activity activity;
        private ServiceConnection serviceConnection;
        private RecordService recordService;

        private ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (Helper.this.serviceConnection != null) {
                    Helper.this.serviceConnection.onServiceConnected(name, service);
                }
                Helper.this.recordService = ((RecordBinder) service).getRecordService();
                DisplayMetrics metrics = new DisplayMetrics();
                Helper.this.activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
                Helper.this.recordService.setConfig(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                if (Helper.this.serviceConnection != null) {
                    Helper.this.serviceConnection.onServiceDisconnected(name);
                }
            }
        };

        public Helper(Activity act, ServiceConnection conn) {
            this.activity = act;
            this.serviceConnection = conn;
        }

        public boolean bindService() {
            boolean result = false;
            if (this.activity != null) {
                result = this.activity.bindService(new Intent(this.activity, RecordService.class), connection, BIND_AUTO_CREATE);
            }
            return result;
        }

        public void unbindService() {
            if (this.activity != null && recordService != null) {
                recordService.stopRecord();
                this.activity.unbindService(connection);
            }
        }

        public void requestRecord() {
            if (this.activity != null && recordService != null) {
                this.recordService.broadcastRecord(this.activity, RECORD_CODE);
            }
        }

        public void record(int resultCode, Intent data) {
            if (this.activity != null && recordService != null) {
                MediaProjectionManager projectionManager = (MediaProjectionManager) activity.getSystemService(MEDIA_PROJECTION_SERVICE);
                MediaProjection mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                if (mediaProjection == null) {
                    return;
                }
                String videoFilePath = recordService.getSaveDirectory() + System.currentTimeMillis() + ".mp4";
                recordService.setMediaProject(mediaProjection);
                recordService.startRecord(videoFilePath);
            }
        }

        public void stopRecord() {
            if (recordService != null) {
                recordService.stopRecord();
            }
        }
    }
}