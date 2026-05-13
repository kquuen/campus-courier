package com.campus.courier.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private ActivityResultLauncher<String> cardPickLauncher;
    private android.net.Uri pendingCourierCardUri;
    private TextView applyDialogCardHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("个人中心");

        cardPickLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    pendingCourierCardUri = uri;
                    if (applyDialogCardHint != null) {
                        applyDialogCardHint.setText(uri == null
                                ? "校园卡照片（选填）：未选择"
                                : "校园卡照片（选填）：已选择，提交时上传");
                    }
                });

        TextView tvNickname = findViewById(R.id.tvNickname);
        TextView tvPhone = findViewById(R.id.tvPhone);
        TextView tvRole = findViewById(R.id.tvRole);
        TextView tvCredit = findViewById(R.id.tvCredit);
        TextView tvBalance = findViewById(R.id.tvBalance);
        Button btnApply = findViewById(R.id.btnApplyCourier);
        Button btnMyCourier = findViewById(R.id.btnMyCourierOrders);
        Button btnTimeline = findViewById(R.id.btnCourierTimeline);
        Button btnEdit = findViewById(R.id.btnEditProfile);
        Button btnPwd = findViewById(R.id.btnChangePassword);

        btnPwd.setOnClickListener(v -> startActivity(new Intent(this, ChangePasswordActivity.class)));

        btnTimeline.setOnClickListener(v ->
                startActivity(new Intent(this, CourierApplicationTimelineActivity.class)));

        // 加载个人信息
        ApiClient.get("/api/user/profile", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                JsonObject u = data.getAsJsonObject();
                runOnUiThread(() -> {
                    String nick = u.get("nickname").getAsString();
                    tvNickname.setText(nick);
                    tvPhone.setText("手机号：" + u.get("phone").getAsString());
                    int role = u.get("role").getAsInt();
                    ApiClient.updateSavedRole(role);
                    tvRole.setText("身份：" + (role == 0 ? "普通用户" : role == 1 ? "代取员" : "管理员"));
                    tvCredit.setText("信用分：" + u.get("creditScore").getAsString());
                    tvBalance.setText("余额：¥" + u.get("balance").getAsString());

                    int audit = u.has("courierAuditStatus") && !u.get("courierAuditStatus").isJsonNull()
                            ? u.get("courierAuditStatus").getAsInt() : 0;

                    btnTimeline.setVisibility(audit != 0 ? android.view.View.VISIBLE : android.view.View.GONE);

                    btnApply.setEnabled(true);
                    if (role == 1) {
                        btnApply.setVisibility(android.view.View.GONE);
                        btnMyCourier.setVisibility(android.view.View.VISIBLE);
                    } else if (audit == 1) {
                        btnApply.setVisibility(android.view.View.VISIBLE);
                        btnApply.setText("资质审核中…");
                        btnApply.setEnabled(false);
                        btnMyCourier.setVisibility(android.view.View.GONE);
                    } else {
                        btnApply.setVisibility(android.view.View.VISIBLE);
                        btnApply.setText(audit == 3 ? "重新申请代取员" : "申请成为代取员");
                        btnMyCourier.setVisibility(android.view.View.GONE);
                    }

                    final String avatarVal = (u.has("avatar") && !u.get("avatar").isJsonNull())
                            ? u.get("avatar").getAsString() : "";
                    btnEdit.setOnClickListener(v -> {
                        Intent it = new Intent(ProfileActivity.this, EditProfileActivity.class);
                        it.putExtra("nickname", nick);
                        it.putExtra("avatar", avatarVal);
                        startActivity(it);
                    });
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                        Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });

        btnApply.setOnClickListener(v -> showApplyDialog(btnApply));

        btnMyCourier.setOnClickListener(v -> {
            Intent intent = new Intent(this, MyCourierOrdersActivity.class);
            startActivity(intent);
        });
    }

    private void showApplyDialog(Button btnApply) {
        pendingCourierCardUri = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("申请成为代取员");
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_apply_courier, null);
        builder.setView(dialogView);
        EditText etRealName = dialogView.findViewById(R.id.etRealName);
        EditText etStudentId = dialogView.findViewById(R.id.etStudentId);
        applyDialogCardHint = dialogView.findViewById(R.id.tvCardStatus);
        Button btnPick = dialogView.findViewById(R.id.btnPickCard);
        btnPick.setOnClickListener(v -> cardPickLauncher.launch("image/*"));

        builder.setPositiveButton("提交", null);
        builder.setNegativeButton("取消", null);
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> applyDialogCardHint = null);
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String realName = etRealName.getText().toString().trim();
            String studentId = etStudentId.getText().toString().trim();
            if (realName.length() < 2) {
                Toast.makeText(this, "请填写真实姓名", Toast.LENGTH_SHORT).show();
                return;
            }
            if (studentId.length() < 5 || !studentId.matches("^[A-Za-z0-9]{5,32}$")) {
                Toast.makeText(this, "学号须为5～32位字母或数字", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, String> body = new HashMap<>();
            body.put("realName", realName);
            body.put("studentId", studentId);

            Runnable submitApply = () -> ApiClient.post("/api/user/apply-courier", body, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JsonElement data) {
                    runOnUiThread(() -> {
                        Toast.makeText(ProfileActivity.this, "已提交，请等待审核", Toast.LENGTH_SHORT).show();
                        btnApply.setText("资质审核中…");
                        btnApply.setEnabled(false);
                        dialog.dismiss();
                    });
                }

                @Override
                public void onError(String m) {
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this, m, Toast.LENGTH_SHORT).show());
                }
            });

            if (pendingCourierCardUri != null) {
                ApiClient.uploadCampusCard(pendingCourierCardUri, new ApiClient.ApiCallback() {
                    @Override
                    public void onSuccess(JsonElement data) {
                        if (data != null && data.isJsonObject()) {
                            String url = data.getAsJsonObject().get("url").getAsString();
                            body.put("campusCardImageUrl", url);
                        }
                        submitApply.run();
                    }

                    @Override
                    public void onError(String m) {
                        runOnUiThread(() -> Toast.makeText(ProfileActivity.this, m, Toast.LENGTH_SHORT).show());
                    }
                });
            } else {
                submitApply.run();
            }
        });
    }
}
