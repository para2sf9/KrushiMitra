package com.example.ai_agri;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SoilDetectionActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private ImageView imgSoilPreview;
    private ProgressBar progressSoil;
    private CardView cardResult;
    private TextView tvSoilType, tvSoilColor, tvSoilDescription, tvFarmingStatus, tvWhereUsed;
    private Button btnCapture, btnReScan;
    private SoilDetectionApi soilApi;
    private String currentPhotoPath;
    private Uri photoURI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_soil_detection);

        imgSoilPreview = findViewById(R.id.imgSoilPreview);
        progressSoil = findViewById(R.id.progressSoil);
        cardResult = findViewById(R.id.cardResult);
        tvSoilType = findViewById(R.id.tvSoilType);
        tvSoilColor = findViewById(R.id.tvSoilColor);
        tvSoilDescription = findViewById(R.id.tvSoilDescription);
        tvFarmingStatus = findViewById(R.id.tvFarmingStatus);
        tvWhereUsed = findViewById(R.id.tvWhereUsed);
        btnCapture = findViewById(R.id.btnCapture);
        btnReScan = findViewById(R.id.btnReScan);

        soilApi = new SoilDetectionApi();

        if (getIntent().hasExtra("imageUri")) {
            Uri imageUri = Uri.parse(getIntent().getStringExtra("imageUri"));
            imgSoilPreview.setImageURI(imageUri);
            btnCapture.setVisibility(View.GONE);
            
            File file = FileUtils.uriToFile(this, imageUri);
            if (file != null) {
                analyzeSoilFromFile(file);
            }
        }

        btnCapture.setOnClickListener(v -> dispatchTakePictureIntent());
        btnReScan.setOnClickListener(v -> dispatchTakePictureIntent());
        
        // Also allow picking from gallery for better quality images
        imgSoilPreview.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "com.example.ai_agri.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                File file = new File(currentPhotoPath);
                if (file.exists()) {
                    imgSoilPreview.setImageURI(photoURI);
                    btnCapture.setVisibility(View.GONE);
                    analyzeSoilFromFile(file);
                }
            } else if (requestCode == REQUEST_PICK_IMAGE && data != null) {
                Uri selectedImage = data.getData();
                imgSoilPreview.setImageURI(selectedImage);
                btnCapture.setVisibility(View.GONE);
                File file = FileUtils.uriToFile(this, selectedImage);
                if (file != null) {
                    analyzeSoilFromFile(file);
                }
            }
        }
    }

    private void analyzeSoilFromFile(File file) {
        progressSoil.setVisibility(View.VISIBLE);
        cardResult.setVisibility(View.GONE);

        soilApi.analyzeSoil(file, new SoilDetectionApi.SoilCallback() {
            @Override
            public void onSuccess(SoilResult result) {
                runOnUiThread(() -> {
                    progressSoil.setVisibility(View.GONE);
                    displayResult(result);
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    progressSoil.setVisibility(View.GONE);
                    Toast.makeText(SoilDetectionActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    btnCapture.setVisibility(View.VISIBLE);
                    
                    // Display the captured image even on failure as requested
                    cardResult.setVisibility(View.GONE);
                    imgSoilPreview.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void displayResult(SoilResult result) {
        cardResult.setVisibility(View.VISIBLE);
        tvSoilType.setText(result.soilType);
        tvSoilColor.setText("Color: " + result.color);
        tvSoilDescription.setText(result.description);
        tvFarmingStatus.setText(result.isSuitableForFarming ? 
                "Highly suitable for intensive farming." : 
                "Limited suitability for farming without amendments.");
        tvWhereUsed.setText(result.whereUsed);
    }
}
