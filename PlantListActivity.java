package com.example.ai_agri;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ai_agri.adapters.PlantAdapter;
import com.example.ai_agri.models.PlantModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;

public class PlantListActivity extends AppCompatActivity {

    RecyclerView recycler;
    ArrayList<PlantModel> list = new ArrayList<>();
    String type;
    TextView plant_list_txt;
    ImageView plant_list_img;
    int image;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_list);

        recycler = findViewById(R.id.recyclerPlantList);
        plant_list_txt = findViewById(R.id.plant_list_txt);
        plant_list_img = findViewById(R.id.plant_list_img);

        type = getIntent().getStringExtra("type");
        image = getIntent().getIntExtra("image", 0);
        plant_list_txt.setText(type);
        plant_list_img.setImageResource(image);
        loadPlants(type);

        recycler.setLayoutManager(new LinearLayoutManager(this));
        PlantAdapter adapter = new PlantAdapter(this, list, true);
        adapter.notifyDataSetChanged();
        recycler.setAdapter(adapter);
    }

    private void loadPlants(String type) {

        String json;

        try {
            InputStream is = getAssets().open("plants_types.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            json = new String(buffer, "UTF-8");

            JSONArray array = new JSONObject(json).getJSONArray("plants");

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                if (obj.getString("type").contains(type)) {
                    list.add(new PlantModel(
                            obj.getString("name"),
                            obj.getString("description"),
                            obj.getString("image"),
                            obj.getString("uses"),
                            obj.getString("benefits"),
                            obj.getString("examples")
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}