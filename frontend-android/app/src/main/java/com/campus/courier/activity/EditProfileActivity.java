package com.campus.courier.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.campus.courier.R;
import com.campus.courier.api.ApiClient;
import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("修改资料");

        EditText etNick = findViewById(R.id.etNickname);
        EditText etAvatar = findViewById(R.id.etAvatar);
        Button btn = findViewById(R.id.btnSave);

        String n = getIntent().getStringExtra("nickname");
        String a = getIntent().getStringExtra("avatar");
        if (n != null) etNick.setText(n);
        if (a != null && !a.isEmpty()) etAvatar.setText(a);

        btn.setOnClickListener(v -> {
            Map<String, String> body = new HashMap<>();
            String nick = etNick.getText().toString().trim();
            String av = etAvatar.getText().toString().trim();
            if (nick.isEmpty() && av.isEmpty()) {
                Toast.makeText(this, "请至少填写一项", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!nick.isEmpty()) body.put("nickname", nick);
            if (!av.isEmpty()) body.put("avatar", av);

            ApiClient.put("/api/user/profile", body, new ApiClient.ApiCallback() {
                @Override
                public void onSuccess(JsonElement data) {
                    runOnUiThread(() -> {
                        if (!nick.isEmpty()) ApiClient.updateSavedNickname(nick);
                        Toast.makeText(EditProfileActivity.this, "已保存", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_SHORT).show());
                }
            });
        });
    }
}
