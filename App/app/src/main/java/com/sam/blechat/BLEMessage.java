package com.sam.blechat;

public class BLEMessage {

    private String mId;
    private String mUser;
    private String mText;
    private String mMAC;
    private String mDate;
    private boolean mIsSelf;
    private BLEMessageState mState;
    private BLEMessageFailureReason mFailureReason;

    public BLEMessage(String mac, String id, String user,
                      String date, String text, boolean isSelf) {
        mMAC = mac;
        mId = id;
        mUser = user;
        mDate = date;
        mIsSelf = isSelf;
        mText = text;
        mState = BLEMessageState.CREATED;
        mFailureReason = BLEMessageFailureReason.NONE;
    }

    public String getId() {return mId; }

    public void setUser(String user) { mUser = user; }
    public String getUser() { return mUser; }

    public void setText(String text) { mText = text; }
    public String getText() { return mText; }

    public String getMAC() {
        return mMAC;
    }

    public boolean isSelf() { return mIsSelf; }

    public String getDate() { return mDate; }

    public void setState(BLEMessageState state) {
        mState = state;
        mFailureReason = BLEMessageFailureReason.NONE;
    }
    public BLEMessageState getState() { return mState; }

    public void setFailureReason(BLEMessageFailureReason reason) { mFailureReason = reason; }
    public BLEMessageFailureReason getFailureReason() { return mFailureReason; }

}
