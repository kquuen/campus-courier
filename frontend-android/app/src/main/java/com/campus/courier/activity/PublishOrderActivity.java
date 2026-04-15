package com.campus.courier.activity;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.campus.courier.util.LoadingStateHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

public class PublishOrderActivity extends AppCompatActivity {

    private TextInputEditText etTrackingNo, etExpressCompany, etPickupAddress;
    private TextInputEditText etDeliveryAddress, etFee, etExpectedTime, etRemark;
    private TextInputLayout tilTrackingNo, tilPickupAddress, tilDeliveryAddress, tilFee;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;

    private View loadingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_order);

        if (getSupportActionBar() != null) getSupportActionBar().setTitle("发布代取订单");

        initViews();
        setupInputListeners();
        setupSubmitButton();
    }

    private void initViews() {
        etTrackingNo = findViewById(R.id.etTrackingNo);
        etExpressCompany = findViewById(R.id.etExpressCompany);
        etPickupAddress = findViewById(R.id.etPickupAddress);
        etDeliveryAddress = findViewById(R.id.etDeliveryAddress);
        etFee = findViewById(R.id.etFee);
        etExpectedTime = findViewById(R.id.etExpectedTime);
        etRemark = findViewById(R.id.etRemark);

        tilTrackingNo = findViewById(R.id.tilTrackingNo);
        tilPickupAddress = findViewById(R.id.tilPickupAddress);
        tilDeliveryAddress = findViewById(R.id.tilDeliveryAddress);
        tilFee = findViewById(R.id.tilFee);

        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        updateSubmitButtonState();
    }

    private void setupInputListeners() {
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateInputs();
                updateSubmitButtonState();
            }
        };

        etTrackingNo.addTextChangedListener(validationWatcher);
        etPickupAddress.addTextChangedListener(validationWatcher);
        etDeliveryAddress.addTextChangedListener(validationWatcher);
        etFee.addTextChangedListener(validationWatcher);

        // 为费用输入添加特殊验证
        etFee.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateFee(s.toString());
                updateSubmitButtonState();
            }
        });
    }

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> submitOrder());
    }

    private void validateInputs() {
        String trackingNo = etTrackingNo.getText().toString().trim();
        String pickup = etPickupAddress.getText().toString().trim();
        String delivery = etDeliveryAddress.getText().toString().trim();

        if (trackingNo.isEmpty()) {
            tilTrackingNo.setError("快递单号不能为空");
        } else {
            tilTrackingNo.setError(null);
        }

        if (pickup.isEmpty()) {
            tilPickupAddress.setError("取件地址不能为空");
        } else {
            tilPickupAddress.setError(null);
        }

        if (delivery.isEmpty()) {
            tilDeliveryAddress.setError("送达地址不能为空");
        } else {
            tilDeliveryAddress.setError(null);
        }
    }

    private void validateFee(String feeStr) {
        if (feeStr.isEmpty()) {
            tilFee.setError(null);
            return;
        }

        try {
            double fee = Double.parseDouble(feeStr);
            if (fee <= 0) {
                tilFee.setError("费用必须大于0");
            } else if (fee > 100) {
                tilFee.setError("费用不能超过100元");
            } else {
                tilFee.setError(null);
            }
        } catch (NumberFormatException e) {
            tilFee.setError("请输入有效的金额");
        }
    }

    private void updateSubmitButtonState() {
        String trackingNo = etTrackingNo.getText().toString().trim();
        String pickup = etPickupAddress.getText().toString().trim();
        String delivery = etDeliveryAddress.getText().toString().trim();
        String feeStr = etFee.getText().toString().trim();

        boolean hasTrackingNo = !trackingNo.isEmpty();
        boolean hasPickup = !pickup.isEmpty();
        boolean hasDelivery = !delivery.isEmpty();
        boolean hasValidFee = true;

        if (!feeStr.isEmpty()) {
            try {
                double fee = Double.parseDouble(feeStr);
                hasValidFee = fee > 0 && fee <= 100;
            } catch (NumberFormatException e) {
                hasValidFee = false;
            }
        }

        boolean isFormValid = hasTrackingNo && hasPickup && hasDelivery && hasValidFee;

        btnSubmit.setEnabled(isFormValid);
        btnSubmit.setAlpha(isFormValid ? 1f : 0.5f);
    }

    private void submitOrder() {
        String trackingNo = etTrackingNo.getText().toString().trim();
        String pickup = etPickupAddress.getText().toString().trim();
        String delivery = etDeliveryAddress.getText().toString().trim();
        String feeStr = etFee.getText().toString().trim();

        // 最终验证
        if (trackingNo.isEmpty() || pickup.isEmpty() || delivery.isEmpty()) {
            LoadingStateHelper.showErrorSnackbar(btnSubmit, "请填写完整必填信息");
            return;
        }

        // 禁用提交按钮，防止重复点击
        btnSubmit.setEnabled(false);

        // 显示加载状态
        showLoading(true);

        Map<String, Object> body = new HashMap<>();
        body.put("trackingNo", trackingNo);
        body.put("expressCompany", etExpressCompany.getText().toString().trim());
        body.put("pickupAddress", pickup);
        body.put("deliveryAddress", delivery);
        body.put("fee", feeStr.isEmpty() ? "2.00" : feeStr);
        body.put("remark", etRemark.getText().toString().trim());

        String exp = etExpectedTime.getText().toString().trim();
        // 只发送有效的日期格式
        if (!exp.isEmpty() && exp.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            body.put("expectedTime", exp);
        }

        ApiClient.post("/api/order/publish", body, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showSuccessSnackbar(btnSubmit, "订单发布成功！");

                    // 延迟返回，让用户看到成功提示
                    btnSubmit.postDelayed(() -> finish(), 500);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showErrorSnackbar(btnSubmit, "发布失败: " + message);
                });
            }
        });
    }

    private void showLoading(boolean show) {
        if (show) {
            loadingOverlay = LoadingStateHelper.showFullScreenLoading(this);
            progressBar.setVisibility(View.VISIBLE);
            btnSubmit.setEnabled(false);
        } else {
            if (loadingOverlay != null) {
                LoadingStateHelper.hideFullScreenLoading(this, loadingOverlay);
                loadingOverlay = null;
            }
            progressBar.setVisibility(View.GONE);
            updateSubmitButtonState();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (loadingOverlay != null) {
            LoadingStateHelper.hideFullScreenLoading(this, loadingOverlay);
        }
    }
}
