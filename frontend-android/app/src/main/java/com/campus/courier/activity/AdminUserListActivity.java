package com.campus.courier.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class AdminUserListActivity extends AppCompatActivity {

    private final List<JsonObject> userList = new ArrayList<>();
    private RecyclerView.Adapter<RecyclerView.ViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("用户管理");

        TextView tvEmpty  = findViewById(R.id.tvEmpty);
        RecyclerView rv   = findViewById(R.id.recyclerView);

        adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup p, int t) {
                TextView tv = new TextView(AdminUserListActivity.this);
                tv.setPadding(32, 24, 32, 24);
                tv.setTextSize(15);
                return new RecyclerView.ViewHolder(tv) {};
            }
            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {
                JsonObject u = userList.get(pos);
                String nick   = u.has("nickname") && !u.get("nickname").isJsonNull()
                        ? u.get("nickname").getAsString() : "—";
                String phone  = u.get("phone").getAsString();
                int role      = u.get("role").getAsInt();
                int status    = u.get("status").getAsInt();
                String roleStr = role == 0 ? "用户" : role == 1 ? "代取员" : "管理员";
                String statStr = status == 1 ? "正常" : "禁用";

                ((TextView) h.itemView).setText(
                        nick + "  " + phone + "\n角色：" + roleStr + "  状态：" + statStr);

                h.itemView.setOnLongClickListener(v -> {
                    long userId = u.get("id").getAsLong();
                    int newStatus = status == 1 ? 0 : 1;
                    String action = newStatus == 0 ? "禁用" : "启用";
                    new android.app.AlertDialog.Builder(AdminUserListActivity.this)
                            .setTitle(action + "用户")
                            .setMessage("确认" + action + " " + nick + " 吗？")
                            .setPositiveButton("确认", (d, w) ->
                                    ApiClient.put("/api/user/" + userId + "/status?status=" + newStatus,
                                            new Object(), new ApiClient.ApiCallback() {
                                @Override public void onSuccess(JsonElement data) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(AdminUserListActivity.this, action + "成功", Toast.LENGTH_SHORT).show();
                                        loadUsers();
                                    });
                                }
                                @Override public void onError(String m) {
                                    runOnUiThread(() -> Toast.makeText(AdminUserListActivity.this, m, Toast.LENGTH_SHORT).show());
                                }
                            }))
                            .setNegativeButton("取消", null).show();
                    return true;
                });
            }
            @Override
            public int getItemCount() { return userList.size(); }
        };

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        loadUsers();
    }

    private void loadUsers() {
        ApiClient.get("/api/user/list?page=1&size=50", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                userList.clear();
                if (data != null && data.isJsonObject()) {
                    JsonObject page = data.getAsJsonObject();
                    if (page.has("records")) {
                        JsonArray arr = page.getAsJsonArray("records");
                        for (JsonElement el : arr) userList.add(el.getAsJsonObject());
                    }
                }
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            }
            @Override
            public void onError(String m) {
                runOnUiThread(() -> Toast.makeText(AdminUserListActivity.this, m, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
