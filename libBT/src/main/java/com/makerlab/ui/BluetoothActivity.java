package com.makerlab.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
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
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.makerlab.bluetooth.R;

public class BluetoothActivity extends AppCompatActivity
        implements View.OnClickListener {
    private static final String LOG_TAG =
            BluetoothActivity.class.getSimpleName();
    private BluetoothDevice mBluetoothDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BleScanCallback mBleScanCallback;
    private SppReceiverCallback mSppReceiverCallback;
    boolean mPermissionGranted;
    private RecyclerView mRecyclerView;
    private RecyclerViewAdapter mAdapter;
    private Button mButtonScan, mButtonCancel;
    private ArrayList<BluetoothDevice> mBluetoothClients;
    private Map<String, BluetoothDevice> mBluetoothDeviceMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate");
        setContentView(R.layout.activity_bluetooth);
        Toolbar toolbar = findViewById(R.id.toolbar_bluetooth);
        setSupportActionBar(toolbar);
        //
        mButtonScan = findViewById(R.id.button_scan);
        mButtonScan.setOnClickListener(this);
        mButtonCancel = findViewById(R.id.button_cancel);
        mButtonCancel.setOnClickListener(this);
        //
        mBluetoothDeviceMap = new HashMap<>();
        mBluetoothClients = new ArrayList<>();
        //
        mRecyclerView = findViewById(R.id.recyclerview_bt_device);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new RecyclerViewAdapter(this, mBluetoothClients);
        mRecyclerView.setAdapter(mAdapter);
        //
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBleScanCallback = new BleScanCallback();
        // bluetooth classic callback
        mSppReceiverCallback = new SppReceiverCallback();
        registerReceiver(mSppReceiverCallback,
                new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(mSppReceiverCallback,
                new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        registerReceiver(mSppReceiverCallback,
                new IntentFilter(BluetoothDevice.ACTION_FOUND));
        //
        boolean flag = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        flag &= getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        if (!flag) {
            AlertDialog.Builder mAlertBuilder = new AlertDialog.Builder(this);
            mAlertBuilder.setTitle("No Bluetooth Subsystem!");
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
        }
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bluetooth, menu);
        return true;
    }

    @SuppressLint("MissingPermission")
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_return_bt) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart ");

        String[] permissions;
        mPermissionGranted = true;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT};
            mPermissionGranted &= ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED;
            mPermissionGranted &= ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION};
            mPermissionGranted &= ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }

        if (!mPermissionGranted) {
            ActivityCompat.requestPermissions(this, permissions, 1234);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "onStop");
        mBluetoothAdapter.cancelDiscovery();
        mBluetoothLeScanner.stopScan(mBleScanCallback);
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
        unregisterReceiver(mSppReceiverCallback);
    }

    @SuppressLint("MissingPermission")
    public void onClick(View view) {
        Log.d(LOG_TAG, "onClick ");
        if (view.getId() == R.id.button_scan) {
            if (!mPermissionGranted) {
                AlertDialog.Builder mAlertBuilder = new AlertDialog.Builder(this);
                mAlertBuilder.setTitle("bluetooth discovery permission is not granted!");
                mAlertBuilder.show();
                return;
            }
            mButtonScan.setEnabled(false);
            if (mBluetoothClients.size() > 0) {
                mBluetoothClients.clear();
                mAdapter.notifyDataSetChanged();
            }
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            //
            Set<BluetoothDevice> bonded= mBluetoothAdapter.getBondedDevices();
            Iterator<BluetoothDevice> devices = bonded.iterator();
            while (devices.hasNext()) {
                BluetoothDevice device=devices.next();
                mBluetoothClients.add(device);
                mBluetoothDeviceMap.put(device.getAddress(),device);
            }
            //
            mBluetoothAdapter.startDiscovery();
            if (mBluetoothLeScanner != null) {
                ScanSettings s = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                mBluetoothLeScanner.startScan(UUID.UUIDfilters, s, mBleScanCallback);
                //mBluetoothLeScanner.startScan( mBleScanCallback);
                Log.d(LOG_TAG, "BLE discovery started...");
            }
        } else if (view.getId() == R.id.button_cancel) {
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothLeScanner.stopScan(mBleScanCallback);
        } else if (view.getId() == R.id.device_list_bluetooth) {
            mBluetoothAdapter.cancelDiscovery();
            mBluetoothLeScanner.stopScan(mBleScanCallback);
            //
            TextView v = view.findViewById(R.id.bt_number);
            int index = Integer.valueOf(String.valueOf(v.getText()));
            mBluetoothDevice = mBluetoothClients.get(index);
            Log.d(LOG_TAG, "cyclerview clicked selected " + index);
            //
            Intent intent = new Intent();
            intent.putExtra("device", mBluetoothDevice);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(LOG_TAG, "onRequestPermissionsResult " + grantResults.length);
        if (grantResults.length == 0) return;
        if (requestCode == 1234) {
            mPermissionGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                boolean flag = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                Log.d(LOG_TAG, "onRequestPermissionsResult " + flag);
                mPermissionGranted &= flag;
            }
        }
    }

    // bluetooth LE scan callback
    public class BleScanCallback extends ScanCallback {
        private String LOG_TAG = BleScanCallback.class.getSimpleName();

        @Override
        @SuppressLint("MissingPermission")
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            //Log.d(LOG_TAG, "onScanResult()");
            BluetoothDevice remoteDevice = result.getDevice();
            String name = remoteDevice.getName();
            if (name != null) {
                synchronized (mBluetoothClients) {
                    if (mBluetoothDeviceMap.get(remoteDevice.getAddress()) == null) {
                        mBluetoothDeviceMap.put(remoteDevice.getAddress(), remoteDevice);
                        int lastItemNo = mBluetoothClients.size();
                        mBluetoothClients.add(remoteDevice);
                        mAdapter.notifyItemInserted(lastItemNo);
                        mRecyclerView.smoothScrollToPosition(lastItemNo);
                        Log.d(LOG_TAG, "onScanResult() - found device " + remoteDevice.getName());
                    }
                }
            }
        }
    } // class BleScanCallback

    // bluetooth classic callback
    public class SppReceiverCallback extends BroadcastReceiver {
        private String LOG_TAG = SppReceiverCallback.class.getSimpleName();
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentActon = intent.getAction();

            Log.d(LOG_TAG, "onReceive()");
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intentActon)) {
                Log.d(LOG_TAG, "onReceive() - discovery started...");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intentActon)) {
                Log.d(LOG_TAG, "onReceive() - discovery completed.");
                mButtonScan.setEnabled(true);
                mBluetoothLeScanner.stopScan(mBleScanCallback);
            } else if (BluetoothDevice.ACTION_FOUND.equals(intentActon)) {
                BluetoothDevice remoteDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (remoteDevice != null && remoteDevice.getName() != null) {
/*                    if (remoteDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                         Log.d(LOG_TAG, "onReceive() - found device ble "+remoteDevice.getName());
                        return;
                    }*/
                    if (remoteDevice.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                        Log.d(LOG_TAG, "onReceive() - found device "+remoteDevice.getName());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            remoteDevice.getAlias();
                        }
                        synchronized (mBluetoothClients) {
                            if (mBluetoothDeviceMap.get(remoteDevice.getAddress()) == null) {
                                mBluetoothDeviceMap.put(remoteDevice.getAddress(), remoteDevice);
                                int lastItemNo = mBluetoothClients.size();
                                mBluetoothClients.add(remoteDevice);
                                mAdapter.notifyItemInserted(lastItemNo);
                                mRecyclerView.smoothScrollToPosition(lastItemNo);
                            }
                        }
                    }
                }
            }
        }
    } // class SppReceiverCallback
}