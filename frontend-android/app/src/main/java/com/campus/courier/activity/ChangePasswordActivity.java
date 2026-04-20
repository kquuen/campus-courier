package com.campus.courier.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

public class ChangePasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("修改密码");

        EditText etOld = findViewById(R.id.etOld);
        EditText etNew = findViewById(R.id.etNew);
        EditText etNew2 = findViewById(R.id.etNew2);
        Button btn = findViewById(R.id.btnSubmit);

        btn.setOnClickListener(v -> {
            String o = etOld.getText().toString();
            String n = etNew.getText().toString();
            String n2 = etNew2.getText().toString();
            if (o.isEmpty() || n.isEmpty()) {
                Toast.makeText(this, "请填写完整", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!n.equals(n2)) {
                Toast.makeText(this, "两次新密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, String> body = new HashMap<>();
            body.put("oldPassword", o);
            body.put("newPassword", n);
            ApiClient.put("/api/user/password", body, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JsonElement data) {
                    runOnUiThread(() -> {
                        Toast.makeText(ChangePasswordActivity.this, "已修改，请重新登录", Toast.LENGTH_SHORT).show();
                        ApiClient.clearLoginInfo();
                        startActivity(new android.content.Intent(ChangePasswordActivity.this, LoginActivity.class));
                        finishAffinity();
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> Toast.makeText(ChangePasswordActivity.this, message, Toast.LENGTH_SHORT).show());
                }
            });
        });
    }
}
