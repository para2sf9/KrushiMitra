package com.example.ai_agri;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ai_agri.adapters.MandiAdapter;
import com.example.ai_agri.models.MandiModel;
import com.example.ai_agri.utils.CropImageUtil;
import com.example.ai_agri.utils.MandiCache;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MandiActivity extends AppCompatActivity {

    private static final String TAG = "MandiActivity";
    private static final String API_URL = "https://api.data.gov.in/resource/9ef84268-d588-465a-a308-a864a43d0070?api-key=579b464db66ec23bdd00000116db7a973cb14be241ccbe09e91ec311&format=json&limit=900";
    private static final String MANDI_BHAV_URL = "https://finnid.in/MarketLinkage/LivePrices";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    RecyclerView recyclerView;
    List<MandiModel> list;
    List<MandiModel> filteredList;
    MandiAdapter adapter;
    ProgressBar progressBar;
    EditText etSearchMandi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mandi);

        recyclerView = findViewById(R.id.mandiRecycler);
        progressBar = findViewById(R.id.progressBar);
        etSearchMandi = findViewById(R.id.etSearchMandi);
        View btnBack = findViewById(R.id.btnBack);
        
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View btnInfo = findViewById(R.id.btnInfo);
        if (btnInfo != null) {
            btnInfo.setVisibility(View.VISIBLE);
            btnInfo.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MANDI_BHAV_URL));
                startActivity(intent);
            });
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        list = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new MandiAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);

        etSearchMandi.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterData(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadCachedData();
        fetchMandiData();
    }

    private void loadCachedData() {
        String cachedJson = MandiCache.getCachedMandiData(this);
        if (cachedJson != null) {
            parseAndDisplayData(cachedJson, false);
        }
    }

    private void filterData(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(list);
        } else {
            for (MandiModel model : list) {
                if (model.getCropName().toLowerCase().contains(query.toLowerCase()) ||
                    model.getMandi().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(model);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void fetchMandiData() {
        if (list.isEmpty() && progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        Request request = new Request.Builder()
                .url(API_URL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && (errorMsg.contains("Unable to resolve host") || errorMsg.contains("timeout"))) {
                        Toast.makeText(MandiActivity.this, "Network error: Mandi API is slow. Showing cached data.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MandiActivity.this, getString(R.string.failed_to_fetch_data), Toast.LENGTH_SHORT).show();
                    }
                    Log.e(TAG, "onFailure: ", e);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) {
                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    });
                    return;
                }

                try (Response res = response) {
                    String jsonData = res.body().string();
                    if (!res.isSuccessful()) {
                        runOnUiThread(() -> {
                            if (progressBar != null) progressBar.setVisibility(View.GONE);
                        });
                        return;
                    }

                    MandiCache.saveMandiData(MandiActivity.this, jsonData);
                    parseAndDisplayData(jsonData, true);

                } catch (Exception e) {
                    Log.e(TAG, "Error processing Mandi response", e);
                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private void parseAndDisplayData(String jsonData, boolean isFresh) {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
            if (jsonObject.has("records")) {
                JsonArray records = jsonObject.getAsJsonArray("records");
                List<MandiModel> fetchedList = new ArrayList<>();
                for (JsonElement element : records) {
                    JsonObject record = element.getAsJsonObject();
                    String commodity = record.has("commodity") ? record.get("commodity").getAsString() :
                            record.has("Commodity") ? record.get("Commodity").getAsString() : getString(R.string.unknown);
                    String market = record.has("market") ? record.get("market").getAsString() :
                            record.has("Market") ? record.get("Market").getAsString() : getString(R.string.unknown_market);
                    String district = record.has("district") ? record.get("district").getAsString() :
                            record.has("District") ? record.get("District").getAsString() : "";
                    String state = record.has("state") ? record.get("state").getAsString() :
                            record.has("State") ? record.get("State").getAsString() : "";

                    String modalPrice = record.has("modal_price") ? record.get("modal_price").getAsString() :
                            record.has("Modal_Price") ? record.get("Modal_Price").getAsString() : "0";

                    int imageRes = CropImageUtil.getLocalResource(commodity);
                    String imageUrl = null;
                    if (imageRes == R.drawable.leaf) {
                        imageUrl = CropImageUtil.getOnlineUrl(commodity);
                    }

                    String priceDisplay = "₹" + modalPrice;
                    String location = market + "\n" + district + ", " + state;

                    boolean isUp = Math.random() > 0.5;
                    String trend = isUp ? "+5%" : "-2%";

                    if (imageUrl != null) {
                        fetchedList.add(new MandiModel(commodity, location, priceDisplay, trend, imageUrl, isUp));
                    } else {
                        fetchedList.add(new MandiModel(commodity, location, priceDisplay, trend, imageRes, isUp));
                    }
                }

                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    list.clear();
                    Set<String> addedKeys = new HashSet<>();
                    for (MandiModel model : fetchedList) {
                        String key = model.getCropName() + "_" + model.getMandi();
                        if (addedKeys.add(key)) {
                            list.add(model);
                        }
                    }
                    filterData(etSearchMandi.getText().toString());
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON", e);
            if (isFresh) {
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(MandiActivity.this, getString(R.string.parsing_error), Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
}
