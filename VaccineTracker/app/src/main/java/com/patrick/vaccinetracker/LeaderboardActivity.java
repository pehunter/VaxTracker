package com.patrick.vaccinetracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.vaccinetracker.R;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {

    int mode = 0;
    List<NationalData> sortedData = new ArrayList<>();
    Resources.Theme currentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        LinearLayout cont = findViewById(R.id.leaderboard_container);
        ImageButton back = findViewById(R.id.leaderboard_backButton);

        currentTheme = getTheme();

        updateLeaderboard(cont);

        Spinner modeSwitch = findViewById(R.id.leaderboard_optionSelect);
        modeSwitch.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = modeSwitch.getSelectedItem().toString();
                //update mode
                switch(item) {
                    case "Single Dose, percent": mode = 0;
                        break;
                    case "Single Dose, number": mode = 1;
                        break;
                    case "Fully Vaccinated, percent": mode = 2;
                        break;
                    case "Fully Vaccinated, number": mode = 3;
                        break;
                    case "Booster Shot, percent": mode = 4;
                        break;
                    case "Booster Shot, number": mode = 5;
                        break;
                    case "Total Doses Administered": mode = 6;
                        break;
                }

                //refresh
                updateLeaderboard(cont);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing
            }
        });

        back.setOnClickListener(v -> finish());
    }

    private double modeCompare(NationalData a, NationalData b) {
        switch(mode) {
            case 0: return b.getPDose() - a.getPDose();
            case 1: return b.getNDose() - a.getNDose();
            case 2: return b.getPVax() - a.getPVax();
            case 3: return b.getNVax() - a.getNVax();
            case 4: return b.getPBoost() - a.getPBoost();
            case 5: return b.getNBoost() - a.getNBoost();
            case 6: return b.getTotal() - a.getTotal();
        }
        return 0;
    }

    private void sortData() {
        //reset list
        sortedData.clear();
        sortedData.add(MainActivity.statesData.get(0));

        //binary add based on mode
        for(int state = 1; state < 52; state++) {
            NationalData area = MainActivity.statesData.get(state);
            int l = 0;
            int h = sortedData.size() - 1;
            while (l <= h) {
                int mid = (l + h) / 2;
                NationalData midpt = sortedData.get(mid);
                double comp = modeCompare(area, midpt);
                if (comp > 0) {
                    l = mid + 1;
                } else {
                    h = mid - 1;
                }
            }
            sortedData.add(l, area);
        }
    }

    private void updateLeaderboard(LinearLayout root) {
        //sort data before continuing
        sortData();

        //plug in info
        for(int idx = 0; idx < 52; idx++) {
            CardView base = (CardView) root.getChildAt(idx);
            TypedValue color = new TypedValue();
            switch(idx) {
                case 0: currentTheme.resolveAttribute(R.attr.colorGold, color, true);
                        break;
                case 1: currentTheme.resolveAttribute(R.attr.colorSilver, color, true);
                        break;
                case 2: currentTheme.resolveAttribute(R.attr.colorBronze, color, true);
                        break;
            }
            if(idx < 3)
                base.setCardBackgroundColor(color.data);

            ConstraintLayout card = (ConstraintLayout) base.getChildAt(0);
            NationalData state = sortedData.get(idx);

            //set rank
            TextView rank = (TextView) card.getChildAt(1);
            rank.setText(Integer.toString(idx + 1));

            //set state
            TextView label = (TextView) card.getChildAt(2);
            label.setText(state.getName());

            //set data
            TextView dataLabel = (TextView) card.getChildAt(4);
            dataLabel.setText(getDataFormatted(state));
            if(idx > 2)
                dataLabel.setTextColor(getVacColor());

            //set button
            Button goToState = (Button) card.getChildAt(3);
            goToState.setOnClickListener(v -> {
                Intent details = new Intent(this, DetailsActivity.class);
                details.putExtra("StateIndex", state.getPos());
                startActivity(details);
            });
            currentTheme.resolveAttribute(R.attr.colorOnSecondary, color, true);
            if(idx < 3)
                goToState.setTextColor(color.data);
        }
    }

    @SuppressLint("DefaultLocale")
    private String getDataFormatted(NationalData data) {
        switch(mode) {
            case 0: return (int)data.getPDose() + "%";
            case 1: return String.format("%,d",data.getNDose());
            case 2: return (int)data.getPVax() + "%";
            case 3: return String.format("%,d",data.getNVax());
            case 4: return (int)data.getPBoost() + "%";
            case 5: return String.format("%,d",data.getNBoost());
            case 6: return String.format("%,d",data.getTotal());
        }
        return "";
    }

    //get respective color per current mode
    private int getVacColor() {
        switch(mode) {
            case 0:
            case 1:
                return getResources().getColor(R.color.dose);
            case 2:
            case 3:
                return getResources().getColor(R.color.both);
            case 4:
            case 5:
                return getResources().getColor(R.color.boost);
            case 6:
                return getResources().getColor(R.color.purple_200);
        }
        return 0;
    }

}