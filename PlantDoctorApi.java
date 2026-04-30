package com.example.ai_agri;

import okhttp3.*;
import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class PlantDoctorApi {

    // URL for leaf disease prediction
    private static final String LEAF_URL = "https://shashankkalwa-plantdoctor-ai-disease-detection-api.hf.space/predict";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    public interface Callback {
        void onSuccess(PredictionResult result);
        void onError(String errorMessage);
    }

    public void predict(File imageFile, String category, Callback callback) {
        if (imageFile == null || !imageFile.exists()) {
            callback.onError("Image file is invalid or does not exist.");
            return;
        }

        // We now only support leaf prediction; other categories are ignored.
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "file",
                        imageFile.getName(),
                        RequestBody.create(imageFile, MediaType.parse("image/jpeg"))
                )
                .build();

        Request request = new Request.Builder()
                .url(LEAF_URL)
                .addHeader("accept", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getLocalizedMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        callback.onError("Empty response from server");
                        return;
                    }

                    String json = responseBody.string();
                    if (!response.isSuccessful()) {
                        callback.onError("Server error " + response.code());
                        return;
                    }

                    PredictionResult result = new Gson().fromJson(json, PredictionResult.class);
                    if (result == null) {
                        callback.onError("Could not parse prediction result");
                        return;
                    }

                    // Filter out non-leaf detections
                    if (isNotLeaf(result)) {
                        callback.onError("Leaf not detected. Please scan a plant leaf clearly.");
                    } else {
                        callback.onSuccess(result);
                    }
                } catch (Exception e) {
                    callback.onError("Process error: " + e.getMessage());
                }
            }
        });
    }

    private boolean isNotLeaf(PredictionResult result) {
        if (result.plant == null || result.plant.isEmpty()) return true;
        
        String plant = result.plant.toLowerCase();
        return plant.contains("background") || 
               plant.contains("non-leaf") || 
               plant.contains("unknown") || 
               plant.contains("ambient");
    }
}
