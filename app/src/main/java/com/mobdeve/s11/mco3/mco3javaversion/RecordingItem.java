package com.mobdeve.s11.mco3.mco3javaversion;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

//import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;

public class RecordingItem extends AppCompatActivity {

    RecyclerView recyclerView;
    MyDatabaseHelper myDB;
    ArrayList<String> coordinatesList;
    CoordinatesAdapter coordinatesAdapter;
    ArrayList<String> anomalyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_recording_item);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
        String date = getIntent().getStringExtra("DATE");
        String timestamp = getIntent().getStringExtra("TIMESTAMP");
        int recordingId = getIntent().getIntExtra("RECORDING_ID", -1);

        TextView dateView = findViewById(R.id.dateHeader);
        TextView timestampView = findViewById(R.id.timestampHeader);

        dateView.setText(date);
        timestampView.setText(timestamp);

        //Recyclerview

        recyclerView = findViewById(R.id.coordinatesRecyclerView);

        myDB = new MyDatabaseHelper(this);
        coordinatesList = new ArrayList<>();
        anomalyList = new ArrayList<String>();

        storeDataInArrays(recordingId);

        coordinatesAdapter = new CoordinatesAdapter(this, coordinatesList, anomalyList);
        recyclerView.setAdapter(coordinatesAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

    }

    void storeDataInArrays(int recordingId) {
        Cursor cursor = myDB.getCoordinates(recordingId);
        if (cursor == null) {
            Toast.makeText(this, "DB is null", Toast.LENGTH_SHORT).show();
        } else if (cursor.getCount() == 0) {
            Toast.makeText(this, "No Data Found", Toast.LENGTH_SHORT).show();
        } else {
            while (cursor.moveToNext()) {
                double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
                double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
                coordinatesList.add("Lat: " + latitude + ", Lon: " + longitude);
                anomalyList.add(cursor.getString(cursor.getColumnIndexOrThrow("anomaly")));
            }
        }

    }
}