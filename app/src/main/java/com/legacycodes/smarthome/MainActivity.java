package com.legacycodes.smarthome;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private EditText etIpAddress;
    private Button btnTest;
    private TextView tvStatus;
    private PingClient pingClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etIpAddress = findViewById(R.id.etIpAddress);
        btnTest     = findViewById(R.id.btnTest);
        tvStatus    = findViewById(R.id.tvStatus);

        pingClient = new PingClient();

        btnTest.setOnClickListener(v -> {
            String ip = etIpAddress.getText().toString().trim();
            if (TextUtils.isEmpty(ip)) {
                tvStatus.setText("Please enter an IP address");
                return;
            }

            btnTest.setEnabled(false);
            tvStatus.setText("Connecting…");

            pingClient.ping(ip, new PingClient.Callback() {
                @Override
                public void onSuccess(String body) {
                    btnTest.setEnabled(true);
                    if (body.contains("\"ok\":true")) {
                        tvStatus.setText("Connected ✅");
                    } else {
                        tvStatus.setText("Reached device but unexpected reply: " + body);
                    }
                }

                @Override
                public void onFailure(String reason) {
                    btnTest.setEnabled(true);
                    tvStatus.setText("Failed: " + reason);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pingClient != null) pingClient.shutdown();
    }
}