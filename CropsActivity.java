package com.example.ai_agri;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ai_agri.adapters.CropAdapter;
import com.example.ai_agri.models.CropModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;

public class CropsActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ArrayList<CropModel> list = new ArrayList<>();
    CropAdapter adapter;
    TextView season_name;
    ImageView season_img,back_btn;
    String SeasonName;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_list);

        recyclerView = findViewById(R.id.cropRecycler);
        recyclerView.setLayoutManager(new GridLayoutManager(this,2));

        season_img = findViewById(R.id.season_img);
        season_name = findViewById(R.id.season_name);
        back_btn = findViewById(R.id.back_btn);

        String cropType = getIntent().getStringExtra("cropType");
         SeasonName = getIntent().getStringExtra("season_name");

        getSeasonName_Img();
        loadCrops(cropType);

        adapter = new CropAdapter(this,list);
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);

        back_btn.setOnClickListener(v -> {
            onBackPressed();
        });
    }

    private void loadCrops(String cropType){
        try{
            InputStream is = getAssets().open("crops.json");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;

            while ((length = is.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }

            String json = baos.toString();
            is.close();

            JSONObject object = new JSONObject(json);
            JSONArray crops = object.getJSONArray("crops");
            for(int i=0;i<crops.length();i++){

                JSONObject crop = crops.getJSONObject(i);


                String season = crop.getString("season");
                String desc = crop.getString("description");

                if(season.equalsIgnoreCase(cropType)){
                    list.add(new CropModel(crop.getString("name"),
                            crop.getString("description"),
                            crop.getString("season"),
                            crop.getString("soil"),
                            crop.getString("water_dependency"),
                            crop.getString("image"),
                            crop.getString("nutrients"),
                            crop.getString("sowing"),
                            crop.getString("harvesting"),
                            crop.getString("climate"),
                            crop.getString("growth"),
                            crop.getString("pest_disease")));

                }
            }

        }catch(Exception e){
            //e.printStackTrace();
            System.out.println("error message"+e.getMessage());
        }
    }

    @SuppressLint("SetTextI18n")
    private void getSeasonName_Img() {
        season_name.setText(SeasonName);

        if (Objects.equals(SeasonName, "Kharif")) {
            season_img.setImageResource(R.drawable.corn);
        } else if (Objects.equals(SeasonName, "Rabi")) {
            season_img.setImageResource(R.drawable.wheat);
        } else {
            season_img.setImageResource(R.drawable.vegetable);
        }
    }
}