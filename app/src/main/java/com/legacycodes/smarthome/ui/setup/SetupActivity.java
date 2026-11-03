package com.legacycodes.smarthome.ui.setup;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.legacycodes.smarthome.AppContainer;
import com.legacycodes.smarthome.PingClient;
import com.legacycodes.smarthome.R;
import com.legacycodes.smarthome.SmartHomeApp;
import com.legacycodes.smarthome.data.repository.DeviceRepository;
import com.legacycodes.smarthome.ui.home.HomeActivity;

/**
 * First-run setup screen.
 *
 * Flow:
 *   1. User enters the ESP32's IP (read from the serial monitor)
 *   2. Tap "Test connection & save"
 *   3. We ping /ping to verify reachability
 *   4. On success: save DeviceEntity to Room, launch HomeActivity, finish
 *   5. On failure: show error, let user retry
 *
 * Reuses PingClient from Phase 0 — that class is exactly right for this.
 * Its simple "can you reach this IP? yes/no" contract is what setup needs.
 */
public class SetupActivity extends AppCompatActivity {

    private TextInputEditText etIp;
    private MaterialButton btnConnect;
    private TextView tvStatus;

    private PingClient pingClient;
    private AppContainer container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setup);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootSetup), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etIp        = findViewById(R.id.etIp);
        btnConnect  = findViewById(R.id.btnConnect);
        tvStatus    = findViewById(R.id.tvStatus);

        container = ((SmartHomeApp) getApplication()).getContainer();
        pingClient = new PingClient();

        btnConnect.setOnClickListener(v -> attemptConnect());
    }

    private void attemptConnect() {
        String ip = etIp.getText() != null ? etIp.getText().toString().trim() : "";

        if (TextUtils.isEmpty(ip)) {
            etIp.setError("Enter an IP address");
            return;
        }

        // Basic validation — 4 groups of digits separated by dots
        if (!isProbablyIpv4(ip)) {
            etIp.setError("Enter a valid IP, e.g. 192.168.1.42");
            return;
        }

        etIp.setError(null);
        btnConnect.setEnabled(false);
        tvStatus.setText("Connecting…");
        tvStatus.setTextColor(getColor(R.color.text_secondary));

        pingClient.ping(ip, new PingClient.Callback() {
            @Override
            public void onSuccess(String body) {
                if (body.contains("\"ok\":true")) {
                    tvStatus.setText("Connected ✅ Saving…");
                    tvStatus.setTextColor(getColor(R.color.success));
                    saveDeviceAndProceed(ip);
                } else {
                    btnConnect.setEnabled(true);
                    tvStatus.setText("Reached device but unexpected reply");
                    tvStatus.setTextColor(getColor(R.color.error));
                }
            }

            @Override
            public void onFailure(String reason) {
                btnConnect.setEnabled(true);
                tvStatus.setText("Failed: " + reason);
                tvStatus.setTextColor(getColor(R.color.error));
            }
        });
    }

    private void saveDeviceAndProceed(String ip) {
        container.deviceRepository.saveDevice("Home", ip,
                new DeviceRepository.Callback<Long>() {
                    @Override
                    public void onSuccess(Long deviceId) {
                        // Kick off /info fetch so pins are cached before user reaches Add screen
                        container.deviceRepository.fetchAndSaveInfo(deviceId,
                                new DeviceRepository.Callback<com.legacycodes.smarthome.data.remote.dto.InfoResponse>() {
                                    @Override
                                    public void onSuccess(com.legacycodes.smarthome.data.remote.dto.InfoResponse info) {
                                        goHome();
                                    }

                                    @Override
                                    public void onError(String reason) {
                                        // Device is saved, /info failed — still proceed. User can
                                        // pull-to-refresh on home to retry.
                                        goHome();
                                    }
                                });
                    }

                    @Override
                    public void onError(String reason) {
                        btnConnect.setEnabled(true);
                        tvStatus.setText("Save failed: " + reason);
                        tvStatus.setTextColor(getColor(R.color.error));
                    }
                });
    }

    private void goHome() {
        Toast.makeText(this, "Connected to your home", Toast.LENGTH_SHORT).show();
        Intent i = new Intent(this, HomeActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    /**
     * Very loose check: 4 dot-separated groups of digits.
     * Not full IPv4 validation — the PingClient will fail fast on unreachable
     * addresses anyway. This just catches obvious typos.
     */
    private static boolean isProbablyIpv4(String s) {
        String[] parts = s.split("\\.");
        if (parts.length != 4) return false;
        for (String p : parts) {
            if (p.isEmpty() || p.length() > 3) return false;
            for (int i = 0; i < p.length(); i++) {
                if (!Character.isDigit(p.charAt(i))) return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pingClient != null) pingClient.shutdown();
    }
}