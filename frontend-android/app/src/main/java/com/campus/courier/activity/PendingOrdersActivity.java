package com.campus.courier.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.campus.courier.R;
import com.campus.courier.adapter.OrderAdapter;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class PendingOrdersActivity extends AppCompatActivity {

    private OrderAdapter adapter;
    private final List<JsonObject> orderList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("待接单列表");

        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvEmpty      = findViewById(R.id.tvEmpty);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        adapter = new OrderAdapter(orderList, order -> {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra("orderId", order.get("id").getAsLong());
            intent.putExtra("mode", "courier");  // 代取员模式
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadPendingOrders);
        loadPendingOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPendingOrders();
    }

    private void loadPendingOrders() {
        ApiClient.get("/api/order/pending?page=1&size=20", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                orderList.clear();
                if (data != null && data.isJsonObject()) {
                    JsonObject page = data.getAsJsonObject();
                    if (page.has("records")) {
                        JsonArray records = page.getAsJsonArray("records");
                        for (JsonElement el : records) orderList.add(el.getAsJsonObject());
                    }
                }
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
                    swipeRefresh.setRefreshing(false);
                });
            }
            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(PendingOrdersActivity.this, message, Toast.LENGTH_SHORT).show();
                    swipeRefresh.setRefreshing(false);
                });
            }
        });
    }
}
