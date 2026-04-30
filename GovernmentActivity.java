package com.example.ai_agri;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ai_agri.adapters.SchemeAdapter;
import com.example.ai_agri.models.SchemeModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;

public class GovernmentActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    SchemeAdapter adapter;
    ArrayList<SchemeModel> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_government);

        recyclerView = findViewById(R.id.schemeRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        list = new ArrayList<>();

        loadJSON();

        adapter = new SchemeAdapter(this, list);
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
    }

    private void loadJSON() {

        try {

            InputStream is = getAssets().open("schemes.json");
            int size = is.available();

            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");

            JSONObject object = new JSONObject(json);
            JSONArray array = object.getJSONArray("schemes");


            for(int i = 0; i < array.length(); i++) {

                JSONObject scheme = array.getJSONObject(i);
                String image = scheme.getString("image");
                list.add(new SchemeModel(
                        scheme.getString("name"),
                        scheme.getString("description"),
                        scheme.getString("benefit"),
                        scheme.getString("eligibility"),
                        scheme.getString("image"),
                        scheme.getString("link")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}