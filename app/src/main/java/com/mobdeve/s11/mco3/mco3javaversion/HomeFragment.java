package com.mobdeve.s11.mco3.mco3javaversion;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment implements SensorEventListener {

    Button recordingButton;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private MyDatabaseHelper myDB;
    private long recordingId; // Track the current recording ID
    private boolean isRecording = false; // Flag to track recording state

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize SensorManager and Accelerometer
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Initialize database helper
        myDB = new MyDatabaseHelper(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recordingButton = view.findViewById(R.id.recordingButton);
        recordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isRecording) {
                    startRecording();
                } else {
                    stopRecording();
                }

            }
        });

        return view;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isRecording && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Check for specific X, Y, Z values (e.g., detect bumps)
            if (Math.abs(x) > 10 || Math.abs(y) > 10 || Math.abs(z) > 20) {
                // Add a coordinate entry with the detected anomaly
                myDB.addCoordinate((int) recordingId, x, y, "Anomaly Detected");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void startRecording() {
        // Create a new recording
        SimpleDateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat formatter2 = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        String currentDate = formatter1.format(date);
        String currentTimestamp = formatter2.format(date);

        recordingId = myDB.addRecording(currentDate, currentTimestamp); // Get the recording ID
        isRecording = true;
        recordingButton.setText("Stop Recording");

        // Start listening to accelerometer events
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void stopRecording() {
        isRecording = false;
        recordingButton.setText("Start Recording");

        // Stop listening to accelerometer events
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }
}