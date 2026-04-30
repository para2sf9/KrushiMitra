package com.example.ai_agri;

import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.example.roboflow.RoboflowApiClient;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InsectDetectionApi {

    private static final String TAG = "InsectDetectionApi";
    private final RoboflowApiClient roboflowClient;
    private static final String INSECT_CLASSES = "ant, bee, bee_apis, bee_bombus, beetle, beetle_cocci, beetle_oedem, bug, bug_grapho, fly, fly_empi, fly_sarco, fly_small, hfly_episyr, hfly_eristal, hfly_eupeo, hfly_myathr, hfly_sphaero, hfly_syrphus, lepi, none_bg, none_bird, none_dirt, none_shadow, other, scorpionfly, wasp";

    public static class RawPrediction implements Comparable<RawPrediction> {
        @SerializedName(value = "class", alternate = {"top", "label", "class_name"})
        public String className;
        public double confidence;

        @Override
        public int compareTo(RawPrediction other) {
            return Double.compare(other.confidence, this.confidence); // Descending
        }
    }

    public InsectDetectionApi() {
        this.roboflowClient = new RoboflowApiClient();
    }

    public interface InsectCallback {
        void onSuccess(InsectResult result);
        void onError(String errorMessage);
    }

    public void detectInsect(File imageFile, InsectCallback callback) {
        if (imageFile == null || !imageFile.exists()) {
            callback.onError("Invalid image file");
            return;
        }

        String encodedImage = encodeImage(imageFile);
        if (encodedImage == null) {
            callback.onError("Failed to read image file");
            return;
        }

        roboflowClient.sendRequest(RoboflowApiClient.INSECT_WORKFLOW_URL, "base64", encodedImage, INSECT_CLASSES, new RoboflowApiClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "Response: " + response);
                InsectResult result = parseInsectResponse(response);
                if (result != null) {
                    callback.onSuccess(result);
                } else {
                    callback.onError("No insect detected in the image");
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
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

            int maxSize = 1024;
            int scale = 1;
            if (options.outWidth > maxSize || options.outHeight > maxSize) {
                scale = (int) Math.pow(2, (int) Math.ceil(Math.log(maxSize / (double) Math.max(options.outWidth, options.outHeight)) / Math.log(0.5)));
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = scale;
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
            if (bitmap == null) return null;

            if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
                float ratio = (float) bitmap.getWidth() / bitmap.getHeight();
                int width = (ratio > 1) ? maxSize : (int) (maxSize * ratio);
                int height = (ratio > 1) ? (int) (maxSize / ratio) : maxSize;
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            return android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Encoding error", e);
            return null;
        }
    }

    private InsectResult parseInsectResponse(String json) {
        try {
            JsonElement root = com.google.gson.JsonParser.parseString(json);
            List<RawPrediction> allPredictions = new ArrayList<>();
            findAllPredictions(root, allPredictions);
            
            Collections.sort(allPredictions);

            RawPrediction bestValidPrediction = null;
            for (RawPrediction pred : allPredictions) {
                if (pred.className == null) continue;
                String className = pred.className.toLowerCase();
                if (!className.contains("none_") && !className.equals("other")) {
                    bestValidPrediction = pred;
                    break;
                }
            }

            if (bestValidPrediction == null) {
                Log.w(TAG, "No valid insect prediction found");
                return null;
            }

            InsectResult result = new InsectResult();
            result.insectName = formatName(bestValidPrediction.className);
            result.confidence = (float) bestValidPrediction.confidence;
            mapInsectData(result, bestValidPrediction.className);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Parsing error", e);
            return null;
        }
    }

    private void findAllPredictions(JsonElement element, List<RawPrediction> results) {
        if (element == null || element.isJsonNull()) return;

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("predictions")) {
                JsonElement preds = obj.get("predictions");
                if (preds.isJsonArray()) {
                    JsonArray array = preds.getAsJsonArray();
                    for (JsonElement item : array) {
                        if (item.isJsonObject()) {
                            results.add(new Gson().fromJson(item, RawPrediction.class));
                        }
                    }
                } else {
                    findAllPredictions(preds, results);
                }
            }

            if ((obj.has("top") || obj.has("class") || obj.has("label") || obj.has("class_name")) && obj.has("confidence")) {
                results.add(new Gson().fromJson(obj, RawPrediction.class));
            }

            for (String key : obj.keySet()) {
                if (key.equals("output_image") || key.equals("predictions")) continue;
                findAllPredictions(obj.get(key), results);
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                findAllPredictions(item, results);
            }
        }
    }

    private String formatName(String raw) {
        if (raw == null) return "Unknown";
        String formatted = raw.replace("_", " ");
        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1).toLowerCase();
    }

    private void mapInsectData(InsectResult result, String rawClass) {
        switch (rawClass.toLowerCase()) {
            case "ant":
                result.scientificName = "Formicidae";
                result.status = "Neutral/Pest";
                result.description = "Ants are social insects. While some protect aphids (pests), others improve soil aeration.";
                result.controlMeasures = "Use boric acid baits, cinnamon powder, or boiling water on mounds. Keep the area clear of food waste.";
                break;
            case "bee":
            case "bee_apis":
            case "bee_bombus":
                result.scientificName = rawClass.equals("bee_apis") ? "Apis mellifera" : (rawClass.equals("bee_bombus") ? "Bombus sp." : "Anthophila");
                result.status = "Beneficial";
                result.description = "Essential pollinators for most crops. Their presence indicates a healthy ecosystem.";
                result.controlMeasures = "Do not kill. Provide water sources and bee-friendly plants. Avoid using broad-spectrum insecticides.";
                break;
            case "beetle":
            case "beetle_cocci":
            case "beetle_oedem":
                result.scientificName = rawClass.equals("beetle_cocci") ? "Coccinellidae" : "Coleoptera";
                result.status = rawClass.equals("beetle_cocci") ? "Beneficial" : "Pest/Neutral";
                result.description = rawClass.equals("beetle_cocci") ? "Ladybugs are predators that eat aphids and other soft-bodied pests." : "Beetles can be varied; some feed on leaves while others are decomposers.";
                result.controlMeasures = rawClass.equals("beetle_cocci") ? "Encourage their presence." : "Handpick larger beetles, use neem oil or pheromone traps.";
                break;
            case "bug":
            case "bug_grapho":
                result.scientificName = "Heteroptera";
                result.status = "Pest";
                result.description = "Sucking insects that can damage plant tissue and spread diseases.";
                result.controlMeasures = "Use insecticidal soaps, neem oil, or introduce natural predators like lacewings.";
                break;
            case "fly":
            case "fly_empi":
            case "fly_sarco":
            case "fly_small":
                result.scientificName = "Diptera";
                result.status = "Neutral/Pest";
                result.description = "Flies can be decomposers, pollinators, or pests that spread disease to livestock.";
                result.controlMeasures = "Improve sanitation, use yellow sticky traps, or light traps.";
                break;
            case "hfly_episyr":
            case "hfly_eristal":
            case "hfly_eupeo":
            case "hfly_myathr":
            case "hfly_sphaero":
            case "hfly_syrphus":
                result.scientificName = "Syrphidae";
                result.status = "Beneficial";
                result.description = "Hoverflies are excellent pollinators. Their larvae often feed on aphids.";
                result.controlMeasures = "Encourage by planting flowers like marigolds and dill. Avoid pesticides.";
                break;
            case "lepi":
                result.scientificName = "Lepidoptera";
                result.status = "Pest (as larvae)";
                result.description = "Butterflies and moths. While adults pollinate, their caterpillar stage can cause significant crop damage.";
                result.controlMeasures = "Use Bacillus thuringiensis (Bt), neem oil, or handpick caterpillars.";
                break;
            case "wasp":
                result.scientificName = "Vespidae";
                result.status = "Beneficial/Pest";
                result.description = "Wasps are natural predators of many agricultural pests, but can be a nuisance to humans.";
                result.controlMeasures = "Only control if nests are in high-traffic areas. Use decoy nests or specialized wasp traps.";
                break;
            case "scorpionfly":
                result.scientificName = "Panorpa sp.";
                result.status = "Neutral";
                result.description = "Scorpionflies primarily feed on dead insects and are not harmful to crops.";
                result.controlMeasures = "No control necessary.";
                break;
            default:
                result.scientificName = "Arthropoda";
                result.status = "Neutral";
                result.description = "An unidentified arthropod. Most are harmless or even beneficial to the garden.";
                result.controlMeasures = "Monitor the plant for damage before taking any action.";
                break;
        }
    }
}
