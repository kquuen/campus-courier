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

/**
 * 登录活动类 - 处理用户登录界面和逻辑
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etPhone, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置布局为 activity_login.xml
        setContentView(R.layout.activity_login);

        // 初始化视图控件
        etPhone    = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin    = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvRegister);
        TextView tvForgot   = findViewById(R.id.tvForgot);

        // 绑定登录按钮点击事件
        btnLogin.setOnClickListener(v -> doLogin());
        // 绑定注册文本点击事件，跳转到注册页面
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        tvForgot.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    /**
     * 执行登录操作
     * 验证输入后调用 API 进行登录，保存登录信息并跳转到主页
     */
    private void doLogin() {
        // 获取输入的手机号和密码
        String phone = etPhone.getText().toString().trim();
        String pwd   = etPassword.getText().toString();
        
        // 验证输入不能为空
        if (phone.isEmpty() || pwd.isEmpty()) {
            Toast.makeText(this, "手机号和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 构建请求参数
        Map<String, String> body = new HashMap<>();
        body.put("phone", phone);
        body.put("password", pwd);

        // 调用后端登录接口
        ApiClient.post("/api/user/login", body, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                // 解析响应数据
                JsonObject obj = data.getAsJsonObject();
                String token   = obj.get("token").getAsString();
                long userId    = obj.get("userId").getAsLong();
                int role       = obj.get("role").getAsInt();
                String nickname = obj.get("nickname").getAsString();

                // 保存登录信息到本地存储
                ApiClient.saveLoginInfo(token, userId, role, nickname);
                
                // 切换到主线程，跳转到主页并关闭当前页面
                runOnUiThread(() -> {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                // 切换到主线程显示错误提示
                runOnUiThread(() ->
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
