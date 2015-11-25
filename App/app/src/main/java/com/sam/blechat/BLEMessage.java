package com.sam.blechat;

public class BLEMessage {

    private String mId;
    private String mUser;
    private String mText;
    private String mMAC;
    private String mDate;
    private boolean mIsSelf;
    private BLEMessageState mState;

    public BLEMessage(String mac, String id, String user,
                      String date, String text, boolean isSelf) {
        mMAC = mac;
        mId = id;
        mUser = user;
        mDate = date;
        mIsSelf = isSelf;
        mText = text;
        mState = BLEMessageState.CREATED;
    }

    public String getId() {return mId; }

    public String getUser() { return mUser; }

    public String getText() { return mText; }

    public String getMAC() {
        return mMAC;
    }

    public boolean isSelf() { return mIsSelf; }

    public String getDate() { return mDate; }

    public void setState(BLEMessageState state) { mState = state; }
    public BLEMessageState getState() { return mState; }

}
