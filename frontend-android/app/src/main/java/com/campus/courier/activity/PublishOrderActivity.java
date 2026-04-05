package com.campus.courier.activity;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

public class PublishOrderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_order);

        if (getSupportActionBar() != null) getSupportActionBar().setTitle("发布代取需求");

        EditText etTrackingNo      = findViewById(R.id.etTrackingNo);
        EditText etExpressCompany  = findViewById(R.id.etExpressCompany);
        EditText etPickupAddress   = findViewById(R.id.etPickupAddress);
        EditText etDeliveryAddress = findViewById(R.id.etDeliveryAddress);
        EditText etFee             = findViewById(R.id.etFee);
        EditText etRemark          = findViewById(R.id.etRemark);
        EditText etExpectedTime    = findViewById(R.id.etExpectedTime);
        Button   btnSubmit         = findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> {
            String trackingNo = etTrackingNo.getText().toString().trim();
            String pickup     = etPickupAddress.getText().toString().trim();
            String delivery   = etDeliveryAddress.getText().toString().trim();
            String fee        = etFee.getText().toString().trim();

            if (trackingNo.isEmpty() || pickup.isEmpty() || delivery.isEmpty()) {
                Toast.makeText(this, "快递单号、取件地址和送达地址不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> body = new HashMap<>();
            body.put("trackingNo",      trackingNo);
            body.put("expressCompany",  etExpressCompany.getText().toString().trim());
            body.put("pickupAddress",   pickup);
            body.put("deliveryAddress", delivery);
            body.put("fee",             fee.isEmpty() ? "2.00" : fee);
            body.put("remark",          etRemark.getText().toString().trim());
            String exp = etExpectedTime.getText().toString().trim();
            if (!exp.isEmpty()) {
                body.put("expectedTime", exp);
            }

            ApiClient.post("/api/order/publish", body, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JsonElement data) {
                    runOnUiThread(() -> {
                        Toast.makeText(PublishOrderActivity.this, "发布成功！", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
                @Override
                public void onError(String message) {
                    runOnUiThread(() ->
                        Toast.makeText(PublishOrderActivity.this, message, Toast.LENGTH_SHORT).show());
                }
            });
        });
    }
}
