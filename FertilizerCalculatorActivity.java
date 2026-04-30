package com.example.ai_agri;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

public class FertilizerCalculatorActivity extends AppCompatActivity {

    private AutoCompleteTextView cropSpinner;
    private TextInputEditText etArea;
    private Button btnCalculate;
    private ProgressBar progressBar;
    private LinearLayout resultLayout;
    private TextView tvResultNPK, tvResultFertilizers, tvResultPrice;

    private final OkHttpClient client = new OkHttpClient();
    private final Map<String, Double> fertilizerPrices = new HashMap<>();
    private final List<CropRequirement> crops = new ArrayList<>();

    private static class CropRequirement {
        String name;
        double n, p, k; // per hectare

        CropRequirement(String name, double n, double p, double k) {
            this.name = name;
            this.n = n;
            this.p = p;
            this.k = k;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fertilizer_calculator);

        initViews();
        setupCrops();
        fetchFertilizerPrices();

        btnCalculate.setOnClickListener(v -> calculate());
    }

    private void initViews() {
        cropSpinner = findViewById(R.id.cropSpinner);
        etArea = findViewById(R.id.etArea);
        btnCalculate = findViewById(R.id.btnCalculate);
        progressBar = findViewById(R.id.progressBar);
        resultLayout = findViewById(R.id.resultLayout);
        tvResultNPK = findViewById(R.id.tvResultNPK);
        tvResultFertilizers = findViewById(R.id.tvResultFertilizers);
        tvResultPrice = findViewById(R.id.tvResultPrice);
    }

    private void setupCrops() {
        crops.add(new CropRequirement("Rice", 100, 50, 50));
        crops.add(new CropRequirement("Wheat", 120, 60, 40));
        crops.add(new CropRequirement("Maize", 120, 60, 40));
        crops.add(new CropRequirement("Cotton", 100, 50, 50));
        crops.add(new CropRequirement("Sugarcane", 250, 100, 100));
        crops.add(new CropRequirement("Potato", 120, 100, 100));
        crops.add(new CropRequirement("Tomato", 100, 60, 60));
        crops.add(new CropRequirement("Soybean", 20, 60, 40));
        crops.add(new CropRequirement("Mustard", 80, 40, 40));
        crops.add(new CropRequirement("Groundnut", 25, 50, 75));
        crops.add(new CropRequirement("Gram", 20, 40, 20));
        crops.add(new CropRequirement("Onion", 120, 50, 50));
        crops.add(new CropRequirement("Chili", 120, 60, 60));

        ArrayAdapter<CropRequirement> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, crops);
        cropSpinner.setAdapter(adapter);
    }

    private void fetchFertilizerPrices() {
        String url = "https://api.data.gov.in/resource/b73c4670-a371-4747-824c-4ea767918dc9?api-key=579b464db66ec23bdd000001cdd3946e44ce4aad7209ff7b23ac571b&format=json";

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(FertilizerCalculatorActivity.this, "Failed to fetch prices. Using offline data.", Toast.LENGTH_SHORT).show();
                    setDefaultPrices();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    parseJsonPrices(jsonData);
                } else {
                    runOnUiThread(() -> setDefaultPrices());
                }
            }
        });
    }

    private void parseJsonPrices(String jsonData) {
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            if (jsonObject.has("records")) {
                JSONArray records = jsonObject.getJSONArray("records");
                for (int i = 0; i < records.length(); i++) {
                    JSONObject record = records.getJSONObject(i);
                    String name = record.optString("fertilizer", "");
                    String priceStr = record.optString("maximum_retail_price__mrp_", "0");
                    try {
                        double price = Double.parseDouble(priceStr);
                        if (!name.isEmpty() && price > 0) {
                            double pricePerKg = price / 50.0;
                            
                            if (name.contains("Di-ammonium Phosphate (DAP)")) {
                                fertilizerPrices.put("DAP", pricePerKg);
                            } else if (name.contains("Muriate of Potash (MOP)")) {
                                fertilizerPrices.put("MOP", pricePerKg);
                            } else if (name.contains("Urea")) {
                                fertilizerPrices.put("Urea", pricePerKg);
                            }
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        runOnUiThread(() -> {
            if (fertilizerPrices.isEmpty()) {
                setDefaultPrices();
            }
        });
    }

    private void setDefaultPrices() {
        if (!fertilizerPrices.containsKey("Urea")) fertilizerPrices.put("Urea", 6.0);
        if (!fertilizerPrices.containsKey("DAP")) fertilizerPrices.put("DAP", 27.0);
        if (!fertilizerPrices.containsKey("MOP")) fertilizerPrices.put("MOP", 34.0);
    }

    private void calculate() {
        String areaStr = etArea.getText().toString();
        if (areaStr.isEmpty()) {
            etArea.setError("Enter area");
            return;
        }

        double area = Double.parseDouble(areaStr);
        String selectedCropName = cropSpinner.getText().toString();
        CropRequirement selectedCrop = null;
        for (CropRequirement crop : crops) {
            if (crop.name.equals(selectedCropName)) {
                selectedCrop = crop;
                break;
            }
        }

        if (selectedCrop == null) {
            cropSpinner.setError("Select a crop");
            return;
        }

        double totalN = selectedCrop.n * area;
        double totalP = selectedCrop.p * area;
        double totalK = selectedCrop.k * area;

        // Simplified NPK to Fertilizer conversion
        double dapNeeded = totalP / 0.46;
        double nFromDap = dapNeeded * 0.18;
        double remainingN = totalN - nFromDap;
        double ureaNeeded = Math.max(0, remainingN / 0.46);
        double mopNeeded = totalK / 0.60;

        // Convert to 50kg bags
        double ureaBags = ureaNeeded / 50.0;
        double dapBags = dapNeeded / 50.0;
        double mopBags = mopNeeded / 50.0;

        tvResultNPK.setText(String.format(Locale.getDefault(), "N: %.1f kg, P: %.1f kg, K: %.1f kg", totalN, totalP, totalK));
        tvResultFertilizers.setText(String.format(Locale.getDefault(), "Urea: %.1f bags, DAP: %.1f bags, MOP: %.1f bags", ureaBags, dapBags, mopBags));

        double ureaPrice = fertilizerPrices.getOrDefault("Urea", 6.0);
        double dapPrice = fertilizerPrices.getOrDefault("DAP", 27.0);
        double mopPrice = fertilizerPrices.getOrDefault("MOP", 34.0);

        double totalPrice = (ureaNeeded * ureaPrice) + (dapNeeded * dapPrice) + (mopNeeded * mopPrice);

        tvResultPrice.setText(String.format(Locale.getDefault(), "Estimated Price: ₹%.0f", totalPrice));
        resultLayout.setVisibility(View.VISIBLE);
    }
}
