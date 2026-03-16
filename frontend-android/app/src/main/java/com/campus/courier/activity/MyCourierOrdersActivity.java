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

public class MyCourierOrdersActivity extends AppCompatActivity {

    private OrderAdapter adapter;
    private final List<JsonObject> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_list);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("我接的订单");

        TextView tvEmpty = findViewById(R.id.tvEmpty);
        RecyclerView rv  = findViewById(R.id.recyclerView);
        adapter = new OrderAdapter(list, order -> {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra("orderId", order.get("id").getAsLong());
            intent.putExtra("mode", "courier");
            startActivity(intent);
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        ApiClient.get("/api/order/my-courier", new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                list.clear();
                if (data != null && data.isJsonArray()) {
                    for (JsonElement el : data.getAsJsonArray()) list.add(el.getAsJsonObject());
                }
                runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
            @Override
            public void onError(String m) {
                runOnUiThread(() -> Toast.makeText(MyCourierOrdersActivity.this, m, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
