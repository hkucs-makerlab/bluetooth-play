package com.makerlab.setting;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.makerlab.bt.BluetoothConnect;
import com.makerlab.widgets.Utils;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

public class CamSettingActivity extends AppCompatActivity
        implements View.OnClickListener, BluetoothConnect.ConnectionHandler {
    private static final String LOG_TAG =
            CamSettingActivity.class.getSimpleName();

    private BluetoothDevice mBluetoothDevice;
    private EditText mStatus, mSSID, mPSK;
    private TextView mVideoUrl;
    private Button mButtonSave, mButtonLoad, mButtonDefault;
    private Button mButtonStart, mButtonStatus, mButtonReset;
    private Button mButtonSSID, mButtonPSK;
    private Button mButtonReturn;
    RadioButton mRadioButtonWifiModeAp, mRadioButtonWifiModeClient;
    RadioButton mRadioButtonAutoStartNo, mRadioButtonAutoStartYes;
    RadioButton mRadioButtonVide640, mRadioButtonVide1024, mRadioButtonVideo1280;
    RadioButton mRadioButtonFlashOn, mRadioButtonFlashOff;
    private Timer mTimer = null;
    private ATcommandProtocol mATcommand;
    private BluetoothConnect mBluetoothConnect;
    private int mCommand = -1, mPrevCommand = -1;
    private String mCommandArg;
    private int mTimeoutCounter = 0;
    private boolean mIsBluetoohReady = false;
    ConstraintLayout mLayoutMain;
    @Override
    @SuppressLint("MissingPermission")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cam_setting_activity);
        //
        mLayoutMain = findViewById(R.id.layout_main);
        mLayoutMain.setEnabled(false);
        Utils.setViewAndChildrenEnabled(mLayoutMain,false);
        //
        TextView t = findViewById(R.id.label_status);
        t.setEnabled(true);
        mStatus = findViewById(R.id.value_status);
        mStatus.setEnabled(true);
        //
        mPSK = findViewById(R.id.value_psk);
        mSSID = findViewById(R.id.value_ssid);
        mVideoUrl = findViewById(R.id.value_video_url);
        //
        mRadioButtonWifiModeAp = findViewById(R.id.wifi_mode_ap);
        mRadioButtonWifiModeAp.setOnClickListener(this);
        mRadioButtonWifiModeClient = findViewById(R.id.wifi_mode_client);
        mRadioButtonWifiModeClient.setOnClickListener(this);
        //
        mRadioButtonAutoStartNo = findViewById(R.id.auto_start_no);
        mRadioButtonAutoStartNo.setOnClickListener(this);
        mRadioButtonAutoStartYes = findViewById(R.id.auto_start_yes);
        mRadioButtonAutoStartYes.setOnClickListener(this);
        //
        mRadioButtonVide640 = findViewById(R.id.video_size_640);
        mRadioButtonVide640.setOnClickListener(this);
        mRadioButtonVide1024 = findViewById(R.id.video_size_1024);
        mRadioButtonVide1024.setOnClickListener(this);
        mRadioButtonVideo1280 = findViewById(R.id.video_size_1280);
        mRadioButtonVideo1280.setOnClickListener(this);
        //
        mRadioButtonFlashOn = findViewById(R.id.led_on);
        mRadioButtonFlashOn.setOnClickListener(this);
        mRadioButtonFlashOff = findViewById(R.id.led_off);
        mRadioButtonFlashOff.setOnClickListener(this);
        //
        mButtonSave = (Button) findViewById(R.id.button_save);
        mButtonSave.setOnClickListener(this);
        mButtonLoad = (Button) findViewById(R.id.button_load);
        mButtonLoad.setOnClickListener(this);
        //
        mButtonDefault = (Button) findViewById(R.id.button_default);
        mButtonDefault.setOnClickListener(this);

        mButtonStart = (Button) findViewById(R.id.button_start);
        mButtonStart.setOnClickListener(this);
        mButtonStatus = (Button) findViewById(R.id.button_status);
        mButtonStatus.setOnClickListener(this);
        mButtonReset = (Button) findViewById(R.id.button_reset);
        mButtonReset.setOnClickListener(this);
        mButtonReturn = (Button) findViewById(R.id.button_return);
        mButtonReturn.setOnClickListener(this);
        mButtonReturn.setEnabled(true);
        mButtonSSID = (Button) findViewById(R.id.button_ssid);
        mButtonSSID.setOnClickListener(this);
        mButtonPSK = (Button) findViewById(R.id.button_psk);
        mButtonPSK.setOnClickListener(this);
        //
        mATcommand = new ATcommandProtocol();
        //
        Intent intent = getIntent();
        mBluetoothDevice = intent.getParcelableExtra("device");
        //
        TextView v2 = findViewById(R.id.value_address);
        v2.setText(mBluetoothDevice.getAddress());
        TextView v1 = findViewById(R.id.value_name);
        String name = mBluetoothDevice.getName();
        if (name != null) {
            v1.setText(mBluetoothDevice.getName());
        } else {
            v1.setText("No Name");
        }
        //
        mBluetoothConnect = new BluetoothConnect(this);
        mBluetoothConnect.setDevice(mBluetoothDevice);
        mBluetoothConnect.setConnectionHandler(this);

        Log.d(LOG_TAG, "onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart");
        mBluetoothConnect.connect();
        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.scheduleAtFixedRate(new SendTimerTask(), 1000, 200);
        }
    }

    protected void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop");
        if (mTimer != null)
            mTimer.cancel();
        mBluetoothConnect.disconnect();
    }

    private void setStatus(String status) {
        mStatus.setText(status);
    }

    private void setVideoUrl(String url) {
        mVideoUrl.setText(url);
    }

    private void setSSID(String ssid) {
        TextView t = findViewById(R.id.value_ssid);
        t.setText(ssid);
    }

    private void setPSK(String psk) {
        TextView t = findViewById(R.id.value_psk);
        t.setText(psk);
    }

    private void setWiFiMode(int mode) {
        switch (mode) {
            case 1:
                mRadioButtonWifiModeClient.setChecked(true);
                break;
            case 2:
                mRadioButtonWifiModeAp.setChecked(true);
                break;
        }
    }

    private void setAutoStart(boolean flag) {
        if (flag) {
            mRadioButtonAutoStartYes.setChecked(true);
        } else {
            mRadioButtonAutoStartNo.setChecked(true);
        }

    }

    private void setVideoMode(int mode) {
        switch (mode) {
            case 1:
                mRadioButtonVide640.setChecked(true);
                break;
            case 2:
                mRadioButtonVide1024.setChecked(true);
                break;
            case 3:
                mRadioButtonVideo1280.setChecked((true));
                break;
        }
    }

    public void returnToParent() {
        Intent intent = new Intent();
        String url = mVideoUrl.getText().toString();
        if (url.indexOf("http") != -1) {
            intent.putExtra("video_url", mVideoUrl.getText());
        } else {
            intent.putExtra("video_url", "");
        }
        if (mPrevCommand == ATcommandProtocol.RESET) {
            intent.putExtra("video_url", "");
        }
        //intent.putExtra("device", mBluetoothDevice);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onClick(View v) {
        Log.d(LOG_TAG, "onClick - " + String.valueOf(mCommand));
        if (v == mButtonReturn) {
            returnToParent();
        }
        //
        if (mCommand == -1) {
            setStatus("wait acknowledgement");
            int id = v.getId();
            if (v.getId() == R.id.wifi_mode_client) {
                mCommand = ATcommandProtocol.MODE_1;
            } else if (v.getId() == R.id.wifi_mode_ap) {
                mCommand = ATcommandProtocol.MODE_2;
            } else if (v.getId() == R.id.video_size_640) {
                mCommand = ATcommandProtocol.VID_1;
            } else if (v.getId() == R.id.video_size_1024) {
                mCommand = ATcommandProtocol.VID_2;
            } else if (v.getId() == R.id.video_size_1280) {
                mCommand = ATcommandProtocol.VID_3;
            } else if (v.getId() == R.id.auto_start_no) {
                mCommand = ATcommandProtocol.AUTO_0;
            } else if (v.getId() == R.id.auto_start_yes) {
                mCommand = ATcommandProtocol.AUTO_1;
            } else if (v.getId() == R.id.button_ssid) {
                String tmp=mSSID.getText().toString();
                if (tmp !=null && tmp.length() > 0) {
                    mCommandArg =tmp;
                    mCommand = ATcommandProtocol.SSID;
                } else {
                    setStatus("invalid SSID");
                }
            } else if (v.getId() == R.id.button_psk) {
                String tmp=mPSK.getText().toString();
                if (tmp !=null && tmp.length() > 0) {
                    mCommandArg =tmp;
                    mCommand = ATcommandProtocol.PSK;
                } else {
                    setStatus("invalid PSK");
                }
            } else if (v.getId() == R.id.button_save) {
                mCommand = ATcommandProtocol.SAVE;
            } else if (v.getId() == R.id.button_load) {
                mCommand = ATcommandProtocol.LOAD;
            } else if (id == R.id.button_status) {
                mCommand = ATcommandProtocol.STATUS;
                //mCommand = ATcommandProtocol.AT;
            } else if (id == R.id.button_start) {
                mCommand = ATcommandProtocol.START;
            } else if (id == R.id.button_default) {
                mCommand = ATcommandProtocol.DEFAULT;
            } else if (id == R.id.button_reset) {
                mCommand = ATcommandProtocol.RESET;
            } else if (v.getId() == R.id.led_on) {
                mCommand = ATcommandProtocol.LED_ON;
            } else if (v.getId() == R.id.led_off) {
                mCommand = ATcommandProtocol.LED_OFF;
            }
        }
    }

    @Override
    public void onConnected() {
        mIsBluetoohReady = true;
        mButtonStatus.setEnabled(mIsBluetoohReady);
        setStatus("bluetooth connected");
    }

    @Override
    public void onDisconnected() {
        Log.d(LOG_TAG, "onDisconnected()");
/*        AlertDialog.Builder mAlertBuilder = new AlertDialog.Builder(this);
        mAlertBuilder.setTitle("Bluetooth connection losted!");
        mAlertBuilder.show();*/
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
    }

    //
    class SendTimerTask extends TimerTask {
        private String LOG_TAG = SendTimerTask.class.getSimpleName();

        @Override
        public void run() {
            if (!mIsBluetoohReady) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        setStatus(mBluetoothConnect.mErrorString);
                    }
                });
                return;
            }
            // Log.d(LOG_TAG, "run start");
            switch (mCommand) {
                case ATcommandProtocol.SEND_WAIT: {
                    // timer do nothing
                }
                break;
                case ATcommandProtocol.SEND_ACK: {
                    Log.d(LOG_TAG, "response - begin");
                    boolean ack = false;
                    String line = mBluetoothConnect.readLine();
                    if (line != null) {
                        Log.d(LOG_TAG, "ack " + line + ",len " + line.length());
                        if (line.equals(ATcommandProtocol.ACK_OK)) {
                            ack = true;
                        } else {
                            if (mPrevCommand == ATcommandProtocol.RESET) { //reset called
                                returnToParent();
                            } else if (mPrevCommand == ATcommandProtocol.STATUS) { // status called
                                Thread th = new Thread() {
                                    public void run() {
                                        try {
                                            JSONObject json = new JSONObject(line);
                                            setSSID(json.getString("ssid"));
                                            setPSK(json.getString("psk"));
                                            setWiFiMode(json.getInt("mode"));
                                            setVideoMode(json.getInt("vid"));
                                            setAutoStart(json.getBoolean("auto-start"));
                                            setVideoUrl(json.getString("url"));
                                        } catch (JSONException e) {
                                            setVideoUrl("no video link");
                                        }
                                        if (!mLayoutMain.isEnabled()) {
                                            mLayoutMain.setEnabled(true);
                                            Utils.setViewAndChildrenEnabled(mLayoutMain,true);

                                        }
                                    }
                                };
                                runOnUiThread(th);
                                try {
                                    th.join();
                                } catch (InterruptedException e) {
                                }
                            } // if
                        }
                    }
                    if (ack) {
                        mPrevCommand = mCommand;
                        mCommand = ATcommandProtocol.SEND_WAIT;
                        mTimeoutCounter = 0;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setStatus("acknowledged");
                            }
                        });

                    } else {
                        if (++mTimeoutCounter >= 20) {
                            mTimeoutCounter = 0;
                            mPrevCommand = mCommand;
                            mCommand = ATcommandProtocol.SEND_WAIT;
                        }
                    }
                    Log.d(LOG_TAG, "response - end");
                }
                break;
                default: {
                    if (mCommand >= ATcommandProtocol.STATUS) {
                        //Log.d(LOG_TAG, "send payload");
                        byte[] payload;
                        switch (mCommand) {
                            case ATcommandProtocol.SSID:
                            case ATcommandProtocol.PSK:
                                payload = mATcommand.getPayload(mCommand, mCommandArg);
                                break;
                            default:
                                payload = mATcommand.getPayload(mCommand, null);
                        }
                        mBluetoothConnect.send(payload);
                        mPrevCommand = mCommand;
                        mCommand = ATcommandProtocol.SEND_ACK;
                    }
                }
            } // switch
        }
    }
}