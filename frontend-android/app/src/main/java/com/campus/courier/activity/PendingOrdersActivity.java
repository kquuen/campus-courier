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

public class PendingOrdersActivity extends AppCompatActivity {

    private OrderAdapter adapter;
    private final List<JsonObject> orderList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private boolean recommendMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("接单大厅");
        }

        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        RadioGroup rgListMode = findViewById(R.id.rgListMode);
        rgListMode.setVisibility(View.VISIBLE);

        adapter = new OrderAdapter(orderList, order -> {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra("orderId", order.get("id").getAsLong());
            intent.putExtra("mode", "courier");
            startActivity(intent);
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        rgListMode.setOnCheckedChangeListener((g, id) -> {
            recommendMode = id == R.id.rbRecommend;
            loadPendingOrders();
        });

        swipeRefresh.setOnRefreshListener(this::loadPendingOrders);
        loadPendingOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPendingOrders();
    }

    private void loadPendingOrders() {
        showLoading();
        String path = recommendMode
                ? "/api/order/recommend?page=1&size=20"
                : "/api/order/pending?page=1&size=20";

        ApiClient.get(path, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                List<JsonObject> loadedOrders = parsePendingOrders(data);
                runOnUiThread(() -> {
                    orderList.clear();
                    orderList.addAll(loadedOrders);
                    adapter.notifyDataSetChanged();
                    tvEmpty.setText(orderList.isEmpty()
                            ? (recommendMode ? "暂无推荐订单" : "暂无可接订单")
                            : "");
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
                    Toast.makeText(PendingOrdersActivity.this, tvEmpty.getText(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private List<JsonObject> parsePendingOrders(JsonElement data) {
        List<JsonObject> parsedOrders = new ArrayList<>();
        if (data == null || data.isJsonNull() || !data.isJsonObject()) {
            return parsedOrders;
        }

        JsonObject page = data.getAsJsonObject();
        if (!page.has("records") || !page.get("records").isJsonArray()) {
            return parsedOrders;
        }

        JsonArray records = page.getAsJsonArray("records");
        for (JsonElement element : records) {
            if (element != null && element.isJsonObject()) {
                parsedOrders.add(element.getAsJsonObject());
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
