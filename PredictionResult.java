package com.example.ai_agri;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PredictionResult {

    @SerializedName("plant")
    public String plant;

    @SerializedName("disease_name")
    public String diseaseName;

    @SerializedName("confidence_pct")
    public float confidencePct;

    @SerializedName("is_healthy")
    public boolean isHealthy;

    @SerializedName("description")
    public String description;

    @SerializedName("prevention")
    public List<String> prevention;

    @SerializedName("cure")
    public List<String> cure;

    @SerializedName("top5")
    public List<Top5Item> top5;

    @SerializedName("inference_time_ms")
    public float inferenceTimeMs;

    @SerializedName("warning")
    public String warning;

    public static class Top5Item {
        @SerializedName("disease_name")
        public String diseaseName;

        @SerializedName("confidence_pct")
        public float confidencePct;
    }
}