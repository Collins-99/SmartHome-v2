package com.legacycodes.smarthome.ui.addedit;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.legacycodes.smarthome.R;
import com.legacycodes.smarthome.SmartHomeApp;
import com.legacycodes.smarthome.data.local.entity.ApplianceEntity;
import com.legacycodes.smarthome.domain.model.ApplianceType;
import com.legacycodes.smarthome.util.AppExecutors;

import java.util.ArrayList;
import java.util.List;

public class AddEditActivity extends AppCompatActivity {

    public static final String EXTRA_APPLIANCE_ID = "extra_appliance_id";

    private AddEditViewModel viewModel;
    private ApplianceTypeAdapter typeAdapter;
    private ArrayAdapter<String> pinAdapter;
    private List<Integer> pinOptions = new ArrayList<>();
    private ApplianceEntity editing;

    private MaterialToolbar toolbar;
    private RecyclerView recyclerTypes;
    private TextInputEditText etName;
    private MaterialAutoCompleteTextView etPin;
    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_edit);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootAddEdit), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar       = findViewById(R.id.toolbar);
        recyclerTypes = findViewById(R.id.recyclerTypes);
        etName        = findViewById(R.id.etName);
        etPin         = findViewById(R.id.etPin);
        btnSave       = findViewById(R.id.btnSave);

        toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
        });

        viewModel = new ViewModelProvider(this).get(AddEditViewModel.class);
        long applianceId = getIntent().getLongExtra(EXTRA_APPLIANCE_ID, 0L);
        viewModel.init(applianceId);

        boolean editMode = applianceId > 0;
        toolbar.setTitle(editMode ? "Edit Appliance" : "Add Appliance");

        typeAdapter = new ApplianceTypeAdapter(type -> {
            viewModel.setSelectedType(type);
            if (etName.getText() == null || etName.getText().toString().trim().isEmpty()) {
                etName.setText(type.getDisplayName());
            }
        });
        recyclerTypes.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerTypes.setAdapter(typeAdapter);

        pinAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, new ArrayList<>());
        etPin.setAdapter(pinAdapter);

        etName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                viewModel.setSelectedName(s != null ? s.toString() : "");
            }
        });

        etPin.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < pinOptions.size()) {
                viewModel.setSelectedPin(pinOptions.get(position));
            }
        });

        btnSave.setOnClickListener(v -> attemptSave());

        observePins();
        if (editMode) {
            loadExisting(applianceId);
        }
    }

    private void observePins() {
        viewModel.getAvailablePins().observe(this, pins -> {
            pinOptions = pins != null ? pins : new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (Integer p : pinOptions) {
                labels.add("GPIO " + p);
            }
            pinAdapter.clear();
            pinAdapter.addAll(labels);
            pinAdapter.notifyDataSetChanged();

            if (editing != null && pinOptions.contains(editing.gpioPin)) {
                String label = "GPIO " + editing.gpioPin;
                etPin.setText(label, false);
                viewModel.setSelectedPin(editing.gpioPin);
            }
        });
    }

    private void loadExisting(long applianceId) {
        AppExecutors.get().diskIO().execute(() -> {
            ApplianceEntity entity;
            try {
                entity = ((SmartHomeApp) getApplication()).getContainer()
                        .database.applianceDao().getByIdBlocking(applianceId);
            } catch (Exception e) {
                entity = null;
            }
            if (entity == null) return;
            final ApplianceEntity loaded = entity;
            editing = loaded;
            AppExecutors.get().mainThread().execute(() -> {
                viewModel.prefill(loaded);
                typeAdapter.setSelected(ApplianceType.fromName(loaded.type));
                etName.setText(loaded.name != null ? loaded.name : "");
                viewModel.setSelectedName(loaded.name);
                viewModel.setSelectedPin(loaded.gpioPin);
            });
        });
    }

    private void attemptSave() {
        viewModel.setSelectedName(etName.getText() != null ? etName.getText().toString() : "");

        btnSave.setEnabled(false);
        viewModel.save(new AddEditViewModel.SaveCallback() {
            @Override
            public void onSuccess(long applianceId) {
                btnSave.setEnabled(true);
                Toast.makeText(AddEditActivity.this,
                        editing == null ? "Appliance added" : "Appliance updated",
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
                overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
            }

            @Override
            public void onError(String reason) {
                btnSave.setEnabled(true);
                Toast.makeText(AddEditActivity.this, reason, Toast.LENGTH_SHORT).show();
            }
        });
    }
}