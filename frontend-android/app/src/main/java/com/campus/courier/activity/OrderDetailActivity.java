package com.campus.courier.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.campus.courier.fragment.PaymentDialogFragment;
import com.campus.courier.util.LoadingStateHelper;
import com.campus.courier.view.OrderStatusTimelineView;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class OrderDetailActivity extends AppCompatActivity
        implements PaymentDialogFragment.PaymentCallback {

    private long orderId;
    private String mode;
    private JsonObject currentOrder;

    // 视图组件
    private OrderStatusTimelineView statusTimelineView;
    private TextView tvOrderNo, tvStatus, tvTracking, tvPickup, tvDelivery, tvFee, tvRemark;
    private TextView tvExpectedTime, tvAppeal, tvPayHint;
    private MaterialButton btnAction, btnCancel, btnPay, btnReview, btnAppeal;
    private ProgressBar progressBar;

    // 状态文本
    private static final String[] STATUS_TEXT = {
            "待接单", "已接单", "取件中", "已完成", "已取消", "异常"
    };

    private View loadingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("订单详情");

        orderId = getIntent().getLongExtra("orderId", -1);
        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "user";

        initViews();
        loadDetail();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDetail();
    }

    private void initViews() {
        // 状态时间线
        statusTimelineView = findViewById(R.id.statusTimelineView);

        // 文本视图
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

        // 按钮
        btnAction = findViewById(R.id.btnAction);
        btnCancel = findViewById(R.id.btnCancel);
        btnPay = findViewById(R.id.btnPay);
        btnReview = findViewById(R.id.btnReview);
        btnAppeal = findViewById(R.id.btnAppeal);

        // 加载指示器
        progressBar = findViewById(R.id.progressBar);
    }

    private void loadDetail() {
        showLoading(true);

        ApiClient.get("/api/order/" + orderId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                currentOrder = data.getAsJsonObject();
                runOnUiThread(() -> {
                    showLoading(false);
                    renderOrder(currentOrder);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showErrorSnackbar(btnAction, "加载失败: " + message);
                });
            }
        });
    }

    private void renderOrder(JsonObject o) {
        int status = o.get("status").getAsInt();

        // 更新状态时间线
        statusTimelineView.setCurrentStatus(status);

        // 更新文本内容
        tvOrderNo.setText("订单号：" + o.get("orderNo").getAsString());
        tvStatus.setText("状态：" + STATUS_TEXT[Math.min(status, STATUS_TEXT.length - 1)]);
        tvTracking.setText("快递单号：" + o.get("trackingNo").getAsString());
        tvPickup.setText("取件地址：" + o.get("pickupAddress").getAsString());
        tvDelivery.setText("送达地址：" + o.get("deliveryAddress").getAsString());
        tvFee.setText("¥" + o.get("fee").getAsString());

        String remark = o.has("remark") && !o.get("remark").isJsonNull()
                ? o.get("remark").getAsString() : "无";
        tvRemark.setText("备注：" + remark);

        // 期望时间
        if (o.has("expectedTime") && !o.get("expectedTime").isJsonNull()) {
            tvExpectedTime.setVisibility(View.VISIBLE);
            tvExpectedTime.setText("期望时间：" + o.get("expectedTime").getAsString());
        } else {
            tvExpectedTime.setVisibility(View.GONE);
        }

        // 申诉原因
        if (status == 5 && o.has("appealReason") && !o.get("appealReason").isJsonNull()) {
            tvAppeal.setVisibility(View.VISIBLE);
            tvAppeal.setText("申诉原因：" + o.get("appealReason").getAsString());
        } else {
            tvAppeal.setVisibility(View.GONE);
        }

        // 隐藏所有按钮和提示
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
            // 代取员视角
            setupCourierButtons(status, myId, publisherId, courierId);
        } else if (imPublisher) {
            // 发布者视角
            setupPublisherButtons(status, myId, publisherId, courierId);
        }
    }

    private void setupCourierButtons(int status, long myId, long publisherId, long courierId) {
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

        setupAppealButton(status, myId, publisherId, courierId);
    }

    private void setupPublisherButtons(int status, long myId, long publisherId, long courierId) {
        if (status == 0) {
            btnCancel.setVisibility(View.VISIBLE);
            btnCancel.setOnClickListener(v -> cancelOrder());
            tvPayHint.setVisibility(View.VISIBLE);
            refreshPublisherPayButtons();
        } else if (status == 3) {
            btnReview.setVisibility(View.VISIBLE);
            btnReview.setOnClickListener(v -> goReview(1));
        }

        setupAppealButton(status, myId, publisherId, courierId);
    }

    private void setupAppealButton(int status, long myId, long publisherId, long courierId) {
        if (status == 0 || status == 4 || status == 5) return;
        boolean canAppeal = (myId == publisherId) || (courierId > 0 && myId == courierId);
        if (!canAppeal) return;

        btnAppeal.setVisibility(View.VISIBLE);
        btnAppeal.setOnClickListener(v -> showAppealDialog());
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
                        btnPay.setOnClickListener(v -> showModernPayDialog());
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    btnPay.setVisibility(View.VISIBLE);
                    tvPayHint.setVisibility(View.VISIBLE);
                    btnPay.setOnClickListener(v -> showModernPayDialog());
                });
            }
        });
    }

    private void showModernPayDialog() {
        if (currentOrder == null) return;

        double fee = currentOrder.get("fee").getAsDouble();
        PaymentDialogFragment dialog = PaymentDialogFragment.newInstance(orderId, fee);
        dialog.setPaymentCallback(this);
        dialog.show(getSupportFragmentManager(), "PaymentDialog");
    }

    // PaymentDialogFragment.PaymentCallback 实现
    @Override
    public void onPaymentSuccess() {
        LoadingStateHelper.showSuccessSnackbar(btnPay, "支付成功！");
        loadDetail(); // 重新加载订单详情
    }

    @Override
    public void onPaymentFailure(String error) {
        LoadingStateHelper.showErrorSnackbar(btnPay, "支付失败: " + error);
    }

    @Override
    public void onPaymentCancelled() {
        LoadingStateHelper.showInfoSnackbar(btnPay, "支付已取消");
    }

    private void showAppealDialog() {
        LoadingStateHelper.showConfirmDialog(this,
                "异常申诉",
                "请填写申诉原因，管理员将在24小时内处理。",
                "提交申诉",
                "取消",
                () -> {
                    // 这里可以扩展为更复杂的申诉表单
                    Map<String, String> body = new HashMap<>();
                    body.put("reason", "用户手动申诉");
                    ApiClient.post("/api/order/" + orderId + "/appeal", body, new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(JsonElement data) {
                            runOnUiThread(() -> {
                                LoadingStateHelper.showSuccessSnackbar(btnAppeal, "申诉已提交");
                                loadDetail();
                            });
                        }

                        @Override
                        public void onError(String m) {
                            runOnUiThread(() ->
                                LoadingStateHelper.showErrorSnackbar(btnAppeal, "申诉失败: " + m));
                        }
                    });
                },
                null
        );
    }

    private void acceptOrder() {
        showLoading(true);
        ApiClient.post("/api/order/" + orderId + "/accept", new Object(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showSuccessSnackbar(btnAction, "接单成功！");
                    loadDetail();
                });
            }

            @Override
            public void onError(String m) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showErrorSnackbar(btnAction, "接单失败: " + m);
                });
            }
        });
    }

    private void startPickup() {
        showLoading(true);
        ApiClient.post("/api/order/" + orderId + "/pickup", new Object(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showSuccessSnackbar(btnAction, "已更新为取件中");
                    loadDetail();
                });
            }

            @Override
            public void onError(String m) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showErrorSnackbar(btnAction, "更新失败: " + m);
                });
            }
        });
    }

    private void completeOrder() {
        showLoading(true);
        Map<String, String> body = new HashMap<>();
        body.put("imageUrl", "");
        ApiClient.post("/api/order/" + orderId + "/complete", body, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showSuccessSnackbar(btnAction, "订单完成，收益已到账！");
                    loadDetail();
                });
            }

            @Override
            public void onError(String m) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showErrorSnackbar(btnAction, "完成失败: " + m);
                });
            }
        });
    }

    private void cancelOrder() {
        LoadingStateHelper.showConfirmDialog(this,
                "确认取消",
                "确定要取消此订单吗？取消后无法恢复。",
                "确认取消",
                "返回",
                () -> {
                    showLoading(true);
                    ApiClient.post("/api/order/" + orderId + "/cancel", new Object(), new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(JsonElement data) {
                            runOnUiThread(() -> {
                                showLoading(false);
                                LoadingStateHelper.showSuccessSnackbar(btnCancel, "订单已取消");
                                loadDetail();
                            });
                        }

                        @Override
                        public void onError(String m) {
                            runOnUiThread(() -> {
                                showLoading(false);
                                LoadingStateHelper.showErrorSnackbar(btnCancel, "取消失败: " + m);
                            });
                        }
                    });
                },
                null
        );
    }

    private void goReview(int type) {
        Intent intent = new Intent(this, ReviewActivity.class);
        intent.putExtra("orderId", orderId);
        intent.putExtra("type", type);
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        if (show) {
            loadingOverlay = LoadingStateHelper.showFullScreenLoading(this);
            progressBar.setVisibility(View.VISIBLE);
            setButtonsEnabled(false);
        } else {
            if (loadingOverlay != null) {
                LoadingStateHelper.hideFullScreenLoading(this, loadingOverlay);
                loadingOverlay = null;
            }
            progressBar.setVisibility(View.GONE);
            setButtonsEnabled(true);
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        btnAction.setEnabled(enabled);
        btnCancel.setEnabled(enabled);
        btnPay.setEnabled(enabled);
        btnReview.setEnabled(enabled);
        btnAppeal.setEnabled(enabled);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadingOverlay != null) {
            LoadingStateHelper.hideFullScreenLoading(this, loadingOverlay);
        }
    }
}
