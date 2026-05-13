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
import com.campus.courier.fragment.RefundDialogFragment;
import com.campus.courier.util.LoadingStateHelper;
import com.campus.courier.view.OrderStatusTimelineView;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class OrderDetailActivity extends AppCompatActivity
        implements PaymentDialogFragment.PaymentCallback, RefundDialogFragment.RefundCallback {

    private static final String[] STATUS_TEXT = {
            "\u5f85\u63a5\u5355",
            "\u5df2\u63a5\u5355",
            "\u53d6\u4ef6\u4e2d",
            "\u914d\u9001\u4e2d",
            "\u5df2\u5b8c\u6210",
            "\u5df2\u53d6\u6d88",
            "\u5f02\u5e38"
    };

    private static final String PAY_STATUS_PAID = "\u5df2\u652f\u4ed8";

    private long orderId;
    private String mode;
    private JsonObject currentOrder;
    private boolean lastKnownPaidStatus;

    private OrderStatusTimelineView statusTimelineView;
    private TextView tvOrderNo;
    private TextView tvStatus;
    private TextView tvTracking;
    private TextView tvPickup;
    private TextView tvDelivery;
    private TextView tvFee;
    private TextView tvRemark;
    private TextView tvExpectedTime;
    private TextView tvAppeal;
    private TextView tvPayHint;
    private MaterialButton btnAction;
    private MaterialButton btnCancel;
    private MaterialButton btnPay;
    private MaterialButton btnReview;
    private MaterialButton btnAppeal;
    private ProgressBar progressBar;

    private View loadingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("\u8ba2\u5355\u8be6\u60c5");
        }

        orderId = getIntent().getLongExtra("orderId", -1L);
        mode = getIntent().getStringExtra("mode");
        if (mode == null) {
            mode = "user";
        }

        initViews();
        loadDetail();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDetail();
    }

    private void initViews() {
        statusTimelineView = findViewById(R.id.statusTimelineView);

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

        progressBar = findViewById(R.id.progressBar);
    }

    private void loadDetail() {
        if (orderId <= 0L) {
            showLoading(false);
            LoadingStateHelper.showErrorSnackbar(
                    btnAction,
                    "\u65e0\u6548\u7684\u8ba2\u5355ID");
            return;
        }

        showLoading(true);
        ApiClient.get("/api/order/" + orderId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                currentOrder = data != null && data.isJsonObject()
                        ? data.getAsJsonObject()
                        : null;
                runOnUiThread(() -> {
                    showLoading(false);
                    if (currentOrder != null) {
                        renderOrder(currentOrder);
                    } else {
                        LoadingStateHelper.showErrorSnackbar(
                                btnAction,
                                "\u8ba2\u5355\u6570\u636e\u89e3\u6790\u5931\u8d25");
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showErrorSnackbar(
                            btnAction,
                            "\u52a0\u8f7d\u5931\u8d25: " + message);
                });
            }
        });

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                () -> showLoading(false),
                15000);
    }

    private void renderOrder(JsonObject order) {
        int status = safeGetInt(order, "status", 0);

        statusTimelineView.setCurrentStatus(status);
        tvOrderNo.setText("\u8ba2\u5355\u53f7\uff1a" + safeGetString(order, "orderNo", "\u672a\u77e5"));
        tvStatus.setText("\u72b6\u6001\uff1a" + statusText(status));
        tvTracking.setText("\u5feb\u9012\u5355\u53f7\uff1a" + safeGetString(order, "trackingNo", "\u6682\u65e0"));
        tvPickup.setText("\u53d6\u4ef6\u5730\u5740\uff1a" + safeGetString(order, "pickupAddress", "\u672a\u77e5"));
        tvDelivery.setText("\u9001\u8fbe\u5730\u5740\uff1a" + safeGetString(order, "deliveryAddress", "\u672a\u77e5"));
        tvFee.setText("\u00a5" + safeGetString(order, "fee", "0"));

        String remark = safeGetString(order, "remark", "\u65e0");
        tvRemark.setText("\u5907\u6ce8\uff1a" + remark);

        if (order.has("expectedTime") && !order.get("expectedTime").isJsonNull()) {
            tvExpectedTime.setVisibility(View.VISIBLE);
            tvExpectedTime.setText(
                    "\u671f\u671b\u65f6\u95f4\uff1a" + order.get("expectedTime").getAsString());
        } else {
            tvExpectedTime.setVisibility(View.GONE);
        }

        if (status == 5 && order.has("appealReason") && !order.get("appealReason").isJsonNull()) {
            tvAppeal.setVisibility(View.VISIBLE);
            tvAppeal.setText(
                    "\u7533\u8bc9\u539f\u56e0\uff1a" + order.get("appealReason").getAsString());
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
        long publisherId = safeGetLong(order, "publisherId", -1L);
        long courierId = safeGetLong(order, "courierId", -1L);

        boolean courierSide = "courier".equals(mode) || myId == courierId;
        boolean publisherSide = myId == publisherId;

        if (courierSide) {
            setupCourierButtons(status, myId, publisherId, courierId);
        } else if (publisherSide) {
            setupPublisherButtons(status, myId, publisherId, courierId);
        }
    }

    private String statusText(int status) {
        int index = Math.max(0, Math.min(status, STATUS_TEXT.length - 1));
        return STATUS_TEXT[index];
    }

    private int safeGetInt(JsonObject order, String key, int defaultValue) {
        if (order == null || !order.has(key) || order.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return order.get(key).getAsInt();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String safeGetString(JsonObject order, String key, String defaultValue) {
        if (order == null || !order.has(key) || order.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return order.get(key).getAsString();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private long safeGetLong(JsonObject order, String key, long defaultValue) {
        if (order == null || !order.has(key) || order.get(key).isJsonNull()) {
            return defaultValue;
        }
        try {
            return order.get(key).getAsLong();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void setupCourierButtons(int status, long myId, long publisherId, long courierId) {
        if (status == 0) {
            btnAction.setText("\u63a5\u5355");
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setOnClickListener(v -> acceptOrder());
        } else if (status == 1) {
            btnAction.setText("\u5f00\u59cb\u53d6\u4ef6");
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setOnClickListener(v -> startPickup());
        } else if (status == 2) {
            btnAction.setText("\u5f00\u59cb\u914d\u9001");
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setOnClickListener(v -> startDeliver());
        } else if (status == 3) {
            btnAction.setText("\u786e\u8ba4\u5b8c\u6210");
            btnAction.setVisibility(View.VISIBLE);
            btnAction.setOnClickListener(v -> completeOrder());
        } else if (status == 4 && myId == courierId) {
            btnReview.setVisibility(View.VISIBLE);
            btnReview.setOnClickListener(v -> goReview(2));
        }

        setupAppealButton(status, myId, publisherId, courierId);
    }

    private void setupPublisherButtons(int status, long myId, long publisherId, long courierId) {
        if (status == 0) {
            btnCancel.setVisibility(View.VISIBLE);
            btnCancel.setText("\u53d6\u6d88\u8ba2\u5355");
            btnCancel.setOnClickListener(v -> handleCancelAction(false));
            tvPayHint.setVisibility(View.VISIBLE);
            refreshPublisherPayButtons();
        } else if (status == 1) {
            btnCancel.setVisibility(View.VISIBLE);
            btnCancel.setText("\u9000\u6b3e\u53d6\u6d88");
            btnCancel.setOnClickListener(v -> handleCancelAction(true));
        } else if (status == 4) {
            btnReview.setVisibility(View.VISIBLE);
            btnReview.setOnClickListener(v -> goReview(1));
        }

        setupAppealButton(status, myId, publisherId, courierId);
    }

    private void setupAppealButton(int status, long myId, long publisherId, long courierId) {
        if (status == 0 || status == 4 || status == 5 || status == 6) {
            return;
        }
        boolean canAppeal = myId == publisherId || (courierId > 0 && myId == courierId);
        if (!canAppeal) {
            return;
        }

        btnAppeal.setVisibility(View.VISIBLE);
        btnAppeal.setOnClickListener(v -> showAppealDialog());
    }

    private void refreshPublisherPayButtons() {
        ApiClient.get("/api/payment/status/" + orderId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                String desc = data != null && data.isJsonPrimitive()
                        ? data.getAsString()
                        : "";
                boolean paid = PAY_STATUS_PAID.equals(desc);
                lastKnownPaidStatus = paid;

                runOnUiThread(() -> {
                    btnCancel.setText(paid ? "\u9000\u6b3e\u53d6\u6d88" : "\u53d6\u6d88\u8ba2\u5355");
                    btnPay.setVisibility(paid ? View.GONE : View.VISIBLE);
                    tvPayHint.setVisibility(paid ? View.GONE : View.VISIBLE);
                    if (!paid) {
                        btnPay.setOnClickListener(v -> showModernPayDialog());
                    }
                });
            }

            @Override
            public void onError(String message) {
                lastKnownPaidStatus = false;
                runOnUiThread(() -> {
                    btnCancel.setText("\u53d6\u6d88\u8ba2\u5355");
                    btnPay.setVisibility(View.VISIBLE);
                    tvPayHint.setVisibility(View.VISIBLE);
                    btnPay.setOnClickListener(v -> showModernPayDialog());
                });
            }
        });

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!lastKnownPaidStatus) {
                btnCancel.setText("\u53d6\u6d88\u8ba2\u5355");
                btnPay.setVisibility(View.VISIBLE);
                tvPayHint.setVisibility(View.VISIBLE);
                btnPay.setOnClickListener(v -> showModernPayDialog());
            }
        }, 15000);
    }

    private void showModernPayDialog() {
        if (currentOrder == null) {
            return;
        }

        double fee = currentOrder.has("fee") && !currentOrder.get("fee").isJsonNull()
                ? currentOrder.get("fee").getAsDouble()
                : 0d;
        PaymentDialogFragment dialog = PaymentDialogFragment.newInstance(orderId, fee);
        dialog.setPaymentCallback(this);
        dialog.show(getSupportFragmentManager(), "PaymentDialog");
    }

    @Override
    public void onPaymentSuccess() {
        LoadingStateHelper.showSuccessSnackbar(
                btnPay,
                "\u652f\u4ed8\u6210\u529f");
        loadDetail();
    }

    @Override
    public void onPaymentFailure(String error) {
        LoadingStateHelper.showErrorSnackbar(
                btnPay,
                "\u652f\u4ed8\u5931\u8d25: " + error);
    }

    @Override
    public void onPaymentCancelled() {
        LoadingStateHelper.showInfoSnackbar(
                btnPay,
                "\u652f\u4ed8\u5df2\u53d6\u6d88");
    }

    private void showAppealDialog() {
        LoadingStateHelper.showConfirmDialog(
                this,
                "\u5f02\u5e38\u7533\u8bc9",
                "\u8bf7\u63d0\u4ea4\u7533\u8bc9\u8bf4\u660e\uff0c\u7ba1\u7406\u5458\u5c06\u5c3d\u5feb\u5904\u7406\u3002",
                "\u63d0\u4ea4\u7533\u8bc9",
                "\u53d6\u6d88",
                () -> {
                    Map<String, String> body = new HashMap<>();
                    body.put("reason", "\u7528\u6237\u624b\u52a8\u7533\u8bc9");
                    ApiClient.post("/api/order/" + orderId + "/appeal", body, new ApiClient.ApiCallback() {
                        @Override
                        public void onSuccess(JsonElement data) {
                            runOnUiThread(() -> {
                                LoadingStateHelper.showSuccessSnackbar(
                                        btnAppeal,
                                        "\u7533\u8bc9\u5df2\u63d0\u4ea4");
                                loadDetail();
                            });
                        }

                        @Override
                        public void onError(String message) {
                            runOnUiThread(() -> LoadingStateHelper.showErrorSnackbar(
                                    btnAppeal,
                                    "\u7533\u8bc9\u5931\u8d25: " + message));
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
                    LoadingStateHelper.showSuccessSnackbar(
                            btnAction,
                            "\u63a5\u5355\u6210\u529f");
                    loadDetail();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showErrorSnackbar(
                            btnAction,
                            "\u63a5\u5355\u5931\u8d25: " + message);
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
                    LoadingStateHelper.showSuccessSnackbar(
                            btnAction,
                            "\u5df2\u66f4\u65b0\u4e3a\u53d6\u4ef6\u4e2d");
                    loadDetail();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showErrorSnackbar(
                            btnAction,
                            "\u66f4\u65b0\u5931\u8d25: " + message);
                });
            }
        });
    }

    private void startDeliver() {
        showLoading(true);
        ApiClient.post("/api/order/" + orderId + "/deliver", new Object(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showSuccessSnackbar(
                            btnAction,
                            "\u5df2\u66f4\u65b0\u4e3a\u914d\u9001\u4e2d");
                    loadDetail();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showErrorSnackbar(
                            btnAction,
                            "\u66f4\u65b0\u5931\u8d25: " + message);
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
                    LoadingStateHelper.showSuccessSnackbar(
                            btnAction,
                            "\u8ba2\u5355\u5df2\u5b8c\u6210\uff0c\u6536\u76ca\u5df2\u5230\u8d26");
                    loadDetail();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showErrorSnackbar(
                            btnAction,
                            "\u5b8c\u6210\u5931\u8d25: " + message);
                });
            }
        });
    }

    private void handleCancelAction(boolean assumePaidFallback) {
        ApiClient.get("/api/payment/status/" + orderId, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                String desc = data != null && data.isJsonPrimitive()
                        ? data.getAsString()
                        : "";
                boolean paid = PAY_STATUS_PAID.equals(desc);
                lastKnownPaidStatus = paid;
                runOnUiThread(() -> {
                    if (paid) {
                        showRefundDemoDialog();
                    } else {
                        showCancelConfirmDialog();
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    if (assumePaidFallback || lastKnownPaidStatus) {
                        showRefundDemoDialog();
                    } else {
                        showCancelConfirmDialog();
                    }
                });
            }
        });
    }

    private void showCancelConfirmDialog() {
        LoadingStateHelper.showConfirmDialog(
                this,
                "\u786e\u8ba4\u53d6\u6d88",
                "\u786e\u5b9a\u8981\u53d6\u6d88\u8fd9\u4e2a\u672a\u652f\u4ed8\u8ba2\u5355\u5417\uff1f",
                "\u786e\u8ba4\u53d6\u6d88",
                "\u8fd4\u56de",
                this::submitCancelOrder,
                null
        );
    }

    private void submitCancelOrder() {
        showLoading(true);
        ApiClient.post("/api/order/" + orderId + "/cancel", new Object(), new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showSuccessSnackbar(
                            btnCancel,
                            "\u8ba2\u5355\u5df2\u53d6\u6d88");
                    loadDetail();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showErrorSnackbar(
                            btnCancel,
                            "\u53d6\u6d88\u5931\u8d25: " + message);
                });
            }
        });
    }

    private void showRefundDemoDialog() {
        double fee = currentOrder != null && currentOrder.has("fee") && !currentOrder.get("fee").isJsonNull()
                ? currentOrder.get("fee").getAsDouble()
                : 0d;
        RefundDialogFragment dialog = RefundDialogFragment.newInstance(orderId, fee);
        dialog.setRefundCallback(this);
        dialog.show(getSupportFragmentManager(), "RefundDialog");
    }

    @Override
    public void onRefundSuccess() {
        LoadingStateHelper.showSuccessSnackbar(
                btnCancel,
                "\u6a21\u62df\u9000\u6b3e\u6210\u529f\uff0c\u8ba2\u5355\u5df2\u53d6\u6d88");
        loadDetail();
    }

    @Override
    public void onRefundFailure(String error) {
        LoadingStateHelper.showErrorSnackbar(
                btnCancel,
                "\u9000\u6b3e\u5931\u8d25: " + error);
    }

    @Override
    public void onRefundCancelled() {
        LoadingStateHelper.showInfoSnackbar(
                btnCancel,
                "\u5df2\u53d6\u6d88\u9000\u6b3e\u6f14\u793a");
    }

    private void goReview(int type) {
        Intent intent = new Intent(this, ReviewActivity.class);
        intent.putExtra("orderId", orderId);
        intent.putExtra("type", type);
        startActivity(intent);
    }

    private void showLoading(boolean show) {
        if (show) {
            if (loadingOverlay != null) {
                LoadingStateHelper.hideFullScreenLoading(this, loadingOverlay);
            }
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
