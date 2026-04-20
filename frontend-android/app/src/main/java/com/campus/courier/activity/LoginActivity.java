package com.campus.courier.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.campus.courier.util.LoadingStateHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 登录活动类 - 处理用户登录界面和逻辑
 * 优化版本：添加输入验证、加载状态、实时反馈
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etPhone, etPassword;
    private TextInputLayout tilPhone, tilPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^.{6,20}$");

    private View loadingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置布局为 activity_login.xml
        setContentView(R.layout.activity_login);

        // 初始化视图控件
        initViews();

        // 设置输入监听
        setupInputListeners();

        // 绑定按钮点击事件
        setupButtonClickListeners();
    }

    /**
     * 初始化所有视图控件
     */
    private void initViews() {
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        // 设置初始状态
        updateLoginButtonState();
    }

    /**
     * 设置输入监听器
     */
    private void setupInputListeners() {
        // 手机号输入监听
        etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateAccount(s.toString());
                updateLoginButtonState();
            }
        });

        // 密码输入监听
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validatePassword(s.toString());
                updateLoginButtonState();
            }
        });
    }

    /**
     * 设置按钮点击监听器
     */
    private void setupButtonClickListeners() {
        // 登录按钮
        btnLogin.setOnClickListener(v -> doLogin());

        // 注册链接
        findViewById(R.id.tvRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        // 忘记密码链接
        findViewById(R.id.tvForgot).setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    /**
     * 验证账号格式（支持手机号、用户名、邮箱）
     */
    private void validateAccount(String account) {
        if (account.isEmpty()) {
            tilPhone.setError("账号不能为空");
        } else if (PHONE_PATTERN.matcher(account).matches() 
                || USERNAME_PATTERN.matcher(account).matches() 
                || EMAIL_PATTERN.matcher(account).matches()) {
            tilPhone.setError(null);
        } else {
            tilPhone.setError("请输入有效的手机号或用户名（3-20位字母、数字、下划线）");
        }
    }

    /**
     * 验证密码格式
     */
    private void validatePassword(String password) {
        if (password.isEmpty()) {
            tilPassword.setError("密码不能为空");
        } else if (!PASSWORD_PATTERN.matcher(password).matches()) {
            tilPassword.setError("密码长度需为6-20位");
        } else {
            tilPassword.setError(null);
        }
    }

    /**
     * 更新登录按钮状态
     */
    private void updateLoginButtonState() {
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();

        boolean isPhoneValid = !phone.isEmpty() && 
                (PHONE_PATTERN.matcher(phone).matches() || 
                 USERNAME_PATTERN.matcher(phone).matches() || 
                 EMAIL_PATTERN.matcher(phone).matches());
        boolean isPasswordValid = !password.isEmpty() && PASSWORD_PATTERN.matcher(password).matches();

        btnLogin.setEnabled(isPhoneValid && isPasswordValid);
        btnLogin.setAlpha(isPhoneValid && isPasswordValid ? 1f : 0.5f);
    }

    /**
     * 执行登录操作
     */
    private void doLogin() {
        // 获取输入的手机号和密码
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();

        // 最终验证
        if (!PHONE_PATTERN.matcher(phone).matches() && 
            !USERNAME_PATTERN.matcher(phone).matches() && 
            !EMAIL_PATTERN.matcher(phone).matches()) {
            tilPhone.setError("请输入有效的手机号或用户名（3-20位字母、数字、下划线）");
            return;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            tilPassword.setError("密码长度需为6-20位");
            return;
        }

        // 显示加载状态
        showLoading(true);

        // 构建请求参数
        Map<String, String> body = new HashMap<>();
        body.put("phone", phone);
        body.put("password", password);

        // 调用后端登录接口
        ApiClient.post("/api/user/login", body, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                // 解析响应数据
                JsonObject obj = data.getAsJsonObject();
                String token = obj.get("token").getAsString();
                long userId = obj.get("userId").getAsLong();
                int role = obj.get("role").getAsInt();
                String nickname = obj.get("nickname").getAsString();

                // 保存登录信息到本地存储
                ApiClient.saveLoginInfo(token, userId, role, nickname);

                // 切换到主线程，跳转到主页并关闭当前页面
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showSuccessSnackbar(btnLogin, "登录成功！");

                    // 延迟跳转，让用户看到成功提示
                    btnLogin.postDelayed(() -> {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    }, 500);
                });
            }

            @Override
            public void onError(String message) {
                // 切换到主线程显示错误提示
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showErrorSnackbar(btnLogin, "登录失败: " + message);
                });
            }
        });
    }

    /**
     * 显示/隐藏加载状态
     */
    private void showLoading(boolean show) {
        if (show) {
            // 显示加载覆盖层
            loadingOverlay = LoadingStateHelper.showFullScreenLoading(this);
            progressBar.setVisibility(View.VISIBLE);
            btnLogin.setEnabled(false);
        } else {
            // 隐藏加载覆盖层
            if (loadingOverlay != null) {
                LoadingStateHelper.hideFullScreenLoading(this, loadingOverlay);
                loadingOverlay = null;
            }
            progressBar.setVisibility(View.GONE);
            updateLoginButtonState();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清理加载状态
        if (loadingOverlay != null) {
            LoadingStateHelper.hideFullScreenLoading(this, loadingOverlay);
        }
    }
}
