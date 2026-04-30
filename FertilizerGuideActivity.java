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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FertilizerGuideActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_fertilizer_guide);

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
        crops.add(new CropRequirement("Onion", 100, 50, 50));
        crops.add(new CropRequirement("Chilli", 120, 60, 60));
        crops.add(new CropRequirement("Groundnut", 25, 50, 75));

        ArrayAdapter<CropRequirement> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, crops);
        cropSpinner.setAdapter(adapter);
    }

    private void fetchFertilizerPrices() {
        String url = "https://api.data.gov.in/resource/b73c4670-a371-4747-824c-4ea767918dc9?api-key=579b464db66ec23bdd00000116db7a973cb14be241ccbe09e91ec311&format=json&filters%5Bfertilizer%5D=all";

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
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

    private void setDefaultPrices() {
        // Fallback prices per kg if API fails or doesn't have the data
        fertilizerPrices.put("Urea", 6.0);
        fertilizerPrices.put("DAP", 27.0);
        fertilizerPrices.put("MOP", 34.0);
    }

    private void parseJsonPrices(String jsonData) {
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            if (jsonObject.has("records")) {
                JSONArray records = jsonObject.getJSONArray("records");
                for (int i = 0; i < records.length(); i++) {
                    JSONObject record = records.getJSONObject(i);
                    String name = record.optString("fertilizer", "").toUpperCase();
                    String priceStr = record.optString("price", "0");
                    try {
                        double price = Double.parseDouble(priceStr);
                        if (!name.isEmpty() && price > 0) {
                            // Normalize price to per kg if it's per 50kg bag
                            if (price > 100) price = price / 50.0;
                            
                            if (name.contains("UREA")) fertilizerPrices.put("Urea", price);
                            else if (name.contains("DAP")) fertilizerPrices.put("DAP", price);
                            else if (name.contains("MOP") || name.contains("POTASH")) fertilizerPrices.put("MOP", price);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        runOnUiThread(() -> {
            if (!fertilizerPrices.containsKey("Urea")) fertilizerPrices.put("Urea", 6.0);
            if (!fertilizerPrices.containsKey("DAP")) fertilizerPrices.put("DAP", 27.0);
            if (!fertilizerPrices.containsKey("MOP")) fertilizerPrices.put("MOP", 34.0);
        });
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

        // DAP contains 18% N and 46% P2O5
        // Urea contains 46% N
        // MOP contains 60% K2O

        double dapNeeded = totalP / 0.46;
        double nFromDap = dapNeeded * 0.18;
        double remainingN = totalN - nFromDap;
        double ureaNeeded = Math.max(0, remainingN / 0.46);
        double mopNeeded = totalK / 0.60;

        tvResultNPK.setText(String.format(Locale.getDefault(), "Required NPK: N:%.1f kg, P:%.1f kg, K:%.1f kg", totalN, totalP, totalK));
        tvResultFertilizers.setText(String.format(Locale.getDefault(), "Quantity: Urea: %.1f kg, DAP: %.1f kg, MOP: %.1f kg", ureaNeeded, dapNeeded, mopNeeded));

        double ureaPrice = fertilizerPrices.getOrDefault("Urea", 6.0);
        double dapPrice = fertilizerPrices.getOrDefault("DAP", 27.0);
        double mopPrice = fertilizerPrices.getOrDefault("MOP", 34.0);

        double totalPrice = (ureaNeeded * ureaPrice) + (dapNeeded * dapPrice) + (mopNeeded * mopPrice);

        tvResultPrice.setText(String.format(Locale.getDefault(), "Estimated Total Cost: ₹%.2f", totalPrice));
        resultLayout.setVisibility(View.VISIBLE);
    }
}
