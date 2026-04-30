package com.example.ai_agri;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.ai_agri.adapters.OnboardingAdapter;
import com.example.ai_agri.models.OnboardingModel;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    ViewPager2 viewPager;
    Button nextBtn;
    List<OnboardingModel> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding);

        viewPager = findViewById(R.id.viewPager);
        nextBtn = findViewById(R.id.nextBtn);

        list = new ArrayList<>();

        list.add(new OnboardingModel(
                R.drawable.smart_farm,
                "Smart Farming",
                "Get AI-based crop suggestions"
        ));

        list.add(new OnboardingModel(
                R.drawable.government_schemes2,
                "Government Schemes",
                "Explore latest farmer schemes easily"
        ));

        list.add(new OnboardingModel(
                R.drawable.methods2,
                "Organic Farming",
                "Learn natural and healthy farming methods"
        ));

        viewPager.setAdapter(new OnboardingAdapter(list));

        nextBtn.setOnClickListener(v -> {

            if (viewPager.getCurrentItem() < list.size() - 1) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                startActivity(new Intent(this, SignUpActivity.class));
                finish();
            }
        });

        SharedPreferences pref = getSharedPreferences("app", MODE_PRIVATE);
        boolean isFirst = pref.getBoolean("first", true);

        if (!isFirst) {
            startActivity(new Intent(this, SignUpActivity.class));
            finish();
        }

        pref.edit().putBoolean("first", false).apply();
    }
}