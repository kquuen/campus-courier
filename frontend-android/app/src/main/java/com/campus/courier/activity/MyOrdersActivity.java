package com.campus.courier.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

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

public class MyOrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private OrderAdapter adapter;
    private final List<JsonObject> orderList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("我的订单");
        }

        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        RadioGroup rgListMode = findViewById(R.id.rgListMode);
        rgListMode.setVisibility(View.GONE);

        adapter = new OrderAdapter(orderList, order -> {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra("orderId", order.get("id").getAsLong());
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadOrders);
        loadOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOrders();
    }

    private void loadOrders() {
        showLoading();
        ApiClient.get("/api/order/my-published", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                List<JsonObject> loadedOrders = parseOrders(data);
                runOnUiThread(() -> {
                    orderList.clear();
                    orderList.addAll(loadedOrders);
                    adapter.notifyDataSetChanged();
                    tvEmpty.setText(orderList.isEmpty() ? "暂无已发布订单" : "");
                    tvEmpty.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
                    finishLoading();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    orderList.clear();
                    adapter.notifyDataSetChanged();
                    tvEmpty.setText(message == null || message.trim().isEmpty() ? "加载失败，请稍后重试" : message);
                    tvEmpty.setVisibility(View.VISIBLE);
                    finishLoading();
                    Toast.makeText(MyOrdersActivity.this, tvEmpty.getText(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private List<JsonObject> parseOrders(JsonElement data) {
        List<JsonObject> parsedOrders = new ArrayList<>();
        if (data == null || data.isJsonNull()) {
            return parsedOrders;
        }

        if (data.isJsonArray()) {
            JsonArray arr = data.getAsJsonArray();
            for (JsonElement element : arr) {
                if (element != null && element.isJsonObject()) {
                    parsedOrders.add(element.getAsJsonObject());
                }
            }
            return parsedOrders;
        }

        if (data.isJsonObject()) {
            JsonObject obj = data.getAsJsonObject();
            if (obj.has("records") && obj.get("records").isJsonArray()) {
                JsonArray records = obj.getAsJsonArray("records");
                for (JsonElement element : records) {
                    if (element != null && element.isJsonObject()) {
                        parsedOrders.add(element.getAsJsonObject());
                    }
                }
            }
        }
        return parsedOrders;
    }

    private void showLoading() {
        runOnUiThread(() -> {
            if (!swipeRefresh.isRefreshing()) {
                progressBar.setVisibility(View.VISIBLE);
            }
            tvEmpty.setVisibility(View.GONE);
        });
    }

    private void finishLoading() {
        progressBar.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
    }
}
