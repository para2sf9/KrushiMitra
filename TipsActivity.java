package com.example.ai_agri;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ai_agri.adapters.TipAdapter;
import com.example.ai_agri.models.TipModel;

import java.util.ArrayList;
import java.util.List;

public class TipsActivity extends AppCompatActivity {
    ArrayList<TipModel> list = new ArrayList<>();
    TipAdapter tipAdapter;
    RecyclerView recyclerViewTips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tips);
        recyclerViewTips = findViewById(R.id.recyclerTips);
        loadTipData();

    }


    @SuppressLint("NotifyDataSetChanged")
    private void loadTipData() {
        list.add(new TipModel(
                "Understand Your Soil",
                "Test soil to know nutrients and pH levels for better crop planning.",
                "Improves productivity & soil health",
                "Use Soil Health Card for insights",
                R.drawable.soil
        ));

        list.add(new TipModel(
                "Choose the Right Crops",
                "Select crops based on climate, soil type, and water availability.",
                "Higher yield & profit",
                "Check market demand before planting",
                R.drawable.wheat
        ));

        list.add(new TipModel(
                "Modern Farming Techniques",
                "Use drip irrigation, precision farming, drones & IoT tools.",
                "Saves cost & increases efficiency",
                "Start small and scale gradually",
                R.drawable.ic_tech
        ));

        list.add(new TipModel(
                "Sustainable Farming",
                "Use crop rotation, organic farming, and reduce chemicals.",
                "Maintains soil fertility & eco-friendly",
                "Use government organic schemes",
                R.drawable.leaf
        ));

        list.add(new TipModel(
                "Water Management",
                "Adopt drip irrigation & rainwater harvesting.",
                "Prevents water wastage",
                "Maintain irrigation systems regularly",
                R.drawable.water
        ));

        list.add(new TipModel(
                "Quality Equipment",
                "Use reliable machinery and maintain it regularly.",
                "Reduces labor cost & increases productivity",
                "Check tyres & service regularly",
                R.drawable.ic_machine
        ));

        list.add(new TipModel(
                "Government Schemes",
                "Use subsidies, insurance, and farming schemes.",
                "Reduces cost & risk",
                "Stay updated via agri offices",
                R.drawable.goverment_schemes
        ));

        list.add(new TipModel(
                "Market Linkages",
                "Sell via eNAM, direct markets, or contracts.",
                "Better pricing & profits",
                "Track market trends regularly",
                R.drawable.ic_market
        ));

        list.add(new TipModel(
                "Post-Harvest Management",
                "Use proper storage, grading & packaging.",
                "Reduces losses & increases value",
                "Use cold storage & hermetic bags",
                R.drawable.storage
        ));

        list.add(new TipModel(
                "Stay Educated",
                "Learn new farming methods & technologies.",
                "Keeps you competitive",
                "Follow agri updates & workshops",
                R.drawable.ic_education
        ));

        list.add(new TipModel(
                "Crop Insurance",
                "Protect crops using insurance schemes.",
                "Reduces financial risk",
                "Use PMFBY scheme",
                R.drawable.insurance
        ));

        list.add(new TipModel(
                "Community Farming",
                "Collaborate with farmers & share knowledge.",
                "Better learning & cost saving",
                "Join cooperatives & farmer groups",
                R.drawable.community
        ));
        recyclerViewTips.setLayoutManager(new LinearLayoutManager(this));
        tipAdapter = new TipAdapter(this, list);
        tipAdapter.notifyDataSetChanged();
        recyclerViewTips.setAdapter(tipAdapter);
    }
}