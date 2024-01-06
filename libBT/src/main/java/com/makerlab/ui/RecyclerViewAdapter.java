/*
 * Copyright (C) 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.makerlab.ui;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import com.makerlab.bluetooth.R;

/***
 * The adapter class for the RecyclerView, contains the BluetoothDevice data.
 */
public class RecyclerViewAdapter extends
        RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    // Member variables.
    private ArrayList<BluetoothDevice> mBluetoothClients;
    private LayoutInflater mInflater;
    private BluetoothActivity mActivity;

    private int mPosition = 0;

    public RecyclerViewAdapter(Context context, ArrayList<BluetoothDevice> bluetoothClients) {
        this.mBluetoothClients = bluetoothClients;
        mActivity = (BluetoothActivity) context;
        mInflater = LayoutInflater.from(mActivity);
    }

    @Override
    public ViewHolder onCreateViewHolder(
            ViewGroup recyclerView, int viewType) {
        // inflate layout to recyclerView
        View layout = mInflater.inflate(R.layout.device_list_bluetooth, recyclerView, false);
        layout.setOnClickListener(mActivity);
        ViewHolder holder = new ViewHolder(layout);

        return holder;
    }


    @SuppressLint("MissingPermission")
    public void onBindViewHolder(ViewHolder holder,
                                 @SuppressLint("RecyclerView") int position) {
        BluetoothItem item = new BluetoothItem();
        item.position = position;
        item.device = mBluetoothClients.get(position);
        holder.bindTo(item);
    }

    @Override
    public int getItemCount() {
        return mBluetoothClients.size();
    }

    //
    class BluetoothItem {
        public int position;
        public BluetoothDevice device;
    }

    //
    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mNumber;
        private TextView mName;
        private TextView mAddress;
        private TextView mType;

        ViewHolder(View layout) {
            super(layout);
            mNumber = layout.findViewById(R.id.bt_number);
            mNumber.setText(String.valueOf(getAdapterPosition()));
            mName = layout.findViewById(R.id.bt_name);
            mAddress = layout.findViewById(R.id.bt_address);
            mType = layout.findViewById(R.id.bt_type);
        }

        @SuppressLint("MissingPermission")
        void bindTo(BluetoothItem item) {
            mNumber.setText(String.valueOf(item.position));
            if (item.device.getName() != null) {
                mName.setText(item.device.getName());
            } else {
                mName.setText("No Name");
            }
            mAddress.setText(item.device.getAddress());
            if (item.device.getType() == BluetoothDevice.DEVICE_TYPE_LE)
                mType.setText("BLE");
            else if (item.device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC)
                mType.setText("Classic");
            else
                mType.setText("Unknown");
        }
    }
}
