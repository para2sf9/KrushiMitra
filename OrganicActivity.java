package com.example.ai_agri;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ai_agri.adapters.OrganicAdapter;
import com.example.ai_agri.models.OrganicModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;

public class OrganicActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ArrayList<OrganicModel> list;
    OrganicAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organic);

        recyclerView = findViewById(R.id.organicRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        list = new ArrayList<>();

        loadOrganicJSON();

        adapter = new OrganicAdapter(this, list);
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
    }

    private void loadOrganicJSON() {

        try {

            InputStream is = getAssets().open("organic.json");
            int size = is.available();

            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");

            JSONObject object = new JSONObject(json);
            JSONArray array = object.getJSONArray("organic_methods");

            for(int i = 0; i < array.length(); i++) {

                JSONObject organic = array.getJSONObject(i);

                list.add(new OrganicModel(
                        organic.getString("name"),
                        organic.getString("description"),
                        organic.getString("benefits"),
                        organic.getString("usage"),
                        organic.getString("image")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}