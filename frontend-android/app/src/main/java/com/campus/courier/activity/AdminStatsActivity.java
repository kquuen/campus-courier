package com.campus.courier.activity;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AdminStatsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_stats);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("运营数据统计");

        TextView tv = findViewById(R.id.tvStats);

        ApiClient.get("/api/admin/stats", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                JsonObject o = data.getAsJsonObject();
                String text = "注册用户总数：" + o.get("userCount").getAsLong() + "\n"
                        + "订单总数：" + o.get("orderCount").getAsLong() + "\n"
                        + "成功支付笔数：" + o.get("paidOrderCount").getAsLong() + "\n"
                        + "成功支付总金额：¥" + o.get("totalPaidAmount").getAsString();
                runOnUiThread(() -> tv.setText(text));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(AdminStatsActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
