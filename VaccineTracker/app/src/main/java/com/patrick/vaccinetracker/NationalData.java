package com.patrick.vaccinetracker;

import org.json.simple.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class NationalData implements Comparable {
    double percentVaccinated = 0;
    double percentDose = 0;
    double percentBooster = 0;
    int numVaccinated = 0;
    int numDose = 0;
    int numBooster = 0;
    int numTotal = 0;
    int pos = -1;
    String name = "";
    String abbreviation = "";
    Date date;

    static SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd");
    static SimpleDateFormat stringFormat = new SimpleDateFormat("MM/dd/yy");
    static String[] namesList = new String[]{"Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut", "Delaware", "District of Columbia", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Puerto Rico", "Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin", "Wyoming"};
    static String[] abbList = new String[]{"AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL", "GA", "HI", "ID", "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA", "PR", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY"};

    public NationalData(JSONObject jsonData) throws ParseException {
        percentVaccinated = Double.parseDouble(checkIfEmpty(jsonData.get("series_complete_pop_pct")));
        percentDose = Double.parseDouble(checkIfEmpty(jsonData.get("administered_dose1_pop_pct")));
        numVaccinated = Integer.parseInt(checkIfEmpty(jsonData.get("series_complete_yes")));
        numDose = Integer.parseInt(checkIfEmpty(jsonData.get("administered_dose1_recip")));
        abbreviation = (String)jsonData.get("location");

        if(abbreviation != null) {
            percentBooster = Double.parseDouble(checkIfEmpty(jsonData.get("additional_doses_vax_pct")));
            numBooster = Integer.parseInt(checkIfEmpty(jsonData.get("additional_doses")));
            numTotal = Integer.parseInt(checkIfEmpty(jsonData.get("administered")));
            for (int i = 0; i < abbList.length; i++) {
                if (abbList[i].equals(abbreviation)) {
                    name = namesList[i];
                }
            }
        } else {
            name = (String)jsonData.get("recip_county");
        }

        date = parseFormat.parse((String) Objects.requireNonNull(jsonData.get("date")));
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() { return abbreviation; }
    public int getNVax() { return numVaccinated; }
    public int getNDose() { return numDose; }
    public int getNBoost() { return numBooster; }
    public double getPVax() { return percentVaccinated; }
    public double getPDose() { return percentDose; }
    public double getPBoost() { return percentBooster; }
    public int getTotal() { return numTotal; }
    public int getPos() { return pos; }

    public void setPos(int nPos) { pos = nPos; }

    public String checkIfEmpty(Object data) {
        String check = (String)data;
        if(data != null && !check.equals(""))
            return check;
        else
            return "0";
    }

    public String getDate() {
        return stringFormat.format(date);
    }


    public int compareTo(Object o) {
        NationalData other = (NationalData) o;
        return name.compareTo(other.getName());
    }
}
