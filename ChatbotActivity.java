package com.example.ai_agri;

import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChatbotActivity extends AppCompatActivity {

    private EditText etMessage;
    private FloatingActionButton btnSend;
    private LinearLayout chatContainer;
    private NestedScrollView chatScroll;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        chatContainer = findViewById(R.id.chatContainer);
        chatScroll = findViewById(R.id.chatScroll);

        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();

            if (!message.isEmpty()) {
                addMessage(message, true);
                etMessage.setText("");
                getBotResponse(message);
            }
        });

        addMessage("Namaste! I am your Krishi Assistant. Ask me any question about agriculture, crops, or farming types!", false);
    }

    private void addMessage(String text, boolean isUser) {
        View bubble = getLayoutInflater().inflate(
                isUser ? R.layout.item_chat_user : R.layout.item_chat_bot,
                chatContainer,
                false
        );

        TextView tv = bubble.findViewById(isUser ? R.id.user_message_text : R.id.bot_message_text);
        
        if (!isUser) {
            TextView btnReadMore = bubble.findViewById(R.id.btnReadMore);
            
            // If answer is long, truncate it and show "Read More"
            if (text.length() > 250) {
                String truncatedText = text.substring(0, 250) + "...";
                setHtmlText(tv, truncatedText);
                btnReadMore.setVisibility(View.VISIBLE);
                
                btnReadMore.setOnClickListener(v -> {
                    setHtmlText(tv, text);
                    btnReadMore.setVisibility(View.GONE);
                });
            } else {
                setHtmlText(tv, text);
                btnReadMore.setVisibility(View.GONE);
            }
        } else {
            setHtmlText(tv, text);
        }

        chatContainer.addView(bubble);
        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }

    private void setHtmlText(TextView tv, String text) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            tv.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
        } else {
            tv.setText(Html.fromHtml(text));
        }
    }

    private void getBotResponse(String message) {
        String lowerMessage = message.toLowerCase().trim();

        // Handle greetings and explain the bot's role
        if (lowerMessage.equals("hi") || lowerMessage.equals("hello") || lowerMessage.equals("namaste")) {
            addMessage("Namaste! I am your Krishi Assistant. My role is to help you with agricultural queries, farming techniques, crop management, and soil health. Please ask me anything related to agriculture!", false);
            return;
        }

        // Validate if the query is agriculture, nature, or pest-related
        String[] allowedKeywords = {
                "agriculture", "farming", "crop", "soil", "irrigation", "fertilizer", 
                "harvest", "pesticide", "seed", "tractor", "organic", "livestock", 
                "weather", "mandi", "yield", "cultivation", "plough", "wheat", "rice", 
                "maize", "pulses", "vegetable", "fruit", "plantation", "horticulture",
                "nature", "environment", "forest", "tree", "plant", "climate", 
                "rain", "ecology", "biodiversity", "wildlife", "ecosystem", "river", 
                "water", "mountain", "season", "pollution", "greenery", "earth",
                "disease", "pest", "insect", "gem", "worm", "fungus", "bacteria", 
                "virus", "prevention", "cure", "treatment", "pesticide", "herbicide", 
                "insecticide", "aphid", "locust", "caterpillar", "infestation", "blight"
        };

        boolean isAllowed = false;
        for (String keyword : allowedKeywords) {
            if (lowerMessage.contains(keyword)) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            addMessage("I'm sorry, I am specifically designed to assist with agricultural, nature, and crop protection topics. Please ask a question related to farming, plants, environment, or pest/disease management.", false);
            return;
        }

        // Step 1: Search Wikipedia for the most relevant page title
        String searchUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=" + message + "&format=json&origin=*&srlimit=1";

        Request request = new Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "AI_AGRI_App/1.0")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> addMessage("I'm having trouble connecting. Please check your internet.", false));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String body = response.body().string();
                        JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();
                        JsonArray searchResults = jsonObject.getAsJsonObject("query").getAsJsonArray("search");
                        
                        if (searchResults.size() > 0) {
                            String title = searchResults.get(0).getAsJsonObject().get("title").getAsString();
                            fetchPageSummary(title);
                        } else {
                            runOnUiThread(() -> addMessage("I couldn't find specific information on that. Could you try rephrasing or asking about a specific crop or pest?", false));
                        }
                    } catch (Exception e) {
                        runOnUiThread(() -> addMessage("Sorry, I encountered an error searching for that topic.", false));
                    }
                }
            }
        });
    }

    private void fetchPageSummary(String title) {
        // Step 2: Use Wikipedia REST API for a clean, concise summary
        String url = "https://en.wikipedia.org/api/rest_v1/page/summary/" + title.replace(" ", "_");

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "AI_AGRI_App/1.0")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> addMessage("Found " + title + " but couldn't load the summary.", false));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonObject jsonObject = JsonParser.parseString(response.body().string()).getAsJsonObject();
                        String extract = jsonObject.get("extract").getAsString();
                        String displayTitle = jsonObject.get("title").getAsString();
                        String description = jsonObject.has("description") ? jsonObject.get("description").getAsString() : "";

                        runOnUiThread(() -> {
                            StringBuilder finalResponse = new StringBuilder();
                            finalResponse.append("<b>").append(displayTitle).append("</b>");
                            if (!description.isEmpty()) {
                                finalResponse.append(" (").append(description).append(")");
                            }
                            finalResponse.append("<br><br>").append(extract);
                            
                            addMessage(finalResponse.toString(), false);
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> addMessage("I found " + title + " but had trouble reading the details.", false));
                    }
                } else {
                    runOnUiThread(() -> addMessage("I couldn't find a detailed summary for " + title + ".", false));
                }
            }
        });
    }
}
