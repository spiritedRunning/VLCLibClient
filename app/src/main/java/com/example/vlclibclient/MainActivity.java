package com.example.vlclibclient;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    private static final String url = "rtsp://rtsp.stream/pattern";
//    private static final String url = "rtsp://rtsp.stream/movie";

    private MediaPlayer mediaPlayer;
    private LibVLC libVlc;
    private VLCVideoLayout videoLayout;

    private TextView taksnapTv, recordTv;
    private ProgressBar progressBar;

    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission();

        ArrayList<String> args = new ArrayList<>();//VLC参数
        args.add("--rtsp-tcp");//强制rtsp-tcp，加快加载视频速度
        args.add("--live-caching=0");
        args.add("--file-caching=0");
        args.add("--network-caching=200");//增加实时性

        libVlc = new LibVLC(this, args);
        mediaPlayer = new MediaPlayer(libVlc);
        videoLayout = findViewById(R.id.videoLayout);

        taksnapTv = findViewById(R.id.takesnap_tv);
        recordTv = findViewById(R.id.record_tv);


        taksnapTv.setOnClickListener(this);
        recordTv.setOnClickListener(this);

        progressBar = findViewById(R.id.progressBar);

        initListener();
    }

    private void initListener() {

        mediaPlayer.setEventListener(new MediaPlayer.EventListener() {
            @Override
            public void onEvent(MediaPlayer.Event event) {
                if (event.type == MediaPlayer.Event.Opening) {
                    Log.d(TAG, "VLC Opening");
                    progressBar.setVisibility(View.VISIBLE);
                } else if (event.type == MediaPlayer.Event.Buffering) {
                    Log.d(TAG, "VLC Buffering：" + event.getBuffering());
                    if (event.getBuffering() >= 100) {
                        progressBar.setVisibility(View.GONE);
                    } else
                        progressBar.setVisibility(View.VISIBLE);
                } else if (event.type == MediaPlayer.Event.Playing) {
                    Log.d(TAG, "VLC Playing");

                } else if (event.type == MediaPlayer.Event.Stopped) {
                    Log.d(TAG, "VLC Stopped");
                    progressBar.setVisibility(View.GONE);
                } else if (event.type == MediaPlayer.Event.EncounteredError) {
                    Log.d(TAG, "VLC EncounteredError");
                    progressBar.setVisibility(View.GONE);

                } else if (event.type == MediaPlayer.Event.Vout) {
                    Log.d(TAG, "VLC Vout" + event.getVoutCount());

                } else if (event.type == MediaPlayer.Event.RecordChanged) {
                    Log.d(TAG, "VLC RecordChanged");
                }
            }
        });
    }

    public boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
            }, 1);

        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        mediaPlayer.attachViews(videoLayout, null, false, false);

        Media media = new Media(libVlc, Uri.parse(url));
        media.setHWDecoderEnabled(false, false); //设置后才可以录制和截屏
//        media.addOption(":network-caching=200");
//        media.addOption(":codec=mediacodec,iomx,all");

        mediaPlayer.setMedia(media);
        media.release();
        mediaPlayer.play();
    }


    @Override
    public void onClick(View view) {
        File sdcardPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath());

        switch (view.getId()) {
            case R.id.takesnap_tv:
                new Thread(() -> {
                    if (mediaPlayer.takeSnapShot(0, sdcardPath.getAbsolutePath(), 0, 0)) {
                        Log.i(TAG, "takeSnapShot succ!");
                    }
                }).start();
                break;

            case R.id.record_tv:
                if (!isRecording) {
                    if (mediaPlayer.record(sdcardPath.getAbsolutePath())) {
                        Toast.makeText(MainActivity.this, "录制开始", Toast.LENGTH_SHORT).show();
                        recordTv.setText("停止");
                        isRecording = true;
                    }
                } else {
                    mediaPlayer.record(null);
                    Toast.makeText(MainActivity.this, "录制结束", Toast.LENGTH_SHORT).show();
                    isRecording = false;
                    recordTv.setText("录制");
                }
                break;

        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        mediaPlayer.stop();
        mediaPlayer.detachViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mediaPlayer.release();
        libVlc.release();
    }

}