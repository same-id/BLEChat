package com.sam.blechat;

import android.app.Application;
import android.net.Uri;
import android.util.Base64;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.ByteArrayInputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import cz.msebera.android.httpclient.Header;

public class BLEServer {

    //private static String SERVER_HOST = "www.blechat.com";
    private static String SERVER_HOST = "10.0.0.3";

    interface PutMessageCallback {

        void putMessageSuccess(BLEMessage msg);

        void putMessageError(BLEMessage msg, Throwable error);

    }

    interface getMessageCallback {

        void getMessageSuccess(String message);

        void getMessageError(Exception error);

    }

    private Application mApplication;

    public BLEServer(Application application) {
        mApplication = application;
    }


    // interface ServerOperation {
    //
    //    void cancel();
    //
    // }

    // TODO:
    // putMessage/getMessage should return ServerOperation API, in order to support cancellation
    // instead of being void

    public void putMessage(
            final BLEMessage message,
            final PutMessageCallback callback) {
        String url = new Uri.Builder()
                .scheme("http")
                .authority(SERVER_HOST)
                .appendPath("message")
                .appendPath(message.getId())
                .build().toString();

        RequestParams params = new RequestParams();
        params.setForceMultipartEntityContentType(true);
        params.put("msg",
                Base64.encodeToString(message.getText().getBytes(StandardCharsets.UTF_8), 0));

        AsyncHttpClient client = new AsyncHttpClient();
        client.setMaxRetriesAndTimeout(1, 10000);
        client.put(mApplication, url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if (statusCode != HttpURLConnection.HTTP_OK) {
                    callback.putMessageError(message, new Exception("Bad status"));
                }
                callback.putMessageSuccess(message);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                callback.putMessageError(message, error);
            }
        });
    }

    public void getMessage(String identifier) {
        // asd;
    }

}
