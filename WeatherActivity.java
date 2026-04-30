package com.example.ai_agri;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Looper;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import com.example.ai_agri.utils.WeatherCache;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private static final String TAG = "WeatherActivity";
    private FusedLocationProviderClient fusedLocationClient;
    private LineChart lineChart;
    private ImageView ivIcon;
    
    private TextView tvCity, tvTemp, tvCondition, tvHumidity, tvWind, tvAqi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        tvCity = findViewById(R.id.city);
        tvTemp = findViewById(R.id.temp);
        tvCondition = findViewById(R.id.condition);
        tvHumidity = findViewById(R.id.humidity);
        tvWind = findViewById(R.id.wind);
        tvAqi = findViewById(R.id.aqi);
        lineChart = findViewById(R.id.weatherChart);
        ivIcon = findViewById(R.id.weatherIcon);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        loadCachedWeather();
        getCurrentLocation();
    }

    private void loadCachedWeather() {
        WeatherCache.CachedWeather cached = WeatherCache.getCachedWeather(this);
        if (cached != null) {
            tvTemp.setText(cached.temp);
            tvCondition.setText(cached.condition);
            tvCity.setText(cached.city);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, default to Kolhapur
            fetchWeatherData(0, 0);
            tvCity.setText("Kolhapur, Maharashtra");
            return;
        }

        // Request a fresh high-accuracy location
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMaxUpdates(1)
                .build();

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    double lat = locationResult.getLastLocation().getLatitude();
                    double lon = locationResult.getLastLocation().getLongitude();
                    Log.d(TAG, "Weather Fresh Location: " + lat + ", " + lon);
                    fetchWeatherData(lat, lon);
                    updateCityName(lat, lon);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error", e);
            // Default to Kolhapur on failure
            fetchWeatherData(0, 0);
            tvCity.setText("Kolhapur, Maharashtra");
        }
    }

    private void updateCityName(double lat, double lon) {
        new Thread(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String city = address.getLocality();
                    String district = address.getSubAdminArea();
                    String state = address.getAdminArea();
                    
                    // Fallback logic for city name
                    String displayCity = city != null ? city : (district != null ? district : "Unknown Location");
                    
                    runOnUiThread(() -> tvCity.setText(displayCity + ", " + state));
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoder error", e);
            }
        }).start();
    }

    private void fetchWeatherData(double lat, double lon) {
        String apiKey = "03cd45dfe3c942598ff144346262304";
        // Using "Kolhapur" as query if lat/lon are 0, or just standard lat,lon
        String query = (lat == 0 && lon == 0) ? "Kolhapur" : String.format(Locale.US, "%f,%f", lat, lon);
        String url = String.format(Locale.US, "https://api.weatherapi.com/v1/forecast.json?key=%s&q=%s&days=7&aqi=yes", apiKey, query);

        Log.d(TAG, "Fetching weather from: " + url);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Weather Network Error: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(WeatherActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String jsonData = response.body() != null ? response.body().string() : null;
                
                if (response.isSuccessful() && jsonData != null) {
                    try {
                        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
                        
                        if (jsonObject.has("error")) {
                            String errorMsg = jsonObject.getAsJsonObject("error").get("message").getAsString();
                            Log.e(TAG, "API Error: " + errorMsg);
                            runOnUiThread(() -> Toast.makeText(WeatherActivity.this, "API Error: " + errorMsg, Toast.LENGTH_SHORT).show());
                            return;
                        }

                        JsonObject current = jsonObject.getAsJsonObject("current");
                        if (current == null) {
                            Log.e(TAG, "Current weather data missing");
                            return;
                        }

                        double currentTemp = current.has("temp_c") ? current.get("temp_c").getAsDouble() : 0;
                        JsonObject forecast = jsonObject.getAsJsonObject("forecast");
                        JsonArray forecastDays = (forecast != null && forecast.has("forecastday")) ? forecast.getAsJsonArray("forecastday") : new JsonArray();

                        JsonObject conditionObj = current.getAsJsonObject("condition");
                        int weatherCode = (conditionObj != null && conditionObj.has("code")) ? conditionObj.get("code").getAsInt() : 1000;
                        String conditionText = (conditionObj != null && conditionObj.has("text")) ? conditionObj.get("text").getAsString() : "Unknown";
                        double windSpeed = current.has("wind_kph") ? current.get("wind_kph").getAsDouble() : 0;
                        int humidity = current.has("humidity") ? current.get("humidity").getAsInt() : 0;

                        // Extract AQI from WeatherAPI response and convert to India AQI
                        int aqiValue = 0;
                        if (current.has("air_quality")) {
                            JsonObject airQuality = current.getAsJsonObject("air_quality");
                            if (airQuality.has("pm2_5")) {
                                double pm25 = airQuality.get("pm2_5").getAsDouble();
                                aqiValue = calculateIndiaAQI(pm25);
                            }
                        }

                        final int finalAqiValue = aqiValue;

                        runOnUiThread(() -> {
                            String tempStr = Math.round(currentTemp) + "°C";
                            tvTemp.setText(tempStr);
                            tvCondition.setText(conditionText);
                            tvWind.setText(windSpeed + " km/h");
                            tvHumidity.setText(humidity + "%");
                            
                            if (finalAqiValue > 0) {
                                tvAqi.setText(String.valueOf(finalAqiValue));
                                tvAqi.setVisibility(View.VISIBLE);
                                updateAqiColor(finalAqiValue);
                            } else {
                                tvAqi.setVisibility(View.GONE);
                            }

                            updateMainIcon(weatherCode);
                            
                            // Update cache
                            WeatherCache.saveWeather(WeatherActivity.this, tempStr, conditionText, tvCity.getText().toString());
                            
                            List<Entry> chartEntries = new ArrayList<>();
                            List<String> xLabels = new ArrayList<>();

                            for (int i = 0; i < forecastDays.size(); i++) {
                                JsonObject dayObj = forecastDays.get(i).getAsJsonObject();
                                String dateStr = dayObj.has("date") ? dayObj.get("date").getAsString() : "";
                                String dayLabel = dateStr.length() > 5 ? dateStr.substring(5) : dateStr; // MM-DD
                                
                                JsonObject dayData = dayObj.getAsJsonObject("day");
                                if (dayData != null && dayData.has("maxtemp_c")) {
                                    float maxVal = (float) dayData.get("maxtemp_c").getAsDouble();
                                    chartEntries.add(new Entry(i, maxVal));
                                    xLabels.add(dayLabel);
                                }
                            }
                            setupChart(chartEntries, xLabels);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Parse error: " + e.getMessage());
                        Log.e(TAG, "JSON content: " + jsonData);
                    }
                } else {
                    Log.e(TAG, "Weather Response Unsuccessful: " + response.code() + " - " + response.message());
                    if (jsonData != null && jsonData.contains("error")) {
                         Log.e(TAG, "Error body: " + jsonData);
                    }
                    runOnUiThread(() -> Toast.makeText(WeatherActivity.this, "Weather update failed: " + response.code(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void updateMainIcon(int code) {
        if (code == 1000) ivIcon.setImageResource(R.drawable.baseline_wheather_sunny_24);
        else if (code == 1003) ivIcon.setImageResource(R.drawable.baseline_wheather_sunny_24);
        else if (code == 1006 || code == 1009) ivIcon.setImageResource(R.drawable.weather);
        else if (code == 1030 || code == 1135) ivIcon.setImageResource(R.drawable.weather); // Foggy
        else if (code >= 1063 && code <= 1201) ivIcon.setImageResource(R.drawable.water); // Rain
        else if (code >= 1240 && code <= 1246) ivIcon.setImageResource(R.drawable.water2); // Showers
        else if (code >= 1273) ivIcon.setImageResource(R.drawable.weather); // Thunderstorm
        else ivIcon.setImageResource(R.drawable.baseline_wheather_sunny_24);
    }

    private void setupChart(List<Entry> entries, List<String> labels) {
        if (entries == null || entries.isEmpty()) return;

        LineDataSet dataSet = new LineDataSet(entries, "Temperature");
        
        // Line styling
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setLineWidth(3f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.parseColor("#2E3A59"));
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return Math.round(value) + "°";
            }
        });
        
        // Circle styling
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#4CAF50"));
        dataSet.setCircleRadius(5f);
        dataSet.setCircleHoleRadius(2.5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(Color.WHITE);

        // Gradient fill
        dataSet.setDrawFilled(true);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#804CAF50"), Color.TRANSPARENT});
        dataSet.setFillDrawable(gradient);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // X Axis styling
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(Color.parseColor("#757575"));
        xAxis.setTextSize(10f);
        xAxis.setYOffset(10f);

        // Left Y Axis styling
        lineChart.getAxisLeft().setEnabled(true);
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#EEEEEE"));
        lineChart.getAxisLeft().setDrawAxisLine(false);
        lineChart.getAxisLeft().setTextColor(Color.parseColor("#757575"));
        lineChart.getAxisLeft().setXOffset(10f);
        
        // General chart styling
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.setExtraOffsets(10f, 20f, 10f, 10f);
        
        lineChart.animateY(1000);
        lineChart.invalidate();
    }

    private int calculateIndiaAQI(double pm25) {
        if (pm25 <= 30) return (int) (pm25 * 50 / 30);
        if (pm25 <= 60) return (int) (51 + (pm25 - 30) * (100 - 51) / (60 - 30));
        if (pm25 <= 90) return (int) (101 + (pm25 - 60) * (200 - 101) / (90 - 60));
        if (pm25 <= 120) return (int) (201 + (pm25 - 90) * (300 - 201) / (120 - 90));
        if (pm25 <= 250) return (int) (301 + (pm25 - 120) * (400 - 301) / (250 - 120));
        return (int) (401 + (pm25 - 250) * (500 - 401) / (500 - 250));
    }

    private void updateAqiColor(int aqi) {
        if (aqi <= 50) tvAqi.setTextColor(Color.parseColor("#4CAF50")); // Good
        else if (aqi <= 100) tvAqi.setTextColor(Color.parseColor("#8BC34A")); // Satisfactory
        else if (aqi <= 200) tvAqi.setTextColor(Color.parseColor("#FFC107")); // Moderate
        else if (aqi <= 300) tvAqi.setTextColor(Color.parseColor("#FF9800")); // Poor
        else tvAqi.setTextColor(Color.parseColor("#F44336")); // Very Poor
    }

    private String getWeatherCondition(int code) {
        if (code == 1000) return "Clear sky";
        if (code == 1003) return "Partly cloudy";
        if (code == 1006 || code == 1009) return "Cloudy";
        if (code == 1030 || code == 1135) return "Foggy";
        if (code >= 1063 && code <= 1201) return "Rainy";
        if (code >= 1240 && code <= 1246) return "Rain showers";
        if (code >= 1273) return "Thunderstorm";
        return "Unknown";
    }
}