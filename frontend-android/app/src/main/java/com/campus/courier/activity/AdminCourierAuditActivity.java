package com.campus.courier.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
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

public class AdminCourierAuditActivity extends AppCompatActivity {

    private final List<JsonObject> rows = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private final List<String> labels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_courier);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("代取员资质审核");

        ListView listView = findViewById(R.id.listView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            JsonObject u = rows.get(position);
            long uid = u.get("id").getAsLong();
            String nick = u.has("nickname") && !u.get("nickname").isJsonNull()
                    ? u.get("nickname").getAsString() : "用户";
            new android.app.AlertDialog.Builder(this)
                    .setTitle("审核：" + nick)
                    .setItems(new String[]{"通过", "驳回"}, (d, which) -> {
                        if (which == 0) {
                            submitAudit(uid, true, null);
                        } else {
                            android.widget.EditText et = new android.widget.EditText(this);
                            et.setHint("驳回原因（可选）");
                            new android.app.AlertDialog.Builder(this)
                                    .setTitle("驳回原因")
                                    .setView(et)
                                    .setPositiveButton("提交", (d2, w) ->
                                            submitAudit(uid, false, et.getText().toString().trim()))
                                    .setNegativeButton("取消", null)
                                    .show();
                        }
                    })
                    .show();
        });

        load();
    }

    private void submitAudit(long userId, boolean approve, String reason) {
        Map<String, Object> body = new HashMap<>();
        body.put("approve", approve);
        if (reason != null && !reason.isEmpty()) {
            body.put("rejectReason", reason);
        }
        ApiClient.post("/api/admin/courier-applications/" + userId, body, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                runOnUiThread(() -> {
                    Toast.makeText(AdminCourierAuditActivity.this, "处理成功", Toast.LENGTH_SHORT).show();
                    load();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(AdminCourierAuditActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void load() {
        ApiClient.get("/api/admin/courier-applications?page=1&size=50", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                rows.clear();
                labels.clear();
                if (data != null && data.isJsonObject()) {
                    JsonObject page = data.getAsJsonObject();
                    if (page.has("records")) {
                        JsonArray arr = page.getAsJsonArray("records");
                        for (JsonElement el : arr) {
                            JsonObject u = el.getAsJsonObject();
                            rows.add(u);
                            String nick = u.has("nickname") && !u.get("nickname").isJsonNull()
                                    ? u.get("nickname").getAsString() : "—";
                            String phone = u.get("phone").getAsString();
                            String rn = u.has("realName") && !u.get("realName").isJsonNull()
                                    ? u.get("realName").getAsString() : "—";
                            String sid = u.has("studentId") && !u.get("studentId").isJsonNull()
                                    ? u.get("studentId").getAsString() : "—";
                            labels.add(nick + "  " + phone + "\n实名：" + rn + "  学号：" + sid);
                        }
                    }
                }
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(AdminCourierAuditActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
