package com.sam.blechat;

public class BLEMessage {

    private String mUser;
    private String mText;
    private String mMAC;

    public BLEMessage(String mac, String id) {

    }

    public String getUser() {
        return mUser;
    }

    public String getText() {
        return mText;
    }

    public String getMAC() {
        return mMAC;
    }

}
