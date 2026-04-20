package com.campus.courier.activity;

import android.os.Bundle;
import android.os.LocaleList;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.campus.courier.fragment.PaymentDialogFragment;
import com.campus.courier.util.LoadingStateHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class PublishOrderActivity extends AppCompatActivity
        implements PaymentDialogFragment.PaymentCallback {

    private TextInputEditText etTrackingNo;
    private TextInputEditText etExpressCompany;
    private TextInputEditText etPickupAddress;
    private TextInputEditText etDeliveryAddress;
    private TextInputEditText etFee;
    private TextInputEditText etExpectedTime;
    private TextInputEditText etRemark;
    private TextInputLayout tilTrackingNo;
    private TextInputLayout tilPickupAddress;
    private TextInputLayout tilDeliveryAddress;
    private TextInputLayout tilFee;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;

    private View loadingOverlay;
    private long publishedOrderId = -1L;
    private double publishedOrderFee = 0d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_order);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("\u53d1\u5e03\u4ee3\u53d6\u8ba2\u5355");
        }

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

        configureImeHints();
        updateSubmitButtonState();
    }

    private void configureImeHints() {
        LocaleList chineseLocales = LocaleList.forLanguageTags("zh-CN");
        applyImeHint(etExpressCompany, chineseLocales);
        applyImeHint(etPickupAddress, chineseLocales);
        applyImeHint(etDeliveryAddress, chineseLocales);
        applyImeHint(etRemark, chineseLocales);

        etExpressCompany.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        etPickupAddress.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS
                | InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        etDeliveryAddress.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_WORDS
                | InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        etRemark.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    }

    private void applyImeHint(TextInputEditText editText, LocaleList locales) {
        if (editText == null) {
            return;
        }
        editText.setImeHintLocales(locales);
    }

    private void setupInputListeners() {
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

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

        etFee.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

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
        String trackingNo = textOf(etTrackingNo);
        String pickup = textOf(etPickupAddress);
        String delivery = textOf(etDeliveryAddress);

        tilTrackingNo.setError(trackingNo.isEmpty() ? "\u5feb\u9012\u5355\u53f7\u4e0d\u80fd\u4e3a\u7a7a" : null);
        tilPickupAddress.setError(pickup.isEmpty() ? "\u53d6\u4ef6\u5730\u5740\u4e0d\u80fd\u4e3a\u7a7a" : null);
        tilDeliveryAddress.setError(delivery.isEmpty() ? "\u9001\u8fbe\u5730\u5740\u4e0d\u80fd\u4e3a\u7a7a" : null);
    }

    private void validateFee(String feeStr) {
        if (feeStr == null || feeStr.trim().isEmpty()) {
            tilFee.setError(null);
            return;
        }

        try {
            double fee = Double.parseDouble(feeStr.trim());
            if (fee <= 0) {
                tilFee.setError("\u8d39\u7528\u5fc5\u987b\u5927\u4e8e0");
            } else if (fee > 100) {
                tilFee.setError("\u8d39\u7528\u4e0d\u80fd\u8d85\u8fc7100\u5143");
            } else {
                tilFee.setError(null);
            }
        } catch (NumberFormatException e) {
            tilFee.setError("\u8bf7\u8f93\u5165\u6709\u6548\u7684\u91d1\u989d");
        }
    }

    private void updateSubmitButtonState() {
        String trackingNo = textOf(etTrackingNo);
        String pickup = textOf(etPickupAddress);
        String delivery = textOf(etDeliveryAddress);
        String feeStr = textOf(etFee);

        boolean hasValidFee = true;
        if (!feeStr.isEmpty()) {
            try {
                double fee = Double.parseDouble(feeStr);
                hasValidFee = fee > 0 && fee <= 100;
            } catch (NumberFormatException e) {
                hasValidFee = false;
            }
        }

        boolean isFormValid = !trackingNo.isEmpty()
                && !pickup.isEmpty()
                && !delivery.isEmpty()
                && hasValidFee;

        btnSubmit.setEnabled(isFormValid);
        btnSubmit.setAlpha(isFormValid ? 1f : 0.5f);
    }

    private void submitOrder() {
        String trackingNo = textOf(etTrackingNo);
        String pickup = textOf(etPickupAddress);
        String delivery = textOf(etDeliveryAddress);
        String feeStr = textOf(etFee);

        if (trackingNo.isEmpty() || pickup.isEmpty() || delivery.isEmpty()) {
            LoadingStateHelper.showErrorSnackbar(
                    btnSubmit,
                    "\u8bf7\u586b\u5199\u5b8c\u6574\u7684\u5fc5\u586b\u4fe1\u606f");
            return;
        }

        btnSubmit.setEnabled(false);
        showLoading(true);

        Map<String, Object> body = new HashMap<>();
        body.put("trackingNo", trackingNo);
        body.put("expressCompany", textOf(etExpressCompany));
        body.put("pickupAddress", pickup);
        body.put("deliveryAddress", delivery);
        body.put("fee", feeStr.isEmpty() ? "2.00" : feeStr);
        body.put("remark", textOf(etRemark));

        String expectedTime = textOf(etExpectedTime);
        if (!expectedTime.isEmpty() && expectedTime.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            body.put("expectedTime", expectedTime);
        }

        ApiClient.post("/api/order/publish", body, new ApiClient.ApiCallback() {
            @Override
            public void onSuccess(JsonElement data) {
                runOnUiThread(() -> {
                    showLoading(false);

                    JsonObject order = data != null && data.isJsonObject()
                            ? data.getAsJsonObject()
                            : null;
                    publishedOrderId = order != null && order.has("id")
                            ? order.get("id").getAsLong()
                            : -1L;
                    publishedOrderFee = order != null && order.has("fee")
                            ? order.get("fee").getAsDouble()
                            : parseOrderFee(feeStr);

                    if (publishedOrderId <= 0L) {
                        LoadingStateHelper.showSuccessSnackbar(
                                btnSubmit,
                                "\u8ba2\u5355\u5df2\u53d1\u5e03\uff0c\u4f46\u672a\u80fd\u6253\u5f00\u652f\u4ed8\u6f14\u793a");
                        finishAfterDelay(800);
                        return;
                    }

                    showPaymentRequestDialog();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    showLoading(false);
                    LoadingStateHelper.showErrorSnackbar(
                            btnSubmit,
                            "\u53d1\u5e03\u5931\u8d25: " + message);
                });
            }
        });
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private double parseOrderFee(String feeStr) {
        if (feeStr == null || feeStr.trim().isEmpty()) {
            return 2.00d;
        }
        try {
            return Double.parseDouble(feeStr.trim());
        } catch (NumberFormatException e) {
            return 2.00d;
        }
    }

    private void showPaymentRequestDialog() {
        PaymentDialogFragment dialog = PaymentDialogFragment.newInstance(publishedOrderId, publishedOrderFee);
        dialog.setPaymentCallback(this);
        dialog.show(getSupportFragmentManager(), "PublishPaymentDialog");
    }

    private void finishAfterDelay(long delayMillis) {
        btnSubmit.postDelayed(this::finish, delayMillis);
    }

    @Override
    public void onPaymentSuccess() {
        LoadingStateHelper.showSuccessSnackbar(
                btnSubmit,
                "\u8ba2\u5355\u5df2\u53d1\u5e03\uff0c\u652f\u4ed8\u6f14\u793a\u6210\u529f");
        finishAfterDelay(700);
    }

    @Override
    public void onPaymentFailure(String error) {
        LoadingStateHelper.showWarningSnackbar(
                btnSubmit,
                "\u8ba2\u5355\u5df2\u53d1\u5e03\uff0c\u652f\u4ed8\u6f14\u793a\u5931\u8d25\uff1a" + error);
        finishAfterDelay(1200);
    }

    @Override
    public void onPaymentCancelled() {
        LoadingStateHelper.showInfoSnackbar(
                btnSubmit,
                "\u8ba2\u5355\u5df2\u53d1\u5e03\uff0c\u53ef\u7a0d\u540e\u5728\u8be6\u60c5\u9875\u7ee7\u7eed\u652f\u4ed8");
        finishAfterDelay(1200);
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
