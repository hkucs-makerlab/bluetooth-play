package com.makerlab.videoshow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.longdo.mjpegviewer.MjpegView;

public class VideoActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String LOG_TAG =
            VideoActivity.class.getSimpleName();
    private MjpegView mJpegViewer;
    private AlertDialog.Builder mAlertBuilder;
    public Button mButtonStart, mButtonStop;

    boolean videoStarted = false;
    private String mVideoUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        mJpegViewer = (MjpegView) findViewById(R.id.mjpegview);
        mJpegViewer.setMode(MjpegView.MODE_FIT_WIDTH);
        mJpegViewer.setAdjustHeight(true);
        mJpegViewer.setSupportPinchZoomAndPan(true);
        mJpegViewer.setMsecWaitAfterReadImageError(2000);
        //
        mButtonStart = (Button) findViewById(R.id.button_start);
        mButtonStart.setOnClickListener(this);
        //
        mButtonStop = (Button) findViewById(R.id.button_stop);
        mButtonStop.setOnClickListener(this);
        //
        Intent intent = getIntent();
        mVideoUrl = intent.getStringExtra("video_url");
        if (mVideoUrl != null) {
            TextView t1 = findViewById(R.id.url_link);
            t1.setText(mVideoUrl);
        }
        mAlertBuilder = new AlertDialog.Builder(this);
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (videoStarted) {
            stopVideo();
        }
        Log.d(LOG_TAG, "onStop");
    }

    @Override
    public void onClick(View v) {
        if (v == mButtonStart) {
            startVideo();
            videoStarted = true;
        } else if (v == mButtonStop) {
            stopVideo();
            videoStarted = false;
        }
    }

    public void startVideo() {
        Thread th = new Thread() {
            public void run() {
                mAlertBuilder.setTitle("No video URL link found!");
                mAlertBuilder.show();
            }
        };
        if (mVideoUrl == null) {
            th.run();
            return;
        }
        //

        mJpegViewer.setUrl(mVideoUrl);
        mJpegViewer.startStream();
        mButtonStart.setEnabled(false);
        mButtonStop.setEnabled(true);
        videoStarted = true;

    }

    public void stopVideo() {
        if (!videoStarted) return;
        Thread th = new Thread() {
            public void run() {
                try {
                    mJpegViewer.stopStream();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
            }
        };
        th.start();
        try {
            mButtonStop.setEnabled(false);
            th.join(2000);
            mButtonStart.setEnabled(true);
        } catch (InterruptedException e) {

        }

    }
}