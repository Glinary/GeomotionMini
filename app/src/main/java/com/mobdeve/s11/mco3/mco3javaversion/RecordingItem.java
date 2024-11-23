package com.mobdeve.s11.mco3.mco3javaversion;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordingItem extends AppCompatActivity  implements OnMapReadyCallback, CoordinatesInterface {

    RecyclerView recyclerView;
    MyDatabaseHelper myDB;
    List<Map<String, Double>> coordinatesList;
    CoordinatesAdapter coordinatesAdapter;
    ArrayList<String> anomalyList;
    Button returnButton;
    ImageView maps;
    private GoogleMap myMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_item);

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

        coordinatesAdapter = new CoordinatesAdapter(this, coordinatesList, anomalyList, this);
        recyclerView.setAdapter(coordinatesAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        returnButton = findViewById(R.id.activityRecordingItemBackButton);
        returnButton.setOnClickListener(v -> {
            finish();
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    void storeDataInArrays(int recordingId) {
        Cursor cursor = myDB.getCoordinates(recordingId);
        if (cursor == null) {
            Toast.makeText(this, "DB is null", Toast.LENGTH_SHORT).show();
        } else if (cursor.getCount() == 0) {
            Toast.makeText(this, "No Data Found", Toast.LENGTH_SHORT).show();
        } else {
            while (cursor.moveToNext()) {
                HashMap<String, Double> coordinate = new HashMap<>();
                double latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"));
                double longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"));
                coordinate.put("lat", latitude);
                coordinate.put("lon", longitude);
                coordinatesList.add(coordinate);
                anomalyList.add(cursor.getString(cursor.getColumnIndexOrThrow("anomaly")));
            }
        }

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;
        SharedPreferences prefs = this.getSharedPreferences("AppPreferences", MODE_PRIVATE);
        Map<String, Double> coordinate = new HashMap<>();

//        Toast.makeText(this, "AnomalyList Count: " + Integer.toString(anomalyList.size()), Toast.LENGTH_SHORT).show();

        for (int i = 0; i < coordinatesList.size(); i++) {
            coordinate = coordinatesList.get(i);
            double latitude = coordinate.get("lat");
            double longitude = coordinate.get("lon");

            LatLng location = new LatLng(latitude, longitude);
            String anomaly = anomalyList.get(i);

            myMap.addMarker(new MarkerOptions().position(location).title(anomaly).icon(BitmapDescriptorFactory.defaultMarker(prefs.getFloat("Hue_" + anomaly, 0))));
        }

//        LatLng sydney = new LatLng(-34,151);
//        myMap.addMarker(new MarkerOptions().position(sydney).title("Sydney"));
//        float zoomLevel = 14.0f;
//        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, zoomLevel));

    }

    @Override
    public void onItemClick(int position) {
        // Get the coordinates of the selected item
        Map<String, Double> coordinate = coordinatesList.get(position);
        double latitude = coordinate.get("lat");
        double longitude = coordinate.get("lon");

        // Focus the map on the clicked marker
        LatLng location = new LatLng(latitude, longitude);
        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
    }
}