package com.example.ai_agri;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.io.File;

public class PredictionActivity extends AppCompatActivity {

    private ImageView    imgLeafPreview;
    private ProgressBar  progressAnalyse;
    private View         scrollResults;
    private TextView     tvPlantName, tvDiseaseName,
            tvHealthStatus, tvDescription, tvWarning;
    private TextView     labelDescription, labelPrevention, labelCure, labelReference;
    private LinearLayout layoutPrevention, layoutCure;
    private ImageView    imgHealthyReference;
    private Button       btnScanAgain;
    
    private String selectedCategory = "leaf";

    private final PlantDoctorApi api = new PlantDoctorApi();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prediction);

        if (getIntent().hasExtra("category")) {
            selectedCategory = getIntent().getStringExtra("category");
        }

        bindViews();
        setupListeners();
        receiveImageAndAnalyse();
    }

    private void bindViews() {
        imgLeafPreview  = findViewById(R.id.imgLeafPreview);
        progressAnalyse = findViewById(R.id.progressAnalyse);
        scrollResults   = findViewById(R.id.scrollResults);
        tvPlantName     = findViewById(R.id.tvPlantName);
        tvDiseaseName   = findViewById(R.id.tvDiseaseName);
        tvHealthStatus  = findViewById(R.id.tvHealthStatus);
        tvDescription   = findViewById(R.id.tvDescription);
        tvWarning       = findViewById(R.id.tvWarning);
        layoutPrevention= findViewById(R.id.layoutPrevention);
        layoutCure      = findViewById(R.id.layoutCure);
        
        labelDescription = findViewById(R.id.labelDescription);
        labelPrevention  = findViewById(R.id.labelPrevention);
        labelCure        = findViewById(R.id.labelCure);
        labelReference   = findViewById(R.id.labelReference);
        imgHealthyReference = findViewById(R.id.imgHealthyReference);
        btnScanAgain = findViewById(R.id.btnScanAgain);
    }

    private void setupListeners() {
        btnScanAgain.setOnClickListener(v -> {
            String source = getIntent().getStringExtra("source");
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("trigger_source", source);
            intent.putExtra("trigger_category", selectedCategory);
            startActivity(intent);
            finish();
        });
    }

    private void receiveImageAndAnalyse() {
        File imageFile = null;

        if (getIntent().hasExtra("imageUri")) {
            String uriString = getIntent().getStringExtra("imageUri");
            Uri uri = Uri.parse(uriString);
            imgLeafPreview.setImageURI(uri);
            imageFile = FileUtils.uriToFile(this, uri);
        }

        if (imageFile == null) {
            Toast.makeText(this, "Could not load image. Please try again.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        sendToApi(imageFile);
    }

    private void sendToApi(File imageFile) {
        progressAnalyse.setVisibility(View.VISIBLE);
        scrollResults.setVisibility(View.GONE);

        api.predict(imageFile, selectedCategory, new PlantDoctorApi.Callback() {

            @Override
            public void onSuccess(PredictionResult result) {
                runOnUiThread(() -> {
                    progressAnalyse.setVisibility(View.GONE);
                    if (result != null) {
                        displayResult(result);
                    } else {
                        onError("Empty response from server");
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressAnalyse.setVisibility(View.GONE);
                    Toast.makeText(PredictionActivity.this,
                            "Analysis failed: " + errorMessage,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void displayResult(PredictionResult result) {
        scrollResults.setVisibility(View.VISIBLE);

        // Confidence Threshold - if it's too low, we probably didn't find what we were looking for
        boolean lowConfidence = result.confidencePct < 35.0f;

        // Check if the result is actually a plant/leaf
        boolean isNotPlant = result.plant == null || 
                           result.plant.isEmpty() ||
                           "Background".equalsIgnoreCase(result.plant) || 
                           "Non-leaf".equalsIgnoreCase(result.plant) ||
                           "Ambient".equalsIgnoreCase(result.plant) ||
                           "Unknown".equalsIgnoreCase(result.plant);

        boolean isInvalid = isNotPlant || lowConfidence;

        if (isInvalid) {
            tvPlantName.setVisibility(View.GONE);
            if ("tools".equalsIgnoreCase(selectedCategory)) {
                tvDiseaseName.setText("Machine/Tool Not Detected");
            } else if ("soil".equalsIgnoreCase(selectedCategory)) {
                tvDiseaseName.setText("Soil Not Detected");
            } else if ("insect".equalsIgnoreCase(selectedCategory)) {
                tvDiseaseName.setText("Insect Not Detected");
            } else {
                tvDiseaseName.setText("Analysis Inconclusive");
            }
            tvHealthStatus.setText(R.string.unknown_status);
            tvHealthStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
            
            labelDescription.setVisibility(View.GONE);
            tvDescription.setVisibility(View.GONE);
            labelPrevention.setVisibility(View.GONE);
            layoutPrevention.setVisibility(View.GONE);
            labelCure.setVisibility(View.GONE);
            layoutCure.setVisibility(View.GONE);
            
            tvWarning.setVisibility(View.VISIBLE);
            if (lowConfidence && !isNotPlant) {
                String categoryDisplay = selectedCategory;
                if ("tools".equalsIgnoreCase(selectedCategory)) categoryDisplay = "machine/tool";
                tvWarning.setText("Low confidence detected (" + String.format("%.1f%%", result.confidencePct) + "). Please ensure you are scanning a " + categoryDisplay + " clearly.");
            } else {
                if ("tools".equalsIgnoreCase(selectedCategory)) {
                    tvWarning.setText("Could not detect any agricultural machinery or tool. Please scan a tractor, plow, or other farm equipment.");
                } else {
                    tvWarning.setText(R.string.leaf_not_detected_warning);
                }
            }
        } else {
            // Update UI based on Category
            updateUILabels();

            // Check if user actually wanted a leaf scan
            if (!"leaf".equalsIgnoreCase(selectedCategory)) {
                tvWarning.setVisibility(View.VISIBLE);
                tvWarning.setText("Note: AI detected a " + result.plant + " while you selected " + selectedCategory + " mode.");
            } else {
                tvWarning.setVisibility(View.GONE);
            }

            tvPlantName.setVisibility(View.VISIBLE);
            tvPlantName.setText(result.plant);
            tvDiseaseName.setText(result.diseaseName != null ? result.diseaseName : "Healthy");
            
            String statusText = result.isHealthy ? "Healthy" : "Infected";
            tvHealthStatus.setText(statusText);
            
            int statusColor = ContextCompat.getColor(this, 
                    result.isHealthy ? android.R.color.holo_green_dark : android.R.color.holo_red_dark);
            tvHealthStatus.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            tvHealthStatus.getBackground().setColorFilter(statusColor, android.graphics.PorterDuff.Mode.SRC_IN);
            tvDiseaseName.setTextColor(statusColor);

            // Description logic
            labelDescription.setVisibility(View.VISIBLE);
            tvDescription.setVisibility(View.VISIBLE);
            if (result.description != null && !result.description.isEmpty()) {
                tvDescription.setText(result.description);
            } else {
                tvDescription.setText("No specific details available for this detection.");
            }

            // Prevention logic
            layoutPrevention.removeAllViews();
            if (result.prevention != null && !result.prevention.isEmpty()) {
                labelPrevention.setVisibility(View.VISIBLE);
                layoutPrevention.setVisibility(View.VISIBLE);
                for (String tip : result.prevention) {
                    layoutPrevention.addView(makeTextItem("• " + tip));
                }
            } else {
                labelPrevention.setVisibility(View.GONE);
                layoutPrevention.setVisibility(View.GONE);
            }

            // Cure logic
            layoutCure.removeAllViews();
            if (result.cure != null && !result.cure.isEmpty()) {
                labelCure.setVisibility(View.VISIBLE);
                layoutCure.setVisibility(View.VISIBLE);
                for (String step : result.cure) {
                    layoutCure.addView(makeTextItem("• " + step));
                }
            } else {
                labelCure.setVisibility(View.GONE);
                layoutCure.setVisibility(View.GONE);
            }
        }

        // Healthy Reference Image Logic
        if (!isInvalid && result.plant != null) {
            String referenceUrl = com.example.ai_agri.utils.CropImageUtil.getOnlineUrl(result.plant);
            if (referenceUrl != null) {
                labelReference.setText("Healthy Reference");
                labelReference.setVisibility(View.VISIBLE);
                imgHealthyReference.setVisibility(View.VISIBLE);
                com.bumptech.glide.Glide.with(this)
                        .load(referenceUrl)
                        .into(imgHealthyReference);
            }
        } else {
            labelReference.setVisibility(View.GONE);
            imgHealthyReference.setVisibility(View.GONE);
        }
    }

    private void updateUILabels() {
        switch (selectedCategory.toLowerCase()) {
            case "tools":
                labelDescription.setText("Specifications & Purpose");
                labelPrevention.setText("Maintenance Tips");
                labelCure.setText("Recommended Actions");
                break;
            case "soil":
                labelDescription.setText("Soil Composition");
                labelPrevention.setText("Fertilizer Advice");
                labelCure.setText("Corrective Measures");
                break;
            case "insect":
                labelDescription.setText("Insect Behavior");
                labelPrevention.setText("Pest Control");
                labelCure.setText("Eradication Steps");
                break;
            case "leaf":
            default:
                labelDescription.setText("Plant Details");
                labelPrevention.setText(R.string.prevention);
                labelCure.setText(R.string.cure);
                break;
        }
    }

    private TextView makeTextItem(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(14f);
        tv.setPadding(0, 6, 0, 6);
        tv.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        return tv;
    }
}
