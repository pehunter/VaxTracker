package com.patrick.vaccinetracker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.vaccinetracker.R;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;

public class RetriverActivity extends AppCompatActivity {
    //list of all states. Will only be filled once.
    static String countiesURL = "https://data.cdc.gov/resource/8xkx-amqh.json";
    static String statesURL = "https://data.cdc.gov/resource/unsk-b7fc.json";
    //1 = good, 0 = empty, -1 = failed
    static int response = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_retriver);
        TextView info = findViewById(R.id.retriver_response);
        Button retry = findViewById(R.id.retriver_retry);

        //retrieve data
        retry.setOnClickListener(v -> {
            info.setText("Retrieving data...");
            retry.setVisibility(View.INVISIBLE);
            if(tryItOut(info, retry))
                finish();
        });

        if(tryItOut(info, retry))
            finish();
    }

    public List<NationalData> getList(String aDate, String dbLink) throws InterruptedException {
        ThreadFunc data = new ThreadFunc(dbLink, aDate);
        Thread run = new Thread(data);
        run.start();
        run.join();
        return data.getThisList();
    }

    public boolean tryItOut(TextView output, Button retry) {
        try {
            //For states, scan all entries from previous week, and find most recent one that works.
            response = -1;
            int count = isNetworkConnected() ? 7 : 0;
            Calendar time = Calendar.getInstance();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            while (response != 1 && count > 0) {
                MainActivity.statesData = getList(formatter.format(time.getTime()) + "T00:00:00.000", statesURL);
                time.add(Calendar.DAY_OF_YEAR, -1);
                count--;
            }
            if (count <= 0) {
                output.setText("Couldn't retrieve data.");
                retry.setVisibility(View.VISIBLE);
                return false;
            } else {
                output.setText("Loading map...");
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int getResponse() {
        return response;
    }

    //check if device has internet access
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    class ThreadFunc implements Runnable {
        List<NationalData> thisList = new ArrayList<>();
        String date = "";
        String url = "";
        public ThreadFunc(String nUrl, String nDate) {
            url = nUrl;
            date = nDate;
        }

        public void run() {
            response = 1;

            //try finding file first. add the database index to it maybe?
            String fileName = getFilesDir() + "/" + date.substring(0,10) + url.substring(30);
            File JSONData = new File(fileName);
            Scanner reader = null;
            try {
                reader = new Scanner(JSONData);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            String jsonString = "";
            try {
                if(reader == null) {
                    Log.d("READER", "Reading from internet!");
                    //get data from the URL (don't forget to make it replaceable)
                    URL request = new URL(url + "?$where=date = '"+date+"'");
                    HttpURLConnection connection = (HttpURLConnection) request.openConnection();
                    connection.setRequestProperty("X-App-Token", "zveUhFZik6G2sHpDpqqYI1a3X");

                    if (connection.getResponseCode() < 200 || connection.getResponseCode() > 299) {
                        throw new Exception("Bad response: " + connection.getResponseCode());
                    }

                    //switch to connection
                    reader = new Scanner(new BufferedInputStream(connection.getInputStream()));
                    String line;
                    while (reader.hasNextLine()) {
                        line = reader.nextLine();
                        jsonString += line + "\n";
                    }

                    //check if empty
                    if(jsonString.length() < 10) {
                        response = 0;
                    } else {
                        //save to file
                        FileWriter save = new FileWriter(fileName);
                        save.write(jsonString);
                        save.close();
                        Log.d("FILE SAVED", fileName);
                    }
                } else {
                    Log.d("READER", "Reading from file!");
                    String line;
                    while (reader.hasNextLine()) {
                        line = reader.nextLine();
                        jsonString += line + "\n";
                    }
                }
                //only proceed if ok
                if(response > 0) {
                    //parse JSON and fill table
                    JSONParser parser = new JSONParser();
                    JSONArray jsonArray = (JSONArray) parser.parse(jsonString);

                    for (int entry = 0; entry < jsonArray.size(); entry++) {
                        JSONObject jObj = (JSONObject) jsonArray.get(entry);
                        NationalData area = new NationalData(jObj);

                        //binary add to list
                        if(!area.getName().equals("")) {
                            int l = 0;
                            int h = thisList.size() - 1;
                            while (l <= h) {
                                int mid = (l + h) / 2;
                                NationalData midpt = thisList.get(mid);
                                if (area.compareTo(midpt) > 0) {
                                    l = mid + 1;
                                } else {
                                    h = mid - 1;
                                }
                            }
                            thisList.add(l, area);
                        }

                    }

                    //set pos
                    for(int entry = 0; entry < 52; entry++) {
                        thisList.get(entry).setPos(entry);
                    }
                }
            } catch (Exception e) {
                Log.d("FAILURE", e.toString());
                response = -1;
            }
        }

        public List<NationalData> getThisList() {
            return thisList;
        }
    }
}