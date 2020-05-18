package com.sanjay.ezyscreenrecorder;
import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.provider.Settings;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.io.FileWriter;
import java.util.Timer;
import java.util.TimerTask;
import java.io.File;
import java.text.SimpleDateFormat;


import java.io.IOException;
import java.util.Calendar;

import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
import static androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;

public class MainActivity extends AppCompatActivity {
    private Timer timer = new Timer();
    private TimerTask timerTask;


    private static final String TAG = "MainActivity";
    private String videofile= "";
    private AlertDialog.Builder dialog;
    private static final int REQUEST_CODE = 1000;
    private int mScreenDensity;
    private Button btn_action;

    private LinearLayout base;
    private TextView textView;
    private Toolbar toolbar;

    private MediaProjectionManager mProjectionManager;
    private static final int DISPLAY_WIDTH = 720;
    private static final int DISPLAY_HEIGHT = 1280;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_PERMISSION_KEY = 1;
    boolean isRecording = false;
    private Calendar calendar = Calendar.getInstance();



    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startForegroundService(new Intent(MainActivity.this, BackGround.class));

        int currentNightMode = this.getResources().getConfiguration().uiMode& Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode){
            case Configuration.UI_MODE_NIGHT_NO:

                setTheme(R.style.AppTheme);
                break;
            case Configuration.UI_MODE_NIGHT_YES:

                setTheme(R.style.DarkTheme);
                break;


        }

        setContentView(R.layout.activity_main);

        dialog = new AlertDialog.Builder(this);
        base = (LinearLayout) findViewById(R.id.base);
        textView = (TextView) findViewById(R.id.textView);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        mScreenDensity = metrics.densityDpi;



        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);


        btn_action = (Button) findViewById(R.id.btn_action);
        int currentNightMode1 = this.getResources().getConfiguration().uiMode& Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode1){
            case Configuration.UI_MODE_NIGHT_NO:
                lighttheme();
                setTheme(R.style.AppTheme);
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                darktheme();
                setTheme(R.style.DarkTheme);
                break;


        }

        btn_action.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onToggleScreenShare();

            }
        });

    }



    @Override
    protected void onResume() {
        super.onResume();
        if (AppCompatDelegate.getDefaultNightMode()== MODE_NIGHT_YES){
            lighttheme();

        }else if (AppCompatDelegate.getDefaultNightMode()== MODE_NIGHT_NO){
            darktheme();
        }
    }

    private void lighttheme() {
        setTheme(R.style.AppTheme);
        base.setBackgroundColor(Color.parseColor("#ffffff"));
        btn_action.setTextColor(Color.parseColor("#000000"));
        textView.setTextColor(Color.parseColor("#000000"));
        btn_action.setBackgroundColor(0xFFFFFFFF);
        toolbar.setBackgroundColor(0xFF008577);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(0xFF1A554D);
    }

    private void darktheme() {
        setTheme(R.style.DarkTheme);
        base.setBackgroundColor(Color.parseColor("#000000"));
        btn_action.setTextColor(Color.parseColor("#ffffff"));
        textView.setTextColor(Color.parseColor("#ffffff"));
        btn_action.setBackgroundColor(0xFF121212);
        toolbar.setBackgroundColor(0xFF121212);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(0xFF000000);
    }


    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int currentNightMode = newConfig.uiMode& Configuration.UI_MODE_NIGHT_MASK;
        switch (currentNightMode){
            case Configuration.UI_MODE_NIGHT_NO:
                lighttheme();
                setTheme(R.style.AppTheme);
                break;
            case Configuration.UI_MODE_NIGHT_YES:
                darktheme();
                setTheme(R.style.DarkTheme);
                break;


        }
    }


    public void actionBtnReload() {
        if (isRecording) {
            btn_action.setText(getString(R.string.stop_recording));
        } else {
            btn_action.setText(getString(R.string.start_recording));
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onToggleScreenShare() {
        if (!isRecording) {
            String[] PERMISSIONS = {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO


            };
            if (!Function.hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSION_KEY);
            }else {
                initRecorder();
                shareScreen();
            }
        } else {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            stopScreenSharing();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }


        mVirtualDisplay = createVirtualDisplay();


        mMediaRecorder.start();
        isRecording = true;
        actionBtnReload();
    }

    private VirtualDisplay createVirtualDisplay() {

        return mMediaProjection.createVirtualDisplay("MainActivity", DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initRecorder() {
        mMediaRecorder = new MediaRecorder();
        try {


             File file = new File("/storage/emulated/0/Ezy/");
             if(!file.exists()) {
                 file.mkdirs();
             }
            calendar=Calendar.getInstance();

            Log.e("e","Before");
            videofile= "/storage/emulated/0/Ezy/Video_".concat(new SimpleDateFormat("dd_MM_yyyy_hh_mm_ss_a").format(calendar.getTime()).concat(".mp4"));
            File file1 = new File(videofile);
            Log.e("e","after");

            FileWriter fileWriter = new FileWriter(file1);
            fileWriter.append("");
            fileWriter.flush();
            fileWriter.close();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            Log.e("e","after");
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoEncodingBitRate(3000000);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.setOutputFile(videofile);
            mMediaRecorder.prepare();




        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        destroyMediaProjection();
        isRecording = false;
        actionBtnReload();

        MediaScannerConnection.scanFile(this,new String[]{videofile.toString()},null,
                new MediaScannerConnection.OnScanCompletedListener(){
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("External","scanned"+path+":");
                        Log.i("External","-> uri="+uri);

                    }
                });
        dialog.setTitle("Video is saved...");
        dialog.setMessage("Saved in /storage/emulated/0/Ezy/");
        dialog.setPositiveButton("View Video", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(videofile));
                intent.setDataAndType(Uri.parse(videofile),
                "video/*");
                startActivity(intent);


            }
        });
        dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        dialog.create();
        dialog.show();

    }



    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }


        Log.i(TAG, "MediaProjection Stopped");
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            isRecording = false;
            actionBtnReload();
            return;
        }

        mMediaProjectionCallback = new MediaProjectionCallback();


        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        btn_action.setEnabled(false);
        moveTaskToBack(true);


        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Screen Recording is started", Toast.LENGTH_SHORT).show();
                        timerTask = new TimerTask() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        btn_action.setEnabled(true);

                                        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
                                        mVirtualDisplay = mMediaProjection.createVirtualDisplay("MainActivity", DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                                                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
                                        isRecording = true;
                                        actionBtnReload();

                                        mMediaRecorder.start();


                                    }
                                });
                            }
                        };
                        timer.schedule(timerTask, (int)(100));
                    }
                });
            }
        };
        timer.schedule(timerTask,(int)(100));

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_KEY:
            {
                if ((grantResults.length > 0) && (grantResults[0] + grantResults[1]) == PackageManager.PERMISSION_GRANTED) {
                    onToggleScreenShare();
                } else {
                    isRecording = false;
                    actionBtnReload();
                    Snackbar.make(findViewById(android.R.id.content), "Please enable Microphone and Storage permissions.",
                            Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                                    intent.setData(Uri.parse("package:" + getPackageName()));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                                    startActivity(intent);
                                }
                            }).show();
                }
                return;
            }
        }
    }





    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (isRecording) {
                isRecording = false;
                actionBtnReload();

                mMediaRecorder.stop();
                mMediaRecorder.reset();
            }
            mMediaProjection = null;
            stopScreenSharing();
        }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService(new Intent(MainActivity.this, BackGround.class));
        destroyMediaProjection();
    }

    @Override
    public void onBackPressed() {
        if (isRecording) {
            Snackbar.make(findViewById(android.R.id.content), "Wanna Stop recording and exit?",
                    Snackbar.LENGTH_INDEFINITE).setAction("Stop",
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            mMediaRecorder.stop();
                            mMediaRecorder.reset();
                            Log.v(TAG, "Stopping Recording");
                            stopScreenSharing();
                            finish();
                        }
                    }).show();
        } else {
            finish();
        }
    }
}