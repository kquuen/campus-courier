package com.campus.courier.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CourierApplicationTimelineActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_courier_timeline);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("代取审核进度");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        TextView tv = findViewById(R.id.tvTimeline);
        tv.setText("加载中…");

        ApiClient.get("/api/user/courier-application/timeline", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                if (data == null || !data.isJsonObject()) {
                    runOnUiThread(() -> tv.setText("暂无数据"));
                    return;
                }
                JsonObject o = data.getAsJsonObject();
                StringBuilder sb = new StringBuilder();
                int st = o.get("currentStatus").getAsInt();
                String desc = o.get("currentStatusDesc").getAsString();
                sb.append("当前状态：").append(desc).append(" (").append(st).append(")\n\n");

                if (o.has("events") && o.get("events").isJsonArray()) {
                    JsonArray arr = o.get("events").getAsJsonArray();
                    if (arr.size() == 0) {
                        sb.append("暂无流程记录（提交申请后将显示时间线）。");
                    } else {
                        sb.append("时间线：\n\n");
                        for (int i = 0; i < arr.size(); i++) {
                            JsonObject ev = arr.get(i).getAsJsonObject();
                            String at = ev.has("createdAt") && !ev.get("createdAt").isJsonNull()
                                    ? ev.get("createdAt").getAsString() : "";
                            String et = ev.has("eventType") && !ev.get("eventType").isJsonNull()
                                    ? ev.get("eventType").getAsString() : "";
                            String remark = ev.has("remark") && !ev.get("remark").isJsonNull()
                                    ? ev.get("remark").getAsString() : "";
                            sb.append(at).append("  ").append(labelEvent(et)).append("\n")
                                    .append(remark).append("\n\n");
                        }
                    }
                }
                String text = sb.toString();
                runOnUiThread(() -> tv.setText(text));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    tv.setText(message);
                    Toast.makeText(CourierApplicationTimelineActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private static String labelEvent(String type) {
        if ("SUBMITTED".equals(type)) return "提交申请";
        if ("APPROVED".equals(type)) return "审核通过";
        if ("REJECTED".equals(type)) return "已驳回";
        return type == null ? "" : type;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
