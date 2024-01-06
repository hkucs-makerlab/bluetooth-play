package com.makerlab.setting;

import android.util.Log;

import java.io.UnsupportedEncodingException;

public class ATcommandProtocol {
    private static final String LOG_TAG =
            ATcommandProtocol.class.getSimpleName();
    static final String commands[]={
            "AT+STATUS",// 0
            "AT+START", // 1
            "AT+DEFAULT", // 2
            "AT+RESET", // 3
            "AT+LEDon", // 4
            "AT+LEDoff", // 5
            "AT+MODE1", // 6
            "AT+MODE2", // 7
            "AT+VID1", // 8
            "AT+VID2", // 9
            "AT+VID3", // 10
            "AT+AUTO0",// 11
            "AT+AUTO1",// 12
            "AT+SSID", // 13
            "AT+PSK", // 14
            "AT+SAVE", // 15
            "AT+LOAD", // 16
            "AT+", // 17
    };
    static public final String ACK_OK="[OK]";
    static public final int SEND_WAIT= -1;
    static public final int SEND_ACK=-2;
    static public final int STATUS=0;
    static public final int START=1;
    static public final int DEFAULT = 2;
    static public final int RESET = 3;
    static public final int LED_ON = 4;
    static public final int LED_OFF = 5;
    static public final int MODE_1 = 6;
    static public final int MODE_2 = 7;
    static public final int VID_1 = 8;
    static public final int VID_2 = 9;
    static public final int VID_3 = 10;
    static public final int AUTO_0 = 11;
    static public final int AUTO_1 = 12;
    static public final int SSID = 13;
    static public final int PSK = 14;
    static public final int SAVE = 15;
    static public final int LOAD = 16;
    static public final int AT = 17;
    public byte[] getPayload(int i, String arg) {
        byte[] payload=null;
        if (i < commands.length && i>=0) {
            try {
                String data;
                if (arg ==null) {
                    data = commands[i] + "\r\n";
                } else {
                    data = commands[i] +arg+"\r\n";
                }
                //Log.d(LOG_TAG, "getPayload - "+ data);
                payload = data.getBytes("iso8859-1");
            } catch (UnsupportedEncodingException e) {}
        }
        return payload;
    };
}
