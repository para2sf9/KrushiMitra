package com.example.ai_agri;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ai_agri.adapters.PlantTypesAdapter;
import com.example.ai_agri.models.PlantTypeModel;

import java.util.ArrayList;

public class PlantTypesActivity extends AppCompatActivity {

    RecyclerView recycler;
    ArrayList<PlantTypeModel> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_types);

        recycler = findViewById(R.id.recyclerPlantTypes);


        list.add(new PlantTypeModel(
                "Trees",
                "Large plants with trunk",
                R.drawable.tree,
                "", "", ""
        ));

        list.add(new PlantTypeModel(
                "Herbs",
                "Small medicinal plants",
                R.drawable.herb,
                "", "", ""
        ));

        list.add(new PlantTypeModel(
                "Medicinal",
                "Plants used in medicine",
                R.drawable.medicinal,
                "", "", ""
        ));

        list.add(new PlantTypeModel(
                "Shrubs",
                "Bushy plants",
                R.drawable.bush,
                "", "", ""
        ));

        list.add(new PlantTypeModel(
                "Climbers",
                "Plants that climb",
                R.drawable.vine,
                "", "", ""
        ));
        list.add(new PlantTypeModel(
                "Creepers",
                "Ground creeper",
                R.drawable.creepers,
                "", "", ""
        ));
        list.add(new PlantTypeModel(
                "Aquatic",
                "Water plant",
                R.drawable.water_ily,
                "", "", ""
        ));


        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(new PlantTypesAdapter(this, list, false));
    }
}