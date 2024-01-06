package com.makerlab.protocol;

public class GoBLE {
    static private String LOG_TAG = GoBLE.class.getSimpleName();
    private boolean mRepeat = false;
    private int mPrevChecksum = -1;

    public GoBLE() {
    }

    public void setRepeat(boolean flag) {
        mRepeat = flag;
    }

    public byte[] getPayload(int xPos, int yPos,byte[] mButtonPressed) {
        return getPayload(xPos,yPos,128,128,mButtonPressed);
    }

    public byte[] getPayload(int xPos, int yPos, int xPos2, int yPos2, byte[] mButtonPressed) {
        byte header1=0x55;
        byte header2=(byte)0xaa;
        byte address=0x12;
        byte center=127;
        byte[] payload = {
                header1, // header 1
                header2, // header 2
                address, // address, no effect!
                0x00, // no.of button pressed?
                0x03, // rocker position, any value, doesn't matter
                center, // joystick y
                center, // joystick x
                center, // joystick y2
                center, // joystick x2
                0x00  // checksum
        };
        int payloadIndex = 0;
        int size = 0;
        if (mButtonPressed != null) {
            size = (byte) mButtonPressed.length;
        }
        if (size > 0) { // if button pressed, data len will be dynamic
            // Log.e(LOG_TAG, "buttons pressed - " + String.valueOf(mButtonPressed.length));
            payload = new byte[size + payload.length];
            //
            payload[payloadIndex++] = header1;
            payload[payloadIndex++] = header2;
            payload[payloadIndex++] = address;
            payload[payloadIndex++] = (byte) size; // no. of button pressed?
            payload[payloadIndex++] = 0x03;      // rocker position, any value
            for (int i = 0; i < mButtonPressed.length; i++) {
                payload[payloadIndex++] = mButtonPressed[i];
            }
            payload[payloadIndex++] = (byte) yPos; // joystick y
            payload[payloadIndex++] = (byte) xPos; // joystick x
            payload[payloadIndex++] = (byte) yPos2; // yPos2
            payload[payloadIndex++] = (byte) xPos2; // xPos2
        } else {
            payload[payloadIndex++] = header1;
            payload[payloadIndex++] = header2;
            payload[payloadIndex++] = address;
            payload[payloadIndex++] = 0; // is a button pressed? no
            payload[payloadIndex++] = 0x03; // rocker position
            payload[payloadIndex++] = (byte) yPos; // joystick y
            payload[payloadIndex++] = (byte) xPos; // joystick x
            payload[payloadIndex++] = (byte) yPos2; // yPos2
            payload[payloadIndex++] = (byte) xPos2; // xPos2
        }
        //
        int checksum = 0;
        for (int i = 0; i < payload.length; i++) {
            checksum += payload[i];
        }
        checksum = (checksum % 256);
        payload[payloadIndex] = (byte) checksum; // checksum
        payload[payloadIndex] = (byte) checksum; // checksum
        if (!mRepeat && checksum == mPrevChecksum) {
            return null;
        }
        mPrevChecksum = checksum;
        return payload;
    }
}
