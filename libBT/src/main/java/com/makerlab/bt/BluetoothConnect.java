package com.makerlab.bt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.UUID;

public class BluetoothConnect implements Serializable {
    private static final String LOG_TAG =
            BluetoothConnect.class.getSimpleName();
    private Activity mActivity;

    //
    private BluetoothDevice mBluetoothDevice;
    private BluetoothSocket mBluetoothSocket;
    private BluetoothGatt mBluetoothGatt = null;
    private BluetoothGattCharacteristic mGattCharacteristic = null;
    private BluetoothGattCallback mGattCallback;
    private OutputStream mOutputStream;
    private ConnectionStateCallback mConnectionStateCallback;
    private ConnectionHandler mConnectionHandler;
    private BufferedReader mBufferReader;
    public String mErrorString = "";
    private ByteArrayOutputStream mOutputByteStream;

    public BluetoothConnect(Activity activity) {
        mActivity = activity;
        mConnectionStateCallback = new ConnectionStateCallback();
        mGattCallback = new BluetoothGattCallback();
        mOutputByteStream = new ByteArrayOutputStream();
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

    @SuppressLint("MissingPermission")
    public boolean isConnected() {
        boolean flag = false;
        if (mBluetoothDevice == null) return false;
        if (mBluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            flag = (mBluetoothSocket.isConnected() && mOutputStream != null);
        }
        if (mBluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
            flag = mGattCharacteristic != null;
        }
        return flag;
    }

    @SuppressLint("MissingPermission")
    synchronized public void disconnect() {
        Log.d(LOG_TAG, "disconnect() " + Build.MODEL);
        if (mBluetoothDevice == null) return;
        if (mBluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
            if (mBluetoothGatt != null) {
                mBluetoothGatt.close();
            }
        }
        if (mBluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
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
        }
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

    @SuppressLint("MissingPermission")
    synchronized public void connect() {

        Thread th;

        Thread ble = new Thread() {
            public void run() {
                Log.d(LOG_TAG, "connect() - ble");
                mActivity.registerReceiver(mConnectionStateCallback,
                        new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
                mActivity.registerReceiver(mConnectionStateCallback,
                        new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
                mBluetoothDevice.connectGatt(mActivity, false, mGattCallback);
            }
        };
        //
        Thread classic = new Thread() {
            @SuppressLint("MissingPermission")
            public void run() {
                Log.d(LOG_TAG, "connect() - spp");
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
        if (mBluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            classic.start();
            th = classic;
        } else if (mBluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
            ble.start();
            th = ble;
        }
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

    @SuppressLint("MissingPermission")
    public String readLine() {
        if (mBluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
            byte[] data = mOutputByteStream.toByteArray();
            if (data.length > 0) {
                InputStream inputStream = new ByteArrayInputStream(data);
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                mBufferReader = new BufferedReader(inputStreamReader);
            } else {
                mBufferReader = null;
            }
        }
        if (mBufferReader != null) {
            try {
                return mBufferReader.readLine();
            } catch (IOException e) {
            }
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    public boolean send(byte[] payload) {
        //Log.d(LOG_TAG, "send()");
        if (payload == null || payload.length == 0) {
            //Log.d(LOG_TAG, "send(): invalid payload");
            return false;
        }
        //
        if (mBluetoothDevice == null) return false;
        if (mBluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            if (mOutputStream == null) {
                return false;
            }
            try {
                mOutputStream.write(payload);
                mOutputStream.flush();
                return true;
            } catch (IOException e) {
                Log.d(LOG_TAG, "send(): " + e.toString());
                return false;
            }
        } else if (mBluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
            return sendLE(payload);
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    public boolean sendLE(byte[] payload) {
        boolean isSuccess = false;
        if (mGattCharacteristic != null) {
            final int maxLength = 20;
            byte[] buffer;
            int loop = payload.length / maxLength;
            int from = 0;
            int to = 0;
            for (int i = 0; i < loop; i++) {
                to += (maxLength);
//                Log.e(LOG_TAG, "send(): A from "+String.valueOf(from));
//                Log.e(LOG_TAG, "send(): A to "+String.valueOf(to));
                buffer = Arrays.copyOfRange(payload, from, to);
//                Log.e(LOG_TAG, "send(): A buffer length "+buffer.length);
                mGattCharacteristic.setValue(buffer);
                mBluetoothGatt.writeCharacteristic(mGattCharacteristic);
                from = to;
                try {
                    Thread.sleep(20);
                } catch (Exception e) {

                }
            }
            int remain = payload.length % maxLength;
            if (remain > 0) {
                to += remain;
                buffer = Arrays.copyOfRange(payload, from, to);
                mGattCharacteristic.setValue(buffer);
                mBluetoothGatt.writeCharacteristic(mGattCharacteristic);
            }
            isSuccess = true;
        }
        return isSuccess;
    }


    private class BluetoothGattCallback extends android.bluetooth.BluetoothGattCallback {
        private String LOG_TAG = BluetoothGattCallback.class.getSimpleName();

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(LOG_TAG, "onConnectionStateChange()");
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // query the device for available GATT services,
                // onServicesDiscovered() will be called if there are services found,
                gatt.discoverServices();
                Log.d(LOG_TAG, "onConnectionStateChange() : gatt STATE_CONNECTED");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(LOG_TAG, "onConnectionStateChange() : gatt STATE_DISCONNECTED");
            }
        }

        @SuppressLint("MissingPermission")
        private BluetoothGattCharacteristic bindServiceAndCharacteristics(BluetoothGatt gatt) {
            String serviceUuid[] = {
                    "0000ffe0-0000-1000-8000-00805f9b34fb",
                    "0000dfb0-0000-1000-8000-00805f9b34fb"
            };
            String charUuid[] = {
                    "0000ffe1-0000-1000-8000-00805f9b34fb",
                    "0000dfb1-0000-1000-8000-00805f9b34fb"
            };
            BluetoothGattService gattService = null;
            BluetoothGattCharacteristic gattCharacteristic = null;
            int i = 0;
            for (i = 0; i < serviceUuid.length; i++) {
                gattService = gatt.getService(UUID.fromString(serviceUuid[i]));
                if (gattService != null) break;
            }
            if (gattService == null) {
                return gattCharacteristic;
            }
/*
            for (BluetoothGattService gattService : gatt.getServices()) {
                Log.e("onServicesDiscovered", "Service: " + gattService.getUuid());
                for (BluetoothGattCharacteristic characteristic : gattService.getCharacteristics()) {
                    Log.e("onServicesDiscovered", "Characteristic: " + characteristic.getUuid());
                    for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                        Log.e("onServicesDiscovered", "descriptor: " + descriptor.getValue().toString());
                    }
                }
            }
            */
            gattCharacteristic = gattService.getCharacteristic(UUID.fromString(charUuid[i]));
            if (gattCharacteristic != null) {
                // turn on data receiving event for the characteristic
                gatt.setCharacteristicNotification(gattCharacteristic, true);
            }
            Log.d(LOG_TAG, "bindServiceAndCharacteristics() - getCharacteristic ");
            return gattCharacteristic;
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mGattCharacteristic = bindServiceAndCharacteristics(gatt);
                if (mGattCharacteristic == null) {
                    gatt.close();
                    Log.d(LOG_TAG, "onServicesDiscovered() : no service/characteristic found!");
                } else {
                    mBluetoothGatt = gatt;
                    Log.d(LOG_TAG, "onServicesDiscovered() : service/characteristic found!");
                }
                Log.d(LOG_TAG, "onServicesDiscovered() : GATT SUCCESS");
            } else {
                Log.d(LOG_TAG, "onServicesDiscovered() : GATT FAILURE");
            }
        }

        // called if receiving data
        // An updated value has been received for a characteristic.
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] buff = characteristic.getValue();
            synchronized (mOutputByteStream) {
                try {
                    mOutputByteStream.write(buff);
                } catch (IOException e) {
                }
            }
            Log.d(LOG_TAG, "onCharacteristicChanged(): receive data length " + buff.length);
        }


    } // class BluetoothGattCallback

    // connect and disconnect events
    private class ConnectionStateCallback extends BroadcastReceiver {
        //private final String LOG_TAG = ConnectionStateCallback.class.getSimpleName();
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                Log.d(LOG_TAG, "ConnectionStateCallback : device disconnected");
                if (mBluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                    mBluetoothGatt = null;
                    mGattCharacteristic = null;
                }
                if (mBluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                    mBluetoothSocket = null;
                    mOutputStream = null;
                }
                if (mConnectionHandler != null)
                    mConnectionHandler.onDisconnected();
            } else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                Log.d(LOG_TAG, "ConnectionStateCallback : device Connected");
                //
                if (mBluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_LE) {
                    if (mConnectionHandler != null)
                        mConnectionHandler.onConnected();
                }
                //
                if (mBluetoothDevice.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
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
    } // class ConnectionStateCallback

    public interface ConnectionHandler {
        void onConnected();

        void onDisconnected();
    }
}
