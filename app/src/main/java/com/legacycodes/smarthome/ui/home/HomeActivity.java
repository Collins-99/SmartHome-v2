package com.legacycodes.smarthome.ui.home;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.legacycodes.smarthome.R;
import com.legacycodes.smarthome.data.local.entity.ApplianceEntity;
import com.legacycodes.smarthome.data.local.entity.DeviceEntity;
import com.legacycodes.smarthome.data.local.entity.ProfileEntity;

import java.util.List;

/**
 * Home screen. First visible screen of the app.
 *
 * Responsibilities:
 *   - Wire ViewModel → RecyclerView + header
 *   - Toggle visibility between recycler and empty state
 *   - Handle toggle clicks, FAB clicks, pull-to-refresh (3.10)
 *   - Observe offline state (3.10)
 *
 * This activity is intentionally thin — all data logic lives in HomeViewModel.
 */
public class HomeActivity extends AppCompatActivity {

    private HomeViewModel viewModel;
    private ApplianceAdapter adapter;

    private RecyclerView recycler;
    private View emptyState;
    private ExtendedFloatingActionButton fabAdd;
    private TextView tvUserGreeting;
    private TextView tvHouseName;
    private TextView tvDeviceStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootHome), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bind views
        recycler         = findViewById(R.id.recyclerAppliances);
        emptyState       = findViewById(R.id.emptyState);
        fabAdd           = findViewById(R.id.fabAdd);
        tvUserGreeting   = findViewById(R.id.tvUserGreeting);
        tvHouseName      = findViewById(R.id.tvHouseName);
        tvDeviceStatus   = findViewById(R.id.tvDeviceStatus);

        // Recycler setup
        adapter = new ApplianceAdapter(new ApplianceAdapter.OnApplianceClickListener() {
            @Override
            public void onCardClick(ApplianceEntity appliance) {
                // Placeholder for Phase 4: open edit screen
                Toast.makeText(HomeActivity.this,
                        "Tapped: " + appliance.name, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onToggleClick(ApplianceEntity appliance, boolean newCheckedState) {
                viewModel.toggleAppliance(appliance, newCheckedState);
            }
        });

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        recycler.setItemAnimator(new DefaultItemAnimator());
        recycler.setHasFixedSize(true);

        // ViewModel
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        observeProfile();
        observeDevice();
        observeAppliances();
        observeMessages();

        // FAB — placeholder; routed to AddEditActivity in 3.11
        fabAdd.setOnClickListener(v -> {
            Toast.makeText(this, "Add screen coming in 3.11", Toast.LENGTH_SHORT).show();
        });


    }

    // ---- LiveData observers ----

    private void observeProfile() {
        viewModel.getProfile().observe(this, profile -> {
            if (profile == null || (isEmpty(profile.userName) && isEmpty(profile.houseName))) {
                tvUserGreeting.setText("WELCOME");
                tvHouseName.setText("My Home");
            } else {
                String user = isEmpty(profile.userName) ? "WELCOME" : ("HI, " + profile.userName.toUpperCase());
                String house = isEmpty(profile.houseName) ? "My Home" : profile.houseName;
                tvUserGreeting.setText(user);
                tvHouseName.setText(house);
            }
        });
    }

    private void observeDevice() {
        viewModel.getDevice().observe(this, device -> {
            updateDeviceStatus(device);
            if (device != null) {
                // Trigger one-time /info fetch if pins not yet cached
                viewModel.fetchDeviceInfoIfNeeded();
            }
        });
    }

    private void observeAppliances() {
        viewModel.getAppliances().observe(this, appliances -> {
            adapter.submitList(appliances);
            boolean isEmpty = appliances == null || appliances.isEmpty();
            emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            recycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });
    }

    private void observeMessages() {
        viewModel.getMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });
    }

    // ---- Helpers ----

    private void updateDeviceStatus(DeviceEntity device) {
        if (device == null) {
            tvDeviceStatus.setText("No device configured");
            return;
        }
        String name = device.deviceName != null ? device.deviceName : "HomeController";
        String fw = device.firmware != null ? "v" + device.firmware : "";
        tvDeviceStatus.setText(name + (fw.isEmpty() ? "" : " · " + fw));
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}