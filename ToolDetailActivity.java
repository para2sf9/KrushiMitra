package com.example.ai_agri;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

public class ToolDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_detail);

        ImageView image = findViewById(R.id.detailImage);
        TextView name = findViewById(R.id.detailName);
        TextView usage = findViewById(R.id.detailUsage);
        TextView category = findViewById(R.id.detailCategory);
        TextView rec = findViewById(R.id.recommendation);

        Intent i = getIntent();

        String toolName = i.getStringExtra("name");
        String toolCategory = i.getStringExtra("category");

        name.setText(toolName);
        usage.setText(i.getStringExtra("usage"));
        category.setText(toolCategory);

        Glide.with(this).load(i.getStringExtra("image")).into(image);

        // 💡 Recommendation Logic
        rec.setText(getString(R.string.maintenance_tip_format, toolName));
    }
}