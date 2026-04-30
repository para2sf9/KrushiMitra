package com.example.ai_agri;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class SplashActivity extends AppCompatActivity {

    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private AlertDialog permissionDialog;
    private boolean isProceeding = false;
    private boolean splashTimeOutReached = false;
    private boolean isPermissionRequested = false;
    private int permissionRequestCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set status bar and navigation bar color to green
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primaryGreen));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.primaryGreen));

        setContentView(R.layout.activity_splash);

        // Find views
        LinearLayout logoContainer = findViewById(R.id.logo_container);
        ImageView logo = findViewById(R.id.splash_logo);
        TextView title = findViewById(R.id.splash_title);
        TextView subtitle = findViewById(R.id.splash_subtitle);

        // Load animations
        Animation scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        // Apply animations
        logo.startAnimation(scaleUp);
        title.startAnimation(fadeIn);
        subtitle.startAnimation(fadeIn);

        com.example.ai_agri.utils.ActivityTracker.clearActivities(this);

        // Pre-load database driver to speed up login later
        new Thread(() -> {
            try {
                Class.forName("org.postgresql.Driver");
            } catch (Exception ignored) {}
        }).start();

        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    isPermissionRequested = false;
                    Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                    if (fineLocationGranted != null && fineLocationGranted || coarseLocationGranted != null && coarseLocationGranted) {
                        checkAndProceed();
                    } else {
                        Toast.makeText(this, getString(R.string.location_permission_required), Toast.LENGTH_LONG).show();
                        showPermissionDeniedDialog();
                    }
                }
        );

        // Start permission check immediately
        checkAndProceed();

        // Wait for 800ms to show animations before attempting to proceed
        new Handler().postDelayed(() -> {
            splashTimeOutReached = true;
            checkAndProceed();
        }, 800);
    }

    private void checkAndProceed() {
        if (isProceeding) return;

        if (hasLocationPermission()) {
            if (splashTimeOutReached) {
                checkLocationSettings();
            }
        } else if (!isPermissionRequested) {
            if (permissionRequestCount == 0) {
                // Auto-ask the first time
                requestLocationPermission();
            } else if (splashTimeOutReached) {
                // If already denied once and we are past splash timeout, show our rationale dialog
                showPermissionDeniedDialog();
            }
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        if (isPermissionRequested) return;
        isPermissionRequested = true;
        permissionRequestCount++;
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void checkLocationSettings() {
        if (isProceeding) return;
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        boolean networkEnabled = false;

        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {}

        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {}

        if (!gpsEnabled && !networkEnabled) {
            showLocationSettingsDialog();
        } else {
            navigateToNextScreen();
        }
    }

    private void showLocationSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.location_services_disabled))
                .setMessage(getString(R.string.location_services_message))
                .setPositiveButton(getString(R.string.settings), (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton(getString(R.string.exit), (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showPermissionDeniedDialog() {
        if (permissionDialog != null && permissionDialog.isShowing()) return;

        boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        // Logic: 
        // 1. If we've already asked at least twice, change to Settings.
        // 2. If the user checked "Don't ask again" (showRationale is false), change to Settings.
        boolean shouldGoToSettings = (permissionRequestCount >= 2) || (!showRationale && permissionRequestCount > 0);

        String buttonText = shouldGoToSettings ? getString(R.string.settings) : getString(R.string.grant);
        String message = shouldGoToSettings ?
                getString(R.string.location_permission_required) + ". Please enable it in Settings." :
                getString(R.string.location_permission_message);

        permissionDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.permission_required))
                .setMessage(message)
                .setPositiveButton(buttonText, (dialog, which) -> {
                    if (shouldGoToSettings) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    } else {
                        requestLocationPermission();
                    }
                })
                .setNegativeButton(getString(R.string.exit), (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void navigateToNextScreen() {
        if (isProceeding) return;
        isProceeding = true;

        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isProceeding) return;
        
        // Re-check permissions and settings when returning to the app
        checkAndProceed();
    }
}
