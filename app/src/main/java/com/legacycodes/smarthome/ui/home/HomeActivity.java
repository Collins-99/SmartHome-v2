package com.legacycodes.smarthome.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.legacycodes.smarthome.R;
import com.legacycodes.smarthome.SmartHomeApp;
import com.legacycodes.smarthome.data.local.entity.ApplianceEntity;
import com.legacycodes.smarthome.data.local.entity.DeviceEntity;
import com.legacycodes.smarthome.data.repository.ApplianceRepository;
import com.legacycodes.smarthome.ui.addedit.AddEditActivity;
import com.legacycodes.smarthome.ui.profile.ProfileActivity;
import com.legacycodes.smarthome.ui.setup.SetupActivity;
import com.legacycodes.smarthome.util.AppExecutors;

public class HomeActivity extends AppCompatActivity {

    private HomeViewModel viewModel;
    private ApplianceAdapter adapter;

    private RecyclerView recycler;
    private View emptyState;
    private View offlineBanner;
    private SwipeRefreshLayout swipeRefresh;
    private ExtendedFloatingActionButton fabAdd;
    private ImageButton btnSettings;
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

        recycler        = findViewById(R.id.recyclerAppliances);
        emptyState      = findViewById(R.id.emptyState);
        offlineBanner   = findViewById(R.id.offlineBanner);
        swipeRefresh    = findViewById(R.id.swipeRefresh);
        fabAdd          = findViewById(R.id.fabAdd);
        btnSettings     = findViewById(R.id.btnSettings);
        tvUserGreeting  = findViewById(R.id.tvUserGreeting);
        tvHouseName     = findViewById(R.id.tvHouseName);
        tvDeviceStatus  = findViewById(R.id.tvDeviceStatus);

        adapter = new ApplianceAdapter(new ApplianceAdapter.OnApplianceClickListener() {
            @Override
            public void onCardClick(ApplianceEntity appliance) {
                Intent i = new Intent(HomeActivity.this, AddEditActivity.class);
                i.putExtra(AddEditActivity.EXTRA_APPLIANCE_ID, appliance.id);
                startActivity(i);
                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            }

            @Override
            public void onToggleClick(ApplianceEntity appliance, boolean newCheckedState) {
                viewModel.toggleAppliance(appliance, newCheckedState);
            }

            @Override
            public void onCardLongClick(ApplianceEntity appliance) {
                new AlertDialog.Builder(HomeActivity.this)
                        .setTitle("Delete " + appliance.name + "?")
                        .setMessage("This will remove it from your home. If it's currently on, the relay will be switched off first.")
                        .setPositiveButton("Delete", (dialog, which) -> deleteAppliance(appliance))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        recycler.setItemAnimator(new DefaultItemAnimator());
        recycler.setHasFixedSize(true);

        swipeRefresh.setColorSchemeResources(R.color.rose_primary, R.color.rose_primary_dark);
        swipeRefresh.setOnRefreshListener(() -> viewModel.refreshFromDevice());

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        observeProfile();
        observeDevice();
        observeAppliances();
        observeMessages();
        observeConnection();

        fabAdd.setOnClickListener(v -> {
            Intent i = new Intent(this, AddEditActivity.class);
            startActivity(i);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        View btnAddFirst = findViewById(R.id.btnAddFirst);
        if (btnAddFirst != null) {
            btnAddFirst.setOnClickListener(v -> {
                Intent i = new Intent(this, AddEditActivity.class);
                startActivity(i);
                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            });
        }

        btnSettings.setOnClickListener(v -> {
            Intent i = new Intent(this, ProfileActivity.class);
            startActivity(i);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        tvHouseName.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Reconfigure device?")
                    .setMessage("This will let you change the ESP32's IP address. Your appliances will not be deleted.")
                    .setPositiveButton("Reconfigure", (dialog, which) -> {
                        Intent i = new Intent(this, SetupActivity.class);
                        startActivity(i);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

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

    private void observeConnection() {
        viewModel.getConnected().observe(this, connected -> {
            boolean offline = connected == null || !connected;
            offlineBanner.setVisibility(offline && viewModel.getDevice().getValue() != null
                    ? View.VISIBLE : View.GONE);
            if (swipeRefresh.isRefreshing() && connected != null && connected) {
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    private void deleteAppliance(ApplianceEntity appliance) {
        AppExecutors.get().diskIO().execute(() -> {
            ((SmartHomeApp) getApplication()).getContainer()
                    .applianceRepository.deleteAppliance(appliance.id,
                            new ApplianceRepository.Callback<Void>() {
                                @Override
                                public void onSuccess(Void ignored) {
                                    runOnUiThread(() ->
                                            Toast.makeText(HomeActivity.this,
                                                    "Deleted " + appliance.name,
                                                    Toast.LENGTH_SHORT).show());
                                }

                                @Override
                                public void onError(String reason) {
                                    runOnUiThread(() ->
                                            Toast.makeText(HomeActivity.this,
                                                    "Delete failed: " + reason,
                                                    Toast.LENGTH_SHORT).show());
                                }
                            });
        });
    }

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