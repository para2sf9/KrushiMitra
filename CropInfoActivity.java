package com.example.ai_agri;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

public class CropInfoActivity extends AppCompatActivity {

    ImageView cropImage;
    TextView cropName, cropDescription,
            cropSeason, cropSoil, cropWater,
            cropNutrients, cropSowing, cropHarvesting,
            cropClimate, cropGrowth, cropPest;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(ContextCompat.getColor(this, R.color.primaryGreen)),
                SystemBarStyle.dark(ContextCompat.getColor(this, R.color.primaryGreen)));
        setContentView(R.layout.activity_crop_info);

        // connect views
        cropImage = findViewById(R.id.cropImage);
        cropName = findViewById(R.id.cropName);
        cropDescription = findViewById(R.id.cropDescription);
        cropSeason = findViewById(R.id.cropSeason);
        cropSoil = findViewById(R.id.cropSoil);
        cropWater = findViewById(R.id.cropWater);


        cropNutrients = findViewById(R.id.cropNutrients);
        cropSowing = findViewById(R.id.cropSowing);
        cropHarvesting = findViewById(R.id.cropHarvesting);
        cropClimate = findViewById(R.id.cropClimate);
        cropGrowth = findViewById(R.id.cropGrowth);
        cropPest = findViewById(R.id.cropPest);

        // get data from intent
        String name = getIntent().getStringExtra("name");
        String image = getIntent().getStringExtra("image");
        String description = getIntent().getStringExtra("description");
        String season = getIntent().getStringExtra("season");
        String soil = getIntent().getStringExtra("soil");
        String water = getIntent().getStringExtra("water");


        cropName.setText(name);
        cropDescription.setText(description);
        cropSeason.setText(season);
        cropSoil.setText(soil);
        cropWater.setText(water);
        cropNutrients.setText(getIntent().getStringExtra("nutrients"));
        cropSowing.setText(getIntent().getStringExtra("sowing"));
        cropHarvesting.setText(getIntent().getStringExtra("harvesting"));
        cropClimate.setText(getIntent().getStringExtra("climate"));
        cropGrowth.setText(getIntent().getStringExtra("growth"));
        cropPest.setText(getIntent().getStringExtra("pest"));

        String imageUrl = getIntent().getStringExtra("image");
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageUrl = com.example.ai_agri.utils.CropImageUtil.getOnlineUrl(name);
        }

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.leaf)
                .error(R.drawable.leaf)
                .into(cropImage);
    }
}