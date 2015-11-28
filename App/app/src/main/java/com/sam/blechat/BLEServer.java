package com.sam.blechat;

import android.app.Application;
import android.net.Uri;
import android.util.Base64;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

import cz.msebera.android.httpclient.Header;

public class BLEServer {

    private static String SERVER_HOST = "nodejs-blechat.rhcloud.com";

    interface PutMessageCallback {

        void putMessageSuccess(BLEMessage msg);

        void putMessageError(BLEMessage msg, Throwable error);

    }

    interface GetMessageCallback {

        void getMessageSuccess(BLEMessage msg);

        void getMessageError(BLEMessage msg, Throwable error);

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
        params.put("user",
                Base64.encodeToString(message.getUser().getBytes(StandardCharsets.UTF_8), 0));

        params.put("msg",
                Base64.encodeToString(message.getText().getBytes(StandardCharsets.UTF_8), 0));

        AsyncHttpClient client = new AsyncHttpClient();
        client.setMaxRetriesAndTimeout(0, 10000);
        client.put(mApplication, url, params, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if (statusCode != HttpURLConnection.HTTP_OK) {
                    callback.putMessageError(message, new Exception("Bad status"));
                    return;
                }
                callback.putMessageSuccess(message);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                callback.putMessageError(message, error);
            }
        });
    }

    public void getMessage(final BLEMessage message,
                           final GetMessageCallback callback) {
        String url = new Uri.Builder()
                .scheme("http")
                .authority(SERVER_HOST)
                .appendPath("message")
                .appendPath(message.getId())
                .build().toString();

        AsyncHttpClient client = new AsyncHttpClient();
        client.setMaxRetriesAndTimeout(0, 10000);
        client.get(mApplication, url, new AsyncHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if (statusCode != HttpURLConnection.HTTP_OK) {
                    callback.getMessageError(message, new Exception("Bad status"));
                    return;
                }

                try {
                    String body = new String(responseBody);
                    JSONObject object = new JSONObject(body);
                    JSONArray msgJSON;
                    JSONArray userJSON;

                    try {
                        JSONObject userObjJSON = object.getJSONObject("user");
                        assert userObjJSON != null;
                        userJSON = userObjJSON.getJSONArray("data");
                        assert userJSON != null;
                    } catch (Exception e) {
                        userJSON = object.getJSONArray("user");
                        assert userJSON != null;
                    }
                    try {
                        JSONObject msgObjJSON = object.getJSONObject("msg");
                        assert msgObjJSON != null;
                        msgJSON = msgObjJSON.getJSONArray("data");
                        assert msgJSON != null;
                    } catch (Exception e) {
                        msgJSON = object.getJSONArray("msg");
                        assert msgJSON != null;
                    }
                    byte[] userBytes = new byte[userJSON.length()];
                    byte[] msgBytes = new byte[msgJSON.length()];
                    for (int i = 0; i < userJSON.length(); i++) {
                        userBytes[i] = (byte)userJSON.getInt(i);
                    }
                    for (int i = 0; i < msgJSON.length(); i++) {
                        msgBytes[i] = (byte)msgJSON.getInt(i);
                    }

                    String user = new String(userBytes, "UTF-8");
                    String msg = new String(msgBytes, "UTF-8");

                    message.setUser(user);
                    message.setText(msg);

                    callback.getMessageSuccess(message);
                } catch (Exception e) {
                    callback.getMessageError(message, e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                callback.getMessageError(message, error);
            }
        });
    }

}
