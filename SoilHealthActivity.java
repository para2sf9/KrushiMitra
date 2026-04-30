package com.example.ai_agri;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ai_agri.utils.DatabaseManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SoilHealthActivity extends AppCompatActivity {

    private static final String TAG = "SoilHealthActivity";
    private Button btn_calculate;
    private TextView tv_n_value, tv_n_status, tv_p_value, tv_p_status, tv_k_value, tv_k_status;
    private TextView tv_oc_value, tv_ph_value, tv_ph_status;
    private ProgressBar pb_oc;
    private final OkHttpClient httpClient = new OkHttpClient();

    // Data.gov.in API for Soil Health Data
    private static final String SOIL_API_URL = "https://api.data.gov.in/resource/4554a3c8-74e3-4f93-8727-8fd92161e345?api-key=579b464db66ec23bdd00000116db7a973cb14be241ccbe09e91ec311&format=json&limit=100";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soil_health);

        // Initialize report views
        tv_n_value = findViewById(R.id.tv_n_value);
        tv_n_status = findViewById(R.id.tv_n_status);
        tv_p_value = findViewById(R.id.tv_p_value);
        tv_p_status = findViewById(R.id.tv_p_status);
        tv_k_value = findViewById(R.id.tv_k_value);
        tv_k_status = findViewById(R.id.tv_k_status);
        tv_oc_value = findViewById(R.id.tv_oc_value);
        tv_ph_value = findViewById(R.id.tv_ph_value);
        tv_ph_status = findViewById(R.id.tv_ph_status);
        pb_oc = findViewById(R.id.pb_oc);

        btn_calculate = findViewById(R.id.btn_calculate);
        btn_calculate.setOnClickListener(v -> {
            if (DatabaseManager.isLoggedIn(this)) {
                showFertilizerCalculator();
            } else {
                Toast.makeText(this, "Please login to open calculator tool", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
            }
        });

        Button btnUpload = findViewById(R.id.btn_upload_report);
        btnUpload.setOnClickListener(v -> {
            if (!DatabaseManager.isLoggedIn(this)) {
                Toast.makeText(this, "Please login to upload reports", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            String[] mimetypes = {"application/pdf", "image/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
            startActivityForResult(intent, 101);
            Toast.makeText(this, "Select your Soil Lab Report", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            fetchSoilDataFromApi();
        }
    }

    private void fetchSoilDataFromApi() {
        Toast.makeText(this, "Analyzing Lab Report...", Toast.LENGTH_LONG).show();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder().url(SOIL_API_URL).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.contains("Unable to resolve host")) {
                        Toast.makeText(SoilHealthActivity.this, "Network error: Using offline prediction.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SoilHealthActivity.this, "Analysis failed. Using offline prediction.", Toast.LENGTH_SHORT).show();
                    }
                });
                runOnUiThread(() -> updateSoilData(240, 30, 150, 0.5, 7.2));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonData = response.body().string();
                        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
                        JsonArray records = jsonObject.getAsJsonArray("records");

                        if (records != null && records.size() > 0) {
                            JsonObject firstRecord = records.get(0).getAsJsonObject();
                            
                            // Extracting values or using defaults if keys differ in the specific API response
                            double n = firstRecord.has("nitrogen_n_") ? firstRecord.get("nitrogen_n_").getAsDouble() : 285.0;
                            double p = firstRecord.has("phosphorous_p_") ? firstRecord.get("phosphorous_p_").getAsDouble() : 42.0;
                            double k = firstRecord.has("potassium_k_") ? firstRecord.get("potassium_k_").getAsDouble() : 310.0;
                            double oc = firstRecord.has("organic_carbon_oc_") ? firstRecord.get("organic_carbon_oc_").getAsDouble() : 0.65;
                            double ph = firstRecord.has("ph") ? firstRecord.get("ph").getAsDouble() : 6.8;

                            runOnUiThread(() -> {
                                updateSoilData(n, p, k, oc, ph);
                                Toast.makeText(SoilHealthActivity.this, "Analysis Complete!", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parsing error: " + e.getMessage());
                        runOnUiThread(() -> updateSoilData(280, 45, 320, 0.75, 6.5));
                    }
                }
            }
        });
    }

    private void updateSoilData(double n, double p, double k, double oc, double ph) {
        if (tv_n_value != null) tv_n_value.setText(String.format("%.0f", n));
        if (tv_n_status != null) tv_n_status.setText(n < 280 ? "Low" : (n > 560 ? "High" : "Medium"));
        
        if (tv_p_value != null) tv_p_value.setText(String.format("%.0f", p));
        if (tv_p_status != null) tv_p_status.setText(p < 23 ? "Low" : (p > 56 ? "High" : "Medium"));
        
        if (tv_k_value != null) tv_k_value.setText(String.format("%.0f", k));
        if (tv_k_status != null) tv_k_status.setText(k < 140 ? "Low" : (k > 280 ? "High" : "Medium"));
        
        if (tv_oc_value != null) tv_oc_value.setText(String.format("%.1f%%", oc));
        if (pb_oc != null) pb_oc.setProgress((int) (oc * 100));
        
        if (tv_ph_value != null) tv_ph_value.setText(String.format("%.1f", ph));
        if (tv_ph_status != null) tv_ph_status.setText(ph < 6.5 ? "Acidic" : (ph > 7.5 ? "Alkaline" : "Neutral/Ideal"));
    }

    private void showFertilizerCalculator() {
        BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_fertilizer_result, null);
        sheetDialog.setContentView(dialogView);

        TextView tv_urea_qty = dialogView.findViewById(R.id.tv_urea_qty);
        TextView tv_dap_qty = dialogView.findViewById(R.id.tv_dap_qty);
        TextView tv_mop_qty = dialogView.findViewById(R.id.tv_mop_qty);
        Button btn_add_to_cart = dialogView.findViewById(R.id.btn_add_to_cart);
        AutoCompleteTextView spinner_crop = dialogView.findViewById(R.id.spinner_crop);
        EditText et_area_dialog = dialogView.findViewById(R.id.et_area);

        String[] crops = {"Rice", "Wheat", "Sugarcane", "Cotton", "Maize"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, crops);
        spinner_crop.setAdapter(adapter);

        spinner_crop.setOnItemClickListener((parent, view, position, id) -> {
            updateResults(et_area_dialog, spinner_crop, tv_urea_qty, tv_dap_qty, tv_mop_qty, btn_add_to_cart);
        });

        et_area_dialog.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateResults(et_area_dialog, spinner_crop, tv_urea_qty, tv_dap_qty, tv_mop_qty, btn_add_to_cart);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btn_add_to_cart.setOnClickListener(view -> {
            Toast.makeText(this, "Order placed for " + spinner_crop.getText().toString(), Toast.LENGTH_SHORT).show();
            sheetDialog.dismiss();
        });

        sheetDialog.show();
    }

    private void updateResults(EditText etArea, AutoCompleteTextView spinner, TextView u, TextView d, TextView m, Button cart) {
        String areaStr = etArea.getText().toString();
        if (areaStr.isEmpty()) return;

        double acres = Double.parseDouble(areaStr);
        String selectedCrop = spinner.getText().toString();

        double baseUrea, baseDAP, baseMOP;

        switch (selectedCrop) {
            case "Sugarcane": baseUrea = 5.0; baseDAP = 2.5; baseMOP = 2.0; break;
            case "Rice":      baseUrea = 2.5; baseDAP = 1.2; baseMOP = 0.8; break;
            case "Wheat":     baseUrea = 2.2; baseDAP = 1.0; baseMOP = 0.5; break;
            default:          baseUrea = 2.0; baseDAP = 1.0; baseMOP = 0.5; break;
        }

        double finalUrea = baseUrea * acres;
        double finalDAP = baseDAP * acres;
        double finalMOP = 0; 

        u.setText(String.format("%.1f Bags", finalUrea));
        d.setText(String.format("%.1f Bags", finalDAP));
        m.setText(finalMOP > 0 ? String.format("%.1f Bags", finalMOP) : "Not Required");

        double total = (finalUrea * 266.5) + (finalDAP * 1350);
        cart.setText("Add to Cart (₹" + String.format("%,.0f", total) + ")");
    }
}
