package com.mobdeve.s11.mco3.mco3javaversion;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.mobdeve.s11.mco3.mco3javaversion.ml.Model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.Manifest;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment implements SensorEventListener, LocationListener {

    Button recordingButton;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private MyDatabaseHelper myDB;
    private long recordingId; // Track the current recording ID
    private boolean isRecording = false; // Flag to track recording state
    private LocationManager locationManager;  // Add LocationManager for GPS
    private Location currentLocation;  // Store the current GPS location
    private NavigationControl navigationControl;

    private ButterworthFilter lowPassFilter;
    private ButterworthFilter highPassFilter;
    private int WINDOW_SIZE;
    private Queue<Float> slidingWindow;
    private double[] features;
    private FeatureClassifier featureClassifier;


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

        // Check for location permission
//        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(requireActivity(),
//                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
//        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // Initialize SensorManager and Accelerometer
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        // Initialize database helper
        myDB = new MyDatabaseHelper(requireContext());

        // Initialize LocationManager for GPS
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        // Uncomment drop when using version used by other
//        myDB.dropTable();
//        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPreferences", MODE_PRIVATE);
//        SharedPreferences.Editor editor = prefs.edit();
//        editor.clear();  // Clears all data in SharedPreferences
//        editor.apply();

        double [] bLow = { 3.75683802e-06, 1.12705141e-05, 1.12705141e-05, 3.75683802e-06 }; /* Insert b_low from Python */
        double [] aLow = { 1.0, -2.93717073, 2.87629972, -0.93909894 }; /* Insert a_low from Python */
        double[] bHigh = { 0.9911536, -1.98230719, 0.9911536 }; /* Insert b_lowhigh from Python */
        double[] aHigh = { 1.0, -1.98222893, 0.98238545 }; /* Insert a_lowhigh from Python */
        lowPassFilter = new ButterworthFilter(bLow, aLow);
        highPassFilter = new ButterworthFilter(bHigh, aHigh);
        WINDOW_SIZE = 150;
        slidingWindow = new LinkedList<>();
        features = new double[3]; // [mean, variance, stdDev]
        featureClassifier = new FeatureClassifier(requireContext());


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPreferences", MODE_PRIVATE);

        SwitchCompat accelerometer = view.findViewById(R.id.sw_accel);
        SwitchCompat  gyroscope = view.findViewById(R.id.sw_gyro);
        SwitchCompat  gps = view.findViewById(R.id.sw_gps);

        accelerometer.setChecked(prefs.getBoolean("isAccel", false));
        gyroscope.setChecked(prefs.getBoolean("isGyro", false));
        gps.setChecked(prefs.getBoolean("isGPS", false));


        accelerometer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isAccel", isChecked);
                editor.apply();
            }
        });

        gyroscope.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isGyro", isChecked);
                editor.apply();
            }
        });

        gps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isGPS", isChecked);
                editor.apply();
            }
        });


        recordingButton = view.findViewById(R.id.recordingButton);
        recordingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Boolean isAccActivated = prefs.getBoolean("isAccel", false);
                Boolean isGPSActivated = prefs.getBoolean("isGPS", false);
                Boolean isGyroActivated = prefs.getBoolean("isGyro", false);

                if (!isRecording && isAccActivated && isGPSActivated && isGyroActivated) {
                    startRecording();
                    navigationControl.setAllowNavigation(false); // Disable navigation
                    recordingButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#A44661FF")));
                } else if (!isRecording && (!isAccActivated || !isGPSActivated || !isGyroActivated)) {
                    Toast.makeText(requireContext(), "Please enable all sensors to record.", Toast.LENGTH_SHORT).show();
                } else{
                    stopRecording();
                    recordingButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#474983")));
                    navigationControl.setAllowNavigation(true); // Enable navigation
                }
            }
        });

        return view;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPreferences", MODE_PRIVATE);
        String json = prefs.getString("anomalySort", null);
        ArrayList<String> sortedAnomalyList = new ArrayList<>();
        Integer WINDOW_SIZE = 150;

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(requireContext(), "GPS is disabled!", Toast.LENGTH_SHORT).show();
            return;
        }


        if (this.currentLocation != null && isRecording) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                // PREPROCESSING START //
                double rawData = Math.sqrt(
                        Math.pow(event.values[0], 2) +
                                Math.pow(event.values[1], 2) +
                                Math.pow(event.values[2], 2)
                );

                // Apply low-pass filter
                double lowPassFiltered = lowPassFilter.apply(rawData);

                // Apply high-pass filter
                double filteredData = highPassFilter.apply(lowPassFiltered);

                processNewData((float)filteredData);

                // PREPROCESSING END //
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
        if (sensorManager != null) {
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, 10000); // 100 Hz
            }
            if (gyroscope != null) { // Register gyroscope listener
                sensorManager.registerListener(this, gyroscope, 10000); // 100 Hz
            }
        }

        // Start listening to GPS updates
        if (locationManager != null) {
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        isRecording = false;
        recordingButton.setText("Start Recording");

        // Stop listening to accelerometer events
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        this.currentLocation = location;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NavigationControl) {
            navigationControl = (NavigationControl) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement NavigationControl");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationControl = null;
    }


    // Process new data points as they arrive
    public void processNewData(float newData) {
        // Add new data to the sliding window
        slidingWindow.add(newData);
        if (slidingWindow.size() > WINDOW_SIZE) {
            slidingWindow.poll(); // Remove the oldest data point
        }

        // Process only when the window is full
        if (slidingWindow.size() == WINDOW_SIZE) {
            computeFeatures();
            classify(features); // Classify using computed features
        }
    }

    // Compute features for the current window
    public void computeFeatures() {
        double sum = 0.0;
        double sumOfSquares = 0.0;

        for (float value : slidingWindow) {
            sum += value;
            sumOfSquares += Math.pow(value, 2);
        }

        double mean = sum / WINDOW_SIZE;
        double variance = (sumOfSquares / WINDOW_SIZE) - Math.pow(mean, 2);
        double stdDev = Math.sqrt(variance);

        features[0] = mean;
        features[1] = stdDev;
        features[2] = variance;
    }

    // Classify the current features
    private void classify(double[] features) {
        System.out.printf("Classifying with features - Mean: %.3f, Variance: %.3f, StdDev: %.3f%n",
                features[0], features[1], features[2]);

        // Pass the features to your classifier
        String outputClass = featureClassifier.classify((float) features[0], (float) features[1], (float) features[2]);

        if (outputClass.equals("Bump")) {
            Toast.makeText(requireContext(), "Bump detected", Toast.LENGTH_SHORT).show();
            myDB.addCoordinate((int) recordingId, currentLocation.getLatitude(), currentLocation.getLongitude(), "Bump");
        }
    }

}

