package com.makerlab.cam;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.makerlab.ControlActivity;
import com.makerlab.ui.BluetoothActivity;
import com.makerlab.setting.CamSettingActivity;
import com.makerlab.videoshow.VideoActivity;

public class MainActivity extends AppCompatActivity
        implements ActivityResultCallback<ActivityResult>, View.OnClickListener {
    private static final String LOG_TAG =
            MainActivity.class.getSimpleName();

    static public final boolean D = BuildConfig.DEBUG;

    private ActivityResultLauncher mActivityResultLauncher;
    private BluetoothDevice mBluetoothDevice;
    private String mVideoUrl = null;
    private BluetoothAdapter mBluetoothAdapter;
    private AlertDialog.Builder mAlertBuilder;

    private Button mButtonControl, mButtonCamera, mButtonVideo;
    private TextView mTextViewName, mTextViewAddress, mTextViewUrl;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        if (D)
            Log.d(LOG_TAG, "onCreate");
        mTextViewName = findViewById(R.id.value_name);
        mTextViewAddress = findViewById(R.id.value_address);
        mTextViewUrl = findViewById(R.id.value_url);
        mButtonControl = (Button) findViewById(R.id.button_control);
        mButtonControl.setOnClickListener(this);
        mButtonCamera = (Button) findViewById(R.id.button_camera);
        mButtonCamera.setOnClickListener(this);
        mButtonVideo = findViewById(R.id.button_video);
        mButtonVideo.setOnClickListener(this);
        //
        mActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this);
        //
        mAlertBuilder = new AlertDialog.Builder(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //
        if (savedInstanceState != null) {
            String tmp = savedInstanceState.getString("bluetooth_name");
            if (tmp != null) mTextViewName.setText(tmp);
            tmp = savedInstanceState.getString("bluetooth_addr");
            if (tmp != null) mTextViewAddress.setText(tmp);
            //
            mVideoUrl = savedInstanceState.getString("video_url");
            if (tmp != null) mTextViewUrl.setText(mVideoUrl);
        }/* else {
            String addr;
            addr = "AB:90:78:56:4B:C8"; // head 1
            //addr = "AB:90:78:56:50:75"; // spp-ca03
            //addr="28:82:3F:C4:A5:60"; // spp-14
            addr = "28:82:3F:C4:D8:01"; // spp-04
            //addr="00:BA:55:56:D2:39"; // cam1
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(addr);
            if (mBluetoothDevice != null) {
                setHeader();
            }
        }*/
    }

    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (D)
            Log.d(LOG_TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);

        if (mTextViewName.getText().length() > 0) {
            outState.putString("bluetooth_name", mTextViewName.getText().toString());
        }
        if (mTextViewAddress.getText().length() > 0) {
            outState.putString("bluetooth_addr", mTextViewAddress.getText().toString());
        }
        if (mTextViewUrl.getText().length() > 0) {
            outState.putString("video_url", mTextViewUrl.getText().toString());
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (D)
            Log.d(LOG_TAG, "onStart ");
        Thread th = new Thread() {
            public void run() {
                try {
                    Thread.sleep(4000); // delay to wait BT reset
                    enableButtons(true);
                } catch (InterruptedException e) {
                }
            }
        };
        th.start();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_setup) {
            checkBluetooth();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onActivityResult(ActivityResult result) {
        if (D)
            Log.d(LOG_TAG, "onActivityResult");
        if (result.getResultCode() == RESULT_OK) {
            Intent intent = result.getData();
            mBluetoothDevice = intent.getParcelableExtra("device");
            if (mBluetoothDevice != null) {
                setHeader();
            }
            mVideoUrl = intent.getStringExtra("video_url");
            if (mVideoUrl != null) {
                mTextViewUrl.setText(mVideoUrl);
                //Log.d(LOG_TAG, mVideoUrl );
            }
        }
    }

    @Override
    @SuppressLint("MissingPermission")
    public void onClick(View v) {
        String addr = mTextViewAddress.getText().toString();
        if (addr.length() == 0) {
            checkBluetooth();
            return;
        }
        if (v == mButtonControl) {
            if (D)
                Log.d(LOG_TAG, "onClick mButtonControl");
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(addr);
            if (mBluetoothDevice != null) {
                Intent intent = new Intent(this, ControlActivity.class);
                intent.putExtra("device", mBluetoothDevice);
                if (mVideoUrl != null) {
                    intent.putExtra("video_url", mVideoUrl);
                }
                startActivity(intent);
                enableButtons(false);
            }
        } else if (v == mButtonCamera) {
            if (D)
                Log.d(LOG_TAG, "onClick mButtonCamera");
            mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(addr);
            if (mBluetoothDevice != null) {
                Intent intent = new Intent(this, CamSettingActivity.class);
                intent.putExtra("device", mBluetoothDevice);
                mActivityResultLauncher.launch(intent);
                enableButtons(false);
            }
        } else if (v == mButtonVideo) {
            if (D)
                Log.d(LOG_TAG, "onClick mButtonVideo");
            if (mVideoUrl == null) {
                mAlertBuilder.setTitle("No video link!");
                mAlertBuilder.show();
                return;
            }
            Intent intent = new Intent(this, VideoActivity.class);
            intent.putExtra("video_url", mVideoUrl);
            startActivity(intent);
            enableButtons(false);
        }
    }

    @SuppressLint("MissingPermission")
    private void setHeader() {
        if (mBluetoothDevice == null) return;
        TextView textView = findViewById(R.id.value_name);
        textView.setText("no name");
        String name = mBluetoothDevice.getName();
        if (name != null)
            textView.setText(name);
        //
        mTextViewAddress.setText(mBluetoothDevice.getAddress());
        mTextViewUrl.setText("");
    }

    private void enableButtons(boolean flag) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mButtonControl.setEnabled(flag);
                mButtonCamera.setEnabled(flag);
                mButtonVideo.setEnabled(flag);
            }
        });
    }

    private void checkBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            mAlertBuilder.setTitle("Bluetooth is not enable!");
            mAlertBuilder.show();
            return;
        }
        Intent intent = new Intent(this, BluetoothActivity.class);
        mActivityResultLauncher.launch(intent);
    }
}