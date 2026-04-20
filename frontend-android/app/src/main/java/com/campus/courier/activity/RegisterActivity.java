package com.campus.courier.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText etPhone     = findViewById(R.id.etPhone);
        EditText etRealName  = findViewById(R.id.etRealName);
        EditText etStudentId = findViewById(R.id.etStudentId);
        EditText etNickname  = findViewById(R.id.etNickname);
        EditText etPassword  = findViewById(R.id.etPassword);
        EditText etConfirm   = findViewById(R.id.etConfirm);
        Button  btnRegister  = findViewById(R.id.btnRegister);
        TextView tvLogin     = findViewById(R.id.tvLogin);

        tvLogin.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> {
            String phone    = etPhone.getText().toString().trim();
            String realName = etRealName.getText().toString().trim();
            String sid      = etStudentId.getText().toString().trim();
            String nick     = etNickname.getText().toString().trim();
            String pwd      = etPassword.getText().toString();
            String confirm  = etConfirm.getText().toString();

            if (phone.isEmpty() || pwd.isEmpty()) {
                Toast.makeText(this, "手机号和密码不能为空", Toast.LENGTH_SHORT).show();
                return;
            }
            if (realName.length() < 2) {
                Toast.makeText(this, "请填写真实姓名（至少2个字符）", Toast.LENGTH_SHORT).show();
                return;
            }
            if (sid.length() < 5 || !sid.matches("^[A-Za-z0-9]{5,32}$")) {
                Toast.makeText(this, "学号须为5～32位字母或数字", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!pwd.equals(confirm)) {
                Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, String> body = new HashMap<>();
            body.put("phone",     phone);
            body.put("password",  pwd);
            body.put("studentId", sid);
            body.put("realName",  realName);
            body.put("nickname",  nick);

            ApiClient.postWithoutAuth("/api/user/register", body, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JsonElement data) {
                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this, "注册成功，请登录", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    });
                }
                @Override
                public void onError(String message) {
                    runOnUiThread(() ->
                        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show());
                }
            });
        });
    }
}
