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
    private String mode;
    private JsonObject currentOrder;

    private TextView tvOrderNo, tvStatus, tvTracking, tvPickup, tvDelivery, tvFee, tvRemark;
    private TextView tvExpectedTime, tvAppeal, tvPayHint;
    private Button btnAction, btnCancel, btnPay, btnReview, btnAppeal;

    private static final String[] STATUS_TEXT = {
            "待接单", "已接单", "取件中", "已完成", "已取消", "异常"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("订单详情");

        orderId = getIntent().getLongExtra("orderId", -1);
        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "user";

        tvOrderNo = findViewById(R.id.tvOrderNo);
        tvStatus = findViewById(R.id.tvStatus);
        tvTracking = findViewById(R.id.tvTracking);
        tvPickup = findViewById(R.id.tvPickup);
        tvDelivery = findViewById(R.id.tvDelivery);
        tvFee = findViewById(R.id.tvFee);
        tvRemark = findViewById(R.id.tvRemark);
        tvExpectedTime = findViewById(R.id.tvExpectedTime);
        tvAppeal = findViewById(R.id.tvAppeal);
        tvPayHint = findViewById(R.id.tvPayHint);
        btnAction = findViewById(R.id.btnAction);
        btnCancel = findViewById(R.id.btnCancel);
        btnPay = findViewById(R.id.btnPay);
        btnReview = findViewById(R.id.btnReview);
        btnAppeal = findViewById(R.id.btnAppeal);

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

        if (o.has("expectedTime") && !o.get("expectedTime").isJsonNull()) {
            tvExpectedTime.setVisibility(View.VISIBLE);
            tvExpectedTime.setText("期望时间：" + o.get("expectedTime").getAsString());
        } else {
            tvExpectedTime.setVisibility(View.GONE);
        }

        if (status == 5 && o.has("appealReason") && !o.get("appealReason").isJsonNull()) {
            tvAppeal.setVisibility(View.VISIBLE);
            tvAppeal.setText("申诉原因：" + o.get("appealReason").getAsString());
        } else {
            tvAppeal.setVisibility(View.GONE);
        }

        btnAction.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        btnPay.setVisibility(View.GONE);
        btnReview.setVisibility(View.GONE);
        btnAppeal.setVisibility(View.GONE);
        tvPayHint.setVisibility(View.GONE);

        long myId = ApiClient.getSavedUserId();
        long publisherId = o.get("publisherId").getAsLong();
        long courierId = o.has("courierId") && !o.get("courierId").isJsonNull()
                ? o.get("courierId").getAsLong() : -1;

        boolean imCourierSide = "courier".equals(mode) || myId == courierId;
        boolean imPublisher = myId == publisherId;

        if (imCourierSide) {
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
                btnReview.setOnClickListener(v -> goReview(2));
            }
            wireAppealIfAllowed(status, myId, publisherId, courierId);
        } else if (imPublisher) {
            if (status == 0) {
                btnCancel.setVisibility(View.VISIBLE);
                btnCancel.setOnClickListener(v -> cancelOrder());
                tvPayHint.setVisibility(View.VISIBLE);
                refreshPublisherPayButtons();
            } else if (status == 3) {
                btnReview.setVisibility(View.VISIBLE);
                btnReview.setOnClickListener(v -> goReview(1));
            }
            wireAppealIfAllowed(status, myId, publisherId, courierId);
        }
    }

    /** 待接单时根据是否已支付显示「立即支付」 */
    private void refreshPublisherPayButtons() {
        ApiClient.get("/api/payment/status/" + orderId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                String desc = data != null && data.isJsonPrimitive() ? data.getAsString() : "";
                boolean paid = "已支付".equals(desc);
                runOnUiThread(() -> {
                    btnPay.setVisibility(paid ? View.GONE : View.VISIBLE);
                    tvPayHint.setVisibility(paid ? View.GONE : View.VISIBLE);
                    if (!paid) {
                        btnPay.setOnClickListener(v -> showPayDialog());
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    btnPay.setVisibility(View.VISIBLE);
                    tvPayHint.setVisibility(View.VISIBLE);
                    btnPay.setOnClickListener(v -> showPayDialog());
                });
            }
        });
    }

    private void wireAppealIfAllowed(int status, long myId, long publisherId, long courierId) {
        if (status == 0 || status == 4 || status == 5) return;
        boolean can = (myId == publisherId) || (courierId > 0 && myId == courierId);
        if (!can) return;
        btnAppeal.setVisibility(View.VISIBLE);
        btnAppeal.setOnClickListener(v -> showAppealDialog());
    }

    private void showAppealDialog() {
        final EditText et = new EditText(this);
        et.setHint("请填写申诉原因");
        new android.app.AlertDialog.Builder(this)
                .setTitle("异常申诉")
                .setView(et)
                .setPositiveButton("提交", (d, w) -> {
                    String reason = et.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "请填写原因", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Map<String, String> body = new HashMap<>();
                    body.put("reason", reason);
                    ApiClient.post("/api/order/" + orderId + "/appeal", body, new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(JsonElement data) {
                            runOnUiThread(() -> {
                                Toast.makeText(OrderDetailActivity.this, "已提交申诉", Toast.LENGTH_SHORT).show();
                                loadDetail();
                            });
                        }

                        @Override
                        public void onError(String m) {
                            runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, m, Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
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
                runOnUiThread(() -> {
                    Toast.makeText(OrderDetailActivity.this, "已更新为取件中", Toast.LENGTH_SHORT).show();
                    loadDetail();
                });
            }

            @Override
            public void onError(String m) {
                runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, m, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void completeOrder() {
        Map<String, String> body = new HashMap<>();
        body.put("imageUrl", "");
        ApiClient.post("/api/order/" + orderId + "/complete", body, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                runOnUiThread(() -> {
                    Toast.makeText(OrderDetailActivity.this, "订单完成，收益已到账！", Toast.LENGTH_SHORT).show();
                    loadDetail();
                });
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
                runOnUiThread(() -> {
                    Toast.makeText(OrderDetailActivity.this, "订单已取消", Toast.LENGTH_SHORT).show();
                    loadDetail();
                });
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
                    int payType = which + 1;
                    Map<String, Object> body = new HashMap<>();
                    body.put("orderId", orderId);
                    body.put("payType", payType);
                    ApiClient.post("/api/payment/pay", body, new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(JsonElement data) {
                            if (data != null && data.isJsonObject()) {
                                JsonObject obj = data.getAsJsonObject();
                                boolean needCb = obj.has("needCallback") && obj.get("needCallback").getAsBoolean();
                                if (needCb && obj.has("paymentNo")) {
                                    String no = obj.get("paymentNo").getAsString();
                                    confirmMockThirdPay(no);
                                    return;
                                }
                            }
                            runOnUiThread(() -> {
                                Toast.makeText(OrderDetailActivity.this, "支付成功", Toast.LENGTH_SHORT).show();
                                loadDetail();
                            });
                        }

                        @Override
                        public void onError(String m) {
                            runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, m, Toast.LENGTH_SHORT).show());
                        }
                    });
                }).show();
    }

    private void confirmMockThirdPay(String paymentNo) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("模拟第三方支付")
                .setMessage("是否模拟用户已完成微信/支付宝付款？\n（确认后将通知服务端支付成功）")
                .setPositiveButton("确认已支付", (d, w) ->
                        ApiClient.post("/api/payment/callback/" + paymentNo, new Object(), new ApiClient.ApiCallback() {
                            @Override
                            public void onSuccess(JsonElement data) {
                                runOnUiThread(() -> {
                                    Toast.makeText(OrderDetailActivity.this, "支付成功", Toast.LENGTH_SHORT).show();
                                    loadDetail();
                                });
                            }

                            @Override
                            public void onError(String m) {
                                runOnUiThread(() -> Toast.makeText(OrderDetailActivity.this, m, Toast.LENGTH_SHORT).show());
                            }
                        }))
                .setNegativeButton("取消", null)
                .show();
    }

    private void goReview(int type) {
        Intent intent = new Intent(this, ReviewActivity.class);
        intent.putExtra("orderId", orderId);
        intent.putExtra("type", type);
        startActivity(intent);
    }
}
