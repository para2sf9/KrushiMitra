package com.example.ai_agri;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ai_agri.adapters.ToolAdapter;
import com.example.ai_agri.models.ToolModel;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ToolsActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ArrayList<ToolModel> list;
    ToolAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tools);

        recyclerView = findViewById(R.id.toolsRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        list = new ArrayList<>();
        loadToolsFromJson();

        adapter = new ToolAdapter(this, list);
        recyclerView.setAdapter(adapter);
    }

    private void loadToolsFromJson() {
        try {
            InputStream is = getAssets().open("tools.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            JSONObject jsonObject = new JSONObject(json);
            JSONArray array = jsonObject.getJSONArray("tools");

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String name = obj.getString("name");
                String usage = obj.getString("usage");
                String image = obj.getString("image");
                String category = obj.getString("category");

                list.add(new ToolModel(name, usage, image, category));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
