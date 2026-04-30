package com.example.ai_agri;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ImagePreviewActivity extends AppCompatActivity {

    private String imageUriString = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        ImageView previewImage = findViewById(R.id.preview_image);
        Button proceedButton  = findViewById(R.id.proceed_button);
        TextView tvTitle      = findViewById(R.id.tv_verify_title);
        TextView tvInstruction= findViewById(R.id.tv_verify_instruction);

        String category = "leaf";
        if (getIntent().hasExtra("category")) {
            category = getIntent().getStringExtra("category");
        }

        updateUIBasedOnCategory(category, tvTitle, tvInstruction);

        if (getIntent().hasExtra("imageUri")) {
            imageUriString = getIntent().getStringExtra("imageUri");
            previewImage.setImageURI(Uri.parse(imageUriString));
        }

        proceedButton.setOnClickListener(v -> {
            Intent intent;
            String selectedCategory = getIntent().getStringExtra("category");
            if ("soil".equalsIgnoreCase(selectedCategory)) {
                intent = new Intent(ImagePreviewActivity.this, SoilDetectionActivity.class);
            } else if ("insect".equalsIgnoreCase(selectedCategory)) {
                intent = new Intent(ImagePreviewActivity.this, InsectDetectionActivity.class);
            } else {
                intent = new Intent(ImagePreviewActivity.this, PredictionActivity.class);
            }

            if (imageUriString != null) {
                Uri uri = Uri.parse(imageUriString);
                intent.putExtra("imageUri", imageUriString);
                if (getIntent().hasExtra("category")) {
                    intent.putExtra("category", getIntent().getStringExtra("category"));
                }
                if (getIntent().hasExtra("source")) {
                    intent.putExtra("source", getIntent().getStringExtra("source"));
                }
                intent.setClipData(ClipData.newRawUri("", uri));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            }
        });
    }

    private void updateUIBasedOnCategory(String category, TextView tvTitle, TextView tvInstruction) {
        if (category == null) return;
        
        switch (category.toLowerCase()) {
            case "tools":
                tvTitle.setText("Verify Tool Image");
                tvInstruction.setText("Ensure the machinery is clearly visible for accurate detection.");
                break;
            case "soil":
                tvTitle.setText("Verify Soil Sample");
                tvInstruction.setText("Hold the soil sample in good light to detect moisture and type.");
                break;
            case "insect":
                tvTitle.setText("Verify Insect Image");
                tvInstruction.setText("Keep the insect in focus to identify pest category correctly.");
                break;
            case "leaf":
            default:
                tvTitle.setText("Verify Leaf Image");
                tvInstruction.setText("Ensure the leaf and its symptoms are in sharp focus.");
                break;
        }
    }
}
