package com.mobdeve.s11.mco3.mco3javaversion;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;

public class RecordingItem extends AppCompatActivity  implements OnMapReadyCallback {

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

        returnButton = findViewById(R.id.activityRecordingItemBackButton);
        returnButton.setOnClickListener(v -> {
            finish();
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Toast.makeText(this, "FR Rec: " + Integer.toString(coordinatesList.size()), Toast.LENGTH_SHORT).show();
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


        Toast.makeText(this, "AnomalyList Count: " + Integer.toString(anomalyList.size()), Toast.LENGTH_SHORT).show();

        for (int i = 0; i < coordinatesList.size(); i++) {
            coordinate = coordinatesList.get(i);
            double latitude = coordinate.get("lat");
            double longitude = coordinate.get("lon");

            LatLng location = new LatLng(latitude, longitude);
            String anomaly = anomalyList.get(i);
            Log.d("ANOMALY NOW:", anomaly);

            myMap.addMarker(new MarkerOptions().position(location).title(anomaly).icon(BitmapDescriptorFactory.defaultMarker(prefs.getFloat("Hue_" + anomaly, 0))));
        }


//        LatLng sydney = new LatLng(-34,151);
//        myMap.addMarker(new MarkerOptions().position(sydney).title("Sydney"));
//        float zoomLevel = 14.0f;
//        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, zoomLevel));

    }
}