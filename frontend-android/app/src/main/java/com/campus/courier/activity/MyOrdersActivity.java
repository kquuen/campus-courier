package com.campus.courier.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.campus.courier.R;
import com.campus.courier.adapter.OrderAdapter;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class MyOrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private OrderAdapter adapter;
    private final List<JsonObject> orderList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("我的订单");

        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty      = findViewById(R.id.tvEmpty);

        adapter = new OrderAdapter(orderList, order -> {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra("orderId", order.get("id").getAsLong());
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void loadOrders() {
        ApiClient.get("/api/order/my-published", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                orderList.clear();
                if (data != null && data.isJsonArray()) {
                    JsonArray arr = data.getAsJsonArray();
                    for (JsonElement el : arr) orderList.add(el.getAsJsonObject());
                }
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                    Toast.makeText(MyOrdersActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
