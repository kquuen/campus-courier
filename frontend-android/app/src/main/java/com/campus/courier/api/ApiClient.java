package com.campus.courier.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ApiClient {

    // 开发时指向本机后端，真机调试换成电脑局域网IP，例如 http://192.168.1.100:8082
    private static final String BASE_URL = "http://10.0.2.2:8082";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static OkHttpClient client;
    private static Context appContext;
    private static final Gson gson = new Gson();

    public static void init(Context context) {
        appContext = context.getApplicationContext();
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    private static String getToken() {
        SharedPreferences sp = appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        return sp.getString("token", "");
    }

    public static void saveLoginInfo(String token, long userId, int role, String nickname) {
        appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("token", token)
                .putLong("userId", userId)
                .putInt("role", role)
                .putString("nickname", nickname)
                .apply();
    }

    public static void clearLoginInfo() {
        appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .edit().clear().apply();
    }

    public static long getSavedUserId() {
        return appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getLong("userId", -1);
    }

    public static int getSavedRole() {
        return appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getInt("role", 0);
    }

    public static String getSavedNickname() {
        return appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("nickname", "");
    }

    // -------- GET --------
    public static void get(String path, ApiCallback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .header("Authorization", "Bearer " + getToken())
                .get()
                .build();
        execute(request, callback);
    }

    // -------- POST (JSON body) --------
    public static void post(String path, Object bodyObj, ApiCallback callback) {
        String json = gson.toJson(bodyObj);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .header("Authorization", "Bearer " + getToken())
                .post(body)
                .build();
        execute(request, callback);
    }

    // -------- PUT --------
    public static void put(String path, Object bodyObj, ApiCallback callback) {
        String json = gson.toJson(bodyObj);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + path)
                .header("Authorization", "Bearer " + getToken())
                .put(body)
                .build();
        execute(request, callback);
    }

    private static void execute(Request request, ApiCallback callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("网络错误：" + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "{}";
                try {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    int code = json.get("code").getAsInt();
                    if (code == 200) {
                        callback.onSuccess(json.has("data") ? json.get("data") : null);
                    } else {
                        String msg = json.has("message") ? json.get("message").getAsString() : "请求失败";
                        callback.onError(msg);
                    }
                } catch (Exception e) {
                    callback.onError("数据解析失败");
                }
            }
        });
    }

    public interface ApiCallback {
        void onSuccess(com.google.gson.JsonElement data);
        void onError(String message);
    }
}
