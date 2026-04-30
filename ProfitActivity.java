package com.example.ai_agri;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ai_agri.utils.DatabaseManager;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.*;

import java.util.ArrayList;

public class ProfitActivity extends AppCompatActivity {

    EditText cropName,cost, yield, price;
    Button calcBtn;
    TextView result, profitDesc;
    BarChart barChart;
    ImageView cal_profit_loss_img;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!DatabaseManager.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_profit);

        cropName = findViewById(R.id.cropName);
        cost = findViewById(R.id.cost);
        yield = findViewById(R.id.yield);
        price = findViewById(R.id.price);
        calcBtn = findViewById(R.id.calcBtn);
        result = findViewById(R.id.result);
        profitDesc = findViewById(R.id.profit_desc);
        barChart = findViewById(R.id.barChart);
        cal_profit_loss_img = findViewById(R.id.cal_profit_loss_img);
        calcBtn.setOnClickListener(v -> calculateProfit());
    }

    private void calculateProfit() {

        int costVal = getValue(cost);
        int yieldVal = getValue(yield);
        int priceVal = getValue(price);

        int sellPrice = yieldVal * priceVal;
        int profit = sellPrice - costVal;

        if (profit > 0) {
            result.setText("Profit: ₹" + profit);
            result.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            profitDesc.setText("Great job! You've made a profit of ₹" + profit + ". Consider reinvesting.");
            cal_profit_loss_img.setImageResource(R.drawable.profit);
            cal_profit_loss_img.setVisibility(View.VISIBLE);
        } else if (profit < 0) {
            result.setText("Loss: ₹" + Math.abs(profit));
            result.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            profitDesc.setText("You've incurred a loss. Analyze your expenses and market prices.");
            cal_profit_loss_img.setImageResource(R.drawable.loss);
            cal_profit_loss_img.setVisibility(View.VISIBLE);
        } else {
            result.setText("Break-even");
            result.setTextColor(getResources().getColor(android.R.color.darker_gray));
            profitDesc.setText("Your earnings cover your expenses. Look for ways to increase yield.");
            cal_profit_loss_img.setVisibility(View.GONE);
        }

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(1, costVal));
        entries.add(new BarEntry(2, sellPrice));
        entries.add(new BarEntry(3, profit));

        BarDataSet dataSet = new BarDataSet(entries, "Farm Data");
        BarData data = new BarData(dataSet);

        barChart.setData(data);
        barChart.invalidate();
    }

    private int getValue(EditText editText) {
        if (editText.getText().toString().isEmpty()) {
            return 0;
        }
        return Integer.parseInt(editText.getText().toString());
    }
}