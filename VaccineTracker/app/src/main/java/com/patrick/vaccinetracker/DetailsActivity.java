package com.patrick.vaccinetracker;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.vaccinetracker.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class DetailsActivity extends AppCompatActivity {
    NationalData thisData;
    Resources.Theme currentTheme;

    class YAxisAbbreviator extends ValueFormatter {
        private DecimalFormat format;
        public YAxisAbbreviator() {
            format = new DecimalFormat("#.#M");
        }

        @Override
        public String getFormattedValue(float value) {
            return format.format(value/1000000);
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //setup
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        //determine if its night mode
        currentTheme = getTheme();

        //retrieve state
        Intent information = getIntent();
        int SI = information.getIntExtra("StateIndex", -1);
        int CI = information.getIntExtra("CountyIndex", -1);
        if(SI > -1)
            thisData = MainActivity.statesData.get(SI);
        else
            thisData = MainActivity.countiesData.get(CI);

        //get elements
        TextView title = findViewById(R.id.details_stateName);
        ImageButton back = findViewById(R.id.details_backButton);
        TextView disc = findViewById(R.id.details_asOf);

        TextView bgTitle = findViewById(R.id.details_barGraphDesc);
        TextView boostNum = findViewById(R.id.details_numBooster);
        TextView doseNum = findViewById(R.id.details_numSingleDose);
        TextView vaxNum = findViewById(R.id.details_numFVax);
        TextView totalNum = findViewById(R.id.details_numTotal);

        PieChart totalVaxChart = findViewById(R.id.details_totalVax_Chart);
        PieChart boostChart = findViewById(R.id.details_boostPct);
        PieChart doseChart = findViewById(R.id.details_firstDosePct);

        BarChart distChart = findViewById(R.id.details_vaxDistChart);

        //setup data
        ArrayList<Integer> numData = new ArrayList<Integer>();
        numData.add(thisData.getNDose());
        numData.add(thisData.getNVax());
        numData.add(thisData.getNBoost());

        //fill in
        title.setText(thisData.getName());
        bgTitle.setText(String.format(bgTitle.getText().toString(), thisData.getName()));

        boostNum.setText(String.format("%,d",numData.get(2)));
        doseNum.setText(String.format("%,d",numData.get(0)));
        vaxNum.setText(String.format("%,d",numData.get(1)));
        totalNum.setText(String.format("%,d",thisData.getTotal()));

        //setup graphs
        int[] vaxColors = new int[]{getResources().getColor(R.color.both), getResources().getColor(R.color.offwhite)};
        int[] doseColors =  new int[]{getResources().getColor(R.color.dose), getResources().getColor(R.color.offwhite)};
        int[] boostColors =  new int[]{getResources().getColor(R.color.boost), getResources().getColor(R.color.offwhite)};
        int[] barColors = new int[]{getResources().getColor(R.color.dose),getResources().getColor(R.color.both),getResources().getColor(R.color.boost)};

        setPieChart(totalVaxChart, thisData.getPVax(), 60, "Vaccinated", vaxColors);
        setPieChart(doseChart, thisData.getPDose(), 45, "Doses", doseColors);
        setPieChart(boostChart, thisData.getPBoost(),45,"Booster", boostColors);
        setBarChart(distChart, numData, barColors);

        //setup back button
        back.setOnClickListener(v -> finish());

        //add last updated
        disc.setText(String.format(disc.getText().toString(), thisData.getDate()));
    }

    public void setPieChart(PieChart chart, double percent, int centerSize, String title, int[] colors) {
        ArrayList<PieEntry> entries = new ArrayList<PieEntry>();
        entries.add(new PieEntry((float) percent));
        entries.add(new PieEntry((float)(100f - percent)));

        PieDataSet set = new PieDataSet(entries, title);
        set.setColors(colors);
        set.setDrawIcons(false);
        set.setDrawValues(false);
        set.setSliceSpace(10f);

        PieData finalData = new PieData(set);
        finalData.setValueFormatter(new PercentFormatter());

        chart.setData(finalData);
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(87f);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.setUsePercentValues(true);
        chart.getLegend().setEnabled(false);
        chart.getDescription().setEnabled(false);

        chart.setCenterText(Double.toString(percent).substring(0,2) + "%");
        chart.setCenterTextColor(colors[0]);
        chart.setCenterTextSize(centerSize);

        chart.invalidate();
    }

    public void setBarChart(BarChart chart, List<Integer> data, int[] colors) {
        ArrayList<BarEntry> entries = new ArrayList<BarEntry>();

        for(int i = 0; i < data.size(); i++) {
            entries.add(new BarEntry(i,data.get(i)));
        }

        BarDataSet set = new BarDataSet(entries, "");
        set.setDrawIcons(false);
        set.setDrawValues(false);
        set.setColors(colors);

        ArrayList<IBarDataSet> sets = new ArrayList<IBarDataSet>();
        sets.add(set);

        BarData barData = new BarData(sets);

        chart.setData(barData);
        chart.getLegend().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setEnabled(false);
        chart.setTouchEnabled(false);

        YAxis axis = chart.getAxisLeft();
        axis.setValueFormatter(new YAxisAbbreviator());
        axis.setTextSize(16);
        axis.setDrawGridLines(false);
        axis.setAxisMinimum(0f);
        TypedValue color = new TypedValue();
        currentTheme.resolveAttribute(R.attr.colorOnSecondary, color, true);
        axis.setTextColor(color.data);

        chart.invalidate();
    }
}
