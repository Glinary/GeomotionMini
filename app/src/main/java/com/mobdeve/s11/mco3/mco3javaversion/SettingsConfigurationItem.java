package com.mobdeve.s11.mco3.mco3javaversion;


import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsConfigurationItem extends AppCompatActivity {
    Button activitySettingsConfigurationItemBackButton;
    MyDatabaseHelper myDB;
    ArrayList<String> anomalyLabel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_configuration_item);

        myDB = new MyDatabaseHelper(this);
        anomalyLabel = new ArrayList<String>();

        storeDataInArrays();

        // Set Header
        String anomalyLabel = getIntent().getStringExtra("ANOMALY_LABEL");
        TextView anomalyLabelView = findViewById(R.id.anomalyLabelTextViewItem);
        anomalyLabelView.setText(anomalyLabel);


        // Set preferred setting
        SharedPreferences prefs = getSharedPreferences("AppPreferences", MODE_PRIVATE);

        EditText accX = findViewById(R.id.acc_X);
        EditText accY = findViewById(R.id.acc_Y);
        EditText accZ = findViewById(R.id.acc_Z);

        accX.setText(prefs.getString("anomaly_" + anomalyLabel + "_accX", "0"));
        accY.setText(prefs.getString("anomaly_" + anomalyLabel + "_accY", "0"));
        accZ.setText(prefs.getString("anomaly_" + anomalyLabel + "_accZ", "0"));

        EditText gyroX = findViewById(R.id.gyro_X);
        EditText gyroY = findViewById(R.id.gyro_Y);
        EditText gyroZ = findViewById(R.id.gyro_Z);

        gyroX.setText(prefs.getString("anomaly_" + anomalyLabel + "_gyroX", "0"));
        gyroY.setText(prefs.getString("anomaly_" + anomalyLabel + "_gyroY", "0"));
        gyroZ.setText(prefs.getString("anomaly_" + anomalyLabel + "_gyroZ", "0"));

        activitySettingsConfigurationItemBackButton = findViewById(R.id.activitySettingsConfigurationItemBackButton);
        activitySettingsConfigurationItemBackButton.setOnClickListener(v -> {
            // Set Configuration

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("anomaly_" + anomalyLabel + "_accX", accX.getText().toString());
            editor.putString("anomaly_" + anomalyLabel + "_accY", accY.getText().toString());
            editor.putString("anomaly_" + anomalyLabel + "_accZ", accZ.getText().toString());

            editor.putString("anomaly_" + anomalyLabel + "_gyroX", gyroX.getText().toString());
            editor.putString("anomaly_" + anomalyLabel + "_gyroY", gyroY.getText().toString());
            editor.putString("anomaly_" + anomalyLabel + "_gyroZ", gyroZ.getText().toString());
            editor.apply();
            finish();

            sortLabels(prefs);
        });
    }

    void storeDataInArrays() {
        Cursor cursor = myDB.getAllAnomalyNames();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String anomalyName = cursor.getString(cursor.getColumnIndexOrThrow("anomaly_name"));
                anomalyLabel.add(anomalyName);
            }
            cursor.close();
        }
    }

    void sortLabels(SharedPreferences prefs){
        Map<String, Float> combinedAxes = new HashMap<>();
        Gson gson = new Gson();

        for (String anomaly : anomalyLabel) {
            float threshX = Float.parseFloat(prefs.getString("anomaly_" + anomalyLabel + "_accX", "0"));
            float threshY = Float.parseFloat(prefs.getString("anomaly_" + anomalyLabel + "_accY", "0"));
            float threshZ = Float.parseFloat(prefs.getString("anomaly_" + anomalyLabel + "_accZ", "0"));

            // Combine gyroscope thresholds
            float gyroThreshX = Float.parseFloat(prefs.getString("anomaly_" + anomaly + "_gyroX", "0"));
            float gyroThreshY = Float.parseFloat(prefs.getString("anomaly_" + anomaly + "_gyroY", "0"));
            float gyroThreshZ = Float.parseFloat(prefs.getString("anomaly_" + anomaly + "_gyroZ", "0"));

            // Combine all axes for sorting
            float combined = threshX + threshY + threshZ + gyroThreshX + gyroThreshY + gyroThreshZ;
            combinedAxes.put(anomaly, combined);
        }

        // Convert the map to a list of entries
        List<Map.Entry<String, Float>> entryList = new ArrayList<>(combinedAxes.entrySet());

        // Sort the list based on values in descending order
        Collections.sort(entryList, new Comparator<Map.Entry<String, Float>>() {
            @Override
            public int compare(Map.Entry<String, Float> entry1, Map.Entry<String, Float> entry2) {
                // Reverse the comparison for descending order
                return entry2.getValue().compareTo(entry1.getValue());
            }
        });

        // If you want the map to be sorted, you can create a new linked map (to maintain insertion order)
        ArrayList<String> sortedCombinedAxes = new ArrayList<>();
        for (Map.Entry<String, Float> entry : entryList) {
            sortedCombinedAxes.add(entry.getKey());
        }

        String json = gson.toJson(sortedCombinedAxes);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("anomalySort", json);
        editor.apply();

    }

}