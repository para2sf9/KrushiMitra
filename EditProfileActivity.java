package com.example.ai_agri;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.ai_agri.utils.DatabaseManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText editName, editPhone, editCity, editDistrict, editCrops, editTotalArea, editFarmSize, editSoil, editIrrigation;
    private ImageView profileImage;
    private Button saveBtn;
    private String base64Image = "";
    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        profileImage.setImageBitmap(bitmap);
                        base64Image = encodeImage(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initViews();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        profileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        fetchLocation();

        saveBtn.setOnClickListener(v -> saveProfile());
    }

    private void initViews() {
        editName = findViewById(R.id.edit_name);
        editPhone = findViewById(R.id.edit_phone);
        editCity = findViewById(R.id.edit_city);
        editDistrict = findViewById(R.id.edit_district);
        editCrops = findViewById(R.id.edit_crops);
        editTotalArea = findViewById(R.id.edit_total_area);
        editFarmSize = findViewById(R.id.edit_farm_size);
        editSoil = findViewById(R.id.edit_soil);
        editIrrigation = findViewById(R.id.edit_irrigation);
        profileImage = findViewById(R.id.edit_profile_image);
        saveBtn = findViewById(R.id.save_profile_btn);

        editName.setText(DatabaseManager.getUserName(this));
        editPhone.setText(DatabaseManager.getUserPhone(this));

        loadExistingProfile();
    }

    private void loadExistingProfile() {
        DatabaseManager.getFullProfile(this, new DatabaseManager.AuthCallback() {
            @Override
            public void onSuccess(String responseData) {
                try {
                    com.google.gson.JsonObject response = new com.google.gson.Gson().fromJson(responseData, com.google.gson.JsonObject.class);
                    if (response.has("rows")) {
                        com.google.gson.JsonArray rows = response.getAsJsonArray("rows");
                        if (rows.size() > 0) {
                            com.google.gson.JsonObject profile = rows.get(0).getAsJsonObject();
                            runOnUiThread(() -> {
                                if (profile.has("city") && !profile.get("city").isJsonNull()) editCity.setText(profile.get("city").getAsString());
                                if (profile.has("district") && !profile.get("district").isJsonNull()) editDistrict.setText(profile.get("district").getAsString());
                                if (profile.has("crops") && !profile.get("crops").isJsonNull()) editCrops.setText(profile.get("crops").getAsString());
                                if (profile.has("total area") && !profile.get("total area").isJsonNull()) editTotalArea.setText(profile.get("total area").getAsString());
                                if (profile.has("farm_size") && !profile.get("farm_size").isJsonNull()) editFarmSize.setText(profile.get("farm_size").getAsString());
                                if (profile.has("soil") && !profile.get("soil").isJsonNull()) editSoil.setText(profile.get("soil").getAsString());
                                if (profile.has("irigation") && !profile.get("irigation").isJsonNull()) editIrrigation.setText(profile.get("irigation").getAsString());
                                
                                if (profile.has("profile_photo") && !profile.get("profile_photo").isJsonNull()) {
                                    String base64 = profile.get("profile_photo").getAsString();
                                    if (!base64.isEmpty()) {
                                        base64Image = base64;
                                        byte[] decodedString = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                                        android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                        profileImage.setImageBitmap(decodedByte);
                                    }
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        editCity.setText(addresses.get(0).getLocality());
                        editDistrict.setText(addresses.get(0).getSubAdminArea());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void saveProfile() {
        String name = editName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String city = editCity.getText().toString().trim();
        String district = editDistrict.getText().toString().trim();
        String crops = editCrops.getText().toString().trim();
        String totalArea = editTotalArea.getText().toString().trim();
        String farmSize = editFarmSize.getText().toString().trim();
        String soil = editSoil.getText().toString().trim();
        String irrigation = editIrrigation.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || city.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String farmerId = generateFarmerId(name);

        saveBtn.setEnabled(false);
        saveBtn.setText("Saving...");

        DatabaseManager.saveFullProfile(this, farmerId, phone, city, crops, totalArea, soil, irrigation, district, farmSize, base64Image, new DatabaseManager.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                // Also update LoginReg if name changed
                DatabaseManager.updateProfile(EditProfileActivity.this, name, phone, new DatabaseManager.AuthCallback() {
                    @Override
                    public void onSuccess(String msg) {
                        runOnUiThread(() -> {
                            Toast.makeText(EditProfileActivity.this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
                    }

                    @Override
                    public void onError(String err) {
                        runOnUiThread(() -> Toast.makeText(EditProfileActivity.this, "LoginReg Update Failed: " + err, Toast.LENGTH_SHORT).show());
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    saveBtn.setEnabled(true);
                    saveBtn.setText("Save Full Profile");
                    Toast.makeText(EditProfileActivity.this, "Failed to save: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private String generateFarmerId(String name) {
        String loginRegId = DatabaseManager.getUserId(this);
        String namePart = name.length() >= 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase();
        int randomNum = new Random().nextInt(900) + 100; // 100-999
        return loginRegId + namePart + randomNum;
    }

    private String encodeImage(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }
}
