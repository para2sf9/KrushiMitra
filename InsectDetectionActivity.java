package com.example.ai_agri;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import java.io.File;

public class InsectDetectionActivity extends AppCompatActivity {

    private ImageView imgInsectPreview;
    private ProgressBar progressInsect;
    private CardView cardResult;
    private TextView tvInsectName, tvConfidence, tvScientificName, tvStatus, tvDescription, tvControlMeasures, tvWarning;
    private Button btnScanAgain;
    private InsectDetectionApi insectApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insect_detection);

        bindViews();
        insectApi = new InsectDetectionApi();

        if (getIntent().hasExtra("imageUri")) {
            Uri imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
            imgInsectPreview.setImageURI(imageUri);
            
            File file = FileUtils.uriToFile(this, imageUri);
            if (file != null) {
                analyzeInsect(file);
            } else {
                Toast.makeText(this, "Failed to load image file", Toast.LENGTH_SHORT).show();
            }
        }

        btnScanAgain.setOnClickListener(v -> {
            String source = getIntent().getStringExtra("source");
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("trigger_source", source);
            intent.putExtra("trigger_category", "insect");
            startActivity(intent);
            finish();
        });
    }

    private void bindViews() {
        imgInsectPreview = findViewById(R.id.imgInsectPreview);
        progressInsect = findViewById(R.id.progressInsect);
        cardResult = findViewById(R.id.cardResult);
        tvInsectName = findViewById(R.id.tvInsectName);
        tvConfidence = findViewById(R.id.tvConfidence);
        tvScientificName = findViewById(R.id.tvScientificName);
        tvStatus = findViewById(R.id.tvStatus);
        tvDescription = findViewById(R.id.tvDescription);
        tvControlMeasures = findViewById(R.id.tvControlMeasures);
        tvWarning = findViewById(R.id.tvWarning);
        btnScanAgain = findViewById(R.id.btnScanAgain);
    }

    private void analyzeInsect(File file) {
        progressInsect.setVisibility(View.VISIBLE);
        cardResult.setVisibility(View.GONE);
        tvWarning.setVisibility(View.GONE);

        insectApi.detectInsect(file, new InsectDetectionApi.InsectCallback() {
            @Override
            public void onSuccess(InsectResult result) {
                runOnUiThread(() -> {
                    progressInsect.setVisibility(View.GONE);
                    displayResult(result);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressInsect.setVisibility(View.GONE);
                    tvWarning.setVisibility(View.VISIBLE);
                    tvWarning.setText(errorMessage);
                    cardResult.setVisibility(View.GONE);
                });
            }
        });
    }

    private void displayResult(InsectResult result) {
        cardResult.setVisibility(View.VISIBLE);
        tvInsectName.setText(result.insectName);
        tvConfidence.setText(String.format("%.1f%% confidence", result.confidence * 100));
        tvScientificName.setText(result.scientificName);
        tvStatus.setText(result.status);
        tvDescription.setText(result.description);
        tvControlMeasures.setText(result.controlMeasures);

        if (result.confidence < 0.45) {
            tvWarning.setVisibility(View.VISIBLE);
            tvWarning.setText("Low confidence (" + String.format("%.1f%%", result.confidence * 100) + ").\n\nTips for better results:\n• Ensure the insect is the main focus\n• Use good lighting (daylight is best)\n• Avoid busy or blurry backgrounds");
        } else {
            tvWarning.setVisibility(View.GONE);
        }
    }
}
