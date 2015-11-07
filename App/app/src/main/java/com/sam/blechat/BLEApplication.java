package com.sam.blechat;

import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class BLEApplication extends Application {

    public interface MessageCallback {

        void onMessage(String message);

        void onError(int errorCode);

    }

    public interface CheckServerCallback {

        enum ServerStatus {
            ALIVE,
            NOT_FOUND,
            NO_INTERNET,
            SERVER_ERROR,
            ERROR
        }

        void onServerAlive();

        void onServerError(ServerStatus errorCode);

    }

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser = null;

    private WeakReference<Activity> mChatScreenActivity = new WeakReference<>(null);
    private List<BLEMessage> mMessages = new LinkedList<>();

    private final static int MAX_CONTENT_LENGTH = 10000;

    public BLEApplication() {
        super();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void setChatScreenActivity(Activity activity) {
        mChatScreenActivity = new WeakReference<Activity>(activity);
    }

    public List<BLEMessage> getMessages() {
        return new LinkedList<>(mMessages);
    }

    public boolean isBluetoothSupported() {
        return mBluetoothAdapter == null;
    }

    public boolean isBluetoothEnabled() {
        if (mBluetoothAdapter == null) {
            return false;
        }
        return mBluetoothAdapter.isEnabled();
    }

    private class CheckServerTask extends AsyncTask<Void, Void, CheckServerCallback.ServerStatus> {

        private CheckServerCallback mCheckServerCallback;

        public CheckServerTask(CheckServerCallback callback) {
            mCheckServerCallback = callback;
        }

        @Override
        protected CheckServerCallback.ServerStatus doInBackground(Void... params) {

            ConnectivityManager connectivityManager
                    = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
                return CheckServerCallback.ServerStatus.NO_INTERNET;
            }

            HttpURLConnection urlConnection = null;

            try {

                URL url = new URL("http://www.sdfdsfdsfndskfds8sldfmdslkfmds9.com/");
                urlConnection = (HttpURLConnection) url.openConnection();
                if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return CheckServerCallback.ServerStatus.SERVER_ERROR;
                }

                return CheckServerCallback.ServerStatus.ALIVE;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return CheckServerCallback.ServerStatus.ERROR;
        }

        @Override
        protected void onPostExecute(CheckServerCallback.ServerStatus status) {
            if (status == CheckServerCallback.ServerStatus.ALIVE) {
                mCheckServerCallback.onServerAlive();
            } else {
                mCheckServerCallback.onServerError(status);
            }
        }
    }

    public void startDiscovery() {


    }

    public void stopDiscovery() {

    }

    public void startAdvertising() {

    }

}
