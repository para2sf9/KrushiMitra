package com.example.ai_agri;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SoilResult {
    public String soilType;
    public String color;
    public String description;
    public boolean isSuitableForFarming;
    public String whereUsed;
    public float confidence;

    // This matches the expected output from the workflow or your mapping logic
    public static class RawPrediction {
        @SerializedName(value = "class", alternate = {"top", "label", "class_name"})
        public String className;
        public double confidence;
    }
}
