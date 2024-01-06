package com.makerlab.ui;

import android.bluetooth.le.ScanFilter;
import android.os.ParcelUuid;

import java.util.ArrayList;

public class UUID {
    public static final String serviceUUID_vendor1 = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static final String charUUID_vendor1 = "0000ffe1-0000-1000-8000-00805f9b34fb";

    // vendor 2
    public static final String serviceUUID_vendor2 = "0000dfb0-0000-1000-8000-00805f9b34fb";
    public static final String charUUID_vendor2 = "0000dfb1-0000-1000-8000-00805f9b34fb";

    public static final java.util.UUID charUUID_v1 =
            java.util.UUID.nameUUIDFromBytes(charUUID_vendor1.getBytes());
    public static final java.util.UUID charUUID_v2 =
            java.util.UUID.nameUUIDFromBytes(charUUID_vendor2.getBytes());
    public static ArrayList<ScanFilter> UUIDfilters;
    static{
        UUIDfilters = new ArrayList<>();
        ParcelUuid pu_v1 = new ParcelUuid(java.util.UUID.fromString(serviceUUID_vendor1));
        ParcelUuid pu_v2 = new ParcelUuid(java.util.UUID.fromString(serviceUUID_vendor2));
        ScanFilter filter1 = new ScanFilter.Builder().setServiceUuid(pu_v1).build();
        ScanFilter filter2 = new ScanFilter.Builder().setServiceUuid(pu_v2).build();
        UUIDfilters.add(filter1);
        UUIDfilters.add(filter2);
    }
}
