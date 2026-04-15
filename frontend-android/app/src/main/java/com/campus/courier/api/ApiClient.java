package com.campus.courier.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import com.campus.courier.R;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class ApiClient {

    private static String baseUrl = "";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static OkHttpClient client;
    private static Context appContext;
    private static final Gson gson = new Gson();

    public static void init(Context context) {
        appContext = context.getApplicationContext();
        baseUrl = appContext.getString(R.string.api_base_url).trim().replaceAll("/$", "");
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    private static String url(String path) {
        if (path.startsWith("/")) {
            return baseUrl + path;
        }
        return baseUrl + "/" + path;
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

    public static void updateSavedRole(int role) {
        if (appContext == null) return;
        appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .edit().putInt("role", role).apply();
    }

    public static void updateSavedNickname(String nickname) {
        if (appContext == null) return;
        appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .edit().putString("nickname", nickname == null ? "" : nickname).apply();
    }

    public static String getSavedNickname() {
        return appContext.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("nickname", "");
    }

    public static void get(String path, ApiCallback callback) {
        Request request = new Request.Builder()
                .url(url(path))
                .header("Authorization", "Bearer " + getToken())
                .get()
                .build();
        execute(request, callback);
    }

    public static void post(String path, Object bodyObj, ApiCallback callback) {
        String json = gson.toJson(bodyObj);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url(path))
                .header("Authorization", "Bearer " + getToken())
                .post(body)
                .build();
        execute(request, callback);
    }

    /** 无需登录态（注册、找回密码等） */
    public static void postWithoutAuth(String path, Object bodyObj, ApiCallback callback) {
        String json = gson.toJson(bodyObj);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url(path))
                .post(body)
                .build();
        execute(request, callback);
    }

    public static void put(String path, Object bodyObj, ApiCallback callback) {
        String json = gson.toJson(bodyObj);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url(path))
                .header("Authorization", "Bearer " + getToken())
                .put(body)
                .build();
        execute(request, callback);
    }

    /**
     * 上传校园卡照片（multipart），成功后 data 为含 url、filename 的对象。
     */
    public static void uploadCampusCard(Uri imageUri, ApiCallback callback) {
        if (appContext == null) {
            callback.onError("客户端未初始化");
            return;
        }
        client.dispatcher().executorService().execute(() -> {
            try (InputStream is = appContext.getContentResolver().openInputStream(imageUri)) {
                if (is == null) {
                    callback.onError("无法读取图片");
                    return;
                }
                byte[] bytes = readStreamFully(is);
                if (bytes.length == 0) {
                    callback.onError("图片为空");
                    return;
                }
                String mime = appContext.getContentResolver().getType(imageUri);
                if (mime == null || mime.isEmpty()) {
                    mime = "image/jpeg";
                }
                MediaType mediaType = MediaType.parse(mime);
                String filename = "campus-card.jpg";
                if ("image/png".equalsIgnoreCase(mime)) {
                    filename = "campus-card.png";
                } else if ("image/webp".equalsIgnoreCase(mime)) {
                    filename = "campus-card.webp";
                }
                RequestBody fileBody = RequestBody.create(bytes, mediaType);
                RequestBody body = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", filename, fileBody)
                        .build();
                Request request = new Request.Builder()
                        .url(url("/api/upload/campus-card"))
                        .header("Authorization", "Bearer " + getToken())
                        .post(body)
                        .build();
                execute(request, callback);
            } catch (IOException e) {
                callback.onError("读取图片失败：" + e.getMessage());
            }
        });
    }

    private static byte[] readStreamFully(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = is.read(tmp)) != -1) {
            buf.write(tmp, 0, n);
        }
        return buf.toByteArray();
    }

    private static void execute(Request request, ApiCallback callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("网络错误：" + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("请求失败: " + response.code());
                    return;
                }
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
