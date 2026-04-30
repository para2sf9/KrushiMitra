package com.example.ai_agri;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import com.example.roboflow.RoboflowApiClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SoilDetectionApi {

    private static final String TAG = "SoilDetectionApi";
    private final RoboflowApiClient roboflowClient;
    private static final String SOIL_CLASSES = "Alluvial_Soil, Arid_Soil, Black_Soil, Laterite_Soil, Mountain_Soil, Red_Soil, Yellow_Soil";

    public SoilDetectionApi() {
        this.roboflowClient = new RoboflowApiClient();
    }

    public interface SoilCallback {
        void onSuccess(SoilResult result);
        void onError(String errorMessage);
    }

    public void analyzeSoil(File imageFile, SoilCallback callback) {
        if (imageFile == null || !imageFile.exists()) {
            callback.onError("Invalid image file");
            return;
        }

        String encodedImage = encodeImage(imageFile);
        if (encodedImage == null) {
            callback.onError("Failed to read image file");
            return;
        }

        // Updated to use the 'general-segmentation-api-4' workflow and passing 
        // the required classes to ensure accurate soil segmentation and classification.
        roboflowClient.sendRequest(RoboflowApiClient.SOIL_WORKFLOW_URL, "base64", encodedImage, SOIL_CLASSES, new RoboflowApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Response: " + response);
                SoilResult result = parseClassificationResponse(response);
                if (result != null) {
                    callback.onSuccess(result);
                } else {
                    callback.onError("Failed to identify soil type");
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Request error: " + error);
                callback.onError(error);
            }
        });
    }

    private String encodeImage(File imageFile) {
        try {
            // Decode with bounds to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            // Calculate scale factor to resize image (max 1024px) for better detection
            int maxSize = 1024;
            int scale = 1;
            if (options.outWidth > maxSize || options.outHeight > maxSize) {
                scale = (int) Math.pow(2, (int) Math.ceil(Math.log(maxSize / 
                    (double) Math.max(options.outWidth, options.outHeight)) / Math.log(0.5)));
            }

            // Decode with scaling
            options.inJustDecodeBounds = false;
            options.inSampleSize = scale;
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            
            if (bitmap == null) return null;

            // Further resize to exact max dimension if needed
            if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
                float ratio = (float) bitmap.getWidth() / bitmap.getHeight();
                int width = (ratio > 1) ? maxSize : (int) (maxSize * ratio);
                int height = (ratio > 1) ? (int) (maxSize / ratio) : maxSize;
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // Use 80% quality JPEG to reduce payload size and work faster
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] byteArray = baos.toByteArray();
            return android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Encoding error", e);
            return null;
        }
    }

    private SoilResult parseClassificationResponse(String json) {
        try {
            // Parse as JsonElement to handle both objects and arrays at root
            JsonElement root = com.google.gson.JsonParser.parseString(json);
            SoilResult.RawPrediction topPrediction = findBestPrediction(root);

            if (topPrediction == null) {
                Log.w(TAG, "No prediction found in the response JSON");
                return null;
            }

            SoilResult result = new SoilResult();
            result.soilType = formatSoilName(topPrediction.className);
            result.confidence = (float) topPrediction.confidence;
            mapSoilData(result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Parsing error", e);
            return null;
        }
    }

    private SoilResult.RawPrediction findBestPrediction(JsonElement element) {
        if (element == null || element.isJsonNull()) return null;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            // 1. Check for 'predictions' block first (common for workflows)
            if (obj.has("predictions")) {
                JsonElement preds = obj.get("predictions");
                if (preds.isJsonArray() && preds.getAsJsonArray().size() > 0) {
                    return new Gson().fromJson(preds.getAsJsonArray().get(0), SoilResult.RawPrediction.class);
                } else if (preds.isJsonObject()) {
                    SoilResult.RawPrediction result = findBestPrediction(preds);
                    if (result != null) return result;
                }
            }

            // 2. Check if this object itself is a classification result
            if ((obj.has("top") || obj.has("class") || obj.has("label")) && obj.has("confidence")) {
                return new Gson().fromJson(obj, SoilResult.RawPrediction.class);
            }

            // 3. Recursively check all children, but skip the large output image
            for (String key : obj.keySet()) {
                if (key.equals("output_image")) continue;
                SoilResult.RawPrediction result = findBestPrediction(obj.get(key));
                if (result != null) return result;
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                SoilResult.RawPrediction result = findBestPrediction(item);
                if (result != null) return result;
            }
        }
        return null;
    }

    private String formatSoilName(String raw) {
        if (raw == null || raw.equalsIgnoreCase("Unknown")) return "Unknown / Mixed Soil";
        String formatted = raw.replace("_", " ");
        if (formatted.length() > 0) {
            return formatted.substring(0, 1).toUpperCase() + formatted.substring(1).toLowerCase();
        }
        return "Unknown / Mixed Soil";
    }

    private void mapSoilData(SoilResult result) {
        String type = result.soilType.toLowerCase();
        
        if (type.contains("alluvial") || type.contains("silt")) {
            result.soilType = "Alluvial Soil";
            result.color = "Light Grey to Ash Grey";
            result.description = "Alluvial soils are formed by the deposition of silt by rivers. They are among the most fertile soils in India and are rich in potash and lime but deficient in nitrogen, phosphorus, and humus. They have a fine to coarse texture and are highly productive for a variety of crops.";
            result.isSuitableForFarming = true;
            result.whereUsed = "Predominantly found in the Indo-Gangetic plains (Punjab, Haryana, Uttar Pradesh, Bihar, West Bengal), as well as in the coastal regions and river valleys of South India.";
        } else if (type.contains("black") || type.contains("regur") || type.contains("clay")) {
            result.soilType = "Black Soil (Regur)";
            result.color = "Deep Black to Light Black";
            result.description = "Black soil is famous for its self-ploughing capacity and moisture retention. It has a high clay content (up to 60%) and becomes sticky when wet. It is rich in lime, iron, magnesium, and alumina, but lacks phosphorus, nitrogen, and organic matter.";
            result.isSuitableForFarming = true;
            result.whereUsed = "Concentrated in the Deccan Trap region, covering Maharashtra, Madhya Pradesh, Gujarat, Andhra Pradesh, and parts of Tamil Nadu and Karnataka.";
        } else if (type.contains("red")) {
            result.soilType = "Red Soil";
            result.color = "Reddish (due to Iron Oxide diffusion)";
            result.description = "Formed from the weathering of ancient crystalline and metamorphic rocks. Its reddish color is due to the presence of iron in crystalline form. It is generally porous and friable, lacking in nitrogen, phosphorus, and humus, but responds well to irrigation and chemical fertilizers.";
            result.isSuitableForFarming = true;
            result.whereUsed = "Covers a large part of the eastern and southern peninsula, including Tamil Nadu, Odisha, Chhattisgarh, and parts of Karnataka and Maharashtra.";
        } else if (type.contains("yellow")) {
            result.soilType = "Yellow Soil";
            result.color = "Yellowish (Hydrated form of Red Soil)";
            result.description = "Yellow soil is essentially the hydrated form of red soil. When red soil comes in contact with significant moisture, it turns yellow. It shares many characteristics with red soil, being slightly more acidic and typically found in areas of higher rainfall.";
            result.isSuitableForFarming = true;
            result.whereUsed = "Found alongside red soils in parts of Odisha, Chhattisgarh, and the southern parts of the Middle Ganga plain.";
        } else if (type.contains("laterite")) {
            result.soilType = "Laterite Soil";
            result.color = "Rusty Red / Brick-like";
            result.description = "Formed under conditions of high temperature and heavy rainfall with alternate wet and dry periods. This leads to 'leaching' where nutrients are washed away, leaving behind iron and aluminum oxides. It is acidic and low in humus content.";
            result.isSuitableForFarming = true;
            result.whereUsed = "Mainly found on the summits of Western Ghats, Eastern Ghats, Malabar Coast, and parts of Assam and Meghalaya.";
        } else if (type.contains("arid") || type.contains("desert")) {
            result.soilType = "Arid / Desert Soil";
            result.color = "Red to Brown";
            result.description = "Characterized by a sandy texture and high saline content. Due to the dry climate and high evaporation, it lacks moisture and humus. In some areas, the salt content is so high that common salt is obtained by evaporating the saline water.";
            result.isSuitableForFarming = false;
            result.whereUsed = "Predominantly in Western Rajasthan, parts of Northern Gujarat, and southern parts of Punjab.";
        } else if (type.contains("mountain") || type.contains("forest") || type.contains("hilly")) {
            result.soilType = "Mountain / Forest Soil";
            result.color = "Dark Brown to Black";
            result.description = "These soils vary greatly depending on the altitude and mountain environment. In valley sides, they are loamy and silty, while on upper slopes, they are coarse-grained. They are rich in organic matter (humus) but are often acidic in higher altitude regions.";
            result.isSuitableForFarming = true;
            result.whereUsed = "Found in the Himalayan regions, including Jammu and Kashmir, Himachal Pradesh, Uttarakhand, Sikkim, and Arunachal Pradesh.";
        } else if (type.contains("peaty") || type.contains("marshy")) {
            result.soilType = "Peaty / Marshy Soil";
            result.color = "Black and Heavy";
            result.description = "Formed in areas of heavy rainfall and high humidity where there is a good growth of vegetation. It contains a large amount of dead organic matter (up to 40-50%), making it highly acidic and heavy.";
            result.isSuitableForFarming = true;
            result.whereUsed = "Kottayam and Alappuzha districts of Kerala, coastal regions of Odisha, and Tamil Nadu.";
        } else {
            result.soilType = "Unknown / Mixed Soil";
            result.color = "Variable";
            result.description = "The specific soil characteristics could not be precisely identified from the image. This may be a mixed soil type or a transition zone between major soil categories. It is recommended to perform a physical pH and nutrient test for accurate farming advice.";
            result.isSuitableForFarming = true;
            result.whereUsed = "Found in transition zones across various geographical regions.";
        }
    }
}
