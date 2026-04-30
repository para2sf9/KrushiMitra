package com.example.ai_agri;

import android.Manifest;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.ai_agri.fragments.CategoryFragment;
import com.example.ai_agri.fragments.CustomerCareFragment;
import com.example.ai_agri.fragments.HomeFragment;
import com.example.ai_agri.fragments.ResourcesFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(ContextCompat.getColor(this, R.color.primaryGreen)),
                SystemBarStyle.dark(ContextCompat.getColor(this, R.color.primaryGreen)));
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null && savedInstanceState.containsKey("photoUri")) {
            photoUri = Uri.parse(savedInstanceState.getString("photoUri"));
        }

        setupLaunchers();

        FloatingActionButton scanButton = findViewById(R.id.scan_button);

        scanButton.setOnClickListener(v -> {
            BottomSheetDialog dialog = new BottomSheetDialog(MainActivity.this);
            View view = getLayoutInflater().inflate(R.layout.scan_options_bottom_sheet, null);

            dialog.setContentView(view);
            dialog.show();

            CardView camera = view.findViewById(R.id.camera_option);
            CardView upload = view.findViewById(R.id.upload_option);

            camera.setOnClickListener(v1 -> {
                dialog.dismiss();
                showCategoryBottomSheet(true);
            });

            upload.setOnClickListener(v2 -> {
                dialog.dismiss();
                showCategoryBottomSheet(false);
            });
        });
        
        handleRetakeTrigger(getIntent());
        
        loadFragments();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleRetakeTrigger(intent);
    }

    private void handleRetakeTrigger(Intent intent) {
        if (intent != null && intent.hasExtra("trigger_source")) {
            String source = intent.getStringExtra("trigger_source");
            String category = intent.getStringExtra("trigger_category");
            if (category != null) this.selectedCategory = category;
            
            if ("camera".equals(source)) {
                checkPermissionAndLaunchCamera(this.selectedCategory);
            } else if ("gallery".equals(source)) {
                galleryLauncher.launch("image/*");
            }
        }
    }

    private void showCategoryBottomSheet(boolean isCamera) {
        BottomSheetDialog categoryDialog = new BottomSheetDialog(MainActivity.this);
        View categoryView = getLayoutInflater().inflate(R.layout.scan_category_bottom_sheet, null);
        categoryDialog.setContentView(categoryView);
        categoryDialog.show();

        View.OnClickListener listener = v -> {
            categoryDialog.dismiss();
            String category = "leaf";
            int id = v.getId();
            if (id == R.id.category_leaf) category = "leaf";
            else if (id == R.id.category_tools) category = "tools";
            else if (id == R.id.category_soil) category = "soil";
            else if (id == R.id.category_insect) category = "insect";

            final String finalCategory = category;
            if (isCamera) {
                checkPermissionAndLaunchCamera(finalCategory);
            } else {
                galleryLauncher.launch("image/*");
                // Note: Gallery launcher doesn't easily pass data back in this flow 
                // without a member variable or custom contract.
                // We'll use a member variable for simplicity in this task.
                this.selectedCategory = finalCategory;
            }
        };

        categoryView.findViewById(R.id.category_leaf).setOnClickListener(listener);
        categoryView.findViewById(R.id.category_tools).setOnClickListener(listener);
        categoryView.findViewById(R.id.category_soil).setOnClickListener(listener);
        categoryView.findViewById(R.id.category_insect).setOnClickListener(listener);
    }

    private String selectedCategory = "leaf";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (photoUri != null) {
            outState.putString("photoUri", photoUri.toString());
        }
        outState.putString("selectedCategory", selectedCategory);
    }

    private void setupLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (photoUri != null) {
                            Intent intent = new Intent(MainActivity.this, ImagePreviewActivity.class);
                            intent.putExtra("imageUri", photoUri.toString());
                            intent.putExtra("category", selectedCategory);
                            intent.putExtra("source", "camera");
                            intent.setClipData(ClipData.newRawUri("", photoUri));
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        }
                    }
                }
        );

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        Intent intent = new Intent(MainActivity.this, ImagePreviewActivity.class);
                        intent.putExtra("imageUri", uri.toString());
                        intent.putExtra("category", selectedCategory);
                        intent.putExtra("source", "gallery");
                        intent.setClipData(ClipData.newRawUri("", uri));
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    }
                }
        );

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        launchCamera();
                    } else {
                        Toast.makeText(this, "Camera permission is required to use this feature", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void checkPermissionAndLaunchCamera(String category) {
        this.selectedCategory = category;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            File photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(this, "com.example.ai_agri.fileprovider", photoFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            cameraLauncher.launch(cameraIntent);
        } catch (IOException e) {
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir("Pictures");
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void loadFragments(){
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, new HomeFragment())
                .commit();

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            if(item.getItemId() == R.id.nav_home){
                fragment = new HomeFragment();
            }
            else if(item.getItemId() == R.id.nav_category){
                fragment = new CategoryFragment();
            }
            else if(item.getItemId() == R.id.nav_resources){
                fragment = new ResourcesFragment();
            }
            else if(item.getItemId() == R.id.nav_customer_care){
                fragment = new CustomerCareFragment();
            }

            if (fragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frameLayout, fragment)
                        .commit();
            }

            return true;
        });
    }
}
