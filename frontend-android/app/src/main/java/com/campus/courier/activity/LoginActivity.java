package com.campus.courier.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etPhone, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etPhone    = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin    = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(v -> doLogin());
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void doLogin() {
        String phone = etPhone.getText().toString().trim();
        String pwd   = etPassword.getText().toString();
        if (phone.isEmpty() || pwd.isEmpty()) {
            Toast.makeText(this, "手机号和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("phone", phone);
        body.put("password", pwd);

        ApiClient.post("/api/user/login", body, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                JsonObject obj = data.getAsJsonObject();
                String token   = obj.get("token").getAsString();
                long userId    = obj.get("userId").getAsLong();
                int role       = obj.get("role").getAsInt();
                String nickname = obj.get("nickname").getAsString();

                ApiClient.saveLoginInfo(token, userId, role, nickname);
                runOnUiThread(() -> {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
