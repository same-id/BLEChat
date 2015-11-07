package com.sam.blechat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class BLE extends Activity {

    private static final String TAG = "BLE";
    private static final int REQUEST_ENABLE_BT = 1;

    /*
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser = null;
    */
    private BLEApplication mApplication = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

        guiRemoveGradientActionBar();

        mApplication = (BLEApplication) getApplication();
        mApplication.setChatScreenActivity(this);
        //if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
        //    Log.e(TAG, "BLE Advertisement not supported");
        //    return;
        //}

        /*
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Log.e(TAG, "Error getting BLE Advertiser");
            return;
        }

        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setConnectable(false);
        AdvertiseSettings settings = settingsBuilder.build();

        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        byte[] dataBytes = new byte[] {
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        };
        int mockManufacturerId = 0xF00D;
        dataBuilder.addManufacturerData(mockManufacturerId, dataBytes);
        AdvertiseData data = dataBuilder.build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
        */
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mApplication.isBluetoothSupported()) {
            guiShowBLENotSupported();
            return;
        }

        if (!mApplication.isBluetoothEnabled()) {
            guiShowEnableBLE();
            return;
        }


    }

    /*
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "BLE advertisement started");

            mBluetoothAdapter.startDiscovery();
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, String.format("BLE advertisement failed %d", errorCode));
        }

    };*/

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(TAG, "BLE Discovery started");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "BLE Discovery finished");
            } else {
                Log.d(TAG, String.format("Unknown intent %s", action));
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ble, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT) {
            switch (resultCode) {
                case Activity.RESULT_CANCELED: return;
                default:
                    guiHideEnableBLE();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void requestEnableBLE() {
        assert !mApplication.isBluetoothEnabled();

        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    private void guiRemoveGradientActionBar() {
        getActionBar().setElevation(0);
    }

    private void guiShowEnableBLE() {
        View errorPane = findViewById(R.id.ble_disabled_pane);
        errorPane.setVisibility(View.VISIBLE);
    }

    private void guiHideEnableBLE() {
        View errorPane = findViewById(R.id.ble_disabled_pane);
        errorPane.setVisibility(View.GONE);
    }

    private void guiShowBLENotSupported() {

    }

    public void guiClickEnableBLE(View v) {
        requestEnableBLE();
    }

}
