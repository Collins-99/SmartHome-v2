package com.legacycodes.smarthome.ui.setup;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.legacycodes.smarthome.AppContainer;
import com.legacycodes.smarthome.R;
import com.legacycodes.smarthome.SmartHomeApp;
import com.legacycodes.smarthome.data.local.entity.DeviceEntity;
import com.legacycodes.smarthome.ui.home.HomeActivity;
import com.legacycodes.smarthome.util.AppExecutors;

/**
 * The real launcher. A blank spinner screen that decides where to send the user.
 *
 *   Room has a device row?  → YES: HomeActivity
 *                           → NO:  SetupActivity
 *
 * Runs on a background thread (Room query is blocking). Finishes itself
 * immediately after launching the next activity so the back button can't
 * return to a blank screen.
 *
 * Why a separate activity instead of logic in HomeActivity.onCreate?
 *   - Keeps HomeActivity clean — it assumes a device exists
 *   - Makes the "no device → setup" decision explicit and testable
 *   - SetupActivity and HomeActivity both remain independently launchable
 */
public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_launcher);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLauncher), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        AppContainer container = ((SmartHomeApp) getApplication()).getContainer();

        AppExecutors.get().diskIO().execute(() -> {
            DeviceEntity device = null;
            try {
                device = container.database.deviceDao().getPrimaryBlocking();
            } catch (Exception ignored) {
            }
            final boolean hasDevice = device != null && device.ipAddress != null
                    && !device.ipAddress.isEmpty();

            AppExecutors.get().mainThread().execute(() -> {
                Intent next;
                if (hasDevice) {
                    next = new Intent(this, HomeActivity.class);
                } else {
                    next = new Intent(this, SetupActivity.class);
                }
                next.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(next);
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                finish();
            });
        });
    }
}