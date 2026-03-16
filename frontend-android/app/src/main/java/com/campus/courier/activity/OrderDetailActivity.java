package com.campus.courier.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class OrderDetailActivity extends AppCompatActivity {

    private long orderId;
    private String mode;   // "user" | "courier"
    private JsonObject currentOrder;

    private TextView tvOrderNo, tvStatus, tvTracking, tvPickup, tvDelivery, tvFee, tvRemark;
    private Button btnAction, btnCancel, btnPay, btnReview;

    // 状态文字映射
    private static final String[] STATUS_TEXT = {
            "待接单", "已接单", "取件中", "已完成", "已取消", "异常"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("订单详情");

        orderId = getIntent().getLongExtra("orderId", -1);
        mode    = getIntent().getStringExtra("mode");
        if (mode == null) mode = "user";

        tvOrderNo  = findViewById(R.id.tvOrderNo);
        tvStatus   = findViewById(R.id.tvStatus);
        tvTracking = findViewById(R.id.tvTracking);
        tvPickup   = findViewById(R.id.tvPickup);
        tvDelivery = findViewById(R.id.tvDelivery);
        tvFee      = findViewById(R.id.tvFee);
        tvRemark   = findViewById(R.id.tvRemark);
        btnAction  = findViewById(R.id.btnAction);
        btnCancel  = findViewById(R.id.btnCancel);
        btnPay     = findViewById(R.id.btnPay);
        btnReview  = findViewById(R.id.btnReview);

        loadDetail();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDetail();
    }

    private void loadDetail() {
        ApiClient.get("/api/order/" + orderId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                currentOrder = data.getAsJsonObject();
                runOnUiThread(() -> renderOrder(currentOrder));
            }
            @Override
            public void onError(String message) {
                runOnUiThread(() ->
                    Toast.makeText(OrderDetailActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void renderOrder(JsonObject o) {
        int status = o.get("status").getAsInt();
        tvOrderNo.setText("订单号：" + o.get("orderNo").getAsString());
        tvStatus.setText("状态：" + STATUS_TEXT[Math.min(status, STATUS_TEXT.length - 1)]);
        tvTracking.setText("快递单号：" + o.get("trackingNo").getAsString());
        tvPickup.setText("取件地址：" + o.get("pickupAddress").getAsString());
        tvDelivery.setText("送达地址：" + o.get("deliveryAddress").getAsString());
        tvFee.setText("代取费：¥" + o.get("fee").getAsString());
        String remark = o.has("remark") && !o.get("remark").isJsonNull()
                ? o.get("remark").getAsString() : "无";
        tvRemark.setText("备注：" + remark);

        // 重置按钮
        btnAction.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        btnPay.setVisibility(View.GONE);
        btnReview.setVisibility(View.GONE);

        long myId = ApiClient.getSavedUserId();
        long publisherId = o.get("publisherId").getAsLong();
        long courierId = o.has("courierId") && !o.get("courierId").isJsonNull()
                ? o.get("courierId").getAsLong() : -1;

        if ("courier".equals(mode) || myId == courierId) {
            // 代取员操作
            if (status == 0) {
                btnAction.setText("接单");
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setOnClickListener(v -> acceptOrder());
            } else if (status == 1) {
                btnAction.setText("开始取件");
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setOnClickListener(v -> startPickup());
            } else if (status == 2) {
                btnAction.setText("确认完成");
                btnAction.setVisibility(View.VISIBLE);
                btnAction.setOnClickListener(v -> completeOrder());
            } else if (status == 3 && myId == courierId) {
                btnReview.setVisibility(View.VISIBLE);
                btnReview.setOnClickListener(v -> goReview(2));  // 代取员评用户
            }
        } else if (myId == publisherId) {
            // 发布者操作
            if (status == 0) {
                btnCancel.setVisibility(View.VISIBLE);
                btnCancel.setOnClickListener(v -> cancelOrder());
                btnPay.setVisibility(View.VISIBLE);
                btnPay.setOnClickListener(v -> showPayDialog());
            } else if (status == 3) {
                btnReview.setVisibility(View.VISIBLE);
                btnReview.setOnClickListener(v -> goReview(1));  // 用户评代取员
            }
        }
    }

    private void acceptOrder() {
        ApiClient.post("/api/order/" + orderId + "/accept", new Object(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                runOnUiThread(() -> {
                    Toast.makeText(OrderDetailActivity.this, "接单成功！", Toast.LENGTH_SHORT).show();
                    loadDetail();
                });
            }
            @Override
            public void onError(String m) {
                runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, m, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void startPickup() {
        ApiClient.post("/api/order/" + orderId + "/pickup", new Object(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                runOnUiThread(() -> { Toast.makeText(OrderDetailActivity.this, "已更新为取件中", Toast.LENGTH_SHORT).show(); loadDetail(); });
            }
            @Override
            public void onError(String m) {
                runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, m, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void completeOrder() {
        // 实际项目可先让用户拍照上传凭证，这里简化直接完成
        Map<String, String> body = new HashMap<>();
        body.put("imageUrl", "");
        ApiClient.post("/api/order/" + orderId + "/complete", body, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                runOnUiThread(() -> { Toast.makeText(OrderDetailActivity.this, "订单完成，收益已到账！", Toast.LENGTH_SHORT).show(); loadDetail(); });
            }
            @Override
            public void onError(String m) {
                runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, m, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void cancelOrder() {
        ApiClient.post("/api/order/" + orderId + "/cancel", new Object(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                runOnUiThread(() -> { Toast.makeText(OrderDetailActivity.this, "订单已取消", Toast.LENGTH_SHORT).show(); loadDetail(); });
            }
            @Override
            public void onError(String m) {
                runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, m, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showPayDialog() {
        String[] options = {"微信支付（模拟）", "支付宝支付（模拟）", "余额支付"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("选择支付方式")
                .setItems(options, (dialog, which) -> {
                    int payType = which + 1;  // 1微信 2支付宝 3余额
                    Map<String, Object> body = new HashMap<>();
                    body.put("orderId", orderId);
                    body.put("payType", payType);
                    ApiClient.post("/api/payment/pay", body, new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(JsonElement data) {
                            runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, "支付成功（模拟）", Toast.LENGTH_SHORT).show());
                        }
                        @Override
                        public void onError(String m) {
                            runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, m, Toast.LENGTH_SHORT).show());
                        }
                    });
                }).show();
    }

    private void goReview(int type) {
        Intent intent = new Intent(this, ReviewActivity.class);
        intent.putExtra("orderId", orderId);
        intent.putExtra("type", type);
        startActivity(intent);
    }
}
