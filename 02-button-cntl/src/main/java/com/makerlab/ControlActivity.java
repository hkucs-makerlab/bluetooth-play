package com.makerlab;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.makerlab.widgets.Utils;
import com.makerlab.bt.BluetoothConnect;
import com.makerlab.protocol.PlainTextProtocol;
import com.longdo.mjpegviewer.MjpegView;

import java.util.Timer;
import java.util.TimerTask;

import com.makerlab.cntl.R;

public class ControlActivity extends AppCompatActivity implements
        View.OnClickListener, BluetoothConnect.ConnectionHandler {
    private static final String LOG_TAG =
            ControlActivity.class.getSimpleName();
    private BluetoothDevice mBluetoothDevice;
    Button mButtonUp, mButtonDown, mButtonLeft, mButtonRight, mButtonStop, mButtonCenter;
    Button mButtonStart, mButtonHalt;
    private AlertDialog.Builder mAlertBuilder;

    private BluetoothConnect mBluetoothConnect;
    int command;
    private Timer mTimer = null;
    private PlainTextProtocol mPlainText;
    MjpegView mJpegViewer;
    private boolean videoStarted = false;
    private String mVideoUrl = null;
    private boolean mIsBluetoohReady = false;
    private ConstraintLayout mLayoutButtons;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        Toolbar toolbar = findViewById(R.id.toolbar_control);
        setSupportActionBar(toolbar);
        mLayoutButtons=findViewById(R.id.layout_buttons);
        Utils.setViewAndChildrenEnabled(mLayoutButtons,false);
        mButtonCenter = findViewById(R.id.button_cemter);
        mButtonCenter.setOnClickListener(this);
        mButtonUp = findViewById(R.id.button_up);
        mButtonUp.setOnClickListener(this);
        mButtonDown = findViewById(R.id.button_down);
        mButtonDown.setOnClickListener(this);
        mButtonStop = findViewById(R.id.button_stop);
        mButtonStop.setOnClickListener(this);
        mButtonLeft = findViewById(R.id.button_left);
        mButtonLeft.setOnClickListener(this);
        mButtonRight = findViewById(R.id.button_right);
        mButtonRight.setOnClickListener(this);
        mButtonStart = findViewById(R.id.button_start);
        mButtonStart.setOnClickListener(this);
        mButtonHalt = findViewById(R.id.button_halt);
        mButtonHalt.setOnClickListener(this);
        //
        mJpegViewer = (MjpegView) findViewById(R.id.mjpegview);
        mJpegViewer.setMode(MjpegView.MODE_FIT_WIDTH);
        mJpegViewer.setAdjustHeight(true);
        mJpegViewer.setSupportPinchZoomAndPan(true);
        mJpegViewer.setMsecWaitAfterReadImageError(2000);
        //
        mPlainText = new PlainTextProtocol();
        mAlertBuilder = new AlertDialog.Builder(this);
        //
        Intent intent = getIntent();
        mVideoUrl = intent.getStringExtra("video_url");
        if (mVideoUrl !=null) {
            TextView t1 = findViewById(R.id.value_url);
            t1.setText(mVideoUrl);
        } else {
            mButtonStart.setEnabled(false);
        }
        mBluetoothDevice = intent.getParcelableExtra("device");
        mBluetoothConnect = new BluetoothConnect(this);
        mBluetoothConnect.setDevice(mBluetoothDevice);
        mBluetoothConnect.setConnectionHandler(this);
        Log.d(LOG_TAG, "onCreate");
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_control, menu);
        return true;
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_return) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart");
        mBluetoothConnect.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop");
        if (mTimer != null)
            mTimer.cancel();
        mBluetoothConnect.disconnect();
        //
        if (videoStarted) {
            stopVideo();
        }
    }

    @Override
    public void onClick(View v) {

        if (v == mButtonStart) {
            startVideo();
            return;
        } else if (v == mButtonHalt) {
            stopVideo();
            return;
        }

        if (!mIsBluetoohReady) {
            Thread th = new Thread() {
                public void run() {
                    mAlertBuilder.setTitle("bluetooth connection not ready!");
                    mAlertBuilder.show();
                }
            };
            th.run();
            return;
        }
        Log.d(LOG_TAG, "onClick() button pressed");
        if (v == mButtonCenter) {
            command = 5;
        } else if (v == mButtonUp) {
            command = 1;
        } else if (v == mButtonDown) {
            command = 2;
        } else if (v == mButtonLeft) {
            command = 3;
        } else if (v == mButtonRight) {
            command = 4;
        } else if (v == mButtonStop) {
            command = 0;
        }
    }

    private void setStatus(String status) {
        TextView textView = findViewById(R.id.value_status);
        textView.setText(status);
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
        mButtonHalt.setEnabled(true);
        videoStarted = true;
    }

    public void stopVideo() {
        Thread th = new Thread() {
            public void run() {
                try {
                    mJpegViewer.stopStream();
                    Thread.sleep(1000);
                    videoStarted = false;
                } catch (InterruptedException e) {
                }
            }
        };
        th.start();
        try {
            mButtonStart.setEnabled(true);
            th.join(1000);
            mButtonHalt.setEnabled(false);
        } catch (InterruptedException e) {

        }

    }
    @Override
    public void onConnected() {
        Log.d(LOG_TAG, "onConnected()");
        mIsBluetoohReady=true;
        Utils.setViewAndChildrenEnabled(mLayoutButtons,true);
        setStatus("bluetooth connected");
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(new SendTimerTask(), 1000, 200);
        }
    }

    @Override
    public void onDisconnected() {
        Log.d(LOG_TAG, "onDisconnected()");
        AlertDialog.Builder mAlertBuilder = new AlertDialog.Builder(this);
        mAlertBuilder.setTitle("Bluetooth connection losted!");
        mAlertBuilder.show();
        Thread th = new Thread() {
            public void run() {
                try {
                    Thread.sleep(3000);
                    finish();
                } catch (InterruptedException e) {
                }
            }
        };
        th.start();
        mIsBluetoohReady=false;
    }

    class SendTimerTask extends TimerTask {
        private String LOG_TAG = SendTimerTask.class.getSimpleName();

        @Override
        public void run() {
            byte[] payload = mPlainText.getPayload(command);
            mBluetoothConnect.send(payload);
        }
    }
}
