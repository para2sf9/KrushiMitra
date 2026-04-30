package com.example.ai_agri;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ai_agri.adapters.FarmingTypesAdapter;
import com.example.ai_agri.models.FarmingType;

import java.util.ArrayList;
import java.util.List;

public class FarmingTypes extends AppCompatActivity {
    RecyclerView recyclerView;
    FarmingTypesAdapter adapter;
    List<FarmingType> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farming_types);
        recyclerView = findViewById(R.id.farmingTypesRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        list = new ArrayList<>();

        list.add(new FarmingType(
                "Organic Farming",
                "Natural farming without chemicals",
                R.drawable.organic_img,
                "Kerala, Maharashtra",
                "Medium",
                "High",
                "Use compost\nAvoid pesticides"
        ));

        list.add(new FarmingType(
                "Hydroponics",
                "Soil-less farming using water",
                R.drawable.hydro,
                "Urban Areas",
                "Low",
                "High",
                "Maintain pH level\nUse clean water"
        ));

        list.add(new FarmingType(
                "Terrace Farming",
                "Farming on rooftops",
                R.drawable.terrace,
                "Cities",
                "Low",
                "Medium",
                "Use lightweight soil\nProper drainage"
        ));

        list.add(new FarmingType(
                "Precision Farming",
                "Technology-based farming",
                R.drawable.precision,
                "Developed Regions",
                "Optimized",
                "Very High",
                "Use sensors\nMonitor data"
        ));

        list.add(new FarmingType(
                "Subsistence Farming",
                "Small-scale farming for family needs",
                R.drawable.subsistence_farming,
                "Rural Areas",
                "Low",
                "Low",
                "Traditional tools\nSelf consumption"
        ));

        list.add(new FarmingType(
                "Commercial Farming",
                "Large-scale farming for profit",
                R.drawable.commercial_farming,
                "All India",
                "High",
                "Very High",
                "Use machinery\nMarket research"
        ));

        list.add(new FarmingType(
                "Intensive Farming",
                "High yield using more inputs",
                R.drawable.intensive_farming,
                "Punjab, Haryana",
                "High",
                "Very High",
                "Fertilizer control\nIrrigation needed"
        ));

        list.add(new FarmingType(
                "Extensive Farming",
                "Large land, low inputs",
                R.drawable.extensive_farming,
                "Low population areas",
                "Low",
                "Medium",
                "Large land use\nLess labor"
        ));

        list.add(new FarmingType(
                "Plantation Farming",
                "Single crop on large scale",
                R.drawable.plantation_farming,
                "Assam, Kerala",
                "High",
                "High",
                "Tea/Coffee crops\nExport oriented"
        ));

        list.add(new FarmingType(
                "Mixed Farming",
                "Crops + livestock together",
                R.drawable.mixed_farming,
                "All India",
                "Medium",
                "High",
                "Diversify income\nUse animal waste"
        ));

        list.add(new FarmingType(
                "Rainfed Farming",
                "Depends on rainfall",
                R.drawable.rain_farm,
                "Rajasthan, Maharashtra",
                "Low",
                "Medium",
                "Grow drought crops\nWater conservation"
        ));

        list.add(new FarmingType(
                "Vertical Farming",
                "Indoor stacked farming",
                R.drawable.vertical_farm,
                "Urban Cities",
                "Very Low",
                "High",
                "Use LED lights\nControl environment"
        ));
        adapter = new FarmingTypesAdapter(this, list);
        recyclerView.setAdapter(adapter);
    }
}