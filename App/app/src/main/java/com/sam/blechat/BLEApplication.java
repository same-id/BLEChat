package com.sam.blechat;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class BLEApplication extends Application {

    private static final String TAG = "BLEApplication";

    private BLEServer mServer = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser = null;
    private AdvertiseCallback mAdvertisementCallback = null;
    private Timer mTimer = null;

    private WeakReference<BLEActivity> mChatScreenActivity = new WeakReference<>(null);
    private List<BLEMessage> mMessages = new LinkedList<>();

    private BLEMessage mCurrentlySentMessage = null;

    public BLEApplication() {
        super();

        mServer = new BLEServer(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        }

        mTimer = new Timer();

        mTimer.scheduleAtFixedRate(mScanWatchdogTask, 0, 10000);
    }

    private TimerTask mScanWatchdogTask = new TimerTask() {
        @Override
        public void run() {

        }
    };

    public void setChatScreenActivity(BLEActivity activity) {
        mChatScreenActivity = new WeakReference<>(activity);
    }

    private void refreshActivity() {
        final BLEActivity activity = mChatScreenActivity.get();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.guiRefresh();
                }
            });
        }
    }

    public List<BLEMessage> getMessages() {
        return new LinkedList<>(mMessages);
    }

    public boolean sendMessage(String text) {
        if (!isBluetoothEnabled()) {
            return false;
        }
        if (!isNetworkAvailable()) {
            return false;
        }
        if (mCurrentlySentMessage != null) {
            return false;
        }

        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US).format(new Date());
        String user = getUserName();
        String mac = getBluetoothMAC();
        String id = getRandomId();

        BLEMessage msg = new BLEMessage(
                mac,
                id,
                user,
                date,
                text,
                true
        );

        mMessages.add(msg);

        msg.setState(BLEMessageState.UPLOADING);

        mCurrentlySentMessage = msg;

        mServer.putMessage(msg, new BLEServer.PutMessageCallback() {

            @Override
            public void putMessageSuccess(BLEMessage msg) {

                try {
                    msg.setState(BLEMessageState.TRANSMITTING);

                    refreshActivity();

                    transmitMessage();
                } catch(Exception e) {
                    Log.e(TAG, "failure transmitting message", e);
                    msg.setState(BLEMessageState.FAILURE);
                    mCurrentlySentMessage = null;
                }
            }

            @Override
            public void putMessageError(BLEMessage msg, Throwable error) {
                mCurrentlySentMessage = null;

                Log.e(TAG, error.getMessage());

                msg.setState(BLEMessageState.FAILURE);

                refreshActivity();
            }
        });

        return true;
    }

    public String getUserName() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString(BLESettingsActivity.PREF_KEY_USERNAME,
                "Anonymous");
    }

    public String getBluetoothMAC() {
        assert mBluetoothAdapter != null;
        return mBluetoothAdapter.getAddress();
    }

    private String getRandomId() {
        return UUID.randomUUID().toString();
    }

    public boolean isBluetoothSupported() {
        return mBluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    public boolean isBluetoothTransmittionOn() {
        return true;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /*

    */

    private void transmitMessage(){
        if (mBluetoothLeAdvertiser == null) {
            mCurrentlySentMessage.setState(BLEMessageState.FAILURE);
            mCurrentlySentMessage = null;
            return;
        }

        assert mAdvertisementCallback == null;

        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setConnectable(false);
        AdvertiseSettings settings = settingsBuilder.build();

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();

        byte[] dataBytes = idToBytes(mCurrentlySentMessage.getId());
        int mockManufacturerId = 0xF00D;
        dataBuilder.addManufacturerData(mockManufacturerId, dataBytes);
        AdvertiseData data = dataBuilder.build();

        Log.d(TAG, "Starting to advertise message " + mCurrentlySentMessage.getId());

        mAdvertisementCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                Log.d(TAG, "Advertise message " + mCurrentlySentMessage.getId() + " started");
                mTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Advertise message " + mCurrentlySentMessage.getId() + " ended");
                        mCurrentlySentMessage.setState(BLEMessageState.SUCCESS);
                        mCurrentlySentMessage = null;
                        try {
                            assert mAdvertisementCallback != null;
                            mBluetoothLeAdvertiser.stopAdvertising(mAdvertisementCallback);
                        } catch (Exception e) {
                            Log.d(TAG, "Probably BLE was turned off while advertising message", e);
                        }
                        mAdvertisementCallback = null;
                        refreshActivity();
                    }
                }, 20000);
            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                Log.d(TAG, "Advertise message " + mCurrentlySentMessage.getId() +
                        " ended with code " + errorCode);
                mCurrentlySentMessage.setState(BLEMessageState.FAILURE);
                mCurrentlySentMessage = null;
            }
        };

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertisementCallback);
    }

    private byte[] idToBytes(String id) {
        UUID uuid = UUID.fromString(id);
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    public void startDiscovery() {


    }

    public void stopDiscovery() {

    }

    public void startAdvertising() {

    }

}
