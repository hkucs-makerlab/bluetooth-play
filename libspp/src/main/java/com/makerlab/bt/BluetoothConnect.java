package com.makerlab.bt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.UUID;

public class BluetoothConnect implements Serializable {
    private static final String LOG_TAG =
            BluetoothConnect.class.getSimpleName();
    private Activity mActivity;

    //
    private BluetoothDevice mBluetoothDevice;
    private BluetoothSocket mBluetoothSocket;
    private OutputStream mOutputStream;
    private ConnectionStateCallback mConnectionStateCallback;
    private ConnectionHandler mConnectionHandler;
    private BufferedReader mBufferReader;
    public String mErrorString = "";

    public BluetoothConnect(Activity activity) {
        mActivity = activity;
        mConnectionStateCallback = new ConnectionStateCallback();
        mConnectionHandler = null;

    }
    public boolean setDevice(String address) {
        BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
        if (device == null) return false;
        setDevice(device);
        return true;
    }
    public void setDevice(BluetoothDevice device) {
        mBluetoothDevice = device;
    }

    public void setConnectionHandler(ConnectionHandler mConnectionHandler) {
        this.mConnectionHandler = mConnectionHandler;
    }

    public boolean isConnected() {
        return (mBluetoothSocket != null && mBluetoothSocket.isConnected() && mOutputStream != null);
    }

    @SuppressLint("MissingPermission")
    synchronized public void disconnect() {
        Log.d(LOG_TAG, "disconnect() " + Build.MODEL);
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException e) {
                Log.d(LOG_TAG, e.toString());
            }
        }
        mOutputStream = null;
        if (mBluetoothSocket != null) {
            try {
                mActivity.unregisterReceiver(mConnectionStateCallback);
                mBluetoothSocket.close();
            } catch (IOException e) {
                Log.d(LOG_TAG, e.toString());
            }
        }
        mBluetoothSocket = null;
        mBluetoothDevice = null;
/*        if (Build.MODEL.equals("Mi A3")) {
            Log.d(LOG_TAG, "disconnect() - reset adapator ");
            //if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            try {
                adapter.disable();
                Thread.sleep(1000);
                adapter.enable();

            } catch (InterruptedException e) {
            }
        }*/
    }

    private void connect() {
        Log.d(LOG_TAG, "connect()");
        //
        Thread th = new Thread() {
            @SuppressLint("MissingPermission")
            public void run() {
                UUID SPPuuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                try {
                    mActivity.registerReceiver(mConnectionStateCallback,
                            new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
                    mActivity.registerReceiver(mConnectionStateCallback,
                            new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
                    mBluetoothSocket =
                            mBluetoothDevice.createInsecureRfcommSocketToServiceRecord(SPPuuid);
                    mBluetoothSocket.connect();
                } catch (IOException e) {
                   // mActivity.unregisterReceiver(mConnectionStateCallback);
                    mBluetoothSocket = null;
                    mErrorString = e.toString();
                    Log.d(LOG_TAG, mErrorString);
                }
            }
        };
        th.start();
        //while (th.isAlive())
    }

    synchronized public void connect(String address) {
        if (setDevice(address)) {
            connect();
        }
    }

    synchronized public void connect(BluetoothDevice device) {
        setDevice(device);
        connect();
    }

    public String readLine() {
        if (mBufferReader != null) {
            try {
                return mBufferReader.readLine();
            } catch (IOException e) {
            }
        }
        return null;
    }

    public boolean send(byte[] payload) {
        //Log.d(LOG_TAG, "send()");
        if (payload == null || payload.length == 0) {
            //Log.d(LOG_TAG, "send(): invalid payload");
            return false;
        }
        if (mOutputStream == null) {
            return false;
        }
        try {
            mOutputStream.write(payload);
            mOutputStream.flush();
        } catch (IOException e) {
            Log.e(LOG_TAG, "send(): " + e.toString());
            return false;
        }
        return true;
    }

    private class ConnectionStateCallback extends BroadcastReceiver {
        //private final String LOG_TAG = ConnectionStateCallback.class.getSimpleName();
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                Log.d(LOG_TAG, "ConnectionStateCallback : device disconnected");
                mBluetoothSocket = null;
                mOutputStream = null;
                if (mConnectionHandler != null)
                    mConnectionHandler.onDisconnected();
            } else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                Log.d(LOG_TAG, "ConnectionStateCallback : device Connected");
                try {
                    if (mBluetoothSocket == null) {
                        return;
                    }
                    mOutputStream = mBluetoothSocket.getOutputStream();
                    mBufferReader = new BufferedReader(new InputStreamReader(mBluetoothSocket.getInputStream()));
                    if (mConnectionHandler != null)
                        mConnectionHandler.onConnected();
                } catch (IOException e) {
                    mBluetoothSocket = null;
                    mOutputStream = null;
                    mBufferReader = null;
                    Log.d(LOG_TAG, e.toString());
                }
            }
        }
    }

    public interface ConnectionHandler {
        void onConnected();

        void onDisconnected();
    }
}
