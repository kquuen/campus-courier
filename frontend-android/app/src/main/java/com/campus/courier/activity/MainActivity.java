package com.campus.courier.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.campus.courier.util.LoadingStateHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MainActivity extends AppCompatActivity {

    private TextView tvOrderCount, tvCreditScore, tvEarnings;
    private MaterialButton btnQuickPublish, btnQuickOrders, btnQuickCourier, btnQuickProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initViews();
        setupClickListeners();
        loadDashboardData();

        // 设置首页为选中状态
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setSelectedItemId(R.id.nav_home);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到主页时刷新数据
        loadDashboardData();
    }

    private void initViews() {
        // 欢迎文本
        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("你好，" + ApiClient.getSavedNickname());

        // 数据统计视图
        tvOrderCount = findViewById(R.id.tvOrderCount);
        tvCreditScore = findViewById(R.id.tvCreditScore);
        tvEarnings = findViewById(R.id.tvEarnings);

        // 快捷操作按钮
        btnQuickPublish = findViewById(R.id.btnQuickPublish);
        btnQuickOrders = findViewById(R.id.btnQuickOrders);
        btnQuickCourier = findViewById(R.id.btnQuickCourier);
        btnQuickProfile = findViewById(R.id.btnQuickProfile);

        // 发布需求按钮
        FloatingActionButton fabPublish = findViewById(R.id.fabPublish);
        fabPublish.setOnClickListener(v ->
                startActivity(new Intent(this, PublishOrderActivity.class)));

        // 底部导航
        BottomNavigationView nav = findViewById(R.id.bottomNav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // 首页（当前页）
                return true;
            } else if (id == R.id.nav_orders) {
                startActivity(new Intent(this, MyOrdersActivity.class));
                return true;
            } else if (id == R.id.nav_courier) {
                if (ApiClient.getSavedRole() >= 1) {
                    startActivity(new Intent(this, PendingOrdersActivity.class));
                } else {
                    LoadingStateHelper.showInfoSnackbar(nav, "请先申请成为代取员");
                }
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        // 管理员入口
        if (ApiClient.getSavedRole() == 2) {
            com.google.android.material.button.MaterialButton btnAdmin = findViewById(R.id.btnAdmin);
            btnAdmin.setVisibility(View.VISIBLE);
            btnAdmin.setOnClickListener(v ->
                    new android.app.AlertDialog.Builder(this, R.style.Dialog_CampusCourier)
                            .setTitle("管理后台")
                            .setItems(new String[]{
                                    "用户账号管理",
                                    "代取员资质审核",
                                    "异常订单仲裁",
                                    "运营数据统计"
                            }, (d, which) -> {
                                Intent intent = null;
                                if (which == 0) intent = new Intent(this, AdminUserListActivity.class);
                                else if (which == 1) intent = new Intent(this, AdminCourierAuditActivity.class);
                                else if (which == 2) intent = new Intent(this, AdminAppealListActivity.class);
                                else if (which == 3) intent = new Intent(this, AdminStatsActivity.class);
                                if (intent != null) startActivity(intent);
                            })
                            .show());
        }
    }

    private void setupClickListeners() {
        // 快捷操作按钮
        btnQuickPublish.setOnClickListener(v ->
                startActivity(new Intent(this, PublishOrderActivity.class)));

        btnQuickOrders.setOnClickListener(v ->
                startActivity(new Intent(this, MyOrdersActivity.class)));

        btnQuickCourier.setOnClickListener(v -> {
            if (ApiClient.getSavedRole() >= 1) {
                startActivity(new Intent(this, PendingOrdersActivity.class));
            } else {
                LoadingStateHelper.showInfoSnackbar(btnQuickCourier, "请先申请成为代取员");
            }
        });

        btnQuickProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void loadDashboardData() {
        // 显示加载状态
        tvOrderCount.setText("...");
        tvCreditScore.setText("...");
        tvEarnings.setText("...");

        // 加载订单统计数据
        ApiClient.get("/api/order/list?page=1&size=1", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                if (data != null && data.isJsonObject()) {
                    JsonObject obj = data.getAsJsonObject();
                    int total = obj.has("total") ? obj.get("total").getAsInt() : 0;

                    runOnUiThread(() -> {
                        tvOrderCount.setText(String.valueOf(total));
                    });
                }
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    tvOrderCount.setText("0");
                });
            }
        });

        // 加载用户信用分（从用户资料接口）
        ApiClient.get("/api/user/profile", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                if (data != null && data.isJsonObject()) {
                    JsonObject profile = data.getAsJsonObject();
                    int credit = profile.has("creditScore") ? profile.get("creditScore").getAsInt() : 80;
                    runOnUiThread(() -> {
                        tvCreditScore.setText(String.valueOf(credit));
                    });
                }
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    tvCreditScore.setText("80"); // 默认信用分
                });
            }
        });

        // 加载代取员收益（如果是代取员）
        if (ApiClient.getSavedRole() >= 1) {
            ApiClient.get("/api/courier/earnings/summary", new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JsonElement data) {
                    if (data != null && data.isJsonObject()) {
                        JsonObject summary = data.getAsJsonObject();
                        double earnings = summary.has("totalEarn") ? summary.get("totalEarn").getAsDouble() : 0.0;
                        runOnUiThread(() -> {
                            tvEarnings.setText(String.format("%.2f", earnings));
                        });
                    }
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        tvEarnings.setText("0.00");
                    });
                }
            });
        } else {
            // 普通用户显示为0
            tvEarnings.setText("0.00");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            LoadingStateHelper.showConfirmDialog(this,
                    "确认退出",
                    "确定要退出登录吗？",
                    "退出",
                    "取消",
                    () -> {
                        ApiClient.post("/api/user/logout", new Object(), new ApiClient.ApiCallback() {
                            @Override public void onSuccess(com.google.gson.JsonElement d) {}
                            @Override public void onError(String m) {}
                        });
                        ApiClient.clearLoginInfo();
                        startActivity(new Intent(this, LoginActivity.class));
                        finishAffinity();
                    },
                    null
            );
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
