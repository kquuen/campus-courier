package com.campus.courier.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("忘记密码");

        EditText etPhone = findViewById(R.id.etPhone);
        EditText etCode = findViewById(R.id.etCode);
        EditText etNew = findViewById(R.id.etNewPwd);
        Button btnSend = findViewById(R.id.btnSendCode);
        Button btnReset = findViewById(R.id.btnReset);

        btnSend.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            if (phone.isEmpty()) {
                Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, String> body = new HashMap<>();
            body.put("phone", phone);
            ApiClient.postWithoutAuth("/api/user/password/forgot", body, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JsonElement data) {
                    runOnUiThread(() -> {
                        String msg = "验证码已发送";
                        if (data != null && data.isJsonObject()) {
                            JsonObject o = data.getAsJsonObject();
                            if (o.has("debugCode")) {
                                msg += "（开发环境验证码：" + o.get("debugCode").getAsString() + "）";
                            }
                        }
                        Toast.makeText(ForgotPasswordActivity.this, msg, Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_SHORT).show());
                }
            });
        });

        btnReset.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            String code = etCode.getText().toString().trim();
            String pwd = etNew.getText().toString();
            if (phone.isEmpty() || code.isEmpty() || pwd.isEmpty()) {
                Toast.makeText(this, "请填写完整", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, String> body = new HashMap<>();
            body.put("phone", phone);
            body.put("code", code);
            body.put("newPassword", pwd);
            ApiClient.postWithoutAuth("/api/user/password/reset", body, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JsonElement data) {
                    runOnUiThread(() -> {
                        Toast.makeText(ForgotPasswordActivity.this, "重置成功，请登录", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_SHORT).show());
                }
            });
        });
    }
}
