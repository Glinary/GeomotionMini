package com.mobdeve.s11.mco3.mco3javaversion;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
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
import android.widget.Switch;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.Manifest;

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
    private List<Double> filteredDataList;


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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
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
        filteredDataList = new ArrayList<>();

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

        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<String>>(){}.getType();
             sortedAnomalyList = gson.fromJson(json, type);
        }

        if (sortedAnomalyList.isEmpty()){
            sortedAnomalyList.add("Pothole");
            sortedAnomalyList.add("Speed Bump");
            sortedAnomalyList.add("Road Crack");
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(requireContext(), "GPS is disabled!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentLocation != null && isRecording) {
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

                //use "filteredData" for further processing

                // Add the filtered data to the list
                filteredDataList.add(filteredData);

                // Check if sliding window logic should be triggered
                if (filteredDataList.size() >= WINDOW_SIZE) {
                    List<List<Double>> windows = generateSlidingWindows(filteredDataList, WINDOW_SIZE);
                    // Process the windows as needed

                    // Extract features
                    float[] inputFeatures = prepareFeaturesForModel(windows);

                    // Run TensorFlow Lite model
                    TFLiteModel model = new TFLiteModel(requireContext(), "model.tflite"); //TODO: CHANGE TO PATH OF MODEL
                    float[] predictions = model.runInference(inputFeatures, windows.size());

                    // Process predictions
                    for (float prediction : predictions) {
                        Log.d("TFLitePrediction", "Prediction: " + prediction);
                    }

                    // Optionally clear old data to avoid memory overhead
                    filteredDataList.clear();
                }

                // PREPROCESSING END //

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                for (String anomaly : sortedAnomalyList) {
                    float threshX = Float.parseFloat(prefs.getString("anomaly_" + anomaly + "_accX", "0"));
//                    float threshY = Float.parseFloat(prefs.getString("anomaly_" + anomaly + "_accY", "0"));
//                    float threshZ = Float.parseFloat(prefs.getString("anomaly_" + anomaly + "_accZ", "0"));

                    if (Math.abs(x) > threshX) {
                        myDB.addCoordinate((int) recordingId, currentLocation.getLatitude(), currentLocation.getLongitude(), anomaly);
                    }
                }
            } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                float angularX = event.values[0];
                float angularY = event.values[1];
                float angularZ = event.values[2];

                for (String anomaly : sortedAnomalyList) {
                    float gyroThreshX = Float.parseFloat(prefs.getString("anomaly_" + anomaly + "_gyroX", "0"));
                    float gyroThreshY = Float.parseFloat(prefs.getString("anomaly_" + anomaly + "_gyroY", "0"));
                    float gyroThreshZ = Float.parseFloat(prefs.getString("anomaly_" + anomaly + "_gyroZ", "0"));

                    if (Math.abs(angularX) > gyroThreshX || Math.abs(angularY) > gyroThreshY || Math.abs(angularZ) > gyroThreshZ) {
                        myDB.addCoordinate((int) recordingId, currentLocation.getLatitude(), currentLocation.getLongitude(), anomaly);
                    }
                }
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
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
            if (gyroscope != null) { // Register gyroscope listener
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        // Start listening to GPS updates
        if (locationManager != null) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
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

    public List<List<Double>> generateSlidingWindows(List<Double> data, int windowSize) {
        List<List<Double>> windows = new ArrayList<>();

        for (int i = 0; i <= data.size() - windowSize; i++) {
            List<Double> window = new ArrayList<>(data.subList(i, i + windowSize));
            windows.add(window);
        }

        return windows;
    }

    public List<double[]> flattenWindows(List<List<Double>> windows) {
        List<double[]> flattenedWindows = new ArrayList<>();

        for (List<Double> window : windows) {
            double[] flattened = new double[window.size()];
            for (int i = 0; i < window.size(); i++) {
                flattened[i] = window.get(i);
            }
            flattenedWindows.add(flattened);
        }

        return flattenedWindows;
    }

    public static class LabeledWindow {
        public double[] window;
        public int label; // or String label, depending on your use case

        public LabeledWindow(double[] window, int label) {
            this.window = window;
            this.label = label;
        }
    }

    public List<LabeledWindow> labelWindows(List<double[]> flattenedWindows, int defaultLabel) {
        List<LabeledWindow> labeledWindows = new ArrayList<>();

        for (double[] window : flattenedWindows) {
            int label = determineLabel(window); // Implement your own labeling logic
            labeledWindows.add(new LabeledWindow(window, label));
        }

        return labeledWindows;
    }

    private int determineLabel(double[] window) {
        // Example: Use the last value of the window as the label
        return (int) window[window.length - 1];
    }

    public StatisticalFeatures extractFeatures(List<Double> window) {
        int n = window.size();
        double sum = 0.0;
        double sumSquared = 0.0;

        // Calculate sum and sum of squares
        for (double value : window) {
            sum += value;
            sumSquared += value * value;
        }

        float mean = (float) (sum / n);
        float variance = (float) ((sumSquared / n) - (mean * mean));
        float stdDev = (float) Math.sqrt(variance);

        return new StatisticalFeatures(mean, stdDev, variance);
    }

    public float[] prepareFeaturesForModel(List<List<Double>> windows) {
        List<float[]> featureList = new ArrayList<>();

        for (List<Double> window : windows) {
            StatisticalFeatures features = extractFeatures(window);
            featureList.add(new float[]{features.mean, features.stdDev, features.variance});
        }

        // Flatten the list of features into a single array for the TensorFlow Lite model
        int numWindows = featureList.size();
        int numFeatures = 3; // Mean, StdDev, Variance
        float[] inputFeatures = new float[numWindows * numFeatures];

        int index = 0;
        for (float[] feature : featureList) {
            for (float value : feature) {
                inputFeatures[index++] = value;
            }
        }

        return inputFeatures;
    }



}