package com.campus.courier.activity;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

public class ReviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("提交评价");

        long    orderId = getIntent().getLongExtra("orderId", -1);
        int     type    = getIntent().getIntExtra("type", 1);

        RatingBar ratingBar = findViewById(R.id.ratingBar);
        EditText  etContent = findViewById(R.id.etContent);
        Button    btnSubmit = findViewById(R.id.btnSubmit);

        String label = type == 1 ? "评价代取员服务" : "评价发件用户";
        ((TextView) findViewById(R.id.tvLabel)).setText(label);

        btnSubmit.setOnClickListener(v -> {
            int score = (int) ratingBar.getRating();
            if (score == 0) {
                Toast.makeText(this, "请先评分", Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> body = new HashMap<>();
            body.put("orderId",  orderId);
            body.put("score",    score);
            body.put("content",  etContent.getText().toString().trim());
            body.put("type",     type);

            ApiClient.post("/api/review/submit", body, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JsonElement data) {
                    runOnUiThread(() -> {
                        Toast.makeText(ReviewActivity.this, "评价成功！", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
                @Override
                public void onError(String message) {
                    runOnUiThread(() ->
                        Toast.makeText(ReviewActivity.this, message, Toast.LENGTH_SHORT).show());
                }
            });
        });
    }
}
