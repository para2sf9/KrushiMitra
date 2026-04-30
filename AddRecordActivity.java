package com.example.ai_agri;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ai_agri.utils.DatabaseManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AddRecordActivity extends AppCompatActivity {

    TextInputEditText cropInput, areaInput, incomeInput, expensesInput, yieldInput;
    MaterialButton saveBtn;
    MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!DatabaseManager.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_add_record);

        toolbar = findViewById(R.id.toolbar);
        cropInput = findViewById(R.id.crop_input);
        areaInput = findViewById(R.id.area_input);
        incomeInput = findViewById(R.id.income_input);
        expensesInput = findViewById(R.id.expenses_input);
        yieldInput = findViewById(R.id.yield_input);
        saveBtn = findViewById(R.id.save_btn);

        toolbar.setNavigationOnClickListener(v -> finish());

        saveBtn.setOnClickListener(v -> {
            String crop = cropInput.getText().toString().trim();
            String area = areaInput.getText().toString().trim();
            String income = incomeInput.getText().toString().trim();
            String expenses = expensesInput.getText().toString().trim();
            String yield = yieldInput.getText().toString().trim();

            if (crop.isEmpty() || area.isEmpty() || income.isEmpty() || expenses.isEmpty() || yield.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            saveBtn.setEnabled(false);
            saveBtn.setText("Saving...");

            DatabaseManager.addFarmRecord(this, crop, area, income, expenses, yield, new DatabaseManager.AuthCallback() {
                @Override
                public void onSuccess(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddRecordActivity.this, "Record saved!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        saveBtn.setEnabled(true);
                        saveBtn.setText("Save Record");
                        Toast.makeText(AddRecordActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
    }
}
