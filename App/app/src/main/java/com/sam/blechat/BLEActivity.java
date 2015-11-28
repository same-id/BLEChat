package com.sam.blechat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

public class BLEActivity extends Activity {

    private static final String TAG = "BLEActivity";
    private static final int REQUEST_ENABLE_BT = 1;

    private BLEApplication mApplication = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

        guiRemoveGradientActionBar();

        mApplication = (BLEApplication) getApplication();
        mApplication.setChatScreenActivity(this);

        guiRefresh();
    }

    private void guiLoadMessages(List<BLEMessage> messages) {
        ChatAdapter adapter = new ChatAdapter(this, messages);
        ListView container = (ListView) findViewById(R.id.msg_list);
        container.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        guiRefresh();

    }

    public void guiRefresh() {
        List<BLEMessage> messages = mApplication.getMessages();
        guiLoadMessages(messages);

        guiHideEnableBLE();
        guiHideBLENotSupported();
        guiHideNotScanning();
        guiHideInternetError();

        if (!mApplication.isBluetoothSupported()) {
            guiShowBLENotSupported();
        } else if (!mApplication.isBluetoothEnabled()) {
            guiShowEnableBLE();
        } else if (!mApplication.isScanning()) {
            guiShowNotScanning();
        }

        if (!mApplication.isNetworkAvailable()) {
            guiShowInternetError();
        }
    }

    /*
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "BLEActivity advertisement started");

            mBluetoothAdapter.startDiscovery();
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, String.format("BLEActivity advertisement failed %d", errorCode));
        }

    };*/

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
            startActivity(new Intent(this, BLESettingsActivity.class));
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
                    guiRefresh();
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
        View errorPane = findViewById(R.id.ble_not_supported_pane);
        errorPane.setVisibility(View.VISIBLE);
    }

    private void guiHideBLENotSupported() {
        View errorPane = findViewById(R.id.ble_not_supported_pane);
        errorPane.setVisibility(View.GONE);
    }

    private void guiShowNotScanning() {
        View errorPane = findViewById(R.id.ble_not_scanning_pane);
        errorPane.setVisibility(View.VISIBLE);
    }

    private void guiHideNotScanning() {
        View errorPane = findViewById(R.id.ble_not_scanning_pane);
        errorPane.setVisibility(View.GONE);
    }

    public void guiClickEnableBLE(View v) {
        requestEnableBLE();
    }

    public void guiClickStartScanning(View v) { mApplication.startScanning(); }

    private void guiShowInternetError() {
        View errorPane = findViewById(R.id.internet_error_pane);
        errorPane.setVisibility(View.VISIBLE);
    }

    private void guiHideInternetError() {
        View errorPane = findViewById(R.id.internet_error_pane);
        errorPane.setVisibility(View.GONE);
    }

    public void guiClickSend(View v) {
        EditText edit = (EditText) findViewById(R.id.msg_edit);
        String text = edit.getText().toString();
        if (TextUtils.isEmpty(text)) {
            return;
        }

        if (!mApplication.sendMessage(text)) {
            Toast.makeText(this, "Couldn't send message", Toast.LENGTH_SHORT).show();
        } else {
            edit.setText("");
        }

        guiRefresh();
    }

}
