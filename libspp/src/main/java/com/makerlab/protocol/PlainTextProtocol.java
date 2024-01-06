package com.makerlab.protocol;

import java.io.UnsupportedEncodingException;

public class PlainTextProtocol {
    static final String commands[]={
            "w 0",
            "w 1",
            "w 2",
            "w 3",
            "w 4",
            "w 5"
    };

    public byte[] getPayload(int i) {
        byte[] payload=null;
        if (i < commands.length && i>=0) {
            try {
                String data= commands[i]+"\r\n";
                payload = data.getBytes("iso8859-1");
            } catch (UnsupportedEncodingException e) {}
        }
        return payload;
    };
}
