package com.example.ai_agri;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ai_agri.adapters.FarmAdapter;
import com.example.ai_agri.models.FarmRecordModel;
import com.example.ai_agri.utils.DatabaseManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class TrackingActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ExtendedFloatingActionButton addBtn;
    ArrayList<FarmRecordModel> list;
    FarmAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!DatabaseManager.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Edge-to-edge UI
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(ContextCompat.getColor(this, R.color.primaryGreen)),
                SystemBarStyle.dark(ContextCompat.getColor(this, R.color.primaryGreen)));
        setContentView(R.layout.activity_tracking);

        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        recyclerView = findViewById(R.id.recycler);
        addBtn = findViewById(R.id.add_btn);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        list = new ArrayList<>();
        adapter = new FarmAdapter(this, list);
        recyclerView.setAdapter(adapter);

        loadRecords();

        // Floating Button Click
        addBtn.setOnClickListener(v -> {
            startActivityForResult(new Intent(this, AddRecordActivity.class), 101);
        });
    }

    private void loadRecords() {
        DatabaseManager.getFarmRecords(this, new DatabaseManager.AuthCallback() {
            @Override
            public void onSuccess(String responseData) {
                try {
                    com.google.gson.JsonObject responseJson = new com.google.gson.Gson().fromJson(responseData, com.google.gson.JsonObject.class);
                    if (responseJson.has("rows")) {
                        com.google.gson.JsonArray rows = responseJson.getAsJsonArray("rows");
                        list.clear();
                        for (int i = 0; i < rows.size(); i++) {
                            com.google.gson.JsonObject row = rows.get(i).getAsJsonObject();
                            list.add(new FarmRecordModel(
                                    row.get("id").getAsInt(),
                                    row.get("crop").getAsString(),
                                    row.get("area").getAsString(),
                                    row.get("income").getAsString(),
                                    row.get("expenses").getAsString(),
                                    row.get("yield").getAsString(),
                                    row.get("record_date").getAsString()
                            ));
                        }
                        runOnUiThread(() -> adapter.notifyDataSetChanged());
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(TrackingActivity.this, "Failed to parse records", Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> Toast.makeText(TrackingActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK) {
            loadRecords();
        }
    }
}