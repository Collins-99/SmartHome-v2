package com.legacycodes.smarthome.ui.profile;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.legacycodes.smarthome.AppContainer;
import com.legacycodes.smarthome.R;
import com.legacycodes.smarthome.SmartHomeApp;
import com.legacycodes.smarthome.data.repository.ProfileRepository;

/**
 * Profile screen.
 *
 * Loads the current profile from Room and pre-fills fields.
 * On save: writes to Room via ProfileRepository, then finishes.
 *
 * The home header (tvUserGreeting / tvHouseName) updates automatically
 * because it observes the profile LiveData — no manual refresh needed.
 */
public class ProfileActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputEditText etUserName;
    private TextInputEditText etHouseName;
    private MaterialButton btnSave;

    private AppContainer container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootProfile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar      = findViewById(R.id.toolbar);
        etUserName   = findViewById(R.id.etUserName);
        etHouseName  = findViewById(R.id.etHouseName);
        btnSave      = findViewById(R.id.btnSave);

        container = ((SmartHomeApp) getApplication()).getContainer();

        toolbar.setNavigationOnClickListener(v -> finish());

        // Pre-fill existing profile
        container.profileRepository.observe().observe(this, profile -> {
            if (profile != null) {
                if (etUserName.getText() == null || etUserName.getText().length() == 0) {
                    etUserName.setText(profile.userName);
                }
                if (etHouseName.getText() == null || etHouseName.getText().length() == 0) {
                    etHouseName.setText(profile.houseName);
                }
            }
        });

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        String userName = etUserName.getText() != null ? etUserName.getText().toString().trim() : "";
        String houseName = etHouseName.getText() != null ? etHouseName.getText().toString().trim() : "";

        if (userName.isEmpty() && houseName.isEmpty()) {
            Toast.makeText(this, "Enter at least one field", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        container.profileRepository.save(userName, houseName,
                new ProfileRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void ignored) {
                        btnSave.setEnabled(true);
                        Toast.makeText(ProfileActivity.this,
                                "Profile saved", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    public void onError(String reason) {
                        btnSave.setEnabled(true);
                        Toast.makeText(ProfileActivity.this,
                                "Save failed: " + reason, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}