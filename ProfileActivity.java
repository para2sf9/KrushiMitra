package com.example.ai_agri;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.ai_agri.utils.DatabaseManager;
import com.example.ai_agri.utils.LocaleHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ProfileActivity extends AppCompatActivity {
    View language, logout_btn;
    TextView user_name, user_email, farmer_id_val, crops_stat, land_stat, soil_stat, profile_phone, user_location, profile_district;
    ImageView profile_image;
    View profile_header, stats_section, details_card, edit_profile_btn, edit_profile_card;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this,
                SystemBarStyle.dark(ContextCompat.getColor(this, R.color.primaryGreen)),
                SystemBarStyle.dark(ContextCompat.getColor(this, R.color.primaryGreen)));
        setContentView(R.layout.activity_profile);

        initViews();

        View.OnClickListener editProfileListener = v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivityForResult(intent, 100);
        };

        edit_profile_btn.setOnClickListener(editProfileListener);
        edit_profile_card.setOnClickListener(editProfileListener);

        updateUI();

        if (!DatabaseManager.isLoggedIn(this)) {
            startActivity(new Intent(this, SignUpActivity.class));
            finish();
            return;
        }

        language.setOnClickListener(v -> {
            LanguageBottomSheet bottomSheet = new LanguageBottomSheet(ProfileActivity.this, langCode -> {
                LocaleHelper.setLocale(ProfileActivity.this, langCode);
                recreate();
            });
            bottomSheet.show();
        });

        logout_btn.setOnClickListener(v -> {
            DatabaseManager.logout(this);
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
            
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        });
    }

    private void initViews() {
        language = findViewById(R.id.language);
        logout_btn = findViewById(R.id.logout);
        user_name = findViewById(R.id.user_name);
        user_email = findViewById(R.id.user_email);
        profile_image = findViewById(R.id.profile_image);
        
        farmer_id_val = findViewById(R.id.farmer_id_val);
        crops_stat = findViewById(R.id.crops_stat);
        land_stat = findViewById(R.id.land_stat);
        soil_stat = findViewById(R.id.soil_stat);
        profile_phone = findViewById(R.id.profile_phone);
        user_location = findViewById(R.id.user_location);
        profile_district = findViewById(R.id.profile_district);
        
        profile_header = findViewById(R.id.profile_header);
        stats_section = findViewById(R.id.stats_section);
        details_card = findViewById(R.id.details_card);
        edit_profile_btn = findViewById(R.id.edit_image);
        edit_profile_card = findViewById(R.id.edit_profile_card);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            updateUI();
        }
    }

    private void updateUI() {
        if (DatabaseManager.isLoggedIn(this)) {
            user_name.setText(DatabaseManager.getUserName(this));
            user_email.setText(DatabaseManager.getUserEmail(this));
            logout_btn.setVisibility(View.VISIBLE);

            // 1. Load from cache first for instant UI response
            loadCachedProfile();

            // 2. Fetch fresh data from network and update cache
            DatabaseManager.getFullProfile(this, new DatabaseManager.AuthCallback() {
                @Override
                public void onSuccess(String responseData) {
                    try {
                        JsonObject response = new com.google.gson.Gson().fromJson(responseData, JsonObject.class);
                        if (response.has("rows")) {
                            JsonArray rows = response.getAsJsonArray("rows");
                            if (rows.size() > 0) {
                                JsonObject profile = rows.get(0).getAsJsonObject();
                                
                                // Update Cache
                                DatabaseManager.cacheFullProfile(ProfileActivity.this, profile);
                                
                                runOnUiThread(() -> {
                                    displayProfileData(profile);
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
        } else {
            user_name.setText("Guest Farmer");
            user_email.setText("Please login to see details");
            logout_btn.setVisibility(View.GONE);
            user_name.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, SignUpActivity.class)));
        }
    }

    private void loadCachedProfile() {
        String cachedPhoto = DatabaseManager.getCachedPhoto(this);
        String googlePhotoUrl = DatabaseManager.getUserPhotoUrl(this);

        if (cachedPhoto != null && !cachedPhoto.isEmpty()) {
            byte[] decodedString = Base64.decode(cachedPhoto, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            profile_image.setImageBitmap(decodedByte);
        } else if (googlePhotoUrl != null && !googlePhotoUrl.isEmpty()) {
            Glide.with(this).load(googlePhotoUrl).into(profile_image);
        }

        String cachedId = DatabaseManager.getCachedFarmerId(this);
        if (!cachedId.isEmpty()) farmer_id_val.setText("ID: " + cachedId);

        String cachedCrops = DatabaseManager.getCachedCrops(this);
        if (!cachedCrops.isEmpty()) {
            crops_stat.setText(String.valueOf(cachedCrops.split(",").length));
        }

        String cachedArea = DatabaseManager.getCachedTotalArea(this);
        if (!cachedArea.isEmpty()) land_stat.setText(cachedArea + " Acres");

        String cachedSoil = DatabaseManager.getCachedSoil(this);
        if (!cachedSoil.isEmpty()) {
            updateSoilText(cachedSoil);
        }

        String cachedCity = DatabaseManager.getCachedCity(this);
        if (!cachedCity.isEmpty()) user_location.setText("Location: " + cachedCity);

        String cachedDistrict = DatabaseManager.getCachedDistrict(this);
        if (!cachedDistrict.isEmpty()) profile_district.setText("District: " + cachedDistrict);
        
        String cachedPhone = DatabaseManager.getUserPhone(this);
        if (!cachedPhone.isEmpty()) profile_phone.setText("Phone: " + cachedPhone);
    }

    private void displayProfileData(JsonObject profile) {
        if (profile.has("profile_photo") && !profile.get("profile_photo").isJsonNull()) {
            String base64 = profile.get("profile_photo").getAsString();
            if (!base64.isEmpty()) {
                byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                profile_image.setImageBitmap(decodedByte);
            }
        }

        if (profile.has("farmer_id")) farmer_id_val.setText("ID: " + profile.get("farmer_id").getAsString());

        if (profile.has("crops")) {
            String cropsStr = profile.get("crops").getAsString();
            if (cropsStr != null && !cropsStr.isEmpty()) {
                String[] cropsArray = cropsStr.split(",");
                crops_stat.setText(String.valueOf(cropsArray.length));
            } else {
                crops_stat.setText("0");
            }
        }

        if (profile.has("total area")) land_stat.setText(profile.get("total area").getAsString() + " Acres");
        if (profile.has("soil")) {
            updateSoilText(profile.get("soil").getAsString());
        }
        if (profile.has("phone")) profile_phone.setText("Phone: " + profile.get("phone").getAsString());
        if (profile.has("city")) user_location.setText("Location: " + profile.get("city").getAsString());
        if (profile.has("district")) profile_district.setText("District: " + profile.get("district").getAsString());
    }

    private void updateSoilText(String soil) {
        if (soil == null || soil.isEmpty()) return;
        
        soil_stat.setText(soil.toUpperCase());
        
        String soilLower = soil.toLowerCase();
        int color;
        if (soilLower.contains("black")) {
            color = Color.parseColor("#212121");
        } else if (soilLower.contains("red")) {
            color = Color.parseColor("#D32F2F");
        } else if (soilLower.contains("alluvial")) {
            color = Color.parseColor("#8D6E63");
        } else if (soilLower.contains("clay")) {
            color = Color.parseColor("#5D4037");
        } else if (soilLower.contains("sandy")) {
            color = Color.parseColor("#FFA000");
        } else if (soilLower.contains("loamy")) {
            color = Color.parseColor("#388E3C");
        } else if (soilLower.contains("laterite")) {
            color = Color.parseColor("#A52A2A");
        } else {
            color = ContextCompat.getColor(this, R.color.primaryGreen);
        }
        soil_stat.setTextColor(color);
    }
}
