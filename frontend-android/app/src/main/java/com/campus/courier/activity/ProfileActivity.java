package com.campus.courier.activity;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("个人中心");

        TextView tvNickname    = findViewById(R.id.tvNickname);
        TextView tvPhone       = findViewById(R.id.tvPhone);
        TextView tvRole        = findViewById(R.id.tvRole);
        TextView tvCredit      = findViewById(R.id.tvCredit);
        TextView tvBalance     = findViewById(R.id.tvBalance);
        Button   btnApply      = findViewById(R.id.btnApplyCourier);
        Button   btnMyCourier  = findViewById(R.id.btnMyCourierOrders);

        // 加载个人信息
        ApiClient.get("/api/user/profile", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                JsonObject u = data.getAsJsonObject();
                runOnUiThread(() -> {
                    tvNickname.setText(u.get("nickname").getAsString());
                    tvPhone.setText("手机号：" + u.get("phone").getAsString());
                    int role = u.get("role").getAsInt();
                    tvRole.setText("身份：" + (role == 0 ? "普通用户" : role == 1 ? "代取员" : "管理员"));
                    tvCredit.setText("信用分：" + u.get("creditScore").getAsString());
                    tvBalance.setText("余额：¥" + u.get("balance").getAsString());

                    if (role == 0) {
                        btnApply.setVisibility(android.view.View.VISIBLE);
                    } else {
                        btnMyCourier.setVisibility(android.view.View.VISIBLE);
                    }
                });
            }
            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                    Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });

        // 申请成为代取员
        btnApply.setOnClickListener(v -> showApplyDialog(btnApply, btnMyCourier));

        // 查看我接的订单
        btnMyCourier.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, MyCourierOrdersActivity.class);
            startActivity(intent);
        });
    }

    private void showApplyDialog(Button btnApply, Button btnMyCourier) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("申请成为代取员");
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_apply_courier, null);
        builder.setView(dialogView);
        EditText etRealName  = dialogView.findViewById(R.id.etRealName);
        EditText etStudentId = dialogView.findViewById(R.id.etStudentId);
        builder.setPositiveButton("提交", (d, w) -> {
            Map<String, String> body = new HashMap<>();
            body.put("realName",  etRealName.getText().toString().trim());
            body.put("studentId", etStudentId.getText().toString().trim());
            ApiClient.post("/api/user/apply-courier", body, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JsonElement data) {
                    runOnUiThread(() -> {
                        Toast.makeText(ProfileActivity.this, "申请成功！", Toast.LENGTH_SHORT).show();
                        btnApply.setVisibility(android.view.View.GONE);
                        btnMyCourier.setVisibility(android.view.View.VISIBLE);
                    });
                }
                @Override
                public void onError(String m) {
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this, m, Toast.LENGTH_SHORT).show());
                }
            });
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
}
