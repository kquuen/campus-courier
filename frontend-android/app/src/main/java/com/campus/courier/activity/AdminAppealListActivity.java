package com.campus.courier.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员查看异常/申诉订单并仲裁
 */
public class AdminAppealListActivity extends AppCompatActivity {

    private final List<JsonObject> rows = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private final List<String> labels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_appeals);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("异常订单仲裁");

        ListView listView = findViewById(R.id.listView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((p, v, position, id) -> {
            JsonObject o = rows.get(position);
            long oid = o.get("id").getAsLong();
            String no = o.get("orderNo").getAsString();
            new android.app.AlertDialog.Builder(this)
                    .setTitle("订单 " + no)
                    .setItems(new String[]{"裁定为：已完成(3)", "裁定为：已取消(4)"}, (d, which) -> {
                        int target = which == 0 ? 3 : 4;
                        EditText et = new EditText(this);
                        et.setHint("仲裁说明（可选）");
                        new android.app.AlertDialog.Builder(this)
                                .setTitle("确认仲裁")
                                .setView(et)
                                .setPositiveButton("提交", (d2, w) -> {
                                    Map<String, Object> body = new HashMap<>();
                                    body.put("targetStatus", target);
                                    body.put("remark", et.getText().toString().trim());
                                    ApiClient.post("/api/admin/orders/" + oid + "/arbitrate", body,
                                            new ApiClient.ApiCallback() {
                                                @Override
                                                public void onSuccess(JsonElement data) {
                                                    runOnUiThread(() -> {
                                                        Toast.makeText(AdminAppealListActivity.this, "已处理", Toast.LENGTH_SHORT).show();
                                                        load();
                                                    });
                                                }

                                                @Override
                                                public void onError(String message) {
                                                    runOnUiThread(() -> Toast.makeText(AdminAppealListActivity.this, message, Toast.LENGTH_SHORT).show());
                                                }
                                            });
                                })
                                .setNegativeButton("取消", null)
                                .show();
                    })
                    .show();
        });

        load();
    }

    private void load() {
        ApiClient.get("/api/order/admin/list?page=1&size=50&status=5", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                rows.clear();
                labels.clear();
                if (data != null && data.isJsonObject()) {
                    JsonObject page = data.getAsJsonObject();
                    if (page.has("records")) {
                        JsonArray arr = page.getAsJsonArray("records");
                        for (JsonElement el : arr) {
                            JsonObject o = el.getAsJsonObject();
                            rows.add(o);
                            String reason = o.has("appealReason") && !o.get("appealReason").isJsonNull()
                                    ? o.get("appealReason").getAsString() : "无";
                            labels.add(o.get("orderNo").getAsString() + "\n申诉：" + reason);
                        }
                    }
                }
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(AdminAppealListActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
