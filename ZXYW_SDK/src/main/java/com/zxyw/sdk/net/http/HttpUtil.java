package com.zxyw.sdk.net.http;

import androidx.annotation.NonNull;

import com.zxyw.sdk.tools.MyLog;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HttpUtil {

    private static final String TAG = "HttpUtil";
    private static final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).build();
//    private static final ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void post(final String url, final String content, final HttpCallback callback) {
        new Thread(() -> client.newCall(new Request.Builder()
                .post(RequestBody.create(MediaType.parse("text"), content))
                .url(url)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                MyLog.e(TAG, "request failed! " + e.toString());
                if (callback != null) {
                    callback.onFailed(e.toString());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.code() == 200) {
                    final ResponseBody body = response.body();
                    if (body == null) {
                        MyLog.e(TAG, "response body is null");
                        if (callback != null) {
                            callback.onFailed("http request has no response data!");
                        }
                    } else {
                        final String s = body.string();
                        MyLog.d(TAG, s);
                        if (callback != null) {
                            callback.onSuccess(s);
                        }
                    }
                } else {
                    final String msg = "request failed! response code=" + response.code();
                    MyLog.e(TAG, msg);
                    if (callback != null) {
                        callback.onFailed(msg);
                    }
                }
            }
        })).start();
    }

    public interface HttpCallback {
        void onFailed(String error);

        void onSuccess(String bodyString);
    }
}
