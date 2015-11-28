package com.sam.blechat;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    private BluetoothLeScanner mBluetoothScanner = null;
    private AdvertiseCallback mAdvertisementCallback = null;
    private Timer mTimer = null;

    private WeakReference<BLEActivity> mChatScreenActivity = new WeakReference<>(null);
    private List<BLEMessage> mMessages = new LinkedList<>();

    private BLEMessage mCurrentlySentMessage = null;

    private boolean mIsCurrentlyDiscovering = false;

    public BLEApplication() {
        super();

        mServer = new BLEServer(this);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null) {
            mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            mBluetoothScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }
        startLEScanning();

        mTimer = new Timer();
    }

    private void startLEScanning() {
        if (isBluetoothEnabled() && mBluetoothScanner != null) {
            Log.d(TAG, "Starting scan");

            mIsCurrentlyDiscovering = true;
            try {
                ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
                ScanSettings settings = settingsBuilder
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                mBluetoothScanner.startScan(null, settings, mScanCallback);
            } catch (Exception e) {
                Log.d(TAG, "Failed starting scan", e);
                mIsCurrentlyDiscovering = false;
            }
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            mIsCurrentlyDiscovering = false;
            Log.d(TAG, "Failed starting scan: " + errorCode);
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord == null) {
                return;
            }
            byte[] data = scanRecord.getManufacturerSpecificData(0xF00D);
            if (data == null) {
                return;
            }
            BluetoothDevice device = result.getDevice();
            if (device == null) {
                return;
            }
            String id = bytesToId(data);
            if (id == null) {
                Log.d(TAG, "Couldn't extract id from data");
            }

            for (BLEMessage msg : mMessages) {
                if (msg.getId().equals(id)) {
                    Log.d(TAG, "Already found message " + id);
                    return;
                }
            }

            Log.d(TAG, "Found message " + id);

            String mac = device.getAddress();
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US).format(new Date());

            BLEMessage msg = new BLEMessage(mac, id, "Unknown", date, "fetching message...", false);
            msg.setState(BLEMessageState.DOWNLOADING);

            mServer.getMessage(msg, new BLEServer.GetMessageCallback() {
                @Override
                public void getMessageSuccess(BLEMessage msg) {
                    msg.setState(BLEMessageState.SUCCESS);

                    refreshActivity();
                }

                @Override
                public void getMessageError(BLEMessage msg, Throwable error) {
                    Log.e(TAG, error.getMessage());

                    msg.setState(BLEMessageState.FAILURE);

                    refreshActivity();
                }
            });

            mMessages.add(msg);

            refreshActivity();
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        mIsCurrentlyDiscovering = false;
                        refreshActivity();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        startScanning();
                        break;
                }
                refreshActivity();
            }
        }
    };

    public void setChatScreenActivity(BLEActivity activity) {
        mChatScreenActivity = new WeakReference<>(activity);
        Log.d(TAG, "Registering receiver");
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
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
                    Log.e(TAG, "Failure transmitting message", e);
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

    public void startScanning() {
        startLEScanning();
        refreshActivity();
    }

    public boolean isScanning() {
        return mIsCurrentlyDiscovering;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

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

    private String bytesToId(byte[] bytes) {
        try {
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            long high = bb.getLong();
            long low = bb.getLong();
            UUID uuid = new UUID(high, low);
            return uuid.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
