package com.campus.courier.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TextView tvWelcome = findViewById(R.id.tvWelcome);
        tvWelcome.setText("你好，" + ApiClient.getSavedNickname());

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
            } else if (id == R.id.nav_orders) {
                startActivity(new Intent(this, MyOrdersActivity.class));
            } else if (id == R.id.nav_courier) {
                if (ApiClient.getSavedRole() >= 1) {
                    startActivity(new Intent(this, PendingOrdersActivity.class));
                } else {
                    android.widget.Toast.makeText(this, "请先申请成为代取员", android.widget.Toast.LENGTH_SHORT).show();
                }
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            }
            return true;
        });

        // 管理员入口
        if (ApiClient.getSavedRole() == 2) {
            Button btnAdmin = findViewById(R.id.btnAdmin);
            btnAdmin.setVisibility(android.view.View.VISIBLE);
            btnAdmin.setOnClickListener(v ->
                    new android.app.AlertDialog.Builder(this)
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            ApiClient.post("/api/user/logout", new Object(), new ApiClient.ApiCallback() {
                @Override public void onSuccess(com.google.gson.JsonElement d) {}
                @Override public void onError(String m) {}
            });
            ApiClient.clearLoginInfo();
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
